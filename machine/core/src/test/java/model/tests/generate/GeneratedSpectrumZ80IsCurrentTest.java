package model.tests.generate;

import model.tags.Slow;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The committed core is what this machine produces today. It is not only the instructions: this
 * core carries the machine's memory and its contention inside, so a change to ContendedMemory,
 * to Memory or to FusePhaseProcessor changes it too, and nothing else would notice.
 */
@Slow
public class GeneratedSpectrumZ80IsCurrentTest {
  @Test
  public void committedCoreIsWhatTheMachineGenerates() throws Exception {
    String committed = Files.readString(GenerateSpectrumZ80.TARGET);
    assertEquals(GenerateSpectrumZ80.generate(), committed,
        "GeneratedSpectrumZ80.java is stale: run mvn test -pl machine/core -Dtest=GenerateSpectrumZ80");
  }
}
