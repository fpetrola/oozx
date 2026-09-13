# Las formas de C que quedaron, y qué las reemplaza

Buena parte de este código tiene todavía la forma que tendría en C y nunca
cambió: funciones que devuelven un número en vez de tirar una excepción, parámetros de salida,
enteros que son estados, structs con todos los campos públicos, y bloques de C comentados que
sobrevivieron. Este plan las nombra, dice qué las reemplaza y en qué orden.

No es un plan de estilo. Cada cosa acá cuesta algo concreto: un código de error que nadie mira es
un error perdido, un entero que es un estado no dice cuáles son sus valores, y una clase que nadie
nombra hace más grande cada búsqueda.

## Lo que se midió, el 6 de septiembre de 2026

| | |
|---|---|
| archivos `.java` en `machine` y `emulator` | 824 |
| clases de `src/main` que nadie nombra, ni siquiera calificadas | 47 |
| de esas, del emulador y su escritorio | 18 |
| declaraciones de `byte[] attached`, el parámetro de salida de leer un puerto | 40 |
| lugares que lo leen como `attached[0]` | 29 |
| métodos de `Disk` que devuelven `boolean` queriendo decir "no entró" | 9 |
| líneas de C comentado en `Tape` / `ZXSpectrumDesktopApp` / `SpecPlus3` | 285 / 273 / 65 |

Los conteos de clases sin uso excluyen las que registra `META-INF/services` — los `*Devices` y
`*Equipment` de cada periférico, que nadie nombra porque los descubre el `ServiceLoader` — y
cuentan los usos calificados con el paquete, que es donde una primera medición se equivocó.

## Los modismos, uno por uno

### Un número en vez de una excepción

`Machine` tiene cuatro métodos que devuelven `int`: `select`, `selectDefault`, `selectMachine` y
`reset`. Cero es que salió bien. **Nadie mira el resultado de ninguno de los cuatro.** El que decía
"machine type unknown" ya perdió su mensaje cuando se borró `UserInterface`, y lo único que queda
es un 1 que se descarta.

Van a ser `void` y a tirar cuando no puedan. Lo que hoy es `return 1` es una `IllegalStateException`
con el nombre de la máquina adentro, que es lo que el que la llamó necesita para entender qué pasó.

### Un `boolean` que quiere decir "falló"

`Disk` tiene nueve: `gapAdd`, `idAdd`, `dataAdd`, `datamarkAdd`, `preindexAdd`, `postindexAdd`,
`gap4Add`, `trackgen` y `dataAdd` con el sitio. Todos devuelven `true` cuando **no** pudieron, que
es el `return 1` de C con otro tipo. Se lee al revés de como se escribe: `if (idAdd(...))` significa
"si no entró".

Acá no corresponde una excepción: no poder escribir un sector porque la pista se llenó es normal
al armar una imagen, y el que llama decide. Corresponde invertir el sentido y el nombre: `wrote…`
que devuelve `true` cuando escribió. Es un cambio mecánico y el compilador no ayuda, así que va de
a un método por commit, con los tests de disco de gate.

`Fdd.writeData()` es el mismo caso y ya está bien documentado ("Answers false when the disk is
write-protected"); queda como está.

### Un código con más de dos valores

`UpdFdc.readId` devuelve 0, 1 o 2; `seekId` devuelve 0, 1, 2 o 3; `readDatamark` devuelve 0 o 1.
Están así porque `upd_fdc.c` los tiene así, y el port es fiel a propósito.

Estos **no se tocan todavía**. Son la parte del chip que menos se entiende y la que más caro sale
equivocar, y el port es nuevo. Cuando haya más tests que los cubran, `readId` pasa a devolver un
`enum Found { OK, CRC_ERROR, NONE }` y `seekId` uno con su cuarto valor. Antes no.

### Parámetros de salida

`PortHandler.read(int port, byte[] attached)` es un parámetro de salida de los de C: el método
devuelve lo que puso en el bus y avisa por el arreglo si lo manejó. Cuarenta declaraciones,
veintinueve lugares que lo leen.

Lo que corresponde es que leer un puerto conteste **una cosa**: qué valor y si alguien lo manejó.
Un `record BusAnswer(int value, boolean driven)` lo dice entero.

Se creía que estaba en el camino caliente, porque se llama por cada `IN` y por cada dispositivo
que responde a ese puerto. **Se midió el 6 de septiembre de 2026 y no lo está.**

| | lecturas/frame | lectores/frame |
|---|---|---|
| una 48K en el prompt de BASIC | 8,00 | 8,00 |
| Jet Set Willy corriendo solo, después de soltar la grabación | 0,07 | 0,07 |

Las ocho de BASIC son el escaneo de teclado de la ROM, ocho medias filas por interrupción, y son
el peor caso: un juego lee mucho menos. La primera medición dio **cero** en playback de RZX, que
es correcto y no sirve — en una grabación los `IN` salen del archivo y la máquina nunca le
pregunta a los periféricos. Por eso el control en BASIC: valida el instrumento antes de creerle.

Después se midió el costo, poniendo una asignación por lectura en ese mismo lugar y forzándola a
escapar, que es el peor caso posible:

| núcleo generado, en BASIC | bytes/frame | cpu/frame |
|---|---|---|
| como está hoy | 230 | 100–114 µs |
| con un objeto por lectura | 422 | 93–107 µs |

Los 192 bytes de más son exactos: ocho lecturas por 24 bytes. **En cpu no se mide nada** — la
diferencia queda debajo del ±15 % que tiene esta máquina. Y 192 bytes/frame son tres órdenes de
magnitud menos que los 300 kB/frame y los 4 MB/frame que sí hubo que perseguir en este repo.

**Entonces el `record` se puede.** La forma se elige por lo que dice, no por lo que cuesta. Y el
número medido es una cota superior: forzamos el objeto a escapar; devuelto y desarmado en el
lugar, el JIT puede no asignarlo nunca.

`Disk` tiene otros dos, `int[] from` y `int[] dataStart`, que son punteros de C. `from` puede ser
un cursor con estado (una clase chica que envuelve el buffer y su posición, que es lo que
`buffer_t` es en C); `dataStart` puede irse si `dataAdd` devuelve dónde escribió en vez de si
falló, que es el mismo cambio de la sección anterior.

### Un entero que es un estado

| dónde | qué es hoy | qué debería ser |
|---|---|---|
| `UpdFdc.seek[4]` | 0, 1, 2, 4, 5 y 6, cada uno un estado del posicionamiento | un `enum` por unidad |
| `UpdFdc.speedlock` | −1 apagado, 0, 1, 2 | un objeto que cuente, o `Optional` |
| `UpdFdc.mt`, `mf`, `sk`, `nonDma`, `firstRw` | 0 o 1 | `boolean` |
| `Fdd.marks` | bit 0 FM, bit 1 débil | dos `boolean`, o un `record` |
| `Disk.flag` | máscara de tres banderas | un `EnumSet` |
| `Disk` `cpcFix` | 0, 1, 2, 3, 5 (no hay 4) | un `enum` con los nombres que ya están en los comentarios |

Los cinco `int` de `UpdFdc` que son 0 o 1 se pueden hacer `boolean` **hoy**, sin riesgo: el
compilador encuentra todos los usos. Los otros esperan a que el chip tenga más tests.

### Structs con todo público

`Speccy` tiene 26 campos públicos mutables, `Fdd` 19, `TrDos` 19, `Memory` 15, `Input` 15,
`Disk` 14, `WdFdc` 13.

En `Speccy` es deliberado y está bien: es la fachada por donde un test agarra las partes de la
máquina. En `Fdd`, `Disk` y los dos controladores es el `struct` de C: el controlador escribe
`d.data` y después llama a `d.writeData()`, que es pasar un argumento por un campo.

El cambio que vale es ese: `writeData(int data)` en vez de `d.data = x; d.writeData();`. Toca el
puerto de los dos controladores y los tests de disco lo cubren. Lo demás — hacer privados 60
campos y ponerles getters — no vale: agrega líneas y no dice nada nuevo.

### C comentado

285 líneas en `Tape`, 273 en `ZXSpectrumDesktopApp`, 65 en `SpecPlus3`, y más repartidas. Son
funciones enteras en C comentadas, algunas nombrando cosas que ya no existen (`Settings.current`,
`UIMedia`, `uiDrives`). Se borran: no dicen nada que el código de al lado no diga, y lo que dicen
ya no es cierto.

### Lo que nadie nombra

47 clases. 18 son del emulador y su escritorio:

- `bridge`: `CommandHandlerMultiplexor`, `EmulatorState`, `RetroMessageExt`
- el escritorio: `Buggy`, `CroationTextInGUI`, `GameDetailsDialogEnhanced`, `ScrollPane`,
  `ThumbnailApp`
- `modules/sound/JavaSoundTest`, que es un `main` en `src/main`
- el emulador: `DebugEnabledOOZ80`, `ExecutedInstruction`, `RegisterUtils`,
  `UnrolledRegisterBankFactory`, `ToPrimitiveIntBiFunction`, `WordNumberOperation`,
  `SyncInstructionSpy`, `ToStringInstructionVisitor`
- `zxinfo/GamesResponse`

Las otras 29 son de la línea de traducción a Java (`translation/`), que es otro trabajo y otro
plan.

### Lo que falta agrupar

En `speccy/` quedaron sueltas siete clases que son el vocabulario del teclado: `KeyBit`,
`KeyboardKeyName`, `KeyInfo`, `KeysymsMap`, `KeyText`, `SpectrumKeys`, `SpectrumKeysWrapper`. Van a
`modules/keyboard/`, donde está el módulo que las usa. Quedan en `speccy/`: `Emulation`, `Input` y
`Movie`.

Y `speccy/machine/` — las 21 clases de los modelos — es la otra mitad del módulo `Machine`, que
quedó pendiente cuando se armaron los subpaquetes por módulo.

## El orden

Cada paso deja el árbol compilando y los gates en verde, y va en su propio commit.

1. **Borrar las 18 clases que nadie nombra.** Sin riesgo: el compilador confirma.
2. **Borrar el C comentado.** 600 líneas y pico. El original está en el repo.
3. **`Machine` deja de devolver códigos.** Cuatro métodos a `void`, con excepción donde hoy hay un
   1 que nadie mira.
4. **Los cinco `int` binarios de `UpdFdc` pasan a `boolean`.**
5. **`speccy/machine/` entra a `modules/machine/`,** y las siete del teclado a `modules/keyboard/`.
   Es el movimiento que quedó pendiente; cambia el nombre de paquete de las fuentes del modelo, así
   que hay que regenerar la copia commiteada del core.
6. ~~**Medir el camino de puertos**~~ — hecho. Ocho lecturas por frame en el peor caso y cero
   diferencia de cpu: `attached` se vuelve un `record BusAnswer(int value, boolean driven)`, y
   `DefaultPortHandler` devuelve una constante compartida para el caso de no manejar el bus.
7. **Los nueve `boolean` de `Disk` se dan vuelta,** uno por commit.
8. **`d.data = x; d.writeData()` se vuelve `writeData(x)`** en los dos controladores.

Los estados de `UpdFdc` (`seek`, `speedlock`, `cpcFix`) y los códigos de `readId`/`seekId` no están
en esta lista: esperan a que el chip tenga más tests que los cinco que tiene hoy.

## Lo que no hay que hacer

- **Hacer privados los campos y generar getters.** Agrega líneas y no dice nada que el campo no
  dijera.
- **Cambiar `Memory`, `Ula` o `Display` por gusto.** Son las fuentes del modelo del que sale el core
  rápido: cada cambio invalida el caché y obliga a regenerar la copia commiteada. Solo cambios que
  mejoren el modelo, nunca cosméticos.
- **Tocar el port de `upd_fdc` antes de tener más tests.** Es fiel a propósito y es nuevo.
