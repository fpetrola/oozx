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
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.devices.EmulatorWindow;
import com.fpetrola.oozx.speccy.devices.interface2.Cartridge;
import com.fpetrola.oozx.speccy.devices.interface2.Interface2Frame;
import com.fpetrola.oozx.speccy.devices.interface2.Interface2Peripheral;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Interface2Test {

  /** DI; LD A,2; OUT (0xFE),A; JR $ - a cartridge that makes the border red and stays there. */
  private static Cartridge redBorder() {
    byte[] image = new byte[Cartridge.SIZE];
    byte[] code = {(byte) 0xF3, 0x3E, 0x02, (byte) 0xD3, (byte) 0xFE, 0x18, (byte) 0xFE};
    System.arraycopy(code, 0, image, 0, code.length);
    return new Cartridge("red border", image, "red.rom");
  }

  private Speccy speccy() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.machine.select(speccy.spec48);
    return speccy;
  }

  private Interface2Peripheral interface2(Speccy speccy) {
    return (Interface2Peripheral) speccy.peripherals.find(Interface2Peripheral.class);
  }

  private void runFrames(Speccy speccy, int frames) {
    long until = speccy.machine.current.frameCount() + frames;
    while (speccy.machine.current.frameCount() < until) {
      speccy.z80.doOpcodes();
      speccy.eventManager.eventDoEvents();
    }
  }

  private int pc(Speccy speccy) {
    return speccy.z80.ooz80.getState().getPc().read();
  }

  private int byteAt(Speccy speccy, int address) {
    return speccy.memory.readByteInternal(address) & 0xff;
  }

  @Test
  void aCartridgeTakesThePlaceOfTheRomAndTheMachineRunsIt() {
    Speccy speccy = speccy();
    interface2(speccy).plugIn(true);
    speccy.peripherals.update();
    assertEquals(0xF3, byteAt(speccy, 0), "the 48K ROM starts with DI, and it should still be there");

    interface2(speccy).insert(redBorder());
    assertEquals(0x3E, byteAt(speccy, 1), "the cartridge is not at the bottom of memory");
    runFrames(speccy, 2);
    assertEquals(5, pc(speccy), "the machine is not running the cartridge's loop");
  }

  @Test
  void takingItOutGivesTheMachineItsRomBack() {
    Speccy speccy = speccy();
    interface2(speccy).plugIn(true);
    speccy.peripherals.update();
    interface2(speccy).insert(redBorder());
    interface2(speccy).eject();
    assertEquals(0xF3, byteAt(speccy, 0), "the ROM did not come back");
    assertEquals(0xAF, byteAt(speccy, 1), "the ROM did not come back");
  }

  @Test
  void andSoDoesUnpluggingTheInterface() {
    Speccy speccy = speccy();
    interface2(speccy).plugIn(true);
    speccy.peripherals.update();
    interface2(speccy).insert(redBorder());
    interface2(speccy).plugIn(false);
    speccy.peripherals.update();
    assertFalse(speccy.peripherals.isActive(Interface2Peripheral.class));
    assertEquals(0xF3, byteAt(speccy, 0), "unplugged, the cartridge is still over the ROM");
  }

  @Test
  void itGoesOnTheSinclairMachinesAndNotOnAPlus3() {
    Speccy speccy = speccy();
    Interface2Peripheral interface2 = interface2(speccy);
    assertTrue(interface2.fitsOn(speccy.spec48));
    assertTrue(interface2.fitsOn(speccy.spec128));
    assertTrue(interface2.fitsOn(speccy.specPlus2));
    assertFalse(interface2.fitsOn(speccy.specPlus3), "a +3 has no /ROMCS on its edge connector");
    assertFalse(interface2.fitsOn(speccy.specPlus2a));
    assertFalse(interface2.fitsOn(speccy.pentagon));
  }

  @Test
  void clippingTheWindowOntoAMachinePlugsItIn() {
    Speccy speccy = speccy();
    class Machine extends JInternalFrame implements EmulatorWindow {
      public JComponent picture() {
        return this;
      }

      public Speccy machine() {
        return speccy;
      }
    }
    Interface2Frame window = new Interface2Frame();
    assertFalse(speccy.peripherals.isActive(Interface2Peripheral.class));
    window.attachTo(new Machine());
    speccy.z80.doOpcodes();
    assertTrue(speccy.peripherals.isActive(Interface2Peripheral.class), "clipping it on did not plug it in");
  }
}
