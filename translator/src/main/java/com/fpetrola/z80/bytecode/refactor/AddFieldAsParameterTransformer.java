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

import com.fpetrola.z80.bytecode.tests.JetSetWilly;
import org.objectweb.asm.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class AddFieldAsParameterTransformer extends ClassVisitor {

  private final MethodFieldsUsageCollector methodInfo;

  public AddFieldAsParameterTransformer(ClassVisitor classVisitor, MethodFieldsUsageCollector methodInfo) {
    super(ASM9, classVisitor);
    this.methodInfo = methodInfo;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    if (name.equals("$38601")) {
      MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
      return new MethodTransformer(mv, access, name, descriptor);
    } else
      return super.visitMethod(access, name, descriptor, signature, exceptions);
  }

  class MethodTransformer extends MethodVisitor {

    private final Map<String, String> fieldsToAddAsParams = new LinkedHashMap<>(); // Para mantener el orden de los campos
    private final String name;
    private final String descriptor;

    public MethodTransformer(MethodVisitor mv, int access, String name, String descriptor) {
      super(ASM9, mv);
      this.name = name;
      this.descriptor = descriptor;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
      if (opcode == GETFIELD) {
        // Cuando se lee un campo, lo agregamos a la lista de campos para ser pasados como parámetros
        if (!fieldsToAddAsParams.containsKey(name)) {
          fieldsToAddAsParams.put(name, descriptor);
          System.out.println("Field read detected: " + name + " with descriptor " + descriptor);
        }
      }
      super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitEnd() {
      // Después de visitar el método original, modificamos la firma del método para agregar los campos como parámetros
      if (!fieldsToAddAsParams.isEmpty()) {
        System.out.println("Modifying method signature to add fields as parameters");

        StringBuilder newMethodDescriptor = new StringBuilder("(");

        // Copiar los parámetros existentes
        Type methodType = Type.getMethodType(descriptor);
        for (Type argType : methodType.getArgumentTypes()) {
          newMethodDescriptor.append(argType.getDescriptor());
        }

        // Agregar los campos leídos como nuevos parámetros
        for (String fieldDescriptor : fieldsToAddAsParams.values()) {
          newMethodDescriptor.append(fieldDescriptor);
        }


        newMethodDescriptor.append(")").append(methodType.getReturnType().getDescriptor());

        System.out.println("New method descriptor: " + newMethodDescriptor);

        // Crear el nuevo método con la firma modificada
        MethodVisitor newMethod = cv.visitMethod(ACC_PUBLIC, "new_" + name, newMethodDescriptor.toString(), null, null);
        newMethod.visitCode();

        int localIndex = 0;
        for (Map.Entry<String, String> entry : fieldsToAddAsParams.entrySet()) {
          String fieldName = entry.getKey();
          String fieldType = entry.getValue();

          // Add local variable table entry for the new parameter
          newMethod.visitLocalVariable(fieldName, fieldType, null, new Label(), new Label(), localIndex);
          mv.visitVarInsn(ALOAD, localIndex); // ALOAD para cargar el parámetro

          localIndex++;
        }


        // Aquí agregamos las instrucciones modificadas al nuevo método
        newMethod.visitMaxs(0, 0);
        newMethod.visitEnd();
      }
      super.visitEnd();
    }
  }

  public static void main(String[] args) throws Exception {
    MethodFieldsUsageCollector methodInfo = getMethodInfo();

    ClassReader classReader = new ClassReader(JetSetWilly.class.getName());
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

    // Aplicar la transformación
    AddFieldAsParameterTransformer transformer = new AddFieldAsParameterTransformer(classWriter, methodInfo);
    classReader.accept(transformer, 0);

    // Guardar la clase modificada
    byte[] bytecode = classWriter.toByteArray();
    Files.write(Path.of("jsw1.class"), bytecode);

    // Guardar el bytecode en un archivo .class
  }

  private static MethodFieldsUsageCollector getMethodInfo() throws IOException {
    ClassReader classReader1 = new ClassReader(JetSetWilly.class.getName());
    MethodFieldsUsageCollector methodFieldsUsageCollector = new MethodFieldsUsageCollector("$38601");
    classReader1.accept(methodFieldsUsageCollector, 0);
    return methodFieldsUsageCollector;
  }
}
