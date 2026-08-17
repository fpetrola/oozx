package com.fpetrola.oozx.generated;

import model.tags.Slow;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The committed core is what this machine produces today. It is not only the instructions: this
 * core carries the machine's memory and its contention inside, so a change to ContendedMemory,
 * to Memory or to RecordingPhaseProcessor changes it too, and nothing else would notice.
 */
@Slow
public class GeneratedSpectrumZ80IsCurrentTest {
  static final Path SOURCE = Path.of("src/main/java/com/fpetrola/oozx/generated/GeneratedSpectrumZ80.java");

  @Test
  public void committedCoreIsWhatTheMachineGenerates() throws Exception {
    String committed = Files.readString(SOURCE);
    assertEquals(GeneratedCores.written(), committed,
        "GeneratedSpectrumZ80.java is stale: build machine/generated, which writes it");
  }
}
