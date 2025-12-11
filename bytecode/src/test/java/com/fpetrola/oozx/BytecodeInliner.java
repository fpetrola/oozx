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

  public String inlineLd(Ld ld) {
    return inlineInstruction(ld);
  }

  public String inlineXor(Xor xor) {
    return inlineInstruction(xor);
  }

  public String inlineOr(Or or) {
    return inlineInstruction(or);
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

  private void generateLdExecute(MethodMaker mm, Ld ld, OpcodeReference target) {
    // Para MemoryPlusRegister8BitReference(target register, memory, pc, offset)
    // que es el patrón (IX+dd) o (IY+dd)
    if (target instanceof MemoryPlusRegister8BitReference memRef) {
      // 1. Leer el byte offset (dd) desde memoria en (pc + offset)
      // Variable local: int dd
      Variable dd = mm.var(int.class);
      
      // memory.read((pc + valueDelta) & 0xFFFF, 0)
      Variable memory = mm.field("memory");
      Variable pc = mm.field("pc");
      
      // Cálculo: (pc + valueDelta) & 0xFFFF
      Variable address = mm.var(int.class);
      Variable pcPlusDelta = pc.add(memRef.getValueDelta());
      Variable addressCalc = pcPlusDelta.and(0xFFFF);
      address.set(addressCalc);
      
      // Leer el byte offset
      dd.set(memory.invoke("read", address, 0));
      
      // 2. Calcular dirección destino: (IX + dd) & 0xFFFF
      Variable targetReg = mm.field("IX");
      Variable destAddr = mm.var(int.class);
      Variable regPlusDd = targetReg.add(dd);
      Variable destAddrCalc = regPlusDd.and(0xFFFF);
      destAddr.set(destAddrCalc);
      
      // 3. Escribir el valor del registro source (A) en la dirección
      Variable source = mm.field("A");
      memory.invoke("write", destAddr, source);
    }
  }

  private void generateXorExecute(MethodMaker mm, Xor xor, OpcodeReference target) {
    // Para MemoryPlusRegister8BitReference(target register, memory, pc, offset)
    // que es el patrón (IX+dd) o (IY+dd)
    if (target instanceof MemoryPlusRegister8BitReference memRef) {
      // 1. Leer el byte offset (dd) desde memoria en (pc + offset)
      Variable dd = mm.var(int.class);
      Variable memory = mm.field("memory");
      Variable pc = mm.field("pc");
      
      // Cálculo: (pc + valueDelta) & 0xFFFF
      Variable address = mm.var(int.class);
      Variable pcPlusDelta = pc.add(memRef.getValueDelta());
      Variable addressCalc = pcPlusDelta.and(0xFFFF);
      address.set(addressCalc);
      
      // Leer el byte offset
      dd.set(memory.invoke("read", address, 0));
      
      // 2. Calcular dirección de lectura: (IX + dd) & 0xFFFF
      Variable targetReg = mm.field("IX");
      Variable readAddr = mm.var(int.class);
      Variable regPlusDd = targetReg.add(dd);
      Variable readAddrCalc = regPlusDd.and(0xFFFF);
      readAddr.set(readAddrCalc);
      
      // 3. Leer el valor de la memoria
      Variable value = mm.var(int.class);
      value.set(memory.invoke("read", readAddr, 0));
      
      // 4. XOR con el registro C
      Variable source = mm.field("C");
      Variable result = mm.var(int.class);
      result.set(source.xor(value));
      
      // 5. Escribir el resultado de vuelta a la memoria
      memory.invoke("write", readAddr, result);
    }
  }

  private void generateOrExecute(MethodMaker mm, Or or, OpcodeReference target) {
    // Para MemoryPlusRegister8BitReference(target register, memory, pc, offset)
    // que es el patrón (IX+dd) o (IY+dd)
    if (target instanceof MemoryPlusRegister8BitReference memRef) {
      // 1. Leer el byte offset (dd) desde memoria en (pc + offset)
      Variable dd = mm.var(int.class);
      Variable memory = mm.field("memory");
      Variable pc = mm.field("pc");
      
      // Cálculo: (pc + valueDelta) & 0xFFFF
      Variable address = mm.var(int.class);
      Variable pcPlusDelta = pc.add(memRef.getValueDelta());
      Variable addressCalc = pcPlusDelta.and(0xFFFF);
      address.set(addressCalc);
      
      // Leer el byte offset
      dd.set(memory.invoke("read", address, 0));
      
      // 2. Calcular dirección de lectura: (IX + dd) & 0xFFFF
      Variable targetReg = mm.field("IX");
      Variable readAddr = mm.var(int.class);
      Variable regPlusDd = targetReg.add(dd);
      Variable readAddrCalc = regPlusDd.and(0xFFFF);
      readAddr.set(readAddrCalc);
      
      // 3. Leer el valor de la memoria
      Variable value = mm.var(int.class);
      value.set(memory.invoke("read", readAddr, 0));
      
      // 4. OR con el registro C
      Variable source = mm.field("C");
      Variable result = mm.var(int.class);
      result.set(source.or(value));
      
      // 5. Escribir el resultado de vuelta a la memoria
      memory.invoke("write", readAddr, result);
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
