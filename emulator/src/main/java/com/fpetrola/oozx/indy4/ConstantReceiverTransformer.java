package com.fpetrola.oozx.indy4;

import org.objectweb.asm.*;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.*;

public class ConstantReceiverTransformer implements ClassFileTransformer {

    private static final Map<Object, Map<String, Class<?>>> instanceFieldTypes = Helper.instanceFieldTypes;
    private static volatile Class<?> classToTransform = null;

    public static void triggerRetransform(Class<?> clazz) {
        classToTransform = clazz;
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {

        if (classToTransform == null || classBeingRedefined != classToTransform) {
            return null;
        }

        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    return new DevirtualizingMethodVisitor(mv, classBeingRedefined);
                }
            };
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
            return cw.toByteArray();
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        } finally {
            classToTransform = null; // solo una vez
        }
    }

    private static class DevirtualizingMethodVisitor extends MethodVisitor {
        private final Class<?> ownerClass;
        private final List<ReceiverInfo> stack = new ArrayList<>();
        private final Map<Integer, ReceiverInfo> locals = new HashMap<>();

        public DevirtualizingMethodVisitor(MethodVisitor mv, Class<?> ownerClass) {
            super(Opcodes.ASM9, mv);
            this.ownerClass = ownerClass;
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            super.visitFieldInsn(opcode, owner, name, desc);

            if (opcode == Opcodes.GETFIELD && owner.equals(Type.getInternalName(ownerClass))) {
                // Alguien hizo: this.field
                synchronized (instanceFieldTypes) {
                    for (Map.Entry<Object, Map<String, Class<?>>> entry : instanceFieldTypes.entrySet()) {
                        Object instance = entry.getKey();
                        if (ownerClass.isInstance(instance)) {
                            Map<String, Class<?>> fields = entry.getValue();
                            Class<?> concrete = fields.get(name);
                            if (concrete != null) {
                                stack.add(new ReceiverInfo(concrete, instance));
                                return;
                            }
                        }
                    }
                }
                stack.add(null); // desconocido
            } else {
                stack.add(null);
            }
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            super.visitVarInsn(opcode, var);
            if (opcode == Opcodes.ALOAD) {
                ReceiverInfo info = locals.get(var);
                stack.add(info != null ? info : null);
            }
        }

        @Override
        public void visitInsn(int opcode) {
            super.visitInsn(opcode);
            if (opcode == Opcodes.DUP) {
                if (!stack.isEmpty()) {
                    stack.add(stack.get(stack.size() - 1));
                }
            } else if (opcode >= Opcodes.POP && opcode <= Opcodes.POP2) {
                for (int i = 0; i < (opcode - Opcodes.POP + 1); i++) {
                    if (!stack.isEmpty()) stack.remove(stack.size() - 1);
                }
            }
            // otros opcodes como SWAP, DUP_X1, etc. se pueden agregar si es necesario
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == Opcodes.INVOKEVIRTUAL && !stack.isEmpty()) {
                ReceiverInfo receiver = stack.remove(stack.size() - 1);
                if (receiver != null && receiver.concreteType != null) {
                    // ¡Podemos devirtualizar!
                    Type[] argTypes = Type.getArgumentTypes(desc);
                    Type returnType = Type.getReturnType(desc);
                    Type[] newArgTypes = new Type[argTypes.length + 1];
                    newArgTypes[0] = Type.getObjectType(Type.getInternalName(receiver.concreteType));
                    System.arraycopy(argTypes, 0, newArgTypes, 1, argTypes.length);
                    String newDesc = Type.getMethodDescriptor(returnType, newArgTypes);

                    Handle bsm = new Handle(Opcodes.H_INVOKESTATIC,
                            Type.getInternalName(Bootstrap.class),
                            "bootstrap",
                            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/CallSite;",
                            false);

                    Object[] bsmArgs = { receiver.concreteType };

                    super.visitInvokeDynamicInsn(name, newDesc, bsm, bsmArgs);
                    return;
                }
            }

            // Si no podemos devirtualizar, o no es invokevirtual, ejecutamos normal
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    private static class ReceiverInfo {
        final Class<?> concreteType;
        final Object instance; // opcional, si quieres bindear

        ReceiverInfo(Class<?> concreteType, Object instance) {
            this.concreteType = concreteType;
            this.instance = instance;
        }
    }
}