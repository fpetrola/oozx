package model.tests.media;

import com.fpetrola.oozx.speccy.modules.z80.Processors;
import model.harness.MachineTest;
import com.fpetrola.oozx.rzx.RzxSession;
import com.fpetrola.z80.minizx.RzxPlayback;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Numbers for a recording on each core, the generated one first: mvn test -pl machine/app -Doozx.measure=true -Dtest=RzxCoreMeasurement. */
@EnabledIfSystemProperty(named = "oozx.measure", matches = "true")
class RzxCoreMeasurement extends MachineTest {
  @Test
  void framesAndInstructionsPerSecondOnEachCore() throws Exception {
    for (String processor : new String[]{"Generated", "OOP"}) {
      Processors.startsOn = processor;
      measure();
    }
  }

  private void measure() throws Exception {
    RzxSession session = RzxSession.open(model.harness.TestFiles.testFile("/rzx/jsw-full.rzx"));
    RzxPlayback playback = session.getPlayback();
    String core = session.getSpeccy().cpu.getOoz80().getClass().getSimpleName();
    long framesBefore = session.getSpeccy().machine.current.frameCount();
    long start = System.nanoTime();
    for (int frame = 0; frame < 6000; frame++) {
      playback.playFrame();
    }
    double seconds = (System.nanoTime() - start) / 1e9;
    System.out.printf("core=%s recordedFrames=6000 machineFrames=%d instructions=%d seconds=%.2f fps=%.0f speed=%.0f%%%n",
        core, session.getSpeccy().machine.current.frameCount() - framesBefore, playback.getInstructions(), seconds, 6000 / seconds, 6000 / seconds / 50 * 100);
    session.getSpeccy().picture.active = false;
    // The sound device paces the machine to the audio card: with it in the way this measures
    // JavaSound, and a loop that runs faster than real time waits forever on line.write.
    session.getSpeccy().sound.output.enabled = false;
    session.getSpeccy().sound.setCard(new com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice());
    for (int round = 0; round < 2; round++) {
      start = System.nanoTime();
      for (int frame = 0; frame < 3000; frame++)
        session.playFrame();
      seconds = (System.nanoTime() - start) / 1e9;
      System.out.printf("session core=%s frames=3000 (with presentFrame, no sound) seconds=%.2f fps=%.0f speed=%.0f%%%n", core, seconds, 3000 / seconds, 3000 / seconds / 50 * 100);
    }
    session.release();
  }
}
