# Extending OOZX

Almost nothing in this emulator is in the emulator. A bare build is a 48K with its ROM and the
snapshot format it writes its own state in; every machine, every board, every window, every screen
effect and every other file format arrives in a jar and adds itself. This page is how you write one.

## How it works

An interface marked `@Plugin` is a way in. Every implementation of one is something that was plugged
in, and that is the whole mechanism:

```java
@Plugin("effect")
public interface ScreenEffect { ... }
```

- **You write a class that implements it.** Nothing else: no registration, no annotation on your class.
- **The compiler writes the paperwork.** An annotation processor walks what you compiled, finds the ways
  in you answer to, and writes the `META-INF/services` entries into your jar. Those files used to be
  written by hand, and a misspelt name was a board that simply never appeared.
- **You drop the jar in `~/.oozx/plugins`.** One folder, one class loader over all of it, which is what
  lets a jar implement an interface another jar brought.
- **The emulator asks.** `Plugins.found(ScreenEffect.class)` returns what answers, from the folder and
  from the classpath alike, and refuses to ask for anything that is not marked `@Plugin`.

There is no classpath scanning at startup: discovery reads one small text file per jar per way in.

## The ways in

| Marked | Interface | What it lets you add |
|---|---|---|
| `device` | `com.fpetrola.oozx.Extension` | Anything that binds itself into a machine: a peripheral, a machine model, a part. A Guice module the emulator installs. |
| `device` | `com.fpetrola.oozx.speccy.devices.Equipment` | A window for a machine, in the `Equipment` menu. Clipping it onto a machine is what plugs it in. |
| `tool` | `com.fpetrola.oozx.speccy.devices.DeskEquipment` | A window of the desk rather than of a machine: a browser, a library. It fills the `Emulator` menu by itself. |
| `effect` | `com.fpetrola.oozx.speccy.screen.ScreenEffect` | A step on the picture on its way to the window, with the knobs that configure it. |
| `scaler` | `com.fpetrola.oozx.speccy.screen.Scaler` | A whole way of making 256×192 into a windowful, offered in the screen settings. |
| `format` | `com.fpetrola.emulation.helpers.snapshots.SnapshotFile` | A snapshot format: which files it reads, how to load and save them. |
| `media` | `com.fpetrola.oozx.speccy.machine.StartsAMachineOn` | A kind of file a machine can be started on: which machine it wants, what to do with it, what goes on happening afterwards. |
| `roms` | `com.fpetrola.oozx.config.RomsOfItsOwn` | What your machines and boards are made with, where the images that cannot be given away are published, and their SHA-256. |
| `extra` | `com.fpetrola.oozx.plugins.BesideTheGame` | Something that travels beside a game file - Spec256's colours are this. |

Each of them is below, with the interface as it is in the source and something that answers to it.

Two more interfaces are not ways in but are what your window may implement: `Opens` (it can be handed a
file) and `KeepsItsPlace` (it comes back where it was left). And `Desk` is what a window may ask of
whoever put it on the screen: choose a file, open a machine for it, keep a game, play a recording.

## The smallest plugin there is

One class, compiled against the emulator's jar, in a jar of its own. This is a screen effect that does
nothing but say it was there:

```java
package mine;

import com.fpetrola.oozx.speccy.screen.*;
import java.awt.image.BufferedImage;

public class Mine implements ScreenEffect {
  public String label() {
    return "Mine";
  }

  /** Before the scaler, so it works on the picture the machine drew rather than on the window. */
  public When when() {
    return When.ON_THE_PICTURE;
  }

  public BufferedImage apply(BufferedImage picture, ScreenContext context) {
    return picture;
  }
}
```

Build it and put it in:

```
javac -proc:full -cp oozx.jar:plugin-processor.jar -d classes Mine.java
(cd classes && jar cf ../mine.jar .)
cp mine.jar ~/.oozx/plugins/
```

Start the emulator and it is in the screen settings, on the `Television` side. Nothing was registered
anywhere, and nothing in the emulator names it.

Two things about that command. `plugin-processor.jar` is eight kilobytes and is on the releases page
beside the plugins; it is what writes this into your jar while it compiles:

```
META-INF/services/com.fpetrola.oozx.speccy.screen.ScreenEffect
    mine.Mine
```

And `-proc:full` is not optional, because from JDK 23 javac ignores the processors it finds on the class
path unless it is told to run them.

If you would rather not have the processor, write that file yourself - it is one line, and the emulator
cannot tell the difference:

```
javac -cp oozx.jar -d classes Mine.java
mkdir -p classes/META-INF/services
echo mine.Mine > classes/META-INF/services/com.fpetrola.oozx.speccy.screen.ScreenEffect
```

## Every way in, with what it looks like

Every example here was compiled against the jar the emulator ships as, put in a folder of its own, and
looked for: what came back is at the end. They are written to be read, not to be useful - each does the
least that is not nothing.

### Adding a board, a part, or a machine

`Extension`, marked `@Plugin("device")`. A Guice module the emulator installs into every machine it builds. It is how anything reaches the inside of a machine: a board on the bus, a machine model, a part of one.

```java
public interface Extension extends Module {
}
```

```java
package mine;

import com.fpetrola.oozx.Extension;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.ports.Wired;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

import java.util.List;

/** A board on the bus: everything written to port 0xfb, kept. */
public class ADevice extends AbstractModule implements Extension {

  protected void configure() {
    Multibinder.newSetBinder(binder(), Peripheral.class).addBinding().to(TheBoard.class);
  }

  @Singleton
  public static class TheBoard extends AbstractPeripheral {
    /** The last byte anybody sent it, which is all this one does. */
    public int last;

    public TheBoard() {
      super(List.of(Wired.at(0x00ff, 0x00fb, new DefaultPortHandler(false, true) {
        public void write(int port, int value) {
        }
      })));
    }
  }
}
```

```java
package mine;

import com.fpetrola.oozx.Extension;
import com.fpetrola.oozx.speccy.machine.*;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.*;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.timer.Timer;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.PeripheralRegistry;
import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;

/** A machine: a 48K under another name, which is what most clones are. */
@Singleton
public class AMachine extends Spec48 {

  @Inject
  public AMachine(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals,
                  Machine.Unit unit, Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, peripherals, unit, roms, scheduler, cpu, timer, sound);
  }

  public String shortName() {
    return "Mine";
  }

  public String getName() {
    return "Mine 48K";
  }

  /** What puts it among the machines there are. */
  public static class Devices extends AbstractModule implements Extension {
    protected void configure() {
      Multibinder.newSetBinder(binder(), Spectrum.class).addBinding().to(AMachine.class);
    }
  }
}
```

### A window for a machine

`Equipment`, marked `@Plugin("device")`. What the `Equipment` menu lists. Clipping the window onto a machine is what plugs it in, and a window clipped to nothing is a window that does nothing.

```java
public interface Equipment {
  String name();
  MachineFrame open();
  default boolean opens(java.io.File file) {
    return false;
  }
}
```

```java
package mine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.Equipment;
import com.fpetrola.oozx.speccy.devices.MachineFrame;
import javax.swing.JLabel;

/** A window for a machine: clipping it onto one is what plugs it in. */
public class AWindow implements Equipment {
  public String name() {
    return "Mine";
  }

  public MachineFrame open() {
    return new TheWindow();
  }

  static class TheWindow extends MachineFrame {
    private final JLabel saying = new JLabel("no machine");

    TheWindow() {
      super("Mine");
      setSize(260, 120);
      assemble(saying);
    }

    /** The two things every clipped window has to say, for the buttons in its corner. */
    @Override
    protected String expandTip() {
      return "Show it all, or just the controls";
    }

    @Override
    protected String attachTip() {
      return "Keep this under the machine's window";
    }

    /** Told when it is clipped onto a machine, or carried away from one. */
    @Override
    protected void machineChanged(Speccy was, Speccy now) {
      saying.setText(now == null ? "no machine" : now.machine.current.getName());
    }
  }
}
```

### A window of the desk

`DeskEquipment`, marked `@Plugin("tool")`. Something there is one of, which is not about any machine: a browser, a library. It appears in the `Emulator` menu by itself.

```java
public interface DeskEquipment {

  String name();

  JInternalFrame open();
  default String keeps() {
    return name();
  }
}
```

```java
package mine;

import com.fpetrola.oozx.speccy.devices.DeskEquipment;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;

/** A window of the desk, in the Emulator menu. */
public class ADeskWindow implements DeskEquipment {
  public String name() {
    return "Mine";
  }

  public JInternalFrame open() {
    JInternalFrame window = new JInternalFrame("Mine", true, true, true, true);
    window.add(new JLabel("Hello from a jar"));
    window.setSize(300, 120);
    return window;
  }
}
```

### A step on the picture

`ScreenEffect`, marked `@Plugin("effect")`. Something done to the picture on its way to the window. It says which side of the scaler it belongs on, and brings the knobs that configure it: an effect that arrives is built with nothing, so its knobs are how it is configured at all.

```java
public interface ScreenEffect {
  enum When {
    ON_THE_PICTURE, ON_THE_SCREEN
  }
  default When when() {
    return When.ON_THE_SCREEN;
  }
  String label();
  BufferedImage apply(BufferedImage picture, ScreenContext context);
  default java.util.List<Knob> knobs() {
    return java.util.List.of();
  }
  default boolean isTransparent() {
    return false;
  }
}
```

```java
package mine;

import com.fpetrola.oozx.speccy.screen.*;
import java.awt.image.BufferedImage;

/** A step on the picture on its way to the window. */
public class AnEffect implements ScreenEffect {
  public String label() {
    return "Mine";
  }

  public When when() {
    return When.ON_THE_PICTURE;
  }

  public BufferedImage apply(BufferedImage picture, ScreenContext context) {
    return picture;
  }
}
```

### A way of making a windowful

`Scaler`, marked `@Plugin("scaler")`. A scaler is an effect that is picked rather than stacked: it turns 256 by 192 into whatever the window is, and is offered in the screen settings beside the ones written here.

```java
public interface Scaler extends ScreenEffect {
  BufferedImage scale(BufferedImage picture, int width, int height, ScreenContext context);

  @Override
  default BufferedImage apply(BufferedImage picture, ScreenContext context) {
    return scale(picture, context.targetWidth(), context.targetHeight(), context);
  }
  static void repeat(int[] source, int sourceWidth, int sourceHeight,
                     int[] target, int targetWidth, int targetHeight) {
    for (int y = 0; y < targetHeight; y++) {
      int from = (int) ((long) y * sourceHeight / targetHeight) * sourceWidth;
      int to = y * targetWidth;
      for (int x = 0; x < targetWidth; x++) {
        target[to + x] = source[from + (int) ((long) x * sourceWidth / targetWidth)];
      }
    }
  }
  static int at(int[] pixels, int width, int height, int x, int y) {
    int cx = x < 0 ? 0 : x >= width ? width - 1 : x;
    int cy = y < 0 ? 0 : y >= height ? height - 1 : y;
    return pixels[cy * width + cx];
  }
}
```

```java
package mine;

import com.fpetrola.oozx.speccy.screen.*;
import java.awt.image.BufferedImage;

/** A whole way of making 256 by 192 into a windowful. */
public class AScaler implements Scaler {
  public String label() {
    return "Mine, doubled";
  }

  public BufferedImage scale(BufferedImage picture, int width, int height, ScreenContext context) {
    return Scalers.byName("Nearest neighbour").scale(picture, width, height, context);
  }
}
```

### A snapshot format

`SnapshotFile`, marked `@Plugin("format")`. Which files are its own, and how to read and write them. The emulator carries one, the `.z80` it writes its own state in, and reads whatever else turns up.

```java
public interface SnapshotFile {
    boolean reads(File file);

    SpectrumState load(File filename) throws SnapshotException;

    boolean save(File filename, SpectrumState state) throws SnapshotException;
    default SnapshotFile fresh() {
        try {
            return getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException cannotBeMadeAgain) {
            return this;
        }
    }
    default String label() {
        return getClass().getSimpleName();
    }
    static boolean named(File file, String... extensions) {
        String name = file.getName().toLowerCase();
        for (String extension : extensions) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
```

```java
package mine;

import com.fpetrola.emulation.helpers.snapshots.*;
import java.io.File;

/** A snapshot format: which files are its own, and how to read and write them. */
public class AFormat implements SnapshotFile {
  public boolean reads(File file) {
    return SnapshotFile.named(file, ".mine");
  }

  public String label() {
    return "Mine";
  }

  public SpectrumState load(File file) throws SnapshotException {
    throw new SnapshotException("not written yet");
  }

  public boolean save(File file, SpectrumState state) {
    return false;
  }
}
```

### A kind of file a machine starts on

`StartsAMachineOn`, marked `@Plugin("media")`. What a file does to a machine being built for it: which machine it wants, what to do with it once it is up, and what goes on happening afterwards. Typing LOAD "" is that.

```java
public interface StartsAMachineOn {
  boolean handles(File file);
  default String machineFor(File file) {
    return null;
  }
  Going start(Speccy machine, File file);
  interface Going {
    boolean done();

    void step();
    default String wrong() {
      return null;
    }
  }
}
```

```java
package mine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.StartsAMachineOn;
import java.io.File;

/** A kind of file a machine can be started on, and what goes on happening afterwards. */
public class StartsOnOne implements StartsAMachineOn {
  public boolean handles(File file) {
    return file.getName().toLowerCase().endsWith(".mine");
  }

  /** What this file was made for; null leaves the machine as it is. */
  public String machineFor(File file) {
    return "Spectrum 48K";
  }

  public Going start(Speccy machine, File file) {
    machine.memory.poke(23692, (byte) 255);       // no "scroll?" while what follows prints
    return null;                            // nothing to keep doing afterwards
  }
}
```

### What your machines are made with

`RomsOfItsOwn`, marked `@Plugin("roms")`. A jar that brings a machine brings what the machine is made of, or it is not a machine anybody can start. Images that may be given away go in your jar's own `/roms`; the rest are named here, with where they are published and what they have to turn out to be.

```java
public interface RomsOfItsOwn {
  Map<String, List<String>> files();
  default Map<String, Map<String, List<String>>> sets() {
    return Map.of();
  }
  default Map<String, RomFiles.Source> sources() {
    return Map.of();
  }
}
```

```java
package mine;

import com.fpetrola.oozx.config.RomFiles;
import com.fpetrola.oozx.config.RomsOfItsOwn;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** What this jar's machines and boards are made with, and where what it cannot carry lives. */
public class ItsRoms implements RomsOfItsOwn {

  /** By the name the machine or the board is known by, which is its class's. */
  public Map<String, List<String>> files() {
    return Map.of("AMachine", List.of("mine.rom"));
  }

  /** For an image that cannot be given away: where it is, and what it has to turn out to be. */
  public Map<String, RomFiles.Source> sources() {
    Map<String, RomFiles.Source> published = new LinkedHashMap<>();
    RomFiles.Source mine = new RomFiles.Source();
    mine.url = "https://example.invalid/mine.rom";
    mine.sha256 = "0".repeat(64);
    mine.at = 0;
    mine.length = 16384;
    published.put("mine.rom", mine);
    return published;
  }
}
```

### Something beside a game

`BesideTheGame`, marked `@Plugin("extra")`. A file that travels next to a game and is worth mentioning where the game is shown. Spec256's colours are this.

```java
public interface BesideTheGame {
  String what();
  boolean isBeside(String game);
  static String whatIsBeside(String game) {
    for (BesideTheGame kind : Plugins.found(BesideTheGame.class)) {
      if (kind.isBeside(game)) return kind.what();
    }
    return null;
  }

  static boolean anythingBeside(String game) {
    return whatIsBeside(game) != null;
  }
}
```

```java
package mine;

import com.fpetrola.oozx.plugins.BesideTheGame;
import java.io.File;

/** Something that travels beside a game file, for the browser to mention. */
public class AnExtra implements BesideTheGame {
  public String what() {
    return "a map";
  }

  public boolean isBeside(String game) {
    return new File(game.replaceAll("\\.[^.]+$", "") + ".map").isFile();
  }
}
```
## All nine in one jar, and what the emulator made of it

The nine examples above are one jar: nine classes, no registration, nothing in the emulator naming any
of them. Compiled and dropped in `~/.oozx/plugins`, this is what it answered:

```
  device  Extension        -> ADevice
  device  Equipment        -> AWindow
  tool    DeskEquipment    -> ADeskWindow
  effect  ScreenEffect     -> AnEffect, AScaler
  scaler  Scaler           -> AScaler
  format  SnapshotFile     -> AFormat
  media   StartsAMachineOn -> StartsOnOne
  roms    RomsOfItsOwn     -> ItsRoms
  extra   BesideTheGame    -> AnExtra
```

A scaler answers to both `Scaler` and `ScreenEffect`, because it is one - the processor writes it into
both files, and the pipeline runs it as the scaler rather than as a step.

The machine in it is offered beside the 48K, and choosing it gets as far as asking for `mine.rom`, which
is the one thing in these examples that was made up: a machine that arrives asks for what its own jar
said it is made of.

## When your plugin needs another

Say so in your pom. The build writes it into your jar's manifest `Class-Path`, and bringing yours brings
what it needs:

```
Class-Path: device-spectrum128-0.0.2-alu-SNAPSHOT.jar ...
```

That is how `device-amstrad` brings the 128 it is built on, and how `+D` brings the parallel printer.

## Seeing what is in

The `Plugins...` window has three tabs: what is published, what each jar brought, and what there is of
each kind. The last two read the `META-INF/services` of everything installed without loading any of it,
so they can list seventy-odd devices without building seventy machines.

## What cannot be taken out

- **One machine.** Without a model there is no `@DefaultMachine` to bind and the emulator cannot build a
  machine at all. That one is the 48K.
- **The `.z80` reader.** It is what the emulator saves its own state in - the snapshot history and the
  windows that come back are written with it.

## The rules the code is held to

- **One concept, one owner.** No `switch` on a format, a model or a device outside the class that owns it.
- **New code goes where its concept lives**, not in the package of whoever calls it.
- **If something existing cannot be used because it is broken, fix it or ask** - never write a second
  implementation beside it.
- **The code should shrink as it improves.**

## Where this lives

`plugin-api` for the annotation, `plugin-processor` for what writes the paperwork and
`machine/core/src/main/java/com/fpetrola/oozx/plugins` for the loader and the discovery, all in
this repository.

The plugins themselves are not: they are [fpetrola/oozx-plugins](https://github.com/fpetrola/oozx-plugins),
which is built against this one and holds sixty-odd worked examples under `devices/*` and
`tools/*`. Yours does not belong there either - a plugin is a jar of your own, and the emulator
finds it the same way it finds those.
