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

package model.tests.regression;


import model.harness.MachineTest;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.EmulatorModule;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.ports.ContendedPortBus;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.ula.Ula;
import com.fpetrola.oozx.speccy.ports.PortBus;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.z80.registers.RegisterName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Fast whole-machine golden-state test: boots a 48K on its own ROM (bundled, no network) and
 * hashes fixed memory ranges plus registers, so an unintended behaviour change shows up in
 * seconds. Complements the slower, network-dependent TzxLoadingTest rather than replacing it.
 * A hash mismatch dumps the screen under {@code target/regression-screens} for inspection.
 */
public class EmulationRegressionTest extends MachineTest {

  /** Frames to reach the copyright screen; raised from 200 after a clock-wrap counting bug
   * undercounted real frames by roughly 1 in 4 during boot. */
  private static final int BOOT_FRAMES = 268;

  private static final int SCREEN_BASE = 0x4000;
  private static final int SCREEN_END = 0x5B00;    // 6912-byte display file plus attributes
  private static final int SYSVARS_END = 0x5D00;   // end of the ROM's system-variable area
  private static final int RAM_END = 0x10000;

  /** Golden hashes from commit 913b58ab; a mismatch means judge and update, not just accept. */
  private static final Map<String, String> EXPECTED = new LinkedHashMap<>() {{
    put("screen", "c2c7ee9cb8d9d65b");
    put("sysvars", "ce13e0af8eacd025");
    put("userram", "b2fe99c0aa3f3310");
    put("registers", "25183beed65fba04");
  }};

  @Test
  public void bootingTheRomLandsOnAKnownState() throws Exception {
    Speccy speccy = silentMachine();
    runFrames(speccy, BOOT_FRAMES);

    // Separate hashes per range point straight at which region changed, unlike one RAM-wide hash.
    Map<String, String> actual = new LinkedHashMap<>();
    actual.put("screen", digest(readRange(speccy, SCREEN_BASE, SCREEN_END)));
    actual.put("sysvars", digest(readRange(speccy, SCREEN_END, SYSVARS_END)));
    actual.put("userram", digest(readRange(speccy, SYSVARS_END, RAM_END)));
    actual.put("registers", digest(registerDump(speccy).getBytes("UTF-8")));

    // A screenshot from a passing run is needed too, for comparison against a future failure.
    File png = dumpScreen(speccy, "boot");

    System.out.println("--- state after " + BOOT_FRAMES + " frames ---");
    actual.forEach((name, hash) -> System.out.printf("%-10s %s%n", name, hash));
    System.out.println("registers  " + registerDump(speccy));
    System.out.println("screen -> " + png.getAbsolutePath());

    EXPECTED.forEach((name, expected) -> assertEquals(expected, actual.get(name),
        name + " changed; look at target/regression-screens and decide whether that was intended"));
  }

  /**
   * Guards against a missing @Singleton: two Tape instances would let Sound, the ULA and the
   * UI each drive a different deck silently, with no loud failure. Scope is per emulator
   * instance, not per JVM, since each Speccy.create builds an independent injector.
   */
  @Test
  public void twoEmulatorsShareNothing() {
    Speccy one = silentMachine();
    Speccy other = silentMachine();

    assertNotSame(one.machine.model(Spec48.class), other.machine.model(Spec48.class), "two emulators were handed the same 48K");
    assertNotSame(one.machine, other.machine, "and the same machine list");
    assertNotSame(one.peripheralRegistry, other.peripheralRegistry, "and the same peripheral bus");
  }

  @Test
  public void theGraphHandsOutOneOfEachSharedPart() {
    Injector injector = Guice.createInjector(new EmulatorModule(new SpectrumZ80Clock()));

    for (Class<?> shared : new Class<?>[]{
        Sound.class, MemoryBus.class, Ula.class,
        PortBus.class, ContendedPortBus.class}) {
      assertSame(injector.getInstance(shared), injector.getInstance(shared),
          shared.getSimpleName() + " is handed out more than once; it needs @Singleton");
    }

    assertNotSame(injector.getInstance(PortBus.class), injector.getInstance(ContendedPortBus.class),
        "the raw and the ULA-decorated peripheral bus collapsed into one object");
  }

  /**
   * Every field Speccy exposes must come from the injector, not a field initializer - a past bug
   * had Speccy hold its own EmulationSession while the Z80 held the injector's, so closing a
   * window ended a session nobody was reading and playback kept running silently.
   */
  @Test
  public void everyPartSpeccyExposesCameFromTheGraph() throws Exception {
    Injector injector = Guice.createInjector(new EmulatorModule(new SpectrumZ80Clock()));
    Speccy speccy = injector.getInstance(Speccy.class);

    for (Field field : Speccy.class.getFields()) {
      if (field.getType().isPrimitive()) continue;

      Object exposed = field.get(speccy);
      if (exposed == null) continue;

      Binding<?> binding = injector.getExistingBinding(Key.get(field.getType()));
      if (binding == null) continue;

      assertSame(binding.getProvider().get(), exposed,
          "speccy." + field.getName() + " was built outside the graph; ask the injector for it");
    }
  }

  /**
   * The default machine model must be pinned by name, not by Multibinder registration order
   * (which is unspecified) - getting it wrong means every 48K tape silently fails to load.
   */
  @Test
  public void switchingModelsFallsBackToThe48K() {
    Speccy speccy = silentMachine();

    // Exactly 8: an unexplained change here means a model got registered or dropped silently.
    assertEquals(13, speccy.machine.getMachineTypes().size(), "not every model was registered");
    assertSame(speccy.machine.model(Spec48.class), speccy.machine.current,
        "the machine did not come up as the 48K; check the @DefaultMachine binding");
  }

  /**
   * A missing configured ROM must fall back to the bundled one - previously dead code, since
   * the file-existence check always answered 0 and the fallback threw before it could run.
   */
  @Test
  public void aMissingRomFallsBackToTheOneTheMachineShippedWith() throws Exception {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));

    speccy.roms.choose("Spec48", "no-such.rom");
    speccy.init();
    speccy.picture.active = false;

    runFrames(speccy, BOOT_FRAMES);
    assertEquals(EXPECTED.get("screen"), digest(readRange(speccy, SCREEN_BASE, SCREEN_END)),
        "the fallback ROM did not produce the same boot as the configured one");
  }

  private byte[] readRange(Speccy speccy, int from, int to) {
    byte[] bytes = new byte[to - from];
    for (int address = from; address < to; address++) {
      bytes[address - from] = (byte) speccy.memory.peek(address);
    }
    return bytes;
  }

  private String registerDump(Speccy speccy) {
    var state = speccy.cpu.getOoz80().getState();
    StringBuilder dump = new StringBuilder();
    dump.append("PC=").append(state.getPc().read())
        .append(" SP=").append(state.getRegisterSP().read());
    for (RegisterName name : new RegisterName[]{
        RegisterName.AF, RegisterName.BC, RegisterName.DE, RegisterName.HL,
        RegisterName.IX, RegisterName.IY}) {
      dump.append(' ').append(name).append('=').append(state.getRegister(name).read());
    }
    return dump.toString();
  }

  private String digest(byte[] bytes) throws Exception {
    byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
    StringBuilder hex = new StringBuilder();
    for (int i = 0; i < 8; i++) hex.append(String.format("%02x", hash[i]));
    return hex.toString();
  }

  /** Reconstructs a PNG using the Spectrum's interleaved screen layout and 0x5800 attributes. */
  private File dumpScreen(Speccy speccy, String name) throws Exception {
    int[] palette = {
        0x000000, 0x0000D7, 0xD70000, 0xD700D7, 0x00D700, 0x00D7D7, 0xD7D700, 0xD7D7D7,
        0x000000, 0x0000FF, 0xFF0000, 0xFF00FF, 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};

    BufferedImage image = new BufferedImage(256, 192, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < 192; y++) {
      int rowBase = SCREEN_BASE + ((y >> 6) << 11) + ((y & 7) << 8) + (((y >> 3) & 7) << 5);
      for (int column = 0; column < 32; column++) {
        int bits = speccy.memory.peek(rowBase + column) & 0xFF;
        int attribute = speccy.memory.peek(0x5800 + (y >> 3) * 32 + column) & 0xFF;
        int bright = (attribute & 0x40) != 0 ? 8 : 0;
        int ink = palette[(attribute & 0x07) + bright];
        int paper = palette[((attribute >> 3) & 0x07) + bright];
        for (int bit = 0; bit < 8; bit++) {
          image.setRGB(column * 8 + bit, y, (bits & (0x80 >> bit)) != 0 ? ink : paper);
        }
      }
    }

    File dir = new File("target/regression-screens");
    dir.mkdirs();
    File png = new File(dir, name + ".png");
    ImageIO.write(image, "png", png);
    return png;
  }
}
