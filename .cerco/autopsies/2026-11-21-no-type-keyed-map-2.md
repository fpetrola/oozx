# Autopsy 2026-11-21-no-type-keyed-map-2 (no-type-keyed-map)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-type-keyed-map` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.ZXSpectrumDesktopApp(java.util.function.BiFunction, Function)|java.util.Map<Class<?>, String>`

## Cause

The same finding as 2026-11-21-no-type-keyed-map, owed again because the sealed baseline was restored to the last
commit after an audit run on one module had emptied it. Where each kind of offered thing goes in the menus was decided
by the desk in a table from role class to menu, while the roles said nothing about where they go. Each role now says
its own place, as an annotation plugdesk reads, and the desk only names which roles it shows.
model: knowledge-without-one-owner

## Context

- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Equipment.java:30 the role now says its own menu, the fact the table held for it
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Explains.java:24 Explains extends DeskEquipment and still goes elsewhere, so the order of the roles list decides as the table's order did
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:534 the desk now passes the roles only; plugdesk's Offered reads each one's place

## Same cause elsewhere

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:611 machines are placed by the desk naming "Emulator/Machines", since a machine model is not a plugin role the annotation can sit on yet; looked with grep for maps keyed by Class in this tree

## Principle

Single owner of a concept: the place a kind of thing has in the application is a fact about that kind, so it is said
once on the kind and read from there, not kept in a lookup table by whoever happens to build the menus.

## Candidate rule

covered-by: no-type-keyed-map
The rule marked the map from role class to menu and asked for the behaviour to live on the type; asking which type
knows where it goes is what showed that each role owns its place, and the annotation each role carries, read by
Offered, is that fact moved to its owner with no table left behind.
