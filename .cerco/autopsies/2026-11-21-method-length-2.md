# Autopsy 2026-11-21-method-length-2 (method-length)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `method-length` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.ZXSpectrumDesktopApp(java.util.function.BiFunction, Function)`
- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.offers()`

## Cause

The same two findings as 2026-11-21-method-length, owed again because the sealed baseline was restored to the last
commit after an audit run on one module had emptied it. The desk's constructor and its offers() narrated every step of
building the desk and the menus inline: the saved screen defaults, the table of where each role goes, and the
television entries with their scan lines. The steps now have names, and the roles say their own places.
model: procedure-instead-of-model

## Context

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:551 screenDefaultsAsSaved, the step the constructor narrated in four lines and a comment
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:625 screens, the television kinds and scan lines offers() wrote out among the desk's own entries
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Equipment.java:30 where the place of equipment now lives, which is what emptied the constructor of its table

## Same cause elsewhere

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:595 offers() still narrates file entries, recent files and tool keys one after another; looked by reading the file

## Principle

Cohesion: a method does one thing and says which in its name, so building the desk reads as the list of what the desk
is made of and each part can be found, changed and tested by its own name.

## Candidate rule

covered-by: method-length
The rule marked both methods as too long and asked for a method per step named by what it does; doing that named the
saved screen defaults and the screens, and asking what the remaining table step was led to the roles owning their
places, so the rule's question is what found each missing name.
