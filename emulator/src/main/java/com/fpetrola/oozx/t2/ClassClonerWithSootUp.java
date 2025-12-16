package com.fpetrola.oozx.t2;

import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import org.apache.commons.io.FileUtils;
import org.cojen.maker.*;
import sootup.core.IdentifierFactory;
import sootup.core.graph.StmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.AbstractConditionExpr;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.common.stmt.*;
import sootup.core.model.*;
import sootup.core.types.ClassType;
import sootup.core.types.Type;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

public class ClassClonerWithSootUp {
  public static void main(String[] args) {
    args = new String[]{IndirectMemory8BitReference.class.getName()};
    if (args.length < 1) {
      System.err.println("Uso: java ClassClonerWithSootUp <nombreClaseOriginal>");
      return;
    }

    String originalClassName = args[0];
    String newClassName = originalClassName + "Clone";

    // Configurar SootUp
    AnalysisInputLocation inputLocation = new JavaClassPathAnalysisInputLocation("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/emulator/target/classes"); // Ajusta al path de clases
    JavaView view = new JavaView(inputLocation);

    IdentifierFactory factory = view.getIdentifierFactory();
    ClassType classType = factory.getClassType(originalClassName);
    JavaSootClass originalClass = (JavaSootClass) view.getClassOrThrow(classType);

    // Iniciar generación con Cojen/Maker
    Optional<JavaClassType> superclass = originalClass.getSuperclass();
    ClassMaker cm = ClassMaker.beginExternal(newClassName)
        .public_();

    // Recorrer y clonar fields
    for (SootField sf : originalClass.getFields()) {
      Set<FieldModifier> modifiers = sf.getModifiers();
      String fieldType = sf.getType().toString();
      String fieldName = sf.getName();
      FieldMaker fieldMaker = cm.addField(fieldType, fieldName);
      setModifiers(fieldMaker, modifiers);
    }

    // Recorrer y clonar methods
    for (SootMethod sm : originalClass.getMethods()) {
      Set<MethodModifier> modifiers = sm.getModifiers();
      String returnType = sm.getReturnType().toString();

      MethodMaker methodMaker;
      if (sm.getName().equals("<init>") && returnType.equals("void")) {
        methodMaker = cm.addConstructor();
        methodMaker.invokeSuperConstructor();
      } else {
        List<Type> parameterTypes1 = sm.getParameterTypes();
        List<String> parameterTypes = new ArrayList<>();
        for (Type pt : parameterTypes1) {
          if (pt instanceof ClassType classType1){
            parameterTypes.add(classType1.getFullyQualifiedName());
          }
        }
        methodMaker = cm.addMethod(returnType, sm.getName(), parameterTypes.toArray(new Class[0]));
      }
      MethodMaker mm = setModifiers(methodMaker, modifiers);

      if (sm.isAbstract() || !sm.hasBody()) {
        continue;
      }

      Body body = sm.getBody();

      // Mapear locales a variables de Maker
      Map<Local, Variable> localToVar = new HashMap<>();
      for (Local local : body.getLocals()) {
        Variable var = mm.var(local.getType().toString());
        localToVar.put(local, var);
      }

      // Parámetros: En SootUp, params son locales con identity stmts; aquí asumimos mm.param(i) para params
      for (int i = 0; i < sm.getParameterCount(); i++) {
        // Encuentra el local correspondiente (típicamente el primero después de 'this')
        // Para precisión, recorre stmts para identity, pero simplificamos
        Local paramLocal = body.getParameterLocal(i); // Si disponible, usa esto
        localToVar.put(paramLocal, mm.param(i));
      }

      // Recorrer statements de Jimple y traducir a Maker (EJEMPLO BÁSICO)
      StmtGraph<?> stmtGraph = body.getStmtGraph();

      for (Stmt stmt : stmtGraph.getStmts()) {
        if (stmt instanceof JAssignStmt) {
          JAssignStmt assign = (JAssignStmt) stmt;
          Value left = assign.getLeftOp();
          Value right = assign.getRightOp();

          Variable leftVar = getVarFromValue(left, mm, localToVar);
          Variable rightVar = getVarFromValue(right, mm, localToVar);

          leftVar.set(rightVar);
        } else if (stmt instanceof JReturnStmt) {
          JReturnStmt ret = (JReturnStmt) stmt;
          Variable retVar = getVarFromValue(ret.getOp(), mm, localToVar);
          mm.return_(retVar);
        } else if (stmt instanceof JIfStmt) {
          JIfStmt ifStmt = (JIfStmt) stmt;
          AbstractConditionExpr cond = (AbstractConditionExpr) ifStmt.getCondition();
          Variable op1 = getVarFromValue(cond.getOp1(), mm, localToVar);
          Variable op2 = getVarFromValue(cond.getOp2(), mm, localToVar);
          Label elseLabel = mm.label();
          op1.ifNe(op2, elseLabel); // Ajusta según cond (eq, ne, lt, etc.)
          elseLabel.here();
        } else if (stmt instanceof JInvokeStmt) {
          // Implementa: mm.invoke(...)
        } else if (stmt instanceof JReturnVoidStmt) {
          mm.return_();
        }
        // Agrega más: IdentityStmt (ignora o mapea params), GotoStmt (usa labels), etc.
      }
    }

    // Finalizar y cargar
    byte[] clonedClass = cm.finishBytes();
    writeClass(clonedClass);
  }

  private static void writeClass(byte[] clonedClass) {
    try {
      File file = new File("Test3.class");
      FileUtils.writeByteArrayToFile(file, clonedClass);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static MethodMaker setModifiers(MethodMaker methodMaker, Set<MethodModifier> modifiers) {
//    MethodMaker modifiers1 = methodMaker.modifiers(modifiers);
    return methodMaker;
  }

  private static void setModifiers(FieldMaker fieldMaker, Set<FieldModifier> modifiers) {
//    fieldMaker.modifiers(modifiers);
  }

  // Helper: Value de Jimple a Variable de Maker
  private static Variable getVarFromValue(Value value, MethodMaker mm, Map<Local, Variable> localToVar) {
    if (value instanceof Local) {
      return localToVar.get((Local) value);
    } else if (value instanceof IntConstant) {
      return mm.var(int.class).set(((IntConstant) value).getValue());
    } else if (value instanceof StringConstant) {
      return mm.var(String.class).set(((StringConstant) value).getValue());
    } else if (value instanceof JFieldRef) {
      JFieldRef fr = (JFieldRef) value;
      return mm.field(fr.getFieldSignature().getName()); // Ajusta instancia/static
    }
    // Agrega más: NullConstant, etc.
    throw new UnsupportedOperationException("Value no soportado: " + value);
  }
}