/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package model.tests.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * That the sound chip of a 128K machine hears what is written to it.
 * <p>
 * The synthesis for it had been in place all along, mixed into the output and fed nothing,
 * because neither of its two ports was decoded anywhere: a game's music went to a chip that
 * never heard a word. Effects came through regardless - those are the beeper, on another port -
 * which is why a 128K game was half audible rather than silent.
 */
class AySoundPortsTest {

  private static Speccy machine(String wanted) {
    OOSpectrumConnector.noTest = true;
    Speccy speccy = Speccy.create();
    speccy.sound.setJavaSoundDevice(new JavaSoundDevice() {
      public void sound_lowlevel_frame(int[] data, int length) {
      }
    });
    speccy.init();
    speccy.machine.getMachineTypes().stream()
        .filter(type -> type.getClass().getSimpleName().equals(wanted))
        .findFirst().ifPresent(type -> {
          speccy.machine.selectDefault();
          speccy.machine.select(type);
        });
    return speccy;
  }

  /** A register is chosen on one port and its value sent on the other. */
  private static long writeTwoRegisters(Speccy speccy) {
    long before = speccy.sound.ayWrites;
    speccy.periph.writePort(0xFFFD, (byte) 7);
    speccy.periph.writePort(0xBFFD, (byte) 0x38);
    speccy.periph.writePort(0xFFFD, (byte) 8);
    speccy.periph.writePort(0xBFFD, (byte) 0x0F);
    return speccy.sound.ayWrites - before;
  }

  @Test
  void a_128k_machine_hears_its_sound_chip() {
    assertEquals(2, writeTwoRegisters(machine("Spec128")),
        "the chip was written to twice and heard nothing, so there is no music");
  }

  /**
   * The +2A and the +3 have the same chip, wired one wire differently, and were silent.
   * <p>
   * Those machines are given AY_PLUS3 rather than AY, and that entry of the enum carried no class
   * - so the chip was declared always present and there was nothing there to be present. It is
   * the same silence the plain AY had before it was given one, one line above it in the same enum.
   * A 128K game arriving on a +2A had effects and no music.
   */
  @Test
  void a_plus_two_a_hears_its_sound_chip_as_well() {
    assertEquals(2, writeTwoRegisters(machine("SpecPlus2A")),
        "a +2A has an AY and heard nothing on it");
    assertEquals(2, writeTwoRegisters(machine("SpecPlus3")), "and so does a +3");
  }

  /**
   * The chip answers as well as listens.
   * <p>
   * A program that writes a register and reads it back is asking whether there is a chip here, and
   * a port that only listens says no - so the program plays beeper music on a machine that has an
   * AY sitting in it. The register port was write-only.
   */
  @Test
  void the_chip_can_be_read_back_which_is_how_a_game_finds_it() {
    Speccy speccy = machine("Spec128");
    speccy.periph.writePort(0xFFFD, (byte) 8);      // channel A volume
    speccy.periph.writePort(0xBFFD, (byte) 0x0D);

    assertEquals(0x0D, speccy.periph.readPort(0xFFFD) & 0xFF,
        "written and read back is how a program decides there is a chip to play");
  }

  /**
   * Unused bits read back as zero, and the two I/O registers do not read back as what was written
   * at all: they answer from the pins unless the mixer says they are outputs.
   */
  @Test
  void the_registers_answer_as_the_chip_does() {
    Speccy speccy = machine("Spec128");
    speccy.periph.writePort(0xFFFD, (byte) 1);      // channel A period, high byte: four bits wide
    speccy.periph.writePort(0xBFFD, (byte) 0xFF);
    assertEquals(0x0F, speccy.periph.readPort(0xFFFD) & 0xFF, "the other four bits are not there");

    speccy.periph.writePort(0xFFFD, (byte) 15);     // the 8912 has no second I/O port
    assertEquals(0xFF, speccy.periph.readPort(0xFFFD) & 0xFF);
  }

  @Test
  void a_48k_machine_has_no_such_chip_to_hear() {
    // Not a detail: a 48K machine has no AY, and one answering there would be an emulator
    // playing music a real machine cannot.
    assertEquals(0, writeTwoRegisters(machine("Spec48")),
        "a 48K machine answered on the sound chip's ports");
  }
}
