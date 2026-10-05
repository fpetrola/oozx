package com.fpetrola.z80.minizx.emulation;

import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.RZXPlayerIO;
import com.fpetrola.z80.minizx.SpectrumApplication;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;

public class PlayTranslatedGame {
  private static final long FETCHES_PER_FRAME = 7800, NANOS_PER_FRAME = 20_000_000;

  public static void main(String[] args) throws Exception {
    MiniZX game = new Emlyn();
    game.setInterruptionCondition(atSpectrumSpeed());
    try {
      Emlyn.class.getMethod("$94AA").invoke(game);
    } catch (java.lang.reflect.InvocationTargetException e) {
      String detail = e.getCause() instanceof com.fpetrola.z80.minizx.StackException stack ? " to $" + Integer.toHexString(stack.getNextPC()) : "";
      System.out.println("ended at $" + Integer.toHexString(game.PC) + ": " + e.getCause() + detail);
      e.getCause().printStackTrace(System.out);
    }
  }

  private static Predicate<Integer> atSpectrumSpeed() {
    long start = System.nanoTime();
    return fetches -> {
      long ahead = start + fetches * NANOS_PER_FRAME / FETCHES_PER_FRAME - System.nanoTime();
      if (ahead > 1_000_000)
        LockSupport.parkNanos(ahead);
      return false;
    };
  }

  private static MiniZX replaying(String recording, int entry, Class<?> type) throws Exception {
    System.setProperty("rzx.advance", "ins");
    EmulatedMiniZX emulator = EmulatedMiniZX.ofRecording(recording, -1, null).stoppingAt(entry);
    emulator.start();
    RZXPlayerIO player = (RZXPlayerIO) emulator.ooz80.getState().getIo();
    player.setStreamed(true);
    System.out.println("recording reaches $%X at frame %d".formatted(entry, player.getCurrentFrameIndex()));
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
    return game;
  }
}
