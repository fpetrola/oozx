/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.fpetrola.oozx.generated;

import com.fpetrola.oozx.speccy.modules.z80.Processors;
import com.fpetrola.oozx.EmulatorModule;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.ProcessorWiring;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.Extension;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.ula.Ula;
import com.fpetrola.oozx.speccy.modules.z80.SpeccyPhaseProcessor;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.z80.cpu.Core;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.OopCore;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.generate.CoreGenerator;
import com.fpetrola.z80.generate.Specializer;
import com.fpetrola.z80.generate.SourceIndex;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.multibindings.OptionalBinder;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * What this module brings a machine: the generated core as its Core. The first machine to ask
 * gets it made, compiled and kept under the user's cache directory, keyed by the model's sources
 * and the generator; every machine after that loads it in milliseconds. Where it cannot run the
 * machine gets the OOP core: under the test harness, whose instrumented memory the generated core
 * does not go through, on a runtime without a compiler, and in a build that did not package the
 * model's sources. Which processor a machine runs on is the machine's to decide; this only says
 * what it can be offered.
 */
public class GeneratedCores implements Extension {
  static final String PACKAGE = "com.fpetrola.oozx.generated";
  static final String NAME = "GeneratedSpectrumZ80";
  private static final String MODEL_SOURCES = "META-INF/model-sources";
  private static final String GENERATOR_SOURCES = "META-INF/generator-sources";
  /** What the generated source says about itself, so that a build can tell whether it is current. */
  private static final String MADE_FROM = "// Written by the build from the model, not by hand. Model: ";
  /** What it is compiled for: the same as everything else in this module. */
  private static final int RELEASE = 21;
  private static Optional<Class<?>> loaded;

  public void configure(Binder binder) {
    OptionalBinder.newOptionalBinder(binder, Core.class).setBinding().toProvider(Tap.class);
    // Also one of the processors a running machine can be moved onto. Whether it can run at all
    // is the wiring's answer, which the tap asks: a wiring that counts T-states through the
    // memory the processor reads sees nothing of a core that reads the tables itself.
    com.google.inject.multibindings.Multibinder.newSetBinder(binder, Core.class).addBinding().toProvider(Tap.class);
  }

  /** One per machine: the generated core over the machine's objects, or the OOP core where it could never run. */
  static class Tap implements Provider<Core> {
    private final MemoryBus memory;
    private final Ula ula;
    private final SpectrumZ80Clock clock;
    private final Display display;
    private final ProcessorWiring wiring;

    @Inject
    Tap(MemoryBus memory, Ula ula, SpectrumZ80Clock clock, Display display, ProcessorWiring wiring) {
      this.memory = memory;
      this.ula = ula;
      this.clock = clock;
      this.display = display;
      this.wiring = wiring;
    }

    public Core get() {
      if (!wiring.takesACoreThatReachesMemoryItself())
        return new OopCore();
      return new GeneratedMachineCore(() -> loaded().orElseThrow(() -> new IllegalStateException("it could not be made here")),
          memory, ula, clock, display);
    }
  }

  /** What the generated core holds of the machine, by the name and type it declares them as, in the order its constructor takes them. */
  record Held(String name, Class<?> type, Object value) {
  }

  static List<Held> held(MemoryBus memory, Ula ula, SpectrumZ80Clock clock, Display display, IO io) {
    List<Held> held = new ArrayList<>(List.of(new Held("ram", memory.getClass(), memory),
        new Held("ula", Ula.class, ula), new Held("clock", SpectrumZ80Clock.class, clock),
        new Held("display", Display.class, display), new Held("io", IO.class, io)));
    // The runs of contended internal cycles, one table per length, held by name so that a run is
    // one lookup in the core; the phase processor asks the ULA for the same ones.
    for (int times = 2; times <= 7; times++)
      held.add(new Held("noMreqRun" + times, byte[].class, ula.contention.run(times)));
    // The decoded bus keeps to itself who covers each page; the core holds those tables by
    // reference, so an access in it is the same index the bus makes, without going through the bus.
    for (String table : List.of("reading", "writing")) {
      java.lang.reflect.Field field = Specializer.findField(memory.getClass(), table);
      held.add(new Held(table, field.getType(), Specializer.get(field, memory)));
    }
    return held;
  }

  /** The machine the generator reads: silent, and with the model's core laid out as the generated one will be. */
  public static Speccy model() {
    // Nothing is chosen for this one. A machine started on the name of the generated core comes
    // back here to make it, and this is the machine being built to make it; a machine started on
    // the name of the OOP core would be read with the wrong bank. Put back afterwards, so the
    // machine that asked for this still starts on what the person chose.
    String chosen = Processors.startsOn;
    Processors.startsOn = null;
    try {
      Speccy speccy = Speccy.create(new SpectrumZ80Clock(), binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class), EmulatorModule.core(ModelCore.class));
      speccy.init();
      return speccy;
    } finally {
      Processors.startsOn = chosen;
    }
  }

  /** The source of the core generated against this machine: its memory and its contention inlined, the machine's objects held. */
  public static String source(Speccy speccy) {
    State state = speccy.cpu.getOoz80().getState();
    CoreGenerator.Target target = new CoreGenerator.Target(PACKAGE, NAME);
    List<Held> held = held(speccy.memory, speccy.ula, speccy.zxClock, speccy.display, state.getIo());
    held.forEach(h -> target.held.put(h.name(), h.type()));
    target.contention = new SpeccyPhaseProcessor(state, speccy.memory, speccy.ula, speccy.zxClock);
    target.helpers.put("read", state.getMemory());
    target.helperSignatures.put("read", "int read(int address, int fetching)");
    target.helpers.put("write", state.getMemory());
    target.helperSignatures.put("write", "void write(int address, int value)");

    CoreGenerator generator = new CoreGenerator(state, modelIndex(), target);
    // The machine's memory is taken apart; the objects it is reached through are held, not frozen.
    // Which memory covers an address is the bus's to know, and is asked: what it answers with is a
    // value. Which kind of memory it is, is a closed set, and is flattened.
    generator.spec.terminals.put(com.fpetrola.z80.memory.Memory.class, "");
    generator.spec.opaque.addAll(List.of("reading", "writing"));
    generator.spec.terminals.put(SpectrumZ80Clock.class, "clock");
    generator.spec.terminals.put(Display.class, "display");
    held.forEach(h -> generator.spec.shared.put(h.value(), h.name()));
    return generator.generate();
  }

  /** The model's sources as packaged with this module, indexed for the specializer. */
  public static SourceIndex modelIndex() {
    return overCodeSource(root -> new SourceIndex(root.resolve(MODEL_SOURCES)));
  }

  /**
   * The generated core's class, once per JVM: the model is the same for every machine.
   * <p>
   * It is an ordinary class of this module, written and compiled by the build. Nothing is made
   * here any more - the first machine to ask used to read four hundred and fifty sources and run
   * javac over a megabyte of generated Java, and every machine after it waited.
   */
  static synchronized Optional<Class<?>> loaded() {
    if (loaded == null) {
      try {
        loaded = Optional.of(Class.forName(PACKAGE + "." + NAME));
      } catch (ClassNotFoundException notThere) {
        System.out.println("oozx: this build carries no generated core, so the machine runs on the OOP core");
        loaded = Optional.empty();
      }
    }
    return loaded;
  }

  /** The whole of the generated file: what it was made from, and the core itself. */
  public static String written() {
    return MADE_FROM + key() + "\n\n" + source(model());
  }

  /**
   * Writes the core's source where the build compiles it from, and compiles it, when the sources
   * that shape it have changed since it was last written. Every other build does nothing: the
   * file says which model it came from and the model is hashed in under a second.
   * <p>
   * Its own javac and not the one that already ran: at this point in the build the compiler has
   * been and gone, and the class beside the stale source would be the stale one.
   */
  public static void main(String[] args) throws IOException {
    Path source = Path.of(args[0]);
    Path classes = Path.of(args[1]);
    String key = key();
    if (key.equals(madeFrom(source)))
      return;
    Files.createDirectories(source.getParent());
    Files.writeString(source, written());
    System.out.println("oozx: the model changed, so the fast core is written again from " + key);
    compile(source, classes);
  }

  /** Which model the core beside us was written from, or nothing when there is no core there. */
  private static String madeFrom(Path source) throws IOException {
    if (!Files.isRegularFile(source))
      return null;
    try (Stream<String> lines = Files.lines(source)) {
      return lines.findFirst().filter(first -> first.startsWith(MADE_FROM)).map(first -> first.substring(MADE_FROM.length())).orElse(null);
    }
  }

  private static void compile(Path source, Path classes) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null)
      throw new IllegalStateException("no Java compiler to build the generated core with");
    // -g because the module's own compiler leaves it on: a stack trace through the core says where.
    if (compiler.run(null, null, null, "-d", classes.toString(), "-cp", classpath(), "-proc:none", "-g",
        "--release", String.valueOf(RELEASE), source.toString()) != 0)
      throw new IllegalStateException("the generated core does not compile: " + source);
  }

  /**
   * What to compile the generated core against: the model's own classes, wherever they are.
   * <p>
   * java.class.path alone is not it. Launched with {@code mvn exec:java} that property is Maven's
   * launcher and nothing else, so every import failed and the machine fell back to the OOP core -
   * silently, until the fallback learned to say so. The classes the model is loaded from are the
   * ones to compile against, so the loaders are asked where they got them.
   */
  static String classpath() {
    StringBuilder path = new StringBuilder(System.getProperty("java.class.path"));
    for (ClassLoader loader = GeneratedCores.class.getClassLoader(); loader != null; loader = loader.getParent()) {
      if (loader instanceof URLClassLoader urls) {
        for (URL url : urls.getURLs()) {
          try {
            path.append(File.pathSeparator).append(Path.of(url.toURI()));
          } catch (URISyntaxException | IllegalArgumentException notAFile) {
            // A class path entry that is not a file is not one javac can be given.
          }
        }
      }
    }
    return path.toString();
  }

  /**
   * SHA-256 of the model's sources and of the generator's: what the written core says it came
   * from. The core itself is left out of it - it is the answer, not the question.
   * <p>
   * Sources on both sides, and not the generator's compiled classes as this used to read. Two JDKs
   * compile one source to different bytes - the class file version alone differs - so the key
   * changed with whichever JDK ran the build, and the committed core came out modified with the
   * same sources on both sides of it. Measured: OpenJDK 21 gave 732e41cff7c18489, GraalVM 25 gave
   * eb67b4ebd80528c5, and OpenJDK 21 again gave the first one back.
   */
  static String key() {
    return overCodeSource(root -> {
      try {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        for (String dir : List.of(MODEL_SOURCES, GENERATOR_SOURCES))
          try (Stream<Path> files = Files.walk(root.resolve(dir))) {
            for (Path file : files.filter(Files::isRegularFile).filter(file -> !file.getFileName().toString().startsWith(NAME))
                .sorted(Comparator.comparing(Path::toString)).toList()) {
              sha.update(root.relativize(file).toString().getBytes(UTF_8));
              sha.update(Files.readAllBytes(file));
            }
          }
        return HexFormat.of().formatHex(sha.digest()).substring(0, 16);
      } catch (IOException | NoSuchAlgorithmException e) {
        throw new IllegalStateException(e);
      }
    });
  }

  /** Where this module's classes come from - a jar when running, a directory under a build - opened as a tree for as long as the reader needs it. */
  private static <T> T overCodeSource(Function<Path, T> reader) {
    try {
      Path code = Path.of(GeneratedCores.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      if (Files.isDirectory(code))
        return reader.apply(code);
      try (FileSystem jar = FileSystems.newFileSystem(code)) {
        return reader.apply(jar.getPath("/"));
      }
    } catch (IOException | URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }
}
