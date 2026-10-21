# La versión 1

Qué falta para que OOZX se pueda publicar como 1.0: lo técnico, y lo que un usuario da por hecho
de un entorno así. Relevado el 21 de octubre de 2026 desde el código, la wiki, los planes de los
dos repositorios, la investigación de septiembre (`~/detodo/spectrum/investigacion`), las páginas
públicas de GitHub y una reproducción local del CI.

**Estado (21 de octubre de 2026):** nada empezado, salvo el guardado del entorno, que tiene su
plan propio (`plan-guardado-estado.md`).

## Qué quiere decir 1.0

Alguien que nunca habló con el autor baja el entorno, lo abre, carga un juego, juega con sonido y
controles, cierra, vuelve a abrir y lo encuentra exactamente como lo dejó. Y la 1.1 no le rompe
lo que guardó ni los plugins que instaló. Lo que hace único a OOZX ya está; lo que falta es casi
todo producto: cómo llega, qué promete y qué garantiza.

## Lo que hoy impide publicar

Verificado el 21 de octubre salvo donde se dice.

| # | Qué | Dónde se vio |
|---|---|---|
| 1 | No hay nada que bajar: ninguna release de la aplicación; solo compilar con JDK 21 y Maven y correr un jar de 87 MB (`app-0.0.2-alu-SNAPSHOT-all.jar`, clases Java 21). Para correr alcanza un JRE 21: el núcleo rápido viene compilado (`GeneratedCores.loaded()` es `Class.forName`); el README dice "un JDK, no un JRE", que vale solo para compilar | GitHub releases, `machine/app/pom.xml` |
| 2 | Los cuatro comandos del README probablemente fallan en un clon limpio: dependen de `crystal-plugins` 0.1.0-SNAPSHOT, que no se publica (el CI lo clona y lo compila antes), y `crystal.bundleGroupId=com.fpetrola` pide artefactos de oozx-plugins en `~/.m2`. No se probó desde cero | `.github/workflows/maven.yml`, `machine/app/pom.xml` |
| 3 | El CI está en rojo desde el 28 de septiembre, en `main` y en todo el PR #1 (`traduccion`). En local, sin pantalla, 3.794 pruebas de los módulos del emulador pasan; `GameBytecodeCreationTests.testTranslateBatmanToJava` no terminó en 46 minutos con el heap de 2 GB lleno. Los logs de GitHub piden sesión: la causa es probable, no confirmada | GitHub Actions, reproducción local |
| 4 | `main` es del 2 de octubre; `traduccion` va 214 commits adelante. Los plugins compilan contra `main`, y los periféricos de Fuse del 21 de octubre usan API que solo está en `traduccion` | git |
| 5 | Todo es `0.0.2-alu-SNAPSHOT`; la API de plugins es `0.1.0-SNAPSHOT`. Un plugin publicado que llama un método borrado revienta con NoSuchMethodError en vez de decir que necesita otra versión | poms, `plugin-metadata.json` |
| 6 | Unas 50 releases viejas de plugins (22 y 23 de septiembre) siguen en el repositorio del emulador y duplican las de oozx-plugins | GitHub releases |
| 7 | `~/.oozx/config.json` está a mitad de migración: `Settings` (329 líneas, 25 archivos) convive con las secciones. Lo que se publique en 1.0 queda congelado | `plan-configuracion.md` |
| 8 | Las sesiones se guardan como SZX y el SZX lee solo Z80R, SPCR, KEYB, AY y PLTT: el estado de los periféricos se pierde. "Cerrás, abrís y vuelven donde estaban" no se cumple con una disquetera o una Multiface enchufada | `SzxBlocks`, README |

## Lo que un usuario da por hecho

Comparado con lo que ofrecen Fuse, Spectaculator, Retro Virtual Machine y ZEsarUX, y con lo que
la investigación de septiembre registra de quejas y pedidos.

| Lo que espera | Dónde está OOZX |
|---|---|
| Doble clic en un `.tap`, arrastrar y soltar, o pasarlo como argumento | No hay arrastrar y soltar, y los argumentos se ignoran (wiki, Limits) |
| Que el juego arranque en la máquina correcta | Elige por bloque TZX, después por nombre, si no 48K. No usa el tipo de máquina de ZXDB, que ya tiene |
| Joystick emulado y mando USB | Emulado sí (Kempston, Sinclair, Cursor...). Mando físico: solo un lector de `/dev/input/js*` heredado de JSpeccy, solo Linux |
| Pantalla completa, escalado, filtros | Sí (`tools/view`, filtros de señal, xBRZ) |
| AY en estéreo (ABC/ACB) | El AY es mono; la línea de audio sí abre en estéreo |
| Guardar y volver; rebobinar | Snapshots y sesiones sí, con lo del punto 8. Rebobinar no |
| Grabar una partida | Reproducir RZX y tomar el control sí; no hay botón para grabar |
| Abrir todo lo que circula | Faltan UDI, FDI, TD0 y SAD, el bloque TZX 0x19, PZX y WAV; el efecto "snow" no está |
| Que lo que está, ande | La wiki admite que la Opus no anda en el núcleo generado, el que se usa por defecto (no reverificado) |
| Saber qué hace por la red, y poder usarlo sin red | La documentación no lo dice: ZXInfo, archive.org y TOSEC, el RZX Archive, GitHub |
| Dónde avisar de un problema | No hay canal ni botón "reportar este juego" |

## Lo que se promete y no se sostiene

- "Más rápido que los emuladores en lenguajes de bajo nivel": nunca se midió; la página de
  límites lo llama "una afirmación de diseño". Medir contra Fuse o sacar la frase.
- Las cifras cambian entre páginas: 22 o 24 periféricos, 24 modelos contra la tabla del README.
- Getting-Started dice que las ROMs, los pokes y el catálogo vienen en el árbol; ahora son
  plugins. La página de límites dice que el µSource y el µSpeech no existen.
- Se promete "RZX escrito" sin botón para grabar; la pestaña "Description" nunca se llena.

## Legal, para distribuir

- ROMs de Amstrad: se pueden distribuir sin cobrar y conservando los avisos (`README.copyright`).
- ROMs de clones (TK90X/TK95, Inves, Chrome, TS2068, Scorpion): "licencia a verificar" en
  `plan-clones-zesarux.md` y `plan-maquinas-faltantes.md`. Confirmarlas o no ofrecerlas.
- Juegos con distribución denegada: ZXDB marca 505; el entorno los baja de TOSEC vía archive.org,
  cuyos términos dicen "for scholarship and research purposes only". En algo público no se
  bajan solos.
- Pokes: NOTICE dice que los 3.683 vienen de Tipshop y "none carries a licence". Pedir permiso o
  enlazar en vez de empaquetar.
- ZXInfo pide un User-Agent propio; ZXDB es ODbL (ya citado).

## Lo que queda fuera de la 1.0

- Los juegos como programas Java: otra línea de trabajo, experimental, fuera del CI de la versión.
- Spec256 (acierta entre 36 % y 84 % de los gráficos y usa casi todo el frame).
- TTX2000S, Spectranet, SpeccyBoot, ZX-Uno y BaseConf.
- µSpeech, µSource y DOCK salen como beta hasta probarlos con ROMs y cartuchos reales.

## Lo que solo tiene OOZX, y por eso tiene que andar sin fallas

Según la investigación de septiembre, ningún emulador revisado conoce a la vez el juego (ZXDB),
los bytes que cargó y la máquina. A eso se suman varias Spectrum a la vez, el hardware que se
engancha a mano, 4.922 juegos identificados por contenido, ver un RZX y tomar el control, y la
velocidad.

## Los pasos

Cada uno se cierra con su prueba; el orden es el de dependencia.

1. **CI en verde y `main` al día.** Las traducciones de juegos enteros salen del build por
   defecto (como `oozx.slow`). `traduccion` se mergea a `main`; primero oozx, después los plugins.
   Se verifica: el CI de los dos repositorios en verde en `main`.
2. **Build desde un clon limpio.** `crystal-plugins` se publica (Maven Central, GitHub Packages o
   un repositorio en el propio GitHub) o se trae como módulo; `bundleGroupId` deja de pedir
   artefactos que todavía no existen. Se verifica: clon nuevo, `~/.m2` vacío, los comandos del
   README, y la prueba que ya define `plan-plugins.md`: "un solo jar, home vacío, sin red".
3. **Guardado del entorno** (`plan-guardado-estado.md`): cada máquina en un SZX con todos los
   bloques conocidos más los propios del entorno; el resto en el JSON del entorno; cada plugin
   aporta lo suyo sin que el entorno lo conozca. Se verifica: cerrar y abrir deja todo igual,
   máquina por máquina y ventana por ventana.
4. **Formatos y API congelados.** Config, sesiones y API de plugins con número de versión; la app
   rechaza con un mensaje el plugin que necesita otra versión; pruebas de migración desde la 1.0
   en adelante.
5. **Paquetes por sistema con Java adentro** (jpackage: `.deb` y AppImage, `.msi`, `.dmg`),
   versión 1.0.0, tag, notas de versión y changelog. Se borran las releases viejas del
   repositorio del emulador.
6. **Lo básico de uso:** abrir con doble clic, arrastrando o por argumento; elegir la máquina con
   el tipo de ZXDB; un botón para grabar RZX. Después, si entran: AY estéreo, mandos USB en los
   tres sistemas, rebobinar.
7. **Documentación:** una sola fuente para las cifras; la página de límites al día; una página de
   red y privacidad (qué contacta, cómo usarlo sin red); una página legal (ROMs, pokes, juegos).
8. **Lo legal:** ROMs de clones, pokes, y no bajar los títulos con distribución denegada.
9. **Beta con pocos usuarios** (foro de Spectrum Computing, Discord) antes de llamarla 1.0, con un
   camino para reportar un juego que no anda.
