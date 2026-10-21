# Guardar el entorno y que vuelva exactamente igual

Paso 3 de `plan-version-1.md`. Cerrar OOZX y abrirlo tiene que dejar cada máquina, cada ventana y
cada aparato enchufado exactamente como estaban. Cada máquina se guarda en un SZX con los bloques
estándar de lo que el formato conoce más bloques propios; lo que no es de una máquina va al JSON del
entorno. **Nada de lo que se guarda sabe que se guarda**: ni los aparatos, ni las herramientas, ni
las ventanas tienen algo para guardarse o restaurarse. Cómo va cada cosa a un archivo lo sabe quien
conoce ese archivo.

**Estado (21 de octubre de 2026):** hechos el paso 0 (en el código) y lo de llevar bloques del 2. El
contrato del paso 1 (`Keeps` y `Kept`, que implementaba cada cosa guardada) se descartó y se sacó:
un aparato no tiene que saber que lo guardan. Lo reemplaza el módulo de la sesión.
Relevado sobre `traduccion` (oozx, `68d400341`) y `prueba-apuntar` (oozx-plugins, `0e3c6b2`).

## Cómo está hoy

Todo vive en `~/.oozx/config.json`, en la sección `desktop` (`OOZxConfiguration`):

- `openWindows` es una lista de `WindowState`: por cada ventana de máquina, x, y, ancho, alto,
  `zOrder`, el archivo de origen, turbo, pausa, mudo, `snapshotId` y los pokes aplicados.
- `snapshots` guarda cada máquina como texto: `"szx:"` más el SZX comprimido con gzip y pasado a
  caracteres (`Snapshots.packed()`, `SnapshotSaver.packed`).
- El SZX (`SzxFormat`, en `devices/snapshots`) lleva Z80R, SPCR, KEYB, AY, PLTT, RAMP y CRTR. De
  cualquier otro periférico escribe "not carried" y lo pierde.
- No se guarda: la posición, el tamaño, el lado ni el modo compacto de ninguna ventana de equipo;
  qué aparatos estaban enchufados; el aspecto de cada máquina (escalador, filtros, perfiles: solo
  los valores por defecto globales); el volumen y la velocidad de cada una; la cinta, los
  discos, cartuchos y tarjetas que tenían adentro.
- Los valores de `Settings.mirror` son globales: al guardar, cada máquina escribe los suyos encima
  y queda el de la última creada.

Lo que sabe que se guarda, y deja de saberlo:

- `MachineTool.remember/restore`: los pokes (`PokesTool`) y el mudo (`SoundTool`) escriben en el
  `WindowState`.
- `KeepsItsPlace`: el catálogo y el historial (`GamesFrame`, `HistoryFrame`) escriben su
  `WindowState`.
- `EmulatorInternalFrame.saveWindowState/restoreWindowState`: la ventana de máquina arma su
  `WindowState`, con campos fijos para lo de las herramientas (`appliedPokes`, `muted`,
  `turboMode`, `paused`, `searchQuery`).
- `RestoredFromASnapshot` (el AY, con `AyFromASnapshot`): el camino viejo de `Snapshots`, que es el
  que usa hoy la sesión empaquetada.

Lo que estaba roto (1, 2, 3 y 5 arreglados en el paso 0):

1. **Al arrancar, ninguna máquina vuelve.** `restoreOpenWindows()` corría dentro del constructor de
   `ZXSpectrumDesktopApp`, antes de que el lanzador registrara la aplicación: NPE tragado, y
   después `config.getSnapshots().clear()` borraba lo guardado.
2. **Archivo > Salir no guarda:** era `System.exit(0)` y no pasaba por `windowClosing`.
3. **Un guardado a mitad de sesión deja `openWindows` apuntando a snapshots que ya se borraron.**
4. **Las máquinas sin `snapshotModel()`** (Pentagon, Scorpion, TC2048, SE, NTSC, +3e y los clones)
   no se pueden escribir en SZX: `Shape.banksOf(null)` revienta, y la ventana no vuelve.
5. **Se escriben y nunca se leen:** `muted`, `snapshotName`, `zOrder`; la ventana principal se
   volvía a centrar.

## El principio

- **Una máquina es un archivo SZX** que cualquier otro emulador puede abrir. Lleva los bloques
  estándar de la máquina y de cada aparato que el estándar conoce. Lo que el estándar no tiene
  lugar para guardar va en un bloque propio, que los demás emuladores saltean.
- **El entorno es un JSON**, el `config.json` de hoy, y lo conoce solo el entorno base.
- **Lo que se guarda no sabe que se guarda.** Un aparato, una herramienta o una ventana tienen su
  API de siempre: lo que son y lo que hacen. No implementan nada para guardarse.
- **Cada mapeo tiene un dueño que conoce las dos puntas.** Los bloques estándar los mapea el dueño
  del SZX, que depende de los aparatos que conoce. Todo lo demás de una máquina lo mapea el módulo
  de la sesión, que conoce el SZX, los aparatos, las herramientas y las ventanas.

## El módulo de la sesión

Un módulo nuevo de oozx-plugins, `tools/session`. Iría en `devices/snapshots`, que ya mapea el AY y
ULAplus al SZX. No va ahí porque `tools/rzx` y `devices/spec256` dependen de `devices/snapshots`, y
el lector de formatos arrastraría las herramientas del escritorio (pokes, controles, aspecto) a cada
reproductor de RZX y a cada prueba.

Sabe:

- **Que existe el SZX y qué es un bloque.** Escribe la máquina con `SnapshotFormat.write(máquina,
  bloques, notas)` agregando sus bloques, y los lee de vuelta con `carried(bytes)` (paso 2). El SZX
  no sabe qué hay adentro ni que el módulo existe.
- **Los ajustes de la máquina y de sus aparatos, sin conocer a cada uno.** Ya están descriptos de
  forma neutral para la ventana de ajustes: `Settings.Configurable` (`ownSettings()` y
  `deviceSettings()`), 20 declarados con `Settings.mirror` (máquina, memoria, sonido, volumen,
  velocidad, cinta, entrada, disquetera, Covox, DivIDE, DivMMC, Interface 1, Melodik, Multiface,
  SpecDrum, µSpeech, ZXATASP, ZXCF, Beta 128, +3). Un aparato nuevo que declara sus ajustes vuelve
  sin tocar el módulo.
- **El aspecto**, también neutral: `ScreenSettings.values()` y `apply(valores)`, todas las perillas
  por nombre.
- **La ventana de la máquina** (lugar, tamaño, pausa, mudo), **las herramientas** (los pokes
  aplicados, por la API de `PokesTool`), **qué aparatos están enchufados** y **cada ventana de
  equipo enganchada** (qué equipo, lado, tamaño, compacta), cada una por su API.
- **El estado de los aparatos sin bloque estándar** (µSource, impresora paralela) y lo que un aparato
  tiene de más que su bloque, uno por uno.

El escritorio no conoce el módulo: lo encuentra por un rol del kit, con dos operaciones. Una escribe
el archivo de una ventana de máquina. La otra, sobre la máquina ya leída de ese archivo, le devuelve
a la ventana lo de alrededor. El sentido es escritorio → módulo → SZX, y no SZX → módulo. Al
guardar, el SZX recibe solo la máquina, que no sabe cuál es su ventana. Al volver, la máquina se lee
antes de que su ventana exista. El único que tiene la máquina y la ventana en el momento justo es el
escritorio.

## Quién guarda qué

| Dónde | Quién | Ejemplos |
|---|---|---|
| Bloques estándar del SZX | `SzxFormat` (`devices/snapshots`), que depende de cada aparato que mapea | CPU, memoria, paginado, borde, AY, ULAplus; después MFCE, IF1 y MDRV, B128 y BDSK, PLSD, OPUS, +3 y DSK, DIDE, DMMC, ZXAT, ZXCF, SIDE, COVX, DRUM, AMXM, ZXPR, JOY, TAPE, SCLD, DOCK, USPE, ROM |
| Bloque propio en el SZX | el módulo de la sesión | la ventana de la máquina; los ajustes de la máquina y de sus aparatos; el aspecto; mudo; pokes aplicados; qué aparatos están enchufados; cada ventana de equipo enganchada; los aparatos sin bloque estándar; lo que un aparato tiene de más |
| JSON del entorno | el entorno base | la ventana principal; los archivos de la sesión, en orden; los recientes; los valores para las máquinas nuevas; las ventanas de escritorio |

## El bloque propio

El SZX admite bloques con un identificador de cuatro letras, que los lectores que no lo conocen
saltean. El módulo usa uno, `OOZX`. Lo que va adentro y cómo se codifica es del módulo: texto
`clave=valor`, que se lee a simple vista y admite claves nuevas sin versiones. El dueño del SZX
recibe bytes con un nombre para escribir y, al leer, devuelve los bloques que no conoce en vez de
descartarlos (`SnapshotFile.carry/carried`, `DeclaredFormat`, `SzxBlocks`). Los formatos sin lugar
para eso (Z80, SNA, SP) avisan y escriben solo la máquina.

## Los archivos

- `~/.oozx/session/<n>.szx`: uno por ventana de máquina abierta. Se puede abrir con Fuse.
- `~/.oozx/config.json`: lo de hoy; `openWindows` pasa a ser la lista de esos archivos, de atrás
  hacia adelante, y desaparece el mapa `snapshots`. Al leer una configuración vieja se migra una
  vez: cada snapshot empaquetado se escribe como archivo.
- `config.json` sigue teniendo los valores por defecto de las máquinas nuevas (aspecto, volumen,
  joystick). Lo de cada máquina abierta va en su SZX y ya no se pisa entre máquinas.

## El orden al volver

1. El entorno lee su JSON: la ventana principal, sin volver a centrarla, y la lista de archivos.
2. Por cada archivo, en orden: lo abre como cualquier archivo (el formato elige el modelo, enchufa
   lo que nombran los bloques estándar y recorre la máquina) y le hace su ventana. Abrirlos de atrás
   hacia adelante deja el orden de las ventanas sin guardarlo aparte.
3. Le pide al módulo lo de alrededor. El módulo lee su bloque, enchufa los aparatos sin bloque
   estándar, pone los ajustes, el aspecto, el mudo y los pokes, ubica la ventana y reabre sus
   ventanas de equipo.
4. Se reabren las ventanas de escritorio.

Al guardar, lo mismo al revés. Se guarda al cerrar por cualquier camino (la ventana o Archivo >
Salir) y no hay un guardado a medias que deje referencias sueltas.

## Decisiones

1. **Quién mapea.** Decidido el 21 de octubre: lo que se guarda no lo sabe; los aparatos con bloque
   estándar los mapea el dueño del SZX, que depende de ellos (como hoy el AY y ULAplus); todo lo
   demás, el módulo de la sesión. Descartados: que cada aparato traiga sus bytes (pone formato en
   los aparatos), que implemente un contrato neutral para guardarse (`Keeps`: igual sabe que lo
   guardan) y que el SZX mapee por nombre sin depender de los aparatos.
2. **El sentido de las llamadas.** Propuesto: escritorio → módulo → SZX, por lo de arriba. A
   confirmar.
3. **Ajustes por máquina o globales.** Recomendado: en la sesión van los de cada máquina; en
   `config.json` quedan los valores para las máquinas nuevas.
4. **Los medios.** Recomendado: si el archivo está igual que cuando se abrió, se guarda su ruta;
   si la máquina escribió en él (un disco formateado, un microdrive), se guarda la imagen adentro,
   como prevén los bloques DSK, BDSK, PDSK, ODSK y MDRV.
5. **Las ventanas de escritorio.** El catálogo y el historial dejan `KeepsItsPlace` y los mapea el
   módulo, pero van al JSON, que el módulo no conoce. Candidato: las secciones de `Configuration`,
   que son objetos y no JSON. A decidir en su paso.

## Los pasos

Uno por commit. Cada uno nace con su prueba en rojo.

0. **Lo que está roto.** **Hecho en el código** (`ZXSpectrumDesktopApp.comeBack()` y
   `keepTheSession()`). La prueba llega con el paso 3, cuando la sesión se pueda probar sin Swing.
1. ~~El contrato `Keeps` y `Kept`.~~ Descartado y sacado.
2. **El SZX lleva bloques ajenos.** **Hecho** (`ASessionIsKeptAsAnSzxTest`, una prueba contra
   libspectrum). Faltan los identificadores de máquina del SZX 6 a 16 (+3e, Pentagon, TC2048,
   TC2068, Scorpion, SE, TS2068, Pentagon 512 y 1024, NTSC, 128Ke) para que toda máquina se pueda
   guardar.
3. **El módulo y la sesión en archivos.** `tools/session` con el rol del kit. El escritorio escribe
   un SZX por ventana de máquina y lista los archivos en `config.json`; al volver los abre como
   cualquier archivo y le pide al módulo lo de alrededor. Lo primero que mapea el módulo: la ventana
   (lugar, tamaño, pausa, mudo) y los ajustes. Migración de los snapshots empaquetados; se van
   `packedState`, el mapa `snapshots`, `snapshotId` y `saveWindowState/restoreWindowState`. Pruebas:
   ida y vuelta de una sesión sin escritorio; migración de un `config.json` viejo.
4. **El aspecto y las herramientas.** `ScreenSettings`; los pokes aplicados. Se van
   `MachineTool.remember/restore` y los campos de herramientas de `WindowState`.
5. **Los aparatos enchufados y sus ventanas.** Qué equipo está enchufado; cada ventana de equipo
   enganchada (`AttachedFrame`: lado, tamaño, compacta; `MachineFrame` dice qué equipo es).
6. **El motor del SZX para los bloques que faltan.** Bloques repetidos (un BDSK por disquetera),
   largos variables, páginas de 8K y 32K, enchufar los aparatos que nombra un bloque. Prueba: los
   fixtures de libspectrum que ya están en `szx-chunks/`.
7. **Cada aparato, uno por commit**, por lo que más se usa: +3 (disco), Beta 128, +D, DISCiPLE,
   Opus, Multiface, Interface 1 y microdrives, Interface 2, DivIDE, DivMMC, ZXATASP, ZXCF, Simple
   IDE, joystick, mouse, Covox, SpecDrum, ZX Printer, cinta (TAPE), Timex (SCLD, DOCK), µSpeech,
   ROM propia. Con bloque estándar, en `SzxFormat`, como el AY; sin él, en el módulo. Cada uno se
   prueba contra libspectrum y con ida y vuelta. Si nadie más usa el camino viejo de `Snapshots`,
   se va con `RestoredFromASnapshot`.
8. **Las ventanas de escritorio.** Según la decisión 5.
9. **"Exactamente igual".** Una prueba que arma varias máquinas con aparatos, medios y ventanas,
   cierra, levanta un escritorio nuevo y compara: el SZX que escribe cada máquina, byte a byte, y el
   lugar, tamaño y estado de cada ventana.

## Lo que no entra

- Guardar varias sesiones con nombre o elegir cuál abrir: hay una sola, la última.
- Rebobinar: es otra cosa, aunque va a usar este mismo SZX.
- Los bloques de aparatos que el entorno no emula (General Sound, Spectranet, LEC).
