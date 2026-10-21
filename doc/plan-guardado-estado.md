# Guardar el entorno y que vuelva exactamente igual

Paso 3 de `plan-version-1.md`. Cerrar OOZX y abrirlo tiene que dejar cada máquina, cada ventana y
cada aparato enchufado exactamente como estaban. Cada máquina se guarda en un SZX con los bloques
estándar de lo que el formato conoce más bloques propios; lo que no es de una máquina va al JSON del
entorno. **Nada de lo que se guarda sabe que se guarda**: ni los aparatos, ni las herramientas, ni
las ventanas tienen algo para guardarse o restaurarse. Cómo va cada cosa a un archivo lo sabe quien
conoce ese archivo.

**Estado (21 de octubre de 2026):** hechos el paso 0 (en el código), lo de llevar bloques del 2, la
mudanza de los mapeos de aparatos al módulo de mapeos (2b), cada máquina como sí misma (2c), la
sesión en archivos (3) y los aparatos enchufados con sus ventanas (5). El contrato del paso 1 (`Keeps` y
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
  los valores por defecto globales); el volumen y la velocidad de cada una; la cinta, los
  discos, cartuchos y tarjetas que tenían adentro.
- Los valores de `Settings.mirror` son globales: al guardar, cada máquina escribe los suyos encima
  y queda el de la última creada.

Lo que sabe que se guarda, y deja de saberlo:

- ~~`MachineTool.remember/restore`~~ y ~~`EmulatorInternalFrame.saveWindowState/restoreWindowState`~~:
  se fueron con el paso 3. Los campos de herramientas de `WindowState` (`appliedPokes`, `muted`,
  `turboMode`, `paused`, `snapshotName`, `zOrder`) y `PokModState` quedan sin uso hasta que se
  publique un `tool-pokes` que ya no los nombre (`plugin-binary-api`).
- `KeepsItsPlace`: el catálogo y el historial (`GamesFrame`, `HistoryFrame`) escriben su
  `WindowState`.
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
- **La ventana de la máquina** (lugar, tamaño, pausa, mudo), **las herramientas** (los pokes
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
| Bloque propio en el SZX | el módulo de mapeos, llamado por el escritorio | la ventana de la máquina; los ajustes de la máquina y de sus aparatos; el aspecto; mudo; pokes aplicados; qué aparatos están enchufados; cada ventana de equipo enganchada; los aparatos sin bloque estándar; lo que un aparato tiene de más |
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
2. Por cada archivo, en orden: lo abre como cualquier archivo y le hace su ventana. El formato elige
   el modelo y, antes de recorrer la máquina, le pide al módulo lo que tiene que estar antes: desde
   su bloque, la máquina exacta y sus ROMs, los ajustes y lo enchufado, con el reset que pide
   enchufar algunos aparatos; y lo que enchufan los bloques estándar. Recién ahí recorre la máquina.
   Abrirlos de atrás hacia adelante deja el orden de las ventanas sin guardarlo aparte.
3. Le pide al módulo lo de alrededor: ubica la ventana, la pausa, el archivo, los pokes y el
   aspecto, y reengancha sus ventanas de equipo, que encuentran su aparato ya enchufado.
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
   módulo lo de alrededor. `SzxSession` (`tools/mappings`) guarda en `OOZX` el lugar y tamaño de
   la ventana, la pausa, el archivo de donde salió la máquina, el procesador, el turbo y todos los
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
4. **El aspecto.** `ScreenSettings`. (Los pokes aplicados y `MachineTool.remember/restore` ya
   quedaron en el paso 3.)
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
   - una ventana de equipo desenganchada no es de ninguna máquina y no se guarda (va con el 8);
   - la ventana de ajustes que engancha su herramienta (`MachineSettingsTool`) no es un `Equipment`
     y no vuelve;
   - lo que cada ventana tiene adentro: la cinta de la casetera, la grabación del reproductor, el
     papel de la impresora, las pestañas del depurador (decisión 4 y paso 7);
   - el lado al que vuelve el botón de enganchar, si no es el que tiene;
   - la memoria del escritorio de qué ventanas ya le ofreció a cada máquina: una del joystick que
     se cerró se vuelve a abrir una vez después de arrancar, si el juego usa el joystick;
   - el escritorio no se probó corriendo.
6. **El motor del SZX para los bloques que faltan.** Bloques repetidos (un BDSK por disquetera),
   largos variables, páginas de 8K y 32K, enchufar los aparatos que nombra un bloque. Prueba: los
   fixtures de libspectrum que ya están en `szx-chunks/`.
7. **Cada aparato, uno por commit**, por lo que más se usa: +3 (disco), Beta 128, +D, DISCiPLE,
   Opus, Multiface, Interface 1 y microdrives, Interface 2, DivIDE, DivMMC, ZXATASP, ZXCF, Simple
   IDE, joystick, mouse, Covox, SpecDrum, ZX Printer, cinta (TAPE), Timex (SCLD, DOCK), µSpeech,
   ROM propia. Cada uno en el módulo de mapeos, como el AY, y se prueba contra libspectrum y con
   ida y vuelta. Si nadie más usa el camino viejo de `Snapshots`,
   se va con `RestoredFromASnapshot`.
8. **Las ventanas de escritorio.** Según la decisión 5.
9. **"Exactamente igual".** Una prueba que arma varias máquinas con aparatos, medios y ventanas,
   cierra, levanta un escritorio nuevo y compara: el SZX que escribe cada máquina, byte a byte, y el
   lugar, tamaño y estado de cada ventana.

## Lo que no entra

- Guardar varias sesiones con nombre o elegir cuál abrir: hay una sola, la última.
- Rebobinar: es otra cosa, aunque va a usar este mismo SZX.
- Los bloques de aparatos que el entorno no emula (General Sound, Spectranet, LEC).
