package com.fpetrola.oozx;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.opcodes.references.IndirectMemory16BitReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;
import org.cojen.maker.ClassMaker;
import org.cojen.maker.Label;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

import java.util.*;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

/**
 * Genera clases Java en bytecode directamente usando cojen/maker,
 * extrayendo e inlineando código de instrucciones de forma dinámica.
 */
public class BytecodeInliner {
  public static final String FLAG = "F";
  private final InstructionAnalyzer analyzer;
  private byte[] lastGeneratedBytecode;
  public static Map<String, byte[]> generatedBytecodes = new HashMap<>();

  public BytecodeInliner(InstructionAnalyzer analyzer) {
    this.analyzer = analyzer;
  }

  public String inlineInstruction(TargetSourceInstruction instruction) {
    String operationName = instruction.getClass().getSimpleName();
    return generateInlinedClass(instruction, operationName);
  }

  /**
   * Genera una clase con múltiples métodos execute a partir de varias instrucciones
   */
  public String inlineMultipleInstructions(String className, Map<Integer, Instruction> instructions) {
    className = className.replace("-", "_");

    ClassMaker cm = createBaseClass(className);

    // Guardar los nombres de métodos generados y sus opcodes asociados
    Map<Integer, String> opcodeToMethodName = new LinkedHashMap<>();
    
    // Detectar si hay prefijos (DefaultFetchNextOpcodeInstruction) y sus instrucciones
    Map<Integer, String> prefixOpcodes = new LinkedHashMap<>();  // prefixOpcode -> methodName de dispatcher
    Map<Integer, Map<Integer, String>> prefixedInstructions = new LinkedHashMap<>();  // prefixOpcode -> (nextOpcode -> methodName)
    
    // Rastrear métodos ya agregados para evitar duplicados
    Set<String> generatedMethods = new HashSet<>();

    // Agregar un método execute para cada instrucción
    for (Map.Entry<Integer, Instruction> entry : instructions.entrySet()) {
      Integer opcode = entry.getKey();
      Instruction instruction = entry.getValue();

      // Filtrar instrucciones con registros especiales I y R (LdAI, LdAR) que no se pueden inlinear
      if (isUnsupportedInstruction(instruction)) {
        continue;  // Omitir estas instrucciones
      }

      if (instruction instanceof DefaultFetchNextOpcodeInstruction prefixInstruction) {
        // Este es un prefijo - lo trataremos especialmente
        prefixOpcodes.put(opcode, prefixInstruction.getClass().getSimpleName());
        prefixedInstructions.put(opcode, new LinkedHashMap<>());
      } else if (isPrefixedOpcode(opcode, instructions)) {
        // Esta instrucción pertenece a un prefijo (ej: 0xCB00, 0xCB20, etc.)
        int prefixByte = (opcode >> 8) & 0xFF;
        int nextOpcode = opcode & 0xFF;
        
        if (instruction instanceof TargetSourceInstruction<?> targetSourceInstruction) {
          try {
            analyzer.analyze(targetSourceInstruction);
            String operationName = instruction.getClass().getSimpleName();
            OpcodeReference target = analyzer.getTarget();
            String methodName = generateUniquMethodName(targetSourceInstruction, operationName, target);
            
            // Solo agregar el método si no existe ya
            if (!generatedMethods.contains(methodName)) {
              addExecuteMethod(cm, targetSourceInstruction, operationName, target);
              generatedMethods.add(methodName);
            }
            prefixedInstructions.get(prefixByte).put(nextOpcode, methodName);
          } catch (Exception e) {
            // Omitir si no puede procesar
          }
        } else if (instruction instanceof ParameterizedUnaryAluInstruction unaryInstruction) {
           try {
             String operationName = instruction.getClass().getSimpleName();
             String methodName = generateUnaryMethodName(unaryInstruction, operationName);
             
             // Solo agregar el método si no existe ya
             if (!generatedMethods.contains(methodName)) {
               addExecuteUnaryMethod(cm, unaryInstruction, operationName);
               generatedMethods.add(methodName);
             }
             prefixedInstructions.get(prefixByte).put(nextOpcode, methodName);
           } catch (Exception e) {
             // Omitir si no puede procesar
             System.err.println("Warning: No se pudo procesar instrucción unaria prefijada 0x" + 
               String.format("%02X%02X", prefixByte, nextOpcode) + 
               " (" + instruction.getClass().getSimpleName() + "): " + e.getMessage());
             e.printStackTrace();
           }
         }
      } else if (instruction instanceof TargetSourceInstruction<?> targetSourceInstruction) {
        try {
          analyzer.analyze(targetSourceInstruction);
          String operationName = instruction.getClass().getSimpleName();
          OpcodeReference target = analyzer.getTarget();
          String methodName = generateUniquMethodName(targetSourceInstruction, operationName, target);
          
          // Solo agregar el método si no existe ya
          if (!generatedMethods.contains(methodName)) {
            addExecuteMethod(cm, targetSourceInstruction, operationName, target);
            generatedMethods.add(methodName);
          }
          opcodeToMethodName.put(opcode, methodName);
        } catch (Exception e) {
          // Si no puede procesar la instrucción, omitirla del switch (no generar nada)
          // La instrucción no se incluirá en opcodeToMethodName
        }
      } else if (instruction instanceof ParameterizedUnaryAluInstruction unaryInstruction) {
        try {
          String operationName = instruction.getClass().getSimpleName();
          String methodName = generateUnaryMethodName(unaryInstruction, operationName);
          
          // Solo agregar el método si no existe ya
          if (!generatedMethods.contains(methodName)) {
            addExecuteUnaryMethod(cm, unaryInstruction, operationName);
            generatedMethods.add(methodName);
          }
          opcodeToMethodName.put(opcode, methodName);
        } catch (Exception e) {
          // Si no puede procesar la instrucción, omitirla del switch
        }
      }
    }

    // Agregar métodos dispatch para prefijos si existen
    for (Map.Entry<Integer, Map<Integer, String>> prefixEntry : prefixedInstructions.entrySet()) {
      Integer prefixOpcode = prefixEntry.getKey();
      Map<Integer, String> prefixMethods = prefixEntry.getValue();
      if (!prefixMethods.isEmpty()) {
        String dispatchMethodName = generatePrefixDispatchMethodName(prefixOpcode);
        addPrefixDispatchMethod(cm, dispatchMethodName, prefixMethods);
        opcodeToMethodName.put(prefixOpcode, dispatchMethodName);
      }
    }

    // Agregar método switch que dispache por opcode
    addDispatchMethodWithOpcodes(cm, opcodeToMethodName, prefixOpcodes);

    return finializeClass(className, cm);
  }

  /**
   * Verifica si un opcode es un opcode prefijado (ej: 0xCB00 significa prefijo CB con siguiente byte 0x00)
   */
  private boolean isPrefixedOpcode(Integer opcode, Map<Integer, Instruction> instructions) {
    // Un opcode prefijado tiene más de 1 byte y existe un prefijo correspondiente
    if (opcode > 0xFF) {
      int prefixByte = (opcode >> 8) & 0xFF;
      // Buscar si existe el prefijo en las instrucciones
      for (Integer key : instructions.keySet()) {
        if (key == prefixByte && instructions.get(key) instanceof DefaultFetchNextOpcodeInstruction) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Genera el nombre del método dispatch para un prefijo
   */
  private String generatePrefixDispatchMethodName(Integer prefixOpcode) {
    String prefixName = switch(prefixOpcode & 0xFF) {
      case 0xCB -> "CB";
      case 0xDD -> "DD";
      case 0xFD -> "FD";
      default -> String.format("Prefix%02X", prefixOpcode & 0xFF);
    };
    return "execute" + prefixName + "Prefix";
  }

  /**
   * Agrega un método dispatch para un prefijo que despacha por el siguiente opcode
   */
  private void addPrefixDispatchMethod(ClassMaker cm, String methodName, Map<Integer, String> opcodeToMethodName) {
    MethodMaker mm = cm.addMethod(int.class, methodName, int.class);
    mm.public_();

    // Crear labels para cada case y el default
    int numCases = opcodeToMethodName.size();
    Label[] caseLabels = new Label[numCases];
    for (int i = 0; i < numCases; i++) {
      caseLabels[i] = mm.label();
    }
    Label defaultLabel = mm.label();
    Label endLabel = mm.label();

    // Crear array de casos a partir de los opcodes
    int[] cases = new int[numCases];
    String[] methodNames = new String[numCases];
    int idx = 0;
    for (Map.Entry<Integer, String> entry : opcodeToMethodName.entrySet()) {
      cases[idx] = entry.getKey();
      methodNames[idx] = entry.getValue();
      idx++;
    }

    // Obtener variable del parámetro nextOpcode
    Variable nextOpcodeVar = mm.param(0);
    nextOpcodeVar.name("nextOpcode");

    // Generar switch statement
    nextOpcodeVar.switch_(defaultLabel, cases, caseLabels);

    // Generar código para cada case
    for (int i = 0; i < numCases; i++) {
      caseLabels[i].here();
      mm.invoke(methodNames[i]);
      mm.goto_(endLabel);
    }

    // Default case: return -1
    defaultLabel.here();
    mm.return_(-1);

    // End label: fin del switch
    endLabel.here();
    mm.return_(0);
  }

  /**
   * Retorna el bytecode de la última clase generada
   */
  public byte[] getLastGeneratedBytecode() {
    return lastGeneratedBytecode;
  }

  /**
   * Genera una clase con múltiples instrucciones y la carga en memoria
   * Retorna el Class<?> directamente usable usando finish()
   */
  public Class<?> generateAndLoadMultipleInstructions(String className, Map<Integer, Instruction> instructions) {
    className = className.replace("-", "_");

    ClassMaker cm = createBaseClass(className);

    MethodMaker constructorMaker = cm.addConstructor();
    constructorMaker.invokeSuperConstructor();
    constructorMaker.public_();
    constructorMaker.return_();

    // Guardar los nombres de métodos generados y sus opcodes asociados
    Map<Integer, String> opcodeToMethodName = new LinkedHashMap<>();
    
    // Detectar si hay prefijos (DefaultFetchNextOpcodeInstruction) y sus instrucciones
    Map<Integer, String> prefixOpcodes = new LinkedHashMap<>();
    Map<Integer, Map<Integer, String>> prefixedInstructions = new LinkedHashMap<>();
    
    // Rastrear métodos ya agregados para evitar duplicados
    Set<String> generatedMethods = new HashSet<>();

    // Agregar un método execute para cada instrucción
    for (Map.Entry<Integer, Instruction> entry : instructions.entrySet()) {
      Integer opcode = entry.getKey();
      Instruction instruction = entry.getValue();

      // Filtrar instrucciones con registros especiales I y R (LdAI, LdAR) que no se pueden inlinear
      if (isUnsupportedInstruction(instruction)) {
        continue;  // Omitir estas instrucciones
      }

      if (instruction instanceof DefaultFetchNextOpcodeInstruction prefixInstruction) {
        // Este es un prefijo - lo trataremos especialmente
        prefixOpcodes.put(opcode, prefixInstruction.getClass().getSimpleName());
        prefixedInstructions.put(opcode, new LinkedHashMap<>());
      } else if (isPrefixedOpcode(opcode, instructions)) {
        // Esta instrucción pertenece a un prefijo (ej: 0xCB00, 0xCB20, etc.)
        int prefixByte = (opcode >> 8) & 0xFF;
        int nextOpcode = opcode & 0xFF;
        
        if (instruction instanceof TargetSourceInstruction<?> targetSourceInstruction) {
          try {
            analyzer.analyze(targetSourceInstruction);
            String operationName = instruction.getClass().getSimpleName();
            OpcodeReference target = analyzer.getTarget();
            String methodName = generateUniquMethodName(targetSourceInstruction, operationName, target);
            
            // Solo agregar el método si no existe ya
            if (!generatedMethods.contains(methodName)) {
              addExecuteMethod(cm, targetSourceInstruction, operationName, target);
              generatedMethods.add(methodName);
            }
            prefixedInstructions.get(prefixByte).put(nextOpcode, methodName);
          } catch (Exception e) {
            // Omitir si no puede procesar
          }
        } else if (instruction instanceof ParameterizedUnaryAluInstruction unaryInstruction) {
          try {
            String operationName = instruction.getClass().getSimpleName();
            String methodName = generateUnaryMethodName(unaryInstruction, operationName);
            
            // Solo agregar el método si no existe ya
            if (!generatedMethods.contains(methodName)) {
              addExecuteUnaryMethod(cm, unaryInstruction, operationName);
              generatedMethods.add(methodName);
            }
            prefixedInstructions.get(prefixByte).put(nextOpcode, methodName);
          } catch (Exception e) {
            // Omitir si no puede procesar
          }
        }
      } else if (instruction instanceof TargetSourceInstruction<?> targetSourceInstruction) {
        try {
          analyzer.analyze(targetSourceInstruction);
          String operationName = instruction.getClass().getSimpleName();
          OpcodeReference target = analyzer.getTarget();
          String methodName = generateUniquMethodName(targetSourceInstruction, operationName, target);
          
          // Solo agregar el método si no existe ya
          if (!generatedMethods.contains(methodName)) {
            addExecuteMethod(cm, targetSourceInstruction, operationName, target);
            generatedMethods.add(methodName);
          }
          opcodeToMethodName.put(opcode, methodName);
        } catch (Exception e) {
          // Si no puede procesar la instrucción, omitirla del switch (no generar nada)
          // La instrucción no se incluirá en opcodeToMethodName
        }
      } else if (instruction instanceof ParameterizedUnaryAluInstruction unaryInstruction) {
        try {
          String operationName = instruction.getClass().getSimpleName();
          String methodName = generateUnaryMethodName(unaryInstruction, operationName);
          
          // Solo agregar el método si no existe ya
          if (!generatedMethods.contains(methodName)) {
            addExecuteUnaryMethod(cm, unaryInstruction, operationName);
            generatedMethods.add(methodName);
          }
          opcodeToMethodName.put(opcode, methodName);
        } catch (Exception e) {
          // Si no puede procesar la instrucción, omitirla del switch
        }
      }
    }

    // Agregar métodos dispatch para prefijos si existen
    for (Map.Entry<Integer, Map<Integer, String>> prefixEntry : prefixedInstructions.entrySet()) {
      Integer prefixOpcode = prefixEntry.getKey();
      Map<Integer, String> prefixMethods = prefixEntry.getValue();
      if (!prefixMethods.isEmpty()) {
        String dispatchMethodName = generatePrefixDispatchMethodName(prefixOpcode);
        addPrefixDispatchMethod(cm, dispatchMethodName, prefixMethods);
        opcodeToMethodName.put(prefixOpcode, dispatchMethodName);
      }
    }

    // Agregar método switch que dispache por opcode
    addDispatchMethodWithOpcodes(cm, opcodeToMethodName, prefixOpcodes);

    // Usar finish() para cargar la clase directamente en memoria
    return cm.finish();
  }

  private String generateInlinedClass(TargetSourceInstruction instruction, String operationName) {
    String className = getClassName(instruction, operationName);
    className = className.replace("-", "_");

    ClassMaker cm = createBaseClass(className);
    OpcodeReference target = analyzer.getTarget();

    // Add execute method with inlined code
    addExecuteMethod(cm, instruction, operationName, target);

    return finializeClass(className, cm);
  }

  /**
   * Crea la clase base que extiende Z80UnRolled
   */
  private ClassMaker createBaseClass(String className) {
    ClassMaker cm = ClassMaker.beginExternal(className);
    cm.public_();
    cm.extend(Z80UnRolled.class);
    return cm;
  }

  /**
   * Compila la clase y guarda el bytecode
   */
  private String finializeClass(String className, ClassMaker cm) {
    byte[] bytecodeBytes = cm.finishBytes();
    lastGeneratedBytecode = bytecodeBytes;
    generatedBytecodes.put(className, bytecodeBytes);
    return className;
  }

  private void addAluOperationField(ClassMaker cm, TargetSourceInstruction instruction) {
    if (isAluOperation(instruction)) {
      // Add flag field if not already added
      if (!analyzer.getRequiredVariables().containsKey(FLAG)) {
        cm.addField(Register.class, FLAG).private_();
      }

      // Add ALU operation field with the correct type
      String fieldName = getAluOperationFieldName(instruction.getClass().getSimpleName());
      Class<?> aluOperationClass = getAluOperationClass(instruction);
      cm.addField(aluOperationClass, fieldName).private_();
    }
  }

  private Class<?> getAluOperationClass(TargetSourceInstruction instruction) {
    try {
      Class<?> instructionClass = instruction.getClass();
      String instructionName = instructionClass.getSimpleName();
      
      // Mapeo de nombres especiales
      String innerClassName = switch(instructionName) {
        case "RRC" -> "RRCAluOperation";
        case "SRA" -> "SRAAluOperation";
        default -> instructionName + "TableAluOperation";
      };

      // Buscar el inner class declarado
      for (Class<?> innerClass : instructionClass.getDeclaredClasses()) {
        if (innerClass.getSimpleName().equals(innerClassName)) {
          return innerClass;
        }
      }
    } catch (Exception e) {
      // Ignorar
    }
    return Object.class;
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


  /**
   * Agrega un método execute(int opcode) que despacha a los métodos específicos usando opcodes reales
   */
  private void addDispatchMethodWithOpcodes(ClassMaker cm, Map<Integer, String> opcodeToMethodName, Map<Integer, String> prefixOpcodes) {
    MethodMaker mm = cm.addMethod(int.class, "execute", int.class);
    mm.public_();

    // Crear labels para cada case y el default
    int numCases = opcodeToMethodName.size();
    Label[] caseLabels = new Label[numCases];
    for (int i = 0; i < numCases; i++) {
      caseLabels[i] = mm.label();
    }
    Label defaultLabel = mm.label();
    Label endLabel = mm.label();

    // Crear array de casos a partir de los opcodes
    int[] cases = new int[numCases];
    String[] methodNames = new String[numCases];
    int idx = 0;
    for (Map.Entry<Integer, String> entry : opcodeToMethodName.entrySet()) {
      cases[idx] = entry.getKey();
      methodNames[idx] = entry.getValue();
      idx++;
    }

    // Obtener variable del parámetro opcode y asignarle el nombre
    Variable opcodeVar = mm.param(0);
    opcodeVar.name("opcode");

    // Generar switch statement
    opcodeVar.switch_(defaultLabel, cases, caseLabels);

    // Generar código para cada case
    for (int i = 0; i < numCases; i++) {
      caseLabels[i].here();
      int currentOpcode = cases[i];
      
      // Si es un prefijo, leer el siguiente byte y despachar
      if (prefixOpcodes.containsKey(currentOpcode)) {
        String dispatchMethodName = methodNames[i];
        Variable memory = mm.field("memory");
        Variable pc = mm.field("PC");
        
        // Calcular la dirección del siguiente byte (PC + 1)
        Variable nextPc = mm.var(int.class);
        nextPc.set(pc.add(1).and(0xFFFF));
        
        Variable nextOpcode = mm.var(int.class);
        nextOpcode.set(memory.invoke("read", nextPc, 1));
        
        // Incrementar PC en 1 para apuntar al siguiente byte (después del prefijo)
        pc.set(nextPc);
        
        Variable result = mm.var(int.class);
        result.set(mm.invoke(dispatchMethodName, nextOpcode));
        mm.return_(result);
      } else {
        mm.invoke(methodNames[i]);
        mm.goto_(endLabel);
      }
    }

    // Default case: throw exception
    defaultLabel.here();
    mm.return_(-1);

    // End label: fin del switch
    endLabel.here();
    mm.return_(0);
  }

  /**
   * Versión anterior del método (sobrecargado para compatibilidad)
   */
  private void addDispatchMethodWithOpcodes(ClassMaker cm, Map<Integer, String> opcodeToMethodName) {
    addDispatchMethodWithOpcodes(cm, opcodeToMethodName, new LinkedHashMap<>());
  }

  private void addExecuteMethod(ClassMaker cm, TargetSourceInstruction instruction, String operationName, OpcodeReference target) {
    // Generar nombre de método único basado en la instrucción y sus referencias
    String methodName = generateUniquMethodName(instruction, operationName, target);
    if (methodName.equals("executeOutA"))
      System.out.println("dsagadg");
    MethodMaker[] mms = new MethodMaker[]{null};

    Supplier<MethodMaker> methodMakerSupplier = () -> {
      if (mms[0] == null) {
        mms[0] = cm.addMethod(void.class, methodName);
        mms[0].public_();
      }
      return mms[0];
    };

    generateExecute(methodMakerSupplier, instruction, target);
    mms[0].return_();
  }

  private void addExecuteUnaryMethod(ClassMaker cm, ParameterizedUnaryAluInstruction instruction, String operationName) {
    String methodName = generateUnaryMethodName(instruction, operationName);
    MethodMaker mm = cm.addMethod(void.class, methodName);
    mm.public_();
    
    // Verificar que el campo ALU existe en la clase padre
    addAluOperationFieldForUnary(cm, instruction);
    
    generateUnaryExecute(mm, instruction);
    mm.return_();
  }

  /**
   * Verifica que el campo ALU existe en la clase padre (Z80UnRolled)
   * No necesitamos agregarlo porque ya está declarado en la clase base
   */
  private void addAluOperationFieldForUnary(ClassMaker cm, ParameterizedUnaryAluInstruction instruction) {
    // Los campos ALU ya existen en Z80UnRolled, no necesitamos agregarlos
    // Solo verificamos que la instrucción tenga una operación ALU válida
    try {
      getAluOperationClassFromUnaryInstruction(instruction);
    } catch (Exception e) {
      System.err.println("Warning: Instrucción unaria no tiene operación ALU válida: " + e.getMessage());
    }
  }

  /**
   * Genera un nombre único para el método execute basado en el tipo de instrucción,
   * target, source y referencias involucradas
   */
  private String generateUniquMethodName(TargetSourceInstruction instruction, String operationName, OpcodeReference target) {
    StringBuilder methodName = new StringBuilder("execute").append(operationName);

    // Agregar información del target
    methodName.append(getReferenceSuffix(target));

    // Agregar información de source
    if (instruction instanceof TargetSourceInstruction tsi) {
      ImmutableOpcodeReference source = tsi.getSource();
      methodName.append(getReferenceSuffix(source));
    }

    return methodName.toString();
  }

  /**
   * Genera un sufijo basado en el tipo de referencia (Register, Memory, etc.)
   */
  private String getReferenceSuffix(ImmutableOpcodeReference reference) {
    if (reference instanceof Register) {
      Register reg = (Register) reference;
      // Para registros, usar el nombre del registro en formato camelCase
      return capitalizeFirstLetter(reg.getName());
    } else if (reference instanceof MemoryPlusRegister8BitReference) {
      MemoryPlusRegister8BitReference mprf = (MemoryPlusRegister8BitReference) reference;
      // MPRF: MemoryPlusRegister8BitReference
      StringBuilder suffix = new StringBuilder("Mprf");
      ImmutableOpcodeReference innerTarget = mprf.getTarget();
      if (innerTarget instanceof Register) {
        Register reg = (Register) innerTarget;
        suffix.append(capitalizeFirstLetter(reg.getName()));
      }
      return suffix.toString();
    } else if (reference instanceof IndirectMemory8BitReference) {
      IndirectMemory8BitReference imr = (IndirectMemory8BitReference) reference;
      // IMR: IndirectMemory8BitReference
      StringBuilder suffix = new StringBuilder("Imr");
      ImmutableOpcodeReference innerTarget = imr.getTarget();
      if (innerTarget instanceof Register) {
        Register reg = (Register) innerTarget;
        suffix.append(capitalizeFirstLetter(reg.getName()));
      } else if (innerTarget instanceof Memory16BitReference) {
        suffix.append("M16R");
      }
      return suffix.toString();
    } else if (reference instanceof IndirectMemory16BitReference) {
      IndirectMemory16BitReference imr16 = (IndirectMemory16BitReference) reference;
      // IMR16: IndirectMemory16BitReference
      StringBuilder suffix = new StringBuilder("Imr16");
      ImmutableOpcodeReference innerTarget = imr16.getTarget();
      if (innerTarget instanceof Register) {
        Register reg = (Register) innerTarget;
        suffix.append(capitalizeFirstLetter(reg.getName()));
      } else if (innerTarget instanceof Memory16BitReference) {
        suffix.append("M16R");
      }
      return suffix.toString();
    } else if (reference instanceof Memory16BitReference) {
      // M16R: Memory16BitReference
      return "M16R";
    } else if (reference instanceof Memory8BitReference) {
      // M16R: Memory16BitReference
      return "M8R";
    }

    return "";
  }

  /**
   * Capitaliza la primera letra de una cadena
   */
  private String capitalizeFirstLetter(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
  }

  /**
   * Lee una dirección de 16 bits desde (pc + delta) en formato little-endian
   */
  private Variable readAddress16Bit(MethodMaker mm, Memory16BitReference mem16Ref) {
    Variable pc = mm.field("PC");
    Variable read16 = mm.invoke("read16", pc);
    return read16;
  }

  private void generateExecute(Supplier<MethodMaker> mm, TargetSourceInstruction ld, OpcodeReference target) {
    // Excluir operaciones de 16 bits (Add16, Adc16, Sbc16, etc.)
//    if (ld instanceof com.fpetrola.z80.instructions.impl.Binary16BitsOperation) {
//      throw new UnsupportedOperationException("Binary16BitsOperation no soportada: " + ld.getClass().getSimpleName());
//    }

    ImmutableOpcodeReference source = ld.getSource();

    // Caso 1: source es un Register
    if (source instanceof Register sourceReg) {
      String sourceRegName = sourceReg.getName();
      // Si target es también un Register (register-to-register)
      if (target instanceof Register targetReg) {
        String targetRegName = targetReg.getName();
        if (ld instanceof Ld) {
          // LD: copiar valor del registro fuente al destino
          Variable sourceValue = resolveRegisterValueByName(mm.get(), sourceRegName);
          assignRegisterValue(mm.get(), targetRegName, sourceValue);
        } else if (isAluOperation(ld)) {
          // Operaciones ALU: aplicar operación entre registros y guardar resultado
          executeRegisterToRegisterAluOperation(mm.get(), ld, sourceRegName, targetRegName);
        } else {
          throw new UnsupportedOperationException("No se soporta operación entre referencias de memoria para " + ld.getClass());
        }

        return;
      }
      generateAluExecute(mm, ld, target, sourceRegName);
    }
    // Caso 2: source es una referencia de memoria pero target es un Register
    else if (target instanceof Register targetReg) {
      String targetRegName = targetReg.getName();
      if (ld instanceof Ld) {
        // LD reg, (mem): copiar de memoria a registro
        Variable address = resolveSourceMemoryAddress(mm.get(), source);
        if (address != null) {
          Variable memory = mm.get().field("memory");
          Variable value = mm.get().var(int.class);
          if (ld.getSource() instanceof Memory16BitReference) {
            value.set(memory.invoke("read16Bits", address));
          } else if (ld.getSource() instanceof IndirectMemory16BitReference) {
            value.set(memory.invoke("read16Bits", address));
          } else {
            value.set(memory.invoke("read", address, 0));
          }
          assignRegisterValue(mm.get(), targetRegName, value);
        }
      } else if (isAluOperation(ld)) {
        // Operaciones ALU desde memoria: A = A op (mem)
        executeAluOperationFromMemory(mm.get(), ld, source, targetRegName);
      }
    }
    // Caso 3: source es memoria y target también es memoria (LD (mem), (mem) - raro pero posible)
    else {
      if (ld instanceof Ld && source instanceof IndirectMemory8BitReference) {
        // Copiar de memoria a memoria: (target) = (source)
        Variable sourceAddr = resolveSourceMemoryAddress(mm.get(), source);
        if (sourceAddr != null && target instanceof IndirectMemory8BitReference targetMem) {
          Variable targetAddr = resolveIndirectMemoryAddress(mm.get(), targetMem);
          if (targetAddr != null) {
            Variable memory = mm.get().field("memory");
            Variable value = mm.get().var(int.class);
            value.set(memory.invoke("read", sourceAddr, 0));
            memory.invoke("write", targetAddr, value);
          }
        }
      } else {
        throw new UnsupportedOperationException("No se soporta operación entre referencias de memoria para " + ld.getClass());
      }
    }
  }

  /**
   * Resuelve la dirección de memoria desde una referencia
   */
  private Variable resolveSourceMemoryAddress(MethodMaker mm, ImmutableOpcodeReference source) {
    if (source instanceof IndirectMemory8BitReference indMem) {
      return resolveIndirectMemoryAddress(mm, indMem);
    } else if (source instanceof IndirectMemory16BitReference indMem16) {
      return resolveIndirectMemory16BitAddress(mm, indMem16);
    } else if (source instanceof MemoryPlusRegister8BitReference memRef) {
      MemoryPlusRegisterContext ctx = readOffsetAndCalculateAddress(mm, memRef);
      return ctx.address;
    } else if (source instanceof Memory8BitReference memory8BitReference) {
      return mm.field("PC").add(memory8BitReference.getDelta()).and(0xFFFF);
    } else if (source instanceof Memory16BitReference memory16BitReference) {
      return mm.field("PC").add(memory16BitReference.getDelta()).and(0xFFFF);
    }
    return null;
  }

  /**
   * Ejecuta operaciones ALU cuando el source es memoria: target = target op (memoria)
   */
  private void executeAluOperationFromMemory(MethodMaker mm, TargetSourceInstruction instruction,
                                             ImmutableOpcodeReference source, String targetRegName) {
    Variable memory = mm.field("memory");

    if (source instanceof IndirectMemory8BitReference indMem) {
      Variable address = resolveIndirectMemoryAddress(mm, indMem);
      Variable value = mm.var(int.class);
      value.set(memory.invoke("read", address, 0));
      executeAluWithMemoryValue(mm, instruction, targetRegName, value);
    } else if (source instanceof IndirectMemory16BitReference indMem16) {
      Variable address = resolveIndirectMemory16BitAddress(mm, indMem16);
      Variable value = mm.var(int.class);
      value.set(memory.invoke("read16Bits", address));
      executeAluWithMemoryValue(mm, instruction, targetRegName, value);
    } else if (source instanceof MemoryPlusRegister8BitReference memRef) {
      MemoryPlusRegisterContext ctx = readOffsetAndCalculateAddress(mm, memRef);
      Variable value = mm.var(int.class);
      value.set(ctx.memory.invoke("read", ctx.address, 0));
      executeAluWithMemoryValue(mm, instruction, targetRegName, value);
    } else if (source instanceof Memory8BitReference memory8BitReference) {
      Variable pcPlusDelta = mm.field("PC").add(memory8BitReference.getDelta()).and(0xFFFF);
      Variable dd = mm.var(int.class);
      dd.set(memory.invoke("read", pcPlusDelta, 0));
      executeAluWithMemoryValue(mm, instruction, targetRegName, dd);
    } else if (source instanceof Memory16BitReference memory16BitReference) {
      Variable pcPlusDelta = mm.field("PC").add(memory16BitReference.getDelta()).and(0xFFFF);
      Variable dd = mm.var(int.class);
      dd.set(memory.invoke("read16Bits", pcPlusDelta, 0));
      executeAluWithMemoryValue(mm, instruction, targetRegName, dd);
    }
  }

  /**
   * Ejecuta la operación ALU con un valor leído de memoria
   */
  private void executeAluWithMemoryValue(MethodMaker mm, TargetSourceInstruction instruction,
                                         String targetRegName, Variable memoryValue) {
    Variable targetValue = resolveRegisterValueByName(mm, targetRegName);

    if (isAluOperation(instruction)) {
      Class<?> aluOperationClass = getAluOperationClass(instruction);
      String fieldName = getAluOperationFieldName(instruction.getClass().getSimpleName());

      Variable aluOp = mm.var(aluOperationClass);
      aluOp.set(mm.field(fieldName));
      Variable flag = mm.field(FLAG);

      Variable result = mm.var(int.class);
      result.set(aluOp.invoke("execute2ValuesAndCarry", targetValue, memoryValue, flag));

      assignRegisterValue(mm, targetRegName, result);

      Variable flagField = mm.field(FLAG);
      flagField.set(aluOp.field(FLAG));
    }
  }

  /**
   * Asigna un valor a un registro, manejando registros de 16 bits compuestos (BC, DE, HL, AF)
   */
  private void assignRegisterValue(MethodMaker mm, String regName, Variable value) {
    if (is16BitCompositeRegister(regName)) {
      // Para registros de 16 bits compuestos, usar el setter correspondiente
      String setterMethodName = "set" + regName;  // setBC, setDE, setHL, setAF
      mm.invoke(setterMethodName, value);
    } else {
      // Para registros de 8 bits o especiales, asignar directamente
      mm.field(regName).set(value);
    }
  }

  /**
   * Verifica si es un registro de 16 bits compuesto que tiene getters/setters (BC, DE, HL, AF)
   */
  private boolean is16BitCompositeRegister(String regName) {
    return (regName.equals("BC") || regName.equals("DE") || regName.equals("HL") || regName.equals("AF"));
  }

  /**
   * Ejecuta operaciones ALU entre dos registros (register-to-register)
   */
  private void executeRegisterToRegisterAluOperation(MethodMaker mm, TargetSourceInstruction instruction,
                                                     String sourceRegName, String targetRegName) {
    // Verificar si es una operación Binary16BitsOperation
    if (instruction instanceof com.fpetrola.z80.instructions.impl.Binary16BitsOperation bin16) {
      executeBinary16BitsOperation(mm, bin16, sourceRegName, targetRegName);
      return;
    }

    Variable sourceValue = resolveRegisterValueByName(mm, sourceRegName);
    Variable targetValue = resolveRegisterValueByName(mm, targetRegName);
    Variable flag = mm.field(FLAG);

    // Obtener la clase específica de la tabla ALU
    Class<?> aluOperationClass = getAluOperationClass(instruction);
    String fieldName = getAluOperationFieldName(instruction.getClass().getSimpleName());

    // Guardar la operación ALU en una variable local con el tipo correcto
    Variable aluOp = mm.var(aluOperationClass);
    aluOp.set(mm.field(fieldName));

    // Ejecutar la operación ALU: execute2ValuesAndCarry(targetValue, sourceValue, flag)
    Variable result = mm.var(int.class);
    result.set(aluOp.invoke("execute2ValuesAndCarry", targetValue, sourceValue, flag));

    // Escribir el resultado de vuelta al registro destino
    assignRegisterValue(mm, targetRegName, result);

    // Actualizar el registro F con los flags de la operación ALU
    Variable flagField = mm.field(FLAG);
    flagField.set(aluOp.field(FLAG));
  }

  /**
   * Ejecuta una operación Binary16BitsOperation llamando directamente a calculateOriginal
   * sin usar compress/decompress.
   */
  private void executeBinary16BitsOperation(MethodMaker mm, com.fpetrola.z80.instructions.impl.Binary16BitsOperation instruction,
                                            String sourceRegName, String targetRegName) {
    Variable sourceValue = resolveRegisterValueByName(mm, sourceRegName);
    Variable targetValue = resolveRegisterValueByName(mm, targetRegName);
    Variable flag = mm.field(FLAG);

    // Obtener la clase específica de la tabla ALU
    Class<?> aluOperationClass = getAluOperationClass(instruction);
    String fieldName = getAluOperationFieldName(instruction.getClass().getSimpleName());

    // Guardar la operación ALU en una variable local con el tipo correcto
    Variable aluOp = mm.var(aluOperationClass);
    aluOp.set(mm.field(fieldName));

    // 1. Calcular el resultado directamente según el tipo de instrucción
    Variable result = mm.var(int.class);
    if (instruction instanceof com.fpetrola.z80.instructions.impl.Add16) {
      result.set(targetValue.add(sourceValue));
    } else if (instruction instanceof com.fpetrola.z80.instructions.impl.Adc16) {
      result.set(targetValue.add(sourceValue).add(flag.and(0x01)));
    } else if (instruction instanceof com.fpetrola.z80.instructions.impl.Sbc16) {
      result.set(targetValue.sub(sourceValue).sub(flag.and(0x01)));
    } else {
      throw new UnsupportedOperationException("Operación Binary16BitsOperation no soportada: " + instruction.getClass().getSimpleName());
    }

    // 2. Calcular resultNotZero: pasamos result & 0xFFFF directamente
    // El método calculateOriginal interpretará esto como: 0 si es cero, 1 si es no-cero
    Variable maskedResultValue = result.and(0xFFFF);

    // 3. Llamar a calculateOriginal directamente en el aluOp
    aluOp.invoke("calculateOriginal", targetValue, sourceValue, result, maskedResultValue);

    // 4. Escribir el resultado de vuelta al registro destino (16 bits)
    assignRegisterValue(mm, targetRegName, result.and(0xFFFF));

    // 5. Actualizar el registro F con los flags de la operación ALU
    Variable flagField = mm.field(FLAG);
    flagField.set(aluOp.field(FLAG));
  }

  /**
   * Genera código de ejecución para instrucciones ALU (LD, XOR, OR, etc.)
   * que leen un valor de memoria (o registro para LD), aplican la operación y escriben el resultado.
   */
  private void generateAluExecute(Supplier<MethodMaker> mm, TargetSourceInstruction instruction,
                                  OpcodeReference target, String sourceRegName) {
    if (target instanceof MemoryPlusRegister8BitReference memRef) {
      MemoryPlusRegisterContext ctx = readOffsetAndCalculateAddress(mm.get(), memRef);
      executeAluOperation(mm.get(), instruction, ctx.memory, ctx.address, sourceRegName);
    } else if (target instanceof IndirectMemory8BitReference indMem) {
      Variable address = resolveIndirectMemoryAddress(mm.get(), indMem);
      Variable memory = mm.get().field("memory");
      executeAluOperation(mm.get(), instruction, memory, address, sourceRegName);
    } else if (target instanceof IndirectMemory16BitReference indMem16) {
      Variable address = resolveIndirectMemory16BitAddress(mm.get(), indMem16);
      Variable memory = mm.get().field("memory");
      executeAluOperation16Bit(mm.get(), instruction, memory, address, sourceRegName);
    } else if (target instanceof Register targetReg) {
      // Fallback: si target es un Register (por ejemplo ADD A con target=A)
      String targetRegName = targetReg.getName();
      executeRegisterToRegisterAluOperation(mm.get(), instruction, sourceRegName, targetRegName);
    } else
      throw new UnsupportedOperationException("No se soporta operación entre referencias de memoria para " + instruction.getClass());

    // Si target es null o tipo desconocido, no generar código (es un no-op)
  }

  /**
   * Resuelve la dirección para IndirectMemory8BitReference
   */
  private Variable resolveIndirectMemoryAddress(MethodMaker mm, IndirectMemory8BitReference target) {
    ImmutableOpcodeReference innerTarget = target.getTarget();

    if (innerTarget instanceof Register reg) {
      return resolveRegisterValue(mm, reg);
    } else if (innerTarget instanceof Memory16BitReference mem16Ref) {
      return readAddress16Bit(mm, mem16Ref);
    }
    return null;
  }

  /**
   * Resuelve la dirección para IndirectMemory16BitReference
   */
  private Variable resolveIndirectMemory16BitAddress(MethodMaker mm, IndirectMemory16BitReference target) {
    ImmutableOpcodeReference innerTarget = target.getTarget();

    if (innerTarget instanceof Register reg) {
      return resolveRegisterValue(mm, reg);
    } else if (innerTarget instanceof Memory16BitReference mem16Ref) {
      return readAddress16Bit(mm, mem16Ref);
    }
    return null;
  }

  /**
   * Resuelve el valor de un registro, manejando registros de 16 bits construidos a partir de 8 bits
   */
  private Variable resolveRegisterValue(MethodMaker mm, Register reg) {
    String regName = reg.getName();
    return resolveRegisterValueByName(mm, regName);
  }

  /**
   * Resuelve el valor de un registro por su nombre, manejando registros de 16 bits usando los getters de UnrolledRegisterBank
   */
  private Variable resolveRegisterValueByName(MethodMaker mm, String regName) {
    // Si es un registro de 16 bits compuesto que tiene getters (BC, DE, HL, AF)
    if (is16BitCompositeRegister(regName)) {
      String getterMethodName = "get" + regName;  // getBC, getDE, getHL, getAF
      Variable result = mm.var(int.class);
      result.set(mm.invoke(getterMethodName));
      return result;
    }

    // Para otros registros (A, F, I, R, IX, IY, SP, PC, etc.), acceder directamente
    return mm.field(regName);
  }

  /**
   * Ejecuta una operación ALU: lee valor (de memoria o registro para LD), aplica operación y escribe resultado
   */
  private void executeAluOperation(MethodMaker mm, TargetSourceInstruction instruction,
                                   Variable memory, Variable address, String sourceRegName) {
    Variable source = resolveRegisterValueByName(mm, sourceRegName);

    // Para LD, escribir directamente sin variable intermedia
    if (instruction instanceof Ld) {
      memory.invoke("write", address, source);
      return;
    }

    // Si la instrucción tiene una operación ALU, usarla
    if (isAluOperation(instruction)) {
      executeWithAluOperation(mm, instruction, memory, address, sourceRegName);
      return;
    }

    // Para XOR/OR: leer, aplicar operación y escribir
    Variable value = mm.var(int.class);
    value.set(memory.invoke("read", address, 0));
    Variable result = mm.var(int.class);

    if (instruction instanceof Xor) {
      result.set(source.xor(value));
    } else if (instruction instanceof Or) {
      result.set(source.or(value));
    }

    // Escribir el resultado
    memory.invoke("write", address, result);
  }

  /**
   * Ejecuta una operación ALU para valores de 16 bits: lee valor (de memoria), aplica operación y escribe resultado
   */
  private void executeAluOperation16Bit(MethodMaker mm, TargetSourceInstruction instruction,
                                        Variable memory, Variable address, String sourceRegName) {
    Variable source = resolveRegisterValueByName(mm, sourceRegName);

    // Leer valor de 16 bits desde la dirección
//    Variable value = mm.var(int.class);
//    value.set(memory.invoke("read16Bits", address));

    Variable value= address;

    // Para LD, escribir directamente el valor leído
    if (instruction instanceof Ld) {
      memory.invoke("write16BitsReverse", source, value);
      return;
    }

    // Para XOR/OR: aplicar operación y escribir
    Variable result = mm.var(int.class);

    if (instruction instanceof Xor) {
      result.set(source.xor(value));
    } else if (instruction instanceof Or) {
      result.set(source.or(value));
    }

    // Escribir el resultado (16 bits)
    memory.invoke("write16BitsReverse", result, address);
  }

  private void executeWithAluOperation(MethodMaker mm, TargetSourceInstruction instruction,
                                       Variable memory, Variable address, String sourceRegName) {
    // Leer valor de memoria
    Variable value = mm.var(int.class);
    value.set(memory.invoke("read", address, 0));
    Variable source = resolveRegisterValueByName(mm, sourceRegName);

    // Obtener la operación ALU
    String fieldName = getAluOperationFieldName(instruction.getClass().getSimpleName());
    Variable aluOp = mm.field(fieldName);

    // Ejecutar la operación ALU: execute2ValuesAndCarry(value, source, flag)
    Variable result = mm.var(int.class);
    Variable flag = mm.field(FLAG);
    result.set(aluOp.invoke("execute2ValuesAndCarry", value, source, flag));

    // Escribir el resultado
    memory.invoke("write", address, result);
  }

  /**
   * Lee el byte offset (dd) desde memoria en (pc + valueDelta) y calcula la dirección
   * destino como (targetReg + dd) & 0xFFFF. Retorna el contexto con memoria y dirección.
   */
  private MemoryPlusRegisterContext readOffsetAndCalculateAddress(MethodMaker mm, MemoryPlusRegister8BitReference memRef) {
    Variable pcPlusDelta = mm.field("PC").add(memRef.getValueDelta()).and(0xFFFF);
    Variable dd = mm.var(int.class);
    Variable memory = mm.field("memory");
    dd.set(memory.invoke("read", pcPlusDelta, 0));

    // 2. Calcular dirección destino: (targetReg + dd) & 0xFFFF
    // Obtener el nombre del registro de forma genérica (puede ser IX, IY, etc.)
    ImmutableOpcodeReference target = memRef.getTarget();
    String registerName = getRegisterName(target);
    Variable targetReg = mm.field(registerName);
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
    // Buscar por reflection si la clase tiene un inner class que implemente AluOperation
    try {
      Class<?> instructionClass = instruction.getClass();
      for (Class<?> innerClass : instructionClass.getDeclaredClasses()) {
        if (AluOperation.class.isAssignableFrom(innerClass)) {
          return true;
        }
      }
    } catch (Exception e) {
      // Ignorar excepciones
    }
    return false;
  }

  private String getAluOperationFieldName(String instructionClassName) {
    // Mapeo especial para instrucciones que tienen names diferentes
    return switch(instructionClassName) {
      case "Inc" -> "inc8TableAluOperation";
      case "Dec" -> "dec8TableAluOperation";
      case "RRC" -> "rRCAluOperation";
      case "SRA" -> "sRAAluOperation";
      case "SLL" -> "sLLAluOperation";
      default -> instructionClassName.toLowerCase() + "TableAluOperation";
    };
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
   * Genera nombre para instrucciones unarias (Inc, Dec, etc.)
   */
  private String generateUnaryMethodName(ParameterizedUnaryAluInstruction instruction, String operationName) {
    StringBuilder methodName = new StringBuilder("execute").append(operationName);
    
    // Obtener el target mediante reflexión - buscar en la jerarquía de clases
    try {
      OpcodeReference target = getTargetFromUnaryInstruction(instruction);
      if (target != null) {
        methodName.append(getReferenceSuffix(target));
      }
    } catch (Exception e) {
      // Si no puede acceder al target por reflexión, usar nombre genérico
      System.err.println("Warning: No se pudo obtener target para " + operationName + ": " + e.getMessage());
    }
    
    return methodName.toString();
  }

  /**
   * Obtiene el target de una instrucción unaria buscando en la jerarquía de clases
   */
  private OpcodeReference getTargetFromUnaryInstruction(ParameterizedUnaryAluInstruction instruction) throws Exception {
    Class<?> clazz = instruction.getClass();
    
    // Buscar el campo 'target' en la jerarquía de clases
    while (clazz != null && clazz != Object.class) {
      try {
        java.lang.reflect.Field targetField = clazz.getDeclaredField("target");
        targetField.setAccessible(true);
        return (OpcodeReference) targetField.get(instruction);
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }
    
    return null;
  }

  /**
    * Genera código para instrucciones unarias (Inc, Dec, etc.)
    * target = operation(target)
    */
   private void generateUnaryExecute(MethodMaker mm, ParameterizedUnaryAluInstruction instruction) {
     try {
       // Obtener el target mediante reflexión
       OpcodeReference target = getTargetFromUnaryInstruction(instruction);
       if (target == null) {
         throw new RuntimeException("No se pudo obtener target de la instrucción unaria");
       }
       
       // Obtener la operación ALU mediante reflexión
       Class<?> aluOperationClass = getAluOperationClassFromUnaryInstruction(instruction);
       
       // Generar el código según el tipo de operando
       if (target instanceof Register targetReg) {
         String targetRegName = targetReg.getName();
         executeUnaryRegisterOperation(mm, instruction, targetRegName, aluOperationClass);
       } else if (target instanceof IndirectMemory8BitReference indMem) {
         Variable address = resolveIndirectMemoryAddress(mm, indMem);
         if (address == null) {
           throw new RuntimeException("No se pudo resolver dirección para IndirectMemory8BitReference en " + instruction.getClass().getSimpleName());
         }
         Variable memory = mm.field("memory");
         executeUnaryMemoryOperation(mm, instruction, memory, address, aluOperationClass);
       } else if (target instanceof MemoryPlusRegister8BitReference memRef) {
         MemoryPlusRegisterContext ctx = readOffsetAndCalculateAddress(mm, memRef);
         executeUnaryMemoryOperation(mm, instruction, ctx.memory, ctx.address, aluOperationClass);
       } else {
         throw new RuntimeException("Tipo de target no soportado para instrucción unaria: " + target.getClass().getSimpleName() + " en " + instruction.getClass().getSimpleName());
       }
     } catch (Exception e) {
       throw new RuntimeException("Error generando código para instrucción unaria (" + instruction.getClass().getSimpleName() + "): " + e.getMessage(), e);
     }
   }

  /**
   * Obtiene la clase de la operación ALU desde una instrucción unaria
   */
  private Class<?> getAluOperationClassFromUnaryInstruction(ParameterizedUnaryAluInstruction instruction) throws Exception {
    Class<?> clazz = instruction.getClass();
    
    // Buscar el campo 'aluOperation' en la jerarquía de clases
    while (clazz != null && clazz != Object.class) {
      try {
        java.lang.reflect.Field aluOpField = clazz.getDeclaredField("aluOperation");
        aluOpField.setAccessible(true);
        Object aluOp = aluOpField.get(instruction);
        return aluOp.getClass();
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      }
    }
    
    throw new RuntimeException("No se pudo obtener aluOperation");
  }

  /**
   * Ejecuta operación unaria en un registro: reg = alu.execute(reg, flag)
   */
  private void executeUnaryRegisterOperation(MethodMaker mm, ParameterizedUnaryAluInstruction instruction,
                                             String targetRegName, Class<?> aluOperationClass) {
    String operationName = instruction.getClass().getSimpleName();
    String fieldName = getAluOperationFieldName(operationName);
    
    Variable targetValue = resolveRegisterValueByName(mm, targetRegName);
    // Acceder al campo de la clase padre (Z80UnRolled) - es público
    Variable aluOp = mm.field(fieldName);
    Variable flag = mm.field(FLAG);
    
    // Ejecutar: result = aluOperation.execute2ValuesAndCarry(value, 0, flag)
    // Inc solo modifica el valor, ignora el segundo parámetro
    Variable result = mm.var(int.class);
    result.set(aluOp.invoke("execute2ValuesAndCarry", targetValue, 0, flag));
    
    assignRegisterValue(mm, targetRegName, result);
    
    // Actualizar flags
    Variable flagField = mm.field(FLAG);
    flagField.set(aluOp.field(FLAG));
  }

  /**
   * Ejecuta operación unaria en memoria: (addr) = alu.execute((addr), flag)
   */
  private void executeUnaryMemoryOperation(MethodMaker mm, ParameterizedUnaryAluInstruction instruction,
                                          Variable memory, Variable address, Class<?> aluOperationClass) {
    String operationName = instruction.getClass().getSimpleName();
    String fieldName = getAluOperationFieldName(operationName);
    
    Variable value = mm.var(int.class);
    value.set(memory.invoke("read", address, 0));
    
    Variable aluOp = mm.field(fieldName);
    Variable flag = mm.field(FLAG);
    
    // Ejecutar: result = aluOperation.execute2ValuesAndCarry(value, 0, flag)
    Variable result = mm.var(int.class);
    result.set(aluOp.invoke("execute2ValuesAndCarry", value, 0, flag));
    
    memory.invoke("write", address, result);
    
    // Actualizar flags
    Variable flagField = mm.field(FLAG);
    flagField.set(aluOp.field(FLAG));
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

  /**
   * Verifica si una instrucción no puede ser inlineada (ej: LdAI, LdAR)
   * Estas instrucciones usan registros especiales (I, R) que requieren lógica especial
   */
  private boolean isUnsupportedInstruction(Instruction instruction) {
    String className = instruction.getClass().getSimpleName();
    return className.equals("LdAI") || className.equals("LdAR");
  }
}
