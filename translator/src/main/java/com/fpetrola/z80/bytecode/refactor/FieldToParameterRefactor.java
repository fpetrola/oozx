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

import com.fpetrola.z80.bytecode.tests.ZxGame1;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class FieldToParameterRefactor extends ClassVisitor {

  public FieldToParameterRefactor(ClassVisitor classVisitor) {
    super(ASM9, classVisitor);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    if (name.equals("$38601")) {
      MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
      return new MethodTransformer(mv);
    } else
      return super.visitMethod(access, name, descriptor, signature, exceptions);
  }

  class MethodTransformer extends MethodVisitor {

    private final Map<String, Integer> fieldsToAddAsParams = new LinkedHashMap<>(); // Para mantener el orden de los campos

    // List to track modified fields that will need to be returned
    private final List<String> modifiedFields = new ArrayList<>();
    private int paramsIndex= 0;

    public MethodTransformer(MethodVisitor mv) {
      super(ASM9, mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
      if (opcode == GETFIELD) {
        if (!fieldsToAddAsParams.containsKey(name)) {
          fieldsToAddAsParams.put(name, paramsIndex++);
          System.out.println("Field read detected: " + name + " with descriptor " + descriptor);
        }

        // Replace GETFIELD with parameter load (assuming we change the method signature)
        System.out.println("Replacing field access: " + name + " with parameter");
        // Load parameter instead of field (adjust index according to the parameter order)
        mv.visitVarInsn(ALOAD, getParameterIndexForField(name)); // ALOAD para cargar el parámetro
      } else if (opcode == PUTFIELD) {

        // Track the modified field to return it later
        if (!modifiedFields.contains(name)) {
          modifiedFields.add(name);
        }
        super.visitFieldInsn(opcode, owner, name, descriptor);

//        // Store the modified value in a local variable instead of field
//        System.out.println("Replacing field write: " + name + " with local variable");
//        mv.visitVarInsn(ASTORE, getLocalVarIndexForField(name)); // ASTORE para variables locales
      } else {
        super.visitFieldInsn(opcode, owner, name, descriptor);
      }
    }

//    @Override
//    public void visitInsn(int opcode) {
//      if (opcode == RETURN) {
//        // Before returning, insert code to return modified fields in an array
//        if (!modifiedFields.isEmpty()) {
//          System.out.println("Returning modified fields in an array");
//
//          // Create an array to hold the modified values
//          mv.visitIntInsn(BIPUSH, modifiedFields.size());
//          mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
//
//          // Store each modified local variable into the array
//          for (int i = 0; i < modifiedFields.size(); i++) {
//            mv.visitInsn(DUP);
//            mv.visitIntInsn(BIPUSH, i);
//            mv.visitVarInsn(ALOAD, getLocalVarIndexForField(modifiedFields.get(i)));
//            mv.visitInsn(AASTORE);  // AASTORE stores the variable in the array
//          }
//
//          // Replace the return with returning the array
//          mv.visitInsn(ARETURN); // ARETURN to return the array
//        }
//      } else {
//        super.visitInsn(opcode);
//      }
//    }

    private int getParameterIndexForField(String fieldName) {
      // Map field names to parameter indexes (you'll need logic here)
      // Por ejemplo, si tienes varios parámetros, puedes asignarles índices
      return fieldsToAddAsParams.get(fieldName); // Ejemplo: asume que el field se reemplaza por el primer parámetro
    }

    private int getLocalVarIndexForField(String fieldName) {
      // Map field names to local variable indexes
      return 2; // Ejemplo: asume que el field modificado es la segunda variable local
    }
  }

  public static void main(String[] args) throws Exception {
    // Leer la clase original
    ClassReader classReader = new ClassReader(ZxGame1.class.getName());
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

    // Aplicar la transformación
    FieldToParameterRefactor transformer = new FieldToParameterRefactor(classWriter);
    classReader.accept(transformer, 0);

    // Guardar la clase modificada
    byte[] bytecode = classWriter.toByteArray();
    // Aquí guardarías el bytecode en un archivo .class
  }
}
