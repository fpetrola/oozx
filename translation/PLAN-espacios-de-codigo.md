# Plan: espacios de código con nombre (bancos y recubrimientos), para traducir Batman entero

Documento aparte de `PLAN-metodos-java-sin-trampolin.md` porque es otra línea de trabajo: aquél saca las máquinas
de estados; este resuelve que una misma dirección tenga más de un código (otro banco paginado en C000-FFFF, o un
recubrimiento que el juego copia sobre su propio código). Batman es la medida.

## 1. Diagnóstico (2026-10-30, corregido con el volcado de memoria del cuadro 415)

- **No son bancos.** En el cuadro 415 el banco 0 tiene en CE3C `60 CD 38 D8 CD 9D CE` (`LD H,B` / `CALL D838` /
  `CALL CE9D`); la grabación había visto antes `FD 7E 00 CB 7F 28 06` (`LD A,(IY+0)` / `BIT 7,A` / `JR Z,+6`, que
  saltea los dos CALL que siguen). Son 7 bytes que el juego reescribe según su estado; de CE43 en adelante los dos
  contenidos coinciden. En otros momentos hay un tercer contenido en toda la zona (`FD 85`, `FD 67`, `EE 18`,
  `D3 FE`). Todo corre con el banco 0 paginado: el código de otros bancos nunca entra en `codeBytes`.
- **La imagen de la ejecución simbólica mezcla contenidos.** `Footprint.install` escribe cada instrucción grabada
  byte a byte sobre una sola imagen; cuando dos contenidos alinean distinto las instrucciones, el último que escribe
  gana por byte. En CE40 quedó `CD 28 06`: el `CD` del `CALL CE9D` del segundo contenido con el `28 06` del `JR Z`
  del primero, un `CALL 0628` que ningún contenido tiene. La ejecución simbólica explora eso, nunca `CALL CE9D`, y
  CE9D no tiene rutina ni llamadores.
- **Lo que sigue de eso.** `modifiedCode()` marca como mutantes los bytes donde los contenidos difieren, así que la
  zona CDA0-CF8E y DC00-DC30 corre por `executeMutantCode`: cada instrucción es un despacho con comparaciones,
  `goOnFrom` y `unwinding`. Consecuencias medidas hoy: el `CALL CE9D` del emulador falla con `untranslated`; cada
  vuelta del bucle que pasa por `goOnFrom` → `runTranslated` anida un marco Java (el respaldo solo sabe llamar,
  nunca saltar); Fernflower se queda sin 3 GB en `$DC4F` y falla al parsear `$CDBD`, `$D5F1` y `$D9E3`.
- **Bancos.** Batman solo tiene código de otro banco en el banco 3, C000-C8F7 (el reproductor de música). Hoy se
  traduce copiándolo a un hueco libre debajo de C000 (`translateBankedCode`, `freeAreaFor`, `relocateJump`) y los
  métodos llevan la dirección falsa de la copia; mantener esa reubicación cuesta 28 casos especiales
  (`inOwnBank`, `inBankOf`, `originalAddress`, `pagedCopy`, `bankedOnly`, `forgetCode`, `withCodeVariants`…) y
  depende de que haya RAM libre.

## 2. Idea: cada código vive en un espacio con nombre

Propuesta de Fernando: cuando hay bancos, un prefijo en los nombres de los métodos que haga de espacio de nombres.
Generalizada: un **espacio** es una fuente de código que comparte direcciones con otra. Espacio 0: la imagen
principal. Un espacio por banco con código en la ventana (`b3`). Un espacio por contenido de una región reescrita
(`c2`). Los hechos (rutinas, pila, versiones) se guardan por espacio y dirección; los métodos se llaman `$C0D6`,
`$b3_C0D6`, `$c2_CE3C`; la ejecución simbólica explora cada espacio sobre su propia imagen; en tiempo de ejecución
el despacho elige el espacio por `bank()` o por el hash del contenido. La reubicación desaparece, y con ella las
direcciones falsas en los fuentes y el límite de la RAM libre.

## 3. Diseño

1. **Clave.** `int clave = espacio << 16 | dirección`. Un solo dueño de la aritmética (`Space` en `routines`):
   `address(clave)`, `space(clave)`, `next(clave, n)` que conserva el espacio, `formatAddress` → `b3:C0D6`. Los
   sitios con `& 0xffff` en routines (29), se (3) y bytecode (4) pasan por ahí; los del runtime (42, aritmética
   del Z80) quedan.
2. **Ejecución simbólica por espacio.** `exploreSpace(espacio, rango, bytes, entradas)`: instala los bytes del
   espacio en su rango (la ventana para un banco, la región para un contenido), fija el espacio actual y recorre
   las entradas. Lo que el listener registra dentro del rango lleva el espacio; fuera del rango es espacio 0
   (código compartido). Un salto de un espacio hacia afuera de su rango va al 0; uno del 0 hacia adentro de un
   rango es una entrada con despacho (hoy `inPagedBank` para bancos y `codeVariantsAt` para contenidos: pasa a
   ser uno solo).
3. **Runtime.** `SpectrumApplication.space(dirección)`: en la ventana, `bank()` si ese banco tiene espacio ahí;
   en una región reescrita, `codeHash` de la región contra una tabla generada (hash → espacio). `runTranslated`
   despacha por clave; `setNextAddress`/`isNextPC` llevan claves; `pc()` informa la dirección Z80. `pagedCopy` y
   `unknownCodeVariant` se funden en `space()`.
4. **Huella.** Un banco es un espacio con sus 16K y sus instrucciones (`BankedCode`, ya existe). Un
   recubrimiento: región con versiones de distinta longitud, y sus contenidos grabados en la misma pasada de la
   huella (hoy `recordBlockContents` es una segunda pasada que solo corre para plantillas declaradas por juego):
   al ejecutar una instrucción de la región con un contenido cuyo hash no se vio, se guarda la región entera. El
   rango de la región: la corrida contigua de direcciones con versiones, unida a la vecina cuando una instrucción
   de algún contenido cruza el hueco. Medir en Batman que CE3C-CE42 queda como una región.
5. **Imagen sin mezcla.** `install` deja en la imagen principal el primer contenido grabado de cada región, nunca
   bytes de dos; los otros contenidos viven en sus espacios. `modifiedCode()` no marca como mutante lo que un
   espacio cubre: mutante queda solo lo que ningún espacio ni versión explica.
6. **Generador.** Nombres con prefijo; se borran `inOwnBank`, `inBankOf`, `originalAddress`, `pagedCopy`,
   `relocateJump`, `freeAreaFor`, `forgetCode`/`forgetExecutions`, `withCodeVariants` y `bankedOnly`. Los
   fuentes dicen de dónde es cada método.

## 4. Pasos, cada uno con su test en rojo primero

- **P1. Clave con espacio.** `Space` y el reemplazo de la aritmética; espacio 0 en todo. Sin cambio de
  comportamiento: batería verde, nocturna igual.
- **P2. Contenidos en la huella.** Grabar regiones y contenidos en la pasada de la huella; `install` sin mezcla.
  Tests sobre hechos: dos contenidos grabados para el programa de abajo, imagen con el primero, CE40 nunca
  `CALL 0628`.
- **P3. Recubrimientos como espacios.** Exploración por espacio, nombres, `space()` por hash, despacho en las
  entradas de la región. Tests: el programa de abajo traduce sin respaldo (la regla del arnés
  `assertRunsLikeTheEmulator` deja de fallar); un contenido cuyo CALL llama a una rutina que solo él llama; un
  salto del espacio 0 al medio de un contenido; una instrucción de un contenido que cruza el borde de la región.
- **P4. Bancos como espacios.** `BankedCode` explorado como espacio `bN`; `space()` por `bank()`; se borra la
  reubicación. Los 14 tests de bancos de `RecordedProgramTests` siguen; cambian solo los nombres esperados.
- **P5. Batman.** Nocturna por bytecode y por fuente; medir cuadros y el tiempo de Fernflower (sin despachos
  mutantes debería bajar de 24 minutos). Después, lo que ya estaba pendiente: el ciclo D537 en cabezas anidadas.

Programa del recubrimiento (el que la regla del arnés rechaza hoy):

```
8000 CALL 8100 / LD HL,8200 / LD DE,8100 / LD BC,4 / LDIR / CALL 8100 / HALT / JR -3
8100 INC A / RET / NOP / NOP          (contenido 1)
8200 CD 00 83 C9                      (contenido 2: CALL 8300 / RET, copiado sobre 8100)
8300 INC C / RET
```

## 4b. Avance (2026-10-30, tarde)

- **P1 (a07d2f5cf).** `Space` en `routines` con `address` y `next`; los 18 pasos de dirección de código de los
  hechos y del generador pasan por ahí (cuatro reusando `addressAfter`). Sin cambio de comportamiento.
- **P2 (026ca31b6).** La huella graba los contenidos de cada región en la misma pasada (segunda reproducción
  al terminar, con las regiones ya conocidas) e `install` pone el primer contenido entero. Test sobre hechos,
  rojo antes; el arnés gana `explore()`.
- **P3, acotado (f2cf700ce).** La clave de 32 bits atraviesa el registro PC de 16 bits del núcleo y las
  acciones por dirección de la ejecución simbólica: es un cambio de días. Como para Batman la RAM libre alcanza
  (regiones de pocos bytes, el banco 3 son 2,3 KB), el paso 3 se hizo sobre la reubicación existente: toda
  región con más de un contenido grabado se traduce con `translateCodeVariants` desde una copia por contenido
  en un hueco libre (`freeAreaFor`), entrada por `codeHash` en sus entradas; la región abarca instrucciones
  enteras de todos los contenidos (una ejecutada que la cruza la extiende; las que se tocan se unen; las
  regiones se reconstruyen solo cuando cambian las versiones y conservan las uniones pedidas); cada copia
  termina con un `JP` al código que sigue a la región, que no se informa como instrucción; y los métodos llevan
  el espacio en el nombre, `$c1_8100` y `$b3_C0D6`, por `RoutineManager.spaceOf`. El arnés perdió el bloque
  explícito y Emlyn une sus plantillas antes de explorar. Dos tests que documentaban el respaldo del emulador
  (R-Type BF24, Renegade 128 con `OUT` en código parcheado) ya no lo usan. La clave con espacio queda para
  cuando a un juego no le alcance la RAM libre para las copias.
- **P3, formas (43dd213f6).** Los contenidos de una región se agrupan por forma (mismas instrucciones en los
  mismos lugares, operandos en cero) y hay una copia por forma, no por contenido: los bytes que varían dentro
  de una forma son operandos que la copia lee de memoria (quedan mutantes en la copia, como los del banco 3) y
  el despacho compara el hash con esos bytes en cero. Antes la segunda reproducción recorría todas las regiones
  por instrucción y Batman no terminaba en la hora (e02a42557: índice de región y comparación con el último
  contenido visto). Una región sin lugar para sus copias se queda como estaba y lo informa.
- **Batman medido (1030-batman7).** 63 regiones; casi todas de 2 o 3 formas (CFFC-D064 pasa de 104 contenidos
  a 2, D065-D191 de 99 a 3, DB1D-DB43 de 110 a 2). Cuatro no entran en la RAM libre bajo C000 y siguen por el
  emulador: D7BB-D996 (475 bytes, 18380 contenidos, 5230 formas: código que el juego genera), DFC3-DFF1
  (46 bytes, 43302 contenidos, 37018 formas: datos que alguna vez se ejecutaron), D2C2-D424 (30 formas) y
  E72A-E80A (11). Con eso Batman se traduce entero (RUNONE ok, antes fallaba) y el lockstep se cortó en el
  cuadro 48 dentro de la copia `$c0_EB38`: la región EB38-EB43 es la espera de cuadro (`EI; HALT; LD C,FE;
  IN A,(C); CPL; AND 1F; LD D,A; RET NZ`) y su otro contenido es otra rutina (`AND 1; ADD A,0E; LD (IY+0),A;
  LD C,A; JP D594`); el `HALT` de la copia informaba al runtime la dirección de la copia (`halt(0x6569)`) y
  el estado al fin de cuadro decía PC=6569 contra EB39.
- **P3, direcciones informadas desde una copia.** `pc()` ya pasaba por `originalAddress`; `halt`, `in`/`inC`
  (`SyncSpectrumApplication` lo usa para sincronizar) y las instrucciones de bloque pasan por el mismo
  `InstructionsBytecodeGenerator.originalAddress()`. El arnés deja que el Java llegue a su `halt()` antes de
  terminar el programa en un `HALT`, así compara también lo que el `HALT` informa. Test
  aHaltInsideACopyOfARewrittenRegionReportsItsOriginalAddress, rojo antes (PC=4008 contra 8102).
- **P3, saltos relativos que salen de una copia (1030-batman9, cuadro 239).** Con el `HALT` arreglado Batman llega
  al cuadro 239 y se corta en la copia de EBA3-EBAC: su `DJNZ EB8F` (EB8F está dentro de EB6B-EB98, otra región
  reescrita) caía a su propio desplazamiento en el área libre (5B50). Cada copia lleva ahora, después de su
  `JP` final, un `JP` por salto relativo que sale de la región, y el salto se desvía ahí (`copiesOf`, un solo
  decodificado por forma, que también sirve para pedir el área); si el trampolín queda a más de 127 bytes la
  región se queda en su lugar y lo informa. Y como una copia puede saltar dentro de una región copiada antes,
  las entradas de cada región se vuelven a mirar hasta que ninguna agrega (`exploreEntriesOf`): la entrada
  interior recibe su despacho y las copias se exploran desde ahí (`$c0_8101`). Tests
  aRelativeJumpThatLeavesACopiedRegionGoesWhereTheOriginalWent y
  aJumpFromACopyIntoARegionCopiedBeforeItEntersThatRegionsCopies, rojos antes. Pendiente: una entrada interior
  que en otra forma cae en medio de una instrucción explora basura desde ahí (la rama del despacho queda muerta).
- **P3, trampolines antes de la copia (1030-batman10).** Con trampolines solo después del `JP` final, cinco
  regiones de Batman de 206 a 354 bytes (CE7D-CF4B, D065-D191, D1A7-D290, D2C2-D424, DB70-DC4C) tienen un
  salto relativo cerca del principio que sale hacia atrás (CE7D+3 → CE61, DB70+4 → DB58) y no llegaba: se
  quedaron en su lugar y la traducción falló en el corte de `{9F73:C6FE}` (`Routine.split`, Optional vacío:
  pendiente, el corte de una rutina con regiones mutantes en su lugar). Ahora cada salto que no alcanza los
  trampolines de después usa uno de antes de la copia (`CodeVariant.prefix`; `relocated`, `originalAddress` y
  `synthetic` lo tienen en cuenta en `RoutineManager`); en Batman todos los saltos que salen (lista con
  `$S/tools/Leaving.java`) alcanzan uno de los dos lados. Test
  aRelativeJumpAtTheStartOfALongCopiedRegionReachesATrampolineBeforeTheCopy (región de 130 bytes), rojo antes.
- **P3, entradas interiores (1030-batman11).** Con los trampolines, los saltos que salen de una región entran
  en otra por el medio (EB8F, CE61, DB58, D2B2…) y cada copia de la región destino se exploraba desde ahí
  aunque en su forma esa dirección cayera en medio de una instrucción: la exploración decodificaba basura, las
  rutinas de las copias se fundieron con las del juego en rutinas de 10 KB ({44C3:AC2E}, {9F73:C6FE}) y el
  corte de una de ellas falló (`Routine.split`, Optional vacío). Cada copia sabe dónde empiezan sus
  instrucciones (`CodeVariant.instructions`, de la misma decodificación de `copiesOf`) y una entrada interior
  solo explora y despacha las copias que tienen una instrucción ahí. Test
  anEntryInsideARegionOnlyEntersTheCopiesThatHaveAnInstructionThere, rojo antes (`$c0_8101` existía).
- **P3, dónde van las copias (1030-batman12).** Con eso la rutina gigante seguía ({9F73:C6FE}, en batman9
  eran cuatro rutinas) y entre batman9 y batman12 cambiaban 877 límites de rutinas lejos de toda región. La
  causa está en `freeAreaFor`: un hueco "libre" era todo byte que no fuera código de una rutina ni byte
  protegido, y las copias (unos 13 KB en Batman, más que la pantalla) caían en huecos de la RAM del juego que
  su código lee como datos (la copia de EB38 quedó en 6568): cada tamaño distinto de copias movía los huecos
  y cambiaba los hechos (tablas de saltos leídas por la exploración). Ahora la exploración anota todo byte que
  lee (`readByTheExploration`, un `MemoryReadListener` desde la primera exploración) y las copias van primero a
  huecos que nadie leyó; si no alcanza, al hueco de antes y lo informa (`REGION … copies at … over bytes the
  exploration read`). Test copiesGoWhereTheExplorationNeverRead, rojo antes (copias en 4000, que el programa
  lee). Pendiente: el corte de `outline` falla con partes no contiguas (`Routine.split`, Optional vacío).
- **Pendiente de P3:** `unknownCodeVariant` sigue lanzando excepción ante un contenido no grabado; el respaldo
  con el emulador ahí (como `goOnFrom`) daría robustez jugando con teclas al azar. Las cuatro regiones de Batman
  sin lugar piden la clave con espacio (sin copias en RAM) o un respaldo por región, no por instrucción.

## 5. Riesgos

- Una clave con espacio que pierde el espacio en una suma: por eso un solo dueño de la aritmética, y un test
  que verifica que los hechos de una rutina del espacio 3 quedan en el 3.
- El rango de una región: si queda corto, un contenido cruza el borde; si queda largo, dos recubrimientos
  independientes se vuelven uno. Medir en Batman y Emlyn (plantillas 9AF3, 9BBF) antes de fijar la regla.
- Un contenido que la grabación ejecutó a medias se explora solo desde sus entradas grabadas, como hoy.
- Más métodos (uno por rutina y espacio): sin problema para cojen; Fernflower mejora porque desaparecen los
  métodos de despachos mutantes.
