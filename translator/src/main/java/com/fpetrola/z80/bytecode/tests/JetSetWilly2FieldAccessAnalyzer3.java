package com.fpetrola.z80.bytecode.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Simplified field access analyzer.
 * 
 * Core logic:
 * - For each field, store only the last write path: [A, B, C]
 * - When a field is read in path [X, Y, Z]:
 *   - If last write path differs from read path:
 *     - Find common ancestor
 *     - All methods from read path to ancestor: add as PARAMETER
 *     - All methods from last write path to ancestor: add as RETURN
 */
public class JetSetWilly2FieldAccessAnalyzer3 extends JetSetWilly2 {

  // Core: for each 8-bit field, track last write path
  private final Map<String, List<String>> fieldLastWritePath = new HashMap<>();
  
  // Call path stack (A -> B -> C)
  private final Deque<String> callPath = new LinkedList<>();
  
  // Result: parameters and returns per method
  private final Map<String, MethodFieldDeps> methodFieldDeps = new HashMap<>();
  
  // 8-bit registers
  private static final Set<String> EIGHT_BIT_REGISTERS = new HashSet<>(Arrays.asList(
      "A", "F", "B", "C", "D", "E", "H", "L"
  ));
  
  // Map 16-bit registers to their 8-bit components
  private static final Map<String, Set<String>> REGISTER_COMPONENTS = new HashMap<>();
  static {
    REGISTER_COMPONENTS.put("HL", new HashSet<>(Arrays.asList("H", "L")));
    REGISTER_COMPONENTS.put("BC", new HashSet<>(Arrays.asList("B", "C")));
    REGISTER_COMPONENTS.put("DE", new HashSet<>(Arrays.asList("D", "E")));
    REGISTER_COMPONENTS.put("AF", new HashSet<>(Arrays.asList("A", "F")));
    REGISTER_COMPONENTS.put("IX", new HashSet<>(Arrays.asList("IXH", "IXL")));
    REGISTER_COMPONENTS.put("IY", new HashSet<>(Arrays.asList("IYH", "IYL")));
    REGISTER_COMPONENTS.put("SP", new HashSet<>(Arrays.asList("SPH", "SPL")));
  }

  private boolean recordingEnabled = true;

  public JetSetWilly2FieldAccessAnalyzer3(MiniZXIO<WordNumber> rzxPlayerIO,
      Predicate<Integer> interruptionCondition) {
    super(rzxPlayerIO, interruptionCondition);
  }

  public JetSetWilly2FieldAccessAnalyzer3() {
    super();
  }

  public void enterMethod(String methodName) {
    callPath.push(methodName);
  }

  public void exitMethod() {
    if (!callPath.isEmpty()) {
      callPath.pop();
    }
  }

  private List<String> getCurrentPath() {
    return callPath.stream().collect(Collectors.toList());
  }

  private String getCurrentMethod() {
    return callPath.isEmpty() ? null : callPath.peek();
  }

  /**
   * Expand 16-bit register to its 8-bit components, or keep 8-bit as is
   */
  private Set<String> expand8BitFields(String fieldName) {
    Set<String> components = REGISTER_COMPONENTS.get(fieldName);
    if (components != null) {
      return new HashSet<>(components);
    }
    if (EIGHT_BIT_REGISTERS.contains(fieldName)) {
      return Set.of(fieldName);
    }
    return Set.of();
  }

  private void recordFieldWrite(String fieldName) {
    if (!recordingEnabled || callPath.isEmpty()) {
      return;
    }

    List<String> currentPath = getCurrentPath();
    Set<String> fields8bit = expand8BitFields(fieldName);
    
    for (String field : fields8bit) {
      fieldLastWritePath.put(field, new ArrayList<>(currentPath));
    }
  }

  private void recordFieldRead(String fieldName) {
    if (!recordingEnabled || callPath.isEmpty()) {
      return;
    }

    List<String> readPath = getCurrentPath();
    Set<String> fields8bit = expand8BitFields(fieldName);
    
    for (String field : fields8bit) {
      List<String> writePath = fieldLastWritePath.get(field);
      
      // If field was never written or written in a different path
      if (writePath == null || !pathsAreEqual(writePath, readPath)) {
        // Propagate parameters and returns through intermediate paths
        propagateDependencies(field, writePath, readPath);
      }
    }
  }

  /**
   * When a field is read in a different path than it was written,
   * propagate as parameter from read path upwards and as return from write path upwards
   */
  private void propagateDependencies(String field, List<String> writePath, List<String> readPath) {
    // Find common ancestor
    int commonDepth = getCommonAncestorDepth(writePath, readPath);
    
    // Add as PARAMETER to all methods from read path to common ancestor (exclusive)
    for (int i = readPath.size() - 1; i > commonDepth; i--) {
      String method = readPath.get(i);
      methodFieldDeps.computeIfAbsent(method, k -> new MethodFieldDeps(method))
          .addParameter(field);
    }
    
    // Add as RETURN to all methods from write path to common ancestor (exclusive)
    if (writePath != null) {
      for (int i = writePath.size() - 1; i > commonDepth; i--) {
        String method = writePath.get(i);
        methodFieldDeps.computeIfAbsent(method, k -> new MethodFieldDeps(method))
            .addReturn(field);
      }
    }
  }

  /**
   * Find the depth of the common ancestor between two paths
   * Returns -1 if no common ancestor (different root)
   * Returns 0 if paths diverge at root level
   */
  private int getCommonAncestorDepth(List<String> path1, List<String> path2) {
    if (path1 == null) {
      return path2.size() - 1; // All of path2 needs the field
    }
    
    int minLen = Math.min(path1.size(), path2.size());
    int commonDepth = -1;
    
    for (int i = 0; i < minLen; i++) {
      if (path1.get(i).equals(path2.get(i))) {
        commonDepth = i;
      } else {
        break;
      }
    }
    
    return commonDepth;
  }

  /**
   * Check if two paths are equal
   */
  private boolean pathsAreEqual(List<String> path1, List<String> path2) {
    if (path1.size() != path2.size()) {
      return false;
    }
    for (int i = 0; i < path1.size(); i++) {
      if (!path1.get(i).equals(path2.get(i))) {
        return false;
      }
    }
    return true;
  }

  // Override all 8-bit field accessors

  @Override
  public int A() {
    recordFieldRead("A");
    return super.A();
  }

  @Override
  public void A(int value) {
    recordFieldWrite("A");
    super.A(value);
  }

  @Override
  public int F() {
    recordFieldRead("F");
    return super.F();
  }

  @Override
  public void F(int value) {
    recordFieldWrite("F");
    super.F(value);
  }

  @Override
  public int B() {
    recordFieldRead("B");
    return super.B();
  }

  @Override
  public void B(int value) {
    recordFieldWrite("B");
    super.B(value);
  }

  @Override
  public int C() {
    recordFieldRead("C");
    return super.C();
  }

  @Override
  public void C(int value) {
    recordFieldWrite("C");
    super.C(value);
  }

  @Override
  public int D() {
    recordFieldRead("D");
    return super.D();
  }

  @Override
  public void D(int value) {
    recordFieldWrite("D");
    super.D(value);
  }

  @Override
  public int E() {
    recordFieldRead("E");
    return super.E();
  }

  @Override
  public void E(int value) {
    recordFieldWrite("E");
    super.E(value);
  }

  @Override
  public int H() {
    recordFieldRead("H");
    return super.H();
  }

  @Override
  public void H(int value) {
    recordFieldWrite("H");
    super.H(value);
  }

  @Override
  public int L() {
    recordFieldRead("L");
    return super.L();
  }

  @Override
  public void L(int value) {
    recordFieldWrite("L");
    super.L(value);
  }

  // 16-bit register accessors (expanded to 8-bit components)

  @Override
  public int AF() {
    recordFieldRead("AF");
    return super.AF();
  }

  @Override
  public void AF(int value) {
    recordFieldWrite("AF");
    super.AF(value);
  }

  @Override
  public int BC() {
    recordFieldRead("BC");
    return super.BC();
  }

  @Override
  public void BC(int value) {
    recordFieldWrite("BC");
    super.BC(value);
  }

  @Override
  public int DE() {
    recordFieldRead("DE");
    return super.DE();
  }

  @Override
  public void DE(int value) {
    recordFieldWrite("DE");
    super.DE(value);
  }

  @Override
  public int HL() {
    recordFieldRead("HL");
    return super.HL();
  }

  @Override
  public void HL(int value) {
    recordFieldWrite("HL");
    super.HL(value);
  }

  @Override
  public int IX() {
    recordFieldRead("IX");
    return super.IX();
  }

  @Override
  public void IX(int value) {
    recordFieldWrite("IX");
    super.IX(value);
  }

  @Override
  public int IY() {
    recordFieldRead("IY");
    return super.IY();
  }

  @Override
  public void IY(int value) {
    recordFieldWrite("IY");
    super.IY(value);
  }

  @Override
  public int SP() {
    recordFieldRead("SP");
    return super.SP();
  }

  @Override
  public void SP(int value) {
    recordFieldWrite("SP");
    super.SP(value);
  }

  public Map<String, Object> generateReport() {
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("title", "Field Access Analysis (Simplified Path-Based)");
    
    // Field state summary
    Map<String, Object> fieldSummary = new LinkedHashMap<>();
    for (Map.Entry<String, List<String>> entry : fieldLastWritePath.entrySet()) {
      Map<String, Object> fieldInfo = new LinkedHashMap<>();
      fieldInfo.put("lastWritePath", entry.getValue());
      fieldSummary.put(entry.getKey(), fieldInfo);
    }
    report.put("fieldStates", fieldSummary);
    
    // Method dependencies
    Map<String, Object> methodInfo = new LinkedHashMap<>();
    for (MethodFieldDeps methodDeps : methodFieldDeps.values()) {
      Map<String, Object> deps = new LinkedHashMap<>();
      deps.put("parameters", new ArrayList<>(methodDeps.parameters));
      deps.put("returns", new ArrayList<>(methodDeps.returns));
      methodInfo.put(methodDeps.methodName, deps);
    }
    report.put("methodDependencies", methodInfo);
    
    return report;
  }

  public void saveReport(String filePath) throws Exception {
    Map<String, Object> report = generateReport();
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.writeValue(new File(filePath), report);
    System.out.println("Report saved to: " + filePath);
  }

  // ============ Helper classes ============

  /**
   * Which fields are parameters/returns for a method
   */
  private static class MethodFieldDeps {
    final String methodName;
    final Set<String> parameters = new HashSet<>();
    final Set<String> returns = new HashSet<>();

    MethodFieldDeps(String methodName) {
      this.methodName = methodName;
    }

    void addParameter(String fieldName) {
      parameters.add(fieldName);
    }

    void addReturn(String fieldName) {
      returns.add(fieldName);
    }
  }
}
