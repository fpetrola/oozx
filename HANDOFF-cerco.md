# Traspaso: cerco sobre oozx (desde la sesión que trabajaba en el repo de cerco, 2026-11-10)

Leé esto entero antes de hacer nada. Después borrá este archivo (no se commitea).

## Qué es esto

- **cerco** (`/home/fernando/detodo/coding/cerco`, rama `main`, versión 0.4.0): el plugin Maven de Fernando que
  convierte reglas de diseño OO en chequeos deterministas que frenan a un agente mientras edita. La primera corrida
  congela todo como línea de base (`.cerco/baseline`); después sólo lo nuevo es rojo y la base sólo se achica.
  Excepciones y configuración las firma un humano (`approve`, terminal interactiva). Documentación completa:
  `README.md` y `doc/CERCO.md` (en español) de ese repo. Leé `doc/CERCO.md` secciones 2, 3 y 3.10.
- **Este worktree** (`/home/fernando/detodo/spectrum/versions/oozx-cerco`, rama `cerco`, salida de `traduccion` en
  022f0eec9) es una prueba de cerco sobre oozx. La rama `traduccion` de Fernando no se toca. No se pushea nada.

## La idea que guía todo (de Fernando)

Los hallazgos de cerco son síntomas de modelo faltante. Arreglarlos para cumplir la letra de la regla no alcanza: cada
arreglo tiene que terminar en una **autopsia** (qué faltaba en el modelo, qué contexto se leyó, dónde más aparece la
misma causa, qué principio, qué regla nueva lo atraparía), y de esas autopsias salen reglas nuevas para cerco. Con el
tiempo, sumar chequeos tiene que hacer que el camino más fácil para el agente sea un buen modelo. Todo lo que obliga
tiene que ser determinista: que el agente no pueda esquivarlo.

## Estado

- cerco 0.3.0 trajo las autopsias (paquete `com.fpetrola.cerco.autopsy`): una corrida que saca entradas de la base
  anota la deuda en `.cerco/autopsies/<id>.owed` (protegido) y escribe `autopsies/pending/<id>.md`; rojo hasta que
  `FormCheck` lo da por completo; entonces lo archiva solo en `autopsies/`. Goal `autopsies` lista las reglas
  propuestas. 0.4.0 trajo `flag-driven-from-outside`, la primera regla nacida de una autopsia
  (`autopsies/2026-11-10-no-instanceof.md` de este worktree). 70 tests de cerco, incluido el autochequeo
  (`SelfCheckTest`: el código de cerco cumple sus propias reglas; tu código nuevo en cerco también tiene que cumplirlas).
- oozx en este worktree: 1.497 hallazgos al empezar, hoy unos 1.340 (+20 congelados de la regla nueva), cerco verde,
  unas -3.080 líneas netas. Commits por módulo en la rama `cerco` (`git log 022f0eec9..HEAD`). Corrigió además un bug
  real que encontró la autopsia: la sombra del EI no se emulaba (commit b7ffb57d2, test TheInterruptAfterEiTest).
- Los hooks de cerco NO están instalados en este worktree todavía (ver "Primer paso").

## Lo que hay que implementar ahora (decidido con Fernando)

Que la regla que propone una autopsia se implemente sí o sí, y que la implemente **la sesión del proyecto, metiendo
mano directo en el repo de cerco**. Cuatro piezas, en cerco:

1. **Ruta del código de cerco embebida en el jar**: al compilar, un recurso filtrado por Maven (por ejemplo
   `cerco-source.properties` con `${project.basedir}`) para que el plugin sepa dónde está su código sin configuración.
2. **Deuda "regla propuesta que no existe"**: cuando se archiva una autopsia con `rule: <id>` y el cerco que corre no
   tiene una regla con ese id, queda una deuda en el proyecto (rojo para el agente, como una autopsia pendiente) que
   dice "implementá <id> en <ruta de cerco>" con la autopsia como especificación. Se cierra sola cuando el plugin que
   corre tiene una regla con ese id (lo sabe por `Rules.ids()`), o cuando un humano la descarta con firma (mismo
   mecanismo que las excepciones / `approve`). El flujo del agente: escribir la regla y su test en cerco, pasar los
   tests de cerco (autochequeo incluido), subir la versión, `mvn install` de cerco, subir la versión en el pom de este
   worktree; la regla llega y congela sus hallazgos como cualquier regla nueva. Trabajar en una rama de cerco, no en
   `main`, por si hay otras sesiones (zx-lift también usa cerco).
3. **Hallazgos resueltos sin que su archivo cambie = rojo humano**: el agente vigilado ahora puede escribir el código
   de cerco, así que podría aflojar una regla en vez de arreglar su código. Guardar un hash de contenido por archivo
   junto a la base; si una entrada sale de la base y su archivo tiene el mismo hash, la regla cambió, no el código:
   rojo que sólo un humano libera.
4. **Las reglas y los defaults no pueden desaparecer**: entre la versión anterior y la nueva ningún id de regla puede
   faltar ni puede cambiar `Settings.fingerprint()` de los defaults (guardar ambos junto a la base y compararlos).

Pendientes anotados en `doc/CERCO.md` §11 (no hacer ahora salvo que Fernando lo pida): exigir arreglos de la base al
tocar un archivo con hallazgos congelados (contra el incentivo de no arreglar deuda vieja que crean las autopsias);
falsos positivos conocidos: `no-cloned-methods` con getters triviales de una línea, `no-instanceof`/`no-cast` en
`equals(Object)`, `no-public-mutable-field` en clases que extienden `com.sun.jna.Structure`.

## Primer paso sugerido

Instalar los hooks de cerco en este worktree para que esta sesión quede bajo el cerco de verdad (es parte de la
prueba): `mvn -o com.fpetrola:cerco-maven-plugin:0.4.0:install-hooks`. Escribe `.claude/settings.json` (guard antes de
cada Bash, quick después de cada edición, audit antes de terminar, deny de `.cerco/**`). Puede requerir reiniciar la
sesión para que los hooks tomen efecto. Consultá con Fernando antes de commitear ese archivo.

## Cómo trabajar acá (importa)

- Máquina con 12 núcleos y sólo 6 GB de RAM: **una corrida de Maven a la vez**, nada de builds o agentes en paralelo.
- `mvn -o verify`, **nunca `install`** en oozx (pisaría los jars de oozx en `~/.m2` que usa el árbol de Fernando).
  Por lo mismo **nunca `-rf`**: el reactor tiene que recompilar lo de arriba. (`install` en el repo de cerco sí.)
- `GameBytecodeCreationTests` del translator se cuelga desde antes de todo esto (recorrido cuadrático en
  `RoutineManager.jumpCycleComponents`). Saltealo con `-Dsurefire.excludesFile=<archivo con
  **/GameBytecodeCreationTests.java>`; **no** con `-Dtest=!X`, que rompe la `SlowSuite` del módulo app.
  Build completo así: unos 3,5 minutos.
- Antes de commitear la base: `mvn -o com.fpetrola:cerco-maven-plugin:audit` tiene que decir GREEN.
- Commits en español, descriptivos (estilo del repo), uno por módulo, con estas dos líneas al final:
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` y la línea `Claude-Session:` de tu sesión.
- El `CLAUDE.md` de oozx manda: que el código se achique, sin comentarios innecesarios, un solo dueño por concepto,
  nunca una segunda implementación paralela, y que cada cambio diga cuántas líneas netas y qué reutilizó.
- No satisfacer una regla por la letra (inventar jerarquías o flags para esconder un hallazgo). Cuando una regla da un
  falso positivo, anotarlo como mejora de cerco en vez de deformar el código.
- Fernando escribe en español rioplatense; respondé igual.
