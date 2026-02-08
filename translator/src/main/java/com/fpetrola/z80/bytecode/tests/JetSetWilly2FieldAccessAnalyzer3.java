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
 * Simplified field access analyzer that tracks:
 * - Current call path (stack of methods being executed)
 * - For each field: last write operation (path, location) and last read operation (path, location)
 * 
 * Analysis logic:
 * - If a field is read in method A, but was last written in an ancestor method or different path,
 *   then it must be passed as a parameter to A
 * - If a field is written in method A, but is read later in an ancestor method,
 *   then it must be returned from A
 */
public class JetSetWilly2FieldAccessAnalyzer3 extends JetSetWilly2 {

  private final Map<String, FieldStateInfo> fieldStates = new HashMap<>();
  private final Map<String, MethodFieldDependencies> methodDependencies = new HashMap<>();
  private final Deque<String> callPath = new LinkedList<>();
  private final Set<String> allFields = new HashSet<>(Arrays.asList(
      "A", "F", "B", "C", "D", "E", "H", "L", "IXH", "IXL", "IYH", "IYL",
      "AF", "BC", "DE", "HL", "IX", "IY", "SP"
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
  }

  private boolean recordingEnabled = true;

  public JetSetWilly2FieldAccessAnalyzer3(MiniZXIO<WordNumber> rzxPlayerIO,
      Predicate<Integer> interruptionCondition) {
    super(rzxPlayerIO, interruptionCondition);
  }

  public JetSetWilly2FieldAccessAnalyzer3() {
    super();
  }

  /**
   * Records that we entered a method
   */
  public void enterMethod(String methodName) {
    callPath.push(methodName);
  }

  /**
   * Records that we exited a method
   */
  public void exitMethod() {
    if (!callPath.isEmpty()) {
      callPath.pop();
    }
  }

  /**
   * Get the current call path as a list (root to leaf)
   */
  private List<String> getCurrentPath() {
    return new ArrayList<>(callPath.stream().collect(Collectors.toList()));
  }

  /**
   * Record a write operation on a field
   */
  private void recordFieldWrite(String fieldName) {
    if (!recordingEnabled || callPath.isEmpty()) {
      return;
    }

    String currentMethod = callPath.peek();
    List<String> path = getCurrentPath();
    
    FieldStateInfo state = fieldStates.computeIfAbsent(fieldName, k -> new FieldStateInfo(fieldName));
    state.lastWritePath = new ArrayList<>(path);
    state.lastWriteMethod = currentMethod;
    state.lastAccessType = AccessType.WRITE;

    // Record dependency in current method
    MethodFieldDependencies methodDeps = methodDependencies.computeIfAbsent(currentMethod,
        k -> new MethodFieldDependencies(currentMethod));
    methodDeps.recordWrite(fieldName);
  }

  /**
   * Record a read operation on a field
   */
  private void recordFieldRead(String fieldName) {
    if (!recordingEnabled || callPath.isEmpty()) {
      return;
    }

    String currentMethod = callPath.peek();
    List<String> path = getCurrentPath();
    
    FieldStateInfo state = fieldStates.computeIfAbsent(fieldName, k -> new FieldStateInfo(fieldName));
    
    // Check if this field was last written in a different method context
    if (state.lastWritePath != null && !isPathAncestorOf(state.lastWritePath, path)) {
      // Field was modified in a different branch, must be passed as parameter
      state.needsAsParameter = true;
    }
    
    state.lastReadPath = new ArrayList<>(path);
    state.lastReadMethod = currentMethod;
    state.lastAccessType = AccessType.READ;

    // Record dependency in current method
    MethodFieldDependencies methodDeps = methodDependencies.computeIfAbsent(currentMethod,
        k -> new MethodFieldDependencies(currentMethod));
    methodDeps.recordRead(fieldName);
  }

  /**
   * Check if pathA is an ancestor of pathB
   * e.g., [main] is ancestor of [main, methodA, methodB]
   */
  private boolean isPathAncestorOf(List<String> pathA, List<String> pathB) {
    if (pathA.size() > pathB.size()) return false;
    for (int i = 0; i < pathA.size(); i++) {
      if (!pathA.get(i).equals(pathB.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Analyze dependencies after recording is complete
   * Determines which fields need to be parameters and which need to be returned
   */
  public void analyzeDependencies() {
    for (FieldStateInfo fieldState : fieldStates.values()) {
      String fieldName = fieldState.fieldName;
      
      // For each method that uses this field
      for (MethodFieldDependencies methodDeps : methodDependencies.values()) {
        if (methodDeps.readsField(fieldName)) {
          // This method reads the field
          // Check if the field was last written in an ancestor method
          if (fieldState.lastWritePath != null && 
              !isPathAncestorOf(fieldState.lastWritePath, methodDeps.getPath())) {
            // Field needs to be passed as parameter
            methodDeps.addRequiredParameter(fieldName);
          }
        }
        
        if (methodDeps.writesField(fieldName)) {
          // This method writes the field
          // Check if it's read in a calling method (ancestor in path)
          List<String> methodPath = methodDeps.getPath();
          if (fieldState.lastReadPath != null && 
              isPathAncestorOf(methodPath, fieldState.lastReadPath)) {
            // Field is read by a caller, must return it
            methodDeps.addReturnValue(fieldName);
          }
        }
      }
    }
  }

  // Override all field accessors to capture reads/writes

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
  public int IXH() {
    recordFieldRead("IXH");
    return super.IXH();
  }

  @Override
  public void IXH(int value) {
    recordFieldWrite("IXH");
    super.IXH(value);
  }

  @Override
  public int IXL() {
    recordFieldRead("IXL");
    return super.IXL();
  }

  @Override
  public void IXL(int value) {
    recordFieldWrite("IXL");
    super.IXL(value);
  }

  @Override
  public int IYH() {
    recordFieldRead("IYH");
    return super.IYH();
  }

  @Override
  public void IYH(int value) {
    recordFieldWrite("IYH");
    super.IYH(value);
  }

  @Override
  public int IYL() {
    recordFieldRead("IYL");
    return super.IYL();
  }

  @Override
  public void IYL(int value) {
    recordFieldWrite("IYL");
    super.IYL(value);
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
    analyzeDependencies();

    Map<String, Object> report = new LinkedHashMap<>();
    report.put("title", "Field Access Analysis (Simplified Path-Based)");
    report.put("totalFields", fieldStates.size());
    report.put("totalMethods", methodDependencies.size());

    // Field state details
    Map<String, Object> fieldDetails = new LinkedHashMap<>();
    for (FieldStateInfo state : fieldStates.values()) {
      Map<String, Object> fieldInfo = new LinkedHashMap<>();
      fieldInfo.put("fieldName", state.fieldName);
      fieldInfo.put("lastWriteMethod", state.lastWriteMethod);
      fieldInfo.put("lastWritePath", state.lastWritePath);
      fieldInfo.put("lastReadMethod", state.lastReadMethod);
      fieldInfo.put("lastReadPath", state.lastReadPath);
      fieldInfo.put("needsAsParameter", state.needsAsParameter);
      fieldDetails.put(state.fieldName, fieldInfo);
    }
    report.put("fieldStates", fieldDetails);

    // Method dependencies
    Map<String, Object> methodDetails = new LinkedHashMap<>();
    for (MethodFieldDependencies methodDeps : methodDependencies.values()) {
      Map<String, Object> methodInfo = new LinkedHashMap<>();
      methodInfo.put("method", methodDeps.methodName);
      methodInfo.put("requiredParameters", new ArrayList<>(methodDeps.requiredParameters));
      methodInfo.put("returnValues", new ArrayList<>(methodDeps.returnValues));
      methodInfo.put("readFields", new ArrayList<>(methodDeps.readFields));
      methodInfo.put("writeFields", new ArrayList<>(methodDeps.writeFields));
      methodDetails.put(methodDeps.methodName, methodInfo);
    }
    report.put("methodDependencies", methodDetails);

    // Summary
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("overview", "Path-based field analysis: tracks where fields are last written/read");
    
    List<String> strategy = new ArrayList<>();
    strategy.add("1. Track current call path (stack of methods)");
    strategy.add("2. For each field, record last write path and last read path");
    strategy.add("3. If a field is read in method A, but last written in ancestor, it's a parameter");
    strategy.add("4. If a field is written in method A, but read in ancestor after return, it's a return value");
    strategy.add("5. Build parameter and return lists per method");
    
    summary.put("strategy", strategy);
    report.put("summary", summary);

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
   * Tracks state of a single field across all methods
   */
  private static class FieldStateInfo {
    final String fieldName;
    List<String> lastWritePath;
    String lastWriteMethod;
    List<String> lastReadPath;
    String lastReadMethod;
    AccessType lastAccessType;
    boolean needsAsParameter = false;

    FieldStateInfo(String fieldName) {
      this.fieldName = fieldName;
    }
  }

  /**
   * Tracks field dependencies for a single method
   */
  private static class MethodFieldDependencies {
    final String methodName;
    final Set<String> readFields = new HashSet<>();
    final Set<String> writeFields = new HashSet<>();
    final Set<String> requiredParameters = new HashSet<>();
    final Set<String> returnValues = new HashSet<>();

    MethodFieldDependencies(String methodName) {
      this.methodName = methodName;
    }

    void recordRead(String fieldName) {
      readFields.add(fieldName);
    }

    void recordWrite(String fieldName) {
      writeFields.add(fieldName);
    }

    void addRequiredParameter(String fieldName) {
      requiredParameters.add(fieldName);
    }

    void addReturnValue(String fieldName) {
      returnValues.add(fieldName);
    }

    boolean readsField(String fieldName) {
      return readFields.contains(fieldName);
    }

    boolean writesField(String fieldName) {
      return writeFields.contains(fieldName);
    }

    List<String> getPath() {
      // For now, return just the method name as a single-element path
      // In a full implementation, this would be the actual call path
      return Arrays.asList(methodName);
    }
  }

  /**
   * Type of field access
   */
  private enum AccessType {
    READ, WRITE
  }
}
