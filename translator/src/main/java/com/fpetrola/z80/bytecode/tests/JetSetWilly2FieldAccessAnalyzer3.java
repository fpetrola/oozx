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
 * Simplified field access analyzer using path-based logic.
 * 
 * Core idea:
 * - Track call path: A -> B -> C
 * - For each 8-bit field, record only: lastWritePath, lastWriteMethod
 * - When a field is read:
 *   - If lastWritePath is NOT an ancestor of currentPath: PARAMETER (passed from caller)
 *   - If lastWritePath IS an ancestor but different from currentPath: PARAMETER
 * - When returning from a method, if a field was modified and is used by parent: RETURN
 */
public class JetSetWilly2FieldAccessAnalyzer3 extends JetSetWilly2 {

  // Core data: for each 8-bit field, track last write location
  private final Map<String, FieldLastWrite> fieldStates = new HashMap<>();
  
  // Call path stack (A -> B -> C)
  private final Deque<String> callPath = new LinkedList<>();
  
  // Result: which fields are parameters and which are returns PER METHOD
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
   * Check if pathA is ancestor of pathB (pathB extends pathA)
   */
  private boolean isAncestorPath(List<String> pathA, List<String> pathB) {
    if (pathA.size() > pathB.size()) return false;
    for (int i = 0; i < pathA.size(); i++) {
      if (!pathA.get(i).equals(pathB.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Expand 16-bit register to its 8-bit components, or keep 8-bit as is
   */
  private Set<String> expand8BitFields(String fieldName) {
    Set<String> components = REGISTER_COMPONENTS.get(fieldName);
    if (components != null) {
      return new HashSet<>(components);
    }
    // If it's an 8-bit register or not a standard register, keep as is
    if (EIGHT_BIT_REGISTERS.contains(fieldName)) {
      return Set.of(fieldName);
    }
    // Ignore unknown registers
    return Set.of();
  }

  private void recordFieldWrite(String fieldName) {
    if (!recordingEnabled || callPath.isEmpty()) {
      return;
    }

    List<String> currentPath = getCurrentPath();
    String currentMethod = getCurrentMethod();
    
    // Expand to 8-bit fields and record write for each
    Set<String> fields8bit = expand8BitFields(fieldName);
    for (String field : fields8bit) {
      fieldStates.put(field, new FieldLastWrite(currentPath, currentMethod));
    }
  }

  private void recordFieldRead(String fieldName) {
    if (!recordingEnabled || callPath.isEmpty()) {
      return;
    }

    List<String> currentPath = getCurrentPath();
    String currentMethod = getCurrentMethod();
    
    // Expand to 8-bit fields and check each
    Set<String> fields8bit = expand8BitFields(fieldName);
    for (String field : fields8bit) {
      FieldLastWrite lastWrite = fieldStates.get(field);
      
      if (lastWrite == null) {
        // Field never written, must be parameter
        methodFieldDeps.computeIfAbsent(currentMethod, k -> new MethodFieldDeps(currentMethod))
            .addParameter(field);
      } else if (!isAncestorPath(lastWrite.path, currentPath)) {
        // Last write is in a DIFFERENT branch, must be parameter
        methodFieldDeps.computeIfAbsent(currentMethod, k -> new MethodFieldDeps(currentMethod))
            .addParameter(field);
      }
      // else: last write is in current or ancestor path, no parameter needed
    }
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

  /**
   * Analyze return values: if a field was modified in method M and then read by caller
   */
  public void analyzeReturns() {
    // For each field, check if it was written and then read in ancestor paths
    for (Map.Entry<String, FieldLastWrite> entry : fieldStates.entrySet()) {
      String fieldName = entry.getKey();
      FieldLastWrite lastWrite = entry.getValue();
      
      // For each method, check if it reads this field after it was modified deeper
      for (MethodFieldDeps methodDeps : methodFieldDeps.values()) {
        List<String> methodPath = methodDeps.getPath();
        
        // If this method is an ancestor of lastWrite path, it reads after modification
        if (isAncestorPath(methodPath, lastWrite.path) && 
            !methodPath.equals(lastWrite.path)) {
          // The method that did the writing should return this field
          MethodFieldDeps writingMethod = methodFieldDeps.get(lastWrite.methodName);
          if (writingMethod != null) {
            writingMethod.addReturn(fieldName);
          }
        }
      }
    }
  }

  public Map<String, Object> generateReport() {
    analyzeReturns();

    Map<String, Object> report = new LinkedHashMap<>();
    report.put("title", "Field Access Analysis (8-bit Path-Based)");
    
    // Field state summary
    Map<String, Object> fieldSummary = new LinkedHashMap<>();
    for (Map.Entry<String, FieldLastWrite> entry : fieldStates.entrySet()) {
      Map<String, Object> fieldInfo = new LinkedHashMap<>();
      fieldInfo.put("lastWritePath", entry.getValue().path);
      fieldInfo.put("lastWriteMethod", entry.getValue().methodName);
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
   * Last write location of a field
   */
  private static class FieldLastWrite {
    final List<String> path;  // e.g., [A, B, C]
    final String methodName;

    FieldLastWrite(List<String> path, String methodName) {
      this.path = new ArrayList<>(path);
      this.methodName = methodName;
    }
  }

  /**
   * Which fields are parameters/returns for a method
   */
  private static class MethodFieldDeps {
    final String methodName;
    final Set<String> parameters = new HashSet<>();
    final Set<String> returns = new HashSet<>();
    final List<String> path = new ArrayList<>();

    MethodFieldDeps(String methodName) {
      this.methodName = methodName;
      this.path.add(methodName);  // Single-element path for now
    }

    void addParameter(String fieldName) {
      parameters.add(fieldName);
    }

    void addReturn(String fieldName) {
      returns.add(fieldName);
    }

    List<String> getPath() {
      return path;
    }
  }
}
