# Una base común para oozx e IdeSeed

Fecha: 2026-11-05. Lo pedido: un entorno Swing que no sabe nada de la aplicación que lo usa (ni
emulador, ni IDE, ni Help, ni menú File) y que solo sabe extenderse con crystal-plugins: la ventana
de plugins de crystal, y una capacidad genérica para que plugins y aplicación propongan acciones en
los menús, botones en la barra y ventanas en el escritorio. oozx e IdeSeed se arman sobre eso, cada
uno agregando lo suyo desde cero por la misma puerta.

Relevamiento hecho sobre oozx (rama `traduccion`, 5661cd036), oozx-plugins (70fb42f), crystal-plugins
(a2b5389) e IdeSeed (a2b14db), con lo que contaron las sesiones ideseed-1d e ideseed-c6.

## Cómo está hoy

### oozx

`ZXSpectrumDesktopApp` (`machine/app`, 2304 líneas; las primeras 354 son `EmulatorInternalFrame`)
es un `JFrame` con un `JDesktopPane` que implementa `Desk` (kit). Lo que tiene, separado por si
sabe o no del emulador:

| de la base (no nombra máquinas) | líneas | del emulador | líneas |
|---|---|---|---|
| marco: tamaño y lugar entre corridas, cerrar guarda la sesión | ~60 | menú File: abrir, recientes, guardar y cargar estado, salir | ~90 |
| armado de la barra de menú a partir de lo que hay | ~40 | menú Emulator: nueva máquina, Equipment, atajos de `MachineTool` | ~60 |
| menú Window: cerrar, cerrar todas, cascada, mosaico (con sus dos algoritmos) | ~140 | menú TV: modos y líneas de barrido, `setScreenDefault` | ~60 |
| Look&Feel: menú, recordar, `LookAndFeels` (434: ponerse un look, confirmarlo, volver si no pinta, el plugin que se va) | ~460 | About | ~60 |
| ventanas del escritorio: `showOnTheDesk`, `place`, `nameOf`, `window`, botones con ícono y el `+` | ~110 | juegos, grabaciones, favoritos, `load`, `nothingOpens`, `Chosen` | ~450 |
| huecos `(+)`: `couldBring`, `lookForWhatCouldBeBrought`, `holes`, `hole`, `bringAndDo`, `addHoles` | ~90 | equipo enganchado: `show(Equipment)`, `clipOntoTheMachineInFront`, `openWhatTheMachineUses`, `equipmentOn`, `cassetteFor` | ~250 |
| paleta "What do you want to do?" (`ActionPalette`, 140) y el menú Discover | ~160 | teclas a la máquina de adelante, `machineBeingUsed`, `somebodyIsWriting` | ~80 |
| ventana de plugins (`PluginsInternalFrame`, 53, sobre `PluginsPanel` de crystal) | ~60 | `registerMachine`, `machineOf`, `coreOf`, `createNewEmulator`, `putMachinesAway` | ~150 |
| `WhatIsMissing` (184): traer lo que falta y hacer lo que se pidió | ~190 | `EmulatorInternalFrame`: pantalla, barra de `MachineTool`, barra de estado | ~300 |
| `TellsThePerson` escuchado y mostrado | ~25 | `whatThereIs`: efectos de pantalla y formatos de snapshot a quien los usa | ~10 |
| guardar y volver cada ventana (`Desk.keep/bringBack` en el kit, con `WindowInside` de tool-mappings) | ~80 | | |

Total: unas 1400 líneas que no saben de máquinas y unas 1500 que sí. Los números son del conteo a
ojo sobre el archivo, no exactos.

Los roles que los plugins ven, en `devices/kit`: `DeskEquipment` (4 implementaciones, `name/open/icon`),
`Explains` (1, va al menú Help), `Look` (7, en `tool-looks`), `Equipment` (43), `MachineTool` (13),
`Session`, `WhatGameThisIs`, `StartsAMachineOn`; `Extension` en core (43); `WindowInside` en
`tools/mappings`. De estos, `DeskEquipment`, `Explains`, `Look` y `WindowInside` no saben de máquinas.

La plomería de plugins vive en `machine/core`, sin Swing: `Plugins` (432: la carpeta `~/.oozx/plugins`,
lo que trae el jar, la caché, `managing/found/asModule/add/readWhatArrived`), `WhereAPluginComesFrom`
(140: carpeta más releases publicadas como un `PluginSource`), `PluginReleases` (279: el board de
GitHub `fpetrola/oozx-plugins`, los prefijos `device-`/`tool-`, quién pregunta), `TellsThePerson`,
`Configuration` (312: `config.json` por secciones, casa `~/.oozx`) y `OOZxConfiguration` (884:
`WindowState`, ventana principal, ventanas abiertas, look, recientes, favoritos, snapshots, pantalla).

`machine/ui`: `AttachedFrame` (819, engancharse a una máquina), `Widgets` (373), `KnobRows`, los
cargadores de SVG sobre batik, `MachineWindow`, y `screen/*` (del emulador).

Lo publicado que nombra estas cosas (`oozx-plugins`, por módulo): `Desk.theOne()` en 6, `Widgets` en
19, `extends AttachedFrame/MachineFrame/DeviceFrame/DacFrame` en 29, `OOZxConfiguration` en 4,
`WindowState` en 1. Nada de eso se puede sacar (`plugin-binary-api`): se deprecia y delega.

crystal-plugins ya tiene lo que la base necesita de él: `framework-swing` (`PluginsPanel`,
`PluginTrees`, `PluginIcons` con jsvg relocado), `framework-guice` (`PluginsModule`), `@Offers`,
`@Answers`, `@OnlyWith`, `applicationImplementations` (la aplicación aporta implementaciones de
sus roles igual que un plugin) y, lo que decide todo lo de abajo: `Roles.of` resuelve los roles de
una extensión en runtime recorriendo superinterfaces. Un rol del kit que pase a extender uno de la
base hace que los plugins ya publicados sean de la base sin recompilarlos.

### IdeSeed

`Main` (171): un `JFrame`, FlatLaf Darcula con Inter y JetBrains Mono, `Zoom`, la última carpeta en
`~/.ideseed/last`. `Workbench` (1566, un `JPanel`): tres `JSplitPane` anidados (árbol | (editores /
vistas) | planes), run bar arriba, barra de estado abajo; `menus()` arma File, Edit, View, Navigate,
Code, Refactor, Build, Run y Help desde ids; `act(id)` es un switch de 110 líneas; Find Action
filtra `Keymap` con CamelCase. `Keymap` (93): una tabla de `Key(id, nombre, teclas, descripción)`,
49 entradas, con el atajo de IntelliJ primero y el de Eclipse después. `Views` (239): pestañas
Problems, Changes, Steps, Output, Results, Run, Replay, Terminal; no se cierran ni flotan. Persiste
poco: `Shown` (booleanos del menú View), el zoom, la última carpeta. Ni tamaño de ventana, ni
divisores, ni pestañas. Sin plugins, sin ServiceLoader: agregar una acción es una fila en `Keymap`
y un `case` en `act`. Regla propia: la UI no decide nada, habla con el IDE solo por capacidades
(`Ide`, JSON), garantizado por `AdapterTest` (ArchUnit, lista los paquetes permitidos). Los tests
corren sin pantalla: paneles nunca frames, `Dialogs` es una interfaz que `ScriptedDialogs` guiona.

Lo que las dos sesiones de IdeSeed consideran genérico y coincide con lo de oozx: el arranque de
look y fuentes, `Keymap` como modelo de acciones con varias teclas, los menús armados desde ids,
Find Action, `Dialogs` guionable, `Zoom`, `Shown`, el layout de splits con Maximize Views, la
barra de estado. Lo específico: editores, árbol con errores, vistas de refactoring, planes,
replay, run, Claude y MCP. `Workbench` mezcla las dos cosas.

IdeSeed está decidiendo su mapa de módulos (ARCHITECTURE.md §4.1, etapa 5, M0 a M7): `adapters/`
como padre de `cli`, `ui` y `mcp`. Las dos sesiones dicen lo mismo: la base sería un módulo aparte
que `ui` declara, con su `AdapterTest`.

## El principio

- **La base no sabe quién la usa.** Sabe que hay un marco, una barra de menú, una barra de
  herramientas, una barra de estado y un lugar donde van ventanas. Lo que hay en cada uno llega por
  roles de crystal, de los plugins y de la aplicación por igual (`applicationImplementations`).
  Ningún `if` por aplicación, ningún nombre de menú escrito en la base.
- **La base no inventa menús.** No hay File ni Help. Cada acción dice su camino (`File/Open...`,
  `Help/About`); los menús nacen de los caminos y mueren con ellos; el orden lo dice cada acción con
  un peso; lo que falta por traer va en gris al final de su menú, como hoy.
- **Un concepto, un dueño.** Acciones (menú, tecla, paleta, botón) en un rol; ventanas en otro;
  looks en otro; dónde van las ventanas en el layout; qué tiene una ventana adentro en
  `WindowInside`. oozx e IdeSeed agregan roles suyos encima (`Equipment`, `MachineTool`,
  capacidades), nunca a la base.
- **Lo publicado sigue andando.** Los roles del kit de oozx pasan a extender los de la base; los
  nombres que los plugins publicados llaman (`Desk.theOne`, `Widgets`, `AttachedFrame`,
  `WindowState`, `Plugins`, `TellsThePerson`, `Configuration`) quedan como delegados deprecados.
- **Todo se prueba sin pantalla.** El escritorio es un objeto que arma paneles; el `JFrame` es un
  sostén de unas líneas. Hoy ningún test construye `ZXSpectrumDesktopApp` (por eso `Desk.keep`
  vive en el kit con un doble); IdeSeed sí construye `Workbench`. La base toma el modo de IdeSeed,
  y el doble de oozx deja de hacer falta. Los puntos fijos: `theDeskComesBackExactlyAsItWasLeft`
  y `WorkbenchTest`.
- **Los dos layouts son de la base.** MDI (`JDesktopPane`, lo de oozx) y acoplado (splits con
  pestañas a los lados y abajo, lo de IdeSeed). La aplicación elige uno; una ventana dice dónde
  prefiere ir (centro, abajo, izquierda, derecha, suelta) y el layout decide.
- **Prueba en rojo primero, una por paso**, y cada paso de la base con su tag; el orden de
  publicación gana un eslabón: crystal, base, oozx, plugins (`publishing-releases`).

## Dónde vive, y de qué está hecha

Un repositorio nuevo, con dos módulos. Decidido por Fernando el 2026-11-05: crystal-plugins queda
puro, maneja plugins de manera abstracta y nada más; su `PluginsPanel` es un ayudante para
configurar en Swing, no parte del framework. Lo que organiza la base gráfica de un entorno no va
ahí. En oozx tampoco puede ir: IdeSeed no puede depender del emulador. El repositorio es una base
para cualquier aplicación que se construya sobre crystal, como hoy lo son oozx e IdeSeed. El nombre
está por elegir (decisión 1); en lo que sigue los módulos se llaman `app` y `desk`. crystal no
cambia en ningún paso: la base solo lo usa.

- **`app`** (sin Swing): lo que una aplicación necesita para tener una casa y plugins. `Home` (la carpeta de la persona, dada una vez: `.oozx`, `.ideseed`) y
  `Configuration` (hoy `com.fpetrola.oozx.config.Configuration`, tal cual), `Plugins` (hoy
  `com.fpetrola.oozx.plugins.Plugins`), `Releases` (hoy `PluginReleases` y `WhereAPluginComesFrom`:
  un board de GitHub como `PluginSource`, con el repositorio, los prefijos de los jars y quién
  pregunta dados por la aplicación), `TellsThePerson`. `Plugins.asModule()` instala toda
  extensión que sea un `com.google.inject.Module`, sin nombrar `Extension`, que se queda en oozx.
  Depende de `framework-runtime` y `framework-guice` de crystal, y de jackson. Lo usa
  `machine/core`, que ya depende de `framework-guice`, así que no cambia el grafo. `Releases` es un
  `PluginSource` concreto (GitHub); si algún día se lo considera manejo de plugins y no de la
  aplicación, se va a crystal sin tocar a nadie.
- **`desk`** (Swing): los roles, el escritorio, los dos layouts, la ventana de plugins, la
  paleta, los huecos, los looks, los widgets e íconos, guardar y volver las ventanas. Depende de
  `app`, y de `framework-swing` y `framework-guice` de crystal.

Los roles de `desk`, con de dónde sale cada uno:

| rol | qué dice | sale de |
|---|---|---|
| `Action` | `text()`, `path()` (camino en el menú, el último tramo es el ítem), `keys()` (varias; la primera se muestra), `icon()` (con ícono hay botón en la barra), `place()` (orden), `run()` | `Keymap.Key` de IdeSeed más `MachineTool.key()` y los ítems a mano de oozx |
| `Actions` | `List<Action> actions()`, vuelto a pedir cuando el escritorio avisa que algo cambió | recientes, Equipment, modos de TV y ventanas abiertas de oozx; More Refactorings de IdeSeed |
| `Window extends Action` | `open()` (un `JComponent`; oozx devuelve `JInternalFrame`, retorno covariante), `prefers()` (dónde ir), `takesKeys()` (la terminal); `run()` por defecto la muestra o la trae adelante | `DeskEquipment` de oozx; las pestañas de `Views`, el árbol y `PlansView` de IdeSeed |
| `Look` | `family()`, `names()`, `wear(name)` | el `Look` del kit, tal cual |
| `WindowInside` | `keep(window, map)`, `bringBack(window, map)` | el de `tools/mappings`, tal cual |
| `Desk` | lo que una ventana le pide a su escritorio: `show(Window)`, `place`, `window(name)`, `nameOf`, `choose(what)`, `configuration()`, `changed()` (avisa que hay que rearmar), y `keep/bringBack` como hoy en el kit | el `Desk` del kit, sin las preguntas de máquinas |

Y lo que no es rol: `Workbench` (un `JPanel` con barra de menú, barra, estado y el layout, armado
desde `Set<Action>`, `Set<Look>` y el `PluginService`), `Layout` con `Mdi` y `Docked`,
`LookAndFeels`, `ActionPalette`, `WhatIsMissing` (con la lista de roles que responden dada por la
aplicación), `Dialogs` (`choose`, `line`, `tell`, `confirm`, archivo; `SwingDialogs` el real y
`ScriptedDialogs` en el test-jar), `Widgets`, íconos SVG por `PluginIcons`, `Zoom`,
`DeskConfiguration` (sección de `config.json`: ventana principal, look, ventanas abiertas con su
lugar y su `inside`, divisores del layout acoplado).

Las acciones propias de la base (Plugins..., la paleta, los looks, cerrar, cascada, mosaico, zoom)
tienen caminos por defecto (`Tools/Plugins...`, `Window/Cascade`...) y la aplicación reubica
cualquiera, suya o de un plugin, por su clase en una línea (`placing(PluginsWindow.class,
"Emulator/Plugins...")`). Un mecanismo para las dos cosas.

## oozx sobre la base: qué sale de dónde

| hoy | después |
|---|---|
| `ZXSpectrumDesktopApp` (app, 2304) | `SpectrumDesk` sobre `Workbench` con `Mdi`: implementa el `Desk` del kit; se queda con máquinas, juegos, grabaciones, equipo enganchado, teclas a la máquina, pantalla. Sus menús pasan a ser `Action`/`Actions` de la app: `File/Open...`, `File/Recent Files` (`Actions`), `File/Save State...`, `File/Load State...`, `File/Quit`, `Emulator/New Emulator`, `Emulator/Equipment/...` (`Actions` sobre `has.equipment()`), los `MachineTool` con tecla, `Window/TV/...`, `Help/About`. Unas 1400 líneas se van |
| `EmulatorInternalFrame` (dentro del mismo archivo) | archivo propio en app; su barra y estado de `MachineTool` siguen igual |
| `ActionPalette`, `PluginsInternalFrame`, `LookAndFeels`, `WhatIsMissing` (app) | base; en app quedan solo los roles que responden (`WhatIsMissing.ANSWERING`) pasados al armar el escritorio |
| `WhatIsPluggedIn` | se queda con `Equipment`, `MachineTool`, efectos, formatos, `starters`, `aboutGames`; pierde `deskWindows` y `looks`, que son de la base |
| kit `Desk` | extiende el `Desk` de la base, conserva las preguntas de máquinas y `theOne()`. Mismo nombre simple en dos paquetes: solo el archivo del kit nombra a los dos |
| kit `DeskEquipment` | `extends Window`, con `path()` por defecto `Emulator/<name>`; `Explains` con `Help/<name>` |
| kit `Look` | `@Deprecated`, extiende el `Look` de la base; `tool-looks` se republica contra la base y deja de depender de oozx (es la primera medida de que la base es real) |
| kit `Session.keep(window)/bringBack(map)` | deprecados; `Desk.keep` pregunta a los `WindowInside` de la base. `write/bringBack` de máquinas se quedan |
| `tools/mappings` `WindowInside` | el de la base |
| core `Plugins`, `PluginReleases`, `WhereAPluginComesFrom`, `TellsThePerson`, `Configuration` | `app` de la base; en core quedan delegados deprecados con los mismos nombres (`Plugins.managing/found/asModule/add/readWhatArrived/whoBrought/generation/whenHeld/beforeTakingOut`, `TellsThePerson.that/thisBuildCannot/listens`, `Configuration extends` la de la base) |
| core `Extension`, `BesideTheGame` | se quedan |
| `OOZxConfiguration` | pierde `mainWindowState`, `openWindows`, `lookAndFeel` (van a `DeskConfiguration`); `WindowState` queda deprecado mientras `KeepsItsPlace` y un plugin lo nombren |
| ui `Widgets` | base; el de oozx delega en lo publicado (`saveFile`, `scrollingWhenLong`, `iconButton`, `tighten`...) |
| ui `SvgIconLoader`, `SvgIconLoader0`, batik | se van: la base carga SVG con `PluginIcons` (jsvg), que ya viaja en `framework-swing` |
| ui `AttachedFrame`, `MachineWindow`, `KnobRows`, `screen/*` | se quedan (29 subclases publicadas; el enganche es de máquinas). `AttachedFrame` puede ir al layout `Mdi` después, con una subclase de compatibilidad |
| `app/pom.xml` | depende de `desk`; `crystal.bundleGroupId` igual |
| `oozx-plugins/tools/pom.xml` | los plugins que solo necesitan la base (`tool-looks`, `tool-help`) declaran `desk` como `provided` y nada de oozx |

Neto esperado en oozx: app unas -1400, core unas -900, ui unas -500. La base suma lo que se va menos
lo que se funde con lo de IdeSeed. El neto entre los tres repositorios es negativo recién cuando
IdeSeed pierde su parte de `Workbench` (paso 8); hasta ahí la base suma.

## IdeSeed sobre la base

- `adapters/ui` declara `desk` (M1 ya está hecho en IdeSeed, 734ce53: `ui` vive en
  `adapters/ui`); `AdapterTest` permite el paquete de la base. La regla "sin lógica, solo
  capacidades" no cambia: las acciones llaman a `Ide` como hoy llama `act`, y la base no ve
  `ide.session` ni recibe lógica del dominio (lo pidió ideseed-c6: es la regla que no se rompe).
- La barra de estado de la base muestra un texto que la aplicación actualiza (`Desk.say(text)`):
  IdeSeed está por abrir la ventana con solo la forma del grafo y mostrar el progreso de una
  capacidad `indexing` por timer; hoy tiene tres etiquetas (`status`, `position`, `libraries`),
  que pasan a ser componentes que el adapter pone en esa barra.
- `Main`: el `JFrame`, el look y las fuentes se vuelven `Desk.builder().home(".ideseed")
  .layout(Docked).look("Darcula")...`; quedan la apertura asíncrona del proyecto, el cartel de
  "Reading...", el opener y el ejemplo.
- `Keymap.KEYS`: cada fila es un `Action` (id, nombre, teclas, descripción, camino = el menú que
  hoy la lista). Primero un `Actions` por menú con el `case` que ya tiene cada acción (nueve
  fuentes, cambio mecánico); una acción se parte en clase propia cuando se va a un plugin.
  `act(id)` y `menus()` se van. Find Action es la paleta de la base, que además muestra lo que un
  plugin podría traer; Keymap Reference es genérico y también va a la base.
- Ventanas: el árbol (`LEFT`), `Editors` (`CENTER`), cada pestaña de `Views` (`BOTTOM`),
  `PlansView` (`RIGHT`); la terminal dice `takesKeys()`. El layout `Docked` pone a cada una en su
  panel, agrupa en pestañas las que comparten lado, pliega con one-touch, Maximize Views es suyo,
  y la base guarda divisores y pestañas abiertas, que IdeSeed hoy no guarda.
- Look: un `Look` propio de IdeSeed que viste FlatDarculaLaf con Inter y JetBrains Mono (esas
  llamadas son de FlatLaf, así que van en la implementación, no en el rol). `Zoom` va a la base
  como tres acciones. `Shown` pasa a ser una sección de `config.json`.
- `Dialogs` de IdeSeed extiende el de la base agregando `fill(Form)`, que depende de
  `Capability`/`Param` de `ide-core` y se queda.
- Se quedan sin tocar: editores, `Form`, el servidor MCP que la ventana sirve, `Ide`.
- Lo que IdeSeed gana sin escribirlo: la ventana de plugins, la paleta con lo que se puede traer,
  cambiar de look, los huecos en los menús, el lugar de cada ventana entre corridas.
- Después, lo que es de "evolución del entorno": la terminal (JediTerm y pty4j viajan en `lib/`
  por `crystal.carry`, igual que `tool-looks` lleva sus looks), Claude Code y los planes, run.
  Cada uno un plugin instalado por defecto por `crystal.bundleGroupId`, como hace oozx.

## Decisiones que son tuyas

1. **El nombre del repositorio.** Dónde vive ya está decidido: repositorio propio, crystal puro,
   dos módulos (`app` sin Swing, porque `machine/core` lo usa y `plan-modulos` lo quiso sin
   Swing; `desk` con Swing). El nombre tiene que decir GUI, plugins y base. Candidatos:
   - `lattice` (recomendado): la red sobre la que crece un cristal; la estructura base sobre la
     que el entorno crece por plugins; una palabra, de la familia de crystal sin ser crystal;
     paquetes `dev.lattice.app` y `dev.lattice.desk`, artefactos `lattice-app` y `lattice-desk`.
   - `facets`: las caras visibles de un cristal, que es lo que la GUI es de crystal.
   - `plugdesk`: dice las tres cosas de frente (plugins, escritorio, base), menos lindo.
   - `crystal-desk`: el más claro sobre la relación, pero parece parte de crystal, que es lo que
     se quiere evitar.
2. **Dónde van las acciones propias de la base.** Recomendado: caminos por defecto y la aplicación
   reubica por clase. Alternativa: un menú cuyo nombre da la aplicación y donde van todas.
3. **SVG.** Recomendado: jsvg (`PluginIcons`, ya en `framework-swing`) y batik se va de oozx. Pide
   un test que renderice cada SVG de oozx con jsvg antes de sacar batik.
4. **Abrir una ventana es una acción.** Recomendado: `Window extends Action` (un concepto menos: la
   ventana ya tiene nombre, ícono, camino y tecla). Alternativa: dos roles sin relación y el
   escritorio les fabrica el ítem.
5. **`Action` y `Actions`, o solo `Action`.** Recomendado: los dos; las listas que cambian solas
   (recientes, Equipment, ventanas abiertas, More Refactorings) no se escriben como una clase por
   ítem. `Look` sigue siendo rol propio porque la base es quien viste, recuerda y confirma.
6. **El nombre `Desk`.** Recomendado: la base se llama `Desk` y el del kit lo extiende con el mismo
   nombre simple, porque los plugins publicados nombran `com.fpetrola.oozx.speccy.devices.Desk` y no
   se puede renombrar. Alternativa: `Shell` o `Environment` en la base.
7. **Cuándo entra IdeSeed.** Recomendado: después de su M2 (`workbench/history`, en curso; M1 ya
   está) y después del paso 0 de acá con tag; hasta ahí la sesión de IdeSeed no toca `Workbench`
   por esto (confirmado por ideseed-c6).

## Estado

- 2026-11-05: Fernando eligió el nombre `plugdesk` (decisión 1). Las decisiones 2 a 7 se toman como
  están recomendadas mientras no diga otra cosa.
- Paso 0 hecho: `~/detodo/spectrum/versions/plugdesk`, commit efd71e5, tag local `step-0`, sin
  remoto todavía. groupId `dev.plugdesk`, módulo `desk` (artefacto `plugdesk-desk`, paquete
  `dev.plugdesk.desk`): `Action`, `Actions`, `Window`, `Desk`, `Layout`, `Mdi`, `Workbench` (287
  líneas) y seis pruebas sin pantalla en verde. Dos diferencias con lo escrito abajo: el módulo
  `app` nace en el paso 1, cuando hay qué mover a él, y `Look` y `WindowInside` en los pasos 2 y 3,
  donde se usan; un módulo o un rol vacío no prueba nada.

- Paso 1 hecho (tag `step-1` de plugdesk): módulo `app` (artefacto `plugdesk-app`, paquete
  `dev.plugdesk.app`) con `Application`, `Configuration`, `Section`, `TellsThePerson`, `Plugins`,
  `Releases` (antes `PluginReleases`) y `WhereAPluginComesFrom`. Lo que era de oozx lo dice
  `META-INF/plugdesk/application.properties` en `core` (nombre, casa, repositorio de releases y el
  de antes, prefijos, quién pregunta); `oozx.plugins=off` sigue andando porque es `<nombre>.plugins`.
  En oozx quedan tres delegados deprecados con lo que los jars publicados llaman (verificado con
  javap sobre la caché): `Configuration.shared/of/home/asText/fromText`, `Plugins.found`,
  `TellsThePerson.that/thisBuildCannot`. oozx y oozx-plugins nombran la base en todo lo demás.
  Se fueron siete métodos que nadie usaba. Trampa encontrada: el padre de los plugins excluía
  `framework-guice` de `core` para que pf4j no llegue a los plugins (su procesador pisa el índice
  de extensiones); ahora excluye `framework-runtime`, que llega por `plugdesk-app`. Y plugdesk solo
  corre el procesador de roles de crystal, por la misma razón. Los CI de oozx y de los plugins
  construyen plugdesk al tag que dice el de oozx.
- Paso 2 hecho (tag `step-2`): `Look` y `LookAndFeels` en `desk`; los looks se buscan con
  `Plugins.found(Look.class)` de la base, y el look con que arranca la aplicación lo dice su archivo
  (`look=Solarized Light` en oozx). `scrollingWhenLong` es de `Workbench`, dueño de los menús, y
  `Widgets` delega. El `Look` del kit queda deprecado extendiendo el de la base. `tool-looks` cuelga
  del padre raíz de los plugins y compila solo contra `plugdesk-desk` y la API de crystal. El padre
  de los plugins también excluye el runtime de crystal de `plugdesk-app` y `plugdesk-desk`.
  Verificado en Xephyr: arranca en el look guardado que trae el jar nuevo, el menú tiene las ocho
  familias, cambiar pregunta y volver vuelve.
- Paso 3 hecho (tag `step-3`): `WindowInside` es de la base (se fue de tools/mappings), y el `Desk`
  de la base sabe el nombre de una ventana (`nameOf`), abrirla por nombre (`window`), y guardarla y
  devolverla con lo que tiene adentro (`inside`/`bringBack`). `SzxSession` usa eso para toda ventana
  que no es una máquina y se queda solo con el plegado de las enganchadas. El `Workbench` de la base
  guarda y devuelve sus ventanas con su lugar (`keep`/`bringBack`), que es lo que va a usar IdeSeed.
  El `Desk` del kit extiende el de la base; su `nameOf(JInternalFrame)` queda deprecado y delega,
  porque el tool-mappings publicado lo llama. `DeskEquipment` es una `Window` con camino
  `Emulator/<nombre>` y `Explains` con `Help/<nombre>`. Lo que el paso 3 decía de sacar del app
  (`showOnTheDesk`, `fillTheDeskWindows`, `fillHelpMenu`, los botones) espera al paso 4: hasta que
  oozx arme sus menús con las acciones de la base, esos métodos son los que los arman.
- Paso 4 hecho (tag `step-4`): los menús y la barra de oozx son acciones que el `Workbench` de la base
  arma. El escritorio de oozx da las suyas (`offers()`: File, Emulator, TV, About, los equipos y las
  herramientas con tecla); las ventanas de los plugins llegan solas porque `DeskEquipment` es una
  `Window`; `Mdi` da las de Window; `LookAndFeels` da los looks; `Offered` da los huecos de lo que
  traerían los plugins que no están, donde va cada rol y todos bajo Discover; `Palette` busca entre
  todo lo ofrecido; `PluginsWindow` es la ventana de plugins. `WhatIsMissing` es de la base, con los
  roles que contestan dichos por la aplicación. El orden de los menús de arriba lo dice
  `menus=` en el archivo de la aplicación; un separador va entre centenas de `place`. `Action` ganó
  `needs()` (el plugin que falta) y `ticked()` (casilla). Se fueron del escritorio de oozx los menús
  hechos a mano, la barra, los huecos, cascada y mosaico, y tres clases (ActionPalette,
  PluginsInternalFrame, WhatIsMissing). Comparado el árbol de menús volcado del escritorio real antes
  y después: igual salvo que las opciones del TV son casillas y no radio, la barra pone Plugins antes
  de las ventanas de los plugins, una "/" en un nombre se escribe con la barra de división (U+2215)
  para no partir el camino, y los ítems con ícono lo muestran también en el menú.
- Paso 5 hecho en parte (tag `step-5`): el marco es de la base. `DeskWindow` hace la ventana, la pone
  donde estaba (o centrada la primera vez) y al cerrarse, como sea que se cierre, guarda su lugar,
  deja a la aplicación guardar lo suyo y escribe config.json. `DeskConfiguration` (sección `desk`)
  tiene el lugar y el look; `LookAndFeels` recuerda el elegido solo. El escritorio de oozx es un
  `JPanel`, ya no un `JFrame`, y se construye sin pantalla: `TheDeskIsMadeWithoutAWindowTest` lo
  arma de verdad y le pide sus menús. File > Quit cierra la ventana como su botón. El look y el
  lugar que guardaba oozx se toman una vez para la sección nueva (`adoptWhatTheBaseKeepsNow`).
  Visto en Xephyr con tu configuración: abre donde estaba y en Darcula, Quit escribe la sección, y
  al volver la ventana sale de ahí. Lo que no se hizo: que la prueba de punto fijo de tool-mappings
  corra sobre el escritorio real y se borre su doble. El doble vive en las pruebas de un plugin, que
  no dependen del app de oozx, y el escritorio real necesita los que construyen máquinas del lanzador
  y los equipos de los plugins con los plugins apagados. Queda como paso propio.
- Paso 6 hecho (tag `step-6`): `Widgets` es de la base (con `scrollingWhenLong` y `tighten`, que
  vuelven de `Workbench`); el de oozx delega cada uno de sus 16 métodos, porque 19 plugins
  publicados lo llaman (verificado con javap). Los SVG los dibuja jsvg 2.0, reubicado dentro de
  `plugdesk-desk` (`dev.plugdesk.desk.internal.jsvg`): `tool-looks` lleva jsvg 0.0.9 para darklaf, y
  crystal le da a un plugin la copia de la aplicación cuando la aplicación tiene la biblioteca. batik
  sale de `ui` y del app, con `SvgIconLoader` y `SvgIconLoader0`. Comparados los 43 SVG de oozx y
  sus plugins dibujados por batik y por jsvg: los mismos dibujos, con otro antialiasing.
  `EveryIconIsDrawnTest` (kit) dibuja cada SVG de `ui` y del kit y pide que pinte algo. Visto en
  Xephyr: la barra del escritorio y la de una máquina, iguales a las de batik.
- Paso 7 hecho (tag `step-7`): `Docked`, el layout de IdeSeed (izquierda, centro, abajo y derecha,
  cada lado con pestañas; divisores que se pliegan; Maximize Views con Ctrl+Shift+'; los divisores se
  guardan con `arrangement()`/`arrange()`). Una ventana acoplada no tiene lugar propio:
  `Layout.placeOf` puede ser null. Dos trampas de `JSplitPane` encontradas en pantalla: el divisor
  que puso el layout dice -1, y uno fijado antes de que su split tenga su tamaño se corre después.
  Para IdeSeed (paso 8) falta que la base guarde los divisores al cerrar: `DeskWindow` no ve el
  layout; va con el paso 8, que es el primero que lo usa.

## Los pasos

Cada paso es un commit con su prueba en rojo primero y la suite verde; los de la base, con tag.
Los pasos 0 a 6 son de la base y oozx; 7 a 9 de la base e IdeSeed; 7 puede empezar en cuanto el 0
tiene tag, en la otra sesión. crystal no cambia en ninguno.

0. **Esqueleto.** El repositorio nuevo con `app` y `desk`, los roles vacíos de la tabla,
   `Workbench` que arma barra de menú, barra y estado desde `Set<Action>`, y el layout `Mdi`. Su
   CI construye contra crystal como hoy lo hace oozx (`.github/workflows/maven.yml`).
   Prueba: un escritorio construido sin pantalla con dos acciones (`A/B/C` y `A/D`, una con ícono)
   tiene los menús A > B > C y A > D, las teclas ligadas y un botón; una ventana mostrada dos veces
   es la misma y queda adelante. Tag.
1. **La casa y los plugins** (`app`). Mueven `Configuration`, `Plugins`,
   `PluginReleases` con `WhereAPluginComesFrom`, y `TellsThePerson`, parametrizados (`Home.is
   (".oozx")`, repositorio, prefijos, quién pregunta); en core quedan los delegados. Pruebas: las
   de core que hoy cubren `Configuration` y `Plugins` siguen igual; en la base, una casa con una
   sección de `config.json` va y vuelve, y un board respondido desde un JSON fijo lista lo que
   tiene los prefijos dados. Medida: oozx arranca y la ventana de plugins muestra lo mismo.
2. **Los looks.** `Look` y `LookAndFeels` a la base; el del kit extiende deprecado; `tool-looks`
   republicado contra la base, sin oozx en su pom. Pruebas: el look elegido vuelve después de
   cerrar (sobre el escritorio sin pantalla, por la sección de configuración); `tool-looks`
   compila sin `core` ni `ui` en el classpath.
3. **Las ventanas.** `Window`, `Desk.show/place/window/nameOf`, `keep/bringBack` con `WindowInside`;
   `DeskEquipment extends Window` con su camino, `Explains` con el suyo; `WindowInside` a la base.
   Del app se van `showOnTheDesk`, `onTheDesk`, `fillTheDeskWindows`, `fillHelpMenu`, los botones
   del escritorio. Pruebas: `theDeskComesBackExactlyAsItWasLeft` sigue en verde con el doble del
   kit ahora sobre la base; en la base, una ventana de prueba va y vuelve con lugar e `inside`.
4. **Las acciones.** `Action`, `Actions`, huecos, paleta, `WhatIsMissing` con los roles de la app,
   `TellsThePerson` a diálogo; cerrar, cerrar todas, cascada y mosaico son acciones de `Mdi`; los
   menús del app se vuelven acciones (tabla de arriba). Se van `createMenuBar`, `addWindowMenu`,
   `createMainToolBar`, `fillWhatToDo`, `lookForWhatCouldBeBrought`, `holes`, `bringAndDo`,
   `ActionPalette`. Pruebas: en la base, una acción ofrecida por un plugin no instalado (un
   `PluginSource` de prueba) aparece en gris al final de su menú y en la paleta, y traído el
   plugin el ítem está vivo. En oozx, antes de empezar se vuelca el árbol de menús de hoy (texto,
   orden, teclas) y al terminar se compara: los pesos tienen que reproducirlo. Después, Xephyr con
   la configuración de Fernando (`running-the-real-desk`).
5. **El marco.** Lugar de la ventana principal y guardar la sesión al cerrar van a
   `DeskConfiguration`; `ZXSpectrumDesktopApp` pasa a `SpectrumDesk` sobre `Workbench`,
   construible sin pantalla. Prueba: el punto fijo corre sobre la clase real, y el doble del kit
   se borra.
6. **Widgets e íconos.** `Widgets` a la base y el de oozx delega; SVG por jsvg; batik fuera de
   `ui` y `app`. Prueba: cada SVG de oozx renderiza con jsvg (el que no, se arregla o se
   convierte, antes de sacar batik).
7. **El layout acoplado** (`Docked`), sacado de `Workbench` de IdeSeed: splits, pestañas por lado,
   plegar, Maximize Views, divisores guardados. Prueba en la base, sin pantalla: ventanas que
   prefieren cada lado caen en su panel, dos abajo son dos pestañas, maximizar esconde los lados y
   devuelve los divisores.
8. **IdeSeed sobre la base.** Lo de la sección de IdeSeed, después de su M2. Pruebas: `WorkbenchTest`,
   `ExampleTest` y `KeymapTest` afirman lo mismo que hoy (`menus()`, `tree()`, `files()`, `act(id)`
   siguen contestando); la ventana se ve y responde a las mismas teclas; `Workbench` perdió
   layout, menús y despacho (unas -700).
9. **El primer plugin de IdeSeed.** La terminal, con JediTerm y pty4j en `lib/`, instalado por
   defecto. Es la prueba de que crystal es la herramienta de evolución del IDE, no solo del
   emulador. Después Claude y planes, y run.
10. **Lo que queda después.** `AttachedFrame` al layout `Mdi` con subclase de compatibilidad;
    `KeepsItsPlace`, `WindowState`, `Session.keep/bringBack` y los delegados de core se borran
    cuando ningún plugin publicado los nombre (`javap` contra `~/.oozx/plugin-cache`).

## Riesgos y trampas

- **La API binaria de 52 plugins.** Nunca sacar, deprecar y delegar; verificar con `javap` sobre la
  caché de plugins antes de cada publicación. Los sub-roles son seguros sin republicar porque
  `Roles.of` recorre superinterfaces (verificado en `framework-runtime`, no corrido).
- **Classloaders.** Un plugin no debe llevar `desk` adentro (`provided`), la misma
  trampa que `framework-api` (`foreignRoleAnnotationHint`). Un rol de la base está en el classpath
  de la aplicación: una sola `Class` para todos.
- **Teclas.** La base liga todas las teclas de cada acción en la ventana enfocada y muestra la
  primera; una ventana que toma las teclas (terminal, máquina) las deja pasar con `takesKeys()`.
  En oozx el despacho a la máquina sigue siendo suyo, y el paso 4 de `plan-escritorio` ("la tecla
  va a un solo lado") sigue vigente.
- **El orden de los menús.** Los pesos deben reproducir el de hoy; volcar el árbol antes y
  compararlo después, no mirarlo.
- **Swing en tests.** Pasos en el EDT con `invokeAndWait`; las ventanas postean eventos desde el
  constructor (`saved-things-dont-know`).
- **Los tags.** oozx y los plugins compilan contra crystal `0.1.0-SNAPSHOT` instalado desde un
  tag; la base se publica igual, con tag propio, y el orden pasa a ser crystal, base, oozx,
  plugins. Los CI de oozx e IdeSeed clonan e instalan crystal y la base (hoy oozx clona solo
  crystal).
- **Dos sesiones.** Después del paso 0, oozx e IdeSeed pueden avanzar en paralelo; la base es lo
  compartido: una sesión por tag, no dos editando `desk` a la vez.

## Lo que no entra

- Pasar oozx al layout acoplado o IdeSeed a MDI.
- Una ventana de ajustes en la base: los de oozx son `Equipment` de una máquina
  (`plan-escritorio`, decisión 2); IdeSeed no tiene.
- Mover `Equipment`, `MachineTool`, `AttachedFrame`, `EmulatorInternalFrame`: son de máquinas.
- `Form`, las capacidades, el servidor MCP y los editores de IdeSeed.
- Contenido de ayuda: el README ya es un plugin (`tool-help`); la base no tiene menú Help, el
  camino `Help/...` lo pone `Explains`, que es de oozx.
