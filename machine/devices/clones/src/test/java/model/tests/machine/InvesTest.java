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

package model.tests.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Inves;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.z80.registers.RegisterName;
import model.harness.MachineTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * A 48K in the way a copy of a drawing is the drawing: everything is where it should be and
 * nothing is quite right. Each of these is one of the ways it is not.
 * <p>
 * Given the Sinclair ROM to read rather than its own, which is not here: none of what is asked
 * about below is in a ROM, and a machine has to have one before it will start at all.
 */
class InvesTest extends MachineTest {
  private final Speccy speccy = silentMachine();
  private List<String> itsOwn;

  @BeforeEach
  void withSomeRomToRead() {
    itsOwn = speccy.roms.files.get("Inves");
    speccy.roms.choose("Inves", "48.rom");
    speccy.machine.select(speccy.machine.model(Inves.class));
  }

  @AfterEach
  void backToItsOwn() {
    speccy.roms.files.put("Inves", itsOwn);
  }

  /**
   * Sixty-four K with the ROM over the first sixteen: what is read there is the ROM, and what is
   * written there is kept underneath it, where a 48K keeps nothing at all.
   */
  @Test
  void theRomIsReadAtTheBottomAndWhatIsWrittenThereGoesUnderneathIt() {
    int wasThere = speccy.memory.peek(0x1234);

    speccy.memory.write(0x1234, (byte) 0x5a);

    assertEquals(wasThere, speccy.memory.peek(0x1234), "the ROM answered with what was written over it");
    assertEquals(0x5a, speccy.banks.ram(1).bytes[0x1234] & 0xff, "and it was not kept underneath");
  }

  /** Its chip holds nothing up, anywhere. */
  @Test
  void nothingOfItIsContended() {
    for (int page = 0; page < 8; page++) {
      assertFalse(speccy.banks.ram(page).contended, "page " + page + " is held up on a machine that holds nothing up");
    }
  }

  /** Its line is four T-states longer than a Sinclair's, and its picture starts that much later. */
  @Test
  void itsLineIsLongerThanASinclairs() {
    assertEquals(228, speccy.machine.current.getTimings().tstatesPerLine());

    speccy.machine.select(speccy.machine.model(Spec48.class));
    assertEquals(224, speccy.machine.current.getTimings().tstatesPerLine(), "which is not what a 48K's is");
  }

  /** Nothing of the picture is ever left on its bus, so a port nobody answers is all ones. */
  @Test
  void aPortNobodyAnswersIsAllOnes() {
    assertEquals(0xff, speccy.ports.read(0x00ff) & 0xff);
  }

  /**
   * What a program writes to the chip that draws is put through whatever was lying in memory at
   * that same address. A program that writes a colour gets that colour and whatever was there.
   */
  @Test
  void whatIsWrittenToTheChipGoesThroughWhatIsInMemoryAtThatAddress() {
    // The port is 0xfe and what is in memory there is under the ROM, where the test can write.
    speccy.picture.active = true;
    speccy.banks.ram(1).bytes[0x00fe] = (byte) 0x03;

    speccy.ports.write(0x00fe, (byte) 0x07);
    speccy.zxClock.setTStates(0);
    speccy.display.frame();

    assertEquals(com.fpetrola.oozx.speccy.modules.display.Picture.SINCLAIR[0x03],
        speccy.picture.pixels[5 * com.fpetrola.oozx.speccy.modules.display.Picture.STRIDE + 8],
        "the colour that reached the border was not the one in memory and the one written together");
  }

  /**
   * Every interrupt it accepts leaves a byte of ones where the address of the next one is read
   * from. It is a fault of the machine, and the reason games whose interrupt table lands on that
   * byte come apart on this one and on nothing else.
   */
  @Test
  void everyInterruptLeavesAByteOfOnesWhereTheNextOnesAddressIsRead() {
    var state = speccy.cpu.getOoz80().getState();
    state.getRegister(RegisterName.I).write(0x80);
    state.getRegister(RegisterName.R).write(0x40);
    speccy.banks.ram(2).bytes[0x0040] = 0x00;
    state.setIff1(true);
    state.getPc().write(0x8000);

    speccy.zxClock.setTStates(0);
    speccy.cpu.interrupt(speccy.machine.current.getTimings().interruptLength());

    assertEquals(0x0038, state.getPc().read(), "it should have taken the interrupt at all");
    int r = state.getRegister(RegisterName.R).read() & 0xff;
    assertEquals(0xff, speccy.memory.peek(0x8000 | r) & 0xff,
        "nothing was written where the next interrupt reads its address");
  }

  /** A 48K does not do that, which is what makes it a fault rather than how a Spectrum works. */
  @Test
  void aSinclairLeavesNothingBehindWhenItTakesOne() {
    speccy.machine.select(speccy.machine.model(Spec48.class));
    var state = speccy.cpu.getOoz80().getState();
    state.getRegister(RegisterName.I).write(0x80);
    state.getRegister(RegisterName.R).write(0x40);
    speccy.banks.ram(2).bytes[0x0040] = 0x00;
    state.setIff1(true);
    state.getPc().write(0x8000);

    speccy.zxClock.setTStates(0);
    speccy.cpu.interrupt(speccy.machine.current.getTimings().interruptLength());

    int r = state.getRegister(RegisterName.R).read() & 0xff;
    assertNotEquals(0xff, speccy.memory.peek(0x8000 | r) & 0xff);
  }
}
