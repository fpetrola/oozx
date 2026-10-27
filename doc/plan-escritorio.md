# El escritorio se comporta igual en todos lados

Lo que quedó feo en el entorno de emulación después de los pasos de `plan-guardado-estado.md`:
ventanas enganchadas que se apilan mal, un botón de cerrar que no agarra, la casetera que no
aparece, y una serie de inconsistencias entre ventanas que hacen lo mismo de maneras distintas.
**Cada concepto del escritorio tiene un solo dueño**: apilarse con la máquina, cerrar, desenganchar,
refrescar, cuál es la máquina en uso. Hoy cada uno está repartido entre el kit, el escritorio y
algunos hijos, y lo que falta en un hijo es lo que se ve como bug.

**Estado (26 de octubre de 2026):** relevado, nada hecho. Relevado sobre `traduccion` (oozx,
`82be36478`) y `prueba-apuntar` (oozx-plugins, `bc0a263`), con la configuración y los plugins
instalados de Fernando (19 en `installed.tsv`).

## Cómo está hoy

Las ventanas del escritorio son de cuatro clases:

- `AttachedFrame` (`machine/ui`): se engancha a una ventana de máquina, la sigue, se pliega, se
  arrastra de una máquina a otra. Hijos directos: `SettingsInternalFrame` y `MachineFrame`.
- `MachineFrame` (kit): un `AttachedFrame` que sabe qué `Speccy` tiene enganchado. Hijos: la
  casetera, el player RZX, la impresora y los 9 `DeviceFrame` de los aparatos (27 ventanas en total).
- `JInternalFrame` directo: el navegador de juegos, favoritos, historial, la ventana de plugins y los
  ajustes de pantalla. Cinco, cada una con su manera de abrirse y subir.
- `EmulatorInternalFrame`: la máquina.

Lo que está roto o repartido:

1. **Apilado.** `AttachedFrame.keepWithMachine` pone la enganchada justo delante de su máquina, pero
   solo corre desde `place()`, o sea cuando la máquina se mueve o cambia de tamaño. Activar la
   máquina (clickearla) la trae al frente y la enganchada queda abajo de lo que hubiera encima.
   `RzxFrame` arregla la mitad por su cuenta con un `internalFrameActivated` (player activado sube la
   máquina y después a sí mismo); la casetera y los `DeviceFrame` no tienen nada. La prueba
   `attached_it_rides_directly_in_front_of_its_machine` cubre solo `place()`.
2. **El botón de cerrar del player.** Ese listener reordena las dos ventanas con `toFront()` en el
   mismo click que activa la ventana; `toFront` saca y vuelve a poner el componente, y el release
   puede no llegar al botón. Sospecha por lectura, no reproducida.
3. **La casetera no aparece.** `installed.tsv` tiene `device-tape` y no `tool-cassette`. Con
   `device-tape` la máquina abre el `.tap` sola (`OOSpectrumLauncher.somethingOpens` dice que sí),
   así que `load()` no ofrece ningún plugin, y `cassetteFor` no encuentra un `Equipment` que abra
   cintas. El escritorio ofrece lo que falta solo cuando nadie abre el archivo (`nothingOpens`) o
   cuando hay cosas al lado del juego (`BesideTheGame`); nunca un `Equipment` para un archivo que la
   máquina ya abre.
4. **Dos máquinas activas.** `getActiveEmulator()` (la seleccionada, si no la primera visible) es la
   de las herramientas, los atajos del menú Emulator y el botón `+`. `machineBeingUsed()` (la
   seleccionada, o la máquina de la enganchada seleccionada) es la de las teclas y del equipo. Con el
   player seleccionado, un atajo de herramienta cae en la primera máquina visible.
5. **Teclas dobles.** `typeIntoTheMachineInFront` manda la tecla a la máquina y devuelve false, así
   que también llega al componente con foco: Espacio con un botón del player enfocado hace las dos
   cosas.
6. **Cerrar significa tres cosas.** Casetera: la cinta sigue sonando. Player: `dispose()` de la
   máquina. `DeviceFrame`: desenchufa el aparato y, si cambia el mapa de memoria, resetea la máquina.
   Desenganchar también difiere: la casetera para la cinta (`deck.stop()`), el aparato se desenchufa
   con el mismo reset, el player para la grabación.
7. **Cascade y Tile** (`cascadeWindows`, `tileWindows`) mueven a las enganchadas como si fueran
   sueltas. Cualquier movimiento por código (`takeMachine`, `bringBack`, esos dos) difiere de
   `placedAt`, pone `dragging=true` e instala el listener AWT de `MOUSE_RELEASED`, que queda armado
   hasta el próximo click en cualquier lado, y recién ahí `snapIfNear` decide.
8. **Ajustes.** `MachineSettingsTool` abre una `SettingsInternalFrame` nueva en cada click, sin
   reusar como hace `show(Equipment)`. `ScreenSettingsInternalFrame` es un `JInternalFrame` pelado:
   no sigue a la máquina, no se guarda, no es `Equipment`.
9. **Al volver la sesión.** Una enganchada vuelve justo delante de su máquina aunque hubiera otra
   entre las dos; `WindowState.zOrder` existe y nadie lo escribe ni lo lee. Dos máquinas guardadas
   con y negativa (-26 y -4 en `config.json`) vuelven con la barra de título fuera del escritorio.
10. **Equipo que se abre solo.** El `Timer` de un segundo `openWhatTheMachineUses` abre la ventana
    del aparato que la máquina empieza a usar, a un tercio del ancho y a la derecha, sin recordar el
    tamaño que se le dejó.
11. **Treinta timers.** Cada ventana enganchada tiene su `Timer` de refresco con su `REFRESH_MILLIS`
    (de 10 a 1000 ms) y su `stop()` en `internalFrameClosed`. Todos paran; es el mismo idioma copiado.
12. **Muerto.** `stateCapture` en `createNewEmulator` nunca arranca. `WindowState` lleva nueve campos
    que la sesión no usa (`filePath`, `snapshotName`, `searchQuery`, `turboMode`, `muted`, `paused`,
    `snapshotId`, `appliedPokes`, `zOrder`); `plugin-binary-api` dice por qué siguen. El historial
    sigue con snapshots empaquetados en `config.json` (`openSaved`, `getSnapshot`).

13. **Una grabación en curso no sigue al volver.** Encontrado por Fernando el 26 de octubre. La
    sesión guarda de una ventana enganchada solo `equipment.N.kind/side/width/height` y el plegado
    (`SzxSession.keep(Map<MachineFrame,String>)`); `WindowInside` se consulta solo para las ventanas
    sueltas del escritorio, y de RZX no hay mapeo en `tools/mappings`. Al volver, la máquina sale de
    su SZX como una máquina común y el player vuelve enganchado y vacío: la grabación no sigue.
    `RzxPlayback.getFrameIndex()` sabe el cuadro; `RzxSession` solo tiene `rewind()`. Y la sesión
    escribe cada máquina sin pararla (`saveOpenWindows` → `DeclaredFormat.write`, sin pausa ni
    espera al fin de cuadro): un SZX de a mitad de cuadro no coincide con ningún cuadro de la
    grabación, así que guardar el cuadro no alcanza. La prueba del punto fijo pasa porque su
    máquina está pausada.

14. **La máquina de una grabación no sabe qué juego es.** Encontrado por Fernando el 26 de octubre:
    pokes y detalles dicen "No game loaded". La cadena: el navegador conoce el juego (`result` tiene
    id y título) y llama `Desk.play(url, label)`, que no lleva el juego; `Desk.show(machine, title,
    asking)` arma `SpeccyEmulatorCore(machine)` sin archivo y sin `Desk.Game`; `PokesTool` pregunta
    por `control.getFilename()` más `gameIn(file)`, `GameDetailsTool` por `window.game()` y si no por
    el archivo: dos maneras de saber el juego (la cuarta vez que un concepto tiene dos dueños en este
    plan), y a esta máquina no le llega ninguna. Encima un `.rzx` no se identifica ni por archivo:
    ningún `SnapshotFormat` lo lee, `SnapshotFactory.payloadOf` toma los bytes crudos y la huella no
    es la de ninguna RAM, aunque la grabación trae su snapshot (`RzxFile.getSnapshotBlock`, que
    `RzxSession.open` ya extrae). El escritorio ya sabe que "el juego de una máquina manejada por una
    grabación es el de la grabación": `keepAsFavorite` lo resuelve con `playingInto`, solo para
    favoritos.

Pendientes de `plan-guardado-estado.md` que son de esto mismo: el lado al que vuelve el botón de
enganchar si no es el que tiene; la línea de progreso de la casetera no vuelve; las ventanas de
escritorio de una configuración vieja no vuelven; el `+` del escritorio espera la lista de la red; el
cohete de `TurboTool` queda gris; Load State es un stub de `MockEmulatorCore`.

## El principio

- **Lo que una ventana enganchada hace por estar enganchada va en `AttachedFrame`.** Apilarse con la
  máquina, seguirla, plegarse, refrescarse, qué pasa al cerrar y al desenganchar. Un hijo no tiene
  listeners de ventana propios para esto; si los necesita, es que al padre le falta algo.
- **Una sola máquina en uso**, y es la de adelante o la de lo que está adelante. Todo lo que hoy
  pregunta "cuál máquina" pregunta eso.
- **Un movimiento por código no es un arrastre.** Lo que decide enganchar o soltar es dónde se
  SOLTÓ el mouse, y solo eso.
- **El escritorio no inventa ventanas.** La casetera sigue siendo del plugin; lo que hace el
  escritorio es ofrecerlo cuando lo que se abre lo pide, por el camino que ya ofrece lo que falta.
- **Prueba en rojo primero, una por paso**, sobre el doble de escritorio de las pruebas
  (`AMachineComesBackWithItsWindowTest`, `AttachedFrameTest`, `RzxPlayerDockingTest`), en el EDT.
  Lo que no se puede probar sin pantalla (el botón de cerrar) se reproduce con el Robot en Xephyr
  antes y después (receta en `running-the-real-desk`).

## Decisiones

**Decisión 1. Qué pasa al cerrar y al desenganchar** (hoy, 6). Opciones:

- (a) **Cerrar es cerrar la ventana, desenganchar es desenchufar.** La casetera cerrada deja la
  cinta en la máquina (como hoy); el player cerrado deja la máquina corriendo donde quedó la
  grabación (hoy la cierra, "ventana que nadie puede parar": ya no, la máquina tiene sus controles);
  el aparato cerrado sigue enchufado y su ventana se puede volver a abrir por Equipment (hoy se
  desenchufa). Desenganchar desenchufa en los tres: la cinta para, la grabación para, el aparato sale.
- (b) **Cerrar es sacar la cosa.** Como hoy en el aparato, extendido a la casetera (eyecta) y al
  player (para y suelta la máquina, sin cerrarla).
- (c) Dejar los tres como están y documentarlo.

Recomendada: **(a)**. Es la única en que la ventana es una vista y el enchufe es el gesto de
enganchar, que es lo que dice el javadoc de `AttachedFrame` ("being clipped on is what plugs it
in"). El reset de un aparato con ROM al desenchufarlo queda, porque es el hardware; lo que cambia
es que cerrar la ventana no lo provoca.

**Decisión 2. Cuántas ventanas de ajustes.** Hoy una nueva por click (8). Opciones: una por
máquina, reusada como el equipo (`show(kind)` ya lo hace por nombre); o una sola en el escritorio
que cambia de máquina al enganchar (`SettingsInternalFrame` ya sigue a `coreOf`). Recomendada:
**una por máquina como `Equipment`**, porque así vuelve con la sesión por el mismo camino que la
casetera (pendiente 5 del otro plan) y la de pantalla se pliega y se engancha igual.

**Decisión 3. La ventana del aparato que se abre sola** (10). Opciones: seguir abriéndola al primer
uso; abrirla solo si alguna vez se abrió para esa máquina; no abrirla y mostrar un indicador en la
barra de estado. Recomendada: **seguir como hoy pero recordando el tamaño y el lado por kind**
(`equipmentOn` ya los conoce), que es lo barato y no cambia el comportamiento que se eligió.

**Decisión 4. Cómo sigue una grabación al volver** (13). Opciones:

- (a) **Desde el cuadro guardado, con el SZX de la máquina.** La sesión guarda la grabación (URL,
  entrada y archivo local, como `cameFrom/entryInside`) y el cuadro; al volver, el player abre la
  grabación, pone el `RzxSession` sobre la máquina que ya volvió del SZX y arranca la reproducción
  en ese cuadro. Exacto solo si el SZX se tomó entre dos cuadros, lo que pide el paso 0.
- (b) **Desde el principio, corriendo hasta el cuadro en silencio.** La sesión guarda grabación y
  cuadro; al volver, `rewind()` y `playFrames(N)` con `quiet()`. No depende de cómo se tomó el SZX,
  pero una grabación de 20.000 cuadros tarda segundos en volver, y la máquina que volvió del SZX se
  descarta.
- (c) Solo la grabación, desde el principio.

Recomendada: **(a)**, porque es la única en que lo guardado es lo que se ve, y el paso 0 hace falta
igual para cualquier máquina corriendo. (b) queda como camino cuando el SZX no es del cuadro (una
sesión guardada antes del paso 0): si el cuadro guardado no coincide con `getFrameIndex` que da el
SZX, se rebobina y se corre.

## Los pasos

0. **Una máquina se guarda entre dos cuadros** (13, y vale para toda máquina corriendo). `Desk.keep`
   pide a cada máquina que se detenga al fin del cuadro antes de escribirla y la suelta después:
   `speccy.loop` ya tiene `later` y `setPaused`; falta "pausada y fuera del loop" como espera
   (`awaitOutOfTheLoop` existe en `session.finish`, se generaliza). Para una máquina manejada por
   un player es el hilo del player el que para: `RzxFrame` expone pausar entre cuadros (hoy ni
   siquiera respeta `loop.isPaused()`: la pausa del escritorio no pausa una grabación, otra
   inconsistencia del mismo origen). Prueba en rojo: una máquina corriendo se guarda y su SZX tiene
   el contador de T-states en 0 y el `frameCount` que dice la máquina; una manejada por una
   grabación se guarda con `getFrameIndex` igual al cuadro que la pantalla muestra.
   Va en `Desk.keep` (kit) la espera, en `Speccy`/`EmulationLoop` la pausa a fin de cuadro.
0b. **La grabación y el cuadro van con la máquina** (13, decisión 4). Nuevo `RzxMapping` en
   `tools/mappings` (`@OnlyWith(RzxFrame)`, como los mapeos de aparatos): `WindowInside` para el
   player que guarda `recording.url`, `recording.entry`, `recording.file` y `recording.frame`, y
   `SzxSession.keep` del equipo enganchado consulta `WindowInside` igual que para las sueltas (hoy
   no lo hace: una línea). Al volver, `clip` trae el player y `bringBack` le da lo de adentro; el
   player necesita `RzxSession.resumeOn(speccy, frame)` (el `of(recording)` que ya existe pero
   sobre la máquina dada y con `playback` en ese cuadro: `RzxPlayback` toma el índice en el
   constructor o lo expone). `RzxFrame` necesita `recording()/frame()` y `resume(file, url, entry,
   frame)`: API de dominio, no de guardado, como `GamesFrame.search()`. Prueba en rojo, en
   `AMachineComesBackWithItsWindowTest`: una máquina manejada por una grabación se guarda en el
   cuadro N y vuelve con el player en el cuadro N, reproduciendo, y a los diez cuadros la pantalla
   es la misma que la de la grabación corrida de un tirón hasta N+10. Trampa: `tool-rzx` publicado
   no tiene esa API; el mapeo es `@OnlyWith` de la clase, así que sin el plugin nuevo no se rompe.

0c. **Una máquina sabe su juego de una sola manera** (14). `EmulatorWindow.game()` es la única
   pregunta; el escritorio la contesta siempre que pueda, en `createNewEmulator`: lo que vino del
   navegador tal cual, y para un archivo `has.aboutGames().gameIn(file)` una vez (hoy lo hace
   `PokesTool` en cada click). `PokesTool` y `GameDetailsTool` dejan de mirar `getFilename` y se
   llevan la limpieza del nombre (-12 en pokes). Para una grabación, el juego viaja con ella:
   `Chosen` lo lleva, `Desk.play(url, label, entry)` gana una sobrecarga con `Game` (la vieja queda
   por el `tool-games` publicado), `Opens` dice `game()` (default null) y `Desk.show(machine, title,
   asking)` arma la ventana con `asking.game()` y `control.setFilename` del archivo de la grabación.
   Una grabación abierta por archivo se identifica por su snapshot: un `SnapshotFormat` para `.rzx`
   en `devices/snapshots` que `reads` y devuelve en `ram` la RAM del snapshot que trae (lo que
   `RzxSession.open` ya saca con `RzxParser`), así `SnapshotPayload.of` le saca la huella de
   siempre. Pruebas en rojo: la ventana armada para una grabación lanzada desde el navegador contesta
   `game()` con ese id; un `.rzx` de un juego del catálogo lo identifica `LocalGames.whoIs`; `PokesTool`
   sobre esa máquina encuentra los pokes por id. Va antes del 0b porque lo que la sesión guarda del
   player (0b) incluye el juego, y el `RzxMapping` lo toma de `game()`, no de otro lado.
1. **El apilado es de `AttachedFrame`** (1 y 2). `setMachineWindow` agrega al `machineEnd` que ya
   escucha a la máquina un `internalFrameActivated` que llama a `keepWithMachine`, y el propio
   frame hace lo mismo al activarse (sube la máquina y se pone delante). Las dos en `invokeLater`,
   después del click que activó, no durante. Se borra el `internalFrameActivated` de `RzxFrame`.
   Prueba en rojo: `raising_the_machine_brings_it_along` (una tercera ventana arriba de las dos, se
   selecciona la máquina, la enganchada queda delante de la máquina y la tercera atrás) y
   `raising_it_brings_the_machine_under_it`. Después, en Xephyr: diez clicks seguidos en el botón de
   cerrar del player, cuántos cierran; si no es diez, seguir buscando ahí antes de pasar al paso 2.
   Neto esperado: ~+15 en `AttachedFrame`, -15 en `RzxFrame`.
2. **Ofrecer el `Equipment` que abre un archivo** (3). En `load()`, después de `somethingOpens`:
   si ningún `Equipment` puesto abre el archivo y algún hueco de `couldBring` ofrece uno que sí
   (`Offer.is(Equipment.class)`; falta que la oferta diga qué abre, o se pregunta por el nombre del
   plugin como `BesideTheGame.kindsBeside`), se ofrece con `WhatIsMissing.offer` una vez, con el
   mismo "no volver a preguntar" (`mayAsk`). Traído, `cassetteFor` lo encuentra solo. Prueba en el
   doble de escritorio: con una cinta y sin casetera instalada, lo que se ofrece la nombra. Va en
   `ZXSpectrumDesktopApp.load`, que es el único camino de un archivo a una máquina; nada nuevo.
3. **Una sola máquina en uso** (4). `getActiveEmulator` se va; todo pasa por `machineBeingUsed`,
   que agrega el caso "ninguna seleccionada, la primera visible" que hoy tiene el otro.
   `getActiveEmulatorOrCreateNew` queda sobre él. Prueba: con el player seleccionado, `tool.use`
   recibe la máquina del player. Neto: -10.
4. **La tecla va a un solo lado** (5). Cuando la tecla fue a la máquina, `typeIntoTheMachineInFront`
   la consume (`return true`) salvo que el foco esté en algo que escribe (`somebodyIsWriting` ya lo
   sabe) o sea un acelerador del menú (`KeyStroke` que algún `JMenuBar` tenga: preguntar a
   `menuBar.getMenu(i)` una vez y guardar el set). Prueba: Espacio con un botón enfocado en una
   enganchada no dispara el botón. Riesgo: los atajos de las herramientas (`MachineTool.key`) son
   aceleradores, así que entran en el set.
5. **Cerrar y desenganchar según la decisión 1.** `DeviceFrame` deja de desenchufar en
   `internalFrameClosed`; `RzxFrame` deja de `dispose()` la máquina; `AttachedFrame.machineClosed`
   sigue cerrando la enganchada cuando se cierra la máquina. `show(Equipment)` vuelve a abrir la
   ventana de un aparato que sigue enchufado (`find(machine)` ya lo encuentra). Pruebas:
   `closing_the_window_leaves_the_device_plugged_in` (hoy en verde al revés en `PrinterDockingTest`:
   `theMachineClosingTakesThePrinterWithIt` queda, `attachingPlugsThePrinterIn` queda) y
   `closing_the_player_leaves_the_machine_running`.
6. **Un movimiento por código no es un arrastre** (7). `AttachedFrame` tiene un `placing(Runnable)`
   que setea `placedAt` alrededor de cualquier `setBounds` propio; `takeMachine`, `attachTo` y
   `setCompact` pasan por ahí (hoy cada uno pone `placedAt` a mano, o no). Cascade y Tile saltean a
   las enganchadas: la máquina se mueve y `place()` las lleva. Prueba: después de `cascadeWindows`
   ninguna enganchada tiene `dragging` ni el listener AWT puesto, y sigue en su máquina. Neto:
   -20 en el escritorio (las dos funciones quedan sobre `getAllFrames()` filtrado), 0 en el kit.
7. **Los ajustes como `Equipment`** (8, decisión 2). `MachineSettingsTool.use` pasa a
   `Desk.theOne().equipment("Settings")`; `ScreenSettingsInternalFrame` se vuelve un `MachineFrame`
   con `prefersDock(RIGHT)` y toma los ajustes de `coreOf(machine)` en `machineChanged`. Las dos
   son `Equipment` de `tool-controls` (una línea `@Offers` cada una). Vuelven con la sesión sin
   tocar los mapeos. Prueba: `theSettingsWindowComesBackClippedWhereItWas`. Trampa: `tool-controls`
   publicado llama `Desk.place`; se deprecia, no se saca.
8. **El orden al volver** (9). `Desk.keep` guarda las ventanas de atrás hacia adelante y `bringBack`
   las abre en ese orden, así que el orden ya está en la lista; lo que lo rompe es `place()` de la
   enganchada, que la pone justo delante de su máquina. En `bringBack`, después de abrir todas, una
   pasada que aplica el orden de la lista con `setComponentZOrder`. `zOrder` de `WindowState` se
   queda sin uso hasta el paso 10. Las coordenadas se acotan al escritorio al volver (y ≥ 0, x tal
   que entre la barra de título): es de `bringBack`, no de quien guardó. Prueba: tres ventanas con
   una entre máquina y enganchada vuelven en ese orden; una con y negativa vuelve con y 0.
9. **El refresco es de `AttachedFrame`** (11, 10 por la decisión 3). `AttachedFrame.refreshEvery(ms,
   Runnable)` arranca el `Timer` y lo para en `internalFrameClosed`; los treinta hijos pasan a una
   línea. `openWhatTheMachineUses` recuerda tamaño y lado por `kind.name()` en el escritorio.
   Neto: -4 líneas por ventana, unas -100 en los plugins.
10. **Limpieza** (12). Se va `stateCapture` y lo del historial de snapshots que ya no corre
    (`openSaved` sigue por un `tool-games` publicado: deprecado). Los nueve campos de `WindowState`
    siguen hasta que se publique un `tool-pokes` que no los nombre; se anota en `plugin-binary-api`.
    `GamesFrame`, `FavoritesFrame`, `HistoryFrame`, `PluginsInternalFrame`: un solo `DeskWindow`
    en el kit si al terminar los pasos anteriores tienen algo en común además de `showOnTheDesk`;
    si no, se quedan.
11. **Los pendientes del otro plan que son del escritorio**, en este orden: el lado del botón de
    enganchar (`preferred` va al `inside` de la ventana, una entrada más en el mapeo que ya guarda
    `dock`); la progresión de la casetera (`showProgress` desde el bloque que el TAPE trae al
    volver); las ventanas de una configuración vieja (sin `inside`: se abren por nombre y listo);
    el cohete de `TurboTool` (lee la velocidad cada vez que se muestra, como el botón de mudo).

Orden: 0, 0c y 0b primero, porque es lo único que pierde algo (una sesión guardada con una
grabación a medias no vuelve, y una máquina de grabación no tiene juego); después 1 y 2 porque son los que se ven; 3 y 4 juntos porque tocan lo mismo;
5 espera la decisión 1; 6 a 9 son mecánicos; 10 y 11 al final. Cada paso es un commit con su prueba, y después
de 5 y 7 se corre el escritorio en Xephyr con la configuración de Fernando.

## Lo que no entra

- Un gestor de ventanas distinto de `JDesktopPane` o varios escritorios.
- Que las ventanas de escritorio (navegador, favoritos, historial) se enganchen a una máquina.
- Load State sobre una máquina abierta: es del plan de guardado, no del escritorio.
- Arrastrar la máquina con sus enganchadas como un bloque: hoy se arrastran por separado a
  propósito ("con varias grabaciones el punto es poner las imágenes una al lado de la otra").
