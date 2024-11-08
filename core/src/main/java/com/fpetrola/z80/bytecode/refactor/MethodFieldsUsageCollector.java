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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class MethodFieldsUsageCollector extends ClassVisitor {
  public final Map<String, String> fieldsToAddAsParams = new LinkedHashMap<>(); // Para mantener el orden de los campos
  public final List<String> modifiedFields = new ArrayList<>();
  private final String methodName;

  public MethodFieldsUsageCollector(String methodName) {
    super(ASM9, new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS));
    this.methodName = methodName;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    if (name.equals(methodName)) {
      MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
      return new MethodInformationCollector(mv, access, name, descriptor);
    } else
      return super.visitMethod(access, name, descriptor, signature, exceptions);
  }

  class MethodInformationCollector extends MethodVisitor {


    private final String name;
    private final String descriptor;

    public MethodInformationCollector(MethodVisitor mv, int access, String name, String descriptor) {
      super(ASM9, mv);
      this.name = name;
      this.descriptor = descriptor;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
      if (opcode == GETFIELD) {
        if (!modifiedFields.contains(name))
          if (!fieldsToAddAsParams.containsKey(name)) {
            fieldsToAddAsParams.put(name, descriptor);
            System.out.println("Field read detected: " + name + " with descriptor " + descriptor);
          }
      } else if (opcode == PUTFIELD) {
        if (!modifiedFields.contains(name)) {
          modifiedFields.add(name);
        }
      }

      super.visitFieldInsn(opcode, owner, name, descriptor);
    }
  }

  public static void main(String[] args) throws Exception {
    ClassReader classReader = new ClassReader(JetSetWilly.class.getName());
    MethodFieldsUsageCollector methodFieldsUsageCollector = new MethodFieldsUsageCollector("$38601");
    classReader.accept(methodFieldsUsageCollector, 0);
  }
}
