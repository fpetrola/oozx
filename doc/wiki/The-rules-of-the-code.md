# The rules of the code

Five rules, and one case where they were measured. They are short because they are meant to be held, not
admired.

## The rules

1. **One concept, one owner.** Everything that depends on a tape's format lives in the tape, and nowhere
   else: there is no `switch` on `tap`, `tzx` or `csw` anywhere outside it. The same goes for a machine's
   model, for a peripheral, for every other concept the emulator has.
2. **New code goes where its concept lives**, not next to whoever happens to call it.
3. **If something that exists cannot be used because it is broken, I fix it or I ask.** I never write a
   second one beside it.
4. **The code should shrink as it improves.** Before adding, I look for what already does something like
   it, or for the idea that covers the old case and the new one.
5. **A name says what a thing does.** A comment is for when a name cannot.

And one that is about the model rather than the code: **the model does not adapt to its tools.** The fast
core is generated from the model because the model has the shape a generator wants. If the generator ever
needed the model bent, the generator would be wrong.

## The case: nine processors, 258 lines

Spec256 asks for something this emulator was not laid out for: nine Z80s running the same program in step,
eight of them over memories that hold colours instead of code.

It landed as a module of 1,689 lines that the rest of the code does not know exists. Outside it, 258 lines
changed, across 23 files, none of them by more than 33. Not one of those files has a special case for
Spec256 in it. What changed were six seams, and each of them was something the model had been missing
anyway: a core that may wrap its memory, a snapshot that can say where it came from, a processor that can
be copied.

One line was removed.

That is what the rules are for: they are what made it cheap when it came. → [256 colours](256-colours)

## The same idea, on the screen

The desk follows the rule the code does. A window is the thing, not a description of the thing: a machine's
window is the machine, a peripheral's window is its front, and where a window sits is what it is plugged
into. There is one owner for "is the mouse plugged in", and it is where the mouse's window is.

## How they are held

- **By the modules.** They can only depend one way, so the compiler refuses a class that reaches for what it
  should not know. The windows know nothing of the emulator. The emulator knows nothing of the peripherals.
- **By the build.** A check runs on every change: a file may not grow past its ceiling, and nothing new may
  be asked of a class from outside until I have signed for it.
- **By the tests**, which do not know which core they are running on.
  → [How right, and how we know](How-right-and-how-we-know)

I sign them: Fernando Damian Petrola.

That is the end of this tour. If you want to write something for it, [start with a peripheral](Build-a-peripheral).
