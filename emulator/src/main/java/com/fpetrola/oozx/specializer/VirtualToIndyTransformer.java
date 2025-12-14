package com.fpetrola.oozx.specializer;

import org.objectweb.asm.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.security.ProtectionDomain;

public class VirtualToIndyTransformer {

    /**
     * Crea una versión especializada de la instancia: genera una subclase donde
     * todas las llamadas virtuales al tipo concreto se convierten en invokedynamic
     * cableadas directamente al método real de la instancia.
     */
    public static <T> T specialize(T original) {
        if (original == null) throw new IllegalArgumentException("Instance cannot be null");

        Class<?> concreteClass = original.getClass();
        String originalName = concreteClass.getName();
        String specializedName = originalName + "$Specialized$" + System.nanoTime();

        byte[] transformedBytes = transformClass(concreteClass, original);

        // Cargar la nueva clase
        ClassLoader loader = concreteClass.getClassLoader();
        Class<?> specializedClass = new CustomClassLoader(loader)
                .defineClass(specializedName.replace('.', '/'), transformedBytes);

        try {
            // Instanciar y copiar estado
            T specialized = (T) specializedClass.getDeclaredConstructor().newInstance();
            copyFields(original, specialized);
            return specialized;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate specialized class", e);
        }
    }

    private static byte[] transformClass(Class<?> concreteClass, Object instance) {
        ClassReader cr = new ClassReader(getClassBytes(concreteClass));
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        String internalName = Type.getInternalName(concreteClass);
        String specializedInternalName = internalName + "$Specialized$" + System.nanoTime();

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                // Subclase final
                super.visit(version, access | Opcodes.ACC_FINAL, specializedInternalName,
                        signature, name, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                // Solo transformamos métodos de instancia (no estáticos ni <init>/<clinit>)
                if ((access & Opcodes.ACC_STATIC) != 0 || name.equals("<init>") || name.equals("<clinit>")) {
                    return mv;
                }

                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        // Solo reemplazamos invokevirtual/invokeinterface si el receiver es compatible con nuestra instancia
                        if ((opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE)
                                && isAssignableFrom(concreteClass, Type.getObjectType(owner).getClassName())) {

                            // Crear invokedynamic con el mismo nombre y descriptor
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

                            // Pasamos la instancia como argumento estático del bootstrap
                            mv.visitInvokeDynamicInsn(
                                    name,
                                    descriptor,
                                    bsm,
                                    instance  // ¡aquí va la instancia real!
                            );
                        } else {
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        }
                    }
                };
            }
        };

        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }

    private static boolean isAssignableFrom(Class<?> superType, String subclassName) {
        try {
            Class<?> subclass = Class.forName(subclassName.replace('/', '.'), false, superType.getClassLoader());
            return superType.isAssignableFrom(subclass);
        } catch (Throwable t) {
            return false;
        }
    }

    private static byte[] getClassBytes(Class<?> clazz) {
        String resource = clazz.getName().replace('.', '/') + ".class";
        try (var is = clazz.getClassLoader().getResourceAsStream(resource)) {
            if (is == null) throw new IllegalStateException("Cannot find class bytes");
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> void copyFields(T source, T target) throws IllegalAccessException {
        Class<?> clazz = source.getClass();
        while (clazz != Object.class) {
            for (Field f : clazz.getDeclaredFields()) {
                if ((f.getModifiers() & (java.lang.reflect.Modifier.STATIC | java.lang.reflect.Modifier.FINAL)) == 0) {
                    f.setAccessible(true);
                    f.set(target, f.get(source));
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    // Clase auxiliar para cargar la clase generada
    private static class CustomClassLoader extends ClassLoader {
        public CustomClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> defineClass(String name, byte[] b) {
            String replace = name.replace('/', '.');
            return defineClass(replace, b, 0, b.length);
        }
    }

    // === BOOTSTRAP METHOD ===
    public static class Bootstrap {
        public static CallSite bootstrap(MethodHandles.Lookup lookup,
                                         String name,
                                         MethodType type,
                                         Object instance) throws Throwable {

            Class<?> receiverClass = instance.getClass();

            // Buscamos el método concreto en la clase real
            MethodHandle target = lookup.findVirtual(
                    receiverClass,
                    name,
                    type.dropParameterTypes(0, 1)  // quita el receiver
            ).bindTo(instance);

            // Convertimos al tipo exacto del callsite
            target = target.asType(type);

            // ¡CONSTANT! → JIT inlinea al máximo
            return new ConstantCallSite(target);
        }
    }
}