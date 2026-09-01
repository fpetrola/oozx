# Partir machine/core en módulos

Plan para que un periférico esté encapsulado de verdad: que el compilador impida que el emulador
lo conozca, en vez de que sea una convención que se respeta hasta que alguien no la respeta.

Medido sobre `machine/core` el 1 de septiembre de 2026, con el gate en 320 tests verdes.

## El principio

Un periférico real conocía el hardware: quien diseñó la ZX Printer tenía los esquemáticos de la
Spectrum, sabía del bus y del timing. Lo que no podía hacer era que la Spectrum supiera de la
impresora.

**La pregunta no es cuánto conoce el periférico, sino quién conoce a quién.** Un periférico que
usa la ULA, el mixer y el reloj está bien. El emulador que nombra a la impresora está mal. La
dirección de dependencia entre módulos hace cumplir exactamente eso, con el compilador y sin
tests.

De ahí sale el resto del plan: **no** hay que angostar lo que un periférico ve (esa fue una
propuesta anterior, descartada), hay que impedir que el emulador vea al periférico.

## Lo que hay hoy

`machine/core` tiene 263 archivos java, de los cuales 51 usan Swing. Es un solo jar con el
emulador, los dispositivos, el escritorio, el launcher y la pantalla adentro.

| dónde | archivos | qué es |
|---|---|---|
| `speccy/peripherals/t` | 32 | el escritorio: ventanas, menús, browser de juegos, docking |
| `speccy/screen` + `SpeccyScreen`, `TvScreen`, `SpectrumPanel`, `ZXScreenComponent` | 13 | la pantalla dibujada |
| `speccy/devices/*` | 33 | los once dispositivos y sus módulos de Guice |
| `speccy/machine`, `modules`, `peripherals`, `ports`, `sound`, `Memory`, `Z80` | ~150 | la emulación |
| `api/`, `speccy/bridge`, `rzx`, `config`, `pokes` | ~35 | servicios y formatos |

Lo bueno: **la emulación casi no conoce al escritorio.** Sólo hay cuatro aristas hacia adentro de
`peripherals/t`, y tres son del launcher, que es parte de la aplicación.

## Los tres nudos — desatados

Hay que desatarlos antes de mover nada, porque son ciclos que ningún orden de módulos resuelve.
**Los tres están hechos** (1 de septiembre de 2026, gate en 338 verdes), y dos resultaron ser otra
cosa de la que decía este plan cuando se escribió:

1. **`Z80.createScreen(...)`** no abría ningún `JFrame`: devolvía `null` siempre. Lo que hacía en
   190 líneas era **construir el adaptador con el que hablan todas las ventanas** — pausa,
   velocidad, el modelo, guardar, el listener de teclado, el componente de pantalla. Por eso un Z80
   importaba `javax.swing`. Ahora es `SpeccyEmulatorCore`, una clase del lado de las ventanas,
   construida a partir de un `Speccy` y no desde adentro de una de sus partes. Tres lugares
   construyen una máquina para ser mirada y cada uno lo dice en una línea: el launcher, la sesión
   de RZX, y el test que graba una máquina a un archivo como lo haría una ventana.

2. **`Tape.TapeTableModel`** estaba muerto: nadie fuera de la cinta lo pedía. Sólo existía para que
   le llamaran `fireTableDataChanged()` tres veces, para una tabla que nadie mostraba — la ventana
   de casete arma su propio modelo con `TapeBlock`. Borrado, −47 líneas.

3. **`api/ZxInfoApiHandler` → `GameBrowserInternalFrame`**: el parseo del JSON vivía como `static`
   en la ventana. Ahora es `Screen.from(...)`, al lado de la cosa que se parsea.

### Lo que queda de Swing fuera del escritorio

Después de los tres nudos, **nada bajo `speccy/modules`, `speccy/machine` ni la memoria importa un
toolkit de UI** — la base compila sin Swing, que es lo que el paso 1 tenía que averiguar. Lo que
queda con Swing fuera de `peripherals/t` está todo del lado de la aplicación:

| archivo | a dónde va |
|---|---|
| `OOSpectrumConnector` | app — es un punto de entrada que abre una ventana |
| `EmulatorCore`, `MockEmulatorCore`, `SpeccyEmulatorCore`, `SettingsDialog` | ui / app |
| `devices/printer/PrinterPaper` | devices — la vista de un periférico, que depende de ui |

### El cuarto nudo, que apareció al desatar el primero — desatado

`EmulatorCore` es **dos interfaces en una**: control del emulador (pausa, velocidad, `saveState`,
el modelo) y vista (`getPanel()` devuelve un `JComponent`, `getKeyListener()` un `KeyListener`). El
Z80 tiene un campo `public EmulatorCore mockCore`, así que si `EmulatorCore` se va a la aplicación,
la base importa la aplicación.

**Partida**: `EmulatorControl` es la mitad que se puede decir sin pantalla, y `EmulatorCore` es lo
que una ventana necesita además — la imagen y a dónde mandar las teclas. La base lleva la mitad de
control (`Cpu`, el campo del Z80, `UserInterface.statusbarUpdateSpeed`) y **el escritorio no cambió
en nada**: conserva el nombre y los métodos que tenía.

De paso, los dos lugares que construyen una máquina para ser mirada se guardan su propia
referencia en vez de leerla de vuelta del procesador — el launcher, que ya la tenía en la mano, y
la sesión de RZX, que construía una y no tenía dónde ponerla (el escritorio la buscaba a través de
`session.getSpeccy().z80.mockCore`).

**Con esto la base no tiene camino a un toolkit de UI, ni por import ni por tipo**, que es la
condición que el paso 3 necesitaba.

## El destino

```
machine/base       emulación, contratos de dispositivo, roles de máquina, el bus,
                   y el hardware propio de cada máquina (pagers, FDC, ULA, el AY de una 128).
                   Sin Swing: corre headless, que es lo que quieren los tests y el core libretro.

machine/ui         infraestructura de ventanas: AttachedFrame, los helpers de iconos,
                   los componentes de pantalla.  → depende de base

machine/devices    los periféricos opcionales: impresora, mouse, Melodik, Kempston…
                   cada uno con su modelo y su vista.  → depende de base y de ui

machine/app        el escritorio, los menús, el launcher, main().
                   → depende de base + ui + devices, y los descubre por ServiceLoader
```

### Por qué existe `machine/ui`

Es la pieza que permite que un periférico traiga su propia ventana. Si `AttachedFrame` se queda en
la aplicación, la ventana de la impresora tiene que vivir ahí, y entonces el periférico no es
autocontenido; si el periférico la trae, depende de la aplicación, que depende del periférico:
ciclo. La salida es que la infraestructura de ventanas acoplables esté **debajo** de los dos.

`AttachedFrame` no es la aplicación: es un mecanismo — engancharse a una máquina, plegarse, y que
el enganche sea la conexión eléctrica. Eso es tan del emulador como el bus de puertos.

### Qué es un periférico y qué no

**Se queda en base** el hardware propio de una máquina: el pager de la 128 y el del +3, la FDC del
+3, la memoria SE, los dos ULA, el AY que una 128 trae de fábrica. Eso no se enchufa, **es la
computadora**.

**Va a devices** lo que se enchufa: impresora, mouse, Melodik, los dos Kempston, y todo lo que
venga (Interface 1, Beta 128, +D, SpecDrum, teclado de Currah…).

Hoy eso son cuatro o cinco periféricos, no once. Ahí muere la preocupación por la verbosidad.

## Los pasos

Cada uno deja el gate verde y se commitea solo.

**1. Desatar los nudos. HECHO — los tres del plan y el cuarto que apareció debajo.** Ninguna clase
de emulación importa Swing ni el escritorio, y ningún tipo que lleve la base arrastra uno. Se
verifica con un grep, sin escribir un pom.

**2. `machine/ui`. HECHO.** 19 archivos: `AttachedFrame`, los cargadores de iconos, y todo lo que
dibuja la pantalla (`speccy/screen/*`, `TvScreen`, `SpeccyScreen`, `SpectrumPanel`). No depende de
nada del emulador, que es lo que sostiene el orden de capas.

Dos cosas tuvieron que cambiar para que pudiera decir eso:

- **El frame pedía sus botones a la aplicación** — `EmulatorInternalFrame.iconToggle` y `.tighten`
  eran estáticos de una ventana de la capa de arriba. Ahora son `Widgets`, en ui, y las versiones
  de la aplicación reenvían, así que ningún call site cambió.
- **Buscaba la clase de ventana de la aplicación** para saber cuáles son máquinas. Ahora una
  ventana de máquina lo dice implementando `MachineWindow` — una pregunta que la capa de abajo sí
  puede hacer.

`ScrollPane` se volvió a core en el camino: necesita una galería, así que no era infraestructura.

**3. `machine/app`.** Mover `peripherals/t` (menos lo que se fue a ui), `OOSpectrumLauncher`,
`SwingUserInterface`, la UI de pokes y de config. `machine/base` queda siendo lo que sobra, y hay
que verificar que compila **sin** el classpath de Swing.

**4. `machine/devices`.** Mover los periféricos opcionales con sus vistas y sus service files. Cada
módulo trae su propio `META-INF/services/…DeviceModule`: el `ServiceLoader` lee todos los que
encuentra en el classpath, así que la lista deja de ser un archivo compartido y pasa a ser una
línea que cada módulo trae consigo. Eso es la historia de plugins funcionando de verdad.

**5. La prueba.** Un test en `app` que arranque el emulador y verifique que la impresora está —
sin importar ninguna clase de la impresora, sólo por el bus. Ya existe en espíritu
(`DiscoveredDeviceTest`); ahí pasa a ser real, porque el dispositivo estará en otro jar.

**6. (Opcional, después.)** Promover la impresora a `machine/device-printer`. Es mover una carpeta
y escribir un pom con **una** dependencia. Vale la pena el día que alguien quiera publicar un
periférico sin compilar el emulador; antes de eso, no paga.

## Decisiones abiertas

- **Dónde viven los tests de integración.** `PrinterOnItsMachineTest` y `RomPrintsToThePrinterTest`
  construyen un `Speccy` entero, así que no pueden vivir en el módulo del periférico sin que ese
  módulo dependa de la aplicación. O se van a `app`, o el periférico se prueba sólo por su modelo
  —la correa y el papel, que ya están aislados— y la integración vive arriba. Hay que decidirlo
  antes del paso 4, no cuando el build se queje.

- **El nombre de `machine/base`.** Hoy `machine/core` es todo; si `core` pasa a ser la base, los
  tres módulos nuevos son los que se mueven. Si en cambio `core` queda como la aplicación, se mueve
  la emulación. Lo primero toca menos.

- **`machine/spectrum`** ya existe como módulo hermano. Ver si `base` va ahí o al lado.

## Riesgos

- **El IDE y el build.** Cuatro módulos donde había uno: reload, y unos minutos de compilación
  cruzada la primera vez. No es riesgo técnico, es fricción.

- **Guice.** No cambia nada: los módulos de dispositivo ya se instalan por `ServiceLoader`, que es
  indiferente a en qué jar está cada uno.

- **Mover archivos rompe imports en masa.** Es mecánico, pero conviene un módulo por commit y el
  gate entre medio.

- **La sesión paralela trabaja en `peripherals/t`.** El paso 3 mueve ese paquete entero: hay que
  coordinarlo, o hacerlo cuando esa zona esté quieta.

## Lo que ya está hecho y hace esto posible

- Los dispositivos se descubren por classpath (`DeviceModule` + `ServiceLoader`), no por una lista.
- Cada dispositivo declara en qué máquina encaja (`fitsOn`) y si lo quieren (`isWanted`).
- Un dispositivo pide roles a la máquina (`Paging128`, `PagingPlus3`, `FloppyDrive`), nunca clases.
- Ningún código fuera de `speccy/devices` nombra un dispositivo, salvo las dos ventanas — y ésas
  son justo las que el paso 4 se lleva con su periférico.
