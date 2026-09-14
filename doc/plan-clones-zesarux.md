# Los clones que ZEsarUX emula y nosotros no, y qué le cuesta a nuestro modelo cada uno

ZEsarUX emula cuarenta y pico de máquinas. De ésas, las que son un Spectrum o un clon de Spectrum
y todavía no están acá son catorce, más una docena de variantes de ROM. Este plan dice, para cada
una, qué hecho del hardware la distingue y en qué punto de **nuestro** modelo aterriza ese hecho,
con la misma regla que ordenó el plan anterior: el core no nombra periféricos ni máquinas. Si un
cambio en `machine/core` necesita decir "Inves" para explicarse, está en el lugar equivocado.

El oráculo esta vez son las fuentes de ZEsarUX, leídas con cuidado: `cpu.c` (`set_machine_params`
y la elección de ROM), `machines/*.c`, `contend.c`, `video_chips/ulaplus.c` y los puntos donde
`operaciones.c` y `core_spectrum.c` preguntan por una máquina. A diferencia de la referencia
anterior, ZEsarUX se puede correr sin pantalla (`--vo stdout --ao null --machine inves
--exit-after 5`), así que además de leerlo se le puede preguntar.

## Lo que queda afuera, y por qué

ZEsarUX lista MK14, ZX80, ZX81 y sus clones (TK80 a TK85, TS1000 y 1500, CZ1000 y 1500), QL, Z88,
Jupiter Ace, SAM Coupé, los Amstrad CPC y PCW, MSX, Spectravideo y tres consolas. Ninguna es un
Spectrum: otra memoria de vídeo, otro bus, otra ULA o directamente otro procesador. Traerlas no es
extender este emulador sino escribir otro al lado, y no es lo que este plan mide.

## Lo que se midió, el 14 de septiembre de 2026

| máquina | reloj | línea / frame | contención | ROM, y quién la publica | lo que la distingue |
|---|---|---|---|---|---|
| TK90X, TK95 (PT y ES) | el del 48K | los del 48K | la del 48K | `tk90x.rom`, `tk90xs.rom`, `tk95.rom`, `tk95es.rom`, 16K, Microdigital; ZEsarUX las trae | un 48K con otra ROM; ZEsarUX no les da ninguna otra diferencia |
| CZ Spectrum, CZ 2000 | el del 48K | los del 48K | la del 48K | `48.rom`, la nuestra | un nombre |
| CZ Spectrum Plus | el del 48K | los del 48K | la del 48K | `inves.rom` | un 48K con la ROM del Inves, sin sus defectos |
| 48K+ ES, 128 ES, +2 FR y ES, +2A 4.0 y 4.1 y ES, +3 4.0 y 4.1 y ES | los de cada una | los de cada una | la de cada una | Amstrad, permitidas | la misma máquina con otra ROM |
| Inves Spectrum+ | 3 500 000 | 228 por línea | ninguna | `inves.rom`, 16K, Investrónica sobre código Amstrad | 64K de RAM bajo la ROM, ULA que lee 255 en 0xff y que enmascara lo que se le escribe con la RAM, Kempston sólo con A5 bajo, y un defecto que escribe 255 en memoria en cada interrupción |
| Chloe 140SE | 3 500 000 | los del 48K | páginas 5 y 7 | `se.rom`, 32K, GPL: son nuestras `se-0` y `se-1` | paginación del 128 con la ROM del SE; sin dock ni exrom |
| Chloe 280SE | 3 500 000 | los del 48K | páginas 5 y 7 | la misma | lo anterior más MMU Timex con 64K de EX y 64K de DOCK, ULAplus, vídeo Timex y turbo por el registro 0 de la ULA2 |
| Chrome | 3 580 000, y 7 100 000 por un bit | 228 por línea | páginas 2 y 5 | `chrome.rom`, 64K; autor a verificar | 128 más dos páginas de RAM y dos ROMs, 0x1ffd con sus propios bits, I2C, turbo, y un +D adentro |
| Prism | el del 48K por 1, 2, 4, 8 o 16 | 133 por línea, bordes de 64 y 48, imagen de 512 por 384 | ninguna | `prism.rom`, 320K, y `prism_failsafe.rom`, 16K; proyecto abierto, licencia a verificar | otro sistema de vídeo: 64 páginas de SRAM, cuatro de VRAM, registros ULA2 en 0x9e3b y 0xae3b, paletas de 12 bits |
| ZX-Uno | el que elija el core | los del 48K, del 128 o del Pentagon, a elección | la que elija | `zxuno_bootloader.rom`, 8K, más una imagen de flash de 4M con la BIOS y las ROMs Sinclair | un fichero de registros en 0xfc3b y 0xfd3b: BOOTM, DivMMC adentro, MMU, ULAplus y Radastan, DMA, UART, flash SPI |
| ZX-Evolution BaseConf | 3 500 000 | 228 por línea | ninguna; Z80 CMOS | `zxevo_baseconf.rom`, 512K, NedoPC; licencia a verificar | Pentagon con 4M en 256 páginas, 32 de ROM, paginación por puertos 0xAF y mapas, NVRAM |
| ZX-Evolution TS-Conf | 3 500 000 por 1, 2 o 4 | 224 por línea | ninguna; Z80 CMOS | `zxevo_tsconf.rom`, 512K | lo anterior más una GPU: sprites, tiles, modos de 16 y 256 colores, DMA, ZiFi |
| ZX Spectrum Next | 3 500 000 por 1, 2, 4 u 8 | los del 48K o del 128, por registro | la que diga el registro | `tbblue_loader.rom`, 8K, más una imagen SD | 2M, un centenar de registros en 0x243b y 0x253b, Layer 2, sprites, tilemap, copper, DMA, y un Z80 con instrucciones que el nuestro no tiene |

De nuestro lado, los puntos donde eso toca:

| pieza nuestra | qué es hoy | lo que los clones le piden |
|---|---|---|
| `Picture.PALETTE` y `Colouring` | dieciséis colores fijos; un atributo es tinta, papel, brillo y flash | una paleta que la máquina puede redefinir, con 64 entradas, y un atributo que en ese modo es un índice y no tiene flash |
| `MachineTimings.processorSpeed` | un número por máquina, leído al elegirla | un número que la máquina cambia corriendo: Chloe, Chrome, Prism, Uno, TS-Conf y Next lo hacen por un registro |
| `MemoryBus` | mapa de lectura y mapa de escritura, ya separados | que una máquina ponga una ROM para leer y RAM para escribir en el mismo tramo |
| `Cpu.takeInterruptIfTheLineIsStillDown` | privado | un asiento para lo que pasa en el instante en que se acepta una interrupción, sin nombrar quién lo usa |
| `Spec128.pageAt` y `romAt` | la aritmética de página, por máquina | lo mismo para Chrome, con dos páginas más y una ROM más |
| `SpecSe` | paginación del 128 más dock y exrom como RAM | es un Chloe 280SE a falta de ULAplus y turbo; el 140SE es eso sin el dock |
| `TimexMemoryPeripheral`, `ScldPortHandler` | el 0xf4 y el 0xff de las Timex | exactamente eso para Chloe, Uno y Prism |
| `DivMmcPeripheral`, `PlusDPeripheral` | dispositivos que se enchufan | ir a bordo, dentro de una máquina, como el Beta va en el Pentagon |
| el Z80 del módulo `emulator` y el núcleo generado | el conjunto de instrucciones del Z80 | el CMOS que devuelve 0xff en `OUT (C),0` para las Evolution; y las instrucciones nuevas del Z80N para el Next |
| `RomFiles.sources` | de dónde se baja una ROM que no es nuestra, con su hash, y a qué posición de una imagen | lo mismo, con las URL de ZEsarUX en GitHub, que son fuentes publicadas y con hash fijable |
| `Machines.MODEL_NAMES` y los conteos en dos suites | crecen en uno por máquina | crecen en catorce |

## Los dos cambios de modelo que importan

El plan anterior tenía uno, dieciséis pixeles por columna. Éste tiene dos, y conviene decidirlos
antes de la primera máquina que los necesite, porque cada uno lo necesitan cinco.

### La paleta es un hecho de la máquina

Hoy `Picture` tiene dieciséis colores en una tabla estática y `Colouring` saca tinta y papel de un
atributo. ULAplus cambia las dos cosas a la vez: la paleta pasa a tener 64 entradas que el programa
escribe por 0xbf3b y 0xff3b, y en su modo el atributo deja de tener flash: los bits 6 y 7 eligen
una de cuatro tablas de dieciséis, y tinta y papel son índices en ella.

**Cómo lo resuelve ZEsarUX.** Una tabla global `ulaplus_palette_table` de 64 bytes GGGRRRBB, un
modo global, y en el trazado de cada línea un `if (ulaplus_enabled)` que elige cómo decodificar. Es
lo que no vamos a hacer: sería el `if (timex)` del plan anterior con otro nombre.

**La forma de traerlo acá.** Dos asientos, ninguno con nombre de dispositivo:

1. `Picture` deja de tener la paleta escrita y la recibe: un arreglo de enteros RGB que por
   omisión es el de siempre, con dieciséis entradas, y que un dispositivo puede reemplazar por uno
   de 64 y modificar entrada a entrada. `plot8`, `plotPair` y `plot16` ya indexan por color; no
   cambian.
2. `Colouring` deja de ser estático: una máquina puede ponerle otra manera de leer un atributo,
   con la firma de siempre, `ink(byte)` y `paper(byte)`, que en el modo ULAplus devuelven índices
   de 0 a 63 y nunca invierten por flash. El borde también pasa por ahí.

El dispositivo `UlaPlusPeripheral`, en `machine/machines`, tiene los dos puertos, la paleta y el
modo, y en sus escrituras ajusta los dos asientos y pide `refreshAll`. Los snapshots SZX ya traen
`ULAPlusEnabled`, `ULAPlusActive` y `ULAPlusPalette` en `SpectrumState` — se leen desde hace
tiempo y no se usan; con el dispositivo, se usan. Los ocho Sinclair no cambian un byte de salida, y
`CostRegressionTest` sigue midiendo lo mismo, porque una indexación en un arreglo cuesta lo que
costaba.

Los modos lineales de ZEsarUX (Radastan, 128 por 96 a 16 colores, y sus propios modos 1, 5 y 9)
son otro paso: una regla de pintado nueva donde un byte son dos pixeles de una paleta, cosa que
`plotPair` ya sabe hacer para el Pentagon 1024. Van con el ZX-Uno, no con ULAplus.

### La velocidad del procesador es un hecho que la máquina cambia corriendo

Hoy `MachineTimings` es un `record` y `processorSpeed` se lee al elegir la máquina: el `Timer`
calcula cuántos cuadros por segundo, `Sound` con qué reloj muestrea, y `Speed` cuánto es el 100 %.
Seis clones cambian el reloj por un registro mientras corren, siempre en múltiplos del base: por
2, 4, 8 y 16.

**Cómo lo resuelve ZEsarUX.** Un `cpu_turbo_speed` global que multiplica los T-states por línea:
el cuadro sigue teniendo las mismas líneas, cada línea tiene más ciclos, y todo el vídeo y la
contención se calculan contra `screen_testados_linea * cpu_turbo_speed`.

**La forma de traerlo acá.** El hecho es "cuántos ciclos del procesador entran en una línea de la
imagen", y eso ya lo dice `MachineTimings.tstatesPerLine()`. Un multiplicador entero en la máquina,
`turbo()`, que por omisión es 1, y tres lectores: `Timer` lo usa para saber cuántos T-states son un
cuadro, `Sound` para el reloj del muestreo, y la contención para no aplicarse al doble de
velocidad donde la máquina dice que no se aplica. Ningún periférico lo cambia por su cuenta: lo
cambia la máquina, en el manejador del registro que lo controla. `Speed`, que es el porcentaje que
pide el usuario, es otra cosa y no se mezcla: el 100 % de un Chrome a 7 MHz es un Chrome a 7 MHz.

Lo que hay que mirar antes de tocarlo: `UlaTimingsOverAFrameTest` fija el retardo en cada T-state
de un cuadro para cada máquina; el turbo lo tiene que dejar idéntico para las que no lo usan.

## Cada clon, y dónde aterriza

### 1. Los que son un 48K con otra ROM

TK90X y TK95, cada uno en portugués y en castellano; CZ Spectrum, CZ 2000 y CZ Spectrum Plus; y
las once variantes de idioma y revisión de las Amstrad. ZEsarUX los trata a todos como máquinas.
Acá hay que decidir una cosa antes: **una ROM distinta, ¿es una máquina distinta?**

- Para las variantes de idioma y revisión de una máquina que ya tenemos, no: `RomFiles.choose`
  existe para eso. Van como ROMs que se pueden elegir para el 48K, el 128, el +2, el +2A y el +3,
  con sus nombres, y no aparecen en el menú de máquinas. Cero clases.
- Para TK90X y TK95, sí: son otro fabricante, otro teclado, y el TK90X real es de 60 Hz aunque
  ZEsarUX no lo modele. `Tk90x extends Spec48` con su ROM, y `Tk95 extends Tk90x`. Ahí queda una
  decisión abierta que anoto sin tomar: darle al TK90X el cuadro de 60 Hz que ya tiene el 48K NTSC
  es más fiel al aparato y menos fiel al oráculo.
- Para los CZ, son un nombre: un `Cz2000 extends Spec48` con la ROM del 48K, y el CZ Spectrum Plus
  con la del Inves pero sin ninguno de sus defectos, que es exactamente como lo tiene ZEsarUX.

Las ROMs de Microdigital no tienen un permiso conocido como el de Amstrad. ZEsarUX las distribuye;
nosotros las traemos como al Scorpion, por `sources` con URL y hash, hasta que alguien encuentre el
permiso. `inves.rom` es código Amstrad con cambios de Investrónica: la parte Amstrad está
permitida, la de Investrónica está por verificar; mismo camino.

### 2. Inves Spectrum+

El único clon con personalidad de este grupo, y toda su personalidad son defectos. De ZEsarUX:

- **64K de RAM y la ROM encima.** La ROM se lee en 0x0000; lo que se escribe ahí va a la RAM de
  abajo. `MemoryBus` ya tiene mapa de lectura y mapa de escritura separados; la máquina los llena
  distinto en el primer slot. Es un hecho que se puede probar en dos líneas.
- **Sin contención**, y 228 T-states por línea en vez de 224.
- **0xff lee 255 siempre.** `hasFloatingBus()` en falso, `unattachedPort` fijo.
- **Kempston sólo con A5 bajo.** El `KempstonStrictPeripheral` decodifica con 0x00e0; el Inves
  decodifica menos. Es una máscara distinta, y la máquina puede decir cuál.
- **La ULA enmascara lo que se le escribe** con el byte de RAM que hay en la dirección del puerto:
  `OUT (0xfe), A` deja `A & RAM[0xfe]` en el borde y el altavoz, y el altavoz suena por EAR xor MIC.
  Es un manejador de puerto propio, en la máquina.
- **El defecto de la interrupción.** En cada interrupción aceptada escribe 255 en `I*256 + R`. Con
  IM 2 y R por encima de 127, la tabla de vectores se va pisando y el programa termina reseteándose:
  ZEsarUX documenta que Ranarama se cae por esto, y que Barbarian, Lorna y Tai-Pan dependen de qué
  quedó en R. Éste es el que toca core: hace falta un asiento en `Cpu`, "en el instante en que se
  acepta una interrupción", al que una máquina se pueda suscribir. Sin nombre, y con un hecho que
  diga que las otras dieciséis máquinas no escriben nada.
- **Nieve en el borde** y el papel negro con brillo: dos glitches visuales que ZEsarUX ofrece como
  opción. Los dejo para el final y quizá para nunca.

### 3. Chloe 140SE y 280SE

El 280SE es, con una diferencia, nuestro `SpecSe`: la misma ROM del SE, la misma paginación del 128
con dock y exrom como RAM. La diferencia es que ZEsarUX le da ULAplus, los modos de vídeo Timex y un
turbo por el registro 0 de la ULA2 del Prism. El 140SE es lo mismo sin dock ni exrom. Contención en
las páginas 5 y 7, cuadro del 48K.

- `Chloe280Se extends SpecSe` con `UlaPlusPeripheral` y `ScldPeripheral` a bordo y el turbo.
- `Chloe140Se extends Chloe280Se` que no lleva el `TimexMemoryPeripheral`.
- Entra después de los dos cambios de modelo, y es la primera máquina que los usa juntos.

### 4. Chrome

Un 128 con dos páginas de RAM más, la 8 y la 9, y cuatro ROMs. El 0x7ffd es el de siempre; el
0x1ffd es suyo: bit 0 pone la 8 o la 9 en 0x0000, bit 1 es el bit alto de la ROM y elige entre 8
y 9, bit 2 pone la 9 en 0x4000 sin mover la pantalla, bit 3 es el turbo a 7.1 MHz, bit 4 apaga la
paginación del disco, bit 5 apaga todo lo anterior y lo vuelve un 128, bits 6 y 7 son I2C.
Contención en las páginas 2 y 5, 228 por línea. Y un +D adentro, con su RAM en 0x2000.

- `Chrome extends Spec128` con `pageAt` para los slots 0 y 1 y `romAt` con dos bits.
- `PlusDPeripheral` a bordo, como el Beta va en el Pentagon: primer caso de un dispositivo que ya
  tenemos yendo dentro de una máquina que no es la suya.
- El turbo, que es el segundo cambio de modelo.
- La ROM es un archivo de 64K con las cuatro; `sources` ya sabe recortar. El autor y la licencia
  del Chrome están por verificar antes de fijar la fuente.

### 5. ZX-Uno

No es una máquina sino una caja de máquinas: un fichero de registros en 0xfc3b y 0xfd3b decide el
modo de arranque, si el DivMMC está, qué MMU rige, qué modo de vídeo, y con qué cuadro se corre.
Casi todas sus piezas ya están o están en este plan: DivMMC, ULAplus, SCLD, la MMU del Chloe, los
tres cuadros. Lo que no está:

- **BOOTM y la flash SPI.** Arranca con 8K de bootloader y lee su BIOS y sus ROMs de una flash de
  4M por SPI. La imagen de flash trae ROMs Sinclair adentro, así que se distribuye con el permiso de
  Amstrad; se trae por `sources` y se recorta. La SPI es un dispositivo nuevo, chico.
- **Radastan**: 128 por 96 a 16 colores, lineal, un byte dos pixeles. Es `plotPair` con otra regla
  de dónde salen los bytes, como los 16 colores del Pentagon 1024.
- **DMA y UART.** Dos dispositivos más, aislados.

Va después de Chloe y Chrome, porque los reusa enteros, y es la primera máquina cuyo cuadro
cambia corriendo: `MachineTimings` tendría que poder ser elegido por la máquina y no fijado en la
clase. Eso es un tercer cambio de modelo, menor: un `getTimings()` que devuelve lo que un registro
dice.

### 6. ZX-Evolution: BaseConf y TS-Conf

Una placa Pentagon con 4M en 256 páginas y 512K de ROM en 32, paginada por puertos 0xAF con tablas
de mapas, con NVRAM y con un **Z80 CMOS**: `OUT (C),0` saca 0xff donde el NMOS saca 0x00. Eso es un
hecho del procesador, y el procesador es el módulo `emulator` más el núcleo generado: es la primera
vez que un clon toca ahí. Se puede hacer como un bit de configuración del Z80, con un hecho, sin
nombrar la placa.

BaseConf es la configuración "Pentagon compatible": va primero, y ya es grande por la paginación.
TS-Conf es la misma placa con una GPU: sprites, tiles, modos de 16 y 256 colores con su propia
paleta, DMA, y los registros ZiFi. Es un segundo sistema de vídeo entero y va al final, si va.

### 7. Prism y ZX Spectrum Next

Los dejo juntos porque comparten el problema: los dos son otro sistema de vídeo, y el Next además
otro procesador.

- **Prism**: imagen de 512 por 384, 133 T-states por línea en su turbo, cuatro páginas de VRAM,
  paletas de 12 bits, registros ULA2 en 0x9e3b y 0xae3b, 64 páginas de SRAM, y un modo failsafe
  con ROM propia. Todo lo que pintamos hoy asume 256 por 192 más borde; esto es una segunda
  `Picture`.
- **Next**: 2M, un centenar de registros de los que ZEsarUX implementa una parte, Layer 2, sprites,
  tilemap, copper, DMA, cuatro velocidades hasta 28 MHz, y el **Z80N**, un Z80 con instrucciones
  que el nuestro no tiene. Agregar instrucciones toca el emulador y el generador de núcleo, que es
  lo más profundo que hay en este árbol. Y arranca de una imagen SD, no de una ROM.

Ninguno de los dos cabe en lo que hay ni en lo que este plan agrega. Si algún día se quieren, son
planes propios, y el del Next empieza por el procesador.

## Dónde está cada paso, al 14 de septiembre de 2026

Los pasos 1 a 7 están hechos, cada uno con sus hechos y con el árbol verde desde un repositorio
local vacío. Lo que se aprendió en el camino, que no estaba en este plan cuando se escribió:

- **La contención se apaga cuando una máquina corre más rápido de lo que fue construida.** El
  emulador del que se leyó todo esto apunta las tablas a unas llenas de ceros y ni las recalcula.
  Eso es lo que el chip que dibuja puede hacer de verdad, y decirlo dejó las tablas del tamaño que
  ya tenían - la alternativa era una tabla dieciséis veces más larga.
- **Un idioma no es una máquina.** Las variantes de ROM de las TK y de las Amstrad entraron como
  juegos a elegir sobre la misma máquina, con el mecanismo del paso 1, y no como entradas del menú.
- **Guardar un snapshot no lleva todavía los colores propios.** Cargarlo sí: quien abre un archivo
  pregunta por "lo que pinte con colores propios" y no por un chip con nombre. Lo que escribe un
  snapshot lo arma con el procesador y la memoria solos, y desde donde está no ve un dispositivo.
- **La suite necesita más heap del que da una caja chica por omisión**, y ahora el build lo dice.
- **Una ROM puede tardar ocho segundos en tomar su primera interrupción.** La Chrome los tarda, en
  bucles de espera, y el límite que les quedaba bien a las Sinclair la daba por muerta.

Con las ROMs traídas, las veinticuatro máquinas del build arrancan y toman su interrupción: 297
hechos en el módulo de máquinas, sin uno solo salteado. Sin ellas se saltean seis, que es lo que
corresponde a un árbol que no las lleva.

**Falta el 8 y el 9.** El ZX-Uno no es una máquina sino una caja de máquinas, y su cargador de ocho
K no muestra nada sin la imagen de flash de cuatro megas: traerlo a medias sería registrar una
máquina que no arranca, que es peor que no tenerla. El BaseConf empieza por el bit CMOS del Z80, que
es lo más profundo del árbol - el procesador y su generador -, y sigue por una paginación de cuatro
megas con tablas de mapas. Los dos son trabajo de una sesión entera cada uno, no de una madrugada.

## El orden, y por qué

1. Las ROMs elegibles para las máquinas que ya tenemos. Cero modelo; descubre si `choose` alcanza.
2. TK90X, TK95 y los CZ. Cuatro clases sin un hecho nuevo; mueven los conteos en cuatro.
3. La paleta como hecho de la máquina, y `UlaPlusPeripheral` sobre el 48K y el 128. El primer
   cambio de modelo, solo, con los snapshots que ya lo traen como prueba.
4. La velocidad como hecho que la máquina cambia. El segundo cambio de modelo, solo, sin máquina
   que lo use todavía: `UlaTimingsOverAFrameTest` tiene que quedar idéntico.
5. Chloe 280SE y 140SE, sobre `SpecSe`. Primera máquina que usa los dos cambios.
6. Inves. El asiento de la interrupción en `Cpu` y la ROM sobre RAM; es el que más hechos nuevos da
   por línea de código.
7. Chrome. `pageAt` y `romAt` de nuevo, y un +D a bordo.
8. ZX-Uno. Composición de todo lo anterior más SPI, Radastan, DMA y UART, y el cuadro elegido por
   registro.
9. BaseConf. El bit CMOS del Z80, la paginación de 4M.
10. TS-Conf, Prism y Next: cada uno con su plan, si se quieren.

Cada paso deja el árbol compilando desde un repositorio local vacío, con `mvn clean test` verde, y
va en su propio commit con sus hechos al lado. Las ROMs que no son nuestras no entran al árbol: van
por `sources`, con URL y hash, y los hechos que las necesitan se saltean solos mientras no estén.

## Los gates que ya tenemos, y para qué sirve cada uno

| suite | fija | la rompe |
|---|---|---|
| `PictureTest`, `ColouringTest`, `SixteenColoursTest` | la imagen de dieciséis colores y las dos reglas de pintado | el paso 3 si toca lo que no debe |
| `UlaTimingsOverAFrameTest`, `SpectrumTest`, `TimingsTest` | el retardo por T-state y los números de cada máquina | el paso 4 |
| `PagingTest`, `PagingReachesItsMachineTest` | los mapas por los puertos, máquina por máquina | Chrome y BaseConf |
| `ScldTest` | el registro Timex y sus modos | Chloe y Uno, que lo reusan |
| `TheInterruptLineTest`, `NmiAndPcTrapsTest` | que cada máquina toma su interrupción, y las trampas | el asiento del paso 6 |
| `GeneratedSpectrumZ80IsCurrentTest` y los 1916 del núcleo generado | que el núcleo generado sigue al modelo | el bit CMOS del paso 9, y cualquier cosa del Next |
| `CostRegressionTest`, `EmulationRegressionTest` | lo que cuesta un cuadro y lo que produce una grabación | una paleta que se decodifique por `if` en vez de por índice |
| `ARomThatIsNotShippedTest`, 10 | las ROMs que se traen de afuera | cada fuente nueva que se fije mal |
| `ResetReachesThePartsInOrderTest` | el orden del reset y la memoria en blanco al encender | un clon cuya ROM mire la RAM al arrancar; ya pasó con el Scorpion |

Y las dos que cuentan máquinas, `SnapshotChoosesItsMachineTest` y `EmulationRegressionTest`, se
mueven una vez por clon. Está bien que lo hagan.

## Lo que no hay que hacer

- **Un `if (ulaplus)` en `Painting` o en `Picture`.** La paleta se indexa; quién la llenó no se
  pregunta.
- **Confundir turbo con velocidad de emulación.** `Speed` es lo que pide el usuario; el turbo es lo
  que pide el programa. Un Chrome al 100 % corre a 7 MHz cuando el programa lo puso ahí.
- **Tocar el Z80 antes de BaseConf.** El bit CMOS es el primer motivo real; el Z80N no es motivo
  hasta que el Next tenga su plan.
- **Traer una máquina sin verificar su ROM.** Microdigital, Investrónica, el autor del Chrome,
  NedoPC y los del Prism: cada uno por su lado antes de fijar una URL. Lo que sí es seguro es el
  camino: `sources` con hash, y las URL de ZEsarUX en GitHub son fuentes publicadas y estables.
- **Modelar el TK90X a 60 Hz sin decidirlo.** Es una diferencia con el oráculo; se anota y se
  decide, no se cuela.
- **Empezar por el Next, por el Prism o por TS-Conf.** Son otros emuladores dentro de éste.
