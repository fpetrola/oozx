# Guardar el entorno y que vuelva exactamente igual

Paso 3 de `plan-version-1.md`. Cerrar OOZX y abrirlo tiene que dejar cada máquina, cada ventana y
cada aparato enchufado exactamente como estaban. Cada máquina se guarda en un SZX con los bloques
estándar de lo que el formato conoce más bloques propios; lo que no es de una máquina va al JSON del
entorno. **Nada de lo que se guarda sabe que se guarda**: ni los aparatos, ni las herramientas, ni
las ventanas tienen algo para guardarse o restaurarse. Cómo va cada cosa a un archivo lo sabe quien
conoce ese archivo.

**Estado (23 de octubre de 2026):** hechos el paso 0 (en el código), lo de llevar bloques del 2, la
mudanza de los mapeos de aparatos al módulo de mapeos (2b), cada máquina como sí misma (2c), la
sesión en archivos (3), el aspecto (4), los aparatos enchufados con sus ventanas (5), el motor del
SZX para los bloques de los aparatos (6), los aparatos que tienen bloque (7, sin los medios ni JOY),
las ventanas de escritorio (8), la prueba de que todo vuelve exactamente igual (9) y los medios
(decisión 4: la cinta, los discos, los microdrives, el dock, los discos IDE y MMC, las ROMs de cada
máquina y el bloque ROM; queda la +3). Decididas las cinco. El contrato del paso 1 (`Keeps` y
`Kept`, que implementaba cada cosa guardada) se descartó y se sacó: un aparato no tiene que saber
que lo guardan. Lo reemplaza el módulo de mapeos.
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
  los valores por defecto globales); el volumen y la velocidad de cada una; ~~la cinta, los
  discos, cartuchos y tarjetas que tenían adentro~~ (hecho con la decisión 4).
- ~~Los valores de `Settings.mirror` son globales: al guardar, cada máquina escribe los suyos
  encima y queda el de la última creada.~~ Hecho con la decisión 3: son los de las máquinas nuevas,
  y cada máquina tiene los suyos.

Lo que sabe que se guarda, y deja de saberlo:

- ~~`MachineTool.remember/restore`~~ y ~~`EmulatorInternalFrame.saveWindowState/restoreWindowState`~~:
  se fueron con el paso 3. Los campos de herramientas de `WindowState` (`appliedPokes`, `muted`,
  `turboMode`, `paused`, `snapshotName`, `zOrder`) y `PokModState` quedan sin uso hasta que se
  publique un `tool-pokes` que ya no los nombre (`plugin-binary-api`).
- ~~`KeepsItsPlace`~~: el catálogo y el historial escribían su `WindowState`. Se fue con el paso 8;
  la interfaz queda deprecada en el kit mientras un tool-games publicado la implemente.
- `RestoredFromASnapshot` (el AY, con `AyFromASnapshot`): el camino viejo de `Snapshots`, que usa
  el historial.

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
- **Todos los mapeos tienen un solo dueño, que conoce las dos puntas:** el módulo de mapeos, que
  conoce los formatos, los aparatos, las herramientas y las ventanas. Los formatos llevan solo la
  máquina base (procesador, memoria, paginado, borde, reloj, teclado) y no conocen ningún aparato.

## El módulo de mapeos

`tools/mappings` (`tool-mappings`), en oozx-plugins. Iría en `devices/snapshots`, que mapeaba el AY
y ULAplus. No va ahí porque `tools/rzx` y `devices/spec256` dependen de `devices/snapshots`, y el
lector de formatos arrastraría los aparatos y las herramientas del escritorio (pokes, controles,
aspecto) a cada reproductor de RZX y a cada prueba.

Sabe:

- **Los bloques y campos de los aparatos en cada formato.** Hoy el AY (en SZX, Z80 y EZX) y ULAplus
  (en SZX, con la regla que la enchufa si el archivo trae PLTT), que salieron de los formatos el 21
  de octubre. Se los da a cada formato por el rol `Mapping` del motor (`libs/formats`): tablas,
  bloques y lo que el archivo enchufa antes de recorrer la máquina. Acá es el formato el que llama,
  porque un aparato es parte de la máquina y ya existe cuando se lee el archivo. Sin el módulo, un
  snapshot se lee sin el estado de lo enchufado.
- **Que existe el SZX y qué es un bloque propio.** Escribe la máquina con `SnapshotFormat.write(máquina,
  bloques, notas)` agregando su bloque, y lo lee de vuelta con `carried(bytes)` (paso 2). El SZX
  no sabe qué hay adentro ni que el módulo existe.
- **Los ajustes de la máquina y de sus aparatos, sin conocer a cada uno.** Ya están descriptos de
  forma neutral para la ventana de ajustes: `Settings.Configurable` (`ownSettings()` y
  `deviceSettings()`), 20 declarados con `Settings.mirror` (máquina, memoria, sonido, volumen,
  velocidad, cinta, entrada, disquetera, Covox, DivIDE, DivMMC, Interface 1, Melodik, Multiface,
  SpecDrum, µSpeech, ZXATASP, ZXCF, Beta 128, +3). Un aparato nuevo que declara sus ajustes vuelve
  sin tocar el módulo.
- **El aspecto**, también neutral: `ScreenSettings.values()` y `apply(valores)`, todas las perillas
  por nombre.
- **La ventana de la máquina** (pausa, mudo; su lugar lo guarda el escritorio), **las herramientas** (los pokes
  aplicados, por la API de `PokesTool`), **qué aparatos están enchufados** y **cada ventana de
  equipo enganchada** (qué equipo, lado, tamaño, compacta), cada una por su API.
- **El estado de los aparatos sin bloque estándar** (µSource, impresora paralela) y lo que un aparato
  tiene de más que su bloque, uno por uno.

Para lo que rodea a una máquina, el escritorio no conoce el módulo: lo encuentra por un rol del kit, con dos operaciones. Una escribe
el archivo de una ventana de máquina. La otra, sobre la máquina ya leída de ese archivo, le devuelve
a la ventana lo de alrededor. El sentido es escritorio → módulo → SZX, y no SZX → módulo. Al
guardar, el SZX recibe solo la máquina, que no sabe cuál es su ventana. Al volver, la máquina se lee
antes de que su ventana exista. El único que tiene la máquina y la ventana en el momento justo es el
escritorio.

## Quién guarda qué

| Dónde | Quién | Ejemplos |
|---|---|---|
| Bloques estándar del SZX, de la máquina base | `SzxFormat` (`devices/snapshots`) | Z80R, SPCR, KEYB, RAMP |
| Bloques estándar del SZX, de los aparatos | el módulo de mapeos, por el rol `Mapping` que el formato consulta | AY, PLTT; después MFCE, IF1 y MDRV, B128 y BDSK, PLSD, OPUS, +3 y DSK, DIDE, DMMC, ZXAT, ZXCF, SIDE, COVX, DRUM, AMXM, ZXPR, JOY, TAPE, SCLD, DOCK, USPE, ROM |
| Bloque propio en el SZX | el módulo de mapeos, llamado por el escritorio | la pausa y el archivo de la máquina; los ajustes de la máquina y de sus aparatos; el aspecto; mudo; pokes aplicados; qué aparatos están enchufados; cada ventana de equipo enganchada; los aparatos sin bloque estándar; lo que un aparato tiene de más |
| JSON del entorno | el entorno base | la ventana principal; los archivos de la sesión, en orden; los recientes; los valores para las máquinas nuevas; dónde está cada ventana, de máquina o de escritorio, y si está maximizada o minimizada; qué es cada ventana de escritorio, como lo dice el módulo |

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
2. Por cada archivo, en orden: lo abre como cualquier archivo y le hace su ventana. El formato elige
   el modelo y, antes de recorrer la máquina, le pide al módulo lo que tiene que estar antes: desde
   su bloque, la máquina exacta y sus ROMs, los ajustes y lo enchufado, con el reset que pide
   enchufar algunos aparatos; y lo que enchufan los bloques estándar. Recién ahí recorre la máquina.
   Abrirlos de atrás hacia adelante deja el orden de las ventanas sin guardarlo aparte.
3. Le pide al módulo lo de alrededor: la pausa, el archivo, los pokes y el aspecto, y reengancha
   sus ventanas de equipo, que encuentran su aparato ya enchufado. Después pone la ventana donde
   estaba, maximizada o minimizada, y las enganchadas la siguen.
4. Se reabren las ventanas de escritorio.

Al guardar, lo mismo al revés. Se guarda al cerrar por cualquier camino (la ventana o Archivo >
Salir) y no hay un guardado a medias que deje referencias sueltas.

## Decisiones

1. **Quién mapea.** Decidido el 21 de octubre: lo que se guarda no lo sabe, y todos los mapeos,
   con bloque estándar o sin él, van en el módulo de mapeos; los formatos no conocen ningún aparato
   y el RZX tampoco. Descartados: que cada aparato traiga sus bytes (pone formato en los aparatos),
   que implemente un contrato neutral para guardarse (`Keeps`: igual sabe que lo guardan), que el
   SZX mapee por nombre sin depender de los aparatos, y que el dueño del SZX dependa de los
   aparatos que conoce (cada lector de snapshots cargaría con todos).
2. **El sentido de las llamadas.** Decidido: para lo que rodea a una máquina, escritorio → módulo →
   SZX; para los aparatos, el formato consulta los mapeos al leer y al escribir.
3. **Ajustes por máquina o globales.** Decidido el 23 de octubre: en la sesión van los de cada
   máquina; en `config.json` quedan los valores para las máquinas nuevas, y los cambia solo la
   ventana de valores por defecto (la de ajustes, desenganchada). **Hecho:**
   - al guardar, las máquinas abiertas ya no escriben sus ajustes en el archivo. Se fueron
     `Configuration.beforeSave`, `notBeforeSave` y `put`, `Settings.letGo` y `from`, y
     `Settings.Part`, que con eso quedaba con un método y una sola clase;
   - un valor por defecto cambiado ya no se les pone a las máquinas abiertas: estaba solo porque
     ellas lo iban a pisar al guardar;
   - un joystick o un teclado que se le pone a una máquina (`Input.Setup.setJoystick1`, `setKeyboard`)
     es todo lo que tiene. Antes se sumaba al que tenía, y una máquina sin joystick volvía con el de
     los valores por defecto. Lo que el archivo no dice lo sigue cubriendo la mezcla de su JSON con
     el del build.

   Pruebas, en rojo antes: `whatAMachineHoldsStaysOutOfTheFile` (una velocidad), `theFileReachesTheSetup`
   (un teclado) y `theDefaultsChangedWhileItIsOpenAreForTheMachinesOpenedAfterIt` (un joystick que
   cambia en los valores por defecto con la máquina abierta). Queda: el lanzador pone tiempo real a
   cada máquina que crea, sin mirar la velocidad por defecto (20000 en el `config.json` del build).
   Antes el primer guardado escribía 100 en el archivo.
4. **Los medios.** Decidido el 23 de octubre: si el archivo está igual que cuando se abrió, se guarda
   su ruta; si la máquina escribió en él (un disco formateado, un microdrive), se guarda la imagen
   adentro, como prevén los bloques DSK, BDSK, PDSK, ODSK y MDRV. Lo que trae el catálogo o sale de un
   zip se descomprime en `~/.oozx/downloads`, una carpeta por cada uno, para que esa ruta dure: antes
   iba a `/tmp`, que se vacía al reiniciar, y dos zips con un archivo del mismo nombre se pisaban
   (oozx `64a5ad886`). **Hecho**, cada uno en `tools/mappings` y en rojo antes:
   - **la cinta** (`TapeMapping`, TAPE; plugins `e74c153`): la máquina nunca la escribe, así que va
     por su ruta, con el bloque en que estaba la casetera. Una casetera vacía no escribe TAPE;
   - **los discos de la Beta, el +D y la Opus** (BDSK, PDSK y ODSK, uno por disquetera, con la misma
     tabla en `Beta128Mapping`; `e4c3491`, `76923ff`): por su ruta o adentro, si está protegido y el
     cilindro donde está el cabezal (`Fdd.headAt`; sin eso el registro de pista y el cabezal no
     coincidían al volver);
   - **los cartuchos de los microdrives** (MDRV en `Interface1Mapping`; `6ecb611`): por su ruta o
     adentro, si el motor andaba y dónde está el cabezal. `Microdrive` es pública, como `Fdd`;
   - **el cartucho del dock** de un Timex (DOCK, `DockMapping`; `ada2a48`): todas sus páginas adentro,
     también las de ROM, como libspectrum, porque SZX no guarda el archivo de un cartucho;
   - **la ventana de la casetera**, enganchada sin cinta propia a una máquina con cinta, toma esa:
     sin eso, la cinta que volvía no se podía tocar, porque el botón de play queda apagado
     (`clippedOnAMachineWithATapeInItsDeckItHasThatTape`; `e5f2e3f`);
   - **los discos IDE y las tarjetas MMC** (DivIDE, ZXATASP, ZXCF, Simple IDE, DivMMC y ZXMMC; `36a72a5`):
     SZX no tiene bloque para ellos y una imagen de disco duro es grande para ir adentro, así que,
     como eligió Fernando el 24 de octubre, el archivo de cada unidad es un ajuste del aparato, como en
     Fuse: `IdeInterface.getDisks/setDisks`, que cada una nombra en su `Settings.mirror`, y `OOZX` lo
     lleva por máquina. Lo escrito y no guardado se pierde al cerrar, como antes;
   - **las ROMs de cada máquina** (oozx `06f6c67cf`, plugins `b68606a`), también elegido el 24:
     `RomFiles` era una sola sección para todas, y elegir el juego español en una 48K (o en una +3,
     que el plugin de Amstrad trae) se lo cambiaba a todas las de su modelo y quedaba en
     `config.json`; `SzxSession.runsOn` hacía lo mismo al volver. Ahora cada máquina recibe sus
     elecciones copiadas de las del archivo (`RomFiles.forOneMachine`), la ventana de valores por
     defecto cambia las del archivo, y `OOZX` guarda en `roms` todas las de la máquina: su juego o
     su archivo, y el firmware de cada placa. Una sesión de antes, con solo el nombre de un juego,
     vuelve una vez con las ROMs de las máquinas nuevas. Elegir la ROM de una placa en su ventana
     también es para esa máquina: las nuevas arrancan con la del archivo;
   - **el bloque ROM** (oozx `f7ee9fbd7`, plugins `e3dad21`), también elegido el 24: lo que tienen
     los chips de ROM de la máquina, solo cuando no es lo que el build le da (otro juego, un archivo
     de la persona, algo escrito encima con ROMs escribibles) y con el largo que libspectrum lee para
     ese modelo. La máquina dice lo que tienen sus chips (`Spectrum.rom`) y lo que tendrían con unas ROMs
     dadas (`romOf`); con una elección que nadie hizo es la del build, y la máquina sigue sin saber de
     archivos. Al leerlo, los bytes van a los chips: la máquina corre lo que corría hasta que se
     prende de nuevo. Escribirlo siempre no servía: libspectrum rechaza el archivo de una 48K NTSC y
     una 128Ke que lo trae. Queda afuera donde el largo no coincide: el Scorpion (carga tres chips y
     SZX lee cuatro), las Pentagon 512 y 1024 (cargan dos y SZX lee cuatro) y los Timex, cuyo EXROM
     no está entre los chips de `Spectrum`.

   El motor sumó `medium` (la cola que es un medio: el nombre de su archivo o la imagen con zlib),
   `writtenWhen`, `each` con `Fixed.each` (un bloque por disquetera, con su número), `Fixed.only`
   (los DOCK del dock y los del EXROM) y `sizedRest` (una cola tan larga como dice el bloque: la ROM).
   La prueba del escritorio entero lleva una cinta en la 48K y
   un disco en la Pentagon (`0770943`).

   No vuelve igual:
   - un disco o un cartucho que vuelve de adentro no tiene el nombre de su archivo: el bloque guarda
     el nombre o la imagen, no los dos;
   - un disco en blanco sin formatear no es todavía una imagen de ningún tipo: la disquetera vuelve
     vacía;
   - una carga a medias: la cinta vuelve parada en el bloque que sonaba, que empieza de nuevo;
   - una cinta que otro emulador puso adentro de TAPE todavía no se lee;
   - `~/.oozx/downloads` crece con cada juego distinto que se abre, y nada lo limpia.

   Queda:
   - **el disco de la +3** (DSK y +3): la +3 no tiene ventana ni otro camino para meterle un disco,
     así que su disquetera vuelve vacía, como estaba.
5. **Las ventanas de escritorio.** Decidido el 22 de octubre: el escritorio sabe de cada ventana
   que no es de una máquina solo que la tiene y dónde está, y no qué plugin la trae. Qué ventana es,
   si está plegada y lo que tiene adentro lo dice el módulo por el rol `Session`, como clave=valor
   que el escritorio guarda en la misma entrada del JSON sin leerlo. Descartado: que el módulo
   mapee también el lugar, en una sección propia (un mapeo por tipo de ventana aunque solo guarde
   dónde está, y el apilado con las máquinas aparte). Desde el paso 9 vale también para las ventanas
   de máquina: su lugar lo guarda el escritorio, y el módulo lo de alrededor de la máquina.

## Los pasos

Uno por commit. Cada uno nace con su prueba en rojo.

0. **Lo que está roto.** **Hecho en el código** (`ZXSpectrumDesktopApp.comeBack()` y
   `keepTheSession()`). La prueba llega con el paso 3, cuando la sesión se pueda probar sin Swing.
1. ~~El contrato `Keeps` y `Kept`.~~ Descartado y sacado.
2. **El SZX lleva bloques ajenos.** **Hecho** (`ASessionIsKeptAsAnSzxTest`, una prueba contra
   libspectrum).
   - 2c. **Cada máquina con número SZX se guarda como sí misma.** **Hecho:** `MachineTypes` tiene
     los once tipos que nombra libspectrum (con sus bancos y su cuadro, nada más: los tiempos son de
     cada máquina), cada modelo del build dice el suyo, el SZX los numera 6 a 16 y el Z80 los lee y
     escribe como libspectrum. `EveryMachineComesBackAsItselfTest`: dieciséis modelos vuelven sobre
     sí mismos con todos sus bancos y libspectrum lee el mismo (el Scorpion se saltea donde no está
     su ROM). La cinta toma el reloj de la máquina, no del tipo. Los clones que ningún formato nombra
     (Inves, Czerweny, Chrome, Chloe, TK90X y TK95) se escriben como el modelo que copian
     (`nearestModel()`), y `OOZX` lleva la máquina exacta y su juego de ROMs, que la regla del SZX
     en el módulo elige antes de recorrer la máquina; unas ROMs que no están quedan anotadas y la
     máquina corre con las suyas. Sin el plugin de una máquina, un archivo cae en la típica de su
     familia (48K, 128K o +3). Probado con lo que hay acá: el bloque que nombra una TC2048 la elige
     y unas ROMs ausentes se anotan; ningún clon ni juego de ROMs alternativo viene con el build,
     así que el cambio de juego no se probó.
   - 2b. **La mudanza.** **Hecho:** el rol `Mapping` en el motor y `tools/mappings` con el AY y
     ULAplus. La red sobre la máquina se mudó con ellos y sus 62 goldens quedaron con las mismas
     líneas (solo se ordenaron las de las partes sin descripción, que seguían el orden del classpath).
3. **La sesión en archivos.** **Hecho:** el rol `Session` en el kit; el escritorio escribe un
   `~/.oozx/session/<n>.szx` por ventana de máquina, de atrás hacia adelante, borra los que
   sobran y los nombra en `openWindows`; al volver los abre como cualquier archivo y le pide al
   módulo lo de alrededor. `SzxSession` (`tools/mappings`) guarda en `OOZX` la pausa (el lugar y
   tamaño de la ventana también, hasta el paso 9), el archivo de donde salió la máquina, el procesador, el turbo y todos los
   ajustes declarados (`Settings.devices()`: volumen, mudo, joystick, cada aparato). Los ajustes se
   ponen antes de recorrer la máquina, con el paso 5: un Melodik que pone un ajuste tiene que estar
   cuando se lee el bloque AY.
   Pruebas: ida y vuelta sin escritorio y libspectrum leyendo el archivo
   (`AMachineComesBackWithItsWindowTest`). Una configuración vieja vuelve una vez por el camino de
   antes y al salir queda en archivos. `putMachinesAway` (al cambiar de plugins) pasa por la misma
   sesión, en una carpeta temporal; la máquina que nadie puede guardar queda como está. Los pokes
   aplicados van en `OOZX` por la API de `PokesTool`, y el botón de mudo y su volumen leen la
   máquina cada vez que se muestran, sin que nadie les avise (`TheMuteButtonSaysWhatTheMachineDoesTest`).
   Pendiente:
   - el escritorio no se probó corriendo: arma máquinas con sonido y ventanas sobre la pantalla;
   - el historial sigue con los snapshots empaquetados;
   - sin el plugin de mapeos las máquinas no vuelven, y nada lo pide todavía.
4. **El aspecto.** **Hecho.** `OOZX` lleva cada perilla de la pantalla de la ventana
   (`screen.<perilla>`, de `ScreenSettings.values()`, también las de un efecto que vino en un
   plugin) y el zoom (`window.zoom`); `bringBack` se los pone a su `SpeccyScreen` con `apply`, que
   saltea lo que no conoce. El escritorio, al cambiar un valor por defecto (el botón del borde, la
   entrada y las líneas del menú de TV), les ponía a todas las ventanas abiertas todos los valores
   por defecto y les borraba el aspecto propio; ahora les pone solo el que cambió. Prueba, en rojo
   antes y en verde después: `itsPictureIsShownAsItWas`. Pendiente:
   - el escritorio no se probó corriendo;
   - el botón del borde de una ventana también lo cambia en las otras abiertas, sin darles ni
     sacarles el lugar (ya era así);
   - las perillas de un efecto que vino en un plugin son las mismas para todas las ventanas (ya era
     así): vuelven como las tenía la última ventana que se abre.
5. **Los aparatos enchufados y sus ventanas.** **Hecho.** `OOZX` nombra cada aparato enchufado
   (`plugged.N`, los `Pluggable` que dicen estarlo, por `PeripheralRegistry.allThatAre`) y cada
   ventana de equipo enganchada (`equipment.N.*`: qué equipo, lado, tamaño, plegada o no y a qué
   altura se despliega). Lo enchufado y los ajustes se ponen antes de recorrer la máquina
   (`SzxSession.plug`, que llama la regla del SZX), con el reset que pide enchufar algunos aparatos
   (SpecDrum, Multiface, Interface 1, DivIDE…). Enchufarlos después de leer reseteaba la máquina
   recién leída, y un aparato que pone un ajuste (Melodik) no estaba cuando se leía su bloque. Las
   ventanas vuelven en `bringBack` por dos métodos de `Desk`, `equipmentOn(ventana)` y
   `equipment(nombre)`, en el orden en que estaban a lo largo de cada lado; al engancharse
   encuentran su aparato ya enchufado y no resetean. La ventana del joystick, que se abre sola
   cuando la máquina lo usa, ya no corre a una que volvió. Pruebas, en rojo antes y en verde
   después: `whatIsPluggedIntoItIsInBeforeItIsRead` y `theEquipmentClippedOnItComesBackAsItWasLeft`.
   Pendiente:
   - ~~una ventana de equipo desenganchada no es de ninguna máquina y no se guarda~~: hecho en el 8;
   - la ventana de ajustes que engancha su herramienta (`MachineSettingsTool`) no es un `Equipment`
     y no vuelve;
   - lo que cada ventana tiene adentro: la cinta de la casetera, la grabación del reproductor, el
     papel de la impresora, las pestañas del depurador (decisión 4 y paso 7);
   - el lado al que vuelve el botón de enganchar, si no es el que tiene;
   - la memoria del escritorio de qué ventanas ya le ofreció a cada máquina: una del joystick que
     se cerró se vuelve a abrir una vez después de arrancar, si el juego usa el joystick;
   - el escritorio no se probó corriendo.
6. **El motor del SZX para los bloques que faltan.** **Hecho.** Lo que un mapeo puede declarar
   ahora:
   - `Fixed.pages(id, largo)`: un bloque que viene una vez por página, como RAMP (ATRP y CFRP de
     16K, DIRP y DMRP de 8K). Las páginas quedan en el archivo por bloque y número
     (`SnapshotFile.pages(bloque)`). `Layout.pages(bloque, cuántas, campo)` las lleva al aparato y
     las trae. El SZX las lee y escribe con el mismo código que RAMP.
   - `Layout.rest(bloque, byte, bit, campo)`: lo que un bloque trae después de sus campos (una ROM,
     la RAM de un aparato), tan largo como lo que tiene el aparato, con zlib cuando ese bit lo dice.
     `Bytes.append` deja crecer el bloque al escribir. `packedRest` es lo mismo siempre comprimido,
     con el largo comprimido en el bloque (IF2R).
   - `Layout.present(bloque)`: un bloque que solo dice que el aparato está (SIDE, ZMMC).
   - Enchufar: cada mapeo enchufa lo suyo en `plug`, y el formato prende lo pedido y hace el reset que
     piden algunos una sola vez, después de todos, antes de recorrer la máquina. `SzxSession` y la
     regla de ULAplus ya no lo hacen cada uno.

   Prueba: `WhatIsPluggedInIsKeptInItsBlocksTest` (`devices/snapshots`), sobre aparatos inventados
   para la prueba. Lee los fixtures de libspectrum (ATRP comprimido y sin comprimir, DIDE, DIRP,
   MFCE, IF2R, SIDE) como los lee la prueba de libspectrum. Después libspectrum lee lo que escribe:
   32 páginas de 16K, la EPROM y 4 páginas de 8K, la RAM de una Multiface, un cartucho, un bloque
   vacío y 32K de ROM de una 128K.

   La tabla de cada bloque según libspectrum, con sus desplazamientos, quedó en
   `~/detodo/spectrum/investigacion/research_notes/szx-bloques-libspectrum.md`.

   Pendiente para el paso 7 o la decisión 4:
   - ~~un bloque por disquetera (BDSK, DSK, PDSK, ODSK, MDRV) y TAPE: libspectrum los saltea y su forma
     va con los medios~~: hecho con la decisión 4, salvo DSK; su forma, de la especificación de
     Spectaculator, quedó en la nota de los bloques;
   - ~~DOCK guarda en el mismo bloque las páginas del cartucho y las del EXROM, y si cada una es RAM:
     `pages` las separa solo por número~~: hecho con la decisión 4 (`Fixed.only`);
   - OPUS y PLSD llevan dos memorias seguidas, cada una con su largo en la cabecera. Resuelto en el
     paso 7: `rest` puede tomar el largo que dice la cabecera, y la ROM propia, que va después de la
     RAM, no se escribe;
   - un archivo sin el bloque de un aparato no lo desenchufa, como sí hace Fuse;
   - la prueba se escribió después del motor: sin él no compila, porque usa sus métodos nuevos.
7. **Cada aparato, uno por commit**, por lo que más se usa: +3 (disco), Beta 128, +D, DISCiPLE,
   Opus, Multiface, Interface 1 y microdrives, Interface 2, DivIDE, DivMMC, ZXATASP, ZXCF, Simple
   IDE, joystick, mouse, Covox, SpecDrum, ZX Printer, cinta (TAPE), Timex (SCLD, DOCK), µSpeech,
   ROM propia. Cada uno en el módulo de mapeos, como el AY, y se prueba contra libspectrum y con
   ida y vuelta. Si nadie más usa el camino viejo de `Snapshots`,
   se va con `RestoredFromASnapshot`. libspectrum no tiene bloque para el DISCiPLE ni el µSource.
   PLTT tiene un byte más en libspectrum (el registro ff), que el mapeo de ULAplus no escribe.

   **Cómo va cada uno (22 de octubre):**
   - Cada aparato tiene su `Mapping`, una clase propia de `tools/mappings` marcada `@OnlyWith` con su
     plugin y los plugins de los que este depende.
   - Crystal (`only-with-1`, que el CI de los dos repos compila por `.github/workflows/maven.yml`)
     escribe esas dependencias como opcionales. `tool-mappings` anda sin ese aparato y deja afuera
     su mapeo; instalarlo no instala el aparato. Si el aparato se instala o se saca con el emulador
     andando, recarga `tool-mappings`.
   - Antes de esto, `tool-mappings` dependía de cada aparato que nombraba, y si faltaba uno se
     rechazaba entero.
   - Se publica primero crystal (`main` y el tag), después oozx y después los plugins.
   - El aparato no cambia: le agrego lo que su dominio no tenía para leerlo y ponerlo, nunca algo para
     guardarse.
   - La prueba de cada uno va en `WhatIsPluggedInComesBackTest`:
     - libspectrum lee lo que escribe una máquina con el aparato;
     - otra máquina lo lee y lo tiene enchufado como estaba;
     - el archivo de libspectrum del bloque da lo que dicen sus pruebas.

   Hechos (en `oozx-plugins`, un commit cada uno):
   - **ZXATASP** (`e2f2b98`): ZXAT (jumpers, el 8255 y el banco paginado, `0xff` con la memoria
     apagada) y un ATRP por banco.
   - **DivIDE** (`eef97c1`) y **DivMMC** (`befc0df`): DIDE y DMMC (el jumper de la EPROM, si está
     paginada, su registro y la EPROM) y un DIRP o DMRP por página de RAM.
   - **Multiface** (`bfdbd5a`): MFCE como la guarda Fuse: One o posterior, paginada, J2, el botón y
     la RAM.
   - **Interface 2** (`32d931b`): IF2R con el cartucho paginado.
   - **Simple IDE** (`2ab6d62`) y **ZXMMC** (`1ce484a`): solo dicen que están.
   - **ZXCF** (`80d3b92`): el jumper, el registro de memoria y un CFRP por banco.
   - **Covox** (`75ebd87`) y **SpecDrum** (`f9410b8`): el último byte que se les escribió.
   - **µSpeech** (`ab06686`): USPE, con su ROM adentro o afuera.
   - **ZX Printer** (`10a61e7`): ZXPR prendida la enchufa. libspectrum escribe ZXPR apagado para
     cualquier máquina, y eso no enchufa nada. El `random.szx` de Fuse ahora la enchufa.
   - **Mouse Kempston** (`e3aef1a`): AMXM de tipo 2; dónde está y sus botones son de la mano.
   - **Interface 1** (`8e341ad`): IF1 prendido, paginado o no, con ocho microdrives.
   - **Beta 128** (`c3886ad`): B128, la de la Pentagon o la enchufada en una 48K.
   - **SCLD** (`f67b535`): lo último escrito a los puertos 0xf4 y 0xff de un Timex.
   - **+D** (`e404002`) y **Opus** (`53c31e3`): PLSD y OPUS con los registros del controlador, la
     PIA de la Opus y la RAM.
   - **Melodik** (`73734da`) y **Fuller Box** (`78c06c6`): un bit del bloque AY en el SZX, y en el Z80
     el byte 37 (0x04 la Melodik, 0x44 la Fuller), como libspectrum. La parte del Z80 va anidada
     en el mismo mapeo, porque un `Mapping` responde a un solo formato.

   Cada uno, salvo el SCLD que va a bordo: un archivo con su bloque lo enchufa. La ROM propia de un
   aparato no se escribe; sus medios (discos, cartuchos de microdrive) vuelven desde la decisión 4.

   Queda:
   - **JOY.** La interfaz Kempston no se enchufa a una máquina: la prende para todas un ajuste
     global (`Input.Setup.kempstonJoystick`). Para que JOY la diga por máquina tiene que ser
     `Pluggable`, como el mouse. Los demás tipos de JOY (Sinclair, cursor) son teclas, de la mano.
     A decidir.
   - ~~**Los medios** (decisión 4): TAPE, DSK, BDSK, PDSK, ODSK, MDRV, DOCK y ROM~~: hechos salvo DSK y
     ROM, que siguen en la decisión 4.
   - **El Z80 también puede decir el IF1, el +D y el DISCiPLE** (el byte 34 de hardware, si está
     paginado, su memoria como página 1). libspectrum los lee; el Z80 de acá todavía no.
   - **Tablas repetidas.** Las de la DivIDE y la DivMMC, y los registros del controlador de disco de
     la Beta, el +D y la Opus, se repiten en cada mapeo. No pueden compartir una clase de apoyo:
     `@OnlyWith` solo vale en extensiones. Si crystal lo aceptara en clases de apoyo, el AY y
     ULAplus también podrían ser opcionales.
   - **Sin probar con ROMs reales** (las pruebas no las tienen): la paginación del IF1, la Beta y el
     +D. Además:
     - la Multiface en modo 16K se rechaza;
     - el xfdd de la Multiface 3 no se guarda;
     - el autoboot de la Beta se escribe y no se lee.
   - **El camino viejo de `Snapshots`** (`SpectrumState`) todavía lo usan el lector de RZX, MiniZX,
     las sesiones viejas empaquetadas y varias pruebas. `RestoredFromASnapshot` se queda hasta
     entonces.
   - La libspectrum instalada (8.9.0) no tiene USPE, así que esa comparación se saltea.

   Siguen siendo obligatorios para `tool-mappings` el AY, ULAplus, la memoria de 128K, los
   snapshots y los pokes. Los mapeos de los primeros cuatro están repartidos entre `SzxMapping`, los
   del Z80 y el EZX, y `AyFields`. Para hacerlos opcionales hay que separarlos como al ZXATASP.
8. **Las ventanas de escritorio.** **Hecho**, según la decisión 5:
   - **El escritorio** anota en `openWindows`, de atrás hacia adelante junto con las máquinas, cada
     otra ventana que el módulo reconoce: su lugar, su tamaño y `inside`, lo que el módulo dijo de
     ella. Al volver le pide la ventana al módulo con eso, la pone donde estaba y la trae adelante.
     No tiene un caso por tipo de ventana.
   - **El kit:** `Session.keep(ventana)` y `bringBack(lo guardado)`; `Desk.nameOf(ventana)` y
     `window(nombre)`, el nombre con que el escritorio abrió una ventana y otra abierta igual: la de
     plugins, las del escritorio (catálogo, historial, favoritos, ajustes) y las de equipo
     desenganchadas.
   - **El módulo:** `SzxSession` guarda ese nombre, si está plegada y a qué altura se despliega, y le
     pregunta a cada `WindowInside` (un rol del módulo) lo que la ventana tiene adentro. La que va
     enganchada a una máquina va con la máquina. `GamesWindowMapping` (`@OnlyWith` tool-games y
     tool-catalogue) guarda la búsqueda del catálogo: las palabras, dónde, el orden, la máquina, el
     género, el año y los cinco filtros. Al volver es una sola búsqueda.
   - **El catálogo:** `GamesFrame.Search`, que `search()` lee de los controles y `search(Search)`
     pone. `performSearch` lee por ahí. Una máquina o un género que las listas no trajeron todavía
     entra en ellas.
   - **Se fue:** el uso de `KeepsItsPlace` (deprecado mientras un tool-games publicado lo
     implemente), `DeskEquipment.keeps()` y sus cuatro versiones, lo que guardaban el catálogo y el
     historial, `setSearchQuery` y `deskKindThatKeeps`.

   Prueba: `theOtherWindowsOfTheDeskComeBackAsTheyWereLeft` (`AMachineComesBackWithItsWindowTest`).
   Una ventana de equipo vuelve plegada como estaba y el catálogo buscando lo mismo; la enganchada a
   una máquina y una que el escritorio no abrió no se guardan. En rojo sin los métodos de la sesión
   (no guarda ninguna) y sin `GamesWindowMapping` (la búsqueda vuelve vacía).
   Pendiente:
   - el escritorio no se probó corriendo;
   - las ventanas de escritorio de una configuración vieja (el catálogo con su búsqueda) no vuelven
     esa vez: no tienen `inside`;
   - ~~minimizada o maximizada no se guarda~~: hecho en el 9;
   - lo de adentro de las demás: la página de los ajustes, lo tildado en la de plugins, la cinta de una
     casetera suelta (la enganchada toma la de su máquina desde la decisión 4).
9. **"Exactamente igual".** Una prueba que arma varias máquinas con aparatos, medios y ventanas,
   cierra, levanta un escritorio nuevo y compara: el SZX que escribe cada máquina, byte a byte, y el
   lugar, tamaño y estado de cada ventana. **Hecho, sin los medios:**
   - **La prueba**, `theDeskComesBackExactlyAsItWasLeft` (`AMachineComesBackWithItsWindowTest`), arma
     en un escritorio:
     - una 128K pausada, más baja y muda, con su aspecto, zoom 3, un poke aplicado y la SpecDrum y la
       impresora enganchadas abajo;
     - una 48K con la Melodik, un joystick Sinclair, la Interface 2 con un cartucho y el mouse
       enganchado a la derecha;
     - una Pentagon maximizada, con su Beta 128 y una Covox;
     - el catálogo minimizado, buscando algo, y una impresora suelta.

     Lo guarda como lo guarda el escritorio, lo abre en otro y compara cada ventana de atrás hacia
     adelante: qué es, dónde está, su tamaño, si está maximizada o minimizada, cómo se pliega y a cuál
     está enganchada. Guardado otra vez, compara cada archivo bloque por bloque y byte a byte.
   - **El escritorio guarda y reabre con métodos de `Desk`** (en el kit): `keep(sesión, ventanas,
     carpeta)` y `bringBack(sesión, lo guardado)`, y `machineFrom(archivo)`, que implementa cada
     escritorio. Salieron de `ZXSpectrumDesktopApp`, que ninguna prueba puede armar: así la prueba corre
     lo mismo que el escritorio y no una copia. `putMachinesAway` usa lo mismo.
   - **El lugar de toda ventana es del escritorio**, también el de las de máquina: su tamaño normal y si
     está maximizada o minimizada (`WindowState.maximum` e `icon`). `OOZX` ya no lo lleva. Es la
     decisión 5 llevada a las máquinas.
   - **Lo que encontró la prueba:**
     - el joystick y el teclado (`input.joystick1`, `joystick2`, `keyboard`) se escribían como
       `Pad@2e913e78` y no volvían. Ahora cada ajuste se escribe como lo escribe `config.json`
       (`Configuration.asText` y `fromText`: un número, una palabra o un nombre tal cual, y lo demás
       en JSON), y se fue la conversión a mano de `SzxSession`;
     - un archivo con B128 enchufaba la Beta de a bordo de una Pentagon, y con el plugin de la
       enchufable le ponía una segunda en los mismos puertos. Ahora la enchufa solo donde no viene de
       fábrica, como ya decía su javadoc;
     - la Pentagon maximizada volvía del tamaño del escritorio sin estar maximizada, y el catálogo
       minimizado volvía abierto.
   - En rojo antes de cada arreglo. Con `fromText` sin JSON, el joystick vuelve sin salida.

   Pendiente:
   - ~~los medios (decisión 4): la cinta, los discos, los microdrives, el DOCK y la ROM propia. De medios
     la prueba tiene solo el cartucho de la Interface 2, que va en su bloque~~: la prueba lleva ahora
     también una cinta y un disco; la ROM propia sigue en la decisión 4;
   - el escritorio no se probó corriendo;
   - qué ventana está elegida no se ve sin pantalla;
   - una ventana enganchada vuelve justo delante de su máquina, aunque antes hubiera otra entre las
     dos;
   - ~~al cerrar, cada máquina escribe sus ajustes en `config.json` como valores para las máquinas
     nuevas y queda el de la última; con eso, una máquina puede volver con un joystick de otra~~:
     hecho con la decisión 3;
   - una sesión guardada antes de este paso vuelve una vez con cada ventana de máquina donde la pone
     el escritorio: su lugar estaba en `OOZX`.

## Lo que no entra

- Guardar varias sesiones con nombre o elegir cuál abrir: hay una sola, la última.
- Rebobinar: es otra cosa, aunque va a usar este mismo SZX.
- Los bloques de aparatos que el entorno no emula (General Sound, Spectranet, LEC).
