# Building from source

JDK 21 or newer, and Maven. To use the emulator you need neither: [the jar is enough](Your-first-five-minutes).

```bash
git clone https://github.com/fpetrola/oozx.git
cd oozx
mvn -DskipTests install                # the whole tree
java -jar machine/app/target/app-<version>-all.jar
```

A JDK this time, and not only a runtime: the build writes the fast core, which takes about eleven seconds,
and compiles it, which takes two, whenever the model it comes from has changed. It hashes the model in
under a second and skips the work otherwise. → [The fast core](The-fast-core)

```bash
mvn -pl machine/core test              # one module's tests
mvn -pl machine/generated test         # the processor's batteries on the fast core
```

## Four repositories

| | |
|---|---|
| [oozx](https://github.com/fpetrola/oozx) | the Z80, the Spectrum, the fast core and the desk. This is what you just built |
| [oozx-plugins](https://github.com/fpetrola/oozx-plugins) | everything that plugs in: the machines that are not the 48K, the peripherals, the tools, the other file formats, the recordings. Built against the first |
| [plugdesk](https://github.com/fpetrola/plugdesk) | the base the desk stands on: windows, menus made of actions, looks, the plugin window |
| [crystal-plugins](https://github.com/fpetrola/crystal-plugins) | what finds and loads plugins |

The jar that is released is the first two built together and packed into one. The recipe is
`.github/workflows/release.yml`, step by step.

## Who owns what

One concept, one owner, one module, and the modules can only depend one way, so the compiler holds it:

```
emulator  →  machine/core  →  machine/machines, machine/media, machine/host
                           →  machine/ui  →  machine/app
```

`machine/bridge` is a leaf: it knows the machine, and nothing knows it.

| Module | Owns |
|---|---|
| `emulator` | the Z80: instructions, operands, registers, decoding |
| `machine/core` | the Spectrum: the memory bus, the ULA, the display, the ports, the mixing of sound, the keyboard's matrix, configuration. There is no window in it |
| `machine/machines` | the 48K, and the parts every machine is made of |
| `machine/media` | the snapshot the emulator itself reads |
| `machine/host` | the other side of the glass: PC keyboard layouts, joysticks, the audio line |
| `machine/ui` | windows and the picture. **It depends on nothing of the emulator** |
| `machine/devices/kit` | the shapes a peripheral's window takes, and what a plugin may ask of the desk |
| `machine/app` | the desk |
| `machine/generated` | the fast core |
| `machine/bridge` | Fuse's real core behind the same interface, for comparing |

In oozx-plugins, `devices/<name>` is one machine or one peripheral, and `tools/<name>` one tool. The
catalogue, the fingerprints and the pokes are tools.

## The rules, enforced

A check runs with the build. It fails a change that grows a file or a method past its ceiling, and one that
adds something a class can be asked from outside without that having been agreed. What the rules are, and
why, is on [The rules of the code](The-rules-of-the-code).

## These pages

They are in `doc/wiki/`, one file each, and they ride inside the jar as the guide. To read them as the site
shows them:

```bash
python3 -m http.server -d doc/wiki 3000   # http://localhost:3000
```
