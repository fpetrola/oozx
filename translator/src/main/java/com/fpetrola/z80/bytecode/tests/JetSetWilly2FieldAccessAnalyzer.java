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
   * Tracks order of reads/writes to determine parameters and returns
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

      analysis.recordAccess(fieldName, isWrite);
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

      List<String> allReads = new ArrayList<>(analysis.getAllReadFields());
      List<String> allWrites = new ArrayList<>(analysis.getAllWriteFields());
      List<String> parameters = new ArrayList<>(analysis.getRequiredParameters());
      List<String> returns = new ArrayList<>(analysis.getReturnValues());

      methodData.put("fieldReads", allReads);
      methodData.put("fieldWrites", allWrites);
      methodData.put("requiresAsParameters", parameters);
      methodData.put("shouldReturn", returns);

      // Analysis notes
      List<String> notes = new ArrayList<>();
      if (!parameters.isEmpty()) {
        notes.add("PARAMETER: Fields read before write (value comes from caller)");
        notes.addAll(parameters.stream().map(f -> "  - " + f).collect(Collectors.toList()));
      }
      if (!returns.isEmpty()) {
        notes.add("RETURN: Fields written before read (modified value needed by caller)");
        notes.addAll(returns.stream().map(f -> "  - " + f).collect(Collectors.toList()));
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
        .filter(m -> !m.getRequiredParameters().isEmpty() || !m.getReturnValues().isEmpty())
        .sorted((a, b) -> Integer.compare(
            b.getRequiredParameters().size() + b.getReturnValues().size(),
            a.getRequiredParameters().size() + a.getReturnValues().size()))
        .limit(10)
        .map(m -> {
          Map<String, Object> item = new LinkedHashMap<>();
          item.put("method", m.getMethodName());
          item.put("parameterFields", m.getRequiredParameters().size());
          item.put("returnFields", m.getReturnValues().size());
          item.put("totalDependencies", m.getRequiredParameters().size() + m.getReturnValues().size());
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
   * Tracks the ORDER of field accesses to determine parameters and returns
   */
  private static class MethodFieldAnalysis {
    private final String methodName;
    // For each field, track: is first access READ or WRITE?
    private final Map<String, FieldAccessInfo> fieldAccess = new HashMap<>();

    public MethodFieldAnalysis(String methodName) {
      this.methodName = methodName;
    }

    public String getMethodName() {
      return methodName;
    }

    /**
     * Get fields that need to be parameters (READ before WRITE or only READ)
     * For 16-bit registers, check if their 8-bit components were initialized first
     */
    public Set<String> getRequiredParameters() {
      return fieldAccess.values().stream()
          .filter(f -> isRequiredParameterForField(f))
          .map(f -> f.fieldName)
          .collect(Collectors.toSet());
    }
    
    private boolean isRequiredParameterForField(FieldAccessInfo field) {
      // Base check: is this field a parameter?
      if (!field.isRequiredParameter()) {
        return false;
      }
      
      // For 16-bit registers, check if components were already initialized
      Set<String> components = REGISTER_COMPONENTS.get(field.fieldName);
      if (components != null) {
        // All components must NOT be required parameters for the 16-bit to be a parameter
        // In other words: if ALL components are written before read, 16-bit doesn't need parameter
        boolean allComponentsInitialized = components.stream()
            .allMatch(comp -> {
              FieldAccessInfo compInfo = fieldAccess.get(comp);
              return compInfo != null && !compInfo.isRequiredParameter();
            });
        
        // If all components were initialized locally, the 16-bit register doesn't need parameter
        return !allComponentsInitialized;
      }
      
      return true;
    }

    /**
     * Get fields that should be returned (ANY WRITE)
     * For 16-bit registers, only return if at least one component is a return value
     */
    public Set<String> getReturnValues() {
      return fieldAccess.values().stream()
          .filter(f -> shouldReturnField(f))
          .map(f -> f.fieldName)
          .collect(Collectors.toSet());
    }
    
    private boolean shouldReturnField(FieldAccessInfo field) {
      // For 16-bit registers, special handling:
      // Return if ANY component was written (modified), since caller may read the 16-bit after the call
      Set<String> components = REGISTER_COMPONENTS.get(field.fieldName);
      if (components != null) {
        // Check if ANY component is a return value
        boolean anyComponentIsReturn = components.stream()
            .anyMatch(comp -> {
              FieldAccessInfo compInfo = fieldAccess.get(comp);
              return compInfo != null && compInfo.shouldReturn();
            });
        
        if (anyComponentIsReturn) {
          return true;
        }
        
        // Also check if ANY component was written (even if not a "return" by 8-bit logic)
        // because the 16-bit register value may be used by the caller after the call
        boolean anyComponentWritten = components.stream()
            .anyMatch(comp -> {
              FieldAccessInfo compInfo = fieldAccess.get(comp);
              return compInfo != null && compInfo.hasDirectWrite;
            });
        
        return anyComponentWritten;
      }
      
      // For 8-bit registers: base check - was this field written and last access is read?
      return field.shouldReturn();
    }

    /**
     * Get all fields accessed (read or write)
     */
    public Set<String> getAllReadFields() {
      return fieldAccess.values().stream()
          .filter(f -> f.hasDirectRead)
          .map(f -> f.fieldName)
          .collect(Collectors.toSet());
    }

    public Set<String> getAllWriteFields() {
      return fieldAccess.values().stream()
          .filter(f -> f.hasDirectWrite)
          .map(f -> f.fieldName)
          .collect(Collectors.toSet());
    }

    public void recordAccess(String fieldName, boolean isWrite) {
      FieldAccessInfo info = fieldAccess.computeIfAbsent(fieldName, 
          k -> new FieldAccessInfo(fieldName));
      
      if (isWrite) {
        info.recordWrite();
      } else {
        info.recordRead();
      }
      
      // If accessing a 16-bit register, also record access for 8-bit components
      // BUT mark them as indirect (not direct accesses)
      Set<String> components = REGISTER_COMPONENTS.get(fieldName);
      if (components != null) {
        for (String component : components) {
          FieldAccessInfo componentInfo = fieldAccess.computeIfAbsent(component,
              k -> new FieldAccessInfo(component));
          
          if (isWrite) {
            componentInfo.recordIndirectWrite();
          } else {
            componentInfo.recordIndirectRead();
          }
        }
      }
    }

    /**
     * Track access order for a single field
     * Distinguishes between direct and indirect accesses
     */
    private static class FieldAccessInfo {
      final String fieldName;
      boolean hasDirectRead = false;
      boolean hasDirectWrite = false;
      boolean firstAccessIsRead = false;
      boolean firstAccessSet = false;
      boolean lastAccessIsRead = false;

      FieldAccessInfo(String fieldName) {
        this.fieldName = fieldName;
      }

      void recordRead() {
        if (!firstAccessSet) {
          firstAccessIsRead = true;
          firstAccessSet = true;
        }
        hasDirectRead = true;
        lastAccessIsRead = true;
      }

      void recordWrite() {
        if (!firstAccessSet) {
          firstAccessIsRead = false;
          firstAccessSet = true;
        }
        hasDirectWrite = true;
        lastAccessIsRead = false;
      }
      
      void recordIndirectRead() {
        // Indirect access through a register (like (IX+0)) is still a read dependency
        // The register value is needed, so treat as direct read
        recordRead();
      }
      
      void recordIndirectWrite() {
        // Indirect writes through a register still count as uses of that register
        // Keep as indirect - the register itself isn't written, just used for addressing
      }

      /**
       * Parameter: READ before WRITE (or READ only)
       * Only counts DIRECT reads/writes
       */
      boolean isRequiredParameter() {
        return hasDirectRead && (firstAccessIsRead || !hasDirectWrite);
      }

      /**
       * Return: WRITE and last access is READ (modified value needed by caller)
       * Only counts DIRECT reads/writes
       * Fields written but ending with WRITE (not READ) have value discarded by caller
       * If READ is first, the value comes from caller, so it's a parameter not a return
       */
      boolean shouldReturn() {
        return hasDirectWrite && lastAccessIsRead && !firstAccessIsRead;
      }
    }
  }
}
