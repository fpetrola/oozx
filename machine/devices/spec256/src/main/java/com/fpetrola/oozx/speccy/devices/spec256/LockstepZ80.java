/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


package com.fpetrola.oozx.speccy.devices.spec256;

import com.fpetrola.z80.cpu.OOZ80;

/**
 * Nine processors where a machine has one: the machine's own, and eight that follow it a step
 * ahead through the same instructions over their own memories.
 * <p>
 * A follower is given the machine's program counter before every instruction, so it always
 * decodes what the machine is about to run and can never wander off; what it keeps is what it
 * moved, and on eight planes that is the colour of a pixel. It counts nobody's time, answers no
 * port, and is invisible to everything that asks this processor about itself: the state handed
 * out here is the machine's.
 * <p>
 * The eight go first and the machine last.
 */
public final class LockstepZ80 extends OOZ80 {
  private final OOZ80[] followers;
  private final Alignment alignment;
  private boolean following;

  public LockstepZ80(OOZ80 one, OOZ80[] followers, Alignment alignment) {
    super(one);
    this.followers = followers;
    this.alignment = alignment;
  }

  @Override
  public void execute() {
    if (!following) startFollowing();
    takenByAll(OOZ80::execute);
    super.execute();
  }

  @Override
  public void interruption() {
    takenByAll(OOZ80::interruption);
    super.interruption();
  }

  @Override
  public void nmi() {
    takenByAll(OOZ80::nmi);
    super.nmi();
  }

  @Override
  public void reset() {
    super.reset();
    for (OOZ80 follower : followers) follower.reset();
    following = false;
  }

  /**
   * Followers start out as copies of the processor they follow, and do it here rather than when
   * they were built: a machine moved onto this processor is given the registers it had afterwards.
   */
  private void startFollowing() {
    following = true;
    for (OOZ80 follower : followers) follower.getState().takeFrom(state);
  }

  private void takenByAll(java.util.function.Consumer<OOZ80> what) {
    for (OOZ80 follower : followers) {
      alignment.from(state, follower.getState());
      what.accept(follower);
    }
  }
}
