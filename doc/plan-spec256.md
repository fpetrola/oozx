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

| núcleo | ms por cuadro | veces el tiempo real |
|---|---|---|
| generado, imagen apagada | 0,10 | ~200 |
| OOP, imagen apagada | 0,35 a 0,38 | ~55 |
| en paso los nueve, imagen apagada | 6,08 | 3,3 |
| **en paso los nueve, un juego de verdad pintándose** | **17 a 19** | **~1,1** |

La tercera fila se midió con el núcleo ya escrito, el mejor de cinco bloques de 300 cuadros sobre
un 48K arrancado, imagen apagada, dos núcleos fijados: 6,08 ms por cuadro, 14,8 veces el OOP solo
y no nueve, porque el resto lo ponen la memoria del plano y lo que se le copia a cada seguidor en
cada instrucción.

**La cuarta es la que importa y se midió después**, con el Cybernoid cargado y la pantalla
pintándose de verdad: 17 a 19 ms por cuadro, contra los 20 que dura un cuadro real. Un juego
Spec256 corre a velocidad real y con poco margen. De eso, 1,1 ms es repintar la pantalla entera
cada cuadro, que es lo que hay que hacer y se explica abajo. Y sólo se paga mientras hay un juego
Spec256 cargado.

Trampa que costó una hora: medir con `taskset -c 2,3` mientras otro barrido propio corría en esos
mismos dos núcleos da 64 ms por cuadro y parece una regresión de cinco veces. Antes de creerle a un
número, mirar que la máquina esté quieta.

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

- **El brillo de la mezcla.** `UpMixChgBright=50` está en nueve `.CFG` y ningún oráculo abierto
  lo implementa. Ninguno de los juegos comparados arriba lo pide, así que todavía no se nota.
- **Con qué dieciséis colores se mezcla.** Con los de esta máquina, que es con lo que la máquina
  habría pintado ahí. El emulador que sacó las capturas usa otros —su blanco apagado es 192 y el
  nuestro 178— y ésa es toda la diferencia que queda en el Cybernoid y en el Solomons Key. Cambiar
  el blanco de la máquina por esto sería cambiar todas las pantallas de todos los juegos: no.
- **Cybernoid 2 y Abu Simbel.** 84 % y 90 %, y lo que difiere son pixeles prendidos contra
  apagados y no tonos. Los dos animan su título; mover el momento del disparo mejora y no cierra.
  Queda para mirar con la ventana abierta y el juego corriendo, que es donde se ve si es el momento
  o es una regla.
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
9. **Contra los juegos.** Con el repositorio del usuario, cada juego contra la captura que trae de
   su propia pantalla de título, pixel por pixel. Head Over Heels y Exolon no sirven: vienen sólo
   en `.EZX`. Los resultados están más abajo, en [Contra los juegos](#contra-los-juegos-el-14-de-septiembre-de-2026).

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
- **Copiar de ZX-Poly.** Es GPL-3, igual que este árbol, así que la licencia no lo impide; igual se
  lee como especificación de las reglas y del formato, y el código es nuestro.

## Lo que sí se puede llevar

La paleta: 768 números de `sp256.pal`, publicados por GZX bajo su licencia tipo MIT, declarados en
el `NOTICE`, con la entrada 255 en blanco por la razón de arriba. Un `ROM0.GFX` no hace falta
llevarlo: viene con el juego el que lo necesita. Y hace falta más de lo que este plan creía: los
planos de la ROM no se calculan de la ROM cuando el juego trae los suyos, porque un juego que
coloreó la fuente los quiere. El Jetpac lo demostró —sus dígitos salían grises y la captura los
tiene celestes— y con su `ROM0.GFX` cargado pasó de 94,50 % a 99,02 %. Sin uno, el byte de la
máquina sigue siendo lo que ocho planos iguales dirían, y no se copia nada.

Y lo que el repositorio de juegos trae de valor para nosotros son sus capturas, que son el oráculo
visual del paso 9, y no se copian: se miran.

## Dónde está cada paso

Al 14 de septiembre de 2026, con el árbol verde desde un repositorio local vacío en cada uno.

| paso | commit | qué dejó, y qué enseñó que el plan no sabía |
|---|---|---|
| 1. La paleta a 256 | `6ed40f795` | Dos líneas y una máscara por celda: pintar una pantalla entera cuesta 0,021 ms con paleta de 64 y con paleta de 256, medido. Lo que no estaba previsto: ULAplus usaba `Picture.COLOURS` para decir cuántos registros tiene. Eran los dos 64 y no son el mismo número; ahora dice 64 él |
| 2. El asiento de la columna | `af4e9d9cf` | `Painting.PixelsOfItsOwn`, consultado antes de las tres reglas, instalado y retirado como `Colouring.Reading`. El haz y las celdas sucias siguen siendo de `Painting`: al asiento se le ofrece una columna sólo cuando está sucia |
| 3. Los planos y el `.GFX` | `f7fdd7837` | `Planes` y el orden del archivo en un solo lugar. Leído contra el Cybernoid real da los mismos 11 706 y 2 421 que el plan había anotado antes de que hubiera código. Los planos de la ROM no se calculan ni se copian: por debajo de 0x4000 el plano devuelve el byte de la máquina, que ya es lo que ocho planos iguales dirían, y así la paginación no se piensa |
| 4. El núcleo en paso | `ba8e1048c` | `LockstepZ80`, `Alignment`, `Spec256Core`. El `peek` de `ContendedMemory` era un hueco de verdad y sin él no arranca. El costo: 6,08 ms por cuadro, 3,3 veces el tiempo real |
| 5. El snapshot y la sesión | `3f82adbb7` | `FilesOfItsOwn` en core: `Snapshots` le dice a quién guarda archivos al lado de dónde vino el snapshot, y no nombra a nadie. `Spec256Peripheral` es la sesión. Los hechos escriben un `.SNA` de 48K y un `.GFX` sintéticos en un directorio temporal, así que son de verdad y no traen nada de nadie |
| 6. La pantalla | `3f82adbb7` | El pintor de columnas, la paleta de 256 y los fondos, todo en `Spec256Peripheral`: la sesión es la que tiene pixeles propios. Cuatro `plotPair` por celda, que es el mismo camino que ya pinta un Timex de color por byte; ninguna función nueva en `Picture`. La dirección que indexa los planos es la de la máquina, no el desplazamiento dentro de la página |
| 7. Las reglas del `.CFG` | `06b90426a` | `Rules` lee el archivo del juego; el pintor las aplica y `LevelledInstructions` da a los seguidores un `OR`, `AND` y `XOR` por nivel, sin tocar el emulador: `doExecute` de una instrucción es `protected`, así que una subclase deja las banderas que la operación de siempre pone y cambia sólo lo que se escribe. Lo que la mezcla usa son los dieciséis colores de la máquina, que es con lo que la máquina habría pintado ahí |
| 8. La ventana | `69517a542` | `Spec256Frame` sobre `MachineFrame` y no sobre `DeviceFrame`: esto no es algo que se enchufa, es una sesión, y la ventana la encuentra en el registro de la máquina a la que está prendida. Muestra el juego, lo que su archivo pidió, los 256 colores, los fondos, y el interruptor que todo emulador de esto tiene |
| 9. Contra los juegos | `edcd0b7a2` | Los 29 juegos del repositorio que traen snapshot y colores **arrancan y se pintan**, ninguno se cuelga. Lo que faltaba y se encontró acá: el `ROM0.GFX`, los colores de la fuente de la ROM, que el Jetpac trae y sin el cual sus dígitos salían grises — 94,50 % a 99,02 % |
| Después: la estela | `caf50a4b1` | La pantalla de un Spec256 se pinta entera cada cuadro, porque los planos cambian donde la memoria de la máquina no lo registra |
| Después: la deriva de punteros | `901ef2623` | Medida, y el interruptor en la ventana. No cambia el valor por omisión: trece de quince juegos dan lo mismo, uno mejora mucho y otro empeora |
| Después: dos reglas por omisión | `7c9c9f4c7` | Una dirección no es un color. La escritura de un seguidor cae donde cayó la de la máquina y la suma de un `ADD` sobre un puntero es la que la máquina hace: el Renegade con la línea por omisión queda a 269 píxeles del `1DEPSs` que ZX-Poly le tiene escrito a mano, y de los quince con captura ninguno empeora. El asiento es `Core.wrapping`, porque un oyente de escrituras en `ContendedMemory` es justo lo que el generador no puede aplanar |
| Después: la tercera regla | `9e422b09e` | Un puntero **cargado** desde un dato con color: lo que distingue la deriva de la búsqueda deliberada no es la instrucción sino qué hay del otro lado, tabla o dibujo. Renegade de 269 a 126 píxeles del `1DEPSs`; los 126 que quedan son los dígitos del récord, que es el canje |

## La estela, y por qué la pantalla de un Spec256 se pinta entera

Encontrada por el usuario jugando: al moverse las cosas quedaban pixeles viejos. Medida así:
correr un cuadro, dejar que el haz complete una pasada más con la máquina quieta, y comparar esa
pantalla contra un repintado completo. Lo que sobra es lo que nunca se iba a repintar.

- Atom Ant: 2 281 celdas viejas en 60 cuadros. Bubbler: 1 963. Y **todas** en celdas donde los
  bytes de la máquina nunca se movieron.
- Cybernoid 2 y Abu Simbel parecían peores todavía —6 353 y 4 137— pero eran celdas donde los
  bytes sí se habían movido: el haz no había vuelto a pasar. Un artefacto del primer diagnóstico,
  no un problema. Con la pasada de más: cero.

Las celdas sucias existen porque la imagen **es** los bytes de la máquina: una celda que nadie
escribió no puede haber cambiado. Con un juego Spec256 eso es falso. Un seguidor escribe en su
plano en la dirección que tiene en sus propios registros, y esos registros llevan colores: basta
una suma para que escriba en otro lado. Ahí cambia un color sin que la memoria de la máquina lo
registre, y la celda no se ensucia nunca más.

Por eso los dos oráculos repintan la pantalla entera todos los cuadros y ninguno usa celdas sucias.
Nosotros también, y sólo mientras alguien tenga pixeles propios: dos líneas en `Painting.startAgain`.
Cuesta 1,1 ms por cuadro, medido de dos maneras que coinciden —el A/B de tres corridas de 300
cuadros (17,8 contra 16,7) y el costo de pintar una pantalla entera con la máquina congelada
(1,244 ms)—. Después de eso, los 29 juegos dan cero celdas viejas.

Lo que **no** conviene: leer los ocho planos de una celda de una sola pasada en vez de uno por
pixel. Parece ocho veces menos memoria y es más lento (18,85 contra 17,82): los ocho bytes ya
estaban en la caché y el arreglo intermedio cuesta más que releerlos. Probado y descartado.

## Los dos interruptores, y por qué son dos y no un valor por omisión

Jugando aparecieron tres cosas que son la misma pregunta: **cuánto de lo que un seguidor hace lo
decide él y cuánto lo decide la máquina**. Y la respuesta medida es que depende del juego, que es
por qué el formato tiene un `zxpAlignRegs` por juego y por qué los dos emuladores abiertos no
coinciden.

### Ir donde va la máquina (`T`)

Un seguidor escribe en su plano en la dirección que tiene en sus propios registros, y esos
registros llevan colores: una suma sobre uno manda la escritura a cualquier lado. Medido con todas
las escrituras a pantalla anotadas, cambien o no el byte:

| juego, 60 cuadros | la máquina nunca escribió ahí | escribió el mismo byte | la cambió |
|---|---|---|---|
| Atom Ant | **2 281** | 92 | 1 883 |
| Bubbler | **1 961** | 1 439 | 1 179 |
| Atic Atac, Renegade en juego | 0 | 124 / 2 | 0 / 28 |

Con `T` las dos primeras dan cero. **Pero `T` rompe otra cosa**: un juego que espeja un sprite lo
hace con una tabla de inversión de bits **indexada por el byte que está espejando**, y ahí el
seguidor tiene razón en ir donde la máquina no fue —su índice es su color—. Con `T` lee la entrada
de la máquina y el sprite espejado sale sin color. Se vio en el Renegade.

El asiento en el emulador es una distinción que faltaba: el registro del que una referencia toma
*una dirección* no es siempre el que la instrucción lee y escribe. `State.pointer` lo dice, por
omisión es `getRegister`, y lo usan las referencias indirectas y las ocho instrucciones de bloque.
Un seguidor devuelve ahí un registro que **se lee** del de la máquina y **se escribe** como el
suyo. No cuesta nada medible: 8,42 ms por cuadro contra 8,38.

### De dónde salen los números escritos en las instrucciones

Un número metido en una instrucción —el `n` de `LD A,n`, el `nn` de `LD (nn),A`— es un color que
un juego puede pintar. GZX lo lee del plano, y entonces un juego puede pintarlo y el seguidor
escribe ese color. ZX-Poly lo lee de la máquina, y entonces un número pintado no puede mandar al
seguidor a una dirección donde la máquina no fue. Los dos juegos que lo muestran:

| | del plano | de la máquina |
|---|---|---|
| Army Moves 1 contra su captura | **95,88 %** | 85,69 % |
| Renegade en juego | figuras con color, basura en el marcador | figuras con color **y marcador limpio** |

Ninguna de las dos gana. Por omisión queda la del plano —la de GZX, que es la que el Army Moves
necesita— y la otra es un interruptor. El asiento es `State.memoryForOpcodes`, que dice de dónde
salen los bytes de una instrucción y por omisión es la memoria de siempre.

Un hecho pagó su sueldo acá: los seguidores leyendo por la memoria de la máquina le **cobraban
T-states al reloj**, 20 000 donde había 8 000, y `NineOnAMachineTest` lo dijo en la primera corrida.

### Y por qué ninguno de los dos va por omisión

Medidos los quince juegos con captura, `T` no empeora a ninguno y mejora a uno; pero lo que se ve
jugando —los sprites espejados del Renegade— no aparece en una pantalla de título. Y los números
de la máquina cuestan diez puntos en el Army Moves y arreglan el marcador del Renegade. Las dos
son reales, ninguna domina, y por eso las dos son un interruptor en la ventana y no una decisión
escrita en el código. Lo que el juego diga en su `.CFG` manda sobre las dos.

Los dos interruptores que vinieron después —escribir donde escribió la máquina y sumar lo que suma
la máquina— sí van por omisión, y la razón es la misma vista del otro lado: éstos no le quitan al
seguidor ningún registro, así que no hay color que puedan costar. Están más abajo.

## Lo que ZX-Poly tiene y este árbol no: una base por juego

La pregunta directa —¿qué se me está pasando respecto al otro emulador?— tiene una respuesta
concreta, y no es una instrucción ni un caso de memoria: es `spec256appbase.txt`, **24 juegos con
su `zxpAlignRegs` propio, indexados por el SHA-256 del snapshot**, que se aplica solo al cargar.
Todo lo demás está comparado línea por línea y coincide: qué lee un GPU por debajo de 0x4000
(la ROM, o sus colores si el juego los trajo; la página de ROM la elige el bit 4 de 7FFD, que en
un 48K es la segunda, `.gfb`, la misma que `rom0.gfx`), cómo se cortan los planos, qué copia
`fillByState` al arrancar (todo, MEMPTR incluido, como nuestro `takeFrom`), cómo se toma una
interrupción, y que el vídeo se rearma entero cada cuadro.

Lo que esa base dice de los juegos que hay en la copia del usuario, citado de ZX-Poly:

| juego | `zxpAlignRegs` | |
|---|---|---|
| Renegade | `1DEPSs` | DE como valor, HL propio, sin `T` |
| Dizzy 1 | `1HLPSs` | |
| Atom Ant, Atic Atac, Army Moves 1, Bruce Lee, Jetpac | `1PSsT` | |
| Cybernoid 2 | `1PSs` | y `Paper00InkFF=0` |
| Phantis | `1XxYyHLDEPSs` | |
| Scooby Doo | `1HLXxYyPSs` | |
| Sabre Wulf | `1HhLlXxYyFfPSs` | |
| Solomons Key | `1PSsXxYy` | |
| Underwurlde | `1HLDEBPSsXxY` | |
| Bubbler | `PSsXxYyHbcde` | sin la `1`: hasta las banderas son suyas |
| Knight Lore | `1PSsHLhlXxYyEe` | |

Nada de esto se lleva al árbol —es de ZX-Poly y es una tabla de ajustes de otro—, pero cada línea
va en el `.CFG` del juego, que es el lugar que el formato ya tiene, o se escribe en la ventana.

**Renegade con `1DEPSs`, comprobado acá**: caras y manos con tono de piel, sin manchas en el
marcador, 4 741 escrituras a pantalla con color y cero sin color. Es exactamente lo que ninguno de
los dos interruptores daba solo: la deriva era por DE —y DE alineado como valor la corta— mientras
que el espejado de sprites indexa una tabla con HL, que tiene que seguir siendo el del seguidor.
`T` tocaba las dos y por eso las caras salían blancas. Lo único que no cierra es un dígito del
récord, "058100" contra "050000", que la captura del juego da a favor de `T`; queda anotado.

## Las reglas que hacen falta una vez, en lugar de una línea por juego

La hipótesis, después de leer la base de ZX-Poly: **una dirección no es un color**. Lo que un
seguidor hace con *datos* es suyo —ahí vive el color— pero una dirección la decide la máquina,
porque un color metido en una dirección lo manda a escribir donde la máquina no estuvo. Faltaba
decir por dónde entra un color a una dirección, y jugando aparecen exactamente dos caminos.

**Por la escritura.** Un seguidor escribe donde apunta su puntero, y si el puntero llevaba color la
escritura cae en cualquier lado: ésa es la estela. La regla es que *la n-ésima escritura de un
seguidor en una instrucción cae donde cayó la n-ésima de la máquina*, y donde la máquina escribió
menos veces, donde el seguidor quería. Lo que **lee** sigue siendo suyo, que es lo que necesita una
tabla indexada por color.

**Por la suma.** `ADD HL,BC` con un color dentro de BC deja a cada plano con un HL distinto, y
desde ahí *lee* y escribe mal. La regla es que *la suma es la que la máquina está por hacer con sus
dos registros*. Un puntero que el seguidor recibió en vez de sumarlo sigue siendo suyo: `LD L,A`
con un color en A es justamente el espejado de sprites del Renegade, y ahí el seguidor tiene razón.

Medidas con «píxeles encendidos donde la máquina no dibujó ninguna forma», que es color que llegó
adonde no iba:

| | ninguna | escritura | suma | las dos |
|---|---|---|---|---|
| Atom Ant, 200 cuadros | 34 | 1 418 | **22** | **22** |
| Bubbler, 400 cuadros | 9 500 | **0** | 1 | **1** |

Las dos hacen falta y ninguna alcanza sola, y se ve por qué. El Atom Ant dibuja con
`POP HL ; LD A,(DE) ; AND F8 ; RRCA×3 ; LD C,A ; LD B,0 ; ADD HL,BC ; LD A,(HL) ; XOR B ; LD (HL),A`:
el desplazamiento sale de un dato, así que el color entra por la suma, y con sólo la regla de la
escritura la basura deja de caer lejos y pasa a caer encima de la celda buena —de 34 a 1 418—. El
Renegade es el caso opuesto: su DE se desvía por otro lado y su HL indexa la tabla de espejado, así
que necesita la escritura y no la suma.

**El resultado que importa**: el Renegade con la línea por omisión (`1PSs`) y las dos reglas,
contra el mismo Renegade con el `1DEPSs` que ZX-Poly le tiene escrito a mano, píxeles distintos de
los 49 152 de la pantalla:

| cuadro | `1PSs` | + escritura | + suma | + las dos |
|---|---|---|---|---|
| 60 | 1 760 | 256 | 1 595 | **256** |
| 200 | 3 882 | 1 266 | 3 674 | **307** |
| 400 | 5 114 | 1 981 | 4 631 | **269** |

Es decir: **la base por juego deja de hacer falta para el caso que la motivó**, y la línea del juego
sigue mandando si el `.CFG` la trae.

A diferencia de `T`, las dos van por omisión porque no cuestan colores: no le sacan al seguidor
ningún registro, sólo le corrigen la dirección en los dos lugares por donde un color se le había
metido. Tampoco cuestan tiempo: tres rondas intercaladas sobre el Renegade en el menú, 300 cuadros,
núcleos fijados, dan **7,10 / 8,22 / 8,31 ms por cuadro con las dos y 7,89 / 8,06 / 7,99 sin
ninguna**, contra 7,88 en el árbol de antes. La dispersión entre corridas es más grande que la
diferencia entre las dos columnas: no se mide.

Los quince títulos que traen captura del título, con nada, con una, con la otra y con las dos
(porcentaje idéntico contra la captura, mejor de los cuadros 200, 300 y 400):

| | nada | escritura | suma | las dos |
|---|---|---|---|---|
| Bubbler | 67,26 % | 83,32 % | 83,15 % | **83,15 %** |
| los otros catorce | — | iguales | iguales | **iguales** |

Uno mejora dieciséis puntos y ninguno empeora: Abu Simbel 90,02, Army Moves 95,88, Atic Atac y
Phantis 100,00, Bruce Lee 64,19, Cybernoid 98,91, Cybernoid 2 81,18, Dizzy 1 99,31, Jetpac 99,02,
Knight Lore 97,92, Sabre Wulf 95,63, Scooby Doo 99,06, Solomons Key 98,63, Underwurlde 36,17, los
mismos dígitos con las cuatro combinaciones. Lo que las reglas arreglan no se ve en una pantalla de
título quieta —se ve jugando— y por eso el Renegade se mide contra el otro emulador y el Atom Ant
con píxeles sobre nada.

### Y una tercera: leer donde lee la máquina, salvo en una tabla

Quedaba un tercer camino, el que ni la escritura ni la suma tocan: un puntero **cargado** desde un
registro de datos. En el Renegade son tres instrucciones, encontradas buscando la primera
instrucción en que dos configuraciones dejan de coincidir (1 532 327 instrucciones trazadas, se
separan en la 1 160 501):

| dónde | qué hace | qué corresponde |
|---|---|---|
| `979f` | `LD E,A` con un A que lleva color, y después `LD A,(DE)` | la dirección de la máquina: es deriva |
| `9998` | `LD L,C` y `OR (HL)`, 2 350 veces en 60 cuadros | **la suya**: es la tabla de espejado indexada por el byte que espeja |
| `a002` | `LD A,(HL) ; LD (DE),A ; INC D ; INC L`, el glifo del marcador | la de la máquina |

Las tres son la misma forma de instrucción. Lo que las distingue no está en el código sino en
**qué hay del otro lado**: ocho planos que dicen todos lo mismo son una tabla, y un seguidor que
la consulta con un color suyo tiene razón en ir donde la máquina no fue; planos que difieren son
un dibujo, y ahí un puntero con color adentro apunta a un pixel que no existe. `Planes` ya sabía
contestar eso —`noColoursOfItsOwn`, que estaba para decir qué celda no tiene colores propios—.

| | sin la regla | con la regla |
|---|---|---|
| Renegade contra el `1DEPSs` de ZX-Poly, cuadro 400 | 269 px | **126 px** |
| Bubbler, color sobre nada | 1 | **0** |
| Atom Ant | 22 | 22 |
| Army Moves contra su captura | 95,88 % | 95,88 % |
| los quince títulos con captura | — | los mismos dígitos, uno por uno |

**Los 126 píxeles que quedan son el marcador, y resultaron ser al revés de lo que decía acá.**
Medidos contra el render de ZX-Poly parecían la regla rompiendo los dígitos del récord. Contra la
**captura que trae el propio pack**, hecha con el emulador original, es al revés: el original dice
`HI: 050000`, nosotros decimos `050000` y el `1DEPSs` de ZX-Poly dice `058100`. La regla los
arregla, y la referencia estaba mal elegida.

**La lección de método, que costó medio día:** el render ajustado a mano de otro emulador no es la
verdad, es otra opinión. La verdad de un juego de Spec256 está en la captura que viene en su propio
pack (`renegade.png`, `*-game.png`), hecha con el emulador para el que el pack fue armado. Contra
esa captura se mide de ahora en adelante.

**Medido y descartado por el camino:** todos los punteros de la máquina (`T`) da 1 839 píxeles de
diferencia contra ZX-Poly, y contra la captura del pack se ve por qué: los sprites que el Renegade
compone salen **blancos**. Los números de 16 bits de la máquina no cambian nada en este juego.

**Y la semántica del modelo de 64 bits, medida entera.** El original es una máquina SIMD de 64 bits:
un solo juego de registros de dirección, los ocho colores viajando al lado como dato. Eso se
implementa en este árbol con dos interruptores que ya existen —`T` y los números de la máquina— y
está medido: en los quince títulos con captura da idéntico a las tres reglas salvo Army Moves, que
pierde diez puntos por los números (95,88 % contra 85,69 %); y jugando, el Renegade pierde el color
de los sprites compuestos. O sea que **direcciones escalares más color al lado no alcanza para
reproducir al original**: falta algo que todavía no sabemos. La hipótesis siguiente era que en ese modelo **leer de una dirección
sin colores propios no pisa el color que el registro ya tenía** —se lleva la forma nueva y conserva
el color viejo—, que es lo que haría que un sprite espejado por tabla salga con color sin ninguna
línea por juego.

**Medida y falsa.** Esa frase sólo tiene sentido si la forma y el color son dos canales, y para eso
el pintor tendría que sacar la forma de la máquina y el color de los planos. Se probó en tres
líneas: el Renegade se vuelve negro —el piso desaparece, el fondo se perfora— porque en un juego de
Spec256 **el dibujo son los planos y nada más**; el bitmap de la máquina no dibuja. Sin canal de
forma, "conservar el color y tomar la forma" no se puede traducir a este modelo: acá el color *es*
la forma.

**Y de paso quedó claro de dónde sale el color de los sprites del Renegade.** Son dos caminos
distintos: el compositor de `9a20` lee la pareja máscara/dato con `POP HL` desde la pila, que está
alineada, así que es una lectura escalar y funciona igual en los dos modelos; pero el de `9998`
hace `LD L,C ; OR (HL)`, que es una dirección de verdad, distinta por plano. Con direcciones
escalares (`T`) los ocho leen la misma entrada y **los sprites salen blancos** — lo que además
prueba que esa página no tiene colores propios en el `.GFX`, porque si los tuviera, leerla escalar
los devolvería. O sea: una lectura escalar de esa tabla no puede dar color, y el original **sí** da
color. Algo por carril hace el original ahí también, y en este árbol eso es exactamente la
indexación por plano que la tercera regla conserva.

**Y esto quedó probado en vez de deducido, leyendo los archivos del juego.** El código de la rutina
está en el `.SNA` y dice dónde vive la tabla:

```
9991: e1        POP HL        ; dos índices, uno por plano
9992: 4c        LD C,H
9993: 26 be     LD H,BE       ; la tabla está en 0xBE00
9995: 1a        LD A,(DE)
9996: a6        AND (HL)      ; la máscara: tabla[L]
9997: 69        LD L,C
9998: b6        OR (HL)       ; el dato:    tabla[C]
9999: 12        LD (DE),A
```

Y el `.GFX` dice qué colores tiene cada página, contando cuántos de sus 256 bytes tienen colores
propios:

| página | con colores propios | valores que aparecen |
|---|---|---|
| **la tabla, `BE00`** | **0 de 256** | **sólo 0 y 255** |
| el buffer de composición, `FD00` | 137 de 256 | 3, 4, 49, 65, 90, 93, 188… |
| gráficos, `C000` / `D000` | 101 / 97 de 256 | 2, 34, 80, 88, 90, 116… |
| pantalla, `4000` | 30 de 256 | 65, 68, 69, 70 |

La tabla está enteramente sin colorear. Leerla con una sola dirección devuelve blanco o negro y
nada más, en cualquier emulador; el original muestra esos sprites con color, así que el original
indexa por carril. **La conclusión que queda: los dos diseños tienen la misma capacidad, y lo que
yo había llamado "el modelo de 64 bits no puede" era falso.** La diferencia entre ellos es de
costo —ocho decodificaciones contra una— y de dónde aparece la deriva, no de lo que se puede
dibujar.

### El criterio exacto, que reemplaza a la heurística

Leer la tabla del `BE00` dio además el criterio que faltaba. Esa tabla, comprobada entrada por
entrada, es **la inversión de bits**: el bit 0 sale del 7, el 1 del 6, y así. Y ahí está el
teorema: **mover bits conmuta con partir un byte en planos**. El byte del plano *p* es el bit *p*
del color de ocho píxeles; invertirle los bits es invertir el orden de esos ocho píxeles, y si los
ocho planos hacen cada uno la suya, los ocho coinciden en a dónde fue cada píxel. El píxel se mueve
entero, con sus ocho bits de color.

Un píxel prendido a la izquierda con color 90 (`01011010`): los planos 1, 3, 4 y 6 tienen `0x80` y
buscan `tabla[0x80] = 0x01`; los otros cuatro buscan `tabla[0x00] = 0x00`. Resultado: el píxel de la
derecha con bits en 1, 3, 4 y 6, **color 90 otra vez**. Con una sola dirección, los ocho leen
`tabla[0x80]`, los ocho bits quedan iguales y el color colapsa a 255: blanco.

Entonces la pregunta correcta no es "¿tiene colores propios?" sino **"¿esta tabla mueve bits sin
mezclarlos?"**: si cada bit de una entrada viene de un solo bit del índice, indexar por plano es
idénticamente lo mismo que indexar una vez, y el color sobrevive; si mezcla bits —una fuente, una
suma— el plano *p* junta bits de píxeles distintos y sale basura. Se comprueba leyendo la entrada
del cero y las de las ocho potencias de dos, y verificando las 256: 264 lecturas por página, una
sola vez, y se vuelve a preguntar cuando el juego escribe en esa página, porque estas tablas se
arman en tiempo de ejecución.

`Permutations` es esa pregunta y nada más. Medido: la imagen del Renegade sale **idéntica** a la
que daba la heurística de color (0 píxeles de diferencia hasta el cuadro 400, 23 ahí), el Atom Ant
sigue en 22, el Bubbler en 1, los quince títulos con captura no mueven un dígito, y el tiempo por
cuadro es el mismo (19,95 ms contra 19,95). Lo que cambia no es el resultado sino de qué depende:
una corazonada sobre colores pasó a ser una propiedad de la tabla que se verifica. Y la regla no cuesta tiempo: 19,95 ms por cuadro con ella y 19,95 sin ella, en la misma
máquina el mismo minuto.

El asiento: `Core.wrapping`, una línea por omisión que devuelve la memoria tal cual y que el
cableado le ofrece a cualquier núcleo antes de construirle el estado. El Spec256 la contesta con
una memoria que delega y le cuenta a los planos dónde escribió la máquina; el núcleo no sabe nada
de Spec256 y el generado no ve un solo cambio. La suma vive en la fábrica de instrucciones del
seguidor, que es donde ya vivían el AND, el OR y el XOR nivelados.

**Y por qué no fue un oyente de escrituras en `ContendedMemory`**, que era lo primero que probé:
`Memory.addMemoryWriteListener` ya existe en la interfaz como no-op, pero el generador aplana esa
clase y con el campo en `null` al generar emitió `if (null != null) { null.writtingMemoryAt(...) }`,
que no compila. Una costura para el núcleo rápido tiene que ser algo que el generador pueda
aplanar: un método que devuelve lo que recibe lo es, un campo que a veces es `null` no.

Lo que no cubre: `ADC` y `SBC` de 16 bits, que no aparecieron como problema en ningún juego medido,
y un puntero que recibe un color por una carga, que del lado de la escritura lo arregla la primera
regla y del lado de la lectura tiene que seguir siendo del seguidor.

Un tropiezo de medición que vale la pena anotar: con el árbol viejo instalado en `~/.m2` y sólo el
módulo del dispositivo recompilado, la regla de la escritura **no hacía nada** y las mediciones
daban iguales con y sin ella. El oyente de escrituras vive en `machine/core`; si no se reinstala,
la interfaz devuelve su no-op y no hay error en ningún lado. Se vio contando las escrituras que el
dispositivo veía: cero.

## El tablero que no se veía entero, y que no era un problema de color

El tablero del Renegade salía incompleto: faltaban la barra `BOSS`, los cuatro retratos y dos de
las cuatro cabezas. Contando en esas 52 líneas dio **1 032 píxeles con forma y sin color visible en
40 celdas** — pero mirando los planos de una de ellas el color estaba entero:

```
celda fila 19 columna 11: atributo 40  (tinta 0, papel 0, brillo 1)
máquina 7c | colores de los ocho píxeles:  0  48  48  48  48  48  0  0
```

El byte tiene los bits 1 a 5 prendidos y los colores están exactamente debajo. Lo que pasa es que
el juego marca esas celdas con **tinta 0 y papel 0** —invisibles en una Spectrum de verdad— y pinta
el tablero sólo por los planos. Nuestra `hiddenWhereInkIsPaper`, la `HideSameInkPaper` del `.CFG`,
las tapaba. No era pérdida de color: era el pintor escondiendo lo que el juego había pintado.

ZX-Poly la trae encendida por omisión también (`gfxHideSameInkPaper = true`, y del `.CFG` sólo la
apaga un `0` explícito), así que tendría el mismo tablero incompleto; el original, por su captura,
no la aplica ahí.

**El arreglo no fue apagarla sino afinarla:** se tapa lo que **no tiene colores propios**. Una celda
que el juego marcó invisible sigue invisible —que es como un juego borra un pedazo de pantalla— pero
lo que este juego pintó en ocho planos se ve, que es para lo que están los planos.

| | antes | con la regla afinada |
|---|---|---|
| tablero del Renegade | sin la barra BOSS, sin retratos, dos cabezas | **completo e igual a la captura** |
| Abu Simbel, el texto que scrollea | `Y SNATCHO.` cortado, sin el punto | **el renglón entero** |
| los quince títulos | — | los mismos, salvo dos décimas de Abu Simbel y cinco centésimas de Scooby que son el momento del scroll, no el render |

La lección repetida: el porcentaje contra una captura **baja** en el Abu Simbel con el arreglo
puesto, y sin embargo el render es más correcto. Una pantalla que se mueve no se puede comparar por
mejor-de-tres cuadros; hay que mirarla.

## Qué instrucción pierde el color, medido en vez de razonado

La pregunta que el Renegade hizo inevitable: ¿hay instrucciones por las que el color no llega a
los otros ocho? Se contesta anotando, por cada escritura a la pantalla, si los ocho seguidores
escribieron lo mismo —y entonces no hay color— o cosas distintas. En el Renegade, 240 cuadros ya
jugando: **4 405 escrituras con color y cero sin color**, desde veinte lugares. El dibujante de
sprites está en 0x9847 y es un `LD A,(DE)` / `LD (HL),A` desenrollado, que es la forma más pura
de llevar un color. Y en pantalla, pixeles del color 255 —el blanco de lo no coloreado—: cero.

Así que no es que una instrucción pierda información. Lo que sí la pierde, para tener la lista:

- **Leer de donde el `.GFX` no pintó.** El color no estaba; es el caso más común de todos.
- **`OR` o `AND` con una máscara sin colorear.** Bit a bit, un `OR` con una máscara pone ese bit
  en los ocho planos, y esos pixeles salen 255: blanco. Es la manera clásica de emblanquecer un
  sprite enmascarado, y sólo la evita `GFXLeveledOR`, que el juego tiene que pedir.
- **Cualquier aritmética sobre un byte de color.** Una suma corrompe, y se acepta.
- **Un `IN`**, que siempre devuelve 0xff.
- **Los dos interruptores puestos al revés** para ese juego.

**Y el arranque.** Cuando la sesión empieza, los ocho seguidores reciben los registros de la
máquina, que no llevan color: lo único que hay para darles. Todo lo que el juego dibuje con lo que
ya estaba en un registro sale sin color hasta que lo vuelva a buscar a memoria — y eso pasa en
cuanto el sprite se redibuja. Por eso se ve mal al principio y bien en cuanto las cosas se mueven.
GZX hace lo mismo en su `gpu_reset`; no hay de dónde sacar un color que todavía no se leyó.

**Y una trampa del propio medidor.** La cuenta de "celdas sin colores propios" contaba también las
celdas negras, porque ocho planos en cero son ocho planos iguales: decía 676 de 768 sobre una
pantalla casi vacía. Ocho planos de nada no es una forma sin colores, es ninguna forma. Corregida,
el Renegade da cero en todo momento: al cargar, en la atracción y jugando.

## Lo que parecía blanco y negro y no lo era

El Renegade con `T` puesto: algunos personajes enteros de color y otros con el torso blanco y los
pantalones azules. Suena a colores que no llegaron, y no lo es. Medido sobre los 49 152 pixeles de
la pantalla, contando cuántos son exactamente el color 255 —que es el blanco que sale de un byte
sin colorear—: **cero**, y 82 entradas de la paleta en uso. Y la captura que el propio juego trae
muestra lo mismo: remeras blancas y vaqueros azules. Es el arte del juego.

Sin `T`, en cambio, la misma pantalla usa 138 entradas. No es más color: es la deriva pintando
donde nadie pidió, y en esa corrida el marcador sale corrompido.

De ahí la línea que la ventana muestra ahora: cuántas de las 768 celdas de la pantalla no tienen
colores propios. Una forma blanca es o una forma blanca, o una forma cuyos colores no llegaron, y
se ven igual; esto las separa sin tener que medir nada a mano.

## Contra los juegos, el 14 de septiembre de 2026

Los 29 del repositorio que traen `.SNA` y `.GFX`, 300 cuadros cada uno con todo por omisión, contra
la captura que cada uno trae de su propia pantalla de título, pixel por pixel sobre los 49 152 de
la pantalla. Ninguno se cuelga ni se queda en negro.

| juego | idéntico | qué explica lo que no |
|---|---|---|
| Atic Atac, Phantis | 100,00 % | nada que explicar |
| Cybernoid | 98,91 % | el resto, dentro de 16 por canal: el blanco apagado de esta máquina contra el de la que sacó la captura |
| Solomons Key | 98,63 % | lo mismo: 100 % dentro de 16 |
| Dizzy 1 | 99,31 % | |
| Scooby Doo | 99,08 % | |
| Jetpac | 99,02 % | era 94,50 % hasta que se cargó su `ROM0.GFX` |
| Knight Lore | 97,92 % | con su fondo 0, que es el que va; con los otros tres, 77 %, 77 % y 70 % |
| Army Moves 1, Sabre Woolf | 95,9 % y 95,6 % | |
| Abu Simbel Profanation | 90,02 % | |
| Cybernoid 2 | 83,97 % | la cuerda del marco se anima; lo que difiere no son tonos sino pixeles prendidos contra apagados, y mover el momento del disparo lo sube pero no lo cierra |
| Underwurlde | 36,17 % | la captura muestra una pared verde que **no está en esta copia del juego**: el único fondo que trae es `UNDERW.B01`, una cueva roja. Con el fondo apagado da 25 %, así que tampoco está en sus planos |

Dos capturas más, Bruce Lee y Bubbler, son PNG con paleta y el comparador no las lee; los juegos
corren igual. Los otros catorce no traen captura del título.

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
