# Contexto del proyecto

Emulador de ZX Spectrum en Java (OOZ80) con un pipeline de análisis que recupera
estructura del juego observando la ejecución: taint de procedencia, árbol de
llamadas, identidad temporal de instancias. El módulo `minizx3d` levanta sprites
2D a representaciones 3D. El objetivo de fondo es la conversión Z80→Java guiada
por semántica recuperada, no por traducción plana de opcodes.

## Cómo trabajar acá

**Diseño primero, implementación después.** Discutir el enfoque y las decisiones
abiertas antes de escribir código. No empezar a implementar porque el diseño
"parece claro"; los puntos difíciles de este proyecto son de política, no de
codificación.

**Honestidad intelectual sobre acuerdo.** Señalar problemas, casos que no cierran
y supuestos frágiles. Un mecanismo que se cae en un caso borde vale menos que
saber de antemano cuál es ese caso.

**Determinista antes que probabilístico.** Donde haya una señal derivable del
sustrato (dataflow, árbol de ejecución), preferirla a una heurística sobre la
imagen. Las heurísticas visuales ya fallaron acá varias veces y está documentado
por qué en los javadocs.

**Medir, no impresionar.** El éxito de un mecanismo es un número contra un
oráculo, no una captura de pantalla que se ve bien. `CallTreeProbe` es el modelo:
mide si el árbol separa objetos ANTES de cambiar nada en el renderer.

## Documentos de diseño

- `doc/DETECCION-SPRITES-3D.md` — cómo se detectan y atribuyen los sprites.
  Referenciado desde los javadocs de `OriginTaint` y `TaintDiscover`.
- `doc/BASE-SEMANTICA.md` — el mecanismo general del que los sprites son un caso
  particular: planos de observación, parametricidad, variantes, esquema de la base
  semántica, plan de verificación. **Estado: diseño, sin implementar.**

## Núcleo sensible: no tocar sin discutir

`TaintReplay` y `OriginTaint` son el corazón del sistema y cada decisión rara que
tienen está ahí por una razón medida, explicada en los javadocs. En particular:

- La **política de poda** en `union()` (conservar el lado shallow) y su inversión
  en discovery (`-Dtaint.depth=512`).
- La exclusión de los reads con rol **ADDR** de la propagación de valor. Es
  deliberada: mezclarlos contamina el catálogo con índices.
- El tratamiento de **EXX / EX** como swap de taint en vez de unión.
- El **shadow stack seguido por SP**, no por conteo de CALL/RET.
- Qué se suprime y qué no en el write listener (**PUSH/POP no se suprimen**).

Antes de modificar cualquiera de estos: leer el javadoc, entender qué medición lo
justificó, y discutir el cambio.

## Estado y próximo paso

Lo implementado y sólido es el **plano de valor** (value-taint) con calidad de
producción, más el árbol de ejecución y el pipeline 3D.

Lo que falta y es prerequisito de todo lo demás es el **plano de dirección**
(dir-taint): ver `doc/BASE-SEMANTICA.md` §2, que detalla los tres cambios de
política necesarios y por qué el 90% de la infraestructura ya está.

El orden acordado (§9 de ese documento) empieza por el **arnés de verificación**
(`jsw-oracle.json` + runner), no por el plano dir: sin medidor no se sabe si la
primera versión funciona.

## Verificación contra JSW

JSW es el primer objetivo porque está desensamblado y documentado byte a byte por
la comunidad, lo que da ground truth real: tablas de entidades, strides, offsets de
campos, direcciones de rutinas, contador de vidas. El oráculo hay que llenarlo
desde esa documentación — no desde memoria ni desde suposiciones sobre direcciones.

## Ejecución

Los pases de análisis son `main()` autocontenidos configurables por `-D`:
`TaintDiscover <rzx> <db> [maxFrames]` reemplaza `sprites_found`; `TaintReplay`
tiene modo headless de validación. Los knobs de discovery se resuelven con
precedencia: `-D` explícito > config por juego > `games.json` por juego >
`games.json` global.
