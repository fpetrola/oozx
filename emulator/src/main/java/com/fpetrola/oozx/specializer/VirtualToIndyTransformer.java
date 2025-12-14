package com.fpetrola.oozx.specializer;

import org.objectweb.asm.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;

public class VirtualToIndyTransformer {

  private static int l = 1;
  private static void saveBytecode(String className, byte[] bytecode) {
    try {
      Path OUTPUT_DIR= Path.of("specialized_classes");
      Files.createDirectories(OUTPUT_DIR);

      // Convertir nombre de clase a ruta: com.example.MyClass -> com/example/MyClass.class
      String filePath = className.replace('.', '/') + ".class";
      Path outputPath = OUTPUT_DIR.resolve(filePath);

      // Crear directorios si no existen
      Files.createDirectories(outputPath.getParent());

      Files.write(outputPath, bytecode);
      System.out.println("[VirtualToIndyTransformer] Bytecode guardado: " + outputPath.toAbsolutePath());
    } catch (Exception e) {
      System.err.println("[VirtualToIndyTransformer] Error guardando bytecode: " + e.getMessage());
      e.printStackTrace();
    }
  }
  /**
   * Crea una versión especializada de la instancia: genera una subclase donde
   * todas las llamadas virtuales se convierten en invokedynamic.
   * 
   * - Genera una subclase de la clase original
   * - Copia todos los métodos públicos del padre
   * - Transforma TODOS los invokevirtual/invokeinterface en invokedynamic
   *   que apunten a tipos asignables desde la instancia original
   * - El bootstrap vincula directamente al método concreto con bindTo(instance)
   * - ConstantCallSite permite que el JIT inline todo
   */
  public static <T> T specialize(T original) {
    if (original == null) throw new IllegalArgumentException("Instance cannot be null");

    Class<?> concreteClass = original.getClass();
    String originalName = concreteClass.getName();
    String specializedName = getSpecializedName(originalName);

    byte[] transformedBytes = transformClass(concreteClass, original, specializedName);

    saveBytecode(specializedName, transformedBytes);
    ClassLoader loader = concreteClass.getClassLoader();
    Class<?> specializedClass = new CustomClassLoader(loader)
        .defineClass(specializedName.replace('.', '/'), transformedBytes);

    try {
      T specialized = (T) specializedClass.getDeclaredConstructor().newInstance();
      copyFields(original, specialized);
      return specialized;
    } catch (Exception e) {
      throw new RuntimeException("Failed to instantiate specialized class", e);
    }
  }

  private static String getSpecializedName(String originalName) {
    return originalName + "$Specialized$" + (l++);
  }

  private static byte[] transformClass(Class<?> concreteClass, Object instance, String specializedName) {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

    String internalName = Type.getInternalName(concreteClass);
    String specializedInternalName = specializedName.replace('.', '/');

    // Crear la clase especializada que extiende la original
    cw.visit(
        Opcodes.V11,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
        specializedInternalName,
        null,
        internalName,
        null
    );

    // Constructor sin argumentos
    {
      MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
      mv.visitCode();
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, internalName, "<init>", "()V", false);
      mv.visitInsn(Opcodes.RETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }

    // Leer bytecode original y copiar/transformar métodos
    ClassReader cr = new ClassReader(getClassBytes(concreteClass));
    
    ClassVisitor methodCopier = new ClassVisitor(Opcodes.ASM9, cw) {
      @Override
      public void visit(int version, int access, String name, String signature,
                        String superName, String[] interfaces) {
        // Saltar este visit - ya creamos la clase arriba
      }

      @Override
      public MethodVisitor visitMethod(int access, String name, String descriptor,
                                       String signature, String[] exceptions) {
        // Saltar constructores y métodos estáticos/privados
        if (name.equals("<init>") || name.equals("<clinit>") || 
            Modifier.isPrivate(access) || Modifier.isStatic(access)) {
          return null;
        }

        // Crear el método en la subclase
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        
        // Envolver para transformar invokevirtual/invokeinterface en invokedynamic
        return new MethodVisitor(Opcodes.ASM9, mv) {
          @Override
          public void visitMethodInsn(int opcode, String owner, String methodName, 
                                      String methodDesc, boolean isInterface) {
            // Transformar invokevirtual e invokeinterface en invokedynamic
            // si el owner es compatible con la clase original
            if ((opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE) &&
                isAssignableFrom(concreteClass, Type.getObjectType(owner).getClassName())) {

              // Bootstrap method que vinculará la llamada a la instancia real
              Handle bsm = new Handle(
                  Opcodes.H_INVOKESTATIC,
                  Type.getInternalName(Bootstrap.class),
                  "bootstrap",
                  MethodType.methodType(
                      CallSite.class,
                      MethodHandles.Lookup.class,
                      String.class,
                      MethodType.class,
                      Object.class
                  ).toMethodDescriptorString(),
                  false
              );

              // Crear invokedynamic que pasará la instancia como constante
              mv.visitInvokeDynamicInsn(methodName, methodDesc, bsm, instance);
            } else {
              super.visitMethodInsn(opcode, owner, methodName, methodDesc, isInterface);
            }
          }
        };
      }
    };

    // Copiar métodos transformados del bytecode original
    cr.accept(methodCopier, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

    cw.visitEnd();
    return cw.toByteArray();
  }

  private static boolean isAssignableFrom(Class<?> superType, String subclassName) {
    try {
      Class<?> subclass = Class.forName(subclassName.replace('/', '.'), false, 
          superType.getClassLoader());
      return superType.isAssignableFrom(subclass);
    } catch (Throwable t) {
      return false;
    }
  }

  private static byte[] getClassBytes(Class<?> clazz) {
    String resource = clazz.getName().replace('.', '/') + ".class";
    try (var is = clazz.getClassLoader().getResourceAsStream(resource)) {
      if (is == null) throw new IllegalStateException("Cannot find class bytes for " + clazz);
      return is.readAllBytes();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static <T> void copyFields(T source, T target) throws IllegalAccessException {
    Class<?> clazz = source.getClass();
    while (clazz != Object.class) {
      for (Field f : clazz.getDeclaredFields()) {
        // Copiar campos no-static (incluyendo final)
        if ((f.getModifiers() & Modifier.STATIC) == 0) {
          f.setAccessible(true);
          Object value = f.get(source);
          // Usar reflection para setear incluso campos final
          try {
            f.set(target, value);
          } catch (IllegalAccessException e) {
            // Si es final, intentar via Unsafe (ignorar si falla)
          }
        }
      }
      clazz = clazz.getSuperclass();
    }
  }

  private static class CustomClassLoader extends ClassLoader {
    public CustomClassLoader(ClassLoader parent) {
      super(parent);
    }

    public Class<?> defineClass(String name, byte[] b) {
      return defineClass(name.replace('/', '.'), b, 0, b.length);
    }
  }

  /**
   * Bootstrap method para invokedynamic.
   * 
   * Vincula el callsite a la implementación real en la instancia:
   * - Resuelve el método en la clase concreta
   * - Lo bindea a la instancia específica
   * - Usa ConstantCallSite para que el JIT sepa que nunca cambia
   */
  public static class Bootstrap {
    public static CallSite bootstrap(MethodHandles.Lookup lookup,
                                     String methodName,
                                     MethodType callType,
                                     Object instance) throws Throwable {

      Class<?> concreteClass = instance.getClass();

      // Resolver el método virtual en la clase concreta
      MethodHandle target = lookup.findVirtual(
          concreteClass,
          methodName,
          callType.dropParameterTypes(0, 1)  // Remove receiver type
      ).bindTo(instance);  // Bindearlo a esta instancia específica

      // Adaptar tipos si es necesario
      target = target.asType(callType);

      // ConstantCallSite = el callsite nunca cambia → JIT puede inline y optimizar agresivamente
      return new ConstantCallSite(target);
    }
  }
}
