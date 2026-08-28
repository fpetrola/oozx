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

package model.tests;

import com.fpetrola.oozx.Fuse;
import com.fpetrola.oozx.fuse.OOSpectrumConnector;
import com.fpetrola.oozx.fuse.sound.JavaSoundDevice;
import com.fpetrola.z80.registers.RegisterName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the emulator's observable state after a fixed run, so a refactor that was meant to be
 * behaviour-preserving has to prove it.
 * <p>
 * This is deliberately the cheapest fixture that still covers the whole machine: reset a 48K
 * Spectrum and let its own ROM boot. No tape, no network, no snapshot file — the ROMs ship in
 * {@code src/main/resources/roms} and the boot exercises the Z80, memory paging, the event
 * queue, the ULA and the display. It runs in a couple of seconds.
 * <p>
 * It is the complement of {@link TzxLoadingTest}, not a replacement: that one answers "can we
 * still load real tapes", which needs the network and takes about eighty seconds. This one
 * answers "did anything at all shift", and is meant to be run after every commit of a refactor.
 * <p>
 * When a hash moves, the test writes the screen to {@code target/regression-screens} so the
 * change can be looked at instead of guessed.
 */
public class EmulationRegressionTest {

  /** Long enough for the 48K ROM to finish its boot and settle on the copyright screen. */
  private static final int BOOT_FRAMES = 200;

  private static final int SCREEN_BASE = 0x4000;
  private static final int SCREEN_END = 0x5B00;    // display file + attributes, 6912 bytes
  private static final int SYSVARS_END = 0x5D00;   // the ROM's system variables
  private static final int RAM_END = 0x10000;

  /**
   * Recorded on 913b58ab. When one of these moves, either the change was not behaviour
   * preserving, or it was and the new value belongs here — but that has to be a decision,
   * which is the whole point of writing them down.
   */
  private static final Map<String, String> EXPECTED = new LinkedHashMap<>() {{
    put("screen", "c2c7ee9cb8d9d65b");
    put("sysvars", "ce13e0af8eacd025");
    put("userram", "b2fe99c0aa3f3310");
    put("registers", "25183beed65fba04");
  }};

  @Test
  public void bootingTheRomLandsOnAKnownState() throws Exception {
    Fuse fuse = bootedSpectrum();
    runFrames(fuse, BOOT_FRAMES);

    // Three disjoint ranges rather than one hash over all of RAM: when this fails, the region
    // that moved is already the first half of the diagnosis.
    Map<String, String> actual = new LinkedHashMap<>();
    actual.put("screen", digest(readRange(fuse, SCREEN_BASE, SCREEN_END)));
    actual.put("sysvars", digest(readRange(fuse, SCREEN_END, SYSVARS_END)));
    actual.put("userram", digest(readRange(fuse, SYSVARS_END, RAM_END)));
    actual.put("registers", digest(registerDump(fuse).getBytes("UTF-8")));

    // Always, not only on failure: a screen from the failing run is of little use without the
    // one from the run that passed.
    File png = dumpScreen(fuse, "boot");

    System.out.println("--- state after " + BOOT_FRAMES + " frames ---");
    actual.forEach((name, hash) -> System.out.printf("%-10s %s%n", name, hash));
    System.out.println("registers  " + registerDump(fuse));
    System.out.println("screen -> " + png.getAbsolutePath());

    EXPECTED.forEach((name, expected) -> assertEquals(expected, actual.get(name),
        name + " changed; look at target/regression-screens and decide whether that was intended"));
  }

  /**
   * A plain {@code new Fuse()}, the way OOSpectrumLauncher builds one — FuseBaseForTests
   * installs an instrumented clock that records every tState update. Sound and display are
   * silenced so the run is headless and does not wait on anything.
   */
  private Fuse bootedSpectrum() {
    OOSpectrumConnector.noTest = true;
    Fuse fuse = new Fuse();
    fuse.sound.setJavaSoundDevice(new JavaSoundDevice() {
      public void sound_lowlevel_frame(int[] data, int len) {
      }
    });
    fuse.init();
    fuse.uiDisplay.active = false;
    fuse.z80.bridgeCommand = (a, b) -> null;
    return fuse;
  }

  /**
   * Counts frames by watching the clock wrap, not by aiming at an absolute tState count:
   * Spectrum.spectrumFrame subtracts a whole frame from the clock at every border, so the
   * clock is not monotonic and an absolute target is never reached.
   */
  private void runFrames(Fuse fuse, int frames) {
    long previous = fuse.zxClock.getTStates();
    int seen = 0;
    while (seen < frames) {
      fuse.z80.doOpcodes();
      fuse.eventManager.eventDoEvents();

      long now = fuse.zxClock.getTStates();
      if (now < previous) seen++;
      previous = now;
    }
  }

  private byte[] readRange(Fuse fuse, int from, int to) {
    byte[] bytes = new byte[to - from];
    for (int address = from; address < to; address++) {
      bytes[address - from] = (byte) fuse.memory.readByteInternal(address);
    }
    return bytes;
  }

  private String registerDump(Fuse fuse) {
    var state = fuse.z80.ooz80.getState();
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

  /**
   * The display file is not linear: within a third, the pixel row inside the character varies
   * first and the character row second. Attributes are linear and live separately at 0x5800.
   */
  private File dumpScreen(Fuse fuse, String name) throws Exception {
    int[] palette = {
        0x000000, 0x0000D7, 0xD70000, 0xD700D7, 0x00D700, 0x00D7D7, 0xD7D700, 0xD7D7D7,
        0x000000, 0x0000FF, 0xFF0000, 0xFF00FF, 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};

    BufferedImage image = new BufferedImage(256, 192, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < 192; y++) {
      int rowBase = SCREEN_BASE + ((y >> 6) << 11) + ((y & 7) << 8) + (((y >> 3) & 7) << 5);
      for (int column = 0; column < 32; column++) {
        int bits = fuse.memory.readByteInternal(rowBase + column) & 0xFF;
        int attribute = fuse.memory.readByteInternal(0x5800 + (y >> 3) * 32 + column) & 0xFF;
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
