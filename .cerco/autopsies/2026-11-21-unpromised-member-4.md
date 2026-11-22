# Autopsy 2026-11-21-unpromised-member-4 (unpromised-member)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `unpromised-member` that went away (written by cerco):

- `machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/z80/Processors.java|Processors.stopTellingAboutReads/1`
- `machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/z80/Processors.java|Processors.whenRead/1`

## Cause

Processors already told listeners about every port read, through the IO it wraps for whichever core runs, but that
was offered as two loose public methods with no type saying it is a promise: the traffic through the ports was a
concept without a class. When a second listener, of writes with the instruction and the stack, was needed, the same
pair would have been added loose again. PortTraffic names the concept, and Processors is the one that keeps it.
model: concept-without-a-class

## Context

- machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/z80/Processors.java:112 the IO wrapper every core gets, where both reads and writes pass and are told
- machine/generated/src/test/java/com/fpetrola/oozx/generated/AWriteSaysWhoMadeItTest.java:67 a caller that only needs the traffic, not the processors, and asks it through PortTraffic on both cores

## Same cause elsewhere

- machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/z80/Processors.java:69 onChanged, the third listener Processors keeps, still a loose public method promised by no type; looked by reading the class's public members

## Principle

Information hiding and cohesion: what others may ask of an object is declared by a type named for the concept, so
callers depend on the port traffic and not on everything else Processors does.

## Candidate rule

covered-by: unpromised-member
The rule marked the new write registration as promised by no interface and asked to move the need into one the class
implements; asking what that interface is about showed the reads were the same concept, already offered loose, so the
two pairs went into one type named for the traffic through the ports.
