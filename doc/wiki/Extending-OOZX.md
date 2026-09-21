# Extending OOZX

The claim on the front page — that a peripheral costs what a module costs — is checkable. Here is the whole
of the smallest one.

## A peripheral in three files

`machine/devices/zxmmc`, 44 lines of Java. The hardware:

```java
/** The ZXMMC: a card slot and nothing else, with the select at 0x1f and the card's byte at 0x3f. */
@Singleton
public class ZxmmcPeripheral extends MmcBoard {
  @Inject
  public ZxmmcPeripheral() {
    super(0x001f, 0x003f);
  }
}
```

Its registration with the machine:

```java
/** The ZXMMC: a card slot on two ports. */
public class ZxmmcDevices extends AbstractModule implements Extension {
  protected void configure() {
    Multibinder.newSetBinder(binder(), Peripheral.class).addBinding().to(ZxmmcPeripheral.class);
  }
}
```

Its window:

```java
public class ZxmmcEquipment implements Equipment {
  public String name() { return "ZXMMC"; }

  public DeviceFrame<?> open() {
    return new IdeBayFrame<>("ZXMMC", ZxmmcPeripheral.class, "card", "mmc", "card");
  }
}
```

And two service files, one line each:

```
META-INF/services/com.fpetrola.oozx.Extension
    com.fpetrola.oozx.speccy.devices.zxmmc.ZxmmcDevices

META-INF/services/com.fpetrola.oozx.speccy.devices.Equipment
    com.fpetrola.oozx.speccy.devices.zxmmc.ZxmmcEquipment
```

That is a complete device: it answers ports, it appears in the `Equipment` menu, it opens a bay where you
insert a card image, and clipping its window onto a machine plugs it in. Nothing in the core was touched, and
nothing in the core names it.

## The steps

1. **A module.** `machine/devices/<name>/pom.xml`, with `machine/devices` as its parent. Add it to
   `machine/devices/pom.xml` and to the dependencies of `machine/devices/all` — that last one is the jar the
   app depends on, and the only reason it exists is to carry every service file onto the classpath.
2. **The hardware.** A `Peripheral`: `activate(machine)`, `deactivate()`, `getPorts()`, `fitsOn(machine)`,
   `isWanted()`, `hasHardReset()`. Add `Pluggable` if the window should plug and unplug it. Reuse what is
   already there — `MmcBoard`, the WD and uPD controllers, the ATA channel in `machine/devices/ide`.
3. **The window.** Usually none of your own: `DriveBayFrame` gives you bays with motor lights and
   Insert/Eject/New/Save/Flip/Write-protect, `IdeBayFrame` a slot per disk, `DacFrame` a level meter and a
   volume knob.
4. **Settings, if any.** A class with `@Section("yourdevice")` — public fields, defaults in the field
   initialisers. The settings window builds its tab from what the device declares; nobody writes a dialog.
5. **The two service files.**

What you may use: the ULA, the mixer, the clock. What you may not: anything that would make the core know
your device exists. Work reaches the machine through `z80.later(...)`, never from the Swing thread.

## Adding a machine

A model is four declarations — how it pages, which chip draws it, its `MachineTimings`, its ROMs — plus a
binding in `Machines`. The Pentagon was added *to find out what that costs*; the seven clones went in over
one night afterwards. If its ROM is not ours to ship, declare where it is published and its SHA-256 in the
configuration, and the emulator will offer to fetch it.

Every machine is then picked up by the tests that ask every machine the same questions, so a new model is
covered the moment it is bound.

## Adding a scaler or a screen knob

A `Scaler` is one method (`scale(picture, width, height, context)`) and a label. A knob is one line in
`ScreenSettings.settings()` — `Knob.choice`, `Knob.number` or `Knob.switching`, with its name, its
description and its range. Both appear in the interface by themselves, because the interface is built from
the descriptors rather than written out.

## The rules the code is held to

- **One concept, one owner.** Anything that depends on a tape format lives in `Tape` and nowhere else; the
  same for every other concept. No `switch` on a format, a model or a device outside the class that owns it.
- **New code goes where its concept lives**, not in the package of whoever calls it.
- **If something existing cannot be used because it is broken, fix it or ask** — never write a second
  implementation beside it.
- **The code should shrink as it improves.** A change that adds a feature and removes lines is the normal
  case, not a surprise.

## Where this lives

`machine/devices/*` for the examples, `machine/devices/kit` for the window shapes, `machine/machines` for the
models, `doc/plan-modulos.md` for why the boundaries are where they are.
