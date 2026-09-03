# Plan: lo que queda entre el núcleo generado y ZXSpin

El núcleo generado (`GeneratedSpectrumZ80`, con memoria, bancos y contención adentro) corre a la
velocidad de Fuse. ZXSpin corre un 20 % más rápido que Fuse. Este documento dice dónde está el
tiempo que **no** es el núcleo, medido, y qué habría que cambiar en cada lugar para sacarlo. Está
ordenado por ganancia esperada sobre riesgo.

Todo lo que sigue se midió el 2026-09-03 con el arnés `Where` (sección 8), en el loop de la app
(`doOpcodes` + `eventDoEvents`, el mismo que corre la ventana), núcleo generado, JDK 21, dos
P-cores (`taskset -c 2,3`), bloques de un segundo y plateau de los últimos cuatro. Dos juegos:
Manic Miner (48K, sin AY, código indexado con IX) y Jet Set Willy 128K (con AY, redibuja los
atributos de la pantalla entera cada frame).

## 0. Dónde está el tiempo hoy

Cada parte que no es el núcleo, encendida de a una:

| | Manic Miner 48K | JSW 128K |
|---|---|---|
| solo el núcleo (sin síntesis de sonido, sin presentar la pantalla) | 15 240 fps | 8 542 fps |
| + síntesis de sonido (beeper / beeper + AY) | 15 442 | 8 231 |
| + presentar la pantalla | 14 298 | 9 208 |
| todo, como la app | 13 333 | 7 939 |

Sonido y presentación de la pantalla juntos cuestan un 10-12 %. **El otro 88 % está en el núcleo
y en lo que el núcleo llama por instrucción**: reloj, contención, marcado de pantalla sucia,
puertos. Ahí es donde hay que mirar.

Perfil a 1 ms (JFR, 7 000-14 000 muestras) del modo "todo", tiempo propio por método:

| Manic Miner | | JSW 128K | |
|---|---|---|---|
| `GeneratedSpectrumZ80.contend5x1` | **46,6 %** | `Display.dirty64` | **32,2 %** |
| `SpectrumZ80Clock.addTStates` | 16,9 % | `SpectrumZ80Clock.addTStates` | **28,4 %** |
| `Display.dirty64` + `dirty8` | 9,9 % | `decode` / `read` / `step` | 16,4 % |
| `decode_26` / `decode` / `step` | 11,7 % | `Keyboard.read` + `readPortInternal` | 6,3 % |
| `Display.updateBorder` | 4,2 % | `Display.updateBorder` | 4,6 % |
| | | `Ay.synthesise` | 2,8 % |

Y lo que el JIT no inlinea en el loop (`-XX:+PrintInlining`, contado por sitio de llamada):

| callee | sitios sin inlinear | razón | bytes de bytecode |
|---|---|---|---|
| `GeneratedSpectrumZ80.read` | 454 + 84 | callee is too large / too big | 78 |
| `GeneratedSpectrumZ80.write` | 188 + 20 | callee is too large / ya compilado aparte | 199 |
| `GeneratedSpectrumZ80.contend1x1` | 109 + 12 | callee is too large | 51 |
| `GeneratedSpectrumZ80.contend5x1` | 80 + 16 | callee is too large | 147 |
| `SpectrumZ80Clock.addTStates` | 58 + 32 | callee is too large | ~40 |
| `Ula.contendPortEarly` / `Late` | 14 + 14 | callee is too large | |
| `Display.copyCriticalRegionLine` | 12 | callee is too large | |
| `BlipSynth.offsetResampled` | 14 | hot method too big | 375 |

`MaxInlineSize` es 35 bytes: un callee más grande solo se inlinea en un sitio que el JIT ya vio
caliente, y con 600 sitios de acceso a memoria repartidos en 242 métodos `decode_N`, la mayoría
son tibios. Subir el presupuesto a mano (`-XX:MaxInlineSize=120 -XX:FreqInlineSize=1200
-XX:InlineSmallCode=8000`) dio +5 % en Manic Miner y −15 % en JSW, dentro del ruido: **la forma
del código es lo que decide, no el presupuesto**.

## 1. Contención por corridas: cinco búsquedas dependientes donde alcanza una

`contend5x1` es el 46 % del tiempo propio en Manic Miner. Es el patrón de las instrucciones
indexadas (`LD (IX+d),n`, `LD r,(IX+d)`, `INC (IX+d)`…: 81 sitios), y lo que hace es esto,
cinco veces:

```java
clock.addTStates(ula.contentionNoMreq[clock.getTStates()] + 1);
```

Cada búsqueda depende de la anterior (el índice es el reloj que la anterior acaba de mover):
cinco cargas encadenadas, cinco sumas, cinco escrituras al reloj. Los otros patrones son lo
mismo con otro número: `contend1x1` 656 sitios, `contend2x1` 551, `contend7x1` 21,
`contend4x1` 3. Fuse hace exactamente estas cinco búsquedas (`contend_read_no_mreq` cinco veces
en el `.dat` de opcodes), así que acá hay una ventaja sobre Fuse, no solo paridad.

**Qué cambia.** La corrida entera depende de una sola cosa, el T-state donde empieza. Una tabla
por largo de corrida, `run5[t]` = lo que consumen cinco accesos de un T-state que arrancan en
`t` (los retardos más los cinco), convierte las cinco búsquedas en una:

```java
clock.addTStates(ula.run5[clock.getTStates()]);
```

**Dónde.** Las tablas son de la ULA, que ya tiene `contention` y `contentionNoMreq` (`byte[]`
de 70 908, se llenan en `Machine.java:202`). Van al lado, `run1`, `run2`, `run4`, `run5`,
`run7`, llenadas desde `contentionNoMreq` en el mismo lugar: 5 × 71 KB en `byte[]` (el máximo es
7 × 7 = 49, entra). Y el `contend(address, times, tstates, kind)` del `PhaseProcessor`, que es lo
que el generador especializa en `CoreGenerator.contendBody`, pasa a usarlas; el generador emite
lo que el modelo diga, no hay que tocarlo.

**Cuánto.** El 46 % de Manic Miner debería bajar a un quinto: +25-35 % en juegos indexados, menos
en otros. Es el ítem más grande y el de menos riesgo.

**Qué lo custodia.** Los tests de contención de `machine/core`
(`ZXSpectrumContendedMemoryTests2`, `ZXSpectrumULATests`: los que fallaron cuando el núcleo de
máquina saltó los listeners); y `Where` en los dos juegos.

**Hecho (2026-09-03).** La ULA construye las corridas a pedido (`Ula.noMreqRun(n)`) y llena sus
propias tablas (`tablesFor`, que estaba en `Machine`); `FusePhaseProcessor.contend` las usa; el
especializador aprendió que un elemento de un arreglo conocido en un índice literal es ese
elemento, y que `.length` de uno conocido es un literal; el núcleo las recibe como campos y
`contend5x1` quedó en `clock.addTStates(noMreqRun5[clock.getTStates()])`.
`ContentionRunsTest` compara cada tabla con el bucle, en seis máquinas, desde cada T-state.

Medido, alternado, mismos cores: Manic Miner 14 151 / 13 743 → 16 368 / 14 898 fps (**+8 a
+16 %**), JSW 8 580 → 8 703 (ruido). Menos que lo estimado, y el perfil dice por qué: el helper
bajó de 147 a 46 bytes, pero 46 sigue por encima de los 35 que el JIT inlinea sin preguntar, así
que queda como llamada en 77 de sus 99 sitios, y adentro le quedan la consulta de página y la
llamada a `addTStates`. Las cinco búsquedas dependientes ya no están; lo que resta es la llamada,
que es el §2 (el reloj de ocho bytes) y el §3 (helpers de menos de 35).

## 2. El reloj: el timeout de la cinta se paga en cada suma

`SpectrumZ80Clock.addTStates` es el 17 % (Manic Miner) y el 28 % (JSW) del tiempo propio, y no
se inlinea en 90 sitios. Es esto:

```java
public void addTStates(int tStatesToAdd) {
  tStates += tStatesToAdd;
  if (timeout > 0 && tStatesToAdd >= 0 && (timeout -= tStatesToAdd) <= 0)
    timedOut();
}
```

La suma es una instrucción. El `if` son tres comparaciones y una escritura más, en la operación
más repetida de todo el emulador (media docena de veces por instrucción del Z80), y es lo que
pone al método por encima de los 35 bytes que el JIT inlinea sin preguntar. **Solo la cinta lo
usa**, mientras carga: diez `clock.setTimeout(...)` en `Tape.java`, todos "avisame dentro de N
T-states".

**Qué cambia.** "Avisame dentro de N T-states" ya tiene dueño: el `EventManager`, y el loop ya
pregunta `zxClock.getTStates() < eventNextEvent` por instrucción. Fuse hace exactamente eso con
la cinta (`tape_edge_event`, un evento por flanco). La cinta pasa a pedir un evento en vez de un
timeout, `ClockTimeoutListener` y el `timeout` desaparecen, y `addTStates` queda en
`tStates += n`: ocho bytes, inlineado en todos lados.

También se van las sobrecargas con etiqueta, `addTStates(int, String)` y `(int, Supplier)`: son
para el reloj de test, pero `Memory.readByte/writeByte` (líneas 318 y 332) las llaman con
`"ula readbyte"` en el camino caliente, y `Ula.addUlaStates(int, String)` **aloca un lambda por
acceso a puerto** para pasarle una descripción que nadie lee (son los 840 bytes por frame que
quedan).

**Después, opcional: el reloj adentro del núcleo.** `tStates` es un campo de otro objeto
(`this.clock.tStates`): cada suma es cargar `clock`, cargar, sumar, guardar, y el JIT no puede
tenerlo en un registro a lo largo de un `decode_N`. Como campo del núcleo generado (el paso
"reloj" del plan anterior, que quedó sin hacer) se ahorra una indirección por suma. Es el mismo
mecanismo con que entró la memoria: `held` en el `Target`, y `SpectrumZ80Clock` lee de ahí.

**Cuánto.** +10-20 %. **Custodia:** los tests de cinta (`TapeHardwareTest`, `TzxLoadingTest`,
la carga con `soundLoad`) son los que pueden romperse; y `Where`.

**Hecho (2026-09-03).** La cinta espera por un evento del `EventManager` ("Tape edge"): cada
flanco pide el siguiente medido desde el instante en que el suyo vencía, que es lo que hacía el
`overshoot` del timeout y lo que hace Fuse. `SpectrumZ80Clock` quedó en la suma que hereda de
`DefaultZ80Clock` (diez bytes) más `rebaseTStates`; el timeout, sus listeners y
`ClockTimeoutListener` en la cinta se fueron. `Ula.addUlaStates(int, String)` ya no arma un
lambda por acceso a puerto. La sobrecarga con etiqueta **se queda**: el reloj de test de
`SpeccyBaseForTests` graba las adiciones a través de ella para las comparaciones de historia de
T-states, y con la etiqueta constante e inlineada no cuesta nada; la de `Supplier` sí se fue.

Medido, alternado, mismos cores: Manic Miner 15 838 / 14 586 → 16 768 / 14 971 (**+3 a
+6 %**), JSW 8 041 → 8 852 (**+10 %**); bytes por frame en el loop 951 → 756. En el perfil de
después `addTStates` y `contend5x1` desaparecieron del tope: se inlinean, y el tiempo aparece
donde corresponde (`decodeED_22`, que es LDIR, es el 28 % del demo de Manic Miner). Lo que sigue
por encima de los 35 bytes es `read` (558 sitios "callee is too large"): el §3.

## 3. Los helpers de memoria no se inlinean

`read` (78 bytes) y `write` (199) son llamadas en 640 sitios. "La memoria inline en el núcleo"
quedó a mitad: es una llamada a un método privado en vez de una virtual, pero llamada al fin,
con su prólogo, y adentro otra a `addTStates`.

```java
private int read(int address, int fetching) {
    MemoryPage mapping = mapRead[address >>> 11];
    if (mapping.contended) clock.addTStates(ula.contention[clock.getTStates()], "ula readbyte");
    int value = mapping.page[mapping.offset + (address & 0x7FF)];
    clock.addTStates(fetching == 1 ? 4 : 3);
    return value;
}
```

**Qué cambia.**

- *Especializar por `fetching` al generar*: `fetch(address)` y `read(address)`, sin el ternario.
  El generador ya sabe cuál es cuál en cada sitio.
- *Camino caliente chico, camino frío aparte*: lo no contendido es `page[...]` y una suma; la
  rama contendida a un método propio. Lo que queda entra en 35 bytes.
- *`write`: una pregunta por página en vez de cinco.* Hoy cada escritura pregunta
  `source == ram.sourceRam && pageNum == ram.currentScreen && (offset & screenMask) < 0x1b00`
  más `writable || (source != sourceNone && settings.current.writableRoms)`: ocho cargas de
  cuatro objetos. Un `boolean screen` y un `writable` efectivo **en `MemoryPage`**, recalculados
  donde cambian (`Memory`, cuando se pagina o cambia `currentScreen` — `SpecPlus3.java:278`,
  `Spec128.java:101`), dejan `if (page.screen && offset < 0x1b00)`. Fuse hace las cinco preguntas;
  esto también es ventaja sobre Fuse.
- *Páginas de `byte[]`.* `MemoryPage.page` es `int[]`: cuatro bytes por byte, los 128 KB de RAM
  ocupan 512 KB, y las tablas de contención otros 140. Son 11 usos fuera del núcleo. Con `byte[]`
  la RAM entera entra en L2 con lugar de sobra, y el `& 0xFF` de la lectura es gratis.

**Cuánto.** Incierto: el experimento del presupuesto del JIT dice que el inlining por sí solo
no mueve mucho, así que el valor está en las cargas que se ahorran, no en la llamada. 0-15 %.
Es barato de probar porque los helpers salen de `Memory.readByte/writeByte` especializados: se
cambia el modelo, se regenera, se mide.

## 4. Puertos y teclado: alocar por cada IN

JSW lee el teclado varias veces por frame y eso es el 6 % de sus muestras (`Keyboard.read` 5,4,
`readPortInternal` 0,9). Por cada `IN`:

- `Peripherals.readPortInternal` aloca un `PeripheralData` y un `byte[1]` por handler que
  coincide, recorre **todos** los puertos registrados y a cada uno le hace tres llamadas de
  interfaz (`isReader`, `getMask`, `getValue`) antes de saber si es el suyo.
- `Ula.contendPortLate` aloca un lambda por llamada (sección 2).
- `Keyboard.read` recorre las ocho filas por lectura para armar el byte.

**Qué cambia.** Sin alocar (el `attached` vuelve por campo, no por `byte[1]`; el `PeripheralData`
es uno reusado). Un despacho por puerto: la lista de handlers que responden a un valor de puerto
se resuelve una vez y se guarda (por byte alto y bajo, o un mapa chico), invalidada cuando se
enchufa o desenchufa algo; el 99 % de los `IN` son `0xFE`, `0x7FFD` y `0xFFFD`. `Keyboard` mantiene
`byte[256] byHighByte`, recalculada cuando cambia una tecla, y `read` es una carga.

**Cuánto.** +5-8 % en juegos que consultan el teclado; nada en un demo. **Custodia:**
`AySoundPortsTest`, `UlaIdlePortValueTest`, `TapeDoesNotSilenceTheKeyboardTest`, los tests de RZX
(que pasan por el mismo `in`).

## 5. Pantalla: ocho marcas por cada byte de atributo

`Display.dirty64` es el **32 %** del tiempo propio de JSW y no depende de que la pantalla se
presente: la llama el helper `write` del núcleo por cada byte de pantalla que cambia, esté o no
activa la UI (por eso la fila "+ presentar la pantalla" de la tabla no lo muestra: se paga
siempre). Por cada atributo escrito hace dos búsquedas de tabla y ocho veces esto:

```java
private void dirtyChunk(int x, int y) {
  if (y > criticalRegionY || (y == criticalRegionY && x >= criticalRegionX)) updateCritical(x, y);
  maybeDirty[y] |= (1 << x);
}
```

JSW redibuja los 768 atributos de la habitación cada frame: 6 144 `dirtyChunk` por frame. Fuse
hace el mismo bucle de ocho (`display_dirty64` → `display_dirty_chunk` × 8): paridad, y otra
ventaja posible sobre Fuse.

**Qué cambia.** Lo sucio se marca **por celda**, no por línea: un atributo ensucia una celda de
8 × 1, y un byte de píxeles ensucia esa misma celda. Una máscara de 32 bits por fila de celdas
(`int[24]`), una OR por escritura sea atributo o píxel, y la pregunta de la región crítica una vez
por celda (las ocho líneas contra `criticalRegionY`). `copyCriticalRegionLine`, que hoy consume
`maybeDirty[y]` por línea, pasa a leer la fila de celdas y expandir a líneas ahí. Las tablas
`dirtyXtable2/dirtyYtable2` se vuelven un shift y una máscara, como en Fuse.

**Cuánto.** Hasta +20 % en juegos de atributos (JSW), +5 % en Manic Miner. **Custodia:** los
tests de video (`ZXSpectrumBeamBatchTests`, la regresión de video): la región crítica es lo que
hace que un cambio a mitad de frame se vea donde estaba el haz, y es lo único que puede romperse.

## 6. Sonido en 128K

La síntesis cuesta un 4 % en JSW (8 542 → 8 231) y nada en 48K. `Ay.synthesise` avanza de a 32
T-states (2 215 pasos por frame) recomputando los tres canales aunque nada haya cambiado, y
`BlipSynth.offsetResampled` (375 bytes) no se inlinea en `update`. Lo que valdría: avanzar de
flanco en flanco (el próximo tick de tono o ruido, o el próximo cambio de registro) en vez de
paso fijo, y partir `offsetResampled` en un camino corto para el delta que entra en un impulso.
Es el ítem de menos valor por esfuerzo; queda para después de los anteriores.

## 7. Lo que el loop hace por instrucción

`Z80.doOpcodes` es el 1,4-1,9 %:

```java
while (zxClock.getTStates() < eventManager.eventNextEvent) {
  while (emulatorPaused) Thread.onSpinWait();
  bridgeCommand.invoke(0, null);
  try { step(); } catch (Exception e) { e.printStackTrace(); }
}
```

y `step()` pregunta `beforeFetch.armed() || afterInstruction.armed()`. Fuse pregunta una cosa por
instrucción, `tstates < event_next_event`. Pausa, bridge y trampas pueden preguntarse **una vez
por tajada de eventos** (al entrar a `doOpcodes`), y quien quiera cortar antes pone
`eventNextEvent` en ahora: es el mecanismo que ya existe. +1-2 %, gratis.

## 8. Cómo medir, y el orden

**El arnés.** `Where` (hoy en el scratchpad de la sesión) corre el loop de la app sobre un
snapshot o una grabación, con la síntesis y la presentación encendidas de a una, sin acompasar:
la velocidad en un millón por ciento y el sonido siempre en `true` con `SilentSoundDevice`,
porque con el sonido en `false` el `Timer` acompasa con `sleep`. Va a `machine/app/src/test`, al
lado de `RzxCoreMeasurement`, que es el mismo tipo de cosa.

**El instrumento miente si se lo deja.** En esta máquina dos corridas iguales difieren un 12 %
(13 309 y 11 672 fps, misma configuración, cinco minutos de diferencia). Lo que vale: dos cores
(`taskset -c 2,3`, nunca uno solo, el JIT y el GC le roban el core al hilo medido), bloques de un
segundo (a 14 000 fps un bloque de 300 frames son 40 ms y el JIT no terminó), plateau de los
últimos cuatro, variantes **alternadas** en la misma sesión, nunca dos JVMs a la vez, y por bloque
imprimir cpu/pared y bytes/frame además de fps: lo de la cola de un millón de lugares lo encontró
bytes/frame, no el perfil. Cada cambio se mide en los dos juegos: Manic Miner ve la contención,
JSW ve la pantalla y los puertos.

**El orden**, por ganancia sobre riesgo:

| paso | ítem | esperado | riesgo |
|---|---|---|---|
| 1 | contención por corridas (§1) | +25-35 % en indexados | bajo: tablas |
| 2 | pantalla sucia por celda (§5) | +20 % en atributos | medio: región crítica |
| 3 | el reloj sin timeout, sin etiquetas (§2) | +10-20 % | medio: la cinta |
| 4 | puertos sin alocar, teclado por tabla (§4) | +5-8 % | bajo |
| 5 | helpers chicos, `screen` por página, `byte[]` (§3) | 0-15 % | bajo, incierto |
| 6 | una pregunta por tajada en `doOpcodes` (§7) | +1-2 % | bajo |
| 7 | el reloj adentro del núcleo (§2, opcional) | +5 % | medio |
| 8 | sonido por flancos (§6) | +3 % en 128K | medio |

Los porcentajes no se suman: cada uno se mide después del anterior, y lo que dice el perfil
después de cada paso decide el siguiente. Con los tres primeros, si dan lo que las cuentas
dicen, sobra para el 20 % que separa de ZXSpin; los demás son para no quedarse justo.

**Lo que no hay que tocar** porque ya está a la par o no pesa: el `EventManager` (0,2 %), la
mezcla del sonido (`Sound.frame`), el `updateBorder` (4 %: es el borde a mitad de frame, y en un
juego real cambia poco), el decodificador por grupos (`GROUP_SHIFT` ya está medido en 3).
