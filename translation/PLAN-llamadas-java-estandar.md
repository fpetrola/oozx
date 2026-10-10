# Plan: llamadas Java estándar en la traducción Z80 → Java

Escrito el 2026-10-06 para ejecutarse en otra sesión. Rama `traduccion`. Todo lo que dice "hoy" describe el árbol de trabajo a esa fecha, incluidos los cambios sin commitear que se listan al final.

## 0. Objetivo y criterio de éxito

El Java generado tiene que usar llamadas y retornos nativos: un `CALL` del Z80 es una invocación de método, un `RET` es `return`, y ningún mecanismo de tiempo de ejecución reconstruye la pila del Z80 ni despacha por valor salvo donde el destino es genuinamente dinámico.

Hecho cuando se cumplen las cinco condiciones:

1. En `Emlyn.java`, `Game.java` (Wally) y `DD.java` regenerados no hay invocaciones a `call(`, `ret(`, `jump(`, `setNextAddress(`, `isNextPC(` ni bloques `catch (StackException`. Métrica: `grep -c` de cada uno, hoy en Emlyn: `jump` 163, `isNextPC` 174, `catch (StackException` 92, `this.call(`/`this.ret()` en cada CALL/RET.
2. Las cuatro grabaciones reproducen hasta "rzx finished" en el Lockstep: Emlyn (`emlyn_r3.rzx`, entrada `fe65`), Wally (`eawally/eawallyCsabaComplete.rzx`, entrada `8185`), Dynamite Dan (`testTranslateDynamite`, entrada `c804`), Jet Set Willy (`jsw_r2.rzx` o `jsw-full-ext*.rzx`, ver `testTranslateWillyToJava`).
3. `RoutinesTests` (25 tests de texto descompilado) pasa con expectativas regeneradas, y el diff de expectativas es solo el cambio mecánico de convención.
4. Las tres fuentes descompiladas (`Emlyn.java`, `Game.java`, `DD.java` en `translation/translator/src/main/java/com/fpetrola/z80/minizx/emulation/`) recompilan y reproducen su grabación con `PlayTranslatedGame`/`ReplayClass`.
5. El runtime (`SpectrumApplication`, `MiniZX`) es más chico que hoy: desaparecen `callSlots`, `call`, `ret`, el `pop` que consume slots, el borrado de slots en `SP(int)`, y al final `jump`, `setNextAddress`, `isNextPC`, `nextAddress`.

Reglas de CLAUDE.md que gobiernan el trabajo: achicar, reutilizar lo que existe, un dueño por concepto, sin comentarios que expliquen duplicaciones. Si un paso pide escribir una segunda implementación de algo que ya existe, el paso está mal planteado: parar y preguntar.

## 1. Qué hay hoy y por qué

### 1.1 La convención de pila exacta (commit `0df5d9eb2`, 2026-10-05)

- `InstructionsBytecodeGenerator.visitingCall` emite `call(returnAddress)` antes de invocar al método del callee (directo con `invokeTransformedMethod`, por trampolín con `callThrough`, o `untranslated`).
- `InstructionsBytecodeGenerator.visitingRet` emite, dentro de `createIfs`, `ret()` y luego `invokeDynamicCall(invocationsSet, poppedValue)` (despacho por valor con un `if` por destino conocido) y `returnFromMethod()`. `RoutineBytecodeGenerator.returnFromRoutine` es `ret(); return`.
- `SpectrumApplication` (translator, paquete `minizx`): `call(int)` hace `push` y guarda el SP en `callSlots`; `ret()` devuelve `-1` si el slot sacado es el del CALL pendiente más interno y si no la dirección sacada; `pop()` consume el slot si coincide; `SP(int)` borra los slots (cambio de pila). `MiniZX.interrupt()` hace `call(PC)` antes de invocar la ISR. `executeMutantCode` usa `call` para un `CALL` automodificado. El `catch` del pop virtual hace `HL = pop()`.
- Antes de ese commit (ver `git show 0df5d9eb2^:translation/bytecode/src/main/java/com/fpetrola/z80/bytecode/generators/InstructionsBytecodeGenerator.java`), `visitingCall` era `createIfs(call, () -> invokeTransformedMethod(jumpLabel))` y `visitingRet` era `createIfs(ret, () -> returnFromMethod())` cuando no había destinos dinámicos, o `pop()` + `invokeDynamicCall` cuando los había. Esa es la forma a la que hay que volver, con la corrección de la sección 3.

### 1.2 El caso que la forzó

Emlyn Hughes, rutina `616E`: hace `LD HL,660D; PUSH HL`, llama a varias rutinas y sale por `RET` condicionales (`6193`, `619C`, `61A0`, `61F8`) o por saltos de cola (`623A JP 5D24`, `6243 JP 5D6D`, `61F2 JP 63B9`). `660D` es `EXX; RET`: restaura el banco alterno y vuelve al llamador real de `616E`. Las rutinas destino de los saltos de cola (`5D24`, `5CF6` vía `63BF`) también se llaman con `CALL` desde otros sitios. Entonces el `RET` de `5D55` y el de `5CF9` son, en la misma instrucción, a veces un retorno al `CALL` y a veces un salto al `660D` plantado.

Con la convención Java anterior fallaba de dos formas: el `return` de Java no hacía `SP += 2`, así que el `660D` quedaba en la pila y los `POP` siguientes sacaban basura; y la rutina no podía distinguir en tiempo de ejecución "arriba hay una dirección plantada" de "arriba hay datos del llamador", porque los CALL de Java no dejaban su dirección de retorno en memoria. La pila exacta lo resolvió por fuerza bruta.

### 1.3 Qué otros idiomas se apoyan hoy en la pila exacta, y su forma estructurada

| Idioma | Hoy | Forma estructurada (dueño) |
|---|---|---|
| Retorno plantado + salto de cola (`616E`/`660D`) | `ret()` devuelve el valor sacado y se despacha | Continuación plantada: el `PUSH` de una dirección de código se modela como "al terminar, correr `660D`"; el `JP 5D24` con la continuación plantada se emite `$5D24(); $660D(); return;` (`StackAnalyzer` detecta, SE lo modela como llamada con continuación, finder asigna, generador emite) |
| Parámetros inline (`CALL 721D; "texto!"`, `721D: POP HL; ...; JP (HL)`) | `HL = pop()` lee la dirección real de la pila; `callContinuations` da dónde sigue | Ya es estructurado: `callContinuations` por sitio de llamada (`returnShifted`); el valor de HL es constante por sitio (`sitio + 3`). Hay que emitirlo como constante en el `catch` del pop virtual, no leerlo de la pila |
| Entrada a mitad de cadena (`FE65` con retornos en la pila del snapshot) | `MiniZX.run(entry)` sigue en `mem[SP-2]` cuando el método de entrada retorna | Igual: es dato del snapshot, no convención. Se mantiene |
| Tabla de saltos `PUSH HL; RET` | `ret()` devuelve el valor, `invokeDynamicCall` despacha | Igual que antes de `0df5d9eb2`: `pop()` + despacho; es `JumpUsingRetAddressAction` |
| Cambio de pila `LD SP,0000`, `LD (924B),SP` / `LD SP,(924B)` | `SP(int)` borra `callSlots` | Solo mueve datos; sin slots no hay nada que borrar |
| Interrupción | `call(PC)` + invocación de la ISR; `RETI` es `ret()` | La ISR es una llamada Java desde `pc()`; `RETI` es `return` |
| `JP (HL)` con destino desconocido | Desde 2026-10-06 `jump(HL)` (falla con "no translated routine at X") | Se mantiene: es dinámico de verdad |

### 1.4 Código automodificable: qué hay y qué toca a la convención

El traductor maneja cuatro formas de código que cambia, y solo una roza la convención de llamadas:

| Forma | Ejemplos | Cómo se traduce hoy | Dependencia de la pila exacta |
|---|---|---|---|
| Operando inmediato que cambia (`CP n`, `LD A,n`, `LD DE,nn`, `LD SP,nn`, `LD IX,nn`) | Emlyn `AB94`, `B066`, `9AF4`, `989C`, `9925`; menú `5E4F`, `6D6E` | La instrucción se traduce normal y el operando se lee de memoria en tiempo de ejecución (`mem(AB94)`, `mem16(9AF4)`), porque `mutantCodeInInstruction` excluye los bytes de operando inmediato | Ninguna |
| Opcode que cambia | Emlyn `9ACE`/`9AD8` (`RRCA`↔`RLCA`), `9AE0` (`INC L`↔`DEC L`); Wally `B8BD`; DD `CA57`, `CA58`, `E990`, `E9AF`; JSW `8D5C` | `executeMutantCode(address)` en `SpectrumApplication`: un intérprete mínimo que decodifica desde `mem` y ejecuta `LD r,n`, `LD rr,nn`, `LD r,r'`, `RLCA/RRCA/RLA/RRA`, `LD (DE),A` y `CALL nn`; cualquier otro opcode lanza "self-modified opcode". El generador ignora el valor devuelto y sigue en la instrucción siguiente, así que solo sirve para instrucciones que no cambian de largo ni transfieren control, salvo `CALL` | **Solo la rama `CALL nn`**: hoy hace `call(address + 3); invokeMethod(nn)`. En la fase 2 pasa a `invokeMethod(nn)`. Verificar con el juego que la usa (buscar `executeMutantCode` en las fuentes generadas y comprobar cuál tiene un `CALL` con destino parcheado; en Emlyn, Wally y DD todos los sitios son instrucciones de 1 byte) |
| Región entera reescrita (código generado o plantilla rellenada por `LDIR`) | Emlyn `9AF7–9B1B` (9 variantes, bytes variables en `9AFB`) y `9BBF–9C1C` (8 variantes, bytes variables en `9BDA`) | `translateCodeVariants(start, end, variableStart, base, hex...)`: cada variante se copia reubicada (`E000`, `E300`...) con sus saltos internos corregidos, se explora como rutinas propias (entradas externas) y en cada entrada a la región el generador emite `codeHash` + un `if` por variante que hace `jumpInto(copia); return`, más `unknownCodeVariant` si ninguna coincide. `RoutineManager.originalAddress` mapea copia → original | Ninguna: las copias son rutinas normales; su `RET` final vuelve por la convención que esté vigente. Con llamadas estándar la rutina original llama a la copia y retorna, el mismo esquema que cualquier salto de cola |
| Código cargado o copiado después del snapshot (núcleo del menú desde `D980`, pantalla de teclas tapada por la tabla de vectores) | Emlyn `5C00–5DFF`, `FD00–FF1D` | `Footprint.codeBytes` + `install`: el SE trabaja con los bytes que tuvo cada instrucción al ejecutarse, protegidos salvo los mutantes | Ninguna |

Puntos a revisar al ejecutar las fases, todos pequeños:

- Fase 2, paso 4: la rama `CALL` de `executeMutantCode` queda en `invokeMethod(nn)`. Si el callee de un `CALL` parcheado tuviera continuación por sitio (`callContinuations`) o sacara su dirección de retorno, el intérprete no lo sabría; hoy tampoco. Dejarlo así y anotar el caso si aparece.
- Las ventanas de "pila como repositorio" del pre-render y del dibujo de Emlyn (`LD SP,HL` ... `PUSH`/`POP` como punteros, `LD SP,(9251)` / `LD SP,nn` guardado en `9C1A`) no contienen `CALL` ni `RET`; con llamadas estándar el Java deja de empujar retornos ahí, que es lo que el Z80 hace de todos modos. `SP(int)` ya no tiene slots que borrar.
- Las copias reubicadas se exploran como entradas externas con pila desconocida: sus `RET` disparan `returningToUnknownAddress`. Con la fase 1 hay que confirmar que ninguna continuación plantada se mezcla con ellas (no debería: `616E` y sus destinos están en el menú, lejos de las regiones variantes).
- `jumpInto(copia)` puede emitir `jump` si la copia está en un ciclo de saltos; cae en la fase 4 como cualquier otro `jump`.

## 2. Dueños

| Concepto | Dueño | Qué ya tiene |
|---|---|---|
| Qué hay en la pila y quién lo puso | `StackAnalyzer` (`translation/routines/.../transformations/StackAnalyzer.java`) | Pila sombra `entries` con `Entry(value, pc, returnAddress)`: el `pc` es la instrucción que empujó. `consumedReturns`, `callContinuations`, eventos `jumpUsingRet`, `returnAddressPopped`, `returnShifted`, `simulatedCall`, `droppingReturnValues`, repositorio de pila |
| Qué camino explora y cómo continúa | `SymbolicExecutionAdapter` (`translation/se/.../SymbolicExecutionAdapter.java`) y las `AddressAction` en `translation/se/.../actions` (`CallAddressAction`, `RetAddressAction`, `JumpUsingRetAddressAction`, `PopReturnCallAddressAction`, `AddressActionDelegate`, `ExecutionStackStorage`) | `CallAddressAction.getNext` ya continúa en `callContinuations` cuando pisa una llamada; frames con foto de pila (`keepStackStorageOf`); `SEStackListener` recibe los eventos |
| Forma de las rutinas | `RoutineFinder`, `RoutineManager`, `Routine` (`translation/routines`) | `getVirtualPop`, `getReturnPoints`, `ownerFlowingInto`, `retInstructionAction`, `splitVirtualRoutines`, `isEnteredFromOutside`, `jumpsAfterStackReset`, `isJumpedIntoFromOtherRoutine`, `routinesInJumpCycles`, `codeVariantsAt` |
| Emisión de Java | `InstructionsBytecodeGenerator`, `RoutineBytecodeGenerator`, `StateBytecodeGenerator` (`translation/bytecode`) | `visitingCall`, `visitingRet`, `visitingJP`, `invokeDynamicCall`, `createIfs`, `jumpInto`, `invokeTransformedMethod`, `throwAtVirtualPop`, etiquetas, `splitRoutineAt` |
| Runtime del Java generado | `SpectrumApplication`, `MiniZX` (`translation/translator/.../minizx`) | `invokeMethod` (reflexión por nombre `$XXXX`), `callThrough`, `untranslated`, `run`, `interrupt`, `pc` |
| Verificación | `Lockstep` y `ReplayClass` (herramientas de sesión, ver sección 6), `RoutinesTests`, `GameBytecodeCreationTests` | `-DexactSP=true` compara SP y 12 bytes de pila por instrucción enmascarando los bits 3/5 de F |

## 3. Fases

Cada fase termina con las cuatro grabaciones en "rzx finished" o con una explicación escrita de por qué no, antes de seguir. Una JVM a la vez, heap ≤ 2 GB (`-Xmx2g` en `argLine`, `-Xmx1g` en el Lockstep): la máquina tiene poca memoria.

### Fase 0: línea base

1. Commitear o apartar los cambios sin commitear (sección 7). El usuario decide; no commitear sin que lo pida.
2. Compilar: `mvn -o -q -pl emulator,translation/translator -am install -DskipTests` desde la raíz del repo. JDK 21, no 25.
3. Correr las cuatro reproducciones con las herramientas de la sección 6 y anotar: frame final, métricas de `grep -c` sobre los tres `.java` descompilados, cantidad de rutinas en `target/game-routines.txt` por juego.
4. Guardar los tres `.java` descompilados actuales en una carpeta de comparación.

### Fase 1: continuaciones plantadas

Objetivo: que un `RET` que consume un `PUSH` de dirección de código deje de ser un despacho por valor y pase a ser "la rutina que empujó corre la continuación al salir".

1. `StackAnalyzer`. Cuando `visitingRet` (el de la pila sombra, el que llama a `jumpingUsingRet`) ve que la entrada sacada no es dirección de retorno, hoy agrega el destino a `dynamicInvocation` y dispara `jumpUsingRet(ret, pc, targets)`. Distinguir dos casos con la información que ya está en `Entry`:
   - El valor empujado es código (`routineManager.isCode(value)`) y lo empujó una instrucción `PUSH` de una rutina P que **todavía está en la cadena de llamadas** (su frame existe, o hay un `CALL`/`JP` pendiente desde ella): es una continuación plantada por P. Nuevo evento en `StackListener`: `plantedContinuation(pushPc, retPc, continuation)`. Si la `RET` está dentro de la propia P, es "salida de P con continuación"; si está en una rutina a la que P saltó de cola, es "salto de cola con continuación".
   - Cualquier otro caso (valor calculado, `PUSH HL; RET` de tabla): sigue siendo `jumpUsingRet`.
   El evento se dispara en modo `collecting` (grabación) y se recuerda por `pushPc` como hoy se recuerdan `callContinuations`, para que la segunda pasada (exploración) lo tenga.
2. SE. En `SEStackListener`, al recibir `plantedContinuation`:
   - Marcar en el frame de P que el `PUSH` en `pushPc` no es un dato: el SE no debe esperar que ese slot vuelva a leerse como dato.
   - Para el salto de cola `JP x` de P ejecutado con la continuación arriba de la pila, crear para esa dirección una acción con la forma de `CallAddressAction` (ver `CallAddressAction.getNext`, que ya sabe continuar en una continuación grabada): "ejecutar x como llamada, al volver continuar en `continuation`". No escribir una clase nueva si `CallAddressAction` con un `callContinuations` por sitio de salto alcanza; si hace falta una clase, decir en qué clase existente iría y por qué no.
   - Para la `RET` de P que consume la continuación: `RetAddressAction` ya sabe que una `RET` termina el frame; el finder necesita saber que esa salida corre `continuation` antes de volver.
   - Dentro de la rutina saltada (5D24), la `RET` que consume la continuación se trata como retorno normal del frame: no `jumpUsingRet`, no caso de despacho.
3. `RoutineFinder`/`Routine`. Registrar en la rutina P, por instrucción de salida (la `RET` o el `JP x`), la continuación (`Map<Integer,Integer> exitContinuations` al lado de `getReturnPoints`/`getVirtualPop`, en `Routine`, que es su dueño). La rutina `660D` tiene que existir como rutina propia (hoy ya existe: `{660D:660E}`).
4. Generador. En `InstructionsBytecodeGenerator.visitingRet`, si la rutina tiene continuación para esa `RET`: `createIfs(ret, () -> { invokeTransformedMethod(continuation); returnFromMethod(); })`. En `visitingJP` con destino inmediato, si hay continuación para ese salto: `invokeTransformedMethod(target); invokeTransformedMethod(continuation); returnFromMethod();` en lugar de `jumpInto`. El `PUSH` que planta la continuación no se emite (el SE lo marcó).
5. Verificar con Emlyn primero (es el único juego que tiene el idioma): `egen.sh` + `elock.sh`. Después Wally, DD, JSW para confirmar que no cambió nada en ellos (sus `game-routines.txt` y `.java` deben ser idénticos salvo la emisión del paso 4 si aparece el idioma).

Criterio de la fase: en `Emlyn.java`, los `RET` de `5D55`/`5CF9` son `return` puros; `$616E` contiene `this.$5D24(); this.$660D(); return;` o equivalente; Emlyn llega a "rzx finished" con la pila exacta todavía activa (la fase 1 no toca la convención; así se aísla el cambio).

### Fase 2: volver a la convención Java

1. `InstructionsBytecodeGenerator.visitingCall`: volver a la forma pre-`0df5d9eb2` (sin `call(returnAddress)`).
2. `InstructionsBytecodeGenerator.visitingRet`: sin `ret()`. Si `invocationsSet` está vacío: `createIfs(ret, returnFromMethod)` (o la continuación de la fase 1). Si no está vacío (tabla `PUSH HL; RET`): `pop()` + `invokeDynamicCall` + fallo explícito si ningún caso coincide (`jump(popped)`, como se hizo el 2026-10-06 para `JP (HL)`). `RoutineBytecodeGenerator.returnFromRoutine` vuelve a ser `return`.
3. Pop virtual (parámetros inline). El `catch (StackException)` generado hoy hace `HL = pop()`. Sin dirección de retorno en memoria, HL tiene que valer la constante `sitioDeLlamada + longitud del CALL` que el SE conoce (`callContinuations` está indexado por sitio; `returnShifted` trae `callSite`). Emitir esa constante en el `catch` del frame que hizo la llamada. Revisar `throwAtVirtualPop`/`throwAfterVirtualPop` y `PopReturnCallAddressAction`: el mecanismo es anterior a la pila exacta y funcionaba para JSW/Wally/DD; lo que se agregó en `0df5d9eb2` (`returnShifted`, continuaciones) se conserva.
4. Runtime: borrar `callSlots`, `calls`, `call(int)`, `ret()`, el consumo de slot en `pop()`, el borrado en `SP(int)`; `MiniZX.interrupt()` sin `call(PC)`; `executeMutantCode` llama al método del callee sin `call`. Buscar en `MiniZX`/`SpectrumApplication` cualquier otro uso con `grep -n "call(\|ret()\|callSlots"`.
5. `MiniZX.run(int entry)`: se mantiene (es la entrada a mitad de cadena); comprobar que sigue funcionando cuando el método de entrada retorna sin haber hecho `ret()`.
6. Lockstep: `-DexactSP=true` ya no puede exigir igualdad de SP ni de pila (el Java no empuja retornos). Volver a correr las cuatro grabaciones sin `exactSP` o con una comparación que enmascare los slots de retorno (el analizador sabe cuáles son: `Entry.returnAddress`). No borrar la comparación exacta: dejarla disponible para diagnósticos.
7. `RoutinesTests`: regenerar expectativas con el procedimiento de la sección 6.3 y revisar que el diff sea solo la convención.

Criterio: las cuatro grabaciones en "rzx finished"; `grep -c "this.call(\|this.ret()"` da 0 en los tres `.java`; `SpectrumApplication` y `MiniZX` tienen menos líneas que en la fase 0.

### Fase 3: partir las entradas internas (`setNextAddress`/`isNextPC` → 0)

Hoy, entrar a una rutina por una etiqueta interna se emite como `setNextAddress(x); $rutina();` y la rutina arranca con una cadena `if (isNextPC(x)) goto x` (`RoutineBytecodeGenerator.jumpInto`, `insertIfNextPc`, `invokeReturnPoints`). La forma estructurada es que cada punto de entrada sea una rutina.

1. En `RoutineManager`/`RoutineFinder`, para cada dirección que `isEnteredFromOutside` acepta (saltos desde otra rutina, `jumpsAfterStackReset`, `externalEntries`, caída desde otra rutina), partir la rutina en esa dirección con `splitRoutineAt`/el mecanismo de rutinas virtuales que ya se usa en `splitVirtualRoutines`. La rutina original pasa a terminar con una llamada de cola al fragmento nuevo (`invokeTransformedMethod(next); return`), que el generador ya emite para `continuationOwner`.
2. Quitar `setNextAddress`, `isNextPC`, `nextAddress` del runtime y `insertIfNextPc`/`invokeReturnPoints` del generador cuando el conteo llegue a 0.
3. Trampa: no partir una rutina en su propio punto de entrada porque otra rutina salta ahí (guardas `address != entryPoint` y `finalI1 != routineAt.entryPoint` en `splitVirtualRoutines`, aprendidas con Emlyn `6CA0`).

### Fase 4: fusionar ciclos de saltos (`jump` → 0)

`jump(address)` existe para rutinas en `routinesInJumpCycles`: A termina en `JP B` y B en `JP A`. Hoy `jump` usa `StackWalker` para ver si el método destino ya está activo y, si lo está, tira `StackException` que el `catch` generado convierte en `goto`.

1. Medir primero: cuántos ciclos hay por juego y de qué tamaño (sumar las instrucciones de las rutinas de cada ciclo). Fernflower deja vacío un método que no puede estructurar (`// $FF: Couldn't be decompiled`, ver `BytecodeGeneration.getDecompiledSource` y su partir-y-reintentar hasta 5 veces); fusionar agranda métodos, así que el límite lo pone eso.
2. Fusionar en `RoutineManager` las rutinas de un ciclo en una sola rutina antes de generar; los `JP` entre ellas se vuelven saltos a etiquetas del mismo método, que el generador ya emite (`getLabel`, `goto_`).
3. Si un ciclo fusionado no descompila, dejarlo como está y anotarlo; el objetivo de la fase es llegar a 0 `jump` donde Fernflower lo permita, no forzarlo.
4. Al llegar a 0: quitar `jump` del runtime y los `catch (StackException)` que solo servían para ciclos. Los que sirven al pop virtual se van con la fase 2 si la constante del paso 2.3 los vuelve innecesarios; verificar.

### Fase 5: cierre

1. Regenerar `Emlyn.java`, `Game.java`, `DD.java` y verificarlos reproduciendo (sección 6.4).
2. Actualizar las notas de memoria del proyecto (`exact-stack-calling-convention` queda como historia: decir que se revirtió y por qué).
3. Informar el neto de líneas y qué se reutilizó, como pide CLAUDE.md.

## 4. Decisiones abiertas

- Una `RET` que el analizador no pueda atar ni a un `CALL` ni a una continuación plantada ni a una tabla (`PUSH` de un valor calculado consumido por `RET` en algunos caminos y retorno normal en otros): fallar con mensaje en el generador ("RET polimórfica en X") y preguntar, en lugar de reintroducir la pila exacta para esa rutina.
- `660D` (`EXX; RET`) como continuación: es una rutina de dos instrucciones; emitirla como método está bien. No inlinearla.
- La opción 2b del SE (admitir sucesores estáticos transitivos; ver sección 7) es independiente de este plan; si molesta durante las fases, desactivarla no es un problema.

## 5. Trampas conocidas (de la sesión del 2026-10-05)

- El listener de ejecución del adaptador SE debe quedar **primero** entre los listeners: su restauración de la foto de pila precede a lo que ve el analizador. Reordenarlos rompió Wally entero.
- Una acción que reemplaza a otra en la misma dirección hereda su foto de pila (`RoutineExecution.replaceAddressAction` → `keepStackStorageOf`).
- `jumpUsingRet` lleva la `Ret` porque el `RoutineManager` todavía no la registró en `beforeExecution`.
- `RoutineManager.reset()` borra `reachable`: los tests se contaminaban entre juegos sin eso.
- `LD SP,0000` que deja la pila nueva sobre el tope de la vieja no es "pila como repositorio" (guarda `CALLER_STACK` en el SE); ese modo se reconoce por un `LD (nn),SP` reciente y termina cuando SP vuelve a menos de 200 bytes del valor guardado.
- `target/game-routines.txt` y `ZxGame1.class` son del **último** test corrido: la suite completa los deja con Wally. Regenerar antes de razonar sobre ellos.
- Las fuentes descompiladas pueden tener métodos vacíos silenciosos: verificar reproduciendo, no solo con el Lockstep del bytecode.
- Los nombres de variables (`var74`) cambian entre corridas; un `.java` regenerado puede diferir del commiteado solo en nombres.
- La grabación de Emlyn tiene frames de 1 a 3 instrucciones (artefacto de SPIN en la aceptación de la interrupción); el footprint y el Lockstep deben correr con `rzx.advance=ins` y en modo estricto, los dos jugadores avanzando en paso.
- `game.fetchCounter` debe empezar en `emulator.playbackFetches()`; desde 0 el primer frame dura 31 millones de instrucciones.
- Emlyn genera código (`9AF3`, `9BBF`): el Lockstep necesita `-Dcopies=E000:5F:8:9BBF,E300:26:9:9AF7`.
- Los scripts de la cadena matan la traducción por `timeout`; si la traducción tarda más que eso, no escribe `ZxGame1.class` ni `game-routines.txt`, y el Lockstep compara la clase anterior sin avisar. Mirar la hora de modificación de `translation/translator/ZxGame1.class` antes de creer un resultado. La exploración estática del SE lleva la traducción de Emlyn a unos 15 a 25 minutos.
- El `GameBytecodeCreationTests` de Emlyn instala el footprint (`Footprint.install`), alimenta `callContinuations` y `dynamicInvocation`, agrega las direcciones de retorno de la pila inicial como entradas externas, traduce las rutinas de ROM `0038, 22B0, 0E44, 03F4, 2C8D` y las variantes de código. Si se toca el orden de esa preparación, el SE cambia de camino.

## 6. Comandos y herramientas

Las herramientas de la sesión están copiadas en `~/detodo/spectrum/investigacion/tools/traduccion/` (los scripts tienen la variable `S` apuntando al scratchpad de la sesión vieja: cambiarla a una carpeta propia antes de usarlos).

### 6.1 Compilar y traducir

```
cd ~/detodo/spectrum/versions/oozx
mvn -o -q -pl emulator,translation/translator -am install -DskipTests
cd translation/translator
mvn -o -q -B test -Dtest='GameBytecodeCreationTests#testTranslateEmlynToJava' -DargLine="-Xmx2g -Doozx.plugins=off -Dtranslation.skipDecompile=true"
```

`skipDecompile=true` genera solo bytecode (`ZxGame1.class` en `translation/translator`, `target/game-routines.txt`); sin esa propiedad también escribe `target/Game.java` descompilado (`ZxGame1`). Tests por juego: `testTranslateWallyToJava`, `testTranslateDynamite` (escribe `target/DD.java`), `testTranslateWillyToJava`, `testTranslateEmlynToJava`. Filtrar la salida con `grep -v "^Push frame\|^Pop frame\|^CREATE\|^empty$"`.

### 6.2 Lockstep (bytecode contra el emulador, instrucción por instrucción)

`check/Lockstep.java` se compila con `javac -cp "$(cat cp.txt):translation/translator/target/classes" -d check check/Lockstep.java` (`cp.txt` tiene los jars de `~/.m2`; agregar los `target/classes` de los módulos). Uso:

```
java -Xmx1g [-DexactSP=true] [-Dcopies=...] -cp check:$CP Lockstep <rzx> <carpeta con ZxGame1.class> ZxGame1 <entrada hex>
```

Emlyn: `~/detodo/spectrum/emlyn_r3.rzx`, entrada `fe65`, `-Dcopies=E000:5F:8:9BBF,E300:26:9:9AF7`. Wally: `~/detodo/spectrum/eawally/eawallyCsabaComplete.rzx`, entrada `8185`. DD: entrada `c804`, rzx según `testTranslateDynamite`. JSW: ver `testTranslateWillyToJava`. `egen.sh`, `elock.sh` y `wstep.sh` encadenan compilar, traducir y comparar. Opciones de diagnóstico: `-DdumpFrom`, `-DdumpTo`, `-DdumpFile` (volcado por PC con los métodos Java activos), `-DexactSP=true` (SP y pila).

### 6.3 Expectativas de `RoutinesTests`

Son comparaciones de texto descompilado. Después de un cambio de convención: agregar temporalmente un `DumpAssert` que escriba `<método>-<n>.expected` y `.actual` (la ruta va fija en el código porque el `<argLine>` configurado en surefire ignora `-DargLine`), correr la clase, aplicar con `apply_dumps.py <RoutinesTests.java> <salida> <carpeta de dumps> [tests] [tests a des-ignorar]`, y revisar el diff a mano: tiene que ser solo el cambio mecánico.

### 6.4 Fuentes descompiladas

`Emlyn.java` se regenera corriendo el test de Emlyn sin `skipDecompile`, anteponiendo `package com.fpetrola.z80.minizx.emulation;` a `target/Game.java` y renombrando `ZxGame1` → `Emlyn` (igual `DD.java` desde `target/DD.java` y `Game.java` de Wally). Después `mvn -o -q -pl translation/translator install -DskipTests` y reproducir: `PlayTranslatedGame <rzx>` (ventana) o `check/ReplayClass` (sin ventana). El criterio es "ended at ...: rzx finished".

### 6.5 Memoria de la máquina

Una JVM a la vez. Las corridas de Emlyn tardan: traducción con descompilación 1–2 min, Lockstep 2–5 min, la suite completa del módulo translator 2 min. Lanzarlas en segundo plano y esperar la notificación.

## 7. Estado del árbol de trabajo al escribir esto (sin commitear)

- `InstructionsBytecodeGenerator.visitingJP`: un `JP (HL)` sin caso conocido emite `jump(HL)` en vez de retornar en silencio (2026-10-06). Trampa: la variable del registro que llega a `visitingJP` es una `Composed16BitRegisterVariable`, no una `Variable` de cojen; para pasar su valor a un `invoke` hay que leerla con `methodMaker.invoke(register.getName())` (como hace `callThrough` con el trampolín).
- `RoutineBytecodeGenerator`: una instrucción que cae en una dirección sin rutina emite `jump(next)` en vez de dejar que el método termine (2026-10-06). Excluye los `CALL`: el sucesor de un `CALL` cuyo callee saca su dirección de retorno es la continuación grabada (el bloque siguiente de la rutina, por ejemplo `631F CALL 5D78` sigue en `6322` y no en `6320`), y el cuerpo del método ya fluye hacia ahí.
- `SymbolicExecutionAdapter.isStaticSuccessor` + `RoutineManager.admitAsCode`/`addressBefore`/`setReachable` copia/`reset` borra `reachable`: exploración estática acotada a las dos ramas de cada salto condicional (no `CALL`). Se probó una versión transitiva (caída secuencial y destinos inmediatos): el SE se perdía en `AF64` a 1.000.000 de pasos y la traducción pasaba de 1 a 15–25 minutos sin cubrir el medio tiempo; se descartó.
- `StackAnalyzer.shiftedReturns`: instrucciones (`RET` o `JP (HL)`) que retornan desplazadas a la continuación de un sitio de llamada; el generador no emite el fallo explícito de `JP (HL)` para ellas.
- `RemoteZ80Translator.Footprint`: `finalMemory` e `install(memory, sp)`.
- `GameBytecodeCreationTests` (Emlyn): preparación descrita en la sección 5.
- `Emlyn.java` regenerado; `PlayTranslatedGame` con modo en vivo (`playing`, 8000 fetches por frame).
- Pendiente del juego, no del traductor: el medio tiempo de Emlyn (`B8C7`, estado 1 por la tabla `B752`) y la vuelta al menú (`9534`) no están en la grabación; hace falta una grabación que los recorra. El informe de ingeniería inversa está en `~/detodo/spectrum/investigacion/reports/emlyn-hughes-ingenieria-inversa.md`.

## 8. Orden de trabajo sugerido para la sesión

1. Leer este plan, las notas de memoria `exact-stack-calling-convention`, `emlyn-recording-and-lockstep`, `decompiled-source-fidelity` y la sección "Query Shape" de CLAUDE.md.
2. Fase 0 completa antes de tocar código.
3. Fase 1 solo con Emlyn hasta el criterio; recién después las otras tres grabaciones.
4. Fase 2 con las cuatro grabaciones y `RoutinesTests`.
5. Fases 3 y 4 midiendo los conteos después de cada una.
6. Commitear por fase, cuando el usuario lo pida, con el neto de líneas en el mensaje.

## 9. Avance de la ejecución

### Fases 1 y 2 (hechas juntas, 2026-10-06)

Desviación del plan: la fase 1 se hizo junto con la 2. Con la pila exacta activa, un salto de cola con continuación plantada necesita `call(continuación)` para que el `RET` del destino vuelva bien, que es justo lo que la fase 2 borra; aislarla no daba seguridad y duplicaba trabajo.

Qué quedó, por dueño:

- `StackAnalyzer` registra, en la grabación y en la exploración, qué `PUSH` puso cada dato que consume un `RET` (`dataConsumedBy`), qué dato hay arriba de la pila en cada instrucción (`dataOnTopAt`), qué `RET` sacaron alguna vez una dirección de retorno real (`returnsConsumedBy`), qué sitios de llamada tienen su dirección de retorno leída por un `POP` (`poppedCallSites`), los valores empujados por cada `PUSH` (`pushedValues`) y los retornos desplazados (`shiftedReturns`). `learnFrom` copia todo desde el analizador de la grabación (el `Footprint` ahora lleva el analizador entero) y `forgetLearned` lo limpia en `SymbolicExecutionAdapter.reset()`.
- `RoutineBytecodeGenerator.plantedContinuation(push)`: un `PUSH` es una continuación plantada si siempre empujó el mismo valor, ese valor es la entrada de una rutina, la instrucción anterior es `LD rr,nn` inmediato al mismo registro y algún `RET` lo consume. El `PUSH` no se emite; un `RET` propio que lo consume emite `$continuación(); return`; una salida de la rutina (salto o caída a otra) con ese dato arriba emite `$destino(); $continuación(); return` (`leaveWithOwnData`).
- `leaveWithOwnData` también cubre el dato propio no plantado que consume un `RET` de otra rutina (Emlyn `5D78`: `EX (SP),HL` deja la dirección desplazada y cae en `5D7F`, cuyo `RET` la saca): después de invocar el destino emite `pop()`, y `jump(valor)` si el consumidor no es un retorno desplazado.
- `RET`: si consume datos propios y nunca sacó una dirección de retorno real, `pop()` y despacho (o solo `pop()` si es retorno desplazado); si consume datos de otra rutina, `return` común (lo resuelve la salida del que empujó); sin datos registrados, el despacho por `dynamicInvocation` de siempre.
- `CALL`: `push(retorno)` antes de invocar si el llamado lee su dirección de retorno con `POP` y la rutina no lo resuelve con pop virtual (`pushesReturnAddress`). Es pasar un dato, no reconstruir la pila.
- Se recuperó lo que `0df5d9eb2` había quitado: el `pop()` de la continuación empujada en las llamadas simuladas (`LD DE,A989; PUSH DE; JP (HL)` de Wally) y la constante del pop virtual (`HL = sitio + 3`).
- Runtime: sin `call`, `ret`, `callSlots`; `MiniZX.run` sigue con `pop()` cuando el método de entrada retorna; la interrupción ya no empuja PC.
- Lockstep: ahora también compara IX e IY (la divergencia de Wally apareció primero en IY). Sin `-DexactSP`.

Resultado: Emlyn, Wally y Dynamite Dan en "rzx finished" por lockstep y por reproducción del fuente recompilado; suite 83/0; las expectativas de `RoutinesTests` quedaron idénticas a las de antes de la pila exacta salvo espacios. Conteos:

| Fuente | `call(` | `ret()` | `jump(` | `setNextAddress(` | `isNextPC(` | `catch (StackException` |
|---|---|---|---|---|---|---|
| Emlyn | 0 | 0 | 257 | 189 | 174 | 92 |
| Game (Wally) | 0 | 0 | 3 | 3 | 5 | 1 |
| DD | 0 | 0 | 5 | 1 | 3 | 10 |


### Fase 3 (2026-10-06)

- `RoutineManager.isEnteredFromOutside` (movido desde el generador, que era quien lo calculaba) y `RoutineManager.splitAtEntriesFromOutside`, llamado por `StateBytecodeGenerator` antes del corte por tamaño: parte una rutina en una entrada desde afuera con `Routine.splitAt` (el `splitBlocksIfRequired` de siempre, que mueve pops virtuales y puntos de retorno) cuando el tramo desde la entrada solo alcanza su propio bloque y nadie de la rutina salta a su mitad, y solo si la rutina no está en un ciclo de saltos.
- La restricción a rutinas fuera de ciclos es necesaria: sin ella Emlyn bajaba `setNextAddress` de 189 a 78 pero subía `jump(` de 257 a 347 y los `catch (StackException` de 92 a 141, porque la entrada a la mitad de una rutina en un ciclo pasaba de `setNextAddress` + llamada a `jump()`.
- Resultado: Emlyn `setNextAddress` 189 → 177, `isNextPC` 174 → 166; Wally 3 → 1 y 5 → 3; Dynamite Dan 1 → 0 y 3 → 2. Lockstep y reproducción del fuente completos en los tres; suite 83/0; las particiones de Wally y Dynamite Dan en `GameBytecodeCreationTests` cambian en tres rutinas (bloques a los que se entra desde afuera pasan a ser rutinas).

### Fase 4: análisis (no implementada como estaba escrita)

Las componentes fuertemente conexas del grafo de saltos entre rutinas de Emlyn:

| Componente | Rutinas | Bytes Z80 | Qué es |
|---|---|---|---|
| `7D9E`, `94AA`, `AB5A`, `AE63`, `B0E8`... | 79 | 5948 | El planificador del partido y sus 8 tareas, que vuelven con `JP 94AA` y comparten código con saltos |
| `9869`, `9C9F`, `9DA2` | 3 | 858 | Mitad de dibujo de la ISR, cortada por el límite de 1500 bytes por método |
| `9E2F`, `A0AD`, `A12B` | 3 | 843 | Mitad de física de la ISR, mismo motivo |
| `A527`, `A5EC` | 2 | 288 | Final de la ISR |
| `5F9E`, `5FAA`, `5FB8`, `5FEE` | 4 | 91 | Bucle de selección del menú |

Fusionar cada componente en un método (lo que pedía la fase) no funciona: la grande no entra en un método de la JVM (64 KB de bytecode) ni la estructura Fernflower, y las de la ISR son justamente los cortes que hizo `splitIfTooLargeForOneMethod`. Además, una componente fusionada con varias entradas desde afuera vuelve a necesitar `isNextPC`: fusionar cambia `jump` por despacho de entrada, no lo elimina. Los `jump(` de Emlyn tienen 148 destinos distintos (`94AA` 22, `B42A` 17, `B113` 6...), así que no hay un patrón único que reconocer.

Propuesta para reemplazar la fase 4: trampolín por componente. Cada rutina de una componente de saltos devuelve un `int` con la dirección a la que salta (o un marcador de salida si ejecuta un `RET` que sale de la componente), y un único bucle por componente las encadena: `for (int next = entrada; next != SALIDA; ) next = switch (next) { case 0x94AA -> $94AA(); ... };`. Los saltos dentro de la componente pasan a ser `return destino`; las llamadas desde afuera llaman al bucle; dentro de la componente ya no hace falta `StackWalker` ni excepciones, y las entradas a mitad de rutina se resuelven partiendo (el corte de la fase 3 sin la restricción de ciclos, porque el trampolín ya no crea ciclos de llamadas). Es un cambio de forma de los métodos generados (devuelven `int`) y toca el generador, el runtime (`jump`, `isOwnAddress`, `nextAddress`) y las expectativas de `RoutinesTests`.

### Fase 4 implementada como trampolín (2026-10-06)

- `StateBytecodeGenerator` declara `runJumps(int)` antes de generar las rutinas y lo llena después: un `while (true) switch (next)` sobre las entradas de las rutinas que están en ciclos de saltos (`BytecodeGenerationContext.routinesInJumpCycles()`, que ya existía); cada caso hace `next = $X();` y el valor por defecto sale.
- `RoutineBytecodeGenerator`: los métodos de esas rutinas devuelven `int` (`createMethod`); `returnFromMethod` devuelve `-1` (salida, un `RET`); `tailJump(destino, sitio)` reemplaza los "`jumpInto` + `return`": entre miembros devuelve el destino (con `setNextAddress` si entra a la mitad), y si hay que correr un epílogo de datos propios (`leaveWithOwnData`) o el destino no es miembro, invoca y retorna. `invokeTransformedMethod` llama a `runJumps(X)` cuando X es miembro. Se eliminaron el `jump()` hacia rutinas en ciclos y los `catch` de reentrada que lo recibían.
- `SpectrumApplication.invokeMethod` sigue el trampolín cuando una invocación reflexiva devuelve una dirección; `jump()` ya no usa `StackWalker` ni lanza excepciones: invoca.
- El corte de la fase 3 ya no excluye rutinas en ciclos (el trampolín no crea ciclos de llamadas).
- Arreglos del camino: la detección de métodos sin descompilar aceptaba solo `void` (ahora `void|int`); el reintento de descompilación también corta en destinos de saltos dentro del bloque, no solo de `CALL` (`$9869` de Emlyn no se descompilaba con la forma nueva); las caídas a direcciones sin rutina emiten `untranslated(destino)` en vez de `jump`.

Resultado (fuentes regeneradas, reproducción completa de las tres grabaciones, suite 83/0):

| Fuente | `jump(` | `setNextAddress(` | `isNextPC(` | `catch (StackException` | métodos `int` |
|---|---|---|---|---|---|
| Emlyn | 7 (antes 257) | 83 (189) | 41 (174) | 5 (92) | 136 |
| Game (Wally) | 3 (3) | 1 (3) | 3 (5) | 1 (1) | 0 |
| DD | 3 (5) | 0 (1) | 0 (3) | 8 (10) | 3 |

Lo que queda:

- Los `jump(` restantes son despachos dinámicos sin caso conocido (`JP (HL)` o `RET` con valor no visto en la grabación): son dinámicos de verdad.
- `setNextAddress`/`isNextPC` restantes: entradas a mitad de una rutina cuyo tramo vuelve hacia atrás (un bucle que cruza la entrada). Sacarlas requiere duplicar el tramo por entrada o reestructurar el bucle; no se hizo.
- Los `catch (StackException` restantes son los pops virtuales (rutinas que tiran su dirección de retorno), un mecanismo anterior a este plan.
