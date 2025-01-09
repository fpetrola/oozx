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

package com.fpetrola.z80.minizx.emulation;

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.MockedIO;
import com.fpetrola.z80.minizx.SpectrumApplication;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.transformations.Base64Utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.fpetrola.z80.helpers.Helper.formatAddress;
import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class MiniZXAndEmulation {
  protected boolean replacing = false;
  private State<WordNumber> state;

  public MiniZXAndEmulation(OOZ80<WordNumber> ooz80, SpectrumApplication spectrumApplication) {
    this.ooz80 = ooz80;
    this.spectrumApplication = spectrumApplication;
  }

  private OOZ80<WordNumber> ooz80;
  private final SpectrumApplication spectrumApplication;

  public void emulate() {
    int i = 0;
    while (true) {
      this.ooz80 = ooz80;
      if (i++ % 1000000000 == 0) this.ooz80.getState().setINTLine(true);
      else if (i % 3 == 0) {
        this.ooz80.execute();
      }
    }
  }

  public void copyStateToJava() {
    Arrays.stream(RegisterName.values()).forEach(n -> {
      try {
        boolean fieldExists = Arrays.stream(spectrumApplication.getClass().getFields()).anyMatch(f -> f.getName().equals(n.name()));
        if (fieldExists) {
          Register register = ooz80.getState().getRegister(n);
          WordNumber read = (WordNumber) register.read();
          if (read != null) {
            Field field = spectrumApplication.getClass().getField(n.name());
            field.set(spectrumApplication, read.intValue());
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    copyMemoryState(ooz80.getState());
  }

  public void copyMemoryState(State state) {
    //spectrumApplication.mem = state.getMemory().getData();
    Object[] data = state.getMemory().getData();
    for (int i = 16384; i < 0xFFFF; i++) {
      WordNumber datum = (WordNumber) data[i];
      if (datum == null)
        datum = createValue(0);

      spectrumApplication.getMem()[i] = (byte) datum.intValue();
    }
  }

  public void copyStateBackToEmulation() {
    spectrumApplication.update16Registers();
    Arrays.stream(RegisterName.values()).forEach(n -> {
      try {
        boolean fieldExists = Arrays.stream(spectrumApplication.getClass().getFields()).anyMatch(f -> f.getName().equals(n.name()));
        Register register = ooz80.getState().getRegister(n);

        Object value = createValue(0);
        if (fieldExists) {
          Field field = spectrumApplication.getClass().getField(n.name());
          Integer o = (Integer) field.get(spectrumApplication);
          value = createValue(o);
        }
        register.write(value);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    copyMemoryStateBack(ooz80.getState());
  }

  public boolean stateIsMatching(Map<String, Integer> writtenRegisters, int address, boolean write) {
    final boolean[] differences = {false};
    spectrumApplication.update16Registers();

    List<RegisterName> list = new ArrayList<>(Arrays.asList(RegisterName.values()));
    list.removeAll(Arrays.asList(RegisterName.PC, RegisterName.F, RegisterName.AF, RegisterName.Fx, RegisterName.AFx, RegisterName.SP, RegisterName.IR, RegisterName.I, RegisterName.R));
    State<WordNumber> state1 = ooz80.getState();

//    Stream<RegisterName> registerNameStream = writtenRegisters.keySet().stream().map(r -> RegisterName.valueOf(r));
    Stream<RegisterName> registerNameStream = list.stream();
    registerNameStream.forEach(n -> {
      if (list.contains(n)) {
        try {
          boolean fieldExists = Arrays.stream(spectrumApplication.getClass().getFields()).anyMatch(f -> f.getName().equals(n.name()));
          if (fieldExists) {
            Register<WordNumber> register = state1.getRegister(n);
            int o = checkField(n, register, differences);
            register.write(createValue(o));
          }
        } catch (Exception e) {
          differences[0] = false;
        }
      }
    });

    if (write) {
      WordNumber[] data = state1.getMemory().getData();
//    for (int i = 16384; i < 65520; i++)
      int i = address;
      checkMem(data, i, differences);
    }

    writtenRegisters.clear();
    return !differences[0];
  }

  private int checkField(RegisterName n, Register<WordNumber> register, boolean[] differences) throws NoSuchFieldException, IllegalAccessException {
    Field field = spectrumApplication.getClass().getField(n.name());
    int o = (Integer) field.get(spectrumApplication);
    int registerValue = register.read().intValue();

    if (register.read() != null && o != registerValue) {
      differences[0] = true;
    }
    if (differences[0]) {
      o = getFrom16BitField(n);
      if (o == registerValue)
        differences[0] = false;
    }

    if (differences[0])
      System.out.println("reg diff in: " + register.getName());
    return o;
  }

  private int getFrom16BitField(RegisterName n) throws IllegalAccessException, NoSuchFieldException {
    if (n.name().equals("A"))
      return get16BitFieldValue("AF") >> 8;
    else if (n.name().equals("B"))
      return get16BitFieldValue("BC") >> 8;
    else if (n.name().equals("C"))
      return get16BitFieldValue("BC") & 0xff;
    else if (n.name().equals("D"))
      return get16BitFieldValue("DE") >> 8;
    else if (n.name().equals("E"))
      return get16BitFieldValue("DE") & 0xff;
    else if (n.name().equals("H"))
      return get16BitFieldValue("HL") >> 8;
    else if (n.name().equals("L"))
      return get16BitFieldValue("HL") & 0xff;
    else if (n.name().equals("IXH"))
      return get16BitFieldValue("IX") >> 8;
    else if (n.name().equals("IXL"))
      return get16BitFieldValue("IX") & 0xff;
    else if (n.name().equals("IYH"))
      return get16BitFieldValue("IY") >> 8;
    else if (n.name().equals("IYL"))
      return get16BitFieldValue("IY") & 0xff;
    else if (n.name().equals("DE"))
      return spectrumApplication.DE();
    else if (n.name().equals("BC"))
      return spectrumApplication.BC();
    else if (n.name().equals("HL"))
      return spectrumApplication.HL();
    else if (n.name().equals("AF"))
      return spectrumApplication.AF();

    throw new RuntimeException("no reg");
  }

  private int get16BitFieldValue(String name) throws IllegalAccessException, NoSuchFieldException {
    int i = (int) spectrumApplication.getClass().getField(name).get(spectrumApplication);
    return i;
  }

  private void checkMem(WordNumber[] data, int i, boolean[] differences) {
    int i1 = data[i].intValue() & 0xFF;
    int i2 = spectrumApplication.getMem()[i] & 0xff;
    if (i1 != i2) {
      System.out.println("mem diff at: " + formatAddress(i) + ": " + formatAddress(i1) + " - " + formatAddress(i2));
      differences[0] = true;
    }
  }

  public void copyMemoryStateBack(State state) {
    Object[] data = state.getMemory().getData();
    for (int i = 0; i < 0xFFFF; i++) {
      data[i] = createValue(spectrumApplication.getMem()[i]);
    }
  }

  protected void main() {
    // OOZ80<WordNumber> ooz80 = createOOZ80(io);
    //String fileName = "/home/fernando/detodo/desarrollo/m/zx/zx/jsw2.z80";
    //  state = ooz80.getState();

    state = new State(new MockedIO(), new MockedMemory(true));

    //loadSnapshotFromBase64(state, "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");

    // copyState(state);
    // byte[] d1 = t1(data);
    //mem = (WordNumber[]) state.getMemory().getData();

    //    new Timer(10, e -> {
//      copyMemoryStateBack(state);
//    }).start();

    // copyState(state);
    //emulate(ooz80);
  }

  //  @Override
//  public int mem(int address) {
//    Object[] data = state.getMemory().getData();
//    Object datum = data[address & 0xFFFF];
//    if (datum == null)
//      datum = createValue(0);
//
//    return ((WordNumber) datum).intValue();
//  }
//
//  @Override
//  public void mem(int address, int value) {
//    Object[] data = state.getMemory().getData();
//    data[address & 0xFFFF] = createValue((int) value);
//  }


  private byte[] t1(WordNumber[] data) {
    byte[] d1 = new byte[0x10000];
    for (int i = 0; i < d1.length; i++) {
      d1[i] = (byte) (data[i].intValue() & 0xff);
    }
    String s = Base64Utils.gzipArrayCompressToBase64(d1);
    return d1;
  }
}
