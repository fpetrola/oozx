package com.fpetrola.oozx.inliner;

import org.cojen.maker.ClassMaker;
import org.cojen.maker.Label;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Genera métodos dispatch que despachan opcodes a métodos específicos.
 * Maneja tanto dispatch simple (execute method) como dispatch para prefijos (CB, DD, FD).
 */
public class DispatchMethodGenerator {

  /**
   * Agrega un método execute(int opcode) que despacha a los métodos específicos usando opcodes reales
   */
  public void addDispatchMethodWithOpcodes(ClassMaker cm, Map<Integer, String> opcodeToMethodName, Map<Integer, String> prefixOpcodes) {
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

        Variable nextOpcode = mm.var(int.class);
        nextOpcode.set(memory.invoke("read", pc, 1));

        // Incrementar PC en 1 para apuntar al siguiente byte (después del prefijo)
        Variable nextPc = mm.var(int.class);
        nextPc.set(pc.add(1).and(0xFFFF));
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
   * Agrega un método dispatch para un prefijo que despacha por el siguiente opcode
   */
  public void addPrefixDispatchMethod(ClassMaker cm, String methodName, Map<Integer, String> opcodeToMethodName) {
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
   * Genera el nombre del método dispatch para un prefijo
   */
  public String generatePrefixDispatchMethodName(Integer prefixOpcode) {
    String prefixName = switch(prefixOpcode & 0xFF) {
      case 0xCB -> "CB";
      case 0xDD -> "DD";
      case 0xFD -> "FD";
      default -> String.format("Prefix%02X", prefixOpcode & 0xFF);
    };
    return "execute" + prefixName + "Prefix";
  }
}
