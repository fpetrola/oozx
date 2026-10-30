# Plan: del trampolín `runJumps` a métodos Java estructurados

Fecha: 2026-10-06. Rama `traduccion`, último commit al escribirlo: `d5be2f045`.

## 0. Objetivo y criterio de éxito

Que el Java generado exprese los ciclos como lo haría un programador: métodos que se llaman, bucles
`while`/`do`/`for` con etiquetas dentro de un método y `return`. Hoy, cuando varias rutinas se saltan
entre sí en ciclo, se generan métodos que devuelven la próxima dirección y un `runJumps(int)` con
`while (true) { switch (pc) ... }` los encadena. Eso es una máquina de estados, más parecida al Z80
que a Java. JSW no lo usa: sus bucles quedan dentro de dos métodos grandes pero estructurados.

Criterio de éxito, por juego:

| Juego | Casos de `runJumps` hoy | Meta |
|---|---|---|
| JSW, Wally | 0 | 0, sin cambios en su traducción |
| DD | 3 | 0 |
| Dizzy | 48 | 0 |
| Emlyn | 67 | solo los irreducibles, cada uno documentado con su causa |

Además, en cada fase:

- Fernflower descompila todo (`notDecomp=0`).
- Lockstep y reproducción desde el fuente completos en todas las grabaciones.
- Lockstep con teclas al azar sin divergencias.
- Suite verde.

## 1. Diagnóstico

### Cómo se decide hoy

- `BytecodeGenerationContext.routinesInJumpCycles()` llama a `RoutineManager.routinesInJumpCycles(destinosDinámicos)`.
- Arma un grafo entre rutinas con una arista de A a B cuando A pasa a B sin `CALL`: por `JP`/`JR`, por un `RET` usado como salto, por un `JP (reg)` con destinos conocidos, o por caída al final.
- Una rutina que puede volver a sí misma siguiendo aristas es miembro. Los `CALL` no cuentan.
- El método de un miembro devuelve `int`: la próxima dirección, o -1 para un `RET` normal.
- `RoutineBytecodeGenerator.tailJump`, entre miembros, devuelve la dirección destino. Si entra en medio de una rutina, antes hace `setNextAddress`.
- `invokeTransformedMethod` entra a un ciclo con `runJumps(dirección)`.
- `StateBytecodeGenerator` genera `runJumps`.
- `SpectrumApplication.invokeMethod` continúa el trampolín cuando un método llamado por reflexión devuelve una dirección.

No hay nada especial para JSW: ahí ninguna rutina salta a otra en ciclo.

### Por qué aparecen en Dizzy (48 miembros)

- **Origen en la partición.** Los 48 ya son rutinas en la partición que arma la ejecución simbólica (SE). Ninguno sale de un corte posterior del generador.
- **Tipo de destino.** 40 son destinos de `JP`/`JR`, es decir etiquetas en medio del código. Solo 2 son destinos de `CALL`.
- **Muchas entradas.** La causa principal es que la SE se lanza desde muchas entradas. `exploreGame(start, …)` recibe:
  - la foto del RZX (F85B), que cae dentro de la ISR y en medio de la música del título;
  - las direcciones de retorno de la pila de esa foto (F7AB, F83D);
  - la ISR (F85A);
  - todos los destinos dinámicos de la grabación (`dynamicInvocation.values()`);
  - los destinos de retornos no locales (D252);
  - los destinos de `CALL` mutantes (`calledThrough`).

  Cada entrada que cae en código sin dueño abre una rutina. Después, el código que salta ahí desde su rutina natural queda como salto entre rutinas.
- **Ejemplo:** la tabla de comandos de la música (`CALL F9B4` seguida de una tabla de `JP`) se explora como entradas sueltas antes que el bucle que la despacha. El reproductor queda partido en unas 17 rutinas en ciclo, y la lógica del juego en otro grupo de unas 11.
- **Otras fuentes generales:**
  - `RoutineFinder` abre una rutina nueva al saltar a código nuevo después de un `LD SP` (`jumpsToNewCodeAfterStackReset`).
  - `RoutineManager.splitAtEntriesFromOutside` corta en entradas desde afuera.
  - `StateBytecodeGenerator.splitIfTooLargeForOneMethod`, junto con el reintento por partes de Fernflower en `BytecodeGeneration`, parte métodos grandes o con bucles de varias entradas. Fernflower vacía en silencio un método cuando a un bucle se entra por el medio.

### Por qué aparecen en Emlyn (67 miembros)

- 62 están en el programa del partido (7FF7–DE53): el planificador 94AA y sus tareas. Cada tarea termina con `JP 94AA`, así que es una máquina de estados del propio juego.
- 4 están en el menú.

## 2. Principio

Un ciclo entre métodos aparece cuando se entra al código por el medio. Con menos entradas, las rutinas quedan enteras y sus bucles quedan dentro de un método. El orden de ataque es:

1. No crear entradas innecesarias: punto de entrada limpio y exploración en orden natural.
2. Fusionar lo que quede, si tiene una sola entrada.
3. Reconocer patrones que son llamadas y retornos disfrazados, como el planificador.

`runJumps` queda solo como respaldo para ciclos de verdad irreducibles, medido y documentado.

Reglas de CLAUDE.md que aplican:
- cada concepto en su dueño (la partición en `RoutineManager`, los hechos de pila en `StackAnalyzer`, el RZX en zx-rzx);
- reusar antes de agregar;
- decir el neto de líneas en cada commit.

## Fase 0. Medición, antes de tocar nada

- **0.1 Reporte de componentes.** Agregar `RoutineManager.jumpCycleComponents()`. Reusa el grafo de `routinesInJumpCycles` y devuelve los componentes fuertemente conexos. Por cada uno da sus rutinas y sus entradas desde afuera, con la causa de cada entrada:
  - `CALL` a un miembro desde fuera del componente;
  - `externalEntries`;
  - punto de retorno de un `catch` (`catchPointsOfCallsIn`);
  - destino dinámico desde afuera;
  - `isNextPC` (entrada en medio de una rutina);
  - entrada de la SE.

  `StateBytecodeGenerator` lo imprime con `-Dtranslation.reportJumpCycles=true`. No hace falta archivo nuevo.
- **0.2 Línea de base.** Por juego: miembros, componentes, entradas por componente, `setNext`, `isNextPC`, `catchSE`, `notDecomp` y líneas, usando `counts` de `env.sh`. Se anota en la sección 9.
- **0.3 Batería de regresión fija.** Ver la sección 7.

## Fase 1. Punto de entrada limpio del RZX

**Para traducir y verificar no hace falta un archivo nuevo.** El emulador reproduce el RZX hasta el punto elegido (`EmulatedMiniZX.ofRecording(…).stoppingAt(pc)`, `RemoteZ80Translator.emulateRecordingUntil`), y el Java sigue con los frames que quedan. `RemoteZ80Translator.footprint(rzx, from)` ya empieza a recolectar en `from`, y Emlyn ya arranca así en FE65. Grabar un RZX nuevo (foto de ese momento más los frames siguientes) es opcional: sirve para jugar, para compartir y para el IDE (1.5).

- **1.1 Definición de punto limpio.** El punto tiene que cumplir:
  1. el PC está al comienzo de un bloque al que se llega por `JP` o `CALL`, no en medio de un bucle;
  2. la pila está vacía para el juego: no hay direcciones de retorno pendientes hacia código, así que `returnAddressesOnStack` da vacío;
  3. no está dentro de la ISR (no hay un PC de interrupción en la pila) y el modo de interrupción es el definitivo del programa;
  4. de preferencia, es el comienzo del bucle principal o de un programa (menú o partido), para que todo lo demás se alcance por llamadas.
- **1.2 Herramienta para encontrar candidatos.** `foot/CleanEntry.java`, en las herramientas de investigación. Reproduce el RZX con un `StackAnalyzer` y por frame reporta los PC que cumplen las condiciones 1 a 3: profundidad de llamadas 0 según las entradas de retorno conocidas, SP igual al tope observado del programa y fuera de la ISR. Para cada uno da el primer frame. En Dizzy se espera D291, el bucle principal, alrededor del frame 169, después de la música del título; hay que verificarlo.
- **1.3 Usar el punto.** La dirección pasa a ser una constante por juego, como FE65, y la usan:
  - `emulateRecordingUntil(rzx, punto)`, `footprint(rzx, punto)` y `exploreGame(punto, …)` en `GameBytecodeCreationTests`;
  - `Lockstep <rzx> <clase> <punto>` y `ReplayClass`;
  - la tabla `GAMES` de `PlayTranslatedGame`.

  Se quitan las entradas que solo existían por el arranque:
  - `returnAddressesOnStack`, afirmando que viene vacío;
  - la ISR como entrada manual, si se alcanza sola;
  - los destinos no locales plantados antes del punto, como D252.
- **1.4 Efecto esperado en Dizzy.** Desaparecen F85B, F7AB, F83D y D252 como entradas. La música se alcanza por `CALL F740`, y su tabla por el despacho dentro de su rutina. Medir con 0.1.
- **1.5 RZX nuevo, opcional, después de validar 1.3.** Herramienta `RzxCut`:
  1. Reproduce hasta el punto limpio.
  2. Saca la foto con `SnapshotSaver.getSnapshotAsBytes(registros, estado)`, en formato `.z80` (módulo machine/spectrum).
  3. Arma los frames desde el actual. El primero lleva los fetches que faltan hasta la interrupción y los IN que faltan (`RZXPlayerIO.getConsumedThisFrame()`, `getFetchCounter()`); después van los frames originales.
  4. Escribe con `RzxWriter.write(foto, "z80", frames, salida)`.

  El dueño es zx-rzx en oozx-plugins (`RzxWriter`/`RZXPlayerIO`), no el traductor. Ojo con la API publicada de los plugins (memoria plugin-binary-api).

  Validación: el RZX nuevo se reproduce completo en nuestro reproductor y en lockstep desde su frame 0, y termina en el mismo estado que el original; opcionalmente se prueba en Fuse. Alternativa más simple: cortar en el borde de frame siguiente al punto limpio, si ese borde también es limpio.
- **1.6 Lo que se pierde.** El lockstep y la reproducción ya no cubren los frames anteriores al punto. En Dizzy son los de la música del título, cuyo código se vuelve a ejecutar más tarde desde el juego. Hay que verificar con la huella que ninguna rutina se use solo antes del punto.

## Fase 2. Exploración en orden natural

- **2.1 No sembrar destinos dinámicos como entradas de la SE.** Hoy los tests pasan `stackAnalyzer.dynamicInvocation.values()` a `exploreGame`. Los destinos de un `JP (reg)` o de un `RET` que despacha ya se exploran desde la instrucción que despacha (`JPRegisterAddressAction` con `getInvocationsSet`). Sembrarlos aparte los convierte en rutinas si se exploran antes. El cambio:
  1. explorar primero desde el punto limpio y desde la ISR;
  2. en un segundo pase, explorar solo los destinos que quedaron sin dueño (`findRoutineAt(t) == null`).
- **2.2 Mantener `calledThrough`.** Son destinos de `CALL`, mutantes o por el trampolín de la ROM, así que son rutinas legítimas.
- **2.3 Revisar `jumpsToNewCodeAfterStackReset`.** Con punto limpio desaparece el cambio de pila del arranque. Los demás, como la salida al menú de Emlyn o el aborto de Dizzy, siguen como retornos no locales (`StackException`), no como rutinas nuevas. Medir cuántas rutinas se siguen abriendo por esta regla.
- **2.4 Medir y verificar.** Se espera que el reproductor de música y la lógica de Dizzy dejen de ser ciclos entre métodos.

## Fase 3. Fusionar componentes con una sola entrada

- **3.1 Dónde.** En `RoutineManager`, que es el dueño de la partición, después de `splitAtEntriesFromOutside` y antes de `splitIfTooLargeForOneMethod`. Para cada componente de `jumpCycleComponents()` cuyas entradas desde afuera llegan todas a la misma rutina (la cabeza), se fusionan sus rutinas en la cabeza. Se reusa el manejo de bloques de `Routine`/`BlocksManager` (`addInstructionAt`, bloques compartidos). El resultado es una rutina con una sola entrada: los saltos quedan internos, el generador emite saltos que Fernflower convierte en bucles, y desaparecen `runJumps` y `setNextAddress` para ese componente.
- **3.2 Cuándo no fusionar:**
  - si hay más de una entrada distinta, contando `isNextPC`, puntos de retorno de `catch` y entradas externas;
  - si la fusión absorbe una rutina que también es destino de `CALL` desde otro lugar, porque esa es otra rutina legítima.

  En esos casos el componente se reporta.
- **3.3 Tamaño.** Estimar el bytecode antes de fusionar. Si supera el límite que hoy dispara `splitIfTooLargeForOneMethod`, no se fusiona todo: se extraen como métodos llamados, no saltados, las regiones sin ciclo, que tienen una entrada y una salida y vuelven al punto de partida. Así el método que queda es chico. Nunca se parte por una arista del ciclo.
- **3.4 Fernflower.** Verificar `notDecomp=0` y reproducir desde el fuente; Fernflower vacía en silencio métodos con bucles de varias entradas (memoria decompiled-source-fidelity). Si un método fusionado sale vacío o mal descompilado, se deshace la fusión de ese componente y se reporta.
- **3.5 Medir y verificar.**

## Fase 4. Retornos disfrazados: el patrón despachador

Caso Emlyn: el planificador 94AA elige una tarea de la tabla 94EF y salta con `JP (HL)`, y cada tarea termina con `JP 94AA`. En Java debería quedar `while (true) { tarea(); }`, con cada tarea como un método que hace `return`.

- **4.1 Detección** (en `RoutineManager`, con hechos del `StackAnalyzer`). Hay un despacho dinámico D (`JP (reg)` o `RET` con destinos T1…Tn) tal que:
  - para cada Ti, todos los caminos de su rutina terminan en un salto incondicional a la cabeza H del bloque que contiene D, o a D mismo, sin pasar por otro despacho;
  - la pila al saltar a H es la misma que al despachar, sin datos propios dejados (`dataOnTopAt`).
- **4.2 Transformación.**
  - D se genera como llamada: `$Ti()` con una cadena de `if` o un `switch` sobre el registro, como ya se hace para el `CALL 162C`.
  - El `JP H` final de cada tarea se genera como `return`: `tailJump` se reemplaza por `returnFromMethod` para los saltos marcados como "vuelta al despachador".
  - La rutina de H queda con el bucle adentro.
- **4.3 Casos borde.**
  - tareas que salen por otro camino, sea retorno no local o `StackException`;
  - tareas compartidas entre despachadores;
  - la ISR de Emlyn, que alterna dibujo y física y es otra forma de despacho.

  Si un Ti no cumple el patrón, queda como hoy.
- **4.4 Medir.** Los 62 miembros del partido de Emlyn deberían bajar a pocos.

### Lo que midió la fase 0 (2026-10-28)

Con `jumpCycleComponents()` y una sonda temporal que anotaba quién creaba cada rutina, cruzado con las
activaciones de la grabación (bajo qué CALL o vector corrió cada dirección):

- Ningún miembro de ningún ciclo, en 7 juegos y 42 componentes, es destino de CALL en la grabación: los ciclos
  no son rutinas del juego que se llaman en rueda.
- 22 componentes corren enteros bajo una sola activación (casi siempre el programa principal, `root`), 15 más
  son código de `root` al que además entra una rutina llamada con `JP` sin desapilar (la familia que resuelve
  `unwind`), 4 son código que solo recorrió la exploración.
- Los componentes grandes salen de `StateBytecodeGenerator.splitIfTooLargeForOneMethod`: al pasarse de 1500
  bytes la rutina se partía **en cada frontera de bloque**. Emlyn: el programa del partido era una sola rutina
  (BA29) de la que salieron 31 de los 32 miembros, y la ISR (963E) dio los otros dos componentes; Target
  Renegade: 15 de los 17 miembros vienen de partir F408. Los componentes chicos restantes salen de
  `Routine.splitVirtualRoutines` (partir donde salta otra rutina).

Conclusión: no hay máquinas de estado del juego; el `runJumps` lo fabrica el corte por tamaño. La fase 3.3
("extraer regiones sin ciclo como métodos llamados, nunca cortar por una arista del ciclo") pasa a ser el
trabajo principal y las fases 1, 2 y 4 quedan para los ciclos que sobrevivan.

### Corte por regiones (hecho el 2026-10-28)

Una rutina que no entra en un método se parte en **partes**: cada una es una región dominada por una
dirección (todo camino desde las entradas de la rutina pasa por ella, así que es su única entrada), y sale a
varios lugares. `ControlFlowStructuring.outlines(keep)` elige, mientras el padre siga sobre `keep` bytes, la
región más grande que entra en `keep` (y mide al menos 64 bytes) que no contenga ninguna entrada desde afuera
(`entriesFromOutside`, puntos de captura, retornos no locales, aterrizajes), y la quita; donde una parte ya
elegida sigue cuenta como entrada despachada por el padre, para que ninguna región posterior la contenga por el
medio. Así una secuencia larga queda en trozos llamados uno tras otro, y el cuerpo de un bucle en trozos que
salen al `continue`/`break` del bucle; no hace falta un árbol de bucles aparte: los dominadores y las salidas
múltiples lo cubren.

`RoutineManager.outline` mueve los bloques a la rutina nueva y anota `Outlined(padre, salidas, leaves)`: cada
salida es `Continuation(to, from)`, adónde sigue y desde qué instrucción; puede estar en el padre, ser la
entrada de una parte hermana, un punto de captura de una llamada de la parte, o estar fuera de la rutina
(entonces la hace el padre con su `tailJump` normal, con el sitio original para las reglas de pila). En el
generador la parte devuelve `void` si tiene un solo desenlace, `int` si tiene varios: el índice de la salida, o
-1 cuando la rutina terminó con RET. El padre emite la llamada de cada parte una sola vez (`callPart`), con el
despacho de los desenlaces (`if (o == i) goTo(salida)`), y toda otra referencia salta a esa etiqueta.

Lo que hizo falta para que Fernflower digiriera el padre (TR lo rechazaba con "parsing failure"):
- Las partes son **nodos** del grafo del padre en `ControlFlowStructuring` (y las hermanas, del de una parte):
  un nodo que sigue en las salidas de la parte. Así el despacho de una parte es un nodo real con sus
  predecesores, y las regiones con varias entradas lo tienen en cuenta.
- **Todo** salto pasa por `goTo` → `headerFor` (antes los manejadores de captura, los destinos dinámicos y el
  despacho de cabecera iban directo a la etiqueta): un salto que entra a una región por una entrada que no es
  la cabecera pone `nextAddress` y va a la cabecera. Con regiones anidadas, `headerFor` da la cabecera de la
  región **más externa** que no contiene al origen; esa cabecera despacha a la siguiente hacia adentro.
- Los puntos de captura de una llamada son sucesores de la llamada en el grafo.
- `isEnteredFromOutside` y compañía ven las partes como parte de su padre (`owns`): lo que una parte hace lo
  hace su padre, así las salidas de las partes no son entradas externas del padre; y en `edges()` un salto de
  una parte hacia afuera de la familia se atribuye al padre, que es quien lo ejecuta (para los ciclos de saltos
  y los saltos a ancestro).
- Un padre o una parte nunca vuelven al corte por bloques: en el reintento de Fernflower se cortan más partes
  o se quedan como están. El corte por bloques de un padre dejaba las salidas de sus partes en otras rutinas y
  las partes entraban a un ciclo de saltos, con `runJumps` tomando sus índices como direcciones.
- Dos reglas de la convención de pila que el corte destapó: después de una llamada simulada (`JP (HL)` con la
  continuación empujada) cuya continuación es una rutina propia, el sitio termina el método en vez de caer en
  el bloque siguiente (antes salía bien porque el `JP (HL)` cerraba su pieza); y `leaveWithOwnData` solo saca
  el dato propio cuando está arriba de la pila (`peek`), porque el sitio puede alcanzarse por un camino que no
  lo empujó y arriba queda la dirección de retorno del padre.

Tests: `aRoutineTooLargeForOneMethodIsCutIntoTasksCalledFromItsLoopNotJumpedBetween` (planificador con dos
tareas por `JP (HL)`), `aRoutineTooLargeForOneJavaMethodIsCutIntoPartsOfAtMostThatSizeCalledOneAfterTheOther`
(secuencia recta de 4 KB: dos partes en cadena), `aPartOfALoopBodyThatBreaksOrContinuesReportsWhichToTheLoop`
(parte con dos salidas, `break` y `continue`), `aSimulatedCallWhoseContinuationIsARoutineOfItsOwnEndsTheMethod…`;
`targetRenegadeE0D6…` afirma ahora que E0D6 corre en el método del bucle que atrapa su llamada, y el Java
esperado de `aCalleeThatPopsItsReturn…` cambió a la forma estructurada (el salto del manejador ya no pasa por
`setNextAddress`). `MiniZX.switchStack` registraba la corrutina nueva después de arrancar su hilo: una carrera
que dejaba a veces el mapa de pilas corrupto (apareció como un test que fallaba solo según el orden).

### Fase 4, hecha (2026-10-28, noche): el bucle vive en el método de la cabeza

Con las partes de varias salidas, un componente de saltos con cabeza se pliega sin código nuevo de protocolo:
`StateBytecodeGenerator.foldJumpCyclesIntoTheirHeads` toma cada componente de `jumpCycleComponents` que tiene
cabeza y anota a los demás miembros como partes de ella (`Outlined(cabeza, salidas, leaves)`, las salidas de
`ControlFlowStructuring.whole()`): la tarea vuelve a la cabeza, a una hermana o afuera con el índice de la
salida, y el ciclo deja de existir para `flow()`, así que no hay `runJumps`. La cabeza es el miembro llamado
(una llamada tiene que volver a quien llamó), o si ninguno lo es, el que más entradas recibe.

Lo que entra por el medio de una tarea pasa por la cabeza: `invokeTransformedMethod` y `enter` llaman al
padre de arriba con `setNextAddress`; los stubs de entrada también; `invokeMethod` del runtime consulta
`headOf` (un método generado) para la entrada por reflexión (ISR, `MiniZX.run`, `jump`); y la cabeza despacha
al empezar, y cada cabecera de región despacha, todas las entradas de sus partes (`entriesOf`: la entrada de
la parte y lo que se entra desde afuera adentro de ella), con el salto llevando la dirección interior
(`goTo(from, target, carry)`) y sus entradas contadas como raíces de la estructura (si no, el despacho entra al
bucle sin pasar por la cabecera y Fernflower lo rechaza: así falló `$F408`, y `$BA29` desbordaba la pila).

Un componente no se pliega, y queda en `runJumps`, cuando no es un bucle sino llamadas y retornos:
- algún miembro que no es la cabeza es llamado (`twoRoutinesThatJumpToEachOtherRunThroughATrampoline`);
- un salto entre miembros es un salto a ancestro (`goesBack`);
- un RET de un miembro toma datos empujados por otro miembro: la continuación plantada la corre quien salta
  (`leaveWithOwnData`), y plegado ese salto es una llamada de parte que no la corre
  (`aStackResetDropsTheReturnIntoTheExecutionThatMadeTheCall`).
Hubo una cuarta regla ("una rutina que el ciclo llama salta de vuelta a un miembro sin desenrollar", TR 5B2C…
y D780/D783/D794) que se quitó el 2026-10-30: la corrida 1028-outline11, con esos dos plegados, ya pasaba
completa por bytecode; lo que fallaba era Fernflower, resuelto después con las entradas de las partes como
raíces de la estructura.

Resultado: Emlyn sin `runJumps` (los 5 componentes plegados: FE61/FE65, 603C/6078, 9BEA, 6865 con 10 tareas,
5F9E); Target Renegade 31 → 9 casos (pliegan la tabla DBB6 con 10 tareas, DAE9 y D8FA; quedan los tres de
arriba). Ambos completos por bytecode y por fuente (corrida 1028-outline12). Test:
`routinesJumpingToEachOtherAroundAHeadAreALoopInTheHeadsMethodAndTheOthersItsParts`;
`aRoutineEnteredInItsMiddleWhoseTailLoopsBackToItsHead…` afirma ahora el plegado.

### Fase 4, cierre (2026-10-30): sin reflexión y sin ciclos en Target Renegade

- Sin reflexión en el runtime: la clase traducida genera `runTranslated(dirección)`, un `switch` sobre todas
  las direcciones con método que entra por el método de la rutina que lleva su bucle (`setNextAddress` cuando
  no es su inicio; `runJumps` si todavía es miembro de ciclo); `jump`, `goOnFrom` y la ISR lo usan.
  Reemplaza `headOf` y la búsqueda de métodos por nombre. Cuesta unas 4 líneas de fuente por método (Emlyn
  +1400 líneas), todas al final de la clase. Los fuentes commiteados en `minizx/emulation` anteriores a esto no
  tienen `runTranslated`: con el runtime actual fallan en su primer `jump`; hay que regenerarlos (nocturna
  1030-all1, lanzada el 2026-10-30 a la 01:44 sobre los 20 juegos con reportes y fuentes).
- Un solo dueño de "adónde sigue una instrucción": `RoutineManager.goesOnAt(dirección, rutina, hechos,
  conLlamadas)`, a partir de los hechos del `StackAnalyzer` y de la misma clasificación que usa el emisor
  (`exitOfJump`). Lo usan el grafo de saltos entre rutinas (`edges`/`flow`: ciclos, saltos a ancestros,
  `routinesThatMayUnwind`) y la estructura de control de cada método (`ControlFlowStructuring`). Antes cada
  uno clasificaba por tipo de instrucción y el grafo no veía los hechos: la llamada simulada de F408
  (`PUSH continuación; JP (HL)`, que el generador ya emitía como llamada) contaba como salto y armaba el falso
  ciclo E695/F408. Ahora es arista de llamada y el ciclo no existe. El orden de los sucesores se conserva
  (continuación primero, después los destinos): `nestedLoopsEachEnteredInTheMiddle…` cambia de cabecera si no.
- Dirección del desacople que pidió Fernando: los trucos (llamada simulada, reseteo de SP, retorno sacado con
  POP, salto por RET) los reconoce el circuito SE → `AddressAction` → hechos del `StackAnalyzer` → generador,
  y el `StackAnalyzer` ya es compartido (aprende de la grabación y de la SE). La clasificación por sitio vive
  en `RoutineManager` (`exitOf`, `exitOfJump`, `goesOnAt`) y la consume el generador; las acciones de SE
  deciden por eventos del mismo analizador. Queda como paso posterior que las acciones de SE consulten esa
  misma clasificación en vez de decidir aparte.
- Una salida de una parte hacia una entrada interior de otra parte (Renegade F280 → F28C dentro de F36E) va
  por el bloque de llamada de la hermana con la dirección cargada (`partContaining` en `tailJump`/`goOn`), en
  vez de `invokeTransformedMethod`, que llamaba al método cabeza de nuevo (un marco por vuelta).
- Resultado: Target Renegade `runJumps` 9 → 0 (5B2C y D783 plegados; E695/F408 ya no es ciclo), Emlyn 0;
  ambos completos por bytecode y por fuente (corrida 1030-edges1). Los `this.jump(dirección)` que quedan (31 en
  TR, 26 en Emlyn) son saltos a direcciones calculadas en tiempo de ejecución, despachados por el `switch`.
  El reporte `LOOP` muestra ahora las salidas de cada tarea con su sitio (`DB5D->[DB7D<-DB5D, …]`).

### Fase 4, diseño afinado (2026-10-28), para los ciclos que queden

Lo que hay hoy para apoyarse: `RoutineManager.jumpCycleComponents()` (fase 0.1) da cada componente con sus
miembros, sus **cabezas** (miembros por los que pasan todos los ciclos del componente, calculado quitando cada
candidato y buscando ciclos con jgrapht) y sus entradas desde afuera por causa (`call`, `jump`, `external`).
`ControlFlowStructuring` ya estructura regiones con varias entradas dentro de un método. El protocolo
`unwind()` reemplazó a las excepciones, así que "salir del bucle" ya no necesita `StackException`.

Un componente **con cabeza H** se genera como un bucle en el método de H; los demás miembros son tareas:

| Hoy (`runJumps`) | Con cabeza |
|---|---|
| todo miembro devuelve `int` (la próxima dirección o -1) | H es `void`; una tarea devuelve `boolean` (true: volvió a H; false: hizo RET y el bucle termina), o `void` si nunca hace RET |
| salto de tarea a H: `return H` | `return true` |
| salto de tarea T a tarea U: `setNextAddress(U); return dueño` | `return $U();` (tail call: lo que U devuelva vale para T) |
| salto de tarea a rutina de afuera X: `$X(); return -1` | `$X(); return false;` (se dejó el bucle, el RET de X vuelve al que llamó al bucle) |
| RET de una tarea: `return -1` | `return false` |
| en H, salto o despacho `JP (HL)` a una tarea T: `return T` | `if (!$T()) return;` y `goto inicio` (Fernflower lo deja como `while (true) { ... continue; }`) |
| CALL desde afuera a una tarea T: `runJumps(T)` | `setNextAddress(T); $H();` y H despacha `isNextPC(T)` al empezar, antes de su propio código |
| entrada externa en medio de una tarea: stub `setNextAddress(x); runJumps(H)` | igual, con `$H()` |
| entrada por reflexión en una tarea (`MiniZX.run`, ISR, `jump`): `invokeMethod` encadena `runJumps(next)` | si la tarea devuelve true, `invokeMethod` sigue en `headOf(tarea)` (un `switch` generado como `pagedCopy`) |
| después de `$T()` en H | `if (unwinding()) return;` como ya hace `runJumps` (es lo que `afterInvoke` emite para las rutinas que pueden desenrollar) |

Condiciones, las de 4.1 más la de cabeza: el componente tiene al menos una cabeza (si hay varias, la que
más entradas desde afuera recibe; las otras entradas van por stub); al saltar a H no se dejan datos propios
(`ownDataLeftForOthers`, el mismo chequeo que hoy en `tailJump`). Un componente sin cabeza (irreducible) sigue
en `runJumps` y se reporta. Los bucles anidados dentro de un componente (ciclos que no pasan por H pero sí por
otra cabeza interior) no entran en la primera versión: el `boolean` no distingue "sigo el bucle interior" de
"sigo el exterior"; se miden primero.

Qué toca: `createMethod` (tipo por rol), `tailJump`, `invokeTransformedMethod`, `returnFromMethod`,
`StateBytecodeGenerator` (sin `runJumps` para los componentes con cabeza, stubs a `$H()`, despacho inicial de H),
`SpectrumApplication.invokeMethod` (`Boolean` + `headOf`). Primero el test en `RecordedProgramTests` con el
patrón planificador (H despacha por `JP (HL)` a dos tareas que vuelven con `JP H`, una termina con RET), con la
expectativa del Java completo como en los casos del informe; después los juegos: Emlyn (94AA), Dizzy, Manic
Miner, y las expectativas de `renegade128At7C46` y `renegadeF28C` que hoy fijan `runJumps`.

## Fase 4b. La dirección viaja como dato: parámetro de entrada y valor de retorno (pendiente, anotado el 2026-10-28)

Hoy hay dos encarnaciones de la misma variable de estructuración de No More Gotos:
- `nextAddress`, un campo del objeto: quien salta a una entrada por el medio lo escribe (`setNextAddress`) y el
  método que lo recibe pregunta `isNextPC(x)` al entrar o en la cabecera de su región, consumiéndolo.
- el índice que devuelve una parte: dice por dónde siguió y el padre lo despacha con `if (o == i)`.

Unificarlas: la dirección viaja como dato, hacia adentro como **argumento** y hacia afuera como **retorno**, con
`switch` en los dos extremos.
- Un método con entradas por el medio toma `int at` y empieza con `switch (at)` a sus etiquetas; la llamada
  normal pasa su propia entrada (o una sobrecarga sin argumento). `this.$BA29(0xC7EE)` en vez de
  `setNextAddress(0xC7EE); this.$BA29()`. Las cabeceras de regiones anidadas reciben la dirección como variable
  local y hacen su propio `switch`.
- Una parte devuelve la **dirección** a la que siguió (-1 si terminó con RET) y el padre hace `switch` sobre
  ella: es lo mismo que hoy hace la cabecera con `isNextPC`, pero con el valor en la mano.
- La entrada por reflexión (`invokeMethod`, `headOf`) y la ISR pasan el argumento en vez de escribir el campo.
- El campo tiene otro uso, que no es de flujo: `unwind` deja ahí adónde se desenrolla (`ownAddress`,
  `isOwnAddress`). Ese queda aparte, es otro concepto; separarlo ya es una ganancia.

Desaparecen `setNextAddress`, `isNextPC` y el campo para el flujo; queda un solo mecanismo, visible en el
fuente. Toca al generador (`goTo`, `callPart`, `dispatchEntry`, `invokeTransformedMethod`, los stubs de
entrada) y a `SpectrumApplication.invokeMethod`; se comprueba con la batería y los dos lockstep. Conviene hacerlo
antes que los tres ciclos que quedan en Target Renegade, porque simplifica el código por el que se resisten.

## Legibilidad del Java, en orden (anotado el 2026-10-28 sobre JSW)

Medido sobre JSW con el generador de hoy: sin trampolín ni métodos `int` de ciclo, pero el bucle principal
`$34463` es un método de 2108 sentencias con 14 etiquetas; con `-Dtranslation.methodBytes=700` se parte en
partes de hasta 1008 sentencias y las etiquetas solo se reparten (26 en total, igual). Las etiquetas son de tres
tipos: escaleras `if (flag) {} else { if (flag) …` (cadenas de `JR cc` a un mismo destino), cabeceras con
`isNextPC` (nuestra variable de No More Gotos) y `break`/`continue` con etiqueta (salida a una cola común, o
vuelta a la cabecera de un bucle exterior desde uno interior). Lo que ordena cada cosa, en este orden:

1. **Corte por estructura, no solo por tamaño.** Sacar como parte toda región de una sola entrada (y una o pocas
   salidas) aunque la rutina quepa en un método: es la misma maquinaria de `outlines()` con otro criterio de
   parada (tamaño mínimo de la parte y pocas salidas, en vez de "hasta que el padre entre"). Métodos más chicos y
   con nombre propio por región; las etiquetas se van con las regiones que las contienen.
2. **Fase 4b**, la dirección como dato: `switch` en vez de cadenas de `isNextPC` y de `if (o == i)`.
3. **Recuperación de condiciones**: `flag(64, false)` → `A == 0`, `B() == 0`, etc., propagando el resultado de
   la ALU que fijó la bandera a la condición que la lee. Es lo que más legibilidad da por línea, y es una pasada
   aparte de la estructuración.
4. **No More Gotos completo** solo si escribimos nuestro emisor de fuente desde `ControlFlowStructuring`
   (condiciones de alcance, refinamiento, impresión de Java) y Fernflower desaparece con sus fallos y
   reintentos. Mejora las escaleras (una condición compuesta) y deja igual las cabeceras; para las salidas de
   varios niveles cambia `break label` por banderas y guardas repetidas por nivel, que en Java no es mejor.
   Vale por el control del resultado, no por las etiquetas.

## Fase 5. Despachos dentro de un método

Los `JP (reg)` cuyos destinos caen en el mismo método ya se generan como saltos a etiquetas: `invokeDynamicCall` usa `getLabel`. Hay que verificar que, después de las fases 2 y 3, la tabla de comandos de la música de Dizzy (F9B4) quede como `if`/`switch` sobre HL dentro del método del reproductor, y que Fernflower la descompile.

## Fase 6. Limpieza y documentación

- Los métodos `int` de miembros, `runJumps` y `setNextAddress` entre miembros quedan solo para componentes irreducibles. Si un juego no tiene ninguno, `runJumps` no se genera, como ya pasa con JSW y Wally.
- Si cambian, actualizar las expectativas de `RoutinesTests` y las particiones de Wally y DD en `GameBytecodeCreationTests`. Las de `RoutinesTests` se regeneran con DumpAssert y `apply_dumps.py` (memoria exact-stack-calling-convention).
- Actualizar las memorias forked-footprint, dizzy-translation y exact-stack-calling-convention, y la sección 9 de este plan.
- Un commit por fase, con el neto de líneas y qué se reusó.

## 7. Verificación: comandos

Hay que cargar `~/detodo/spectrum/investigacion/tools/traduccion/run/env.sh`. Sus funciones son `build`, `translate <test> <log> [skipDecompile]`, `lock <nombre> <rzx> <entrada> [opciones]`, `replay <rzx> <clase> <entrada>` y `counts <clase>`. Una sola JVM a la vez, con `-Xmx2g`.

- **Emlyn:**
  - `lock` sobre r3 y r4 con `-Dcopies=E000:5F:8:9BBF,E300:26:9:9AF7`;
  - regenerar Emlyn.java y hacer `replay` de r3 y r4;
  - fuzz con `-DfuzzFrom=9000 -DfuzzTo=14300 -DfuzzSeed=2|4 -DfuzzAvoid=2:4`;
  - salir del partido con T, semillas 1 y 3.
- **Dizzy:**
  - la ruta del RZX tiene espacios, así que `lock` necesita un enlace sin espacios;
  - `lock` completo y `replay`;
  - fuzz con semilla 1 (frames 20000–45000) y semilla 2 (60000–90000).
- **Wally y DD:** regenerar y comparar con los fuentes commiteados. La partición de DD está en su test.
- **JSW:** su traducción no debe cambiar.
- **Suite:** `mvn -o -q -B test` en translation/translator. Hoy da 84 tests, 41 saltados.

## 8. Riesgos y decisiones abiertas

- **Cortar un RZX** a mitad de frame exige que el primer frame tenga los fetches y los IN restantes. Si no funciona, se corta en el borde de frame.
- **Fernflower** maneja bien los métodos grandes y estructurados, como muestra JSW. Lo que lo rompe son los bucles con varias entradas.
- **El límite de 64 KB por método** acota la fusión en Emlyn. La salida es extraer regiones sin ciclo como métodos llamados.
- **Los retornos no locales (`StackException`) se mantienen.** Son la forma Java de "volver al nivel principal": F877 en Dizzy, 9534 en Emlyn.
- **Variantes de código automodificable** (copias E000+ en Emlyn): cada variante es otra rutina. Hay que verificar que no formen ciclos con el original.
- **Ciclo irreducible de verdad:** queda en `runJumps`, documentado con su causa.

## 9. Resultados por fase

| Fase | Juego | Miembros | Componentes | notDecomp | Lockstep / fuente / fuzz | Commit |
|---|---|---|---|---|---|---|
| base | JSW | 0 | 0 | 0 | — | d5be2f045 |
| base | Wally | 0 | 0 | 0 | completo / completo / — | d5be2f045 |
| base | DD | 3 | ? | 0 | completo / completo / — | d5be2f045 |
| base | Dizzy | 48 | ? (fase 0) | 0 | completo / completo / semillas 1 y 2 | d5be2f045 |
| base | Emlyn | 67 | ? (fase 0) | 0 | r3 y r4 completos / completos / semillas 2 y 4 | d5be2f045 |
| 0 (medido sobre los fuentes de la nocturna 1012-fall, miembros = métodos `int`; componentes = SCC de los `return dirección`) | Emlyn 60 (32,4,3,2,2) · Dizzy 23 (11,6,3,3) · DD 5 · Wally 0 · Target Renegade 46 (17,12) · Rainbow Islands 62 (12,12,10,7) · Renegade 26 (9,5) · Renegade 128 33 (8,5,4,3) · Manic Miner 9 (6,3) · Skool Daze 117 (casi todos sueltos, 258 `runJumps` desde afuera) · Head over Heels 20 · Fairlight 22 · Monty 13 · R-Type 13 · Great Escape 13 · Exolon 15 · Zynaps 6 · Equinox 2 · DD2 2 | | 0 en 1012-fall | 19/20 completos (Batman 415) | e396ab725 |
| 3.3 corte por regiones (2026-10-28, corridas 1028-outline7..9) | Emlyn: casos de `runJumps` 60 → 23 en 5 componentes del juego (FE61/FE65, 603C/6078, 9B23…, 67F7… de 11, 5F9E…), 4 partes, traduce en 2-3 min y Fernflower descompila al primer intento · Target Renegade: 48 → 31 en 6 componentes del juego (despachador DB9D…DC1B, bucle E695/F408, 5B2C…, D780/D783/D794, DAE9…, D8FA…), 2 partes | | 0 | Emlyn y TR completos por bytecode y por fuente | pendiente de commit |
| 4 bucle en la cabeza (2026-10-28, corrida 1028-outline12) | Emlyn: `runJumps` 23 → 0 · Target Renegade: 31 → 9 (quedan E695/F408, 5B2C…, D780/D783/D794, ver arriba) | | 0 | Emlyn y TR completos por bytecode y por fuente | 56cc0d4a7 |
| 4 cierre: sin reflexión, `goesOnAt`, sin regla nested (2026-10-30, corrida 1030-edges1) | Emlyn: 0 · Target Renegade: 9 → 0 | | 0 | Emlyn y TR completos por bytecode y por fuente; los 20 juegos en la nocturna 1030-all1 | 84dc865d4, 12eb15715 |

## 10. Segunda parte: dejar de depender de la pila y de SP

La dirección de retorno no le sirve a Java: lo que hay que conservar es a qué nivel vuelve el control y los datos que el juego lee de la pila. Cada uso raro de la pila que el `StackAnalyzer` ya detecta se reemplaza por su equivalente en el dominio Java; donde el uso no se reconoce con seguridad se mantiene la pila exacta, así se migra un idiom por vez.

| Uso en el Z80 | Detección actual | Equivalente Java |
|---|---|---|
| `LD SP,pantalla` + ráfaga de `PUSH` | `pushedValues`, `LD SP` | `ScreenWriter` / copia de arreglo |
| `LD SP,tabla` + `POP` | `dataConsumedBy` | iterador o arreglo |
| Parámetros a continuación del `CALL` | `shiftedReturns`, `returnsConsumedBy` | argumentos y `return` |
| `PUSH dir; RET`, tablas por dirección de retorno | `calledThrough`, `jumpingUsingRet` | `switch` o llamada directa |
| Descartar el retorno y salir varios niveles | `nonLocalRets`, `poppedCallSites` | `return` con resultado o excepción tipada |
| `LD SP,tope` (volver al menú) | `afterStackReset` | salida hasta el bucle principal |
| `PUSH`/`POP` de registros | ejecución simbólica | variables locales |

Etapas, cada una verificada en Emlyn, Dizzy, Equinox, JSW, Wally y DD:

1. Lockstep que compara todo menos SP y la zona de pila (la conoce el `StackAnalyzer`); compara registros, RAM restante, pantalla y puertos.
2. `PUSH`/`POP` de registros a variables locales.
3. Parámetros a continuación del `CALL` y saltos por dirección de retorno a argumentos y `switch`.
4. Retornos no locales y reinicios de pila a excepciones o retornos tipados.
5. Volcados con `PUSH` y lecturas de tablas con `POP` a `ScreenWriter` e iteradores.

Riesgos: solo se ve lo grabado (la huella con forks amplía, la pila exacta queda como respaldo); la ISR apila donde esté SP, así que un volcado con `PUSH` solo se convierte si el tramo es atómico (`DI` o tolerado); cuando la RAM del Java deja de ser igual a la del Z80, la verificación pasa a ser por lo observable.

## 11. Tercera parte: código versionado (código que se automodifica)

Va antes que las otras dos: sus saltos dinámicos alimentan `runJumps` y sus reglas de pila son las que más se rompen.

### Diagnóstico

Hoy cada etapa trata el código automodificado por su cuenta:

1. La huella guarda solo la primera versión de cada instrucción y una marca por byte (`modifiedCode`); para `CALL`/`JP` reescritos anota destinos en `calledThrough`. El resto de las versiones que vio la grabación se pierde.
2. El modelo es una instrucción por dirección (rutinas, ejecución simbólica, `StackAnalyzer`, generador). Lo que no entra se resuelve en ejecución: `executeMutantCode` y un `jump` dinámico.
3. `executeMutantCode` es un segundo Z80 escrito a mano que crece con cada juego (Dizzy: CB; Equinox: `JP cc`, `INC`/`DEC`).
4. `calledThrough` mezcla los trampolines de la ROM con los `CALL`/`JP` de operando reescrito; se distinguen con `size() > 1`, y las reglas de pila (`pastPoppedReturn`, `planPoppedReturnsOfRewrittenCalls`, `consumedReturns`) dependen de esa señal. De ahí salieron D015 de Equinox, la regresión de Dizzy en el frame 172, el cuelgue de Emlyn y el C608 de Equinox.
5. La ejecución simbólica decodifica una sola versión y, como los bytes mutantes quedan sin proteger, sus propias escrituras cambian lo que decodifica después: la versión traducida depende del orden de exploración. Los sucesores de las otras versiones solo se exploran si cada test los agrega a mano.
6. Hay tres detectores con criterios distintos (huella, `findMutantCode`, `AbstractInstructionSpy`) y dos sitios que emiten `executeMutantCode`.
7. No hay noción de longitud ni de bloque: cuando una versión cambia de longitud o de límites, el modelo por dirección se rompe (el log de 185 GB). `CodeVariant` lo resuelve, pero con 17 cadenas hex pegadas a mano y `-Dcopies` en el lockstep.
8. Cada test repite el armado con sus propios parches.

### Principio

La grabación ya vio todas las versiones que importan: se guardan todas y un solo dueño las clasifica. Cada versión se traduce con el decodificador y el generador normales; no hay intérprete propio.

| Tipo | Criterio | Traducción | Ejemplos |
|---|---|---|---|
| Operando de datos | mismo opcode, varía un inmediato, no salta | leer el operando de memoria (ya existe) | Emlyn AB94 `CP n`, 989B; Dizzy FDFD |
| Versiones de instrucción | misma longitud, cambia opcode o destino | `switch` sobre las versiones grabadas; la ejecución simbólica ve la unión de sucesores | Dizzy E2DF, E4F8, E299; Equinox CEC1, D035, D015; Emlyn 9ACE |
| Versiones de bloque | cambian longitudes o límites | `CodeVariant` alimentado desde la grabación, con el mapa de relocación exportado | Emlyn 9AF3, 9BBF |
| Una sola versión ejecutada | se escribe pero no cambia lo ejecutado | código normal | Emlyn 94DD, 961A |

Una versión no grabada falla con `unknownCodeVersion`, como `unknownCodeVariant`. Si después se quiere tolerar, el respaldo es la implementación de instrucciones del emulador, nunca una escrita a mano.

### Pasos

0. **Inventario, sin cambiar comportamiento.** La huella guarda las versiones de cada dirección; un reporte por juego (Emlyn, Dizzy, Equinox) da la cantidad de sitios por tipo y tres ejemplos de cada uno. Se compara con el catálogo conocido antes de tocar el generador.
1. **Versiones como dato único.** Las versiones reemplazan a `modifiedCode` y al registro de `calledThrough` en la huella; todo el código queda protegido durante la ejecución simbólica; un solo método traduce desde la grabación y los tres tests lo usan.
2. **Versiones de instrucción.** Cada versión se decodifica; la ejecución simbólica sigue la unión de sucesores; el generador emite el `switch`. Se borran `executeMutantCode`, sus dos emisiones, la rama `rewritten` y el caso de `CALL` en `mutantCodeInInstruction`. `calledThrough` queda solo para trampolines y las reglas de pila preguntan al clasificador.
3. **Versiones de bloque.** `CodeVariant` sale de la grabación; se borran las cadenas hex de Emlyn y `-Dcopies`.
4. **Limpieza.** Se borran los detectores viejos y lo que haya quedado sin uso (por ejemplo `exploreOrphanContinuations`).

Cada paso se verifica con los comandos de la sección 7: lockstep y reproducción desde el fuente de Emlyn, Dizzy y Equinox, lockstep con teclas al azar, Wally y DD idénticos, suite.

### Riesgos

- Una versión que la grabación no vio aparece jugando: el lockstep con teclas al azar lo detecta, y la huella con forks amplía lo explorado.
- La ejecución simbólica es por dirección: la unión de sucesores sirve mientras las versiones compartan longitud; si no, el sitio es de bloque.
- Las reglas de pila de D015 hay que rehacerlas sobre el clasificador; el estado al empezar está en el commit de arranque.

### Avance

- **Paso 0** (ffdda0492): la huella guarda las versiones y `CodeVersions` las clasifica. Equinox 16 sitios, Dizzy 30, Emlyn 54; coinciden con el catálogo.
- **Pasos 1 y 2**: `calledThrough` queda solo para los trampolines; el generador emite `switch` sobre `codeHash` con cada versión generada por `InstructionsBytecodeGenerator`; `executeMutantCode` ejecuta la instrucción con el propio emulador (comparte `mem`) en lugar del intérprete escrito a mano, y queda como respaldo para versiones no grabadas y para los juegos sin grabación (Wally, DD). La divergencia de Equinox en el frame 1139 era que el CALL de D015 apilaba su retorno y el catch del pop virtual no lo desapilaba.
- **Protección**: no se protege todo el código durante la ejecución simbólica. Necesita ver los datos que el juego escribe en operandos, por ejemplo el SP que Emlyn guarda en el `LD SP,nn` de 9C19; con todo protegido, Emlyn diverge en el frame 5669.

- **Paso 3** (5dc5f81bc): las variantes de bloque salen de la grabación (`blockRegions`, `recordBlockContent`, `mergeBlockRegions` para las subregiones de una plantilla) y se borran las 17 cadenas hex de Emlyn. `pc()` reporta la dirección original dentro de las copias relocadas, así que el lockstep ya no necesita `-Dcopies`. 9AF7 graba 8 de las 9 variantes que había a mano; la novena nunca corre en r3 ni en r4 y, si aparece jugando, falla con `unknownCodeVariant`.
- **Paso 4**: `RealCodeBytecodeCreationBase.exploreRecording` es el único armado de una traducción desde grabación (Emlyn, Dizzy, Equinox y `RecordedProgramTests`). Las continuaciones huérfanas se detectan con el decodificador. El detector de `AbstractInstructionSpy` se fue en el paso 2.

### Pendiente

- Respaldo para una forma de bloque no grabada (hoy falla con `unknownCodeVariant`): ejecutar la región con el emulador como `executeMutantCode`.
- Sin datos de grabación (traducción estática), un `CALL` a un `JP (HL)` que es código hace fallar la ejecución simbólica con NullPointerException en `executeAllCode`; con grabación anda (test en `RecordedProgramTests`).
- Equinox conserva `untranslated(31006)` y `untranslated(32456)`, nunca alcanzados por la grabación.
- Lockstep con teclas al azar: Equinox (desde el frame 3000, semilla 1) diverge en el frame 13520 porque el `CALL 9E80` de 8684 nunca volvió en la grabación, su continuación 8687 no está traducida y `$8681` termina en silencio; Emlyn r4 (desde 9000) llega a 6A89, sin traducir. Son huecos de cobertura de la grabación, no de las transformaciones. Después de un `CALL` cuya continuación no es código el generador no emite `untranslated` (sí lo hace después de otras instrucciones); hacerlo fallar en voz alta pide revisar antes los `CALL` cuyo destino descarta el retorno.

### Tests

- `CodeVersionsTest`: clasificación, sucesores, bloques, versiones que sobreviven a `learnFrom`.
- `SpectrumApplicationTest`: el respaldo con el emulador (cambios de opcode, operandos, flags, saltos, `BIT`/`SET` indexados, `CALL` como llamada Java) y `LD A,R` con IFF2.
- `RoutinesTests`: `switch` sobre versiones, respaldo sin versiones, continuación plantada.
- `RecordedProgramTests`: programas chicos en bytes que pasan por el mismo camino que los juegos grabados (huella, ejecución simbólica, generación). Un test por truco: CALL y JP C reescritos, cambio de opcode, operando reescrito, los dos POP de JSW, datos después del CALL (Emlyn 721D), RET como salto (Dizzy), CALL al JP (HL) de la ROM (Emlyn 162C), continuación plantada (Emlyn 616E), reinicio de pila (Dizzy F877) y el patrón de D015.

Cada truco nuevo que aparezca en un juego entra primero como test en rojo en `RecordedProgramTests` o `RoutinesTests`.

## 12. Cuarta parte: sin la cerca

La cerca limitaba la ejecución simbólica a las direcciones que la grabación o las ramas bifurcadas habían ejecutado, más los sucesores estáticos de los saltos condicionales. Evitaba decodificar datos como código, pero también dejaba sin traducir los caminos que la grabación no recorrió. Se reemplazó por detecciones, cada una con su test en `RecordedProgramTests`:

- **Interior de una instrucción**: no se decodifica desde adentro de una instrucción grabada (Emlyn FD20), salvo que esa dirección también sea el comienzo de una instrucción grabada (`LD A,n` cuyo operando es otra entrada).
- **Datos después de un `CALL`**: los bytes que saltea un `CALL` con continuación aprendida, si nunca corrieron, son parte del `CALL` (`Footprint.span`); su bloque los incluye, así que `split` no deja la continuación del otro lado (Emlyn 648E).
- **Código parcheado**: todo `LD (nn)` que la huella ejecutó o que quedó traducido marca como mutante la instrucción que escribe, aunque la grabación haya visto una sola versión (Equinox 7879, 7C08).
- **Ramas bifurcadas imposibles**: una rama que vuelve a su nivel de pila con un `POP` del retorno no se reúne todavía con la grabación; una que salta por registro a un callejón sin salida (pantalla nunca ejecutada, 0000) no enseña nada (Emlyn 6075 → 57CB).
- **Ejecución simbólica**: al volver a la otra rama de un condicional restaura también los retornos sacados con `POP`.

Resultado: los cinco juegos pasan el lockstep completo y la reproducción de las fuentes; con teclas al azar Equinox llega al frame 24077 (13520 con cerca) y Emlyn igual que con cerca. Se fueron `fenced`, `reachable`, `admitAsCode`, `isRestrictedToRecording` e `isStaticSuccessor` (−32 líneas).

Pendiente: lo que con teclas al azar todavía no se traduce es código al que solo se llega por destinos que dependen de datos y que las grabaciones nunca ejercitaron (la tabla del menú de Emlyn en 5C9E, el código de Equinox cerca de 7E00 que parchea por punteros).
