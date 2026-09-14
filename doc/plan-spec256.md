# Spec256: qué es, qué le cuesta a nuestro modelo, y cómo entra sin tocarle la velocidad a nada

Spec256 fue un emulador de DOS de 1999 que corría juegos de 48K en 256 colores sin colour clash:
los gráficos originales se reemplazan por una versión de 8 bits por pixel que viaja por el mismo
código del juego, sin modificarlo. Este plan dice qué es exactamente, en qué punto de **nuestro**
modelo aterriza cada parte, cuánto cuesta, y cómo se hace para que el core no sepa que Spec256
existe y para que la velocidad de todo lo demás quede intacta. Las dos condiciones son la misma
condición: si el core no lo nombra, el core no lo paga.

## Lo que se midió, el 14 de septiembre de 2026

**El oráculo.** El Spec256 original es cerrado y de DOS; EmuZWin, que heredó el formato, es cerrado
y de Windows; ZEsarUX no lo implementa (no hay una línea sobre él en sus fuentes). La única
implementación abierta es la de GZX (Jiří Svoboda, licencia tipo MIT): `z80g.c`, 267 líneas, y
`video/spec256.c`, 330. GZX mismo dice "not 100% done". Lo que sigue se leyó de ahí y se comprobó
contra los archivos de un juego real, el Cybernoid del repositorio `mvvproject/Spec256-Games`.

**Los archivos de un juego.** Un `.SNA` o `.Z80` de 48K, y a su lado, con el mismo nombre, un
`.GFX` de exactamente 393 216 bytes: 49 152 direcciones de RAM por ocho bytes, **un byte de color
por pixel**, el pixel de la derecha primero. Opcionalmente un `ROM0.GFX` con lo mismo para la ROM,
y fondos `.B00`, `.B01`… de 64 000 bytes: imágenes de 320 por 200 de un byte por pixel, centradas
bajo la pantalla, que se ven donde el color es cero. La paleta es fija, 256 entradas RGB, y viene
con el emulador y no con el juego (`sp256.pal` en GZX, 768 números, MIT). Los `.CFG` y `.EZX` son
de EmuZWin y describen reglas que GZX no tiene; van al final.

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

Trescientos cuadros son seis segundos de máquina. Nueve ejecuciones OOP por instrucción —que es lo
que Spec256 hace— dan del orden de un segundo por 300 cuadros: **seis veces el tiempo real**, sin
optimizar nada, y sólo mientras hay un juego Spec256 cargado.

## Cómo funciona, leído de GZX

Nueve procesadores. Uno es el Z80 de la máquina, sobre su memoria, y es el único que existe para
todo lo demás: puertos, contención, interrupciones, sonido, cinta. Los otros ocho son *GPUs*: un
Z80 cada uno sobre su plano, sin puertos y sin reloj.

Por cada instrucción:

1. A cada GPU se le copian del CPU el PC, SP, I, R, IFF1, IFF2, el modo de interrupción, si está
   parado, y **todas las banderas menos el acarreo**. Sus registros de datos - A, BC, DE, HL, los
   alternativos, IX, IY - no se tocan: son suyos, y llevan colores.
2. Cada GPU ejecuta la instrucción que está en su propio plano en ese PC. Como en las zonas de
   código los planos son copias del original, ejecuta la misma instrucción que el CPU.
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

**Puertos en los GPUs.** No los tienen. Un `IN` en un GPU devuelve 0xff y un `OUT` no hace nada, y
no importa: lo que lee un puerto nunca es un color, y el flujo lo lleva el CPU.

**Sólo 48K.** GZX se niega con otro modelo; el formato es de 49 152 bytes y no sabe de páginas.

## Dónde aterriza en nuestro modelo

| pieza nuestra | qué es hoy | lo que Spec256 le pide | cambio en core |
|---|---|---|---|
| `Core`, `Processors` | un conjunto de implementaciones del procesador (`Multibinder<Core>`), cada una llega en su módulo por `Extension`, y `Processors.use` mueve una máquina que corre a otra | una implementación más, "Spec256": el OOP nueve veces en paso | **ninguno** |
| `OOZ80` | `execute()` e `interruption()` públicos | una subclase que ejecuta en nueve | ninguno, si admite herencia; si está cerrada, abrirla es una palabra |
| `State`, `Memory` | un `State` sobre una `Memory` y un `IO`; `State(IO, Memory)` existe | ocho estados sobre ocho planos con un `IO` que no contesta | ninguno: el plano implementa `Memory` en el módulo |
| `Picture.COLOURS` y sus búsquedas | 64 colores, índices `byte` | 256, e índices que no se vuelvan negativos | **dos líneas**: 256 y `& 0xff` al buscar |
| `Painting.plotLine` | tres reglas, una por bandera del `ScreenLayout` | una cuarta que no lee la memoria de la máquina sino ocho planos que viven en otro lado | **un asiento**: quién pinta una columna, si alguien lo dijo |
| `Snapshots.load(url)` | conoce la ruta y no se la dice a nadie | que alguien sepa de qué archivo vino un snapshot, para mirar al lado | **un asiento**: a quién avisar |
| `SpectrumMemory` | 65 páginas | nada: los planos no son páginas de la máquina | ninguno |
| las máquinas | veinticuatro | nada: es un juego de 48K en un 48K | ninguno |
| el escritorio | ventanas por `Equipment` en `META-INF/services` | una ventana más | ninguno |

Tres cambios en core, ninguno con la palabra Spec256, ninguno en un camino caliente de nadie:

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
  el estado de la máquina y ocho OOZ80 sobre ocho `Plane` - una `Memory` de 64K cada uno, sin
  contención y con un `IO` que devuelve 0xff - y devuelve un `LockstepZ80` cuyo `execute()` hace
  los tres pasos de arriba y cuyo `interruption()` la toma en los nueve. Ningún oyente de
  contención en los GPUs: los T-states los cuenta el CPU, una vez. El PC del CPU sigue siendo el
  que ven las trampas, el depurador y el RZX.
- **`Spec256Peripheral`**, un dispositivo sin puertos. Al activarse instala su `ColumnPainter` y la
  paleta; al desactivarse los retira y pide `refreshAll`. Escucha a `Snapshots`: si al lado del
  snapshot hay un `.GFX`, carga los planos, los fondos y el `ROM0.GFX` si está - y si no está,
  llena los planos de ROM con la ROM misma, que es exactamente la codificación de "no es gráfico"
  y deja a los GPUs ejecutar sus rutinas en paso -, mueve la máquina al núcleo Spec256 y recuerda
  en cuál estaba. Un snapshot sin `.GFX`, un cambio de máquina o un reset la devuelven a ese.
- **`Spec256Equipment`**, la ventana: qué `.GFX` está cargado, cuántos fondos hay y cuál se ve, los
  256 colores, y un interruptor "256 colores / los originales" que es lo que EmuZWin tiene en F2.
  Es la ventana de ULAplus de ayer con otros datos adentro.

## Qué queda como pregunta

- **Los pixeles apagados con color.** En el Cybernoid hay 2 421 en la pantalla del título. GZX los
  pinta porque no mira el bitmap; el Spec256 original quizá enmascaraba. Se decide mirando el
  resultado contra las capturas que el repositorio trae de cada juego (`*-title.png`), que son
  el único oráculo visual que hay.
- **Las reglas de EmuZWin.** `GFXLeveledXOR`, `GFXLeveledOR`, `GFXLeveledAND`, las mezclas con
  papel y brillo, `BkOverFF`: los juegos nuevos del repositorio, hechos con EmuZWin, dependen de
  ellas y GZX no las tiene. Su documentación está en un sitio con certificado inválido y sus
  fuentes son cerradas. Se hace después y con otro oráculo, o no se hace.
- **`Processors.use` en caliente.** Mueve una máquina que corre entre implementaciones; hay que
  confirmar que conserva los registros y no reconstruye el estado. Si los conserva, la sesión
  arranca sin resetear el juego; si no, se copia el estado, que es lo que hace `gpu_reset`.
- **Qué se le copia a un GPU.** GZX copia lo listado arriba y deja el acarreo. Quedan MEMPTR y la
  bandera Q, que GZX no tiene y nosotros sí; se copian también, y un hecho lo dice.

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
   donde debe, que un `IN` en un GPU no lee nada, que un salto que el CPU toma por un puerto lo
   toman los nueve, que una interrupción llega a los nueve, que los T-states se cuentan una vez.
   Y el número: cuánto cuesta un cuadro en este núcleo, medido y escrito.
5. **El asiento del snapshot y la sesión.** Core, agnóstico, más el dispositivo. Hechos: un
   snapshot con `.GFX` al lado enciende la sesión y uno sin ella no; la sesión devuelve la máquina
   al núcleo en que estaba; un cambio de máquina la termina.
6. **La pantalla.** El `ColumnPainter` del módulo, los fondos, el color cero. Hechos con planos
   sintéticos: ocho pixeles con ocho colores de una columna, el fondo donde el color es cero, el
   borde de la ULA intacto.
7. **La ventana.**
8. **Contra los juegos.** A mano, con el repositorio del usuario: Cybernoid, Head over Heels,
   Exolon, comparando con las capturas que trae. Ahí se contesta la pregunta de los pixeles
   apagados, y ahí se ve qué juegos son de EmuZWin y no arrancan bien.

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
- **Empezar por las reglas de EmuZWin.** Sin oráculo abierto no hay hechos que escribir.

## Lo que sí se puede llevar

La paleta: 768 números de `sp256.pal`, publicados por GZX bajo su licencia tipo MIT, declarados en
el `NOTICE`. Un `ROM0.GFX` no hace falta llevarlo: los planos de la ROM se calculan de la ROM. Y
lo que el repositorio de juegos trae de valor para nosotros son sus capturas, que son el oráculo
visual del paso 8, y no se copian: se miran.
