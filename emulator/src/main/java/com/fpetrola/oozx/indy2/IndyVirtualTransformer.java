// src/main/java/agent/IndyVirtualTransformer.java (igual que antes, pero lo hacemos reutilizable)
package com.fpetrola.oozx.indy2;

import org.objectweb.asm.*;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class IndyVirtualTransformer implements ClassFileTransformer {

    private static final String BOOTSTRAP_CLASS = "agent/IndyBootstrap";
    private static final Handle BOOTSTRAP_HANDLE = new Handle(
            Opcodes.H_INVOKESTATIC,
            BOOTSTRAP_CLASS,
            "bootstrapVirtual",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/invoke/CallSite;",
            false);

    public byte[] transform(String className, byte[] classfileBuffer) {
        if (className.startsWith("java/") || className.startsWith("jdk/") || className.startsWith("sun/") ||
            className.startsWith("agent/")) {  // Evitamos tocarnos a nosotros mismos
            return classfileBuffer;
        }

        try {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, writer) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String mname, String mdesc, boolean itf) {
                            if (opcode == Opcodes.INVOKEVIRTUAL && !itf) {
                                super.visitInvokeDynamicInsn(mname, mdesc, BOOTSTRAP_HANDLE, owner, mname, mdesc);
                            } else {
                                super.visitMethodInsn(opcode, owner, mname, mdesc, itf);
                            }
                        }
                    };
                }
            };
            reader.accept(cv, ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        } catch (Throwable t) {
            System.err.println("Error transformando " + className + ": " + t);
            return classfileBuffer;
        }
    }

    // Implementación del interface para compatibilidad con agent si se quiere
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        return transform(className.replace('.', '/'), classfileBuffer);
    }
}