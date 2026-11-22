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

package com.fpetrola.oozx.generated;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.PortTraffic;
import com.fpetrola.oozx.speccy.modules.z80.Processors;
import com.fpetrola.z80.registers.RegisterName;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A port write says which instruction made it and where the stack stood, on every processor alike: what tells one
 * sound of a game from another is the routine that played it and where that routine was called from.
 */
class AWriteSaysWhoMadeItTest {

  private static final int PROGRAM = 0x8000, ROUTINE = 0x8100, STACK = 0xC000;
  /** DI; LD SP,STACK; again: CALL ROUTINE; JR again */
  private static final int[] CALLING = {0xF3, 0x31, STACK & 0xff, STACK >> 8, 0xCD, ROUTINE & 0xff, ROUTINE >> 8, 0x18, 0xFB};
  /** IN A,(0xFE); LD A,0x10; OUT (0xFE),A; RET */
  private static final int[] SOUNDING = {0xDB, 0xFE, 0x3E, 0x10, 0xD3, 0xFE, 0xC9};
  private static final int RETURNS_TO = PROGRAM + 7;

  record Written(int port, int value, int pc, int sp, int stackTop) {
  }

  @Test
  void theInstructionAndTheStackOfAWriteAreTheSameOnEveryProcessor() {
    Written generated = firstSpeakerWrite("Generated"), oop = firstSpeakerWrite("OOP");

    assertEquals(0x10, oop.value());
    assertEquals(RETURNS_TO, oop.stackTop(), "the top of the stack is where the routine returns to");
    assertEquals(STACK - 2, oop.sp());
    assertEquals(ROUTINE + 4, oop.pc(), "the write is said to come from its OUT");
    assertEquals(oop, generated, "the generated processor says what the OOP one says");
  }

  private static Written firstSpeakerWrite(String processor) {
    dev.plugdesk.app.Configuration.shared().setValue("machine", "processor", processor);
    try {
      Speccy speccy = MachineTest.silentMachine();
      for (int i = 0; i < CALLING.length; i++) speccy.memory.poke(PROGRAM + i, (byte) CALLING[i]);
      for (int i = 0; i < SOUNDING.length; i++) speccy.memory.poke(ROUTINE + i, (byte) SOUNDING[i]);
      speccy.cpu.getOoz80().getState().getRegister(RegisterName.PC).write(PROGRAM);
      PortTraffic traffic = speccy.processors;
      List<Written> heard = new ArrayList<>();
      List<Integer> read = new ArrayList<>();
      Processors.ReadListener reading = (port, value) -> read.add(port & 0xff);
      Processors.WriteListener listening = new Processors.WriteListener() {
        public void written(long tstates, int port, int value, int pc, int sp) {
          if ((port & 0xff) == 0xFE)
            heard.add(new Written(port & 0xff, value, pc, sp, speccy.memory.peek(sp) & 0xff | (speccy.memory.peek(sp + 1) & 0xff) << 8));
        }
      };
      traffic.whenWritten(listening);
      traffic.whenRead(reading);
      MachineTest.runFrames(speccy, 2);
      traffic.stopTellingAboutWrites(listening);
      traffic.stopTellingAboutReads(reading);
      assertEquals(processor, speccy.processors.current());
      assertFalse(heard.isEmpty(), processor + " told of no write to the speaker");
      assertFalse(read.isEmpty(), processor + " told of no read of the keyboard");
      return heard.getFirst();
    } finally {
      dev.plugdesk.app.Configuration.shared().setValue("machine", "processor", null);
    }
  }
}
