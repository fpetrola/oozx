package com.fpetrola.oozx.proto.inflate;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.rzx.RzxSession;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** Plays a recording for a while and writes what is on the screen, so a sprite can be cut out. */
public class Grab {

  private static final int[] PALETTE = {
      0x000000, 0x0000D7, 0xD70000, 0xD700D7, 0x00D700, 0x00D7D7, 0xD7D700, 0xD7D7D7,
      0x000000, 0x0000FF, 0xFF0000, 0xFF00FF, 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFFFFFF};

  public static void main(String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    Emulation.noTest = true;
    RzxSession session = RzxSession.open(new File(args[0]));
    session.getSpeccy().sound.setJavaSoundDevice(new JavaSoundDevice() {
      public void sound_lowlevel_frame(int[] data, int length) {
      }
    });
    int frames = Integer.parseInt(args[1]);
    session.getPlayback().playFrames(frames);
    ImageIO.write(screen(session.getSpeccy()), "png", new File(args[2]));
    System.out.println("wrote " + args[2] + " after " + frames + " frames");
    System.exit(0);
  }

  /** The screen as it is in memory: the thirds, the scan-line interleave, and the attributes. */
  static BufferedImage screen(Speccy speccy) {
    BufferedImage image = new BufferedImage(256, 192, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < 192; y++) {
      int row = 0x4000 + ((y & 0xC0) << 5) + ((y & 7) << 8) + (((y >> 3) & 7) << 5);
      for (int column = 0; column < 32; column++) {
        int bits = speccy.memory.readByteInternal(row + column) & 0xFF;
        int attribute = speccy.memory.readByteInternal(0x5800 + (y >> 3) * 32 + column) & 0xFF;
        int bright = (attribute & 0x40) != 0 ? 8 : 0;
        int ink = PALETTE[(attribute & 7) | bright];
        int paper = PALETTE[((attribute >> 3) & 7) | bright];
        for (int bit = 0; bit < 8; bit++) {
          image.setRGB(column * 8 + bit, y, (bits & (0x80 >> bit)) != 0 ? ink : paper);
        }
      }
    }
    return image;
  }
}
