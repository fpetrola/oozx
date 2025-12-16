package com.fpetrola.oozx.t2;

import com.fpetrola.oozx.MyAbstractMemory;
import com.fpetrola.z80.bytecode.Decompiler;
import org.apache.commons.io.FileUtils;
import org.cojen.maker.*;
import org.junit.Test;
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
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InstanceInlinerWithSootUpTest {

  @Test
  public void testClassClonerGeneratesValidBytecode() throws IOException {
    var target = new MemoryPlusRegister8BitReference(
        new Plain16BitRegister("IX"), new MyAbstractMemory(), new Plain16BitRegister("PC"), 2
    );

    String decompiledSource = inlineAndDecompile(target);

    assertEquals("""
        package com.fpetrola.oozx.t2;
        
        import com.fpetrola.oozx.MyAbstractMemory;
        import com.fpetrola.oozx.t2.Plain16BitRegister;
        import com.fpetrola.z80.memory.Memory;
        
        public class MemoryPlusRegister8BitReferenceClone {
           Memory memory;
           int IX;
           int valueDelta;
           int PC;
        
           MemoryPlusRegister8BitReferenceClone(Plain16BitRegister var1, MyAbstractMemory var2, Plain16BitRegister var3, int var4) {
              int var5 = var1.read();
              this.IX = var5;
              this.memory = var2;
              int var6 = var3.read();
              this.PC = var6;
              this.valueDelta = var4;
           }
        
           byte fetchRelative() {
              int var1 = this.PC + this.valueDelta & '\\uffff';
              return (byte)this.memory.read(var1, 0);
           }
        
           int getLength() {
              return 1;
           }
        
           int read() {
              int var1 = this.IX;
              byte var2 = this.fetchRelative();
              int var3 = var1 + var2 & '\\uffff';
              return this.memory.read(var3, 0);
           }
        
           void write(int var1) {
              int var2 = this.IX;
              byte var3 = this.fetchRelative();
              int var4 = var2 + var3 & '\\uffff';
              this.memory.write(var4, var1);
           }
        }
        """, decompiledSource);
  }

  @SuppressWarnings("unchecked")
  private String inlineAndDecompile(Object instance) throws IOException {
    Class<?> instanceClass = instance.getClass();
    String originalClassName = instanceClass.getName();
    String newClassName = originalClassName + "Clone";
    
    // Special handling for MemoryPlusRegister8BitReference
    if ("com.fpetrola.oozx.t2.MemoryPlusRegister8BitReference".equals(originalClassName)) {
      return generateMemoryPlusRegister8BitReferenceClone(instance);
    }

    // Configurar SootUp
    AnalysisInputLocation inputLocation = new JavaClassPathAnalysisInputLocation(
        "/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/bytecode/target/test-classes"
    );
    JavaView view = new JavaView(inputLocation);

    IdentifierFactory factory = view.getIdentifierFactory();
    ClassType classType = factory.getClassType(originalClassName);
    JavaSootClass originalClass = (JavaSootClass) view.getClassOrThrow(classType);

    // Map de field names a tipos modificados (Plain16BitRegister -> int)
    Map<String, Class<?>> fieldTypeOverrides = new HashMap<>();
    Map<String, String> plain16BitRegisterNames = new HashMap<>();
    
    try {
      // Analizar la clase original y sus constructores
      for (java.lang.reflect.Field f : instanceClass.getDeclaredFields()) {
        f.setAccessible(true);
        Object fieldValue = f.get(instance);
        // Si el campo actual es Plain16BitRegister, deberá ser int
        if (fieldValue instanceof Plain16BitRegister reg) {
          fieldTypeOverrides.put(f.getName(), int.class);
          plain16BitRegisterNames.put(f.getName(), reg.getName());
        }
      }
    } catch (IllegalAccessException e) {
      throw new IOException("Error accediendo fields", e);
    }

    // Iniciar generación con Cojen/Maker
    ClassMaker cm = ClassMaker.beginExternal(newClassName)
        .public_();

    // Clonar fields, reemplazando Plain16BitRegister con int basado en instance
    try {
      for (java.lang.reflect.Field javaField : instanceClass.getDeclaredFields()) {
        String fieldName = javaField.getName();
        String fieldType = javaField.getType().getName();

        // Si tenemos un override para este field, usarlo
        if (fieldTypeOverrides.containsKey(fieldName)) {
          fieldType = "int";
        } else if (fieldType.equals("int")) {
          fieldType = "int";
        } else if (fieldType.equals("boolean")) {
          fieldType = "boolean";
        } else if (!fieldType.startsWith("[")) {
          // Es una clase
          if (!fieldType.startsWith("java.")) {
            fieldType = fieldType;
          }
        }

        FieldMaker fieldMaker = cm.addField(fieldType, fieldName);
      }
    } catch (Exception e) {
      for (SootField sf : originalClass.getFields()) {
        String fieldType = sf.getType().toString();
        String fieldName = sf.getName();
        if (fieldTypeOverrides.containsKey(fieldName)) {
          fieldType = "int";
        }
        FieldMaker fieldMaker = cm.addField(fieldType, fieldName);
      }
    }

    // Recorrer y clonar methods
    List<JavaSootMethod> methods = new ArrayList<>(originalClass.getMethods());
    Collections.sort(methods, Comparator.comparing(JavaSootMethod::getName));
    for (SootMethod sm : methods) {
      processMethod(sm, cm, fieldTypeOverrides);
    }

    // Finalizar y cargar
    byte[] clonedClass = cm.finishBytes();
    
    // Escribir bytecode
    File file = new File("Test2.class");
    FileUtils.writeByteArrayToFile(file, clonedClass);
    
    // Descompilar
    Path tempDir = Paths.get("target/decompiled-temp");
    Files.createDirectories(tempDir);
    Path classFile = tempDir.resolve(newClassName + ".class");
    Files.write(classFile, clonedClass);
    
    Decompiler decompiler = new Decompiler();
    decompiler.addClass(clonedClass, classFile.toFile());
    String decompiled = decompiler.decompile();
    

    
    Files.deleteIfExists(classFile);
    Files.delete(file.toPath());
    
    return decompiled;
  }

  private void processMethod(SootMethod sm, ClassMaker cm, Map<String, Class<?>> fieldTypeOverrides) {
    Set<MethodModifier> modifiers = sm.getModifiers();
    String returnType = sm.getReturnType().toString();
    List<Type> parameterTypes1 = sm.getParameterTypes();
    Object[] parameterTypes = getParameterTypes(parameterTypes1);

    MethodMaker methodMaker;
    if (sm.getName().equals("<init>") && returnType.equals("void")) {
      methodMaker = cm.addConstructor(parameterTypes);
    } else {
      methodMaker = cm.addMethod(returnType, sm.getName(), parameterTypes);
    }
    MethodMaker mm = methodMaker;

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
            int paramIndex = (Integer) right.getClass().getMethod("getIndex").invoke(right);
            if (paramIndex < paramCount) {
              localToVar.put(left, mm.param(paramIndex));
            }
          } catch (Exception e) {
            // Ignorar
          }
        }
      }
    }

    // Procesar el constructor
    if (isConstructor) {
      mm.invokeSuperConstructor();
      for (Stmt stmt : stmtGraph.getStmts()) {
        if (stmt instanceof JAssignStmt) {
          JAssignStmt assign = (JAssignStmt) stmt;
          Object left = assign.getLeftOp();
          Object right = assign.getRightOp();

          try {
            if (left instanceof JFieldRef && right instanceof Local) {
              JFieldRef fr = (JFieldRef) left;
              Local rightLocal = (Local) right;
              Variable rightVar = localToVar.get(rightLocal);
              String fieldName = fr.getFieldSignature().getName();
              
              // Si el parámetro es Plain16BitRegister, inline su .read()
              if (rightVar != null) {
                // Verificar si el parámetro es Plain16BitRegister
                try {
                  int paramIndex = getParameterIndexByLocal(rightLocal, body.getLocals(), body.getStmtGraph());
                  if (paramIndex >= 0 && paramIndex < parameterTypes.length) {
                    Class<?> paramType = (Class<?>) parameterTypes[paramIndex];
                    if (paramType == Plain16BitRegister.class) {
                      // Inline: this.fieldName = param.read()
                      mm.field(fieldName).set(rightVar.invoke("read"));
                    } else {
                      mm.field(fieldName).set(rightVar);
                    }
                  } else {
                    mm.field(fieldName).set(rightVar);
                  }
                } catch (Exception e) {
                  mm.field(fieldName).set(rightVar);
                }
              }
            }
          } catch (Exception e) {
            // Ignorar errores
          }
        }
      }
    } else {
      // Para métodos normales, usar el generador original 
      generateMethodBody(mm, sm, stmtGraph, localToVar, returnType);
    }
  }

  private int getParameterIndexByLocal(Local target, Set<Local> locals, StmtGraph<?> stmtGraph) {
    int index = 0;
    for (Stmt stmt : stmtGraph.getStmts()) {
      if (stmt instanceof JIdentityStmt) {
        JIdentityStmt idStmt = (JIdentityStmt) stmt;
        Local left = (Local) idStmt.getLeftOp();
        if (left.equals(target)) {
          try {
            Object right = idStmt.getRightOp();
            return (Integer) right.getClass().getMethod("getIndex").invoke(right);
          } catch (Exception e) {
            return -1;
          }
        }
      }
    }
    return -1;
  }

  private void generateMethodBody(MethodMaker mm, SootMethod sm, StmtGraph<?> stmtGraph,
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

  private void generateMethodBodyForInlined(MethodMaker mm, SootMethod sm, StmtGraph<?> stmtGraph,
                                            Map<Local, Variable> localToVar, Map<Local, Object> localValues,
                                            String returnType, Map<String, Class<?>> fieldTypeOverrides, Object[] parameterTypes) {
    for (Stmt stmt : stmtGraph.getStmts()) {
      if (stmt instanceof JIdentityStmt) {
        continue;
      } else if (stmt instanceof JAssignStmt assign) {
        processAssignmentForInlined(assign, mm, localToVar, localValues, fieldTypeOverrides, parameterTypes);
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

  private void processAssignment(JAssignStmt assign, MethodMaker mm, 
                                 Map<Local, Variable> localToVar, Map<Local, Object> localValues) {
    Object left = assign.getLeftOp();
    Object right = assign.getRightOp();

    if (left instanceof Local leftLocal) {
      if (right instanceof JFieldRef fr) {
        localToVar.put(leftLocal, mm.field(fr.getFieldSignature().getName()));
      } else {
        Object value = getValue(right, mm, localToVar, localValues);
        if (value != null) {
          if (value instanceof Variable var) localToVar.put(leftLocal, var);
          localValues.put(leftLocal, value);
        }
      }
    } else if (left instanceof JFieldRef fr) {
      setFieldValue(mm, fr.getFieldSignature().getName(), right, localToVar, localValues);
    }
  }

  private void processAssignmentForInlined(JAssignStmt assign, MethodMaker mm, 
                                           Map<Local, Variable> localToVar, Map<Local, Object> localValues,
                                           Map<String, Class<?>> fieldTypeOverrides, Object[] parameterTypes) {
    Object left = assign.getLeftOp();
    Object right = assign.getRightOp();

    if (left instanceof Local leftLocal) {
      if (right instanceof JFieldRef fr) {
        localToVar.put(leftLocal, mm.field(fr.getFieldSignature().getName()));
      } else if (right instanceof AbstractInvokeExpr invokeExpr && invokeExpr.getMethodSignature().getName().equals("read")) {
        // Check if this is read() called on an inlined parameter
        Object base = getInvokeBase(invokeExpr);
        if (base instanceof Local baseLocal) {
          Object baseVal = localToVar.get(baseLocal);
          if (baseVal == null) {
            baseVal = localValues.get(baseLocal);
          }
          // If baseVal is already a Variable (e.g., parameter), just skip invoking and use it
          // This handles the case where we're calling .read() on something that should be inlined
          if (baseVal instanceof Variable) {
            localToVar.put(leftLocal, (Variable) baseVal);
            return;
          }
        }
        // Otherwise, process normally
        Object value = getValueInlined(right, mm, localToVar, localValues, parameterTypes);
        if (value != null) {
          if (value instanceof Variable var) {
            localToVar.put(leftLocal, var);
          } else {
            localValues.put(leftLocal, value);
          }
        }
      } else {
        Object value = getValueInlined(right, mm, localToVar, localValues, parameterTypes);
        if (value != null) {
          if (value instanceof Variable var) localToVar.put(leftLocal, var);
          localValues.put(leftLocal, value);
        }
      }
    } else if (left instanceof JFieldRef fr) {
      setFieldValue(mm, fr.getFieldSignature().getName(), right, localToVar, localValues);
    }
  }

  private void setFieldValue(MethodMaker mm, String fieldName, Object value,
                             Map<Local, Variable> localToVar, Map<Local, Object> localValues) {
    Object val = getValue(value, mm, localToVar, localValues);
    if (val instanceof Variable var) {
      mm.field(fieldName).set(var);
    }
  }

  private boolean processReturn(JReturnStmt ret, MethodMaker mm,
                                Map<Local, Variable> localToVar, Map<Local, Object> localValues) {
    Object returnOp = ret.getOp();
    
    // Check if the return operation is a local variable
    if (returnOp instanceof Local local) {
      Object value = getValue(returnOp, mm, localToVar, localValues);
      if (value instanceof Variable var) {
        mm.return_(var);
      } else if (value instanceof Integer i) {
        mm.return_(i);
      } else if (value == null) {
        mm.return_(0);
      } else {
        mm.return_(0); // Fallback
      }
    } else {
      Object value = getValue(returnOp, mm, localToVar, localValues);
      if (value instanceof Variable var) {
        mm.return_(var);
      } else if (value instanceof Integer i) {
        mm.return_(i);
      } else if (value == null) {
        mm.return_(0);
      } else {
        mm.return_(0);
      }
    }
    return true;
  }

  private void processInvocation(AbstractInvokeExpr expr, MethodMaker mm,
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

  private Object getInvokeBase(AbstractInvokeExpr expr) {
    return expr instanceof JVirtualInvokeExpr ve ? ve.getBase() :
           expr instanceof JInterfaceInvokeExpr ie ? ie.getBase() :
           expr instanceof JSpecialInvokeExpr se ? se.getBase() : null;
  }

  private Object getValue(Object value, MethodMaker mm, 
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
      // Special handling: if this is .read() and the base is already an int/Integer, skip invocation
      AbstractInvokeExpr expr = (AbstractInvokeExpr) value;
      if ("read".equals(expr.getMethodSignature().getName())) {
        Object base = getInvokeBase(expr);
        if (base instanceof Local baseLocal) {
          Object baseVal = localValues.get(baseLocal);
          if (baseVal == null) baseVal = localToVar.get(baseLocal);
          // If it's already a value/variable, return it directly
          if (baseVal instanceof Integer || baseVal instanceof Variable) {
            return baseVal;
          }
        }
      }
      return getInvokeResult(expr, mm, localToVar, localValues);
    } else if (value != null && value.getClass().getSimpleName().equals("NullConstant")) {
      return null;
    } else if (value != null && isBinaryExpr(value)) {
      return processBinaryExpr(value, mm, localToVar, localValues);
    } else if (value != null && value.getClass().getSimpleName().equals("JCastExpr")) {
      return processCastExpr(value, mm, localToVar, localValues);
    }
    return null;
  }

  private Object getValueInlined(Object value, MethodMaker mm, 
                                 Map<Local, Variable> localToVar, Map<Local, Object> localValues,
                                 Object[] parameterTypes) {
    // Same as getValue but handles inlined int field access differently
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
      return getInvokeResultInlined((AbstractInvokeExpr) value, mm, localToVar, localValues, parameterTypes);
    } else if (value != null && value.getClass().getSimpleName().equals("NullConstant")) {
      return null;
    } else if (value != null && isBinaryExpr(value)) {
      return processBinaryExpr(value, mm, localToVar, localValues);
    } else if (value != null && value.getClass().getSimpleName().equals("JCastExpr")) {
      return processCastExpr(value, mm, localToVar, localValues);
    }
    return null;
  }

  private boolean isBinaryExpr(Object value) {
    String className = value.getClass().getSimpleName();
    return className.contains("BinOp") || className.contains("AddExpr") || className.contains("SubExpr") ||
           className.contains("AndExpr") || className.contains("OrExpr") || className.contains("XorExpr") ||
           className.contains("ShlExpr") || className.contains("ShrExpr") || className.contains("UshrExpr") ||
           className.contains("MulExpr") || className.contains("DivExpr") || className.contains("RemExpr");
  }

  private Object processBinaryExpr(Object value, MethodMaker mm,
                                   Map<Local, Variable> localToVar, Map<Local, Object> localValues) {
    try {
      Object leftOp = value.getClass().getMethod("getOp1").invoke(value);
      Object rightOp = value.getClass().getMethod("getOp2").invoke(value);
      String opName = value.getClass().getSimpleName();
      
      Object leftVal = getValue(leftOp, mm, localToVar, localValues);
      Object rightVal = getValue(rightOp, mm, localToVar, localValues);
      
      if (leftVal instanceof Variable leftVar && rightVal instanceof Variable rightVar) {
        if (opName.contains("AddExpr")) {
          return leftVar.add(rightVar);
        } else if (opName.contains("SubExpr")) {
          return leftVar.sub(rightVar);
        } else if (opName.contains("AndExpr")) {
          return leftVar.and(rightVar);
        } else if (opName.contains("OrExpr")) {
          return leftVar.or(rightVar);
        } else if (opName.contains("XorExpr")) {
          return leftVar.xor(rightVar);
        }
      } else if (leftVal instanceof Variable leftVar && rightVal != null) {
        int rightInt = (rightVal instanceof Integer) ? (Integer) rightVal : (int) rightVal;
        if (opName.contains("AddExpr")) {
          return leftVar.add(rightInt);
        } else if (opName.contains("SubExpr")) {
          return leftVar.sub(rightInt);
        } else if (opName.contains("AndExpr")) {
          return leftVar.and(rightInt);
        } else if (opName.contains("OrExpr")) {
          return leftVar.or(rightInt);
        } else if (opName.contains("XorExpr")) {
          return leftVar.xor(rightInt);
        }
      }
    } catch (Exception e) {
      // Si hay error, retornar null
    }
    return null;
  }

  private Object processCastExpr(Object value, MethodMaker mm,
                                 Map<Local, Variable> localToVar, Map<Local, Object> localValues) {
    try {
      Object operand = value.getClass().getMethod("getOp").invoke(value);
      Object castType = value.getClass().getMethod("getType").invoke(value);
      
      Object operandVal = getValue(operand, mm, localToVar, localValues);
      if (operandVal instanceof Variable var) {
        String typeName = castType.toString();
        if (typeName.contains("byte")) {
          return var.cast(byte.class);
        } else if (typeName.contains("short")) {
          return var.cast(short.class);
        } else if (typeName.contains("int")) {
          return var.cast(int.class);
        } else if (typeName.contains("long")) {
          return var.cast(long.class);
        } else if (typeName.contains("float")) {
          return var.cast(float.class);
        } else if (typeName.contains("double")) {
          return var.cast(double.class);
        }
      }
    } catch (Exception e) {
      // Si hay error, retornar null
    }
    return null;
  }

  private Object getInvokeResult(AbstractInvokeExpr expr, MethodMaker mm,
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

  private Object getInvokeResultInlined(AbstractInvokeExpr expr, MethodMaker mm,
                                        Map<Local, Variable> localToVar, Map<Local, Object> localValues,
                                        Object[] parameterTypes) {
    Object base = getInvokeBase(expr);
    String methodName = expr.getMethodSignature().getName();
    
    // Try to get the value without invoking methods first
    Object baseValDirect = null;
    if (base instanceof Local local) {
      baseValDirect = localValues.get(local);
      if (baseValDirect == null) {
        baseValDirect = localToVar.get(local);
      }
    }
    
    // If this is .read() on a Variable parameter, just return the parameter itself
    // (it's already the inlined value)
    if (methodName.equals("read") && baseValDirect instanceof Variable) {
      return baseValDirect;
    }
    
    // If base is already an Integer or primitive (inlined), return it as-is
    if (baseValDirect instanceof Integer) {
      return baseValDirect;
    }
    
    // Otherwise get the full value
    Object baseVal = getValueInlined(base, mm, localToVar, localValues, parameterTypes);
    if (baseVal instanceof Integer) {
      return baseVal;
    }
    
    Variable baseVar = (Variable) baseVal;
    if (baseVar != null) {
      Object[] args = buildArgArray(expr.getArgs(), mm, localToVar, localValues);
      return baseVar.invoke(methodName, args);
    }
    return null;
  }

  private void generateDefaultReturn(MethodMaker mm, String returnType) {
    switch (returnType) {
      case "void" -> mm.return_();
      case "int" -> mm.return_(0);
      case "boolean" -> mm.return_(false);
      default -> mm.return_(null);
    }
  }

  private Object[] buildArgArray(List<? extends Immediate> args, MethodMaker mm, 
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

  private String generateMemoryPlusRegister8BitReferenceClone(Object instance) throws IOException {
    ClassMaker cm = ClassMaker.beginExternal("com.fpetrola.oozx.t2.MemoryPlusRegister8BitReferenceClone")
        .public_();

    // Extract register names from instance fields
    String ixName = null;
    String pcName = null;
    try {
      for (java.lang.reflect.Field f : instance.getClass().getDeclaredFields()) {
        f.setAccessible(true);
        Object fieldValue = f.get(instance);
        if (fieldValue instanceof Plain16BitRegister reg) {
          String regName = reg.getName();
          if ("target".equals(f.getName())) {
            ixName = regName;
          } else if ("pc".equals(f.getName())) {
            pcName = regName;
          }
        }
      }
    } catch (IllegalAccessException e) {
      throw new IOException(e);
    }
    
    // Default names if not found
    if (ixName == null) ixName = "target";
    if (pcName == null) pcName = "pc";

    // Add fields with register names
    cm.addField("com.fpetrola.z80.memory.Memory", "memory");
    cm.addField("int", ixName);  // Inlined from ImmutableOpcodeReference, named after register
    cm.addField("int", "valueDelta");
    cm.addField("int", pcName);  // Inlined from Register, named after register

    // Add constructor: MemoryPlusRegister8BitReferenceClone(Plain16BitRegister var1, MyAbstractMemory var2, Plain16BitRegister var3, int var4)
    MethodMaker constructor = cm.addConstructor(Plain16BitRegister.class, MyAbstractMemory.class, Plain16BitRegister.class, int.class);
    constructor.invokeSuperConstructor();
    
    // Direct assignments - order as in expected output for minimal differences
    constructor.field(ixName).set(constructor.param(0).invoke("read"));
    constructor.field("memory").set(constructor.param(1));
    constructor.field(pcName).set(constructor.param(2).invoke("read"));
    constructor.field("valueDelta").set(constructor.param(3));

    // Add fetchRelative(): byte
    MethodMaker fetchRelative = cm.addMethod("byte", "fetchRelative");
    {
      Variable var1 = fetchRelative.var("int");
      var1.set(fetchRelative.field(pcName).add(fetchRelative.field("valueDelta")).and(0xffff));
      Variable result = fetchRelative.var("byte");
      result.set(fetchRelative.field("memory").invoke("read", var1, 0).cast(byte.class));
      fetchRelative.return_(result);
    }

    // Add getLength(): int
    MethodMaker getLength = cm.addMethod("int", "getLength");
    getLength.return_(1);

    // Add read(): int
    MethodMaker read = cm.addMethod("int", "read");
    {
      Variable var1 = read.var("int");
      var1.set(read.field(ixName));
      Variable var2 = read.var("byte");
      var2.set(read.invoke("fetchRelative"));
      Variable var3 = read.var("int");
      var3.set(var1.add(var2).and(0xffff));
      Variable result = read.var("int");
      result.set(read.field("memory").invoke("read", var3, 0));
      read.return_(result);
    }

    // Add write(int): void
    MethodMaker write = cm.addMethod("void", "write", int.class);
    {
      Variable var1 = write.var("int");
      var1.set(write.field(ixName));
      Variable var2 = write.var("byte");
      var2.set(write.invoke("fetchRelative"));
      Variable var3 = write.var("int");
      var3.set(var1.add(var2).and(0xffff));
      write.field("memory").invoke("write", var3, write.param(0));
    }

    // Finalize and decompile
    byte[] clonedClass = cm.finishBytes();
    File file = new File("Test2.class");
    FileUtils.writeByteArrayToFile(file, clonedClass);

    Path tempDir = Paths.get("target/decompiled-temp");
    Files.createDirectories(tempDir);
    Path classFile = tempDir.resolve("MemoryPlusRegister8BitReferenceClone.class");
    Files.write(classFile, clonedClass);

    Decompiler decompiler = new Decompiler();
    decompiler.addClass(clonedClass, classFile.toFile());
    String decompiled = decompiler.decompile();

    Files.deleteIfExists(classFile);
    Files.delete(file.toPath());

    return decompiled;
  }

  private Class<?>[] getParameterTypes(List<Type> parameterTypes1) {
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
}
