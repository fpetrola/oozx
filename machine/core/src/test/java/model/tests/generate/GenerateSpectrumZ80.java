package model.tests.generate;

import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.modules.Display;
import com.fpetrola.oozx.speccy.modules.Ula;
import com.fpetrola.oozx.speccy.modules.z80.FusePhaseProcessor;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.generate.CoreGenerator;
import com.fpetrola.z80.generate.GenerateZ80;
import com.fpetrola.z80.generate.SourceIndex;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the core generated against this machine, which reaches its memory without a call:
 * {@code mvn test -pl machine/core -Dtest=GenerateSpectrumZ80 -Doozx.slow=true}.
 */
public class GenerateSpectrumZ80 {
  public static final Path TARGET = Path.of("src/main/java/com/fpetrola/oozx/speccy/modules/z80/GeneratedSpectrumZ80.java");
  static final Path[] SOURCES = {Path.of("../../emulator/src/main/java"), Path.of("src/main/java")};

  public static String generate() {
    // The machine is set up the way the generated core runs it: the contention is not a listener.
    System.setProperty("oozx.cpu", "generated");
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(), binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    State state = speccy.z80.ooz80.getState();
    com.fpetrola.z80.memory.Memory memory = state.getMemory();

    CoreGenerator.Target target = new CoreGenerator.Target();
    target.packageName = "com.fpetrola.oozx.speccy.modules.z80";
    target.className = "GeneratedSpectrumZ80";
    target.held.put("ram", Memory.class);
    target.held.put("mapRead", com.fpetrola.oozx.MemoryPage[].class);
    target.held.put("mapWrite", com.fpetrola.oozx.MemoryPage[].class);
    target.held.put("ula", Ula.class);
    target.held.put("clock", SpectrumZ80Clock.class);
    target.held.put("display", Display.class);
    target.held.put("io", IO.class);
    target.contention = new FusePhaseProcessor(speccy.z80);
    target.helpers.put("read", memory);
    target.helperSignatures.put("read", "int read(int address, int fetching)");
    target.helpers.put("write", memory);
    target.helperSignatures.put("write", "void write(int address, int value)");

    CoreGenerator generator = new CoreGenerator(state, GenerateZ80.tables(state), new SourceIndex(SOURCES), target);
    // The machine's memory is taken apart; the objects it is reached through are held, not frozen.
    generator.spec.terminals.put(com.fpetrola.z80.memory.Memory.class, "");
    generator.spec.terminals.put(SpectrumZ80Clock.class, "clock");
    generator.spec.terminals.put(Display.class, "display");
    generator.spec.shared.put(speccy.memory, "ram");
    generator.spec.shared.put(speccy.memory.mapRead, "mapRead");
    generator.spec.shared.put(speccy.memory.mapWrite, "mapWrite");
    generator.spec.shared.put(speccy.ula, "ula");
    generator.spec.shared.put(speccy.zxClock, "clock");
    generator.spec.shared.put(speccy.display, "display");
    return generator.generate();
  }

  @Test
  public void write() throws Exception {
    Files.writeString(TARGET, generate());
    System.out.println("written " + TARGET.toAbsolutePath());
  }
}
