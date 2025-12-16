package com.fpetrola.oozx.t2;

import org.apache.commons.io.FileUtils;
import org.cojen.maker.*;
import sootup.core.IdentifierFactory;
import sootup.core.graph.StmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.common.stmt.*;
import sootup.core.model.*;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClassClonerWithSootUp {
  public static void main(String[] args) {
    String originalClassName = args[0];
    String newClassName = originalClassName + "Clone";

    // Configurar SootUp
    AnalysisInputLocation inputLocation = new JavaClassPathAnalysisInputLocation("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/bytecode/target/test-classes");
    JavaView view = new JavaView(inputLocation);

    IdentifierFactory factory = view.getIdentifierFactory();
    ClassType classType = factory.getClassType(originalClassName);
    JavaSootClass originalClass = (JavaSootClass) view.getClassOrThrow(classType);

    // Iniciar generación con Cojen/Maker
    ClassMaker cm = ClassMaker.beginExternal(newClassName)
        .public_();

    // Recorrer y clonar fields (mantener orden original usando reflection)
    try {
      Class<?> originalJavaClass = Class.forName(originalClassName);
      for (java.lang.reflect.Field javaField : originalJavaClass.getDeclaredFields()) {
        String fieldName = javaField.getName();
        String fieldType = javaField.getType().getName();

        // Convertir tipos primitivos a nombres correctos
        if (fieldType.equals("int")) {
          fieldType = "int";
        } else if (fieldType.equals("boolean")) {
          fieldType = "boolean";
        } else if (!fieldType.startsWith("[")) {
          // Es una clase
          if (!fieldType.startsWith("java.")) {
            fieldType = fieldType; // Ya está completo
          }
        }

        FieldMaker fieldMaker = cm.addField(fieldType, fieldName);
      }
    } catch (Exception e) {
      // Fallback: usar SootUp fields
      for (SootField sf : originalClass.getFields()) {
        String fieldType = sf.getType().toString();
        String fieldName = sf.getName();
        FieldMaker fieldMaker = cm.addField(fieldType, fieldName);
      }
    }

    // Recorrer y clonar methods
    for (SootMethod sm : originalClass.getMethods()) {
      processMethod(sm, cm);
    }

    // Finalizar y cargar
    byte[] clonedClass = cm.finishBytes();
    writeClass(clonedClass);
  }

  private static void processMethod(SootMethod sm, ClassMaker cm) {
    Set<MethodModifier> modifiers = sm.getModifiers();
    String returnType = sm.getReturnType().toString();
    List<Type> parameterTypes1 = sm.getParameterTypes();
    Object[] parameterTypes = getParameterTypes(parameterTypes1);

    MethodMaker methodMaker;
    if (sm.getName().equals("<init>") && returnType.equals("void")) {
      methodMaker = cm.addConstructor(parameterTypes);
      // No llamamos invokeSuperConstructor() aquí, lo haremos después de procesar statements
    } else {
      methodMaker = cm.addMethod(returnType, sm.getName(), parameterTypes);
    }
    MethodMaker mm = setModifiers(methodMaker, modifiers);

    // Para constructores, invocar super solo si es necesario
    boolean isConstructor = sm.getName().equals("<init>") && returnType.equals("void");

    if (sm.isAbstract() || !sm.hasBody()) {
      return;
    }

    Body body = sm.getBody();
    StmtGraph<?> stmtGraph = body.getStmtGraph();
    int paramCount = sm.getParameterCount();

    // Mapear locales a variables de Maker
    Map<Local, Variable> localToVar = new HashMap<>();
    for (Local local : body.getLocals()) {
      Variable var = mm.var(local.getType().toString());
      localToVar.put(local, var);
    }

    // Parámetros: Mapear identity statements
    for (Stmt stmt : stmtGraph.getStmts()) {
      if (stmt instanceof JIdentityStmt) {
        JIdentityStmt idStmt = (JIdentityStmt) stmt;
        Object right = idStmt.getRightOp();
        Local left = (Local) idStmt.getLeftOp();

        String rightClass = right.getClass().getSimpleName();
        if (rightClass.contains("ThisRef")) {
          localToVar.put(left, mm.this_());
        } else if (rightClass.contains("ParameterRef")) {
          try {
            // Usar reflection para obtener el índice del parámetro
            int paramIndex = (Integer) right.getClass().getMethod("getIndex").invoke(right);
            // Los parámetros ya se declararon en addMethod/addConstructor
            // Aquí simplemente registramos la variable local como parámetro
            if (paramIndex < paramCount) {
              localToVar.put(left, mm.param(paramIndex));
            }
          } catch (Exception e) {
            // Si hay error, simplemente continuar - la variable local ya está mapeada arriba
          }
        }
      }
    }

    // Procesar el constructor para asignar parámetros a campos
    if (isConstructor) {
      mm.invokeSuperConstructor();
      // Procesar statements del constructor para asignar parámetros
      for (Stmt stmt : stmtGraph.getStmts()) {
        if (stmt instanceof JAssignStmt) {
          JAssignStmt assign = (JAssignStmt) stmt;
          Object left = assign.getLeftOp();
          Object right = assign.getRightOp();

          try {
            // Si es this.field = param, procesarlo
            if (left instanceof JFieldRef && right instanceof Local) {
              JFieldRef fr = (JFieldRef) left;
              Local rightLocal = (Local) right;
              Variable rightVar = localToVar.get(rightLocal);
              if (rightVar != null) {
                mm.field(fr.getFieldSignature().getName()).set(rightVar);
              }
            }
          } catch (Exception e) {
            // Ignorar errores
          }
        }
      }
    } else {
      // Para métodos normales, intentar generar código desde statements
      generateMethodBody(mm, sm, stmtGraph, localToVar, returnType);
    }
  }

  private static Class<?>[] getParameterTypes(List<Type> parameterTypes1) {
    List<Class<?>> parameterList = new ArrayList<>();
    for (Type pt : parameterTypes1) {
      if (pt instanceof ClassType classType1) {
        try {
          parameterList.add(Class.forName(classType1.getFullyQualifiedName()));
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      } else if (pt instanceof PrimitiveType primitiveType) {
        parameterList.add(int.class);
      }
    }

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
  return methodMaker;
}

private static void setModifiers(FieldMaker fieldMaker, Set<FieldModifier> modifiers) {
}

// Helper: Generar body del método desde Jimple statements
private static void generateMethodBody(MethodMaker mm, SootMethod sm, StmtGraph<?> stmtGraph,
                                       Map<Local, Variable> localToVar, String returnType) {
  boolean foundReturn = false;
  Map<Local, JFieldRef> localToField = new HashMap<>();

  // Primer paso: mapear locales a campos
  for (Stmt stmt : stmtGraph.getStmts()) {
    if (stmt instanceof JAssignStmt) {
      JAssignStmt assign = (JAssignStmt) stmt;
      Object left = assign.getLeftOp();
      Object right = assign.getRightOp();

      if (left instanceof Local && right instanceof JFieldRef) {
        localToField.put((Local) left, (JFieldRef) right);
      }
    }
  }

  // Segundo paso: procesar statements y generar código
  for (Stmt stmt : stmtGraph.getStmts()) {
    if (stmt instanceof JIdentityStmt || stmt instanceof JAssignStmt) {
      if (stmt instanceof JAssignStmt) {
        JAssignStmt assign = (JAssignStmt) stmt;
        Object left = assign.getLeftOp();
        Object right = assign.getRightOp();

        // Si es asignación a campo desde variable o invocación
        if (left instanceof JFieldRef) {
          JFieldRef fr = (JFieldRef) left;
          try {
            // Intentar asignar al campo
            if (right instanceof JVirtualInvokeExpr) {
              // Ignorar por ahora - es complejo
            } else if (right instanceof Local) {
              Variable rightVar = localToVar.get((Local) right);
              if (rightVar != null) {
                mm.field(fr.getFieldSignature().getName()).set(rightVar);
              }
            }
          } catch (Exception e) {
            // Ignorar errores
          }
        }
      }
    } else if (stmt instanceof JReturnStmt) {
      JReturnStmt ret = (JReturnStmt) stmt;
      Object op = ret.getOp();

      try {
        if (op instanceof JFieldRef) {
          // Retornar un campo directamente
          JFieldRef fr = (JFieldRef) op;
          mm.return_(mm.field(fr.getFieldSignature().getName()));
          foundReturn = true;
          break;
        } else if (op instanceof Local) {
          // Retornar una variable local que es un campo
          Local retLocal = (Local) op;
          JFieldRef fr = localToField.get(retLocal);
          if (fr != null) {
            mm.return_(mm.field(fr.getFieldSignature().getName()));
            foundReturn = true;
            break;
          }
        }
      } catch (Exception e) {
        // Ignorar
      }
    } else {
      System.out.println("Cannot process");
    }
  }

  // Si no encontramos un return válido, generar default
  if (!foundReturn) {
    if (returnType.equals("void")) {
      mm.return_();
    } else if (returnType.equals("int")) {
      mm.return_(0);
    } else if (returnType.equals("boolean")) {
      mm.return_(false);
    } else {
      // Para objetos, retornar null
      mm.return_(null);
    }
  }
}

// Helper: Traducir invocación de método
private static void translateInvoke(AbstractInvokeExpr invokeExpr, MethodMaker mm,
                                    Map<Local, Variable> localToVar) {
  String methodName = invokeExpr.getMethodSignature().getName();

  // Ignorar invocaciones a constructores (<init>)
  if (methodName.equals("<init>")) {
    return;
  }

  List<? extends Immediate> args = invokeExpr.getArgs();
  Object[] argVars = new Object[args.size()];

  for (int i = 0; i < args.size(); i++) {
    Immediate arg = args.get(i);
    if (arg instanceof Local) {
      Local local = (Local) arg;
      Variable v = localToVar.get(local);
      argVars[i] = v;
    } else if (arg instanceof IntConstant) {
      argVars[i] = ((IntConstant) arg).getValue();
    }
  }

  // Obtener el objeto sobre el que se invoca (para métodos instancia)
  if (invokeExpr instanceof JVirtualInvokeExpr) {
    JVirtualInvokeExpr vInvoke = (JVirtualInvokeExpr) invokeExpr;
    Object base = vInvoke.getBase();
    Variable baseVar = getVarFromValue(base, mm, localToVar);
    if (baseVar != null) {
      baseVar.invoke(methodName, argVars);
    }
  } else if (invokeExpr instanceof JSpecialInvokeExpr) {
    JSpecialInvokeExpr sInvoke = (JSpecialInvokeExpr) invokeExpr;
    Object base = sInvoke.getBase();
    Variable baseVar = getVarFromValue(base, mm, localToVar);
    if (baseVar != null) {
      baseVar.invoke(methodName, argVars);
    }
  }
}

// Helper: Value de Jimple a Variable de Maker o resultado de invocación
private static Object getValueOrVarFromValue(Object value, MethodMaker mm, Map<Local, Variable> localToVar) {
  if (value instanceof Local) {
    Local local = (Local) value;
    return localToVar.get(local);
  } else if (value instanceof IntConstant) {
    return ((IntConstant) value).getValue();
  } else if (value instanceof StringConstant) {
    return ((StringConstant) value).getValue();
  } else if (value instanceof JFieldRef) {
    JFieldRef fr = (JFieldRef) value;
    return mm.field(fr.getFieldSignature().getName());
  } else if (value instanceof JVirtualInvokeExpr) {
    JVirtualInvokeExpr vInvoke = (JVirtualInvokeExpr) value;
    String methodName = vInvoke.getMethodSignature().getName();
    Object base = vInvoke.getBase();
    List<? extends Immediate> args = vInvoke.getArgs();

    Variable baseVar = getVarFromValue(base, mm, localToVar);
    Object[] argVars = buildArgArray(args, mm, localToVar);

    if (baseVar != null) {
      return baseVar.invoke(methodName, argVars);
    }
  }
  return null;
}

// Helper: Construir array de argumentos
private static Object[] buildArgArray(List<? extends Immediate> args, MethodMaker mm, Map<Local, Variable> localToVar) {
  Object[] argVars = new Object[args.size()];
  for (int i = 0; i < args.size(); i++) {
    Immediate arg = args.get(i);
    if (arg instanceof Local) {
      Local local = (Local) arg;
      Variable v = localToVar.get(local);
      argVars[i] = v;
    } else if (arg instanceof IntConstant) {
      argVars[i] = ((IntConstant) arg).getValue();
    }
  }
  return argVars;
}

// Helper: Value de Jimple a Variable de Maker
private static Variable getVarFromValue(Object value, MethodMaker mm, Map<Local, Variable> localToVar) {
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
    return mm.field(fr.getFieldSignature().getName());
  }
  return null;
}
}
