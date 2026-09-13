# Los formatos de archivo: cómo probarlos antes de rehacerlos

## El problema

Rehacer los lectores de tap, tzx, csw, z80, sna y szx bien orientados a objetos es seguro sólo si
hay tests antes. Escribir esos tests a mano tiene un agujero: los casos raros son justamente los
que uno no sabe que existen.

## El andamio

libspectrum es la implementación de referencia, está instalada en esta máquina como
`libspectrum.so.8`, y `machine/bridge` ya tenía un binding JNA. Así que no hace falta inventar
expectativas: se le pregunta a la referencia.

Y el corpus difícil ya está en el repo. `libspectrum/test/` tiene los archivos que sus autores
usan para sus propias regresiones: seis tzx inválidos, los de loop y jump, el turbo sin piloto, el
bloque de datos puros con cero bits usados, el de grabación directa vacío, un tap con offset roto.

`model.tests.formats.LibspectrumOracle` recorre los bloques de una cinta con libspectrum;
`WhatJavaMakesOfTheCorpusTest` imprime, archivo por archivo, qué ve cada uno. No afirma nada
todavía: es la primera mirada, para que el trabajo tenga un orden.

## Lo que dijo la primera corrida

**Las cintas se leen bien.** Para los once tzx válidos, Java identifica exactamente los mismos
bloques que libspectrum, en el mismo orden, incluidos los difíciles: datos generalizados, nivel de
señal, inicio y fin de bucle, salto, grabación directa, datos puros. La única diferencia es
sistemática y no es un error de lectura: Java cuenta la firma del archivo, `ZXTape!`, como si
fuera un bloque más.

**Tres huecos concretos:**

1. La firma del tzx aparece como bloque `5A: TZX header`. No es un bloque.
2. En un tap, Java no le pone id a los bloques; libspectrum dice que son `0x10`, datos estándar.
3. Java no valida: los seis archivos que libspectrum rechaza por estar truncados o mal formados,
   Java los lee como si nada. Un tzx con un bloque de tipo `0xFF` sale como "Unknown id FF".

**Los snapshots todavía no dicen nada** porque el oráculo sólo compara el PC. `invalid.szx` se
rechaza, que está bien, y hay un chunk `ZXPR` que se saltea.

## Los tres huecos, cerrados

1. **La firma no es un bloque.** El reproductor numeraba sus bloques desde el offset 0, y el
   listado lo copiaba para que coincidieran, así que el bloque 1 de un tzx era su propia cabecera.
   Ahora los dos empiezan pasada la firma, y la numeración es la que el formato usa.
2. **Un bloque de tap es 0x10**, datos estándar, que es como lo llama el formato y como lo llama
   un tzx; no era -1.
3. **Un archivo mal formado se rechaza entero**, que es lo que ya hacía el reproductor: un
   bloque cuyo largo pasa el fin del archivo, un id que el formato no define, o un bloque de
   información de archivo que declara más textos de los que trae.

Después de eso, los once archivos válidos del corpus dan exactamente los mismos bloques que
libspectrum, en el mismo orden y con los mismos ids, y cinco de los seis inválidos se rechazan
igual que la referencia.

**Queda uno:** `invalid-gdb.tzx`. Su bloque de datos generalizados tiene el largo exterior
correcto, y lo que miente son las cuentas de símbolos de adentro. Validar eso es implementar la
estructura interna del bloque, que es justamente el trabajo de rehacer el formato, no un parche
al listado.

## El oráculo de snapshots, por reserialización

Dos lectores guardan un snapshot en su propia forma y no hay manera de comparar un struct de C con
un objeto Java. Así que los dos se escriben de vuelta con el mismo escritor, el de libspectrum: la
referencia escribe lo que leyó del archivo, y escribe también lo que leyó Java, y los dos arreglos
de bytes tienen que ser iguales. Están todos los campos, incluidos los que nadie pensó en revisar,
y la codificación es la misma de los dos lados porque la produjo el mismo código.

Son dos funciones más de JNA, `libspectrum_snap_write` y `libspectrum_free`, y en la primera
corrida encontró tres errores:

1. **`plus3.z80` revienta.** Un snapshot de +3 tira `NullPointerException` porque `MemoryState` no
   tiene reservadas sus páginas de RAM.
2. **`empty.szx` lee A y F al revés.** Y la causa es justo el tipo de caso que no se testea a
   mano: las versiones de libspectrum hasta la 0.5.0 escribían esos dos registros invertidos en un
   szx, así que la propia libspectrum mira el bloque de creador y, si dice `libspectrum: 0.5.0` o
   anterior, los da vuelta al leer. Java no tiene esa compensación.
3. **`random.szx` se sale del arreglo**, leyendo una página más allá de su fin.

Y una buena: `manicminer.z80`, un snapshot de un juego real que ha estado corriendo, sale byte por
byte idéntico. El lector de z80 de 48K está bien.

### Los tres, arreglados

1. **El +3 reventaba en el escritor**, no en el lector: al comprimir una página que el archivo no
   traía, `MemoryState.readByte` se caía sobre un null. Una página que un snapshot no trae se lee
   ahora como los ceros que una máquina tiene en la RAM que nunca escribió. Y el escritor ya no
   inventa esas páginas: escribía las ocho, rellenando de ceros las que faltaban, y el archivo
   crecía una página por cada una.
2. **`random.szx` se salía del arreglo**, también en el escritor. Bytes al azar salen *más largos*
   que la página de la que vienen, porque cada 0xED cuesta dos, así que comprimir escribía pasado
   el fin del buffer. Ahora una página que no se achica se escribe como está, que es lo que el
   formato dice con un largo de 0xFFFF. De paso se arregló algo latente: una página que comprimía a
   exactamente 0x4000 se escribía marcada como sin comprimir, y el lector siguiente la habría
   desempaquetado como bytes crudos.
3. **A y F al revés en un szx viejo**, y también el par alternativo. Se lee el bloque de creador y,
   si dice `libspectrum: 0.5.0` o anterior, se dan vuelta los dos pares, que es lo que hace la
   propia libspectrum.

Y uno más que apareció en el camino: el bit 2 del byte 37 de un z80 dice que un 48K tiene un AY
puesto, y es lo único de lo que puede hablar, porque un 128 o un +3 lo traen en la placa. Se
escribía desde "tiene AY", así que todo snapshot de 128K salía con el bit puesto, y el lector lo
lee de vuelta como un Fuller en un 48K.

Los cinco archivos pasan, y el test dejó de imprimir para afirmar.

### Y lo que la referencia no quiere, Java tampoco

El corpus guarda sus dos `.sna` comprimidos, y por eso quedaban afuera: ahora se descomprimen a un
archivo y entran como los demás. Los dos son inválidos a propósito, y el oráculo pasó a exigir la
otra mitad de la simetría, que un archivo que la referencia no acepta Java tampoco lo acepte.

Los tomaba. Un `.sna` de 48K no guarda el PC: se saca de la pila al arrancar el snapshot, así que
el puntero de pila tiene que apuntar a RAM con lugar para la palabra. Uno en ROM, o justo arriba de
todo, no tiene un PC que dar, y la máquina arrancaría desde lo que la ROM tenga ahí. Ahora se
rechaza, que es lo que hace la referencia.

## El oráculo de RZX, que no necesita ninguna biblioteca

Una grabación lleva adentro el snapshot desde el que arrancó la máquina y, cuadro por cuadro, cada
byte que el juego leyó de un puerto. Reproducirla sólo se mantiene en paso mientras la máquina hace
exactamente lo que hizo la que se grabó: un registro mal cargado del snapshot, un byte de memoria
que falta, y el juego toma otro camino en uno o dos cuadros y pide lecturas que no están. El
reproductor lleva dos índices de cuadro, el suyo y el de la grabación, y se separan justo ahí.

`model.tests.media.RzxOracleTest`, en el módulo rzx, tiene los dos:

1. **La grabación sigue el paso** durante 400 cuadros de Jet Set Willy. Eso dice que el snapshot se
   cargó entero y que la máquina lo corre bien, que es lo que ninguna comparación de campos puede
   decir.
2. **Lo guardado a mitad vuelve entero.** Se juegan 200 cuadros, se guarda el estado a un .z80, se
   vuelve a cargar en la misma máquina, y la grabación sigue 200 cuadros más. Se comparan además
   los registros y los 48K de RAM.

No hace falta JNA ni tener libspectrum instalada: la grabación es un archivo del repo.

### Lo que enseñó de paso

El primer intento comparaba los 64K y fallaba en 1228 bytes de la ROM. No era un error del
emulador: la grabación trae su propia ROM, distinta del `48.rom` del proyecto, y un .z80 de 48K no
guarda la ROM, así que al recargar vuelve la del proyecto. Se ve midiendo antes de reproducir un
solo cuadro. La comparación es de lo que el formato lleva, y lo que prueba que el estado volvió no
son los bytes sino que la reproducción siga.

## Lo que sigue

1. Ampliar el oráculo a los campos de un snapshot, o mejor, compararlo reserializando: los dos
   leen el archivo y lo escriben, y se comparan los bytes.
2. Formato por formato, convertir lo que el oráculo dice en tests propios que nombren los campos.
3. Encima de eso, los dos oráculos que no necesitan libspectrum: la reproducción de una RZX, que
   prueba que el estado cargado hace correr el juego, y guardar a mitad de una grabación para
   seguirla desde el archivo, que prueba que lo guardado estaba completo.
4. Cuando cada formato tenga sus tests, esto queda fuera del gate, no borrado: el día que alguien
   agregue pzx o un chunk nuevo, tiene contra qué validarlo.
