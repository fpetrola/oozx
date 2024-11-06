/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.fpetrola.z80.bytecode.refactor;

import com.fpetrola.z80.minizx.JetSetWilly;
import org.objectweb.asm.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class CopyMethodWithFieldParamsTransformer extends ClassVisitor {

    public CopyMethodWithFieldParamsTransformer(ClassVisitor classVisitor) {
        super(ASM9, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodTransformer(mv, access, name, descriptor);
    }

    class MethodTransformer extends MethodVisitor {

        private final String methodName;
        private final String originalDescriptor;
        private final Map<String, String> fieldsToParams = new LinkedHashMap<>(); // Campos leídos en el método original

        public MethodTransformer(MethodVisitor mv, int access, String name, String descriptor) {
            super(ASM9, mv);
            this.methodName = name;
            this.originalDescriptor = descriptor;
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            if (opcode == GETFIELD) {
                // Si es GETFIELD, lo agregamos como parámetro del nuevo método
                fieldsToParams.putIfAbsent(name, descriptor);
            }
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        public void visitEnd() {
            // Crear el nuevo método con el prefijo
            String newMethodName = "prefijo_" + methodName;
            StringBuilder newDescriptor = new StringBuilder("(");

            // Añadir los parámetros originales al nuevo método
            Type methodType = Type.getMethodType(originalDescriptor);
            for (Type argType : methodType.getArgumentTypes()) {
                newDescriptor.append(argType.getDescriptor());
            }

            // Añadir parámetros adicionales para cada campo accedido con GETFIELD
            for (String fieldDescriptor : fieldsToParams.values()) {
                newDescriptor.append(fieldDescriptor);
            }

            newDescriptor.append(")").append(methodType.getReturnType().getDescriptor());

            MethodVisitor newMethod = cv.visitMethod(ACC_PUBLIC, newMethodName, newDescriptor.toString(), null, null);
            newMethod.visitCode();

            // Mapear los índices de los parámetros originales y nuevos
            int paramIndex = methodType.getArgumentTypes().length;  // Índice donde comienzan los nuevos parámetros
            Map<String, Integer> fieldParamIndices = new HashMap<>();
            for (String fieldName : fieldsToParams.keySet()) {
                fieldParamIndices.put(fieldName, paramIndex++);
            }

            // Visitar las instrucciones y reemplazar accesos a field
            mv = newMethod;
            super.visitEnd();  // Terminar de visitar el método original

            // Recorrer nuevamente las instrucciones y copiarlas en el nuevo método
            int finalParamIndex = paramIndex;
            MethodVisitor methodCopyVisitor = new MethodVisitor(ASM9, newMethod) {
                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    if (opcode == GETFIELD && fieldParamIndices.containsKey(name)) {
                        // Reemplazar GETFIELD con carga del parámetro correspondiente
                        int paramIdx = fieldParamIndices.get(name);
                        visitVarInsn(ALOAD, paramIdx);
                    } else if (opcode == PUTFIELD && fieldParamIndices.containsKey(name)) {
                        // Reemplazar PUTFIELD con almacenamiento en una variable local
                        int paramIdx = fieldParamIndices.get(name);
                        int localIndex = finalParamIndex + fieldParamIndices.size();
                        visitVarInsn(ASTORE, localIndex); // Guardar en local variable
                    } else {
                        // Copiar la instrucción tal cual si no es GETFIELD o PUTFIELD
                        super.visitFieldInsn(opcode, owner, name, descriptor);
                    }
                }

                @Override
                public void visitInsn(int opcode) {
                    super.visitInsn(opcode);
                }
            };

            methodCopyVisitor.visitMaxs(0, 0);
            methodCopyVisitor.visitEnd();
        }
    }

    public static void main(String[] args) throws Exception {
        // Leer la clase original
        ClassReader classReader = new ClassReader(JetSetWilly.class.getName());
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        // Aplicar la transformación
        CopyMethodWithFieldParamsTransformer transformer = new CopyMethodWithFieldParamsTransformer(classWriter);
        classReader.accept(transformer, 0);

        // Guardar la clase modificada
        // Guardar el bytecode en un archivo .class

        byte[] bytecode = classWriter.toByteArray();
        Files.write(Path.of("jsw1.class"), bytecode);
    }
}
