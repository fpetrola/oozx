package com.fpetrola.z80.minizx.emulation;

import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.RZXPlayerIO;
import com.fpetrola.z80.minizx.SpectrumApplication;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class PlayRecordedGame {
  public static void main(String[] args) throws Exception {
    System.setProperty("rzx.advance", "ins");
    String recording = args[0], classes = args[1], className = args[2];
    int entry = Integer.parseInt(args[3], 16);

    EmulatedMiniZX emulator = EmulatedMiniZX.ofRecording(recording, -1, null).stoppingAt(entry);
    emulator.start();
    RZXPlayerIO player = (RZXPlayerIO) emulator.ooz80.getState().getIo();
    player.setStreamed(true);
    System.out.println("recording reaches $%X at frame %d".formatted(entry, player.getCurrentFrameIndex()));

    Class<?> type = new URLClassLoader(new URL[]{Path.of(classes).toUri().toURL()}, PlayRecordedGame.class.getClassLoader()).loadClass(className);
    MiniZX game = (MiniZX) type.getConstructor().newInstance();
    game.loadState(emulator.ooz80.getState());
    SpectrumApplication.io = player;

    long[] ins = {0};
    player.addInListener((port, value) -> ins[0]++);
    Thread progress = new Thread(() -> {
      while (true) {
        System.out.println("frame " + player.getCurrentFrameIndex() + "  ins " + ins[0] + "  instructions " + game.fetchCounter + "  pc " + Integer.toHexString(game.PC));
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          return;
        }
      }
    });
    progress.setDaemon(true);
    progress.start();
    try {
      type.getMethod("$%X".formatted(entry)).invoke(game);
    } catch (java.lang.reflect.InvocationTargetException e) {
      String detail = e.getCause() instanceof com.fpetrola.z80.minizx.StackException stack ? " to $" + Integer.toHexString(stack.getNextPC()) : "";
      System.out.println("ended at frame " + player.getCurrentFrameIndex() + " at $" + Integer.toHexString(game.PC) + ": " + e.getCause() + detail);
      e.getCause().printStackTrace(System.out);
    }
  }
}
