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
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.model.*;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
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
    List<JavaSootMethod> methods = new ArrayList<>(originalClass.getMethods());
    Collections.sort(methods, Comparator.comparing(JavaSootMethod::getName));
    for (SootMethod sm : methods) {
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
    Map<Local, Object> localValues = new HashMap<>();

    for (Stmt stmt : stmtGraph.getStmts()) {
      if (stmt instanceof JIdentityStmt) {
        continue;
      } else if (stmt instanceof JAssignStmt assign) {
        processAssignment(assign, mm, localToVar, localValues);
      } else if (stmt instanceof JReturnStmt ret) {
        if (processReturn(ret, mm, localToVar, localValues)) return;
      } else if (stmt instanceof JReturnVoidStmt) {
        mm.return_();
        return;
      } else if (stmt instanceof JInvokeStmt invokeStmt) {
        invokeStmt.getInvokeExpr().ifPresent(expr -> processInvocation(expr, mm, localToVar, localValues));
      }
    }
    
    generateDefaultReturn(mm, returnType);
  }

  private static void processAssignment(JAssignStmt assign, MethodMaker mm, 
                                       Map<Local, Variable> localToVar, Map<Local, Object> localValues) {
    Object left = assign.getLeftOp();
    Object right = assign.getRightOp();

    if (left instanceof Local leftLocal) {
      Object value = getValue(right, mm, localToVar, localValues);
      if (value != null) {
        if (value instanceof Variable var) localToVar.put(leftLocal, var);
        localValues.put(leftLocal, value);
      }
    } else if (left instanceof JFieldRef fr) {
      setFieldValue(mm, fr.getFieldSignature().getName(), right, localToVar, localValues);
    }
  }

  private static void setFieldValue(MethodMaker mm, String fieldName, Object value,
                                   Map<Local, Variable> localToVar, Map<Local, Object> localValues) {
    Object val = getValue(value, mm, localToVar, localValues);
    if (val instanceof Variable var) {
      mm.field(fieldName).set(var);
    }
  }

  private static boolean processReturn(JReturnStmt ret, MethodMaker mm,
                                      Map<Local, Variable> localToVar, Map<Local, Object> localValues) {
    Object value = getValue(ret.getOp(), mm, localToVar, localValues);
    if (value instanceof Variable var) {
      mm.return_(var);
    } else if (value instanceof Integer i) {
      mm.return_(i);
    }
    return true;
  }

  private static void processInvocation(AbstractInvokeExpr expr, MethodMaker mm,
                                       Map<Local, Variable> localToVar, Map<Local, Object> localValues) {
    Object base = getInvokeBase(expr);
    if (base == null) return;
    
    Variable baseVar = (Variable) getValue(base, mm, localToVar, localValues);
    if (baseVar != null) {
      String method = expr.getMethodSignature().getName();
      Object[] args = buildArgArray(expr.getArgs(), mm, localToVar, localValues);
      baseVar.invoke(method, args);
    }
  }

  private static Object getInvokeBase(AbstractInvokeExpr expr) {
    return expr instanceof JVirtualInvokeExpr ve ? ve.getBase() :
           expr instanceof JInterfaceInvokeExpr ie ? ie.getBase() :
           expr instanceof JSpecialInvokeExpr se ? se.getBase() : null;
  }

  private static Object getValue(Object value, MethodMaker mm, 
                                Map<Local, Variable> localToVar, Map<Local, Object> localValues) {
    if (value instanceof Local local) {
      Object cached = localValues.get(local);
      return cached != null ? cached : localToVar.get(local);
    } else if (value instanceof IntConstant ic) {
      return ic.getValue();
    } else if (value instanceof StringConstant sc) {
      return sc.getValue();
    } else if (value instanceof JFieldRef fr) {
      return mm.field(fr.getFieldSignature().getName());
    } else if (value instanceof JVirtualInvokeExpr || value instanceof JInterfaceInvokeExpr) {
      return getInvokeResult((AbstractInvokeExpr) value, mm, localToVar, localValues);
    } else if (value != null && value.getClass().getSimpleName().equals("NullConstant")) {
      // Manejo de NullConstant de Jimple
      return null;
    }
    return null;
  }

  private static Object getInvokeResult(AbstractInvokeExpr expr, MethodMaker mm,
                                       Map<Local, Variable> localToVar, Map<Local, Object> localValues) {
    Object base = getInvokeBase(expr);
    Variable baseVar = (Variable) getValue(base, mm, localToVar, localValues);
    
    if (baseVar != null) {
      String method = expr.getMethodSignature().getName();
      Object[] args = buildArgArray(expr.getArgs(), mm, localToVar, localValues);
      return baseVar.invoke(method, args);
    }
    return null;
  }

  private static void generateDefaultReturn(MethodMaker mm, String returnType) {
    switch (returnType) {
      case "void" -> mm.return_();
      case "int" -> mm.return_(0);
      case "boolean" -> mm.return_(false);
      default -> mm.return_(null);
    }
  }

  // Helper: Construir array de argumentos con soporte a localValues
  private static Object[] buildArgArray(List<? extends Immediate> args, MethodMaker mm, 
                                       Map<Local, Variable> localToVar, Map<Local, Object> localValues) {
    Object[] argVars = new Object[args.size()];
    for (int i = 0; i < args.size(); i++) {
      Immediate arg = args.get(i);
      if (arg instanceof Local local) {
        Object val = localValues.getOrDefault(local, localToVar.get(local));
        argVars[i] = val;
      } else if (arg instanceof IntConstant ic) {
        argVars[i] = ic.getValue();
      }
    }
    return argVars;
  }
}
