# Llevar a plugins todo lo que no es la Spectrum

Plan para que la base quede como el chasis que arranca una Spectrum y la muestra en una ventana
sin nada alrededor, y que todo lo demás —las máquinas, los chips de cada máquina, las
herramientas, los medios, la ropa de las ventanas— sea un plugin, con las dependencias entre
plugins dichas por el build y cumplidas por el framework.

Medido el 24 de septiembre de 2026, con las dos suites verdes (138 clases de test en la base, 147
en oozx-plugins) y el framework en `swing-11` / `processor-3`.

**Estado: plan.** Ya hecho antes de escribirlo: los 52 plugins en su repositorio, el catálogo de
juegos como plugin (`tool-catalogue`, con `tool-games` dependiendo de él), el escritorio sin
saber de juegos (rol `WhatGameThisIs`), y el botón de plugins en la barra.

## Lo que queda al final

Una persona que baja el emulador ve lo mismo que hoy: el jar de un solo archivo trae los plugins
adentro y el primer arranque los pone. Lo que cambia es de qué está hecho:

- **La base** arranca una 48K y la dibuja en una ventana con la pantalla y nada más: sin barra,
  sin barra de estado, sin menú contextual. Se puede tipear en ella.
- **Cada cosa que hoy rodea a la pantalla** —pausa, turbo, volumen, zoom, borde, pokes, guardar
  el estado, detalles del juego— la pone un plugin, y sacar el plugin la saca de la ventana.
- **Cada máquina que no es la 48K** es un plugin, y **cada chip que esa máquina necesita** también,
  con la dependencia explícita: sacar el AY se lleva a todas las 128K, y el panel lo dice.

## El principio

El mismo de `plan-modulos.md`: **no importa cuánto conoce el plugin, importa quién conoce a
quién.** Un plugin de AY que usa el bus, el mixer y el reloj está bien. La base que nombra al AY
está mal. Con plugins, además, la dirección de la dependencia la hace cumplir el framework en
tiempo de ejecución, no sólo el compilador:

| lo que hace el framework hoy | qué significa para este plan |
|---|---|
| deduce `Plugin-Dependencies` del bytecode | una máquina que nombra `AyPeripheral` en `onBoard()` depende del plugin del AY sin que nadie lo declare |
| sacar un plugin se lleva a los que dependen de él, en orden | sacar el AY saca las 128K; nunca queda una máquina sin su chip |
| instalar uno trae sus dependencias, a la versión compilada | pedir la +3 trae el AY, la memoria +3 y la disquetera |
| lo que la aplicación declara en `META-INF/services` entra como un plugin más, con id `application` | lo que se quede en la base se ve en el panel y un plugin lo puede tapar con `@Replaces` |
| las bibliotecas de terceros de un plugin viajan en su `lib/` (`libs-1`) | un plugin se lleva lo suyo y no depende de lo que la aplicación trae por casualidad |
| `@Needs` para lo que el bytecode no muestra | una dependencia por reflexión, o hacia un plugin biblioteca, se declara a mano |

## Lo que hay hoy

Los 21 módulos de la base, con lo que tienen adentro y quién los usa. "Plugins" cuenta los poms
de oozx-plugins que dependen del módulo, que es la API publicada que hay que cuidar.

| módulo | clases | qué es | lo usan en la base | plugins | veredicto |
|---|---:|---|---|---:|---|
| `emulator` | 229 | el Z80 y sus instrucciones | casi todos | 1 | **queda** |
| `machine/core` | 99 | el chasis: bus, pantalla, puertos, mezclador, reloj, contención | todos | 18 | **queda** |
| `machine/spectrum` | 19 | el estado de una máquina y los formatos de snapshot | core | 0 | **queda** (ver etapa 5) |
| `machine/ui` | 20 | panel de pantalla, widgets, efectos y escaladores | app, rzx | 3 | **queda**: es el SDK de ventanas |
| `machine/devices/kit` | 14 | `Equipment`, `DeskEquipment`, `Desk`, `DeviceFrame`… | app | 26 | **queda**: es la API que implementan los plugins |
| `machine/generated` | 11 | el núcleo rápido, generado de las fuentes de `emulator` y `core` | app | 0 | **queda**: se deriva de la base |
| `machine/bridge` | 31 | el puente a Fuse, oráculo de los tests | — | 0 | **queda**: es de los tests |
| `machine/app` | 23 | el escritorio y la ventana de máquina | — | — | **se parte** (etapas 3 y 4) |
| `machine/machines` | 2 | la 48K y el `Machines` que la registra | 5 | 18 | **queda** (decisión 1) |
| `machine/machines/ula` | 5 | la ULA y el beeper | host/input | 1 | **queda** (decisión 1) |
| `machine/machines/memory` | 5 | la paginación 128 y +3 | — | 1 | **plugin** |
| `machine/machines/ay` | 7 | el AY, el del +3 y el de Timex | media/snapshot | 3 | **plugin** |
| `machine/machines/disk` | 14 | uPD765, WD1793, TR-DOS, las disqueteras | — | 1 | **plugin** |
| `machine/machines/scld` | 4 | el SCLD y la memoria de Timex | — | 0 | **plugin** |
| `machine/devices/ide` | 9 | IDE, MMC y los canales que comparten | — | 6 | **plugin biblioteca** |
| `machine/host/sound` | 5 | la placa de sonido de Java, DAC, entrada de audio | bridge (test) | 1 | **plugin** |
| `machine/host/input` | 13 | teclado de PC, disposiciones, joysticks | disk | 4 | **se parte** (decisión 3) |
| `machine/media/snapshot` | 3 | `Snapshots`, quien arranca una máquina desde un snapshot | rzx, bridge | 2 | **queda** (ver etapa 5) |
| `machine/media/rzx` | 4 | sesión y grabación RZX | app | 2 | **plugin** |
| `zx-rzx` | 16 | el formato RZX | media/rzx | 0 | **plugin**, adentro de `media-rzx` |
| `machine/pokes` | 3 + 2,8 MB | los listados `.pok` y quien los lee | app | 2 | **plugin** |

Fuera de alcance: `translation/*` y `prototypes/*`. Son el traductor de Z80 a Java y los
prototipos del modelo; ningún módulo del emulador depende de ellos.

## Lo que no se mueve, y por qué

- **`emulator` y `core`** son la definición de la Spectrum. Además el generador del núcleo rápido
  lee las fuentes de esos dos módulos y de ningún otro (`machine/generated/pom.xml`, ejecución
  `model-sources`): lo que salga de ahí sale del modelo que se genera.
- **`generated`** se deriva de las fuentes de la base y tiene que versionarse con ellas. Como
  plugin en otro repositorio quedaría viejo con cada cambio del núcleo.
- **`ui` y `devices/kit`** son contra lo que compilan los plugins: el panel, los widgets, `Equipment`,
  `Desk`, `DeviceFrame`. Moverlos haría que la base dependa de un plugin.
- **`bridge`** es el oráculo de los tests contra Fuse. No lo ve nadie que use el emulador.
- **RZX** (formato y sesión): `RzxOracleTest` y la medición A/B del núcleo (`RzxCoreMeasurement`)
  corren grabaciones en la base. El escritorio ya no lo nombra: lo abre quien diga que abre ese
  archivo (`opensFor`).
- **`host-sound`**: `bridge` reproduce el audio de Fuse con él, y sin sonido la base no es un
  emulador que se pueda usar.
- **`host-input`** (etapa 5, medido): lo que ya podía irse se fue antes, en `device-joystick`
  (gamepads, Kempston estricto). Lo que queda es la tecla de la PC llevada a la matriz y los
  joysticks que la Spectrum lee por esa misma matriz (Sinclair, cursor): `Input` los mezcla en
  55 lugares, el archivo de configuración y el Z80 guardan qué joystick había, y `JoystickTest`
  y `KeyboardTest` de la base los usan. Cortarlo sería partir la matriz en dos.
- **Los formatos de snapshot** (etapa 5, medido): en `machine/spectrum` queda sólo el Z80, que
  usan los tests de la base; los demás ya llegan por plugin (`has::formats`).
- **La 48K de referencia** (decisión 1): es la máquina con que corren los tests de la base y la
  que arma el generador en su paso de build.

## Decisiones que son tuyas

Cuatro, y el plan está escrito con la recomendación de cada una:

1. **La 48K y la ULA: ¿quedan en la base?** Recomendado: **sí**. Los tests de la base nombran
   sólo `Spec48` (10 usos), el generador arma una 48K en su build, y el puente contra Fuse compara
   una 48K. Si se van a oozx-plugins, la base no puede testearse sin el otro repositorio, que a su
   vez necesita la base construida: un ciclo entre repositorios que el CI no puede resolver en un
   solo paso. Quedándose, con `application-1` se ven en el panel como `application` y un plugin
   puede taparlas con `@Replaces`. Y es lo que dice la meta: una ventana con **una Spectrum**.
   La alternativa es mover también los tests que arman una máquina y darle al generador una
   máquina de mentira; es más trabajo y no gana nada visible.

2. **Los prefijos de los ids.** Hoy el CI de oozx-plugins publica sólo los módulos cuyo
   `artifactId` empieza con `device-` o `tool-` (`plugins.yml`, línea 3), y
   `PluginReleases.WHAT_PLUGS_IN` filtra el catálogo de GitHub por los mismos dos. Un plugin
   `media-rzx` o `desk-looks` no se publicaría y no aparecería para instalar. Recomendado:
   **no renombrar ningún id que ya exista** —un id es la identidad del plugin instalado y de su
   release— y, para lo nuevo, o bien quedarse en `device-`/`tool-`, o bien sumar los prefijos
   nuevos en los dos lugares a la vez. El panel agrupa por prefijo, así que un prefijo por clase
   de cosa se ve mejor.

3. **El teclado.** La ventana pelada tiene que poder tipearse, y `SwingKeyboard` (en `app`) usa
   `Input` y `Keyboard` de `host/input`. Recomendado: **partir `host/input`**: lo que lleva una
   tecla de la PC a la matriz de la Spectrum queda en la base; las disposiciones extra y los
   joysticks se van a un plugin. Hay que medir qué parte usa `SwingKeyboard` antes de cortar.

4. **¿Existe una distribución pelada?** Hoy todo build empaqueta los plugins. Una "sólo la base"
   serviría para probar que la base se sostiene sola —y para quien quiera armarse la suya— pero
   es un segundo jar que mantener. Recomendado: **no como producto**, sí **como test**: uno que
   arranque la base sin plugins y compruebe que muestra una 48K que responde al teclado.

## Los chips de las máquinas

Es la parte donde las dependencias son lo fundamental. Hoy las máquinas ya son plugins, pero sus
chips están en la base: por eso el manifiesto de `device-spectrum128` dice `Plugin-Dependencies`
vacío aunque la 128K no funcione sin el AY — el AY está en el classpath de la aplicación y el
framework no lo ve como dependencia. **El día que el AY sea un plugin, esa dependencia aparece
sola en el manifiesto de cada máquina que lo nombra**, y sacar el AY pasa a llevarse a las 128K.

### Qué chip necesita cada máquina

Medido por lo que cada máquina nombra (en `onBoard()` y alrededores):

| máquina (plugin) | ULA | memoria | AY | disco | SCLD | depende además de |
|---|:-:|---|---|---|:-:|---|
| 48K (base) | ✔ | — | — | — | — | — |
| `device-spectrum128` | ✔ | 128 | AY | — | — | — |
| `device-amstrad` (+2A/+3) | ✔ | +3 | AY +3 | uPD765 | — | spectrum128 |
| `device-chrome` | ✔ | +3 | AY | — | — | spectrum128 |
| `device-spectrum-se` | ✔ | 128 + Timex | AY | — | ✔ | spectrum128 |
| `device-chloe` | ✔ | 128 + Timex | AY | — | ✔ | spectrum-se, ulaplus |
| `device-pentagon` | ✔ | 128 | AY | Beta128 | — | spectrum128 |
| `device-scorpion` | ✔ | +3 | AY | Beta128 | — | amstrad |
| `device-timex` | ✔ | Timex | AY Timex | — | ✔ | — |
| `inves`, `czerweny`, `microdigital`, `spectrum-variants` | ✔ | — | — | — | — | — |

La ULA no aparece nombrada en ninguna: se pone en toda máquina por su propia regla
(`UlaPeripheral.fitsOn`). Por eso, quedándose en la base, no genera ninguna dependencia.

### El grafo que queda

```
BASE     emulator · core · spectrum · ui · devices-kit · app (ventana pelada)
         48K de referencia: Spec48 + ULA + beeper · teclado a matriz
         generated · bridge (tests)
           │
CHIPS    device-memory   device-ay   device-disk ──► host-input (joystick de Beta128)
           │                │            │           device-scld      lib-ide
           │                │            │               │               │
MÁQUINAS device-spectrum128 ──► memory(128), ay
           ├── device-amstrad ──► memory(+3), ay(+3), disk(uPD765)
           │      └── device-scorpion ──► disk(Beta128)
           ├── device-chrome ──► memory(+3)
           ├── device-pentagon ──► disk(Beta128)
           └── device-spectrum-se ──► scld
                  └── device-chloe ──► device-ulaplus
         device-timex ──► scld, ay(Timex)
                                                        │
DISPOSITIVOS  divide, divmmc, zxmmc, zxatasp, zxcf, simpleide ──► lib-ide
              beta128, plusd, opus, disciple, didaktik, snapshots ──► device-disk
```

### Los nudos, antes de mover un chip

1. **`Snapshots` nombra `AyPeripheral`** (`media/snapshot/.../Snapshots.java:231`) para saber si
   restaurar el estado del AY que trae un `.z80`. Si el AY se va, la base importa un plugin. El
   estado del AY es del AY: lo que corresponde es un rol —algo como "lo que viaja en un snapshot"—
   que el plugin del AY implementa, y que `Snapshots` recorre sin saber quién es. Es el único
   lugar de la base que nombra un chip.
2. **`machines/pom.xml` depende de los cinco chips en compile**, pero `Spec48` no nombra ninguno.
   Es una dependencia que quedó de cuando las ocho máquinas vivían ahí. Se saca con la primera
   etapa y se comprueba que la 48K sigue arrancando.
3. **`host/input` depende de `device-ula` en su pom** sin importar nada de ella en el código. A
   verificar: si no la usa, se saca, y si la usa, es lo que decide la decisión 3.
4. **`Beta128Peripheral` usa `Input` y `Joystick`** de `host/input`. Si la entrada se parte
   (decisión 3), `device-disk` depende del plugin de entrada, o de la parte que quede en la base.
5. **Los tests viajan con su chip.** `AyStepsToTheNextEventTest`, `DiskImageTest`,
   `JoystickTest` y `KeyboardTest` están hoy en `machines/src/test`. Se mudan con el plugin que
   prueban, como se hizo con `WhatTheArchiveWithholdsTest` al mudar el catálogo.
6. **`device-ide` pasa a ser un plugin biblioteca**: no hace nada solo, lo usan seis dispositivos.
   Como lo usan por sus tipos, la dependencia la detecta el build; no hace falta `@Needs`.
7. **Seis dispositivos usan las clases del disco sin declararlo**: `beta128`, `plusd`, `opus`,
   `disciple`, `didaktik` y `snapshots` nombran `Fdd`, `WdFdc` o `Disk`, pero ninguno tiene a
   `device-disk` en su pom — hoy les llega por el classpath de la aplicación. Cuando el disco sea
   un plugin, el build va a encontrar la dependencia en el bytecode; sus poms tienen que declararla
   para compilar.

## La ventana de la máquina, pelada

Hoy `EmulatorInternalFrame` (en `ZXSpectrumDesktopApp.java`, líneas 68 a 930) tiene alrededor
de la pantalla:

| dónde | qué |
|---|---|
| barra de herramientas (13) | turbo con su regla de velocidad, borde, pausa, ajustes, reset, silencio con volumen, pokes, detalles del juego, pantalla completa, zoom, snapshot, pantalla (TV y escalado), favorito |
| barra de estado (5) | velocidad, combo de modelo, indicador de pausa, indicador de turbo, lo que el emulador tiene para decir |
| menú contextual (6) | look, velocidad, volumen, borde, pausa, pantalla completa |
| la ventana misma | abrir archivo, recordar y restaurar su lugar |

**Lo que ya hay para que un plugin opere la máquina:** `EmulatorControl`, en `core`, que los
plugins ya ven: pausa, reset, velocidad, cargar y guardar estado, opciones de video, audio y
entrada, modelo, procesador, juego de ROMs, silencio, volumen, archivo cargado. **Lo que falta es
el lugar donde un plugin pone un botón**: hoy los trece están escritos a mano en `createToolBar()`.

**La costura.** Un rol en `devices/kit`, al lado de `Equipment` —es donde viven los roles que ven
los plugins, así que no hace falta otro lugar—: una herramienta de la ventana de máquina con su
ícono, su texto, lo que hace sobre esa ventana, y opcionalmente cómo se entera de un cambio para
reflejarlo (el turbo encendido, la pausa). La barra, la barra de estado y el menú contextual se
arman recorriendo lo que haya de ese rol. Sin plugins, no hay nada que recorrer: queda la pantalla.
A decidir al implementarlo si la barra de estado y el menú contextual son el mismo rol con un
lugar distinto o roles aparte; el criterio es el del `CLAUDE.md`, que haya uno solo si alcanza.

**Cómo se agrupan en plugins**, por lo que la persona entiende como una cosa:

| plugin | qué pone | depende de |
|---|---|---|
| `tool-controls` | pausa, reset, turbo y velocidad, silencio y volumen | — |
| `tool-view` | borde, zoom, pantalla completa, pantalla (TV, escalado, look del menú) | — |
| `tool-snapshots` | guardar el estado, historial, galería | — |
| `tool-model` | el combo de modelo, procesador y juego de ROMs | las máquinas que haya |
| `tool-pokes` | el botón de pokes y el diálogo, con los listados | — |
| `tool-games` (ya existe) | detalles del juego, favorito | `tool-catalogue` |

## El escritorio

Hoy `ZXSpectrumDesktopApp` (líneas 931 a 2804) tiene:

| dónde | qué |
|---|---|
| menú File | abrir, archivos recientes, cargar y guardar estado, salir |
| menú Emulator | pausa, turbo, silencio, Equipment, Plugins |
| menú Options | TV, líneas de barrido, ajustes |
| menú Window | look and feel, ventanas abiertas |
| menú Help | README, acerca de |
| barra | nueva máquina, navegador de juegos, historial, favoritos, plugins, ajustes |
| ventanas propias | historial de snapshots, favoritos, galería, ajustes, pantalla, pokes |

Queda en la base lo que hace falta para abrir algo y ver la máquina: abrir un archivo (que ya
pregunta a `StartsAMachineOn`), nueva máquina, salir, la ventana de plugins y el aviso de lo que
falta. **El resto usa la misma costura que la ventana de máquina**, en su versión de escritorio, y
las ventanas propias se vuelven `DeskEquipment` de su plugin, como ya lo es el navegador de juegos.

**El que más pesa es el look and feel.** `LookAndFeels.java` (390 líneas) es el único archivo que
usa darklaf, FlatLaf, Radiance, Material y JGoodies: unos 8 MB de bibliotecas que hoy viajan en la
base. Como plugin (`desk-looks`, o `tool-looks` si se decide no sumar prefijos) se los lleva en
su `lib/`, y un rol "un look" deja que otro plugin agregue los suyos. Un detalle ya conocido:
darklaf usa jsvg 0.0.9 y `framework-swing` trae el suyo relocado desde `swing-7`, así que no chocan.

## Herramientas y medios

| plugin | qué se lleva | depende de | nudo |
|---|---|---|---|
| `tool-pokes` | `machine/pokes` (3 clases, 2,8 MB de `.pok`), `PokesDialog` (419 líneas), aplicar y revertir pokes de la ventana de máquina | — | `tool-poke-finder` y `tool-catalogue` pasan a depender de él: el catálogo arma los pokes por número de entrada |
| `media-rzx` | `zx-rzx` (el formato), `media/rzx` (sesión y grabación), `RzxOption` | — | el escritorio usa `RzxOption` 7 veces y `RzxSession` 4: esos usos se vuelven del plugin o de un rol |
| `host-sound` | `JavaSoundDevice`, DAC, entrada de audio | — | ya hay costura: `core` liga `SoundCard` con un `OptionalBinder` cuyo valor por defecto es el silencio. `bridge` lo usa sólo en tests |
| `lib-ide` | `devices/ide` | — | biblioteca de seis dispositivos |
| `host-input` (lo que no quede en la base) | disposiciones extra, joysticks | — | decisión 3 |

Cómo queda la cadena de juegos, que es la más larga:

```
tool-games ──► tool-catalogue ──► tool-pokes ◄── tool-poke-finder
     │
     └──► media-rzx ◄── tool-rzx
```

Sacar `tool-pokes` se lleva al buscador de pokes, al catálogo y al navegador. Es correcto —el
catálogo no funciona sin los pokes que arma— pero el panel tiene que decirlo antes, no después.

## Lo aprendido que aplica a cada etapa

Todo esto ya mordió al menos una vez:

- **Lo que los plugins ya publicados llaman es API.** Un método público que desaparece da
  `NoSuchMethodError` en un jar cacheado. Deprecar con un delegado, no borrar, y listar lo que
  llaman los jars de `~/.oozx/plugin-cache` con `javap` antes de tocar nada.
- **Las bibliotecas de terceros viajan con el plugin.** Mover un módulo a plugin le saca el
  classpath de la aplicación: el catálogo no arrancaba por `jakarta.ws.rs`. Con `libs-1` el build
  las empaqueta; mirar la línea `crystal: carries N libraries in lib/` en cada uno.
- **Lo que busca por el classloader del hilo no encuentra lo del plugin.** Resteasy, y cualquier
  biblioteca que use `ServiceLoader` o el `ContextClassLoader`: el catálogo parecía preferir lo
  local porque internet nunca contestaba. Poner el classloader del plugin durante la llamada.
- **Mover directorios es seguro; renombrar paquetes no.** Varios módulos comparten paquetes y se
  ven sin `import`. Mudar un módulo tal cual, con `git subtree` para que traiga su historia.
- **El padre de los plugins arrastra el núcleo.** `tools/pom.xml` declara `core`, `ui`,
  `devices-kit` y `machines` sin scope: un plugin que empaqueta se los lleva adentro. `libs-1`
  deja afuera lo de `com.fpetrola`, pero conviene que el log no los liste.
- **El índice de roles se pierde en silencio** y aparece lejos, como un binding de Guice que
  falta. Resuelto para el jar único (`AppendingTransformer` + `check-roles`) y para la compilación
  incremental del IDE (`processor-3`).
- **Un respaldo que funciona demasiado bien tapa la falla.** Cuando algo degrada con elegancia,
  que además lo diga.
- **Los tests de la base corren con los plugins apagados**, y apagados no es sin lo propio: lo
  que la base trae adentro tiene que seguir contestando.

## Orden

Cada etapa termina con las dos suites verdes, el jar armado y arrancado sin red, y el panel
mostrando lo que se movió con su dependencia. Primero lo que no tiene nudos, después lo que
enseña cómo es la costura, al final lo que toca la ventana que la persona mira.

| etapa | qué | por qué en este orden | tamaño |
|---|---|---|---|
| 0 | las costuras: rol de herramienta de ventana; rol de "lo que viaja en un snapshot"; prefijos (decisión 2) | sin ellas no hay a dónde mover | chico |
| 1 | `tool-pokes`, `media-rzx`, `desk-looks`, `host-sound` | ya tienen costura o casi, y son los que más pesan | mediano |
| 2 | chips: `device-scld`, `device-memory`, `device-ay`, `device-disk`, `lib-ide` | la base casi no los nombra; el AY espera al nudo 1 | mediano |
| 3 | la ventana de máquina: `tool-controls`, `tool-view`, `tool-snapshots`, `tool-model` | usa la costura de la etapa 0 y deja la ventana pelada | grande |
| 4 | el escritorio: menús y barra por la misma costura, ventanas propias como `DeskEquipment` | lo último que la persona ve cambiar | grande |
| 5 | `host-input` según la decisión 3; revisar si los formatos de snapshot que no usan los tests se van a `media-snapshot` | depende de decisiones y de medir `SwingKeyboard` | chico |

Hecho hasta la etapa 5. Lo que cambió respecto del plan: RZX y `host-sound` quedaron (ver arriba);
historial y favoritos fueron a `tool-games` y no a un `tool-snapshots` propio, porque ya tenía el
favorito; la galería se borró porque nadie la usaba.

En cada etapa, antes de mover algo:

1. `grep` del nombre simple de cada clase en los dos repositorios, no sólo de su `import`.
2. `javap` de los plugins cacheados, para saber qué métodos públicos no se pueden sacar.
3. `git subtree split` del módulo, `subtree add` en oozx-plugins, y recién después borrar en la base.
4. Suite de la base con `install`, **después** la de plugins: al revés, los plugins compilan
   contra lo viejo que hay en `~/.m2`.
5. El jar de un solo archivo, abierto y contado, y arrancado con casa vacía y sin red.
