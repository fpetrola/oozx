/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


package model.tests.devices;

import com.fpetrola.oozx.speccy.devices.spec256.Alignment;
import com.fpetrola.oozx.speccy.devices.spec256.Planes;
import com.fpetrola.oozx.speccy.devices.spec256.Rules;
import com.fpetrola.oozx.speccy.devices.spec256.Spec256Core;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.registers.RegisterName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Nine processors through the same instructions: the one a game was written for, and eight over
 * the colours of its pixels. No game here - a few bytes of the machine's own code, and colours
 * put where the facts need them.
 */
class NineInStepTest {
  private static final int CODE = 0x8000, FROM = 0x9000, TO = 0xa000;

  private final byte[] ram = new byte[0x10000];
  private final Map<Integer, int[]> painted = new LinkedHashMap<>();
  private final Alignment alignment = new Alignment();
  private final Rules rules = new Rules();
  private int portsRead, portsWritten, lastPortWritten = -1;
  private Planes planes;
  private State state;
  private Memory memory;

  private void code(int at, int... bytes) {
    for (int i = 0; i < bytes.length; i++) ram[at + i] = (byte) bytes[i];
  }

  /** The eight colours of one address, leftmost pixel first, over whatever the byte there says. */
  private void colours(int address, int... fromTheLeft) {
    painted.put(address, fromTheLeft);
  }

  /** A game's file: every byte as the machine has it, and colours where a picture was painted. */
  private byte[] file() {
    byte[] file = new byte[Planes.LENGTH];
    for (int address = Planes.RAM; address < 0x10000; address++) {
      for (int pixel = 0; pixel < 8; pixel++) {
        file[(address - Planes.RAM) * 8 + (7 - pixel)] = (byte) ((ram[address] & (0x80 >> pixel)) != 0 ? 0xff : 0);
      }
    }
    painted.forEach((address, colours) -> {
      for (int pixel = 0; pixel < 8; pixel++) {
        file[(address - Planes.RAM) * 8 + (7 - pixel)] = (byte) colours[pixel];
      }
    });
    return file;
  }

  private OOZ80 nine() {
    planes = Planes.of(file());
    memory = new Memory() {
      public int read(int address, int fetching) {
        return ram[address] & 0xff;
      }

      public int peek(int address) {
        return ram[address] & 0xff;
      }

      public void write(int address, int value) {
        ram[address] = (byte) value;
      }

      public void reset() {
      }
    };
    state = new State(new IO() {
      public int in(int port) {
        portsRead++;
        return 0x5a;
      }

      public void out(int port, int value) {
        portsWritten++;
        lastPortWritten = value;
      }
    }, new DefaultRegisterBankFactory().createBank(), memory);
    OOZ80 cpu = new Spec256Core(planes, alignment, rules).cpu(state, null);
    cpu.reset();
    state.setHalted(false);
    state.getRegister(RegisterName.PC).write(CODE);
    return cpu;
  }

  private void run(OOZ80 cpu, int instructions) {
    for (int i = 0; i < instructions; i++) cpu.execute();
    assertEquals(State.RunState.STATE_RUNNING, state.getRunState(), "nothing threw on the way");
  }

  private int colourOf(int address, int pixel) {
    return planes.colourOf(address, pixel);
  }

  @Test
  void aColourIsCarriedFromOneAddressToAnotherByTheMachinesOwnInstructions() {
    code(CODE, 0x7e, 0x12);                          // LD A,(HL) ; LD (DE),A
    colours(FROM, 200, 1, 0, 0, 0, 0, 0, 255);
    OOZ80 cpu = nine();
    state.getRegister(RegisterName.HL).write(FROM);
    state.getRegister(RegisterName.DE).write(TO);

    run(cpu, 2);

    assertEquals(200, colourOf(TO, 0), "eight processors moved one bit each, and the colour arrived whole");
    assertEquals(255, colourOf(TO, 7));
    assertEquals(ram[FROM], ram[TO], "and the machine itself moved the byte it always moved");
  }

  @Test
  void aBlockCopyCarriesAWholeSpriteWithItsColours() {
    code(CODE, 0xed, 0xb0);                          // LDIR
    for (int i = 0; i < 4; i++) colours(FROM + i, 10 + i, 20 + i, 0, 0, 0, 0, 0, 0);
    OOZ80 cpu = nine();
    state.getRegister(RegisterName.HL).write(FROM);
    state.getRegister(RegisterName.DE).write(TO);
    state.getRegister(RegisterName.BC).write(4);

    run(cpu, 8);

    for (int i = 0; i < 4; i++) {
      assertEquals(10 + i, colourOf(TO + i, 0), "the leftmost pixel of byte " + i);
      assertEquals(20 + i, colourOf(TO + i, 1));
    }
    assertEquals(0, state.getRegister(RegisterName.BC).read(), "the machine counted the bytes, once");
  }

  @Test
  void aMaskLetsThroughTheColoursItLeaves() {
    code(CODE, 0x7e, 0x2c, 0xa6, 0x12);              // LD A,(HL) ; INC L ; AND (HL) ; LD (DE),A
    colours(FROM, 200, 201, 202, 203, 204, 205, 206, 207);
    code(FROM + 1, 0b10100000);
    OOZ80 cpu = nine();
    state.getRegister(RegisterName.HL).write(FROM);
    state.getRegister(RegisterName.DE).write(TO);

    run(cpu, 4);

    assertEquals(200, colourOf(TO, 0), "where the mask leaves a bit, the colour goes through");
    assertEquals(202, colourOf(TO, 2));
    assertEquals(0, colourOf(TO, 1), "and where it does not, nothing does");
    assertEquals(0, colourOf(TO, 7));
  }

  @Test
  void onlyTheMachineTalksToAPort() {
    code(CODE, 0xdb, 0xfe, 0xd3, 0xfe);              // IN A,(0xfe) ; OUT (0xfe),A
    OOZ80 cpu = nine();

    run(cpu, 2);

    assertEquals(1, portsRead, "eight followers read nothing: what a port says is never a colour");
    assertEquals(1, portsWritten, "and none of them writes one, where the port could be the border");
    assertEquals(0x5a, lastPortWritten);
  }

  @Test
  void anInterruptIsTakenByAllNine() {
    code(CODE, 0x00);
    colours(0xbffe, 1, 1, 1, 1, 1, 1, 1, 1);
    OOZ80 cpu = nine();
    state.getRegister(RegisterName.SP).write(0xc000);
    state.setIntMode(State.InterruptionMode.IM1);
    state.setIff1(true);
    run(cpu, 1);

    cpu.interruption();

    assertEquals(0x38, state.getRegister(RegisterName.PC).read(), "the machine went to answer it");
    assertEquals(0, colourOf(0xbffe, 0), "and so did the eight, over the colours their stacks were written on");
    Memory plane = planes.plane(0, memory);
    assertEquals(CODE + 1, (plane.peek(0xbffe) & 0xff) | (plane.peek(0xbfff) << 8),
        "each of them pushed the machine's own return address, in its own plane");
  }

  /**
   * Two pixels of one byte, so that the planes hold bytes that differ as numbers and not only as
   * bits: with the rule off the two drawings merge, with it on the higher byte wins outright.
   */
  @Test
  void whereAGameSaysSoTheLogicalInstructionsTakeTheHigherByteInsteadOfBothOfThem() {
    code(CODE, 0x7e, 0x2c, 0xb6, 0x12);              // LD A,(HL) ; INC L ; OR (HL) ; LD (DE),A
    colours(FROM, 1, 0, 0, 0, 0, 0, 0, 0);
    colours(FROM + 1, 0, 1, 0, 0, 0, 0, 0, 0);
    rules.levelledOr = true;
    OOZ80 cpu = nine();
    state.getRegister(RegisterName.HL).write(FROM);
    state.getRegister(RegisterName.DE).write(TO);

    run(cpu, 4);

    assertEquals(1, colourOf(TO, 0), "the higher byte is the one with the leftmost pixel, and it is what arrives");
    assertEquals(0, colourOf(TO, 1), "the other drawing is covered rather than merged into it");
  }

  @Test
  void otherwiseTheyAreTheOnesTheMachineHasAndBothDrawingsShow() {
    code(CODE, 0x7e, 0x2c, 0xb6, 0x12);              // LD A,(HL) ; INC L ; OR (HL) ; LD (DE),A
    colours(FROM, 1, 0, 0, 0, 0, 0, 0, 0);
    colours(FROM + 1, 0, 1, 0, 0, 0, 0, 0, 0);
    OOZ80 cpu = nine();
    state.getRegister(RegisterName.HL).write(FROM);
    state.getRegister(RegisterName.DE).write(TO);

    run(cpu, 4);

    assertEquals(1, colourOf(TO, 0), "bit for bit, which is what a machine does");
    assertEquals(1, colourOf(TO, 1));
  }

  @Test
  void aFollowerRunsTheMachinesCodeEvenWhereItsOwnPlaneIsAPicture() {
    code(CODE, 0x7e);                                // LD A,(HL)
    colours(CODE, 0, 0, 0, 0, 0, 0, 0, 0);           // the plane holds no instruction at all there
    colours(FROM, 77, 0, 0, 0, 0, 0, 0, 0);
    OOZ80 cpu = nine();
    state.getRegister(RegisterName.HL).write(FROM);
    state.getRegister(RegisterName.DE).write(TO);

    run(cpu, 1);
    code(CODE + 1, 0x12);                            // LD (DE),A
    run(cpu, 1);

    assertEquals(77, colourOf(TO, 0), "they decoded what the machine decoded, not what their planes said");
  }
}
