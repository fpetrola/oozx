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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import org.junit.jupiter.api.Test;
import java.util.List;
import com.fpetrola.oozx.speccy.devices.ay.AyPlus3Peripheral;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression coverage for a bug where neither AY port was decoded: synthesis ran and mixed in,
 * but the chip never received any register writes, so 128K games had beeper effects with no
 * AY music.
 */
class AySoundPortsTest {

  private static Speccy machine(String wanted) {
    Speccy speccy = Speccy.create();
    speccy.sound.setCard(new SilentSoundDevice() {
      public void play(int[] data, int length) {
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

  /** Write count from whichever AY peripheral is present; the mixer no longer tracks this
   * itself, so it is asked of the peripheral directly. */
  private static long writesTo(Speccy speccy) {
    for (Class<? extends Peripheral> kind : List.of(AyPeripheral.class, AyPlus3Peripheral.class)) {
      Peripheral peripheral = speccy.peripheralRegistry.find(kind);
      if (peripheral instanceof AyPeripheral ay && ay.writes() > 0) {
        return ay.writes();
      }
    }
    return 0;
  }

  /** Selects a register on 0xFFFD and writes its value on 0xBFFD, twice. */
  private static long writeTwoRegisters(Speccy speccy) {
    long before = writesTo(speccy);
    speccy.ports.write(0xFFFD, (byte) 7);
    speccy.ports.write(0xBFFD, (byte) 0x38);
    speccy.ports.write(0xFFFD, (byte) 8);
    speccy.ports.write(0xBFFD, (byte) 0x0F);
    return writesTo(speccy) - before;
  }

  @Test
  void a_128k_machine_hears_its_sound_chip() {
    assertEquals(2, writeTwoRegisters(machine("Spec128")),
        "the chip was written to twice and heard nothing, so there is no music");
  }

  /** Regression: the AY_PLUS3 enum entry once carried no implementation class, so +2A/+3
   * machines declared the chip present with nothing backing it, silencing AY music. */
  @Test
  void a_plus_two_a_hears_its_sound_chip_as_well() {
    assertEquals(2, writeTwoRegisters(machine("SpecPlus2A")),
        "a +2A has an AY and heard nothing on it");
    assertEquals(2, writeTwoRegisters(machine("SpecPlus3")), "and so does a +3");
  }

  /** Regression: the register port was write-only, so read-after-write presence detection
   * failed and games fell back to beeper music despite an AY being present. */
  @Test
  void the_chip_can_be_read_back_which_is_how_a_game_finds_it() {
    Speccy speccy = machine("Spec128");
    speccy.ports.write(0xFFFD, (byte) 8);      // R8: channel A volume
    speccy.ports.write(0xBFFD, (byte) 0x0D);

    assertEquals(0x0D, speccy.ports.read(0xFFFD) & 0xFF,
        "written and read back is how a program decides there is a chip to play");
  }

  /** Unused register bits always read zero; the two I/O-port registers reflect pin state,
   * not the last written value, unless configured as outputs. */
  @Test
  void the_registers_answer_as_the_chip_does() {
    Speccy speccy = machine("Spec128");
    speccy.ports.write(0xFFFD, (byte) 1);      // R1: tone A period high byte, only 4 bits wide
    speccy.ports.write(0xBFFD, (byte) 0xFF);
    assertEquals(0x0F, speccy.ports.read(0xFFFD) & 0xFF, "the other four bits are not there");

    speccy.ports.write(0xFFFD, (byte) 15);     // R15: the 8912 variant has no port B
    assertEquals(0xFF, speccy.ports.read(0xFFFD) & 0xFF);
  }

  @Test
  void a_48k_machine_has_no_such_chip_to_hear() {
    // A 48K genuinely has no AY; any response here would be emulator-only, impossible on real hardware.
    assertEquals(0, writeTwoRegisters(machine("Spec48")),
        "a 48K machine answered on the sound chip's ports");
  }
}
