package com.fpetrola.z80.minizx.emulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.RZXPlayerIO;
import com.fpetrola.z80.minizx.SpectrumApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * Runs a translated game as translated-games.json says: "game" names the entry of "games" to run and "mode" is
 * "play" (keyboard, at Spectrum speed, from the point where the game's recording reaches its entry) or "replay"
 * (the recording's inputs drive the translation, with progress on the console). -Dgame=, -Dmode= and -Dconfig=
 * override the file, which is looked for in the working directory and in translation/translator.
 */
public class PlayTranslatedGame {
  private static final long TSTATES_PER_FRAME = 69888, NANOS_PER_FRAME = 20_000_000;

  public record Translated(String title, String type, String recording, String snapshot, String entry) {
    int entryAddress() {
      return Integer.parseInt(entry, 16);
    }

    Class<?> gameClass() throws ClassNotFoundException {
      return Class.forName(PlayTranslatedGame.class.getPackageName() + "." + type);
    }
  }

  public record Config(String game, String mode, String recording, Map<String, Translated> games) {
  }

  public static void main(String[] args) throws Exception {
    Config config = new ObjectMapper().readValue(configFile().toFile(), Config.class);
    String name = System.getProperty("game", config.game());
    Translated translated = config.games().get(name);
    if (translated == null)
      throw new IllegalArgumentException("no game " + name + " among " + config.games().keySet());
    String recording = args.length > 0 ? args[0] : config.recording() != null ? config.recording() : translated.recording();
    int entry = translated.entryAddress();
    System.out.println(translated.title() + " (" + translated.type() + ") from $" + translated.entry() + ", " + System.getProperty("mode", config.mode()));
    MiniZX game = System.getProperty("mode", config.mode()).equals("replay") ? replaying(recording, entry, translated.gameClass()) : playing(translated, recording, entry);
    try {
      game.run(entry);
    } catch (RuntimeException e) {
      String detail = e instanceof com.fpetrola.z80.minizx.StackException stack ? " to $" + Integer.toHexString(stack.getNextPC()) : "";
      System.out.println("ended at $" + Integer.toHexString(game.PC) + ": " + e + detail);
      e.printStackTrace(System.out);
    }
  }

  private static Path configFile() {
    String name = System.getProperty("config", "translated-games.json");
    Path here = Path.of(name);
    return Files.exists(here) ? here : Path.of("translation/translator").resolve(name);
  }

  private static Predicate<Integer> atSpectrumSpeed(MiniZX game) {
    long start = System.nanoTime();
    long[] firstTstates = {-1}, nextFrame = {0};
    return fetches -> {
      long tstates = game.tstates;
      if (firstTstates[0] < 0)
        nextFrame[0] = (firstTstates[0] = tstates) + TSTATES_PER_FRAME;
      long ahead = start + (tstates - firstTstates[0]) * NANOS_PER_FRAME / TSTATES_PER_FRAME - System.nanoTime();
      if (ahead > 1_000_000)
        LockSupport.parkNanos(ahead);
      boolean frameEnded = tstates >= nextFrame[0];
      if (frameEnded)
        nextFrame[0] = tstates + TSTATES_PER_FRAME;
      return frameEnded;
    };
  }

  private static MiniZX playing(Translated translated, String recording, int entry) throws Exception {
    EmulatedMiniZX emulator = recording != null ? EmulatedMiniZX.ofRecording(recording, -1, null).stoppingAt(entry) : new EmulatedMiniZX(translated.snapshot(), 1, false, 0, false);
    emulator.start();
    MiniZX game = (MiniZX) translated.gameClass().getConstructor().newInstance();
    game.loadState(emulator.ooz80.getState());
    game.setInterruptionCondition(atSpectrumSpeed(game));
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
    Predicate<Integer> pace = atSpectrumSpeed(game);
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
