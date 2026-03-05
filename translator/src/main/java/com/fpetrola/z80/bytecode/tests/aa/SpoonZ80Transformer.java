package com.fpetrola.z80.bytecode.tests.aa;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Spoon-based transformer that uses Spoon's structural AST model for both
 * analysis and code transformation.
 *
 * Per JSON spec, for each method:
 *   "inputs"  =E2=80=93 fields read  =E2=86=92 become method parameters
 *   "outputs" =E2=80=93 fields written =E2=86=92 become local vars returne d as int[]33333
 * Call sites get rewritten to capture int[] and unpack outputs.
 */
public class SpoonZ80Transformer {

  static class MethodSpec {
    List<String> inputs;
    List<String> outputs;
  }

  static class TransformSpec {
    String className;
    Map<String, MethodSpec> methods;
  }

  public static void main(String[] args) throws IOException {
    String specPath = args.length > 0 ? args[0]
        : "src/main/resources/z80-transform-spec.json";
    String srcDir = args.length > 1 ? args[1]
        : "src/main/java";
    String outDir = args.length > 2 ? args[2]
        : "target/generated-z80";

    // 1. Load JSON spec
    TransformSpec spec;
    try (FileReader reader = new FileReader(specPath)) {
      Gson gson = new Gson();
      JsonObject root = gson.fromJson(reader, JsonObject.class);
      spec = new TransformSpec();
      spec.className = root.get("className").getAsString();
      spec.methods = new LinkedHashMap<>();
      for (Map.Entry<String, JsonElement> e :
          root.getAsJsonObject("methods").entrySet()) {
        MethodSpec ms = new MethodSpec();
        if (ms.inputs == null) ms.inputs = new ArrayList<>();
        if (ms.outputs == null) ms.outputs = new ArrayList<>();
        spec.methods.put(e.getKey(), ms);
      }
    }

    // 2. Build Spoon model
    Launcher launcher = new Launcher();
    launcher.addInputResource(srcDir);
    launcher.getEnvironment().setNoClasspath(true);
    launcher.getEnvironment().setAutoImports(true);
    launcher.getEnvironment().setComplianceLevel(17);
    CtModel model = launcher.getModel();

    CtClass<?> targetClass = model.getAllTypes().stream()
        .filter(t -> t.getQualifiedName().equals(spec.className))
        .filter(t -> t instanceof CtClass<?>)
        .map(t -> (CtClass<?>) t)
        .findFirst().orElseThrow(() ->
            new RuntimeException("Class not found: " + spec.className));

    Factory F = targetClass.getFactory();
    CtTypeReference<Integer> intRef = F.Type().integerPrimitiveType();
    CtTypeReference<?> intArrayRef = F.Type().createReference(int[].class);

    // Collect names of instance fields that are candidates for    transformation
    // (exclude static fields like ZF, and array fields like mem)
    Set<String> instanceFieldNames = new LinkedHashSet<>();
    for (CtField<?> field : targetClass.getFields()) {
      if (field.isStatic()) continue;
      if (field.getType().isArray()) continue; // skip mem
      instanceFieldNames.add(field.getSimpleName());
    }

    // Auto-detect inputs/outputs from the AST for each method in the    spec
    for (Map.Entry<String, MethodSpec> entry : spec.methods.entrySet()) {
      String methodName = entry.getKey();
      MethodSpec ms = entry.getValue();
      CtMethod<?> method = targetClass.getMethodsByName(methodName).get(0);

      // Find all instance fields written in this method
      Set<String> writtenFields = new LinkedHashSet<>();
      for (CtFieldWrite<?> fw : method.getElements(new
          TypeFilter<>(CtFieldWrite.class))) {
        String fn = fw.getVariable().getSimpleName();
        if (instanceFieldNames.contains(fn)) writtenFields.add(fn);
      }

      // Find all instance fields read in this method
      Set<String> readFields = new LinkedHashSet<>();
      for (CtFieldRead<?> fr : method.getElements(new
          TypeFilter<>(CtFieldRead.class))) {
        String fn = fr.getVariable().getSimpleName();
        if (instanceFieldNames.contains(fn)) readFields.add(fn);
      }

      // Auto-populate: any instance field used in the method that is
      // NOT already listed in inputs or outputs gets added to      outputs.
      // Also: fields that ARE in inputs but are WRITTEN inside the      method
      // must also appear in outputs (they are both input and output)
      Set<String> alreadyInInputs = new LinkedHashSet<>(ms.inputs);
      Set<String> alreadyInOutputs = new LinkedHashSet<>(ms.outputs);

      Set<String> detectedFields = new LinkedHashSet<>();
      detectedFields.addAll(writtenFields);
      detectedFields.addAll(readFields);

      for (String detected : detectedFields) {
        if (alreadyInOutputs.contains(detected)) continue; //        already in outputs
        if (alreadyInInputs.contains(detected) &&
            writtenFields.contains(detected)) continue;
        // either not in inputs at all, or in inputs but also        written -> add to outputs
        ms.outputs.add(detected);
      }

      System.out.println("auto-detect " + methodName
                         + " : inputs=" + ms.inputs + " outputs=" + ms.outputs);
    }

    // Collect all spec field names (after auto-detection enriched them)
    Set<String> allSpecFields = new LinkedHashSet<>();
    for (MethodSpec m : spec.methods.values()) {
      allSpecFields.addAll(m.inputs);
      allSpecFields.addAll(m.outputs);
    }

    // Track which methods originally returned a value (before we    change them)
    Map<String, Boolean> originallyReturnsValue = new LinkedHashMap<>();
    for (String mn : spec.methods.keySet()) {
      CtMethod<?> m = targetClass.getMethodsByName(mn).get(0);
      originallyReturnsValue.put(mn, !
          m.getType().equals(F.Type().voidPrimitiveType()));
    }

    // 3. Transform each method
    for (Map.Entry<String, MethodSpec> entry : spec.methods.entrySet()) {
      String methodName = entry.getKey();
      MethodSpec ms = entry.getValue();
      CtMethod<?> method = targetClass.getMethodsByName(methodName).get(0);
      boolean hadReturn = originallyReturnsValue.get(methodName);

      Set<String> inputSet = new LinkedHashSet<>(ms.inputs);
      Set<String> outputSet = new LinkedHashSet<>(ms.outputs);
      Set<String> bothSet = new LinkedHashSet<>(inputSet);
      bothSet.retainAll(outputSet);

      // 3a. Change signature: return type =E2=86=92 int[], add param      eters
      method.setType((CtTypeReference) intArrayRef);
      method.setParameters(new ArrayList<>());

      for (String inp : ms.inputs) {
        CtParameter<?> p = F.createParameter();
        p.setSimpleName(inp);
        p.setType(intRef);
        method.addParameter(p);
      }

      // 3b. Insert local variable declarations for outputs at top of      body
      // Skip fields that are both input and output =E2=80=93 the par          ameter      already
      // acts as the local variable (same name, no prefix).
      CtBlock<?> body = method.getBody();
      int insertPos = 0;

      for (String out : ms.outputs) {
        if (bothSet.contains(out)) continue; // parameter already        provides this
        CtLocalVariable<?> decl = F.createLocalVariable(
            intRef, out, (CtExpression) F.createLiteral(0));
        body.addStatement(insertPos++, decl);
      }

      // 3c. Replace field reads in body
      List<CtFieldRead<?>> reads = new ArrayList<>(
          body.getElements(new TypeFilter<>(CtFieldRead.class)));
      for (CtFieldRead<?> fr : reads) {
        String fn = fr.getVariable().getSimpleName();
        if (outputSet.contains(fn)) {
          fr.replace(varRead(F, intRef, fn));
        } else if (inputSet.contains(fn)) {
          fr.replace(varRead(F, intRef, fn));
        }
      }

      // 3d. Replace field writes in body
      List<CtFieldWrite<?>> writes = new ArrayList<>(
          body.getElements(new TypeFilter<>(CtFieldWrite.class)));
      for (CtFieldWrite<?> fw : writes) {
        String fn = fw.getVariable().getSimpleName();
        if (outputSet.contains(fn) || inputSet.contains(fn)) {
          fw.replace(varWrite(F, intRef, fn));
        }
      }

      // 3e. Transform return statements
      if (hadReturn) {
        List<CtReturn<?>> returns = new ArrayList<>(
            body.getElements(new TypeFilter<>(CtReturn.class)));
        for (CtReturn<?> ret : returns) {
          if (ret.getReturnedExpression() != null) {
            CtExpression<?> origExpr = ret.getReturnedExpression().clone();
            CtNewArray<?> arr = buildReturnArray(F, intRef,
                origExpr, ms.outputs);
            ret.setReturnedExpression((CtExpression) arr);
          }
        }
      }

      // 3f. Transform all existing bare "return;" into "return newint[]{...} "
      List<CtReturn<?>> returns = new ArrayList<>(
          body.getElements(new TypeFilter<>(CtReturn.class)));
      for (CtReturn<?> ret : returns) {
        CtNewArray<?> arr = buildReturnArray(F, intRef, null,
            ms.outputs);
        ret.setReturnedExpression((CtExpression) arr);

        // also add a return at the end of body
        CtNewArray<?> arr2 = buildReturnArray(F, intRef, null,
            ms.outputs);
        CtReturn<?> ret2 = F.createReturn();
        ret2.setReturnedExpression((CtExpression) arr2);
        body.addStatement(ret2);
      }
    }

    // 4. Transform call sites
    for (Map.Entry<String, MethodSpec> entry : spec.methods.entrySet()) {
      String calleeName = entry.getKey();
      MethodSpec calleeSpec = entry.getValue();
      boolean calleeHadReturn = originallyReturnsValue.get(calleeName);

      // Detect if callee uses mem (check its parameters after      transformation)
      CtMethod<?> calleeMethod = targetClass.getMethodsByName(calleeName).get(0);

      // Compute the callee's return array index mapping:
      // index 0 = original return value (if hadReturn), then outpu      ts      in order
      // but buildReturnArray may have skipped a duplicated output
      // We need to know the actual index for each output name
      // Rebuild the same skip logic used in buildReturnArray
      String calleeSkipOutput = null;
      if (calleeHadReturn) {
        List<CtReturn<?>> calleeReturns = calleeMethod.getBody()
            .getElements(new TypeFilter<>(CtReturn.class));
        if (!calleeReturns.isEmpty()) {
          CtExpression<?> retExpr = calleeReturns.get(0).getReturnedExpression();
          if (retExpr instanceof CtNewArray<?> newArr &&
              !newArr.getElements().isEmpty()) {
            CtExpression<?> first = (CtExpression<?>)
                newArr.getElements().get(0);
            if (first instanceof CtVariableRead<?> vr) {
              String retVarName = vr.getVariable().getSimpleName();
              for (String out : calleeSpec.outputs) {
                if (retVarName.equals(out)) {
                  calleeSkipOutput = out;
                  break;
                }
              }
            }
          }
        }
      }

      // Build the index map: output name =E2=86=92 array index
      Map<String, Integer> outputIndexMap = new LinkedHashMap<>();
      int arrIdx = calleeHadReturn ? 1 : 0;
      for (String out : calleeSpec.outputs) {
        if (out.equals(calleeSkipOutput)) {
          // this output was merged into index 0 (the return          value position)
          continue;
        } else {
          outputIndexMap.put(out, arrIdx++);
        }
      }

      // Find all invocations to this callee in ALL methods of the      class
      for (CtMethod<?> callerMethod : new
          ArrayList<>(targetClass.getMethods())) {
        // Determine which vXX locals exist in the caller
        String callerName = callerMethod.getSimpleName();
        MethodSpec callerSpec = spec.methods.get(callerName);
        Set<String> callerOutputs = callerSpec != null ?
            new LinkedHashSet<>(callerSpec.outputs) :
            Collections.emptySet();

        List<CtInvocation<?>> invocations = new ArrayList<>(
            callerMethod.getElements(new
                TypeFilter<>(CtInvocation.class)));
        for (CtInvocation<?> inv : invocations) {
          if
          (inv.getExecutable().getSimpleName().equals(calleeName)) {

            // Build the new argument list for the call
            List<CtExpression<?>> newArgs = new ArrayList<>();
            for (String inp : calleeSpec.inputs) {
              newArgs.add(varRead(F, intRef, inp));
            }
            inv.setArguments(newArgs);

            // Find the statement that directly contains this            invocation
            // and its parent block
            CtElement current = inv;
            CtStatement parentStmt = null;
            CtBlock<?> parentBlock = null;
            while (current != null) {
              if (current instanceof CtStatement stmt) {
                parentStmt = stmt;
                if (current instanceof CtBlock<?> blk) {
                  parentBlock = blk;
                  break;
                }
              }
              current = current.getParent();
            }
            if (parentBlock == null) continue;
            int stmtIdx = parentBlock.getStatements().indexOf(parentStmt);

            // Determine if the result was assigned
            String assignTarget = null;
            if (parentStmt instanceof CtLocalVariable<?> lv) {
              assignTarget = lv.getSimpleName();
            } else if (parentStmt instanceof CtAssignment<?, ?>
                assign
                       && assign.getAssigned() instanceof
                           CtVariableWrite<?> vw) {
              assignTarget = vw.getVariable().getSimpleName();
            }

            // Build replacement statements
            List<CtStatement> replacements = new ArrayList<>();

            // int[] _res = callee(args...);
            String resVarName = "_res_" + calleeName;
            CtLocalVariable<?> resDecl = F.createLocalVariable(
                (CtTypeReference) intArrayRef, resVarName,
                (CtExpression) inv.clone());
            replacements.add(resDecl);

            // If callee originally returned a value and it was
            assigned:
            if (calleeHadReturn && assignTarget != null) {
              // assignTarget = _res[0];
              CtAssignment<?, ?> unpackOrig = buildArrayUnpack(
                  F, intRef, assignTarget, resVarName, 0);
              replacements.add(unpackOrig);
            }

            // Unpack each output that exists in the caller's            locals
            for (String out : calleeSpec.outputs) {
              if (callerOutputs.contains(out)) continue; //              caller doesn 't have this var
              int unpackIdx = outputIndexMap.get(out);
              CtAssignment<?, ?> unpack = buildArrayUnpack(
                  F, intRef, out, resVarName, unpackIdx);
              replacements.add(unpack);
            }

            // Replace the original statement with the new            statements
            parentBlock.removeStatement(parentStmt);
            for (int i = 0; i < replacements.size(); i++) {
              parentBlock.addStatement(stmtIdx + i,
                  replacements.get(i));
            }
          }
        }
      }
    }

    // 5. Remove spec fields that are fully replaced by the spec
    for (CtField<?> field : new ArrayList<>(targetClass.getFields())) {
      if (allSpecFields.contains(field.getSimpleName())) {
        field.delete();
      }
    }

    // 6. Write output
    Path outputPath = Path.of(outDir);
    Files.createDirectories(outputPath);
    launcher.setSourceOutputDirectory(outDir);
    launcher.prettyprint();

    System.out.println("Transformation complete. Output in: " + outDir);
    System.out.println("");
    System.out.println("=== Transformed source ===");
    System.out.println("");
    System.out.println(targetClass);
  }

  // Build: new int[] { origExpr, v_out1, v_out2, ... }
  // If origExpr is a variable read like "v_X" that matches one of the
  // outputs, that output is skipped to avoid duplication.
  private static CtNewArray<?> buildReturnArray(
      Factory F, CtTypeReference<Integer> intRef,
      CtExpression<?> origReturnExpr, List<String> outputs) {

    CtNewArray<Integer> arr = F.createNewArray();
    arr.setType(F.Type().createReference(int[].class));
    arr.setType(F.Type().createReference(int[].class));

    // Detect if the original return expr is already one of the output    vars
    String skipOutput = null;
    if (origReturnExpr instanceof CtVariableRead<?> vr) {
      String retVarName = vr.getVariable().getSimpleName();
      for (String out : outputs) {
        if (retVarName.equals(out)) {
          skipOutput = out;
          break;
        }
      }
    }

    if (origReturnExpr != null) {
      arr.addElement(origReturnExpr);
    }

    for (String out : outputs) {
      if (out.equals(skipOutput)) continue; // already included as      origReturnExpr
      arr.addElement(varRead(F, intRef, out));
    }

    return arr;
  }

  // Build: targetVar = resArray[index];
  private static CtAssignment<?, ?> buildArrayUnpack(
      Factory F, CtTypeReference<Integer> intRef, String targetVar, String resArrayVar, int index) {
    CtVariableWrite<Integer> lhs = varWrite(F, intRef, targetVar);
    CtVariableRead<?> arrayRead = varRead(F, F.Type().createReference(int[].class), resArrayVar);
    CtArrayRead<?> rhs = F.Core().createArrayRead();
    rhs.setTarget(arrayRead);
    rhs.setIndexExpression(F.createLiteral(index));
    CtAssignment<?, ?> assign = F.createAssignment();
    assign.setAssigned((CtExpression) lhs);
    assign.setAssignment((CtExpression) rhs);
    return assign;
  }

  @SuppressWarnings("unchecked")
  private static <T> CtVariableRead<T> varRead(Factory F, CtTypeReference<T>
      type, String name) {
    CtLocalVariableReference<T> ref = F.Core().createLocalVariableReference();
    ref.setType(type);
    ref.setSimpleName(name);
    CtVariableRead<T> vr = F.Core().createVariableRead();
    vr.setVariable(ref);
    return vr;
  }

  @SuppressWarnings("unchecked")
  private static <T> CtVariableWrite<T> varWrite(Factory F,
                                                 CtTypeReference<T> type, String name) {
    CtVariableWrite<T> vw = F.Core().createVariableWrite();
    CtLocalVariableReference<T> ref = F.Core().createLocalVariableReference();
    ref.setType(type);
    ref.setSimpleName(name);
    vw.setVariable(ref);
    return vw;
  }
}