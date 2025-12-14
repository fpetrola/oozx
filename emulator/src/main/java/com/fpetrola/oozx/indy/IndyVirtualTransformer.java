// src/main/java/agent/IndyVirtualTransformer.java
package com.fpetrola.oozx.indy;

import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.invoke.ConstantCallSite;
import java.security.ProtectionDomain;

public class IndyVirtualTransformer implements ClassFileTransformer {

    private static final String BOOTSTRAP_CLASS = "agent/IndyBootstrap";
    private static final Handle BOOTSTRAP_HANDLE = new Handle(
            Opcodes.H_INVOKESTATIC,
            BOOTSTRAP_CLASS,
            "bootstrapVirtual",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/invoke/CallSite;",
            false);

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {

        if (className.startsWith("java/") || className.startsWith("jdk/") || className.startsWith("sun/")) {
            return null; // No tocar clases del JDK
        }

        try {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, writer) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            if (opcode == Opcodes.INVOKEVIRTUAL && !isInterface) {
                                // Convertimos invokevirtual -> invokedynamic

                                // Descriptor del invokedynamic: mismo que invokevirtual (receiver + args)
                                String indyDesc = descriptor;

                                super.visitInvokeDynamicInsn(
                                        name,
                                        indyDesc,
                                        BOOTSTRAP_HANDLE,
                                        owner,          // arg0: owner
                                        name,           // arg1: method name
                                        descriptor      // arg2: method desc
                                );
                            } else {
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        }
                    };
                }
            };
            reader.accept(cv, ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        } catch (Throwable t) {
            // En caso de error, devolvemos la clase original
            return classfileBuffer;
        }
    }
}