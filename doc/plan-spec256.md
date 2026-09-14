# Spec256: qué es, qué le cuesta a nuestro modelo, y cómo entra sin tocarle la velocidad a nada

Spec256 fue un emulador de DOS de 1999 que corría juegos de 48K en 256 colores sin colour clash:
los gráficos originales se reemplazan por una versión de 8 bits por pixel que viaja por el mismo
código del juego, sin modificarlo. Este plan dice qué es exactamente, en qué punto de **nuestro**
modelo aterriza cada parte, cuánto cuesta, y cómo se hace para que el core no sepa que Spec256
existe y para que la velocidad de todo lo demás quede intacta. Las dos condiciones son la misma
condición: si el core no lo nombra, el core no lo paga.

## Lo que se midió, el 14 de septiembre de 2026

**Los oráculos.** El Spec256 original es cerrado y de DOS; EmuZWin, que heredó el formato, es
cerrado y de Windows; ZEsarUX no lo implementa (no hay una línea sobre él en sus fuentes). Hay dos
implementaciones abiertas. La de GZX (Jiří Svoboda, licencia tipo MIT): `z80g.c`, 267 líneas, y
`video/spec256.c`, 330, y GZX mismo dice "not 100% done". Y la de ZX-Poly (Igor Maznitsa, Java,
GPL-3), que es la completa: lee los `.CFG` de EmuZWin, sabe de 128K, y trae una base de 24 juegos
conocidos con el ajuste que cada uno necesita. Lo que sigue se leyó de las dos y se comprobó contra
los archivos de un juego real, el Cybernoid del repositorio `mvvproject/Spec256-Games`. De ZX-Poly
no se copia una línea, por la licencia: se lee como especificación, y la sección [ZX-Poly contra
el plan](#zx-poly-contra-el-plan) dice qué cambió por leerla.

**Los archivos de un juego.** Un `.SNA` o `.Z80` de 48K, y a su lado, con el mismo nombre, un
`.GFX` de exactamente 393 216 bytes: 49 152 direcciones de RAM por ocho bytes, **un byte de color
por pixel**, el pixel de la derecha primero. Opcionalmente un `ROM0.GFX` con lo mismo para la ROM,
y fondos `.B00`, `.B01`… de 64 000 bytes: imágenes de 320 por 200 de un byte por pixel, centradas
bajo la pantalla, que se ven donde el color es cero. La paleta es fija, 256 entradas RGB, y viene
con el emulador y no con el juego: GZX y ZX-Poly traen los mismos 768 números salvo la entrada
255, roja en GZX y blanca en ZX-Poly. Es blanca: 255 es el color que sale de un byte sin colorear,
porque ocho planos iguales dan todos los bits donde el bitmap está encendido, y las reglas de
EmuZWin la tratan como "la tinta". Los `.CFG` son texto `clave=valor` de EmuZWin con las reglas de
mezcla, y ZX-Poly los lee. Los `.EZX` son el contenedor propio de EmuZWin (`EZX\0`, comprimido),
nadie abierto lo lee, y siete de los 37 juegos del repositorio vienen sólo así: Dizzy 2, Exolon,
Gun Runner, Head Over Heels, Highway Encounter, Mad Mix 2 y Pac Mania. De los otros treinta, 29
traen `.SNA` más `.GFX`, 18 traen `.CFG`, 15 fondos y 3 un `ROM0.GFX`; ninguno trae paleta.

**Cómo se leen los ocho bytes.** GZX no los guarda así: los rebana en **ocho planos**. El plano *v*
guarda, en cada dirección, un byte cuyo bit *w* es el bit *v* del color del pixel *w*. Un plano es
entonces una memoria de 64K con la forma de la memoria de un Spectrum, y ésa es toda la idea.

**Lo que dice el Cybernoid**, muestreando 16 384 direcciones de su `.GFX`:

- en 12 870 los ocho planos son **idénticos al byte original**: es código y datos, codificados de
  modo que cada plano reproduzca el byte;
- en 3 514 difieren: son gráficos;
- en la pantalla, los 11 706 pixeles encendidos del bitmap tienen color distinto de cero, sin
  excepción, con el orden "el byte 7 es el pixel de la izquierda"; con el orden contrario sólo el
  80 %. El orden está confirmado;
- 2 421 pixeles *apagados* del bitmap tienen color: GZX los dibujaría, porque su pantalla no mira
  el bitmap original. Es una de las dos cosas que este plan deja como pregunta.

**Nuestros números**, 300 cuadros con la imagen apagada, el mejor de cinco bloques:

| núcleo | ms por 300 cuadros | veces el tiempo real |
|---|---|---|
| generado | 29 | ~200 |
| OOP | 105 a 114 | ~55 |
| **en paso, los nueve** | **1825** | **3,3** |

Trescientos cuadros son seis segundos de máquina. La última fila se midió el 14 de septiembre con
el núcleo ya escrito, el mejor de cinco bloques de 300 cuadros sobre un 48K arrancado, imagen
apagada, dos núcleos fijados: **6,08 ms por cuadro, tres veces y media el tiempo real**. Son 14,8
veces el OOP solo y no nueve: el resto lo ponen la memoria del plano, que es otro sitio de llamada,
y lo que se le copia a cada seguidor en cada instrucción. Sobra para correr un juego a velocidad
real, y sólo se paga mientras hay un juego Spec256 cargado.

## Cómo funciona, leído de GZX

Nueve procesadores. Uno es el Z80 de la máquina, sobre su memoria, y es el único que existe para
todo lo demás: puertos, contención, interrupciones, sonido, cinta. Los otros ocho son *GPUs*: un
Z80 cada uno sobre su plano, sin puertos y sin reloj.

Por cada instrucción:

1. A cada GPU se le copian del CPU el PC, SP, I, R, IFF1, IFF2, el modo de interrupción, si está
   parado, y **todas las banderas menos el acarreo**. Sus registros de datos - A, BC, DE, HL, los
   alternativos, IX, IY - no se tocan: son suyos, y llevan colores.
2. Cada GPU ejecuta la instrucción que hay en ese PC. GZX la lee de su propio plano, que en las
   zonas de código es copia del original. ZX-Poly la lee de **la memoria de la máquina** —para un
   GPU, todo byte que se lee por el PC es código y viene de ahí; todo lo que se lee o escribe por
   una dirección es dato y va a su plano— y así el GPU decodifica siempre la misma instrucción que
   el CPU, aun cuando el código lo generó el juego en tiempo de ejecución y en los planos hay ahí
   colores.
   <p>
   **La nuestra es una tercera, y es la que el modelo ya sabía distinguir.** `Memory.read(address,
   fetching)` marca el opcode y el prefijo, y nada más: el plano manda **eso** a la máquina y todo
   lo demás —incluidos los operandos inmediatos, que GZX lee del plano y ZX-Poly de la máquina— a
   sus propios bits. Un GPU nunca decodifica otra instrucción que el CPU, que es lo que ZX-Poly
   arregló, y un inmediato que el juego coloreó sigue llevando color, que es lo que GZX permite.
   Los dos casos que importan salen bien: código generado en ejecución se decodifica igual en los
   nueve, y una rutina que se automodifica —que escribe su propio operando y después lo ejecuta—
   escribe un color en su plano y lo usa, que es exactamente para lo que se automodifica un
   ploteador de sprites. Un inmediato que alimenta el PC no puede desviar a nadie: el PC se
   realinea antes de la siguiente instrucción.
3. El CPU la ejecuta.

Una interrupción se toma en los nueve. Un GPU no puede desviarse del flujo del CPU aunque calcule
otra bandera: al empezar la siguiente instrucción el PC vuelve a ser el del CPU. Lo único que le
queda propio es lo que movió: un `LD A,(HL)` seguido de `LD (DE),A` lleva, en cada plano, el bit
del color que ese plano guarda; un `LDIR` copia un sprite con sus 256 colores; un `AND` con una
máscara deja pasar los bits del color donde la máscara los deja; una suma corrompe, y se acepta.

**La pantalla.** El color del pixel *i* de la dirección *u* es el byte formado por el bit *7-i* del
plano 0, 1, … 7 en *u*. Si da cero se ve el fondo, y si no hay fondo, negro. Los atributos no se
miran. El borde es el de la ULA de siempre. GZX dibuja el cuadro entero a 50 Hz; nosotros ya
pintamos siguiendo el haz, así que quedaríamos mejor sin hacer nada.

**Puertos en los GPUs.** No los tienen. En GZX un `IN` en un GPU devuelve 0xff y un `OUT` no hace
nada; ZX-Poly le deja leer el puerto real, con la dirección de puerto tomada de los registros del
CPU. Da lo mismo: lo que lee un puerto nunca es un color, y el flujo lo lleva el CPU. Lo que no da
lo mismo es que un `OUT` de un GPU no llegue jamás al bus —lleva colores en A, y el puerto puede
ser el borde o la paginación—, y un `IO` que no contesta lo garantiza sin preguntar nada.

**Sólo 48K.** GZX se niega con otro modelo; el formato crudo es de 49 152 bytes y no sabe de
páginas. ZX-Poly lo extiende con un plano por página (`.gf0` a `.gf7`) que sigue al 7FFD y con
`rom0.gfx`, `.gfa` y `.gfb` para las ROMs, dentro de un `.zip`. Ningún juego del repositorio lo
usa; queda para cuando uno lo use.

## Dónde aterriza en nuestro modelo

| pieza nuestra | qué es hoy | lo que Spec256 le pide | cambio en core |
|---|---|---|---|
| `Core`, `Processors` | un conjunto de implementaciones del procesador (`Multibinder<Core>`), cada una llega en su módulo por `Extension`, y `Processors.use` mueve una máquina que corre a otra | una implementación más, "Spec256": el OOP nueve veces en paso | **ninguno** |
| `OOZ80`, `Cpu` | `Cpu` sólo le pide al procesador `execute()`, `interruption()`, `nmi()`, `reset()` y `getState()`; `OOZ80` es una clase abierta con esos cuatro públicos | una subclase que es el OOZ80 del CPU con ocho seguidores y contesta esos cuatro en nueve | **cinco líneas**: un constructor de copia protegido en `OOZ80`, para que la subclase sea el mismo procesador y no otro armado en paralelo, y que `OopCore` acepte que nadie le cuente el tiempo —un procesador que no está en una máquina—. `getState()` sigue siendo el del CPU, y con él las trampas, el RZX, la contención y `takeFrom` |
| `State`, `Memory` | un `State` sobre una `Memory` y un `IO`; `State(IO, Memory)` existe; `Memory.read(address, fetching)` ya distingue el fetch de un opcode del dato, y `peek` lee sin avisar a nadie | ocho estados sobre ocho planos con un `IO` que no contesta; cada plano sirve el código de la máquina y los datos suyos | **ocho líneas, y son un arreglo**: `ContendedMemory`, que es la memoria que recibe el procesador, no contestaba `peek` ni `poke` —caían en el `getData()` vacío de `Memory`, que tira `ArrayIndexOutOfBounds`— y delegarlos a lo que envuelve es lo que prometen. Sin eso cada fetch de un seguidor tiraba una excepción que el procesador se tragaba, e imprimía una traza: el árbol andaba, pero a paso de hombre |
| `Picture.COLOURS` y sus búsquedas | 64 colores, índices `byte` | 256, e índices que no se vuelvan negativos | **dos líneas**: 256 y `& 0xff` al buscar |
| `Painting.plotLine` | tres reglas, una por bandera del `ScreenLayout` | una cuarta que no lee la memoria de la máquina sino ocho planos que viven en otro lado | **un asiento**: quién pinta una columna, si alguien lo dijo |
| `Snapshots.load(url)` | conoce la ruta y no se la dice a nadie | que alguien sepa de qué archivo vino un snapshot, para mirar al lado | **un asiento**: a quién avisar |
| `SpectrumMemory` | 65 páginas | nada: los planos no son páginas de la máquina | ninguno |
| las máquinas | veinticuatro | nada: es un juego de 48K en un 48K | ninguno |
| el escritorio | ventanas por `Equipment` en `META-INF/services` | una ventana más | ninguno |

Tres asientos en core y un arreglo, ninguno con la palabra Spec256, ninguno en un camino caliente
de nadie. El arreglo es el `peek` de `ContendedMemory`, que hoy no cumple el contrato de `Memory`;
los asientos:

### 1. La paleta tiene 256 entradas

`Picture.COLOURS` pasa de 64 a 256 y `sinclairColours()` sigue llenándola con los dieciséis
repetidos. Los índices son `byte` en `plot8`, `plotPair`, `plot16` y `Colouring`: con 256 colores
un índice de 128 para arriba es negativo. Se enmascara al buscar, `palette[ink & 0xff]`, que es una
operación por celda y no por pixel y no cambia ninguna firma. ULAplus ya llena la paleta por
índice; Spec256 llenaría las 256 igual. `CostRegressionTest` y `PictureTest` dicen si algo se movió.

### 2. Una columna la pinta quien diga que la pinta

`Painting` tiene tres ramas y todas leen `banks.shown()`. La cuarta no puede: sus datos no están en
la memoria de la máquina. El asiento es un `ColumnPainter` opcional - "dame la línea y la columna y
yo pinto sus ocho pixeles en el lienzo" - que `Painting` consulta antes de sus ramas, y que un
dispositivo instala al activarse y retira al desactivarse. Nombra a nadie: un dispositivo que sepa
cómo son sus pixeles es un concepto, no una marca. El seguimiento del haz y las celdas sucias
siguen siendo de `Painting`: una escritura del CPU a la pantalla ensucia la celda, y como los GPUs
escribieron sus planos en la misma instrucción, la celda se pinta con planos al día.

### 3. Un snapshot dice de qué archivo vino

`Snapshots.load(String url)` gana una lista de oyentes a los que les dice la ruta después de
cargar. Quien quiera mirar si hay un `.GFX` al lado, mira. Es el mismo asiento que necesitaría
cualquier cosa que acompañe a un archivo - un `.pok` con el mismo nombre, por ejemplo - y no sabe
de ninguna.

## El módulo `spec256`, donde vive todo lo demás

Un módulo bajo `machine/devices`, como los demás dispositivos que traen ventana, que aporta por
`Extension` tres cosas:

- **`Spec256Core`**, un `Core` más. Su `cpu(state, contention)` construye el OOZ80 de siempre sobre
  el estado de la máquina y ocho OOZ80 sobre ocho `Plane` - una `Memory` de 64K cada uno que da
  el código de la máquina cuando `fetching != 0` y sus propios bits cuando no, sin contención y con
  un `IO` que devuelve 0xff - y devuelve un `LockstepZ80`: el OOZ80 del CPU, construido como lo
  construye `OopCore`, con los ocho adentro. Hereda, y redefine lo único que la máquina le pide a
  un procesador: `execute()` hace los tres pasos de arriba, `interruption()` y `nmi()` las toman
  en los nueve, `reset()` resetea a los nueve; `getState()` sigue devolviendo el estado del CPU, y
  por eso `Cpu`, las trampas, el RZX y `Processors` no notan nada. Un GPU nunca toma una
  interrupción por su cuenta: su línea INT no la levanta nadie, y la toma cuando la toma el CPU.
  Lo que se le copia a cada GPU antes de su paso lo dice un `Alignment`: por omisión PC, SP, I, R, IFF1, IFF2, IM, parado y F menos el
  acarreo; por juego, lo que su `.CFG` diga en `zxpAlignRegs`, que admite además A, BC, DE, HL,
  IX, IY y los alternativos —15 de los 24 juegos de la base de ZX-Poly se apartan del valor por
  omisión. Al entrar en
  el núcleo los ocho estados arrancan como copias del CPU con `State.takeFrom`, que ya existe, y lo
  hacen en su primer `execute()`, no al construirse: `Processors.runOn` pasa los registros del
  procesador anterior *después* de construir el nuevo.
  Ningún oyente de contención en los GPUs: los T-states los cuenta el CPU, una vez. El PC del CPU
  sigue siendo el que ven las trampas, el depurador y el RZX.
- **`Spec256Peripheral`**, un dispositivo sin puertos. Al activarse instala su `ColumnPainter` y la
  paleta; al desactivarse los retira y pide `refreshAll`. Escucha a `Snapshots`: si al lado del
  snapshot hay un `.GFX`, carga los planos, los fondos y el `ROM0.GFX` si está - y si no está,
  llena los planos de ROM con la ROM misma, que es exactamente la codificación de "no es gráfico"
  y deja a los GPUs ejecutar sus rutinas en paso -, mueve la máquina al núcleo Spec256 y recuerda
  en cuál estaba; si hay un `.CFG`, lee de él el `Alignment` y las reglas de mezcla, y si no hay,
  las de EmuZWin por omisión. Un snapshot sin `.GFX`, un cambio de máquina o un reset la devuelven
  a ese.
- **`Spec256Equipment`**, la ventana: qué `.GFX` está cargado, cuántos fondos hay y cuál se ve, los
  256 colores, y un interruptor "256 colores / los originales" que es lo que EmuZWin tiene en F2.
  Es la ventana de ULAplus de ayer con otros datos adentro.

## Las reglas del `.CFG`, leídas de ZX-Poly

Son las de EmuZWin, y ahora tienen oráculo abierto. ZX-Poly implementa estas y sólo estas:

| clave | por omisión | qué hace |
|---|---|---|
| `BkOverFF` | 0 | con fondo, el color 255 también deja ver el fondo, no sólo el 0 |
| `Paper00InkFF` | 0 | el color 0 se pinta con el papel del atributo y el 255 con su tinta: un gráfico sin colorear se ve como en el Spectrum, no negro y blanco |
| `HideSameInkPaper` | 1 | donde tinta y papel del atributo son iguales, se pinta ese color, o el fondo si lo hay: es cómo el juego borra |
| `UpColorsMixed`, `DownColorsMixed` | 64, 0 | los colores por encima de `255-Up` y por debajo de `Down` se promedian en RGB con la tinta o el papel del atributo, según el bit del bitmap original |
| `GFXLeveledXOR`, `GFXLeveledOR`, `GFXLeveledAND` | 0 | en los GPUs la operación no es bit a bit: `OR` es `max`, `AND` es `min`, `XOR` es `max` salvo `XOR A,A`, que da 0. Ojo con qué se compara: el máximo es **del byte del plano**, no del color del pixel. Con un solo pixel por byte da lo mismo que el `OR`; la diferencia aparece cuando el byte lleva varios, y entonces un dibujo tapa al otro en vez de sumarse |
| `UseBrightInMix` | 0 | si la mezcla usa los ocho colores brillantes cuando el atributo dice brillante, o los apagados igual. ZX-Poly no lo lee; la captura del Cybernoid dice que no los usa |
| `zxpAlignRegs` | `1PSsT` | qué registros toma el GPU del CPU antes de cada instrucción; suyo, no de EmuZWin |

Y además: con el atributo en FLASH y la fase activa, se ve el fondo. Quedan sin leer `BkMixed`,
`BkMixBkAttr`, `UpMixChgBright`, `DownMixChgBright`, `UpMixPaper`, `DownMixPaper`,
`GFXScreenXORbuffered` y `OrderPaletteSignedBytes`, que también están en los `.CFG` del
repositorio. De los 18 que hay, lo único que se aparta de los valores por omisión es
`BkOverFF=1` en 17, y `UpColorsMixed=1` con `UpMixChgBright=50` en 9: alcanza para todos ellos
salvo por el brillo de la mezcla. El bitmap original **sólo** se mira para elegir tinta o papel en
la mezcla; un pixel apagado con color se pinta, en GZX y en ZX-Poly.

**Cómo se comprobó la mezcla.** La pantalla del título del Cybernoid, que no trae `.CFG` y por lo
tanto corre con los valores por omisión, contra la captura que el repositorio trae de ella. Sin las
reglas: 48 616 de los 49 152 pixeles idénticos, y los 536 que no lo eran, todos del color 246, a 18
y 96 de distancia por canal. Con las reglas: los mismos 536, pero todos a **7 de distancia**, que
es exactamente la diferencia entre el blanco apagado de esta máquina (178) y el del emulador que
sacó la captura (192) entrando en la mezcla. Es decir: la regla es la de arriba, `UpColorsMixed=64`
con `UseBrightInMix=0`, y lo único que queda es un tono de gris que es nuestro y no de Spec256.

## Qué queda como pregunta

- **El `T` de `zxpAlignRegs`.** Con `T`, que está en el valor por omisión de ZX-Poly, un GPU usa
  para *direccionar* los punteros del CPU —HL, DE, BC, IX, IY, SP, y B o BC como cuenta en los
  bloques— y conserva los suyos como *valor*: un HL que una suma de colores corrompió no lo lleva
  a otra dirección, y `LD HL,(a); LD (b),HL` sigue moviendo dos bytes de color. Distinguir el uso
  del valor sólo se puede dentro del procesador; ZX-Poly lo hace con ganchos del bus y un `ctx`.
  Nuestro procesador arma las referencias `(HL)` en el decodificador (`OpcodeTargets`), no en la
  fábrica, así que hoy no hay asiento. Sin `T`, lo que hay es alinear HL como valor, que diez de
  los 24 juegos de la base de ZX-Poly necesitan de todos modos; y de esos 24, los quince que fueron
  ajustados a mano apagaron `T`: sólo sobrevive en los nueve que quedaron con el valor por omisión.
  Se decide en el paso 9, con los juegos: si alguno lo pide, el asiento OOP es que `InstructionFactory` arme las referencias
  indirectas, y la fábrica de un GPU las armaría sobre los registros del CPU.
- **El brillo de la mezcla.** `UpMixChgBright=50` está en nueve `.CFG` y ningún oráculo abierto
  lo implementa. Se ve contra las capturas del repositorio si se nota.
- **Qué se le copia a un GPU además de lo alineado.** MEMPTR y la bandera Q, que GZX no tiene y
  nosotros sí: al entrar, con el estado entero; por paso, sólo lo que diga el `Alignment`. Un
  hecho lo dice.

Y dos que ya no lo son. **Los pixeles apagados con color** se pintan; el bitmap sólo elige tinta o
papel para la mezcla, y los 2 421 del Cybernoid son la imagen del `.GFX`, que es más rica que el
bitmap. **`Processors.use` en caliente** conserva los registros: `runOn` hace `State.takeFrom` del
estado anterior, así que la sesión arranca sin resetear el juego.

## El orden, y por qué

1. **La paleta a 256.** Core, agnóstico, dos líneas. `CostRegressionTest` y el A/B de siempre
   dicen que nadie pagó nada. Se hace primero y solo porque es el único cambio que toca la
   velocidad de todos, y así se mide aislado.
2. **El asiento de la columna.** Core, agnóstico. Un hecho que diga que sin nadie sentado las
   tres reglas pintan byte a byte lo mismo que antes; los tests de imagen que ya existen lo dicen.
3. **Los planos y el lector de `.GFX`.** Módulo, puro. Hechos con datos sintéticos: ocho bytes por
   dirección, el orden de los pixeles, que un byte que no es gráfico da ocho planos iguales al
   original, que 393 216 es el único tamaño que se acepta.
4. **El núcleo en paso.** Módulo. Hechos sin ningún juego: que un `LD` mueve un color plano a
   plano, que un `LDIR` copia un sprite entero, que un `AND` con máscara deja pasar los colores
   donde debe, que un `IN` en un GPU no lee nada y un `OUT` no llega al bus, que un salto que el
   CPU toma por un puerto lo toman los nueve, que una interrupción llega a los nueve, que los
   T-states se cuentan una vez, que un GPU cuyo plano tiene colores donde el CPU escribió código
   ejecuta el código igual, y que el `Alignment` por omisión deja HL suyo y uno que diga `HL` no.
   Y el número: cuánto cuesta un cuadro en este núcleo, medido y escrito.
5. **El asiento del snapshot y la sesión.** Core, agnóstico, más el dispositivo. Hechos: un
   snapshot con `.GFX` al lado enciende la sesión y uno sin ella no; la sesión devuelve la máquina
   al núcleo en que estaba; un cambio de máquina la termina.
6. **La pantalla.** El `ColumnPainter` del módulo, los fondos, el color cero. Hechos con planos
   sintéticos: ocho pixeles con ocho colores de una columna, el fondo donde el color es cero, el
   borde de la ULA intacto.
7. **Las reglas del `.CFG`.** El lector del `.CFG` y las seis reglas de la tabla, en el pintor y en
   la fábrica de instrucciones de los GPUs. Hechos con un `.CFG` sintético por regla: un pixel de
   color 255 que con `Paper00InkFF` es la tinta del atributo y sin ella es la entrada 255; un `OR`
   que con `GFXLeveledOR` da el mayor y sin ella el bit a bit. El `Alignment` es una regla más.
8. **La ventana.**
9. **Contra los juegos.** A mano, con el repositorio del usuario, comparando con las capturas que
   trae. Primero uno sin `.CFG` y con fondo, el Cybernoid; después uno con `.CFG` y fondo, el
   Bruce Lee o el Scooby Doo, y uno con `.CFG` sin fondo, el Atic Atac o el Renegade. Head Over
   Heels y Exolon no sirven: vienen sólo en `.EZX`. Ahí se decide lo del `T` y se ve qué juegos no
   arrancan bien.

Cada paso deja el árbol verde desde un repositorio local vacío y va en su commit con sus hechos.

## Los gates que ya tenemos, y para qué sirve cada uno

| suite | fija | la rompe |
|---|---|---|
| `CostRegressionTest` | lo que cuesta un cuadro en el núcleo de siempre | el paso 1, si la máscara costara algo; nada de lo demás lo toca |
| `PictureTest`, `SixteenColoursTest`, `ScldTest`, `UlaPlusTest` | las cuatro formas de pintar que hay hoy | el paso 2, si el asiento cambiara alguna |
| los 1916 del núcleo generado | que el generado sigue al modelo | nada: Spec256 no pasa por el generado ni lo regenera |
| `TheInterruptLineTest` | que cada máquina toma su interrupción | el paso 4, si el núcleo en paso la tomara de más o de menos |
| `SnapshotChoosesItsMachineTest` | qué máquina elige un snapshot | el paso 5, si la sesión se metiera en la elección |
| el A/B de reloj de pared, con dos árboles y repositorios aislados | la velocidad real | el paso 1; después, el número del paso 4 se mide con él |

## Lo que no hay que hacer

- **Un `if (spec256)` en `Painting`, en `Picture` o en `OOZ80`.** Cada uno tiene su asiento y el
  asiento no sabe quién se sienta.
- **Tocar el núcleo generado.** Ni para leerlo: la sesión corre sobre el OOP, y cuando termina la
  máquina vuelve a donde estaba.
- **Reflejar las escrituras del CPU en los planos.** Es el modelo equivocado: los GPUs escriben
  sus planos ellos mismos, ejecutando la misma instrucción. Un reflejo daría planos con bytes de
  un solo color donde el juego escribió datos, que es justo lo que los ocho bytes por dirección
  evitan.
- **Ensanchar todos los índices de color a `int` por esto.** Una máscara al buscar hace lo mismo
  sin tocar una firma.
- **Traer al árbol un `.GFX` de esos juegos.** Son trabajo del equipo Spec256 y de la comunidad de
  EmuZWin, sin licencia escrita; y los `.SNA` de Ultimate y Codemasters ni siquiera se
  distribuyen con ellos, lo dice el propio repositorio. Los hechos usan planos sintéticos; la
  comprobación contra juegos es a mano y contra la copia del usuario.
- **Un `ctx` en el procesador.** ZX-Poly pasa un entero por cada acceso a memoria y a puerto, y su
  bus pregunta `ctx == 0` en cada rama; su Z80 sabe que puede ser un GPU. Acá el objeto `Memory`
  de cada GPU *es* el contexto, y el procesador no sabe que hay nueve.
- **Copiar de ZX-Poly.** Es GPL-3 y este árbol es Apache-2.0. Se lee como especificación de las
  reglas y del formato, y el código es nuestro.

## Lo que sí se puede llevar

La paleta: 768 números de `sp256.pal`, publicados por GZX bajo su licencia tipo MIT, declarados en
el `NOTICE`, con la entrada 255 en blanco por la razón de arriba. Un `ROM0.GFX` no hace falta
llevarlo: los planos de la ROM se calculan de la ROM, y ahora sólo importan para los datos de la
ROM, como la fuente, porque el código lo leen de la máquina. Y lo que el repositorio de juegos
trae de valor para nosotros son sus capturas, que son el oráculo visual del paso 9, y no se copian:
se miran.

## Dónde está cada paso

Al 14 de septiembre de 2026, con el árbol verde desde un repositorio local vacío en cada uno.

| paso | commit | qué dejó, y qué enseñó que el plan no sabía |
|---|---|---|
| 1. La paleta a 256 | `6ed40f795` | Dos líneas y una máscara por celda: pintar una pantalla entera cuesta 0,021 ms con paleta de 64 y con paleta de 256, medido. Lo que no estaba previsto: ULAplus usaba `Picture.COLOURS` para decir cuántos registros tiene. Eran los dos 64 y no son el mismo número; ahora dice 64 él |
| 2. El asiento de la columna | `af4e9d9cf` | `Painting.PixelsOfItsOwn`, consultado antes de las tres reglas, instalado y retirado como `Colouring.Reading`. El haz y las celdas sucias siguen siendo de `Painting`: al asiento se le ofrece una columna sólo cuando está sucia |
| 3. Los planos y el `.GFX` | `f7fdd7837` | `Planes` y el orden del archivo en un solo lugar. Leído contra el Cybernoid real da los mismos 11 706 y 2 421 que el plan había anotado antes de que hubiera código. Los planos de la ROM no se calculan ni se copian: por debajo de 0x4000 el plano devuelve el byte de la máquina, que ya es lo que ocho planos iguales dirían, y así la paginación no se piensa |
| 4. El núcleo en paso | `ba8e1048c` | `LockstepZ80`, `Alignment`, `Spec256Core`. El `peek` de `ContendedMemory` era un hueco de verdad y sin él no arranca. El costo: 6,08 ms por cuadro, 3,3 veces el tiempo real |
| 5. El snapshot y la sesión | | `FilesOfItsOwn` en core: `Snapshots` le dice a quién guarda archivos al lado de dónde vino el snapshot, y no nombra a nadie. `Spec256Peripheral` es la sesión. Los hechos escriben un `.SNA` de 48K y un `.GFX` sintéticos en un directorio temporal, así que son de verdad y no traen nada de nadie |
| 6. La pantalla | | El pintor de columnas, la paleta de 256 y los fondos, todo en `Spec256Peripheral`: la sesión es la que tiene pixeles propios. Cuatro `plotPair` por celda, que es el mismo camino que ya pinta un Timex de color por byte; ninguna función nueva en `Picture`. La dirección que indexa los planos es la de la máquina, no el desplazamiento dentro de la página |
| 7. Las reglas del `.CFG` | | `Rules` lee el archivo del juego; el pintor las aplica y `LevelledInstructions` da a los seguidores un `OR`, `AND` y `XOR` por nivel, sin tocar el emulador: `doExecute` de una instrucción es `protected`, así que una subclase deja las banderas que la operación de siempre pone y cambia sólo lo que se escribe. Lo que la mezcla usa son los dieciséis colores de la máquina, que es con lo que la máquina habría pintado ahí |
| 8 y 9 | | pendientes |

## ZX-Poly contra el plan

Leído el 14 de septiembre de 2026. Lo que cambió el plan está marcado; lo demás confirma lo que
ya decía o queda para después.

| en ZX-Poly | en el plan | qué pasó |
|---|---|---|
| un GPU lee por el PC de la memoria de la máquina y por dirección de su plano (`ctx == 0 \|\| cmdOrPrefix`) | los GPUs ejecutaban lo que había en su plano, como GZX | **cambió**: el `Plane` manda `fetching != 0` al `peek` de la máquina. Un GPU nunca decodifica otra instrucción que el CPU, y los planos ya no tienen que reproducir el código. Cuesta cero en core: `Memory.read(address, fetching)` ya existía |
| un `Z80` con `ctx` en cada acceso y un bus con `if (ctx == 0)` | ocho `State` sobre ocho `Memory` | igual: la `Memory` de cada GPU es el contexto |
| `alignRegisterValuesWith(cpu, flags)` con `zxpAlignRegs` por juego, por omisión PC, SP, F menos C, y siempre I, R, IFF, IM, prefijo | se copiaba una lista fija, la de GZX | **cambió**: es un `Alignment` configurable, leído del `.CFG`, con la lista de GZX por omisión |
| `T`: los punteros para direccionar vienen del CPU, por ganchos del bus (`readPtr`, `readSpecRegValue`) | no existía | pregunta abierta: no hay asiento sin tocar el decodificador; se decide con los juegos |
| `postProcessXor/And/Or` en el bus, por `ctx` | no existía | **cambió**: la fábrica de instrucciones de los GPUs devuelve `And`, `Or` y `Xor` por nivel; `InstructionFactory.And(source)` es el asiento, y `DefaultInstructionFetcher` acepta la fábrica |
| `fillDataBufferForSpec256VideoMode`: las reglas de la tabla, contra tinta y papel del atributo | "las reglas de EmuZWin se hacen después o no se hacen" | **cambió**: son el paso 7, con hechos por regla |
| `readGfxVideo` vuelve a armar los ocho bytes por dirección desde los planos, cada cuadro entero | el `ColumnPainter` hace lo mismo por celda sucia, siguiendo el haz | igual, y mejor |
| planos entrelazados, `(dirección << 3) + plano`, un solo arreglo | ocho `Plane` con su arreglo cada uno | igual; el entrelazado es una optimización del lector de video que no hace falta |
| `.zip` con `.sna`, `.gfx` o `.gf0`–`.gf7`, `rom0.gfx`/`.gfa`/`.gfb`, `.bNN`, `.pal`/`.pNN`, `.cfg`, `.xor` | archivos sueltos al lado del snapshot, que es cómo viene el repositorio | igual por ahora; el `.zip` y las páginas quedan para un juego que los traiga |
| la paleta con la 255 en blanco | la de GZX, con la 255 en rojo | **cambió**: blanca |
| los GPUs leen los puertos reales con la dirección del CPU | `IO` que devuelve 0xff | igual: el `OUT` es lo que importa y ninguno lo deja pasar |
| `fillByState` al entrar: el estado entero, MEMPTR incluido | `gpu_reset` de GZX | igual, y ya existe: `State.takeFrom` |
| base de 24 juegos por SHA-256 con sus ajustes | nada | para después: cuando un juego del repositorio lo pida, el `.CFG` de al lado es el lugar |
