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

package com.fpetrola.z80.minizx;

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.LinkedList;

public class MiniZXIO implements IO<WordNumber> {
  private int[] ports = initPorts();
  private LinkedList<PortInput> lastEmuInputs = new LinkedList<>();
  private LinkedList<PortInput> lastJavaInputs = new LinkedList<>();
  public MiniZXKeyboard miniZXKeyboard;

  public MiniZXIO() {
    miniZXKeyboard = new MiniZXKeyboard();
  }

  public synchronized WordNumber in(WordNumber port) {
    WordNumber wordNumber = processLastInputs(port, lastEmuInputs, lastJavaInputs);
//      System.out.println("emu IN: " + port.intValue() + "= " + wordNumber.intValue());
    return wordNumber;
  }

  private WordNumber in0(WordNumber port) {
    WordNumber value = WordNumber.createValue(performIn(port.intValue()));
    return value;
//      int portNumber = port.intValue();
//      //  portNumber = portNumber & 0xff;
//      int port1 = ports[portNumber];
//      WordNumber value = WordNumber.createValue(port1);
//
////      if (portNumber == 31 && value.intValue() != 0)
////        ports[31] = 0;
//
//      return value;

//      if (port1 != 0) {
//        //      System.out.println(port1);
//        return WordNumber.createValue(port1);
//      } else {
//        if (portNumber == 31)
//          return WordNumber.createValue(0);
//        else
//          return WordNumber.createValue(191);
//      }
  }

  public void out(WordNumber port, WordNumber value) {
  }

  public void setCurrentKey(int e, boolean pressed) {
    if (KeyEvent.VK_RIGHT == e) {
      activateKey(1, pressed);
//        ports[getAnInt(61438)] = pressed ? 187 : 255;
//        ports[getAnInt(59390)] = pressed ? 187 : 255;
    } else if (KeyEvent.VK_LEFT == e) {
      activateKey(2, pressed);
//        ports[getAnInt(61438)] = pressed ? 175 : 255;
//        ports[getAnInt(59390)] = pressed ? 175 : 255;
    } else if (KeyEvent.VK_UP == e) {
      activateKey(8, pressed);
    } else if (KeyEvent.VK_DOWN == e) {
      activateKey(4, pressed);
    } else if (KeyEvent.VK_SPACE == e) {
      activateKey(16, pressed);
//        ports[getAnInt(61438)] = pressed ? 254 : 255;
    } else if (KeyEvent.VK_ENTER == e) {
      activateKey(16, pressed);

//        ports[getAnInt(49150)] = pressed ? 254 : 255;
//        ports[getAnInt(45054)] = pressed ? 190 : 255;
//        ports[getAnInt(61438)] = pressed ? 254 : 255;
    }
  }

  private int getAnInt(int i) {
    //System.out.println(i & 0xff);
    return i;
  }

  private void activateKey(int i, boolean pressed) {
    if (pressed)
      ports[31] |= i;
    else {
      int i1 = ~i;
      ports[31] &= i1;
    }
  }

  private int[] initPorts() {
    int[] ports = new int[0x10000];
    Arrays.fill(ports, 0);
    //    ports[65278]= 191;
    //    ports[32766]= 191;
    //    ports[65022]= 191;
    //    ports[49150]= 191;
    //    ports[61438]= 191;
    //    ports[64510]= 191;
    //    ports[59390]= 191;
    //    ports[59390]= 191;
    //    ports[59390]= 191;
    ports[45054] = 1;
    return ports;
  }

  public synchronized WordNumber in2(WordNumber port) {
    WordNumber wordNumber = processLastInputs(port, lastJavaInputs, lastEmuInputs);
//      System.out.println("java IN: " + port.intValue() + "= " + wordNumber.intValue());
    return wordNumber;
  }

  private WordNumber processLastInputs(WordNumber port, LinkedList<PortInput> ownInputs, LinkedList<PortInput> otherInputs) {
    if (otherInputs.isEmpty()) {
      WordNumber in = in0(port);
      ownInputs.offer(new PortInput(port, in));
      return in;
    } else {
      PortInput pop = otherInputs.poll();
//        if (pop.port.intValue() != port.intValue())
//          System.out.println("port!");

      return pop.result;
    }
  }

  private int performIn(int port) {
    if ((port & 0x0001) == 0) {
      int earBit = 191;
      int i = miniZXKeyboard.readKeyboardPort(port, true) & earBit;
//        if (i != 191)
      //   System.out.println("port: " + port + " -> " + i);
      return i;
    }

    return 0 & 0xff;
  }

  public static class PortInput {
    public WordNumber result;
    public WordNumber port;

    public PortInput(WordNumber port, WordNumber in) {
      this.port = port;
      this.result = in;
    }
  }
}
