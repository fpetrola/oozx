package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Xor;
import com.fpetrola.z80.instructions.impl.Or;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.ClassMaker;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;
import org.objectweb.asm.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Genera clases Java en bytecode directamente usando cojen/maker,
 * extrayendo e inlineando código de instrucciones de forma dinámica.
 */
public class BytecodeInliner {
  private final InstructionAnalyzer analyzer;
  private final Path sourcePath;
  private final MethodCodeExtractor extractor;
  private Path bytecodeOutputDir;
  private byte[] lastGeneratedBytecode;
  public static Map<String, byte[]> generatedBytecodes = new HashMap<>();

  public BytecodeInliner(InstructionAnalyzer analyzer, Path sourcePath) {
    this(analyzer, sourcePath, null);
  }

  public BytecodeInliner(InstructionAnalyzer analyzer, Path sourcePath, Path bytecodeOutputDir) {
    this.analyzer = analyzer;
    this.sourcePath = sourcePath;
    this.extractor = new MethodCodeExtractor(sourcePath);
    this.bytecodeOutputDir = bytecodeOutputDir;
  }

  public String inlineInstruction(TargetSourceInstruction instruction) {
    String operationName = instruction.getClass().getSimpleName();
    return generateInlinedClass(instruction, operationName);
  }

  /**
   * Retorna el bytecode de la última clase generada
   */
  public byte[] getLastGeneratedBytecode() {
    return lastGeneratedBytecode;
  }

  private String generateInlinedClass(TargetSourceInstruction instruction, String operationName) {
    String className = getClassName(instruction, operationName);
    className= className.replace("-", "_");

    // Crear class maker - cojen generará un nombre único basado en el className
    ClassMaker cm = ClassMaker.beginExternal(className);
    cm.public_();

    // Get analyzed variables in order
    Map<String, InstructionAnalyzer.VariableInfo> requiredVars = analyzer.getRequiredVariables();
    OpcodeReference target = analyzer.getTarget();

    // Add fields for target and other variables
    addFieldsInOrder(cm, requiredVars, target);

    // No agregar constructor (se omite siempre)
    // addConstructor(cm, className, instruction, target);

    // Add execute method with inlined code
    addExecuteMethod(cm, instruction, operationName, target);

    // Compilar la clase y obtener el bytecode directamente
    byte[] bytecodeBytes = cm.finishBytes();
    
    lastGeneratedBytecode = bytecodeBytes;

    generatedBytecodes.put(className, bytecodeBytes);

    // Si se especificó un directorio de salida, guardar el bytecode
    if (bytecodeOutputDir != null && bytecodeBytes != null) {
      saveBytecode(bytecodeBytes, className);
    }
    return className;
  }

  /**
   * Extrae el bytecode de una clase compilada usando el ClassLoader
   */
  private byte[] extractBytecodeFromCompiledClass(Class<?> clazz) {
    try {
      String resourcePath = clazz.getName().replace('.', '/') + ".class";
      var resourceStream = clazz.getClassLoader().getResourceAsStream(resourcePath);
      
      if (resourceStream != null) {
        return resourceStream.readAllBytes();
      }
    } catch (Exception e) {
      // Ignorar
    }
    
    return null;
  }

  /**
   * Guarda el bytecode a un archivo .class
   */
  private void saveBytecode(byte[] bytecodeBytes, String className) {
    try {
      String classPath = className.replace('.', '/') + ".class";
      Path classFile = bytecodeOutputDir.resolve(classPath);
      
      // Crear directorios si no existen
      Files.createDirectories(classFile.getParent());
      
      // Guardar el bytecode
      Files.write(classFile, bytecodeBytes);
      System.out.println("✓ Bytecode guardado en: " + classFile + " (" + bytecodeBytes.length + " bytes)");
    } catch (Exception e) {
      System.err.println("Error guardando bytecode para " + className + ": " + e.getMessage());
    }
  }

  private void addFieldsInOrder(ClassMaker cm, Map<String, InstructionAnalyzer.VariableInfo> vars, OpcodeReference target) {
     Set<String> excluded = Set.of("F", "Q");

     // 1. Variables en orden manteniendo el de inserción
     for (String name : vars.keySet()) {
       InstructionAnalyzer.VariableInfo var = vars.get(name);

       // Saltar si es excluido o si es pc/memory (los hacemos aparte)
       if (excluded.contains(name) || "memory".equals(name) || "pc".equals(name)) {
         continue;
       }

       Class<?> fieldType = resolveType(var.type);
       cm.addField(fieldType, var.name).private_();
     }

     // 2. Memory
     if (vars.containsKey("memory")) {
       Class<?> fieldType = Memory.class;
       cm.addField(fieldType, "memory").private_();
     }

     // 3. PC
     if (vars.containsKey("pc")) {
       cm.addField(int.class, "pc").private_();
     }
   }



  private void addExecuteMethod(ClassMaker cm, TargetSourceInstruction instruction, String operationName, OpcodeReference target) {
    MethodMaker mm = cm.addMethod(void.class, "execute");
    mm.public_();

    // Generar lógica según el tipo de instrucción
    if (instruction instanceof Ld ld) {
      generateLdExecute(mm, ld, target);
    } else if (instruction instanceof Xor xor) {
      generateXorExecute(mm, xor, target);
    } else if (instruction instanceof Or or) {
      generateOrExecute(mm, or, target);
    }
    // Para otras instrucciones, por ahora solo stub vacío
    
    mm.return_();
  }

  /**
   * Lee una dirección de 16 bits desde (pc + delta) en formato little-endian
   */
  private Variable readAddress16Bit(MethodMaker mm, Memory16BitReference mem16Ref) {
    Variable memory = mm.field("memory");
    Variable pc = mm.field("pc");
    
    // Leer dirección de 16 bits desde (pc + delta)
    Variable addr1 = mm.var(int.class);
    addr1.set(memory.invoke("read", pc.add(mem16Ref.getDelta()).and(0xFFFF), 0));
    Variable addr2 = mm.var(int.class);
    addr2.set(memory.invoke("read", pc.add(mem16Ref.getDelta() + 1).and(0xFFFF), 0));
    
    // Combinar en dirección de 16 bits (little-endian)
    Variable address = mm.var(int.class);
    address.set(addr1.or(addr2.shl(8)));
    return address;
  }

  private void generateLdExecute(MethodMaker mm, Ld ld, OpcodeReference target) {
    String sourceRegName = ((Register) ld.getSource()).getName();
    generateAluExecute(mm, ld, target, sourceRegName, AluOp.LD);
  }

  private void generateXorExecute(MethodMaker mm, Xor xor, OpcodeReference target) {
    generateAluExecute(mm, xor, target, "C", AluOp.XOR);
  }

  private void generateOrExecute(MethodMaker mm, Or or, OpcodeReference target) {
    generateAluExecute(mm, or, target, "C", AluOp.OR);
  }

  /**
    * Genera código de ejecución para instrucciones ALU (LD, XOR, OR, etc.)
    * que leen un valor de memoria (o registro para LD), aplican la operación y escriben el resultado.
    */
  private void generateAluExecute(MethodMaker mm, TargetSourceInstruction instruction, 
                                   OpcodeReference target, String sourceRegName, AluOp operation) {
    if (target instanceof MemoryPlusRegister8BitReference memRef) {
      MemoryPlusRegisterContext ctx = readOffsetAndCalculateAddress(mm, memRef);
      executeAluOperation(mm, ctx.memory, ctx.address, sourceRegName, operation);
    } else if (target instanceof IndirectMemory8BitReference indMem) {
      Variable address = resolveIndirectMemoryAddress(mm, indMem);
      Variable memory = mm.field("memory");
      executeAluOperation(mm, memory, address, sourceRegName, operation);
    }
  }

  /**
   * Resuelve la dirección para IndirectMemory8BitReference
   */
  private Variable resolveIndirectMemoryAddress(MethodMaker mm, IndirectMemory8BitReference target) {
    ImmutableOpcodeReference innerTarget = target.getTarget();
    
    if (innerTarget instanceof Register reg) {
      return mm.field(reg.getName());
    } else if (innerTarget instanceof Memory16BitReference mem16Ref) {
      return readAddress16Bit(mm, mem16Ref);
    }
    return null;
  }

  /**
    * Ejecuta una operación ALU: lee valor (de memoria o registro para LD), aplica operación y escribe resultado
    */
  private void executeAluOperation(MethodMaker mm, Variable memory, Variable address, 
                                     String sourceRegName, AluOp operation) {
    Variable source = mm.field(sourceRegName);
    
    // Para LD, escribir directamente sin variable intermedia
    if (operation == AluOp.LD) {
      memory.invoke("write", address, source);
      return;
    }
    
    // Para XOR/OR: leer, aplicar operación y escribir
    Variable value = mm.var(int.class);
    value.set(memory.invoke("read", address, 0));
    Variable result = mm.var(int.class);
    
    switch (operation) {
      case XOR -> result.set(source.xor(value));
      case OR -> result.set(source.or(value));
      default -> {}  // LD ya fue manejado arriba
    }
    
    // Escribir el resultado
    memory.invoke("write", address, result);
  }

  /**
    * Enumeración de operaciones ALU soportadas
    */
  private enum AluOp {
    LD, XOR, OR
  }

  /**
   * Lee el byte offset (dd) desde memoria en (pc + valueDelta) y calcula la dirección
   * destino como (targetReg + dd) & 0xFFFF. Retorna el contexto con memoria y dirección.
   */
  private MemoryPlusRegisterContext readOffsetAndCalculateAddress(MethodMaker mm, MemoryPlusRegister8BitReference memRef) {
    // 1. Leer el byte offset (dd) desde memoria en (pc + valueDelta)
    Variable dd = mm.var(int.class);
    Variable memory = mm.field("memory");
    Variable pc = mm.field("pc");
    
    // Cálculo: (pc + valueDelta) & 0xFFFF
    Variable pcPlusDelta = pc.add(memRef.getValueDelta());
    Variable addressDelta = pcPlusDelta.and(0xFFFF);
    dd.set(memory.invoke("read", addressDelta, 0));
    
    // 2. Calcular dirección destino: (targetReg + dd) & 0xFFFF
    Variable targetReg = mm.field("IX");
    Variable regPlusDd = targetReg.add(dd);
    Variable address = mm.var(int.class);
    address.set(regPlusDd.and(0xFFFF));
    
    return new MemoryPlusRegisterContext(memory, address);
  }

  /**
   * Contexto para operaciones de MemoryPlusRegister8BitReference
   */
  private static class MemoryPlusRegisterContext {
    final Variable memory;
    final Variable address;
    
    MemoryPlusRegisterContext(Variable memory, Variable address) {
      this.memory = memory;
      this.address = address;
    }
  }

  private boolean isAluOperation(TargetSourceInstruction instruction) {
    Class<?> clazz = instruction.getClass();
    try {
      return clazz.getDeclaredField(getAluOperationFieldName(clazz.getSimpleName())) != null;
    } catch (NoSuchFieldException e) {
      return false;
    }
  }

  private String getAluOperationFieldName(String instructionClassName) {
    return instructionClassName.toLowerCase() + "TableAluOperation";
  }

  private String getClassName(TargetSourceInstruction instruction, String operationName) {
    // Generar nombre sin sufijo (o agregar sufijo si necesitas múltiples variantes)
    return operationName + "Bytecode";
  }

  private String getSourceExpression(ImmutableOpcodeReference source) {
    if (source instanceof Register reg) {
      return reg.getName();
    }
    return "source.read()";
  }

  private String getRegisterName(ImmutableOpcodeReference ref) {
    if (ref instanceof Register reg) {
      return reg.getName();
    }
    return "register";
  }



  /**
    * Resuelve nombres de tipo a clases Java.
    */
   private Class<?> resolveType(String typeName) {
     return switch (typeName) {
       case "int" -> int.class;
       case "long" -> long.class;
       case "byte" -> byte.class;
       case "short" -> short.class;
       case "boolean" -> boolean.class;
       case "char" -> char.class;
       case "float" -> float.class;
       case "double" -> double.class;
       case "Register" -> Register.class;
       case "Memory" -> Memory.class;
       case "Plain8BitRegister" -> int.class;
       case "Plain16BitRegister" -> int.class;
       default -> Object.class;
     };
   }
  }
