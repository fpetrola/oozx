package model.tests.plugins;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.peripherals.SpeccyEmulatorCore;
import org.junit.jupiter.api.Test;

/**
 * The options the controls plugins tell a machine by name: the speed tool, the sound tool, and the pause a session
 * mapping restores. Said the way they say them, since they are built against these names.
 */
class WhatTheToolsAskOfAMachineTest {
  private static SpeccyEmulatorCore machine() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    return new SpeccyEmulatorCore(speccy);
  }

  @Test
  void theSpeedToolTurnsTurboOnAndOff() {
    SpeccyEmulatorCore machine = machine();

    machine.setGeneralOption("speed", 400);
    assertTrue(machine.isTurboMode());
    machine.setGeneralOption("speed", 100);
    assertFalse(machine.isTurboMode());
  }

  @Test
  void theSoundToolMutesAndUnmutes() {
    SpeccyEmulatorCore machine = machine();

    machine.setGeneralOption("mute", !machine.isMuted());
    assertTrue(machine.isMuted());
    machine.setGeneralOption("mute", !machine.isMuted());
    assertFalse(machine.isMuted());
  }

  @Test
  void aSessionPausesAndResumes() {
    SpeccyEmulatorCore machine = machine();

    machine.setGeneralOption("pause", true);
    assertTrue(machine.isPaused());
    machine.setGeneralOption("pause", false);
    assertFalse(machine.isPaused());
  }
}
