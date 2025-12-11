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

  public BytecodeInliner(InstructionAnalyzer analyzer, Path sourcePath) {
    this(analyzer, sourcePath, null);
  }

  public BytecodeInliner(InstructionAnalyzer analyzer, Path sourcePath, Path bytecodeOutputDir) {
    this.analyzer = analyzer;
    this.sourcePath = sourcePath;
    this.extractor = new MethodCodeExtractor(sourcePath);
    this.bytecodeOutputDir = bytecodeOutputDir;
  }

  public Class<?> inlineInstruction(TargetSourceInstruction instruction) {
    String operationName = instruction.getClass().getSimpleName();
    return generateInlinedClass(instruction, operationName);
  }

  public Class<?> inlineLd(Ld ld) {
    return inlineInstruction(ld);
  }

  public Class<?> inlineXor(Xor xor) {
    return inlineInstruction(xor);
  }

  public Class<?> inlineOr(Or or) {
    return inlineInstruction(or);
  }

  private Class<?> generateInlinedClass(TargetSourceInstruction instruction, String operationName) {
    String className = getClassName(instruction, operationName);

    // Crear class maker - cojen generará un nombre único basado en el className
    ClassMaker cm = ClassMaker.begin(className, getClass().getClassLoader(), Object.class);
    cm.public_();

    // Get analyzed variables in order
    Map<String, InstructionAnalyzer.VariableInfo> requiredVars = analyzer.getRequiredVariables();
    OpcodeReference target = analyzer.getTarget();

    // Add fields for target and other variables
    addFieldsInOrder(cm, requiredVars, target);

    // Add constructor
    addConstructor(cm, className, target);

    // Add execute method with inlined code
    addExecuteMethod(cm, instruction, operationName, target);

    // Compilar la clase
    Class<?> compiledClass = cm.finish();
    
    // Si se especificó un directorio de salida, guardar el bytecode
    if (bytecodeOutputDir != null) {
      // Obtener el bytecode de la clase compilada
      byte[] bytecodeBytes = extractBytecodeFromCompiledClass(compiledClass);
      if (bytecodeBytes != null) {
        saveBytecode(bytecodeBytes, className);
      }
    }

    return compiledClass;
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
      cm.addField(Register.class, "pc").private_();
    }
  }

  private void addConstructor(ClassMaker cm, String className, OpcodeReference target) {
    List<Class<?>> paramTypes = new ArrayList<>();
    List<String> paramNames = new ArrayList<>();

    if (target instanceof MemoryPlusRegister8BitReference || target instanceof IndirectMemory8BitReference) {
      paramTypes.add(Memory.class);
      paramNames.add("memory");
    }

    if (target instanceof MemoryPlusRegister8BitReference) {
      paramTypes.add(Register.class);
      paramNames.add("pc");
    } else if (target instanceof IndirectMemory8BitReference indMem) {
      if (indMem.getTarget() instanceof Memory16BitReference) {
        paramTypes.add(Register.class);
        paramNames.add("pc");
      }
    }

    Class<?>[] types = paramTypes.toArray(new Class<?>[0]);
    MethodMaker mm = cm.addConstructor(types);
    mm.public_();
    
    // Llamar a super()
    mm.invokeSuperConstructor();

    for (int i = 0; i < paramNames.size(); i++) {
      String paramName = paramNames.get(i);
      Variable field = mm.field(paramName);
      Variable param = mm.param(i);
      field.set(param);
    }

    mm.return_();
  }

  private void addExecuteMethod(ClassMaker cm, TargetSourceInstruction instruction, String operationName, OpcodeReference target) {
    MethodMaker mm = cm.addMethod(void.class, "execute");
    mm.public_();

    // Por ahora solo generamos un stub vacío que compila
    // La lógica de bytecode se puede expandir después

    mm.return_();
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
    OpcodeReference target = analyzer.getTarget();
    int suffix = 1;
    if (target instanceof MemoryPlusRegister8BitReference) {
      suffix = 1;
    } else if (target instanceof IndirectMemory8BitReference indMem) {
      if (indMem.getTarget() instanceof Register) {
        suffix = 2;
      } else if (indMem.getTarget() instanceof Memory16BitReference) {
        suffix = 3;
      }
    }
    return operationName + "Bytecode" + suffix;
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
      default -> Object.class;
    };
  }
}
