/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
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
 * The eight go first and the machine last, and the planes are told when its turn begins and ends,
 * because where it writes is where what they held back has to land.
 */
public final class LockstepZ80 extends OOZ80 {
  private final OOZ80[] followers;
  private final Alignment alignment;
  private final Planes planes;
  private boolean following;

  public LockstepZ80(OOZ80 one, OOZ80[] followers, Alignment alignment, Planes planes) {
    super(one);
    this.followers = followers;
    this.alignment = alignment;
    this.planes = planes;
  }

  @Override
  public void execute() {
    if (!following) startFollowing();
    takenByAll(OOZ80::execute);
    planes.machineIsWriting();
    super.execute();
    planes.machineHasWritten();
  }

  @Override
  public void interruption() {
    takenByAll(OOZ80::interruption);
    planes.machineIsWriting();
    super.interruption();
    planes.machineHasWritten();
  }

  @Override
  public void nmi() {
    takenByAll(OOZ80::nmi);
    planes.machineIsWriting();
    super.nmi();
    planes.machineHasWritten();
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
