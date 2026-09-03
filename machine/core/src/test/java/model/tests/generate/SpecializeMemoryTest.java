package model.tests.generate;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import com.fpetrola.z80.generate.SourceIndex;
import com.fpetrola.z80.generate.Specializer;
import com.fpetrola.z80.memory.Memory;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.Statement;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

/** What the specializer makes of the machine's memory, printed. The short loop for building it. */
public class SpecializeMemoryTest {
  static final Path[] SOURCES = {Path.of("../../emulator/src/main/java"), Path.of("src/main/java")};

  @Test
  public void readAndWrite() {
    // The core being generated is the generated one, so the contention is not a listener outside.
    System.setProperty("oozx.cpu", "generated");
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(), binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    Memory memory = speccy.z80.ooz80.getState().getMemory();
    System.out.println("the memory is a " + memory.getClass().getName());
    Specializer specializer = new Specializer(new SourceIndex(SOURCES));
    specializer.terminals.remove(Memory.class);
    specializer.shared.put(speccy.memory, "ram");
    specializer.shared.put(speccy.ula, "ula");
    specializer.shared.put(speccy.zxClock, "clock");
    specializer.shared.put(speccy.display, "display");
    // The clock and the display stay calls: small, concrete, and off the arithmetic of an access.
    specializer.terminals.put(speccy.zxClock.getClass(), "clock");
    specializer.terminals.put(speccy.display.getClass(), "display");
    specializer.newCase();
    List<Statement> read = specializer.statementsOf(specializer.of(memory), "read", new NameExpr("address"), new NameExpr("fetching"));
    System.out.println("==== read");
    read.forEach(s -> System.out.println(s));
    specializer.newCase();
    System.out.println("==== write");
    specializer.statementsOf(specializer.of(memory), "write", new NameExpr("address"), new NameExpr("value")).forEach(s -> System.out.println(s));
  }
}
