package com.fpetrola.z80.bytecode.tests.aa;

import java.util.*;

public class Z80Registers {
  private SmartRegister16 hl = new SmartRegister16("HL");
  private SmartRegister16 de = new SmartRegister16("DE");
  private SmartRegister16 bc = new SmartRegister16("BC");
  private SmartRegister16 af = new SmartRegister16("AF");
  private SmartRegister16 ix = new SmartRegister16("IX");
  private SmartRegister16 iy = new SmartRegister16("IY");

  private static int currentPC = 0;

  private static Map<Integer, Access> accesses = new HashMap<>();
  private static Map<String, Map<Integer, CountMode>> modesAtPC = new HashMap<>();

  static {
    modesAtPC.put("HL", new HashMap<>());
    modesAtPC.put("DE", new HashMap<>());
    modesAtPC.put("BC", new HashMap<>());
    modesAtPC.put("AF", new HashMap<>());
    modesAtPC.put("IX", new HashMap<>());
    modesAtPC.put("IY", new HashMap<>());
  }

  public void pc(int dir) {
    currentPC = dir;
//    recordMode("HL", dir, hl.getCurrentMode());
//    recordMode("DE", dir, de.getCurrentMode());
//    recordMode("BC", dir, bc.getCurrentMode());
//    recordMode("AF", dir, af.getCurrentMode());
//    recordMode("IX", dir, ix.getCurrentMode());
//    recordMode("IY", dir, iy.getCurrentMode());
  }

  public static void recordMode(String reg, int pc, Mode mode) {
    Map<Integer, CountMode> map = modesAtPC.get(reg);
    CountMode cm = map.computeIfAbsent(pc, k -> new CountMode());
    if (mode == Mode.MODE_16) {
      cm.count16++;
    } else {
      cm.count8++;
    }
  }

  public static void recordAccess(String reg, int pc, String op, boolean conv, String type) {
    if (conv) {
      accesses.computeIfAbsent(pc, (f) -> new Access(pc, reg, op, conv, type));
    }
  }

  public static int getCurrentPC() {
    return currentPC;
  }

  public void AF(int val) {
    af.set16(val);
  }

  public int AF() {
    return af.get16();
  }

  public void A(int val) {
    af.setHigh(val);
  }

  public int A() {
    return af.getHigh();
  }

  public void F(int val) {
    af.setLow(val);
  }

  public int F() {
    return af.getLow();
  }

  public void BC(int val) {
    bc.set16(val);
  }

  public int BC() {
    return bc.get16();
  }

  public void B(int val) {
    bc.setHigh(val);
  }

  public int B() {
    return bc.getHigh();
  }

  public void C(int val) {
    bc.setLow(val);
  }

  public int C() {
    return bc.getLow();
  }

  public void DE(int val) {
    de.set16(val);
  }

  public int DE() {
    return de.get16();
  }

  public void D(int val) {
    de.setHigh(val);
  }

  public int D() {
    return de.getHigh();
  }

  public void E(int val) {
    de.setLow(val);
  }

  public int E() {
    return de.getLow();
  }

  public void HL(int val) {
    hl.set16(val);
  }

  public int HL() {
    return hl.get16();
  }

  public void H(int val) {
    hl.setHigh(val);
  }

  public int H() {
    return hl.getHigh();
  }

  public void L(int val) {
    hl.setLow(val);
  }

  public int L() {
    return hl.getLow();
  }

  public void IX(int val) {
    ix.set16(val);
  }

  public int IX() {
    return ix.get16();
  }

  public void IXH(int val) {
    ix.setHigh(val);
  }

  public int IXH() {
    return ix.getHigh();
  }

  public void IXL(int val) {
    ix.setLow(val);
  }

  public int IXL() {
    return ix.getLow();
  }

  public void IY(int val) {
    iy.set16(val);
  }

  public int IY() {
    return iy.get16();
  }

  public void IYH(int val) {
    iy.setHigh(val);
  }

  public int IYH() {
    return iy.getHigh();
  }

  public void IYL(int val) {
    iy.setLow(val);
  }

  public int IYL() {
    return iy.getLow();
  }


  public static String generarReporte() {

    clearModesAtPC();
    StringBuilder json = new StringBuilder("{");
        extracted(json);
    boolean firstReg;
    // Recomendaciones
    json.append("\"recomendaciones\": [");
    boolean firstRec = true;
    for (String reg : modesAtPC.keySet()) {
      Map<Integer, CountMode> m = modesAtPC.get(reg);
      for (Map.Entry<Integer, CountMode> e : m.entrySet()) {
        CountMode cm = e.getValue();
        if (cm.count16 > 0 && cm.count8 > 0) {
          if (!firstRec) json.append(",");
          firstRec = false;
          int total = cm.count16 + cm.count8;
          String modoRecom = cm.count16 > cm.count8 ? "16bits" : "8bits";
          json.append("{");
          json.append("\"tipo\":\"normalizar\",");
          json.append("\"descripcion\":\"Normalizar el registro " + reg + " a modo " + modoRecom + " antes de llegar al PC " + e.getKey() + "\"");
          json.append("}");
        }
      }
    }
        extracted(firstRec, json);
    return json.toString();
  }

  private static void clearModesAtPC() {
    Set<String> strings = modesAtPC.keySet();
    for (String s : strings) {
      clearSimple(modesAtPC.get(s));
    }
  }

  private static void clearSimple(Map<Integer, CountMode> integerCountModeMap) {
    Set<Integer> integers = integerCountModeMap.keySet();
    for (Iterator<Integer> iterator = integers.iterator(); iterator.hasNext(); ) {
      Integer i = iterator.next();
      CountMode cm = integerCountModeMap.get(i);
      if (cm.count16 == 0 || cm.count8 == 0) {
        iterator.remove();
      }
    }
  }

  private static void extracted(boolean firstRec, StringBuilder json) {
    boolean firstReg;
    // Ping-pong
    Map<String, List<Access>> perReg = new HashMap<>();
    for (String reg : modesAtPC.keySet()) {
      perReg.put(reg, new ArrayList<>());
    }
    for (Access a : accesses.values()) {
      perReg.get(a.reg).add(a);
    }
    for (String reg : perReg.keySet()) {
      List<Access> list = perReg.get(reg);
      for (int i = 1; i < list.size(); i++) {
        Access prev = list.get(i - 1);
        Access curr = list.get(i);
        if (prev.conversion && curr.conversion && prev.pc != curr.pc) {
          if (("16to8".equals(prev.type) && "8to16".equals(curr.type)) || ("8to16".equals(prev.type) && "16to8".equals(curr.type))) {
            if (!firstRec) json.append(",");
            firstRec = false;
            json.append("{");
            json.append("\"tipo\":\"pingpong\",");
            json.append("\"descripcion\":\"Detectada conversión ping-pong en registro " + reg + " entre PC " + prev.pc + " y " + curr.pc + "\"");
            json.append("}");
          }
        }
      }
    }
    json.append("],");
    // Regiones exclusivas
    json.append("\"regiones_exclusivas\": {");
    firstReg = true;
    for (String reg : modesAtPC.keySet()) {
      if (!firstReg) json.append(",");
      firstReg = false;
      json.append("\"").append(reg).append("\": [");
      Map<Integer, CountMode> m = modesAtPC.get(reg);
      List<Integer> pcs = new ArrayList<>(m.keySet());
      if (pcs.isEmpty()) {
        json.append("]");
        continue;
      }
      pcs.sort(Integer::compareTo);
      int start = pcs.get(0);
      String currentExclusive = getExclusiveMode(m.get(start));
      boolean firstRegion = true;
      for (int i = 1; i < pcs.size(); i++) {
        int thisPC = pcs.get(i);
        String thisEx = getExclusiveMode(m.get(thisPC));
        if (thisEx == null || !thisEx.equals(currentExclusive) || thisPC != pcs.get(i - 1) + 1) {
          if (currentExclusive != null) {
            if (!firstRegion) json.append(",");
            firstRegion = false;
            json.append("{");
            json.append("\"inicio\":").append(start).append(",");
            json.append("\"fin\":").append(pcs.get(i - 1)).append(",");
            json.append("\"modo\":\"").append(currentExclusive).append("\"");
            json.append("}");
          }
          start = thisPC;
          currentExclusive = thisEx;
        }
      }
      if (currentExclusive != null) {
        if (!firstRegion) json.append(",");
        json.append("{");
        json.append("\"inicio\":").append(start).append(",");
        json.append("\"fin\":").append(pcs.get(pcs.size() - 1)).append(",");
        json.append("\"modo\":\"").append(currentExclusive).append("\"");
        json.append("}");
      }
      json.append("]");
    }
    json.append("}");
    json.append("}");
  }

  private static void extracted(StringBuilder json) {
    // Resumen
    json.append("\"resumen\": {");
    json.append("\"total_accesos\": ").append(accesses.size()).append(",");
    int totalConv = 0;
    for (Access a : accesses.values()) {
      if (a.conversion) totalConv++;
    }
    json.append("\"total_conversiones\": ").append(totalConv).append(",");
    int totalMixed = 0;
    for (Map<Integer, CountMode> m : modesAtPC.values()) {
      for (CountMode cm : m.values()) {
        if (cm.count16 > 0 && cm.count8 > 0) totalMixed++;
      }
    }
    json.append("\"pcs_con_estado_mixto\": ").append(totalMixed);
    json.append("},");
    // Historical
//        json.append("\"historical_accesos\": [");
//        for (int i = 0; i < accesses.size(); i++) {
//            Access a = accesses.get(i);
//            json.append("{");
//            json.append("\"pc\":").append(a.pc).append(",");
//            json.append("\"registro\":\"").append(a.reg).append("\",");
//            json.append("\"operacion\":\"").append(a.op).append("\",");
//            json.append("\"conversion\":").append(a.conversion).append(",");
//            json.append("\"tipo_conversion\":").append(a.type != null ? "\"" + a.type + "\"" : "null");
//            json.append("}");
//            if (i < accesses.size() - 1) json.append(",");
//        }
//        json.append("],");
    // Estados
    json.append("\"estados\": {");
    boolean firstReg = true;
    for (String reg : modesAtPC.keySet()) {
      if (!firstReg) json.append(",");
      firstReg = false;
      json.append("\"").append(reg).append("\": {");
      Map<Integer, CountMode> m = modesAtPC.get(reg);
      List<Integer> pcs = new ArrayList<>(m.keySet());
      pcs.sort(Integer::compareTo);
      boolean firstPC = true;
      for (int p : pcs) {
        if (!firstPC) json.append(",");
        firstPC = false;
        CountMode cm = m.get(p);
        int total = cm.count16 + cm.count8;
        json.append("\"").append(p).append("\": {");
        json.append("\"count16\":").append(cm.count16).append(",");
        json.append("\"count8\":").append(cm.count8);
        if (cm.count16 > 0 && cm.count8 > 0) {
          double perc16 = (double) cm.count16 / total * 100;
          json.append(",\"porc16\":").append(perc16).append(",");
          json.append(",\"porc8\":").append((double) cm.count8 / total * 100);
        }
        json.append("}");
      }
      json.append("}");
    }
    json.append("},");
    // Conversiones
    Map<String, Map<Integer, Integer>> convPerPCPerReg = new HashMap<>();
    for (String reg : modesAtPC.keySet()) {
      convPerPCPerReg.put(reg, new HashMap<>());
    }
    for (Access a : accesses.values()) {
      if (a.conversion) {
        Map<Integer, Integer> m = convPerPCPerReg.get(a.reg);
        m.put(a.pc, m.getOrDefault(a.pc, 0) + 1);
      }
    }
    json.append("\"conversiones\": {");
    firstReg = true;
    for (String reg : convPerPCPerReg.keySet()) {
      if (!firstReg) json.append(",");
      firstReg = false;
      json.append("\"").append(reg).append("\": {");
      Map<Integer, Integer> m = convPerPCPerReg.get(reg);
      List<Integer> pcs = new ArrayList<>(m.keySet());
      pcs.sort(Integer::compareTo);
      boolean first = true;
      for (int p : pcs) {
        if (!first) json.append(",");
        first = false;
        json.append("\"").append(p).append("\":").append(m.get(p));
      }
      json.append("}");
    }
    json.append("},");
  }

  private static String getExclusiveMode(CountMode cm) {
    if (cm.count16 > 0 && cm.count8 == 0) return "16bits";
    if (cm.count8 > 0 && cm.count16 == 0) return "8bits";
    return null;
  }

  public static void limpiarEstadisticas() {
    accesses.clear();
    for (Map<Integer, CountMode> m : modesAtPC.values()) {
      m.clear();
    }
  }

  public static class CountMode {
    int count16 = 0;
    int count8 = 0;
  }

  public static class Access {
    @Override
    public boolean equals(Object object) {
      if (object == null || getClass() != object.getClass()) return false;
      Access access = (Access) object;
      return pc == access.pc && conversion == access.conversion && Objects.equals(reg, access.reg) && Objects.equals(op, access.op) && Objects.equals(type, access.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(pc, reg, op, conversion, type);
    }

    int pc;
    String reg;
    String op;
    boolean conversion;
    String type;

    public Access(int pc, String reg, String op, boolean conversion, String type) {
      this.pc = pc;
      this.reg = reg;
      this.op = op;
      this.conversion = conversion;
      this.type = type;
    }
  }
}
