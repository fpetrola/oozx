package com.fpetrola.z80.bytecode.tests.aa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Z80Registers {
    private SmartRegister hl = new SmartRegister("HL");
    private SmartRegister de = new SmartRegister("DE");
    private SmartRegister bc = new SmartRegister("BC");
    private SmartRegister af = new SmartRegister("AF");

    private static int currentPC = 0;

    private static List<Access> accesses = new ArrayList<>();
    private static Map<String, Map<Integer, CountMode>> modesAtPC = new HashMap<>();

    static {
        modesAtPC.put("HL", new HashMap<>());
        modesAtPC.put("DE", new HashMap<>());
        modesAtPC.put("BC", new HashMap<>());
        modesAtPC.put("AF", new HashMap<>());
    }

    public void pc(int dir) {
        currentPC = dir;
        recordMode("HL", dir, hl.getCurrentMode());
        recordMode("DE", dir, de.getCurrentMode());
        recordMode("BC", dir, bc.getCurrentMode());
        recordMode("AF", dir, af.getCurrentMode());
    }

    private static void recordMode(String reg, int pc, Mode mode) {
        Map<Integer, CountMode> map = modesAtPC.get(reg);
        CountMode cm = map.computeIfAbsent(pc, k -> new CountMode());
        if (mode == Mode.MODE_16) {
            cm.count16++;
        } else {
            cm.count8++;
        }
    }

    public static void recordAccess(String reg, int pc, String op, boolean conv, String type) {
        accesses.add(new Access(pc, reg, op, conv, type));
    }

    public static int getCurrentPC() {
        return currentPC;
    }

    // Métodos convenientes
    public void setHL(int val) { hl.set16(val); }
    public int getHL() { return hl.get16(); }
    public void setH(int val) { hl.setHigh(val); }
    public int getH() { return hl.getHigh(); }
    public void setL(int val) { hl.setLow(val); }
    public int getL() { return hl.getLow(); }

    public void setDE(int val) { de.set16(val); }
    public int getDE() { return de.get16(); }
    public void setD(int val) { de.setHigh(val); }
    public int getD() { return de.getHigh(); }
    public void setE(int val) { de.setLow(val); }
    public int getE() { return de.getLow(); }

    public void setBC(int val) { bc.set16(val); }
    public int getBC() { return bc.get16(); }
    public void setB(int val) { bc.setHigh(val); }
    public int getB() { return bc.getHigh(); }
    public void setC(int val) { bc.setLow(val); }
    public int getC() { return bc.getLow(); }

    public void setAF(int val) { af.set16(val); }
    public int getAF() { return af.get16(); }
    public void setA(int val) { af.setHigh(val); }
    public int getA() { return af.getHigh(); }
    public void setF(int val) { af.setLow(val); }
    public int getF() { return af.getLow(); }

    public static String generarReporte() {
        StringBuilder json = new StringBuilder("{");
        // Resumen
        json.append("\"resumen\": {");
        json.append("\"total_accesos\": ").append(accesses.size()).append(",");
        int totalConv = 0;
        for (Access a : accesses) {
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
        json.append("\"historical_accesos\": [");
        for (int i = 0; i < accesses.size(); i++) {
            Access a = accesses.get(i);
            json.append("{");
            json.append("\"pc\":").append(a.pc).append(",");
            json.append("\"registro\":\"").append(a.reg).append("\",");
            json.append("\"operacion\":\"").append(a.op).append("\",");
            json.append("\"conversion\":").append(a.conversion).append(",");
            json.append("\"tipo_conversion\":").append(a.type != null ? "\"" + a.type + "\"" : "null");
            json.append("}");
            if (i < accesses.size() - 1) json.append(",");
        }
        json.append("],");
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
        for (Access a : accesses) {
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
        // Ping-pong
        Map<String, List<Access>> perReg = new HashMap<>();
        for (String reg : modesAtPC.keySet()) {
            perReg.put(reg, new ArrayList<>());
        }
        for (Access a : accesses) {
            perReg.get(a.reg).add(a);
        }
        for (String reg : perReg.keySet()) {
            List<Access> list = perReg.get(reg);
            for (int i = 1; i < list.size(); i++) {
                Access prev = list.get(i - 1);
                Access curr = list.get(i);
                if (prev.conversion && curr.conversion && prev.pc != curr.pc) {
                    if (( "16to8".equals(prev.type) && "8to16".equals(curr.type) ) || ( "8to16".equals(prev.type) && "16to8".equals(curr.type) )) {
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
        return json.toString();
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
