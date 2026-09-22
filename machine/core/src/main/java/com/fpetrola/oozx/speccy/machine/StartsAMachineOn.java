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
package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.plugins.Plugin;

import java.io.File;

/**
 * What a file does to a machine that is being built for it: a tape is put in and loaded, a
 * snapshot is a machine that already ran and is put back as it was.
 * <p>
 * A way in, because which files can start a machine is what this build happens to have: the
 * deck is a jar, and a build without it is a Spectrum that cannot be given a tape rather than
 * one that crashes when it is given one.
 */
@Plugin("media")
public interface StartsAMachineOn {

  /** Whether a machine can be started on that file by this. */
  boolean handles(File file);

  /**
   * Which machine the file wants, or null if it has no opinion. A tape that says it is for a
   * 128 is the tape asking; what is done about it is the desk's, since a machine it has no ROM
   * for is a question for whoever is watching.
   */
  default String machineFor(File file) {
    return null;
  }

  /**
   * Puts the file in and gets it going, on a machine that is already up.
   *
   * @return what has to be stepped alongside the machine's own loop until it is done - typing
   *         LOAD "" is that - or null when there is nothing to step
   */
  Going start(Speccy machine, File file);

  /** Something that goes on happening after the file is in: keys typed, a loader watched. */
  interface Going {
    boolean done();

    void step();

    /** What went wrong, or null while it has not. */
    default String wrong() {
      return null;
    }
  }
}
