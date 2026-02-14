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
 * Stores:
 * - fieldLastWritePath: last path where each field was written
 * - methodFieldDeps: parameters and returns for each method
 */
public class JetSetWilly2FieldAccessAnalyzer3 extends JetSetWilly2 {

  // Core data: for each 8-bit field, store last write path
  private final Map<String, List<Segment>> fieldLastWritePath = new HashMap<>();

  // Result: parameters and returns per method
  private final Map<Segment, MethodFieldDeps> methodFieldDeps = new HashMap<>();

  // Call path stack
  private final Deque<Segment> callPath = new LinkedList<>();

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
    callPath.push(new Segment(methodName));
  }

  public void exitMethod(String name) {
    if (!callPath.isEmpty()) {
      if (!callPath.peek().getMethodName().equals(name)) {
        System.out.println("Warning: exiting method " + name + " but top of call stack is " + callPath.peek().getMethodName());
      }
      callPath.pop();
    }
  }

  private List<Segment> getCurrentPath() {
    return callPath.stream().collect(Collectors.toList());
  }

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

    List<Segment> currentPath = getCurrentPath();
    Set<String> fields8bit = expand8BitFields(fieldName);

    for (String field : fields8bit) {
      fieldLastWritePath.put(field, new ArrayList<>(currentPath));
    }
  }

  private void recordFieldRead(String fieldName) {
    if (!recordingEnabled || callPath.isEmpty()) {
      return;
    }

    List<Segment> readPath = getCurrentPath();
    Set<String> fields8bit = expand8BitFields(fieldName);

    for (String field : fields8bit) {
      List<Segment> writePath = fieldLastWritePath.get(field);

//      if (writePath == null /*|| !pathsAreEqual(writePath, readPath)*/) {
      propagateDependencies(field, writePath, readPath);
//      }
    }
  }

  private void propagateDependencies(String field, List<Segment> writePath, List<Segment> readPath) {
    int commonDepth = getCommonAncestorDepth(writePath, readPath);

    if (readPath.stream().anyMatch(s -> s.getMethodName().contains("36288")) && field.contains("IX")) {
      System.out.println("DAfadgadg");
    }

    checkSegments(field, readPath, writePath);

    int i1 = readPath.indexOf(writePath.get(0));
    // Add as PARAMETER to all methods from read path to common ancestor (exclusive)
    for (int i = i1 - 1; i >= 0; i--) {
      Segment method = readPath.get(i);
      methodFieldDeps.computeIfAbsent(method, k -> new MethodFieldDeps(method))
          .addParameter(field);
    }

    // Add as RETURN to all methods from write path to common ancestor (exclusive)
    if (writePath != null) {

      checkSegments(field, writePath, readPath);

      int i2 = writePath.indexOf(readPath.get(0));
      for (int i = i2 - 1; i >= 0; i--) {
        Segment method = writePath.get(i);
        methodFieldDeps.computeIfAbsent(method, k -> new MethodFieldDeps(method))
            .addReturn(field);
      }
    }
  }

  private boolean checkSegments(String field, List<Segment> writePath, List<Segment> readPath) {
    for (int j = readPath.size() - 1; j >= 0; j--) {
      for (int i = writePath.size() - 1; i >= 0; i--) {
        Segment segment1 = writePath.get(i);
        Segment segment2 = readPath.get(j);
        if (segment1.getMethodName().equals(segment2.getMethodName()))

          if (segment1.id != segment2.id) {
            methodFieldDeps.computeIfAbsent(segment1, k -> new MethodFieldDeps(segment1)).addReturn(field);
            return false;
//            System.out.println("Warning: different root methods for read and write of field " + field);
          }
      }
    }
    return true;
  }

  private int getCommonAncestorDepth(List<Segment> path1, List<Segment> path2) {
    if (path1 == null) {
      return path2.size() - 1;
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

  private boolean pathsAreEqual(List<Segment> path1, List<Segment> path2) {
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

  public void saveAnalysis(String filePath) throws Exception {
    Map<String, Object> json = new LinkedHashMap<>();

    // fieldLastWritePath data - convert Segments to strings
    Map<String, List<String>> fieldPathsStr = new LinkedHashMap<>();
    for (Map.Entry<String, List<Segment>> entry : fieldLastWritePath.entrySet()) {
      List<String> pathStrs = entry.getValue().stream()
          .map(Segment::getMethodName)
          .collect(Collectors.toList());
      fieldPathsStr.put(entry.getKey(), pathStrs);
    }
    json.put("fieldLastWritePath", fieldPathsStr);

    // methodFieldDeps data
    Map<String, Object> methods = new LinkedHashMap<>();
    for (MethodFieldDeps deps : methodFieldDeps.values()) {
      Map<String, Object> methodData = new LinkedHashMap<>();
      methodData.put("parameters", new ArrayList<>(deps.parameters));
      methodData.put("returns", new ArrayList<>(deps.returns));
      methods.put(deps.segment.getMethodName(), methodData);
    }
    json.put("methodDependencies", methods);

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.writeValue(new File(filePath), json);
    System.out.println("Analysis saved to: " + filePath);
  }

  private static class Segment {
    private static int idCounter = 0;
    private final int id;
    private final String methodName;

    Segment(String methodName) {
      this.id = idCounter++;
      this.methodName = methodName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Segment segment = (Segment) o;
      return methodName.equals(segment.methodName);
    }

    @Override
    public int hashCode() {
      return methodName.hashCode();
    }

    @Override
    public String toString() {
      return methodName + "#" + id;
    }

    public String getMethodName() {
      return methodName;
    }

    public int getId() {
      return id;
    }
  }

  private static class MethodFieldDeps {
    final Segment segment;
    final Set<String> parameters = new HashSet<>();
    final Set<String> returns = new HashSet<>();

    MethodFieldDeps(Segment segment) {
      this.segment = segment;
    }

    void addParameter(String fieldName) {
      parameters.add(fieldName);
    }

    void addReturn(String fieldName) {
      returns.add(fieldName);
    }
  }
}
