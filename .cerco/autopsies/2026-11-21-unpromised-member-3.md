# Autopsy 2026-11-21-unpromised-member-3 (unpromised-member)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `unpromised-member` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/OOSpectrumLauncher.java|OOSpectrumLauncher.createBareSpeccy/0`
- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|ZXSpectrumDesktopApp.rememberMachines/1`

## Cause

The same two findings as 2026-11-21-unpromised-member-2, owed again because the sealed baseline was restored to the last
commit after an audit run on one module had emptied it. The launcher's bare machine was public though only its own
factory builds one, and the desk let every emulator window tell it which machines exist, when that list belongs to the
build. Now the bare machine is private and takes the model asked for, and the desk asks the build once per set of plugins.
model: knowledge-without-one-owner

## Context

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/OOSpectrumLauncher.java:73 the only caller of createBareSpeccy, the factory the desk is given, which now passes the machine chosen
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:353 models(), the build's own answer that replaced what each window used to report through rememberMachines
- machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/machine/Machine.java:107 getMachineTypes, where the models live, the same list every machine of the build holds

## Same cause elsewhere

- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/DefaultsCore.java:63 the settings build their own never-started machine to ask the same question; looked with grep for Speccy.create in the desk and the kit

## Principle

Information hiding and a single owner: a member is public only when something outside needs it, and a fact such as
which machines a build has is asked of its owner instead of being reported to the desk by whoever saw it last.

## Candidate rule

covered-by: unpromised-member
The rule marked both members as public with no type promising them, and asking who outside calls them is what led
to the missing owner: nobody outside builds a bare machine, and the windows were reporting a fact the build itself
answers. Making one private and letting the build answer the other is what the rule's question led to.
