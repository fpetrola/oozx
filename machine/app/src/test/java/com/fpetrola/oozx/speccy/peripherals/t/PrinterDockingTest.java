package com.fpetrola.oozx.speccy.peripherals.t;

import com.fpetrola.oozx.speccy.devices.EmulatorWindow;
import com.fpetrola.oozx.speccy.devices.printer.PrinterInternalFrame;
import javax.swing.JComponent;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.devices.printer.ZxPrinterPeripheral;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import javax.swing.JInternalFrame;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Clipping the printer onto a machine is what plugs it in.
 * <p>
 * The window is the printer: attached, that machine has one and LPRINT reaches this paper;
 * detached, it has none. Nothing about that is cosmetic, so it is worth a test even though the
 * rest of the window needs eyes.
 */
class PrinterDockingTest {

  private Speccy speccy() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.machine.select(speccy.spec48);
    return speccy;
  }

  /** A machine's window, as far as a printer can tell: it says which machine it shows. */
  private JInternalFrame windowOf(Speccy speccy) {
    class Machine extends JInternalFrame implements EmulatorWindow {
      public JComponent picture() {
        return this;
      }

      public Speccy machine() {
        return speccy;
      }
    }
    return new Machine();
  }

  /** The window asks for the change; the emulator's own thread makes it, between instructions. */
  private void letTheEmulatorCatchUp(Speccy speccy) {
    speccy.z80.doOpcodes();
  }

  @Test
  void attachingPlugsThePrinterIn() {
    Speccy speccy = speccy();
    PrinterInternalFrame printer = new PrinterInternalFrame();

    assertFalse(speccy.peripherals.isActive(ZxPrinterPeripheral.class),
        "a printer nobody has clipped on is not plugged in");

    printer.attachTo(windowOf(speccy));
    letTheEmulatorCatchUp(speccy);
    assertTrue(speccy.peripherals.isActive(ZxPrinterPeripheral.class),
        "clipping the printer onto the machine did not plug it in");
  }

  @Test
  void theMachineClosingTakesThePrinterWithIt() {
    Speccy speccy = speccy();
    PrinterInternalFrame printer = new PrinterInternalFrame();
    JInternalFrame machine = windowOf(speccy);
    printer.attachTo(machine);
    letTheEmulatorCatchUp(speccy);

    machine.dispose();
    letTheEmulatorCatchUp(speccy);
    assertFalse(speccy.peripherals.isActive(ZxPrinterPeripheral.class),
        "the machine went away and its printer stayed plugged into it");
  }
}
