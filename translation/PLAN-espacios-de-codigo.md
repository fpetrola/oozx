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
- **Pendiente de P3:** `unknownCodeVariant` sigue lanzando excepción ante un contenido no grabado; el respaldo
  con el emulador ahí (como `goOnFrom`) daría robustez jugando con teclas al azar.

## 5. Riesgos

- Una clave con espacio que pierde el espacio en una suma: por eso un solo dueño de la aritmética, y un test
  que verifica que los hechos de una rutina del espacio 3 quedan en el 3.
- El rango de una región: si queda corto, un contenido cruza el borde; si queda largo, dos recubrimientos
  independientes se vuelven uno. Medir en Batman y Emlyn (plantillas 9AF3, 9BBF) antes de fijar la regla.
- Un contenido que la grabación ejecutó a medias se explora solo desde sus entradas grabadas, como hoy.
- Más métodos (uno por rutina y espacio): sin problema para cojen; Fernflower mejora porque desaparecen los
  métodos de despachos mutantes.
