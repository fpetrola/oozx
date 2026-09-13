Tratar siempre de achicar el codigo no de agregar lineas porque si. La idea es si hay que agregar funcionalidad buscar primero si existe algo parecido y reusarlo o encontrar abstracciones que puede cumplir con lo que hay y lo nuevo.
El codigo tiene que tender a achicarse por tener mejor implementacion cada vez.
Los comentarios en el codigo hay que tratar de evitarlos, solo tiene que estar presente cuando el nombre de la clase, metodo o variable no refleja claramente lo que hace, o en caso de que sea un algoritmo complejo que necesite explicacion.

Antes de crear una clase o archivo nuevo, decir en una linea en que clase existente iria ese codigo y por que no va ahi. Si no hay una razon fuerte, va en la clase existente.

Si estoy por escribir un comentario que explica por que duplico o evito codigo que ya existe, ese comentario es la señal de que la decision esta mal: parar, reportar el problema del codigo existente y preguntar.

Si algo existente no se puede usar porque esta roto (excepcion, dependencia faltante, mal diseñado), arreglar eso o preguntar. Nunca escribir una segunda implementacion en paralelo.

Cada concepto tiene un solo dueño. La logica que depende del formato de archivo (tap/tzx/csw) va solamente en Tape: ningun switch ni if por formato fuera de ahi. Lo mismo para el resto de los conceptos del emulador.

El codigo nuevo va donde pertenece el concepto, no en el paquete donde esta el que lo llama.

Al terminar un cambio, decir el neto de lineas y que se reuso.



Query Shape

The tool is not what costs: `jq` and `python3 -c json.load` both run outside the context and only
what they PRINT comes in. Measured on `deducido.json` (6.4 MB): same question, 43 bytes of output
either way. What costs is the shape of the question.

- **Count first, then sample.** A query that can return many rows returns the count and three
  examples. Ask for the rest only when the three are not enough to decide. Measured on this session:
  the 39 stray zones cost 3.5 KB printed in full, where the count plus three would have been ~300
  bytes and led to the same next step.
- **Aggregate in the query, not in the answer.** `... | sort | uniq -c` beats printing the rows and
  counting them by reading. If the point is "how many of each", say that to the tool.
- **Never count with `grep -c` or by eye on multi-line output.** `grep -c` counts LINES, and an
  ast-grep match spans several — that gave 1,557 matches where there were 1,492, and 3,623 grep
  lines for `ldir` of which most were `visitLdir`, `Ldir` and javadoc. To count matches:
  `ast-grep ... --json=stream | wc -l`.
- **When a number moves, diff the data, not the summary.** A test going from 112 to 111 says
  nothing; seeing that `(35211, 35244)` became `(35211, 36146)` names the bug. Keep the previous
  artefact and diff against it.

## Which Model Does What

**The session model is whatever is active — nothing here pins it.** Whoever is running this
conversation picked it with `/model`, and that one is the smartest thing available right now: it
does the reasoning, the design, and anything that has to hold this repo's chain in its head.

**Delegation is RELATIVE to it.** The ladder, most to least capable:

```
fable  >  opus  >  sonnet  >  haiku
```

Never delegate at or above the active model — that spends more to get less. Pick the LOWEST rung
that can do the task and whose answer can be checked at a glance. So the same task lands on
different models depending on where the session is:

| the task | session on fable | session on opus | session on sonnet |
|---|---|---|---|
| locate something, count across a subtree | haiku | haiku | haiku |
| summarise a long output, extract a table, apply an exact mechanical spec | sonnet | sonnet | haiku, or do it here |
| a bounded job that still needs judgement — read a diff against a known trap list | opus | sonnet | do it here |
| anything about the game, the chain, a threshold, what a range means | here | here | here |

The mechanism: the subagents in `.claude/agents/` declare a floor in their frontmatter (`haiku`,
because that is what they need), and the `model` argument at call time overrides it when a
particular question is harder than usual. Set it deliberately; leaving it out makes the subagent
inherit the session model, which is the one thing this section exists to avoid.

Two agents exist so far: `buscador` to locate, `inventario` to count across a subtree, both
read-only. The search output stays in the subagent and only the conclusion comes back, which is
where the context saving is.
