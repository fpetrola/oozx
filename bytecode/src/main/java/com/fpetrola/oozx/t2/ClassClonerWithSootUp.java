package com.fpetrola.oozx.t2;

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
import java.io.IOException;
import java.util.*;

public class ClassClonerWithSootUp {
  public static void main(String[] args) {
    String originalClassName = args[0];
    String newClassName = originalClassName + "Clone";

    // Configurar SootUp
    AnalysisInputLocation inputLocation = new JavaClassPathAnalysisInputLocation("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/bytecode/target/classes"); // Ajusta al path de clases
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
      List<Type> parameterTypes1 = sm.getParameterTypes();
      Object[] parameterTypes = getParameterTypes(parameterTypes1);

      MethodMaker methodMaker;
      if (sm.getName().equals("<init>") && returnType.equals("void")) {
        methodMaker = cm.addConstructor(parameterTypes);
        methodMaker.invokeSuperConstructor();
      } else {

        methodMaker = cm.addMethod(returnType, sm.getName(), parameterTypes);
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
      int paramCount = sm.getParameterCount();
      for (int i = 0; i < paramCount; i++) {
        // Encuentra el local correspondiente (típicamente el primero después de 'this')
        // Para precisión, recorre stmts para identity, pero simplificamos
        try {
          Local paramLocal = body.getParameterLocal(i); // Si disponible, usa esto
          if (paramLocal != null && i < paramCount) {
            localToVar.put(paramLocal, mm.param(i));
          }
        } catch (Exception e) {
          System.err.println("Error mapeando parámetro " + i + ": " + e.getMessage());
        }
      }

      // Recorrer statements de Jimple y traducir a Maker (EJEMPLO BÁSICO)
      StmtGraph<?> stmtGraph = body.getStmtGraph();

      for (Stmt stmt : stmtGraph.getStmts()) {
        if (stmt instanceof JAssignStmt) {
          JAssignStmt assign = (JAssignStmt) stmt;
          Value left = assign.getLeftOp();
          Value right = assign.getRightOp();

          try {
            Variable leftVar = getVarFromValue(left, mm, localToVar);
            Variable rightVar = getVarFromValue(right, mm, localToVar);
            if (leftVar != null && rightVar != null) {
              leftVar.set(rightVar);
            }
          } catch (Exception e) {
            System.err.println("Error procesando assign: " + assign + " -> " + e.getMessage());
          }
        } else if (stmt instanceof JReturnStmt) {
          JReturnStmt ret = (JReturnStmt) stmt;
          Variable retVar = getVarFromValue(ret.getOp(), mm, localToVar);
          if (retVar != null) {
            mm.return_(retVar);
          }
        } else if (stmt instanceof JIfStmt) {
          JIfStmt ifStmt = (JIfStmt) stmt;
          AbstractConditionExpr cond = (AbstractConditionExpr) ifStmt.getCondition();
          try {
            Variable op1 = getVarFromValue(cond.getOp1(), mm, localToVar);
            Variable op2 = getVarFromValue(cond.getOp2(), mm, localToVar);
            Label elseLabel = mm.label();
            if (op1 != null && op2 != null) {
              op1.ifNe(op2, elseLabel); // Ajusta según cond (eq, ne, lt, etc.)
              elseLabel.here();
            }
          } catch (Exception e) {
            System.err.println("Error procesando if: " + ifStmt + " -> " + e.getMessage());
          }
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

  private static Class<?>[] getParameterTypes(List<Type> parameterTypes1) {
    List<String> parameterTypes = new ArrayList<>();
    for (Type pt : parameterTypes1) {
      if (pt instanceof ClassType classType1){
        parameterTypes.add(classType1.getFullyQualifiedName());
      }
    }
    List<? extends Class<?>> parameterList = parameterTypes.stream().map(s -> {
      try {
        return Class.forName(s);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }).toList();
    return parameterList.toArray(new Class[0]);
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
      Local local = (Local) value;
      Variable v = localToVar.get(local);
      return v != null ? v : null;
    } else if (value instanceof IntConstant) {
      int val = ((IntConstant) value).getValue();
      return mm.var(int.class).set(val);
    } else if (value instanceof StringConstant) {
      String val = ((StringConstant) value).getValue();
      return mm.var(String.class).set(val);
    } else if (value instanceof JFieldRef) {
      JFieldRef fr = (JFieldRef) value;
      return mm.field(fr.getFieldSignature().getName()); // Ajusta instancia/static
    } else if (value instanceof sootup.core.jimple.common.expr.AbstractInvokeExpr) {
      // Para invocaciones, retorna null - no se puede representar con Maker sin contexto más complejo
      return null;
    }
    // Agrega más: NullConstant, etc.
    return null; // En lugar de lanzar excepción, retorna null
  }
}