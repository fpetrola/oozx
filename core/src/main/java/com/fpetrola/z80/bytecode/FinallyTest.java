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

package com.fpetrola.z80.bytecode;

import org.apache.commons.io.FileUtils;
import org.cojen.maker.*;
import org.junit.jupiter.api.Test;

import java.io.File;


public class FinallyTest {

  private Field de;
  private Field ix;
  private Field a;
  private Field bc;
  private Field af;
  private Field memory;

  @Test
  public void goto1() throws Exception {
    ClassMaker cm = ClassMaker.beginExternal("JSW2").public_();

    MethodMaker mm = cm.addMethod(null, "fun_90C0").public_();
    addRegisters(cm, mm);

    ix.set(0x8100);
    Label $90C4 = mm.label().here();

    a.set(memory.aget(ix.add(4)));

    Label cont1 = mm.label();
    a.ifEq(0xFF, cont1);
    mm.return_();

    cont1.here();
    a.set(a.and(0x03));

    Label $91B6 = mm.label();
    a.ifEq(0, $91B6);


    Label ok = mm.label().here();
    de.inc(1);
    de.ifNe(0, ok);


    bc.set(memory.aget(1000));
    Label cont = mm.label().goto_();
    af.set(2);
    cont.here();

    $91B6.here();
    ix.add(de.set(8));
    $90C4.goto_();

    byte[] bytes = cm.finishBytes();
    FileUtils.writeByteArrayToFile(new File("JSW2.class"), bytes);
  }

  private void addRegisters(ClassMaker cm, MethodMaker mm) {
    cm.addField(int.class, "A").public_();
    cm.addField(int.class, "F").public_();
    cm.addField(int.class, "B").public_();
    cm.addField(int.class, "C").public_();
    cm.addField(int.class, "D").public_();
    cm.addField(int.class, "E").public_();
    cm.addField(int.class, "AF").public_();
    cm.addField(int.class, "BC").public_();
    cm.addField(int.class, "DE").public_();
    cm.addField(int.class, "IX").public_();
    cm.addField(int[].class, "memory").public_();

    de = mm.field("DE");
    ix = mm.field("IX");
    a = mm.field("A");
    bc = mm.field("BC");
    af = mm.field("AF");
    memory = mm.field("memory");
  }


}