package com.fpetrola.z80.minizx.emulation;

import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.RZXPlayerIO;
import com.fpetrola.z80.minizx.SpectrumApplication;

import java.util.concurrent.locks.LockSupport;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

public class PlayTranslatedGame {
  private static final long FETCHES_PER_FRAME = 80000, NANOS_PER_FRAME = 20_000_000;

  public static void main(String[] args) throws Exception {
    boolean replaying = args.length > 0;
    int entry = 0xFE65;
    MiniZX game = replaying ? replaying(args[0], entry, Emlyn.class) : playing("/home/fernando/detodo/spectrum/emlyn_r4.rzx", entry, Emlyn.class);
    try {
      game.run(entry);
    } catch (RuntimeException e) {
      String detail = e instanceof com.fpetrola.z80.minizx.StackException stack ? " to $" + Integer.toHexString(stack.getNextPC()) : "";
      System.out.println("ended at $" + Integer.toHexString(game.PC) + ": " + e + detail);
      e.printStackTrace(System.out);
    }
  }

  private static Predicate<Integer> atSpectrumSpeed() {
    long start = System.nanoTime();
    long[] firstFetches = {-1}, nextFrame = {0};
    return fetches -> {
      if (firstFetches[0] < 0)
        nextFrame[0] = (firstFetches[0] = fetches) + FETCHES_PER_FRAME;
      long ahead = start + (fetches - firstFetches[0]) * NANOS_PER_FRAME / FETCHES_PER_FRAME - System.nanoTime();
      if (ahead > 1_000_000)
        LockSupport.parkNanos(ahead);
      boolean frameEnded = fetches >= nextFrame[0];
      if (frameEnded)
        nextFrame[0] = fetches + FETCHES_PER_FRAME;
      return frameEnded;
    };
  }

  private static MiniZX playing(String recording, int entry, Class<?> type) throws Exception {
    EmulatedMiniZX emulator = EmulatedMiniZX.ofRecording(recording, -1, null).stoppingAt(entry);
    emulator.start();
    MiniZX game = (MiniZX) type.getConstructor().newInstance();
    game.loadState(emulator.ooz80.getState());
    game.setInterruptionCondition(atSpectrumSpeed());
    return game;
  }

  private static MiniZX replaying(String recording, int entry, Class<?> type) throws Exception {
    System.setProperty("rzx.advance", "ins");
    EmulatedMiniZX emulator = EmulatedMiniZX.ofRecording(recording, -1, null).stoppingAt(entry);
    emulator.start();
    RZXPlayerIO player = (RZXPlayerIO) emulator.ooz80.getState().getIo();
    System.out.println("recording reaches $%X at frame %d".formatted(entry, player.getCurrentFrameIndex()));
    MiniZX game = (MiniZX) type.getConstructor().newInstance();
    game.loadState(emulator.ooz80.getState());
    game.fetchCounter = emulator.playbackFetches();
    SpectrumApplication.io = player;
    player.setAcceptsInterrupt(game::acceptsInterrupt);
    IntPredicate endOfFrame = player.getInterruptionCondition();
    Predicate<Integer> pace = atSpectrumSpeed();
    game.setInterruptionCondition(fetches -> {
      pace.test(fetches);
      return endOfFrame.test(fetches);
    });

    Thread progress = new Thread(() -> {
      while (true) {
        System.out.println("frame " + player.getCurrentFrameIndex() + "  instructions " + game.fetchCounter + "  pc " + Integer.toHexString(game.PC));
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
