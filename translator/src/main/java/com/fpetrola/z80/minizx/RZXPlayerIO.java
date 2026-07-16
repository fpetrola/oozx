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

import com.fpetrola.z80.ide.rzx.InputRecordingBlock;
import com.fpetrola.z80.ide.rzx.RzxFile;
import com.fpetrola.z80.minizx.emulation.OutListener;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class RZXPlayerIO<T extends WordNumber> implements MiniZXIO<T> {
  public MiniZXKeyboard miniZXKeyboard;
  private Register<T> pc;
  private int currentFrameIndex;
  private InputRecordingBlock.Frame currentFrame;
  private SimpleQueue<Byte> inputs = new SimpleQueue<>(1000000);
  private InputRecordingBlock inputRecordingBlock;
  private List<InputRecordingBlock.Frame> frames;
  private long lastCount;
  private byte lastPoll;
  private List<OutListener> outListeners = new ArrayList<>();
  private int fetchCounter;
  public static boolean stop;
  private static final boolean DEBUG_SYNC = Boolean.getBoolean("rzx.debug");

  public RZXPlayerIO() {
    miniZXKeyboard = new MiniZXKeyboard();
  }

  public void addOutListener(OutListener outListener) {
    outListeners.add(outListener);
  }

  public int getCurrentFrameIndex() {
    return currentFrameIndex;
  }

  public void out(T port, T value) {
//    outListeners.forEach(l -> l.outAt(port, value));
  }

  public synchronized T in(T port) {
    if (currentFrame == null)
      return createValue(0);
    else {
      T value = createValue(performIn(port.intValue()));
//      if (value.intValue() != -65)
//        System.out.println("");
      return value;
    }
  }

  private int performIn(int port) {
    return getNextInput();
  }

  private byte getNextInput() {
    if ( stop)
      throw new RuntimeException("stop");
    if (inputs.isEmpty()) {
      if (DEBUG_SYNC)
        System.out.println("rzx-sync: frame " + currentFrameIndex
            + " ran out of inputs BEFORE its interrupt (emulator consumed too many INs)");
      ++currentFrameIndex;
      changeFrame();
    }
    Byte poll = inputs.poll();
    if (poll == null)
      return lastPoll;
    else
      lastPoll = poll;
    return poll;
  }

  public MiniZXKeyboard getMiniZXKeyboard() {
    return miniZXKeyboard;
  }

  public void setPc(Register pc) {
    this.pc = pc;
  }

  public void setup(RzxFile rzxFile) {
    inputRecordingBlock = rzxFile.getInputRecordingBlock();
    frames = inputRecordingBlock.frames;
    currentFrameIndex = 0;
    lastCount = inputRecordingBlock.tStates;
    lastPoll = 0;
    fetchCounter = 0;
    inputs.clear();
    changeFrame();
  }

  private void changeFrame() {
    if (currentFrameIndex < frames.size()) {
      printFrameCount();

      currentFrame = frames.get(currentFrameIndex);
      for (int i = 0; i < currentFrame.returnValues.length; i++) {
        inputs.add(currentFrame.returnValues[i]);
      }
    } else {
      if (inputs.isEmpty()) {
        throw new RuntimeException("rzx finished");
      }
      inputs.add((byte) 0);
    }
  }

  private void printFrameCount() {
    if (currentFrameIndex % 1000 == 0)
      System.out.println(currentFrameIndex);
  }

  public Predicate<Integer> getInterruptionCondition() {
    return (i) -> {
      fetchCounter = i;
      if (currentFrame != null)
        if (i - lastCount + 1 > currentFrame.fetchCounter) {
          if (DEBUG_SYNC && !inputs.isEmpty())
            System.out.println("rzx-sync: frame " + currentFrameIndex
                + " reached its interrupt with unconsumed inputs (emulator consumed too few INs)");
          ++currentFrameIndex;
          changeFrame();
          lastCount = i;
          return true;
        } else
          return false;

      return false;
    };
  }
}
