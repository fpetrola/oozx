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
 * Analyzes field dependencies in JetSetWilly2 by capturing stack traces
 * whenever fields are read or written during game execution.
 * 
 * Extends JetSetWilly2 and intercepts all field accessor methods to track:
 * - Which methods read/write which fields
 * - The call chain (parent->child->grandchild)
 * - Parameter requirements for refactoring
 * - Return value requirements for refactoring
 */
public class JetSetWilly2FieldAccessAnalyzer extends JetSetWilly2 {

  private final Map<String, MethodFieldAnalysis> methodAnalyses = new HashMap<>();
  private final Set<String> allFields = new HashSet<>(Arrays.asList(
      "A", "F", "B", "C", "D", "E", "H", "L", "IXH", "IXL", "IYH", "IYL",
      "AF", "BC", "DE", "HL", "IX", "IY", "SP"
  ));
  private final Map<String, Integer> fieldAccessCounter = new HashMap<>();
  private boolean recordingEnabled = true;

  public JetSetWilly2FieldAccessAnalyzer(MiniZXIO<WordNumber> rzxPlayerIO,
      Predicate<Integer> interruptionCondition) {
    super(rzxPlayerIO, interruptionCondition);
  }

  public JetSetWilly2FieldAccessAnalyzer() {
    super();
  }

  /**
   * Record field access using methodStack (RoutineCallInterceptor maintains it)
   * Much faster than reading Java stack traces
   */
  private void recordFieldAccess(String fieldName, boolean isWrite) {
    if (!recordingEnabled) {
      return;
    }

    // Get current method from routine call stack (maintained by RoutineCallInterceptor)
    if (methodStack != null && !methodStack.isEmpty()) {
      String currentMethod = (String) methodStack.peek();
      
      MethodFieldAnalysis analysis = methodAnalyses.computeIfAbsent(
          currentMethod, k -> new MethodFieldAnalysis(currentMethod));

      if (isWrite) {
        analysis.addFieldWrite(fieldName);
      } else {
        analysis.addFieldRead(fieldName);
      }

      fieldAccessCounter.merge(fieldName + (isWrite ? ":W" : ":R"), 1, Integer::sum);
    }
  }

  // Override all field accessors to capture reads/writes

  @Override
  public int A() {
    recordFieldAccess("A", false);
    return super.A();
  }

  @Override
  public void A(int value) {
    recordFieldAccess("A", true);
    super.A(value);
  }

  @Override
  public int F() {
    recordFieldAccess("F", false);
    return super.F();
  }

  @Override
  public void F(int value) {
    recordFieldAccess("F", true);
    super.F(value);
  }

  @Override
  public int B() {
    recordFieldAccess("B", false);
    return super.B();
  }

  @Override
  public void B(int value) {
    recordFieldAccess("B", true);
    super.B(value);
  }

  @Override
  public int C() {
    recordFieldAccess("C", false);
    return super.C();
  }

  @Override
  public void C(int value) {
    recordFieldAccess("C", true);
    super.C(value);
  }

  @Override
  public int D() {
    recordFieldAccess("D", false);
    return super.D();
  }

  @Override
  public void D(int value) {
    recordFieldAccess("D", true);
    super.D(value);
  }

  @Override
  public int E() {
    recordFieldAccess("E", false);
    return super.E();
  }

  @Override
  public void E(int value) {
    recordFieldAccess("E", true);
    super.E(value);
  }

  @Override
  public int H() {
    recordFieldAccess("H", false);
    return super.H();
  }

  @Override
  public void H(int value) {
    recordFieldAccess("H", true);
    super.H(value);
  }

  @Override
  public int L() {
    recordFieldAccess("L", false);
    return super.L();
  }

  @Override
  public void L(int value) {
    recordFieldAccess("L", true);
    super.L(value);
  }

  @Override
  public int AF() {
    recordFieldAccess("AF", false);
    return super.AF();
  }

  @Override
  public void AF(int value) {
    recordFieldAccess("AF", true);
    super.AF(value);
  }

  @Override
  public int BC() {
    recordFieldAccess("BC", false);
    return super.BC();
  }

  @Override
  public void BC(int value) {
    recordFieldAccess("BC", true);
    super.BC(value);
  }

  @Override
  public int DE() {
    recordFieldAccess("DE", false);
    return super.DE();
  }

  @Override
  public void DE(int value) {
    recordFieldAccess("DE", true);
    super.DE(value);
  }

  @Override
  public int HL() {
    recordFieldAccess("HL", false);
    return super.HL();
  }

  @Override
  public void HL(int value) {
    recordFieldAccess("HL", true);
    super.HL(value);
  }

  @Override
  public int IX() {
    recordFieldAccess("IX", false);
    return super.IX();
  }

  @Override
  public void IX(int value) {
    recordFieldAccess("IX", true);
    super.IX(value);
  }

  @Override
  public int IY() {
    recordFieldAccess("IY", false);
    return super.IY();
  }

  @Override
  public void IY(int value) {
    recordFieldAccess("IY", true);
    super.IY(value);
  }

  @Override
  public int IXH() {
    recordFieldAccess("IXH", false);
    return super.IXH();
  }

  @Override
  public void IXH(int value) {
    recordFieldAccess("IXH", true);
    super.IXH(value);
  }

  @Override
  public int IXL() {
    recordFieldAccess("IXL", false);
    return super.IXL();
  }

  @Override
  public void IXL(int value) {
    recordFieldAccess("IXL", true);
    super.IXL(value);
  }

  @Override
  public int IYH() {
    recordFieldAccess("IYH", false);
    return super.IYH();
  }

  @Override
  public void IYH(int value) {
    recordFieldAccess("IYH", true);
    super.IYH(value);
  }

  @Override
  public int IYL() {
    recordFieldAccess("IYL", false);
    return super.IYL();
  }

  @Override
  public void IYL(int value) {
    recordFieldAccess("IYL", true);
    super.IYL(value);
  }

  @Override
  public int SP() {
    recordFieldAccess("SP", false);
    return super.SP();
  }

  @Override
  public void SP(int value) {
    recordFieldAccess("SP", true);
    super.SP(value);
  }

  public Map<String, Object> generateReport() {
    Map<String, Object> report = new LinkedHashMap<>();

    report.put("analysisType", "Field Dependency Analysis via Stack Trace Interception");
    report.put("gameClass", "JetSetWilly2");
    report.put("analysisDate", new Date().toString());
    report.put("description",
        "Tracks which fields are read/written in each method by intercepting field accessors");

    // Summary statistics
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("totalMethodsAnalyzed", methodAnalyses.size());
    summary.put("totalFieldsIdentified", allFields.size());
    summary.put("totalFieldAccessRecorded", fieldAccessCounter.size());
    report.put("summary", summary);

    // Method-level analysis
    Map<String, Object> methodsData = new LinkedHashMap<>();
    for (Map.Entry<String, MethodFieldAnalysis> entry : methodAnalyses.entrySet()) {
      MethodFieldAnalysis analysis = entry.getValue();

      Map<String, Object> methodData = new LinkedHashMap<>();
      methodData.put("name", analysis.getMethodName());

      List<String> reads = new ArrayList<>(analysis.getFieldReads());
      List<String> writes = new ArrayList<>(analysis.getFieldWrites());

      methodData.put("fieldReads", reads);
      methodData.put("fieldWrites", writes);
      methodData.put("requiresAsParameters", reads);
      methodData.put("shouldReturn", writes);

      // Analysis notes
      List<String> notes = new ArrayList<>();
      if (!reads.isEmpty()) {
        notes.add("PARAMETER: Add these as parameters since they're read without initialization");
        notes.addAll(reads.stream().map(f -> "  - " + f).collect(Collectors.toList()));
      }
      if (!writes.isEmpty()) {
        notes.add("RETURN: Create return object with these fields (modified during execution)");
        notes.addAll(writes.stream().map(f -> "  - " + f).collect(Collectors.toList()));
      }
      methodData.put("refactoringNotes", notes);

      methodsData.put(entry.getKey(), methodData);
    }

    report.put("methods", methodsData);

    // Field access frequency
    Map<String, Integer> fieldFrequency = new LinkedHashMap<>();
    for (String field : allFields) {
      int reads = fieldAccessCounter.getOrDefault(field + ":R", 0);
      int writes = fieldAccessCounter.getOrDefault(field + ":W", 0);
      if (reads > 0 || writes > 0) {
        fieldFrequency.put(field, reads + writes);
      }
    }
    report.put("fieldAccessFrequency", fieldFrequency);

    // Refactoring strategy
    Map<String, Object> strategy = generateRefactoringStrategy();
    report.put("refactoringStrategy", strategy);

    return report;
  }

  private Map<String, Object> generateRefactoringStrategy() {
    Map<String, Object> strategy = new LinkedHashMap<>();

    strategy.put("overview",
        "Convert instance fields to local variables and parameters");

    List<String> steps = new ArrayList<>();
    steps.add("1. For each method, identify requiredAsParameters - these must be added as method parameters");
    steps.add("2. Fields that are read but not in parent scope must come from grandparent - trace up the call chain");
    steps.add("3. Create a return record/class for shouldReturn fields");
    steps.add("4. Propagate return values up the call chain (grandchild -> child -> parent)");
    steps.add("5. For methods called from multiple places, union all parameter requirements");
    steps.add("6. Use try-catch or explicit return to handle fields modified in nested calls");

    strategy.put("steps", steps);

    // Methods needing most refactoring
    List<Map<String, Object>> methodsNeedingWork = methodAnalyses.values().stream()
        .filter(m -> !m.getFieldReads().isEmpty() || !m.getFieldWrites().isEmpty())
        .sorted((a, b) -> Integer.compare(
            b.getFieldReads().size() + b.getFieldWrites().size(),
            a.getFieldReads().size() + a.getFieldWrites().size()))
        .limit(10)
        .map(m -> {
          Map<String, Object> item = new LinkedHashMap<>();
          item.put("method", m.getMethodName());
          item.put("readFields", m.getFieldReads().size());
          item.put("writeFields", m.getFieldWrites().size());
          item.put("totalDependencies", m.getFieldReads().size() + m.getFieldWrites().size());
          return item;
        })
        .collect(Collectors.toList());

    strategy.put("topMethodsNeedingRefactoring", methodsNeedingWork);

    return strategy;
  }

  public void saveReport(String filePath) throws Exception {
    Map<String, Object> report = generateReport();
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.writeValue(new File(filePath), report);
    System.out.println("Report saved to: " + filePath);
  }

  /**
   * Information about a method's field dependencies
   */
  private static class MethodFieldAnalysis {
    private final String methodName;
    private final Set<String> fieldReads = new HashSet<>();
    private final Set<String> fieldWrites = new HashSet<>();

    public MethodFieldAnalysis(String methodName) {
      this.methodName = methodName;
    }

    public String getMethodName() {
      return methodName;
    }

    public Set<String> getFieldReads() {
      return fieldReads;
    }

    public Set<String> getFieldWrites() {
      return fieldWrites;
    }

    public void addFieldRead(String field) {
      fieldReads.add(field);
    }

    public void addFieldWrite(String field) {
      fieldWrites.add(field);
    }
  }
}
