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

## A plugin that brings more than one thing

A jar can answer to several ways in at once, and the paperwork for each is written separately. The
emulator's own `device-debugger` brings two windows; `tool-calls` brings two; a jar with a machine in it
usually brings three things: the machine, its `Extension`, and what it is made of:

```java
public class MineDevices extends AbstractModule implements Extension {
  protected void configure() {
    Multibinder.newSetBinder(binder(), Spectrum.class).addBinding().to(MyMachine.class);
  }
}

public class MineRoms implements RomsOfItsOwn {
  public Map<String, List<String>> files() {
    return Map.of("MyMachine", List.of("mine-0.rom", "mine-1.rom"));
  }
}
```

If the images may be given away, put them in your jar's own `/roms` - the emulator reads them through the
same loader as its own. If they may not, declare where they are published and their SHA-256 in
`sources()`, and the emulator will offer to fetch them once, with a yes.

A machine that arrives this way runs on the generated core like any other: the fast core is specialised
over the model's structures, not over a list of machines.

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

`plugin-api` for the annotation, `plugin-processor` for what writes the paperwork,
`machine/core/src/main/java/com/fpetrola/oozx/plugins` for the loader and the discovery,
`machine/devices/*` and `machine/tools/*` for sixty-odd worked examples.
