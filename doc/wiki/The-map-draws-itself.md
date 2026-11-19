# The map draws itself

Walk through a game and its map appears, room by room, where each room lies. Nothing has to be known about
the game: the map is worked out from what the game's own memory does while you play.

This is a prototype. It has been tried on Jet Set Willy, Dynamite Dan and Dizzy, and it is for flip-screen
games, the ones made of rooms.

> **Clip** · *The map draws itself.* Willy walks right and a second square appears beside the first, then a
> third; the room in play is outlined. (`the-map`, to film)

## Using it

`Emulator → Equipment → Map` opens the map. Clip it onto the machine that is running the game, and play.

Each room you reach is drawn on its own square, with the one you are in outlined. Leave by the right and the
next room is drawn to the right; climb out through the top and it goes above. *Start again* begins the map
anew.

## How it finds out

Three questions, each answered by watching:

- **Which room is this?** The game keeps its own idea of where it is, somewhere in its memory. The map looks
  for the bytes that change between rooms and never inside one. A room has been arrived at when the screen is
  redrawn, dozens of cells changing at once, and then stands still. What changes while standing in a room,
  timers, guardians, the score, is struck off. What is left is the room's identity.
- **Where is the player?** As the byte the keys move: it goes up while one key is held and down while another
  is, and hardly ever moves with no key held. Which key is which comes out with the byte.
- **Which way did they leave?** A game puts the player at the far door of the next room: leave on the right
  and you appear on the left. Each way between two rooms is voted for a side every time it is taken, so a
  way told wrong once is outvoted by the times after.

## Drawn twice, while you play

What the map learns arrives late: by the time it has found the player, you may be several rooms in. So it
plays the game again. A second machine, a twin, replays everything you have done since the map was
opened, this time knowing which bytes are the room and which the player, and draws the map a second pass
would give. It catches up with you and then follows a frame behind, while you go on playing.

A twin is only started again once you have played for as long as the last one took to catch up, so no more
time goes into replaying than into playing. It is one of the things a machine that runs far above its own
speed is for. → [How fast, and how we know](How-fast-and-how-we-know)

## What it does not do yet

- Games that scroll, or that are not made of rooms.
- Two rooms the game's memory cannot tell apart become one.
- I have not checked what other emulators did here before: EmuZWin announced a "Map Builder" in 2006. Until
  that is looked at, this page says what this one does and nothing about who was first.

The maps people drew by hand are in the catalogue as well: the *Maps* tile of a game's card.
→ [Your files](Your-files)

Next: [256 colours](256-colours).
