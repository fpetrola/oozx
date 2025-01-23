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

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.ide.rzx.InputRecordingBlock;
import com.fpetrola.z80.ide.rzx.RzxFile;
import com.fpetrola.z80.minizx.emulation.InterruptionListener;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public class RZXPlayerIO<T extends WordNumber> implements MiniZXIO<T> {
  public MiniZXKeyboard miniZXKeyboard;
  private Register<T> pc;

  private int currentFrameIndex;

  private RzxFile rzxFile;
  private OOZ80 ooz80;
  private InputRecordingBlock.Frame currentFrame;
  private int fetchCounter;
  private LinkedList<Byte> inputs = new LinkedList<>();
  private volatile boolean interruption = false;
  private InputRecordingBlock inputRecordingBlock;
  private List<InputRecordingBlock.Frame> frames;
  private volatile boolean lastInterruption = true;
  private boolean noInputs;
  private long lastCount;

  public RZXPlayerIO() {
    miniZXKeyboard = new MiniZXKeyboard();
  }

  public int getCurrentFrameIndex() {
    return currentFrameIndex;
  }

  public void out(T port, T value) {
  }

  public synchronized T in(T port) {
    return WordNumber.createValue(performIn(port.intValue()));
  }

  private int performIn(int port) {
    return getNextInput();
//    if (currentFrame.returnValues.length == 0)
//      return 0;
//
//    if (returnValuesIndex >= currentFrame.returnValues.length) {
//      System.out.println("error");
//      return currentFrame.returnValues[currentFrame.returnValues.length - 1];
//    } else {
//      byte value = currentFrame.returnValues[returnValuesIndex++];
//      return value;
//    }
//
//    if ((port & 0x0001) == 0) {
//      int earBit = 191;
//      return miniZXKeyboard.readKeyboardPort(port, true) & earBit;
//    }
//
//    return 0 & 0xff;
  }

  private byte getNextInput() {
//    while (inputs.isEmpty()) {
//      ++currentFrameIndex;
//      changeFrame();
//    }

    if (inputs.isEmpty()) {
      ++currentFrameIndex;
      changeFrame();
      System.out.println("why?");
    }
    Byte poll = inputs.poll();
    noInputs = inputs.isEmpty();
    return poll;
  }

  public MiniZXKeyboard getMiniZXKeyboard() {
    return miniZXKeyboard;
  }

  public void setPc(Register pc) {
    this.pc = pc;
  }

  public void setup(RzxFile rzxFile, OOZ80 ooz80) {
    this.rzxFile = rzxFile;
    inputRecordingBlock = rzxFile.getInputRecordingBlock();
    frames = inputRecordingBlock.frames;
    this.ooz80 = ooz80;
    currentFrameIndex = 0;
    lastCount= inputRecordingBlock.tStates;
    changeFrame();

//    ObservableRegister<T> registerR = (ObservableRegister<T>) ooz80.getState().getRegisterR();
//    registerR.addIncrementWriteListener(value -> {
//      fetchCounter++;
//      if (fetchCounter >= currentFrame.fetchCounter) {
//        ++currentFrameIndex;
//        changeFrame();
//      }
//    });
//    registerR.listening(true);
  }

  private void changeFrame() {
    if (currentFrameIndex < frames.size()) {
      printFrameCount();

      currentFrame = frames.get(currentFrameIndex);
      for (int i = 0; i < currentFrame.returnValues.length; i++) {
        inputs.add(currentFrame.returnValues[i]);
      }
      fetchCounter = 0;
    } else
      inputs.add((byte) 0);
  }

  private void printFrameCount() {
    if (currentFrameIndex % 1000 == 0)
      System.out.println(currentFrameIndex);
  }

  public Predicate<Integer> getInterruptionCondition() {
    return (i) -> {
      if (i - lastCount +1 >= currentFrame.fetchCounter) {
        ++currentFrameIndex;
        changeFrame();
        lastCount= i;
        return true;
      } else
        return false;
    };
  }
}
