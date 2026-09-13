# Las máquinas que faltan, y qué le cuesta a nuestro modelo cada una

Faltan cinco: TC2068, TS2068, SE, Pentagon 1024 y Scorpion. Este plan dice, para cada una, qué
hecho del hardware la distingue y en qué punto de **nuestro** modelo aterriza ese hecho. La regla
que lo ordena todo es la de siempre, extendida: el core no nombra ningún periférico, y tampoco
nombra ninguna máquina. Cada cambio en `machine/core` tiene que poder decirse sin decir "Timex".

Las máquinas viven en `machine/machines`. Pueden compartir un dispositivo, como comparten la ULA
o el AY, y pueden heredar de un modelo general, como el Pentagon hereda del 128. Lo que no pueden
es meterse en la implementación de otra máquina: ni un `if` por modelo dentro de una clase ajena,
ni una máquina que sepa que otra existe.

## Lo que se midió, el 13 de septiembre de 2026

Del otro lado, con las fuentes de referencia abiertas al lado:

| máquina | reloj | frame | contención | lo que la distingue |
|---|---|---|---|---|
| TC2068 | 3 500 000 | Timex 50 Hz, primer pixel 14321 | la del 48K | SCLD completa, dock y exrom por 0xf4, AY en 0xf5 y 0xf6 |
| TS2068 | 3 528 000 | Timex 60 Hz: 262 líneas, primer pixel 9169 | la del 48K | un TC2068 a 60 Hz con otras ROMs |
| SE | 3 500 000 | 312 líneas repartidas 47, 192, 48 y 25; primer pixel 14336 | la del 48K | paginación del 128 más dock y exrom, con los dos como RAM |
| Pentagon 1024 | 3 584 000 | el del Pentagon | ninguna | un segundo puerto en 0xeff7 y un modo de 16 colores |
| Scorpion | 3 500 000 | 224 por línea con bordes 24 y 32; interrupción 36 | ninguna | pagina por 0x7ffd y 0x1ffd con sus propios significados |

De nuestro lado, los puntos donde eso toca:

| pieza nuestra | qué es hoy | lo que las máquinas le piden |
|---|---|---|
| `ScreenLayout` | dónde vive un pixel y su color; ya contesta con dos archivos y color por línea | un tercer byte por columna y un par de colores fijo, para alta resolución |
| `Picture` | 320 de ancho, fijo, y un `plot8` como única entrada | dieciséis pixeles por columna cuando la máquina lo pide |
| `Painting.plotLine` | lee dos bytes por columna y pinta ocho | leer tres y pintar dieciséis, en una sola rama |
| `SpeccyScreen` | tiene el ancho escrito a mano, 256 más bordes | leerlo de la imagen |
| `MemoryBus.plug` | rangos arbitrarios que tapan los slots de 16K; lo usa el DivIDE para sus páginas de 8K | exactamente eso, ocho veces |
| `Spec128.pageAt` | qué página va en cada slot, sobreescribible; ya lo usa el Pentagon 512 | lo mismo para el Scorpion y el 1024, más un `romAt` gemelo |
| `AyPeripheral` | recibe máscara y valor de sus dos puertos | otra instancia cableada a 0xf5 y 0xf6 |
| `Machines.MODEL_NAMES` | una lista fija de nombres que el navegador usa antes de que exista una máquina | crece, o se deriva del conjunto que ya declaran los módulos |

Todo lo demás está: la contención que usan las tres Timex y el SE es la del 48K, `SIX_DOWN_TO_NOTHING`;
las 64 páginas del Pentagon 1024 entran en las 65 que `SpectrumMemory` ya tiene; el `absent()`
del 16K sirve para un dock vacío, que lee 0xff igual.

## El único cambio de modelo que importa: dieciséis pixeles por columna

Todo lo demás es dispositivos y clases de máquina. Esto no, y conviene decidirlo primero porque
las cuatro máquinas Timex dependen de ello.

**Cómo lo resuelve la referencia.** Su buffer es del doble de ancho siempre que la máquina es Timex,
no cuando el modo lo pide. En los modos normales escribe cada pixel dos veces; en alta resolución
junta dos bytes de bitmap en una palabra y pinta dieciséis. Los colores de alta no salen de la
memoria sino del registro: tres bits eligen uno de ocho pares fijos, convertidos a un byte de
atributo normal para que el resto del camino no se entere. Y su caché de redibujo lleva el modo en
la clave, así que cambiar de modo invalida todo sin código que lo diga.

**Las dos formas de traerlo acá.**

1. *Toda imagen es de 640.* `Picture.WIDTH` se duplica para todas las máquinas y `plot8` escribe
   cada pixel dos veces. Es el cambio de código más chico. Es también el de mayor radio: dobla las
   escrituras por frame de todas las máquinas, dobla la entrada de todos los escaladores, y
   `CostRegressionTest` fija justamente lo que cuesta un frame. Lo descarto por eso.
2. *El ancho es un hecho de la máquina.* La máquina declara cuántos pixeles dibuja por columna, ocho
   o dieciséis; la imagen se dimensiona con eso al elegirla; el panel deja de tener el 320 escrito a
   mano y lo lee de la imagen. Las ocho máquinas que ya existen no cambian ni un byte de salida.
   Cuesta un `plot16` en `Picture`, una rama en `plotLine` y que el panel envuelva de nuevo el arreglo
   cuando el ancho cambia. Es la que elijo.

**Qué le pregunta la pintura al layout, en esa rama.** Hoy le pregunta dos cosas: dónde están los
pixeles de una columna y dónde su color. Alta resolución agrega una tercera y cambia la segunda:
el segundo byte de bitmap está en el otro archivo a la misma dirección, y el color es el par fijo que
dice el registro. Ninguna de las cuatro preguntas nombra una máquina; la SCLD, que es un dispositivo,
es la que pone las respuestas.

Los ocho pares, como byte de atributo, todos con brillo y opuestos entre sí:

| bits 3 a 5 | atributo | par |
|---|---|---|
| 0 | 0x78 | negro sobre blanco |
| 1 | 0x71 | azul sobre amarillo |
| 2 | 0x6a | rojo sobre cian |
| 3 | 0x63 | magenta sobre verde |
| 4 | 0x5c | verde sobre magenta |
| 5 | 0x55 | cian sobre rojo |
| 6 | 0x4e | amarillo sobre azul |
| 7 | 0x47 | blanco sobre negro |

**Lo que se rompe si se hace mal.** `BorderTest`, `PictureTest`, `BorderComesFromThePortTest` y
`ScreenFollowsTheBeamTest` usan `Picture.WIDTH` y `plot8`. Si el ancho pasa a ser de la máquina, los
cuatro siguen valiendo tal cual para las máquinas de ocho, y son el gate de que nada cambió para
ellas. El de alta resolución es un hecho nuevo, en la SCLD.

## Cada máquina, en el orden en que conviene hacerlas

### 1. Alta resolución, sobre el TC2048 que ya está

El TC2048 hoy tiene la SCLD sin su bit 2. Se completa primero porque es la única máquina Timex ya
en el árbol, así que el cambio de modelo se prueba contra una máquina que ya tiene sus hechos.

- `Picture`: `plot16`, y el ancho como parámetro de construcción en vez de constante.
- `ScreenLayout`: `secondByteAt(line, column)` y un par de colores fijo cuando lo hay.
- `Painting.plotLine`: una rama, que pregunta al layout si una columna son dieciséis.
- `SpeccyScreen`: ancho y alto leídos de la imagen. Es un arreglo aparte del resto: hoy repite un
  número que el core ya sabe.
- `ScldPortHandler`: los bits 2 a 5 escriben lo de arriba en el layout.
- Hechos, en `ScldTest`: que con el bit 2 la columna cero muestra dieciséis pixeles armados de los dos
  archivos, y que los colores son el par de los bits 3 a 5 y no un atributo.

Gates: `core` entero, `machines` entero, y `PictureTest` y `BorderTest` sin tocar.

### 2. Dock y exrom, que es un dispositivo y no cambia nada en core

El puerto 0xf4 tiene ocho bits, uno por trozo de 8K. Un bit puesto pone en ese trozo el dock o el
exrom, según el bit 7 del registro de la SCLD; un bit quitado deja el mapa de la máquina. Eso es,
literalmente, `MemoryBus.plug` y `unplug` con ocho `MappedMemory` de 8K, que es lo que el DivIDE ya
hace con las suyas. Los trozos que caen entre 0x4000 y 0x7fff esperan al haz como la RAM de ahí.

- Un `TimexMemoryPeripheral` en el módulo `scld`, cableado a 0xf4, que enchufa y desenchufa. Qué
  hay en el dock y en el exrom se lo dice la máquina al activarlo: un cartucho, nada, o RAM.
- En el TC2068 y el TS2068 el dock es un cartucho o `absent()`, y el exrom es su ROM de 8K.
- Hechos: que un bit de 0xf4 tapa exactamente su trozo y ninguno más, que el bit 7 del registro elige
  cuál de los dos, y que un dock vacío lee 0xff.

Gates: `PagingTest` entero, los 68, y los de memoria de core.

### 3. TC2068 y TS2068

Con lo anterior son dos clases de máquina y un AY cableado distinto.

- `Tc2068 extends Tc2048`: exrom desde su segunda ROM, dock vacío, y un `AyTimexPeripheral` a bordo,
  que es `AyPeripheral` con `Wired.at(0x00ff, 0x00f5)` y `(0x00ff, 0x00f6)`, como el
  `AyPlus3Peripheral` ya es el mismo chip en otros puertos. Un hecho a fijar: en la referencia una
  lectura del puerto de datos contesta a veces como el de registro; se pregunta a la máquina real y
  se escribe lo que conteste.
- `Ts2068 extends Tc2068`: reloj 3 528 000, frame de 60 Hz, sus ROMs. La ROM del TS2068 no está en el
  árbol: hasta tenerla, no entra, por la misma razón que el Scorpion.
- Los frames nuevos van en la clase de la máquina, como el `TIMINGS` de cada una, y no en la lista de
  `MachineTimings`. `Frame` es un record público: cualquiera lo construye. El `TIMEX_SCLD_50HZ` que
  quedó en core se muda a `Tc2048` en el mismo paso, para que core no cargue con un nombre de máquina.

Gates: `SpectrumTest` con las dos en sus listas, `TimingsTest`, `ScldTest`, `SoundTest` con el AY.

### 4. SE

Es un 128 con dock y exrom, y con los dos hechos de RAM. Su mapa es el del 128 por 0x7ffd, tapado
por lo que 0xf4 enchufa, con una excepción: si en 0xc000 hay una página impar, los bits 2 y 3 de
0xf4 deciden además si dock y exrom van a 0xc000 y 0xe000. Y su mapa de casa tiene la página 8 en
0x8000, no la 2.

- `SpecSe extends Spec128`, con el `Spec128MemoryPeripheral` que ya existe para 0x7ffd, el
  `TimexMemoryPeripheral` para 0xf4, y la SCLD. Los dieciséis trozos de RAM de dock y exrom son
  de la máquina: `new Ram(0x2000)` cada uno, que la máquina le da al dispositivo al activarlo.
- La excepción vive en el `memoryMap()` del SE, que es suyo: compone el mapa del 128 y después
  enchufa lo que corresponde. No hay nada del SE en el 128 ni en la SCLD.
- ROMs se-0 y se-1 ya están.
- Hechos: los del 128 que ya existen, más tres propios: la página 8 en 0x8000, que dock y exrom
  retienen lo escrito, y la excepción de la página impar.

Gates: `PagingTest` con el SE en `paged()` y `pagedLikeA128()`.

### 5. Pentagon 1024, en dos mitades

La paginación es un `Pentagon512` con un puerto más, 0xeff7, cuyo byte cambia tres cosas: con el bit
2 la máquina pasa al modo 2.2, donde el bloqueo de 0x7ffd vuelve a valer y la aritmética de página
cambia; con el bit 3 pone la RAM 0 en 0x0000; y con el bit 0 entra el modo de 16 colores.

- `Pentagon1024 extends Pentagon512`: `pageAt` según el modo, un `romAt` para el bit 3, y el
  segundo puerto como dispositivo propio de la máquina. `Paging` no se toca: el "special" del 1024
  viene de otro puerto, así que lo decide su `memoryMap()`.
- Hechos: las 64 páginas por los tres caminos, el bloqueo que aparece en 2.2, la RAM abajo.

**Los 16 colores son otra cosa y van después de todo lo demás.** Ese modo lee cuatro bytes por
columna de dos páginas a la vez, la 5 y la 4 o la 7 y la 6, y no usa atributos: dos bitmaps se
combinan en cuatro bits por pixel. Nuestra pintura lee un banco, el que se muestra. Traer esto es
una regla de pintura nueva, "el color de una columna sale de cuatro bytes en dos páginas", y es el
cambio más profundo de este plan. Se hace último, solo, y con la máquina ya andando en los otros
modos: hasta entonces el bit 0 se lee, se guarda y se ignora, y el hecho que lo dice es que no está
soportado.

### 6. Scorpion, cuando haya ROM

Cabe entero en lo que hay: pagina por los dos puertos del +3 con otros significados. Bit 0 de 0x1ffd
pone RAM en 0x0000, bit 1 elige su ROM de servicio, bit 4 es el cuarto bit de página. Dieciséis
páginas, sin contención, Beta 128 a bordo.

- `Scorpion extends SpecPlus3` con `pageAt` y `romAt`.
- No entra hasta tener sus tres ROMs: una máquina que no arranca es peor que una que no está.

## El orden, y por qué

1. Alta resolución sobre el TC2048. Es el único cambio de modelo; se hace primero y solo.
2. Dock y exrom como dispositivo. Cero cambios en core; prueba `plug` en serio.
3. TC2068. Y el TS2068 el día que esté su ROM.
4. SE.
5. Pentagon 1024 sin 16 colores.
6. Scorpion, con ROM.
7. Los 16 colores del Pentagon 1024.

Cada paso deja el árbol compilando desde un repositorio local vacío, con `mvn clean test` verde, y
va en su propio commit con sus hechos al lado.

## Los gates que ya tenemos, y para qué sirve cada uno

| suite | fija | la rompe |
|---|---|---|
| `SpectrumMemoryTest`, 50 | el bus y las páginas | tocar `MemoryBus` o `MappedMemory` |
| `PictureTest`, `BorderTest`, `TheScreenLayoutTest`, `ColouringTest`, `DirtyCellsTest` | la imagen de ocho por columna | el paso 1 si cambia lo que no debe |
| `SpectrumTest`, 30, y `TimingsTest` | los números de cada máquina, en sus listas | una máquina nueva que no entró a las listas |
| `UlaTimingsOverAFrameTest`, 44 | el retardo en cada T-state de un frame | cualquier cambio en las esperas |
| `PagingTest`, 68 | los mapas por los puertos | el paso 2 y el 4 |
| `ScldTest`, 5 y creciendo | el registro y sus modos | el paso 1 |
| `GeneratedSpectrumZ80IsCurrentTest` y los 1916 sobre el núcleo generado | que el núcleo generado sigue al modelo | cualquier cambio en las fuentes del modelo: se regenera y se commitea |
| `CostRegressionTest`, `EmulationRegressionTest` | lo que cuesta un frame y lo que produce una grabación | la opción 1 de arriba; por eso no es |
| el bridge, 212 | lo mismo que el emulador de referencia | necesita la biblioteca instalada |

Y dos que ya avisaron esta semana y van a volver a avisar: `SnapshotChoosesItsMachineTest` cuenta
los modelos que un snapshot puede nombrar, y `EmulationRegressionTest` cuenta las máquinas
registradas. Cada máquina nueva los mueve en uno, y está bien que lo hagan.

## Lo que no hay que hacer

- **Un `if (timex)` en core.** Ni en `Display`, ni en `Painting`, ni en `MemoryBus`. Si un cambio en
  core necesita el nombre de una máquina para explicarse, está en el lugar equivocado.
- **Duplicar el ancho para todos.** Rompe lo que mide el costo por frame y toca todos los escaladores
  para dar un modo que tres máquinas usan.
- **Empezar por los 16 colores del Pentagon 1024.** Es lo más profundo y lo menos usado.
- **Traer una máquina sin su ROM.** El TS2068, el Scorpion y el 128Ke esperan. El 128Ke, de hecho,
  no espera nada: del otro lado tampoco es una máquina, es un identificador de formato.
- **Tocar `Paging`.** Las tres formas que tiene son las tres formas que hay; el 512, el 1024 y el
  Scorpion son aritméticas de la máquina, y `pageAt` y `romAt` son su lugar.
