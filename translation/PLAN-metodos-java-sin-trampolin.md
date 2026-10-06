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
