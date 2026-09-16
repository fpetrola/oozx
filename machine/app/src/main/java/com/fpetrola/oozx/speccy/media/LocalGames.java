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
package com.fpetrola.oozx.speccy.media;

import com.fpetrola.emulation.helpers.snapshots.SnapshotFactory;
import com.fpetrola.oozx.speccy.devices.spec256.Spec256Peripheral;
import com.fpetrola.oozx.api.GameFingerprint;
import com.fpetrola.oozx.api.GameLibrary;
import com.fpetrola.oozx.api.GameSummary;
import com.fpetrola.oozx.config.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The library of this machine, wired to this emulator: the catalogue that ships with it, the
 * formats it can load, and the readers that unpack a snapshot into the RAM that is worth
 * fingerprinting. The library itself knows none of those, which is why they are joined here.
 */
public class LocalGames {
  /**
   * How sure the catalogue has to be. Measured over a real collection, a game and its own tape in
   * another format score from 0.22 upwards once a snapshot is unpacked, while unrelated games sit
   * below 0.05; the room in between is what this leaves.
   */
  public static final double CERTAINTY = 0.2;

  private static GameLibrary library;

  public static Path file() {
    return Configuration.home().toPath().resolve("library.json");
  }

  /** The one library of this machine, read from disk the first time somebody asks for it. */
  public static synchronized GameLibrary library() {
    if (library == null) {
      library = new GameLibrary(GameFingerprint.Index.shipped(), new GameLibrary.Emulator() {
        /**
         * What the emulator can open, less the recordings: an RZX is somebody playing a game, not
         * a copy of one, and it belongs to the player rather than to this shelf.
         */
        public boolean loadable(Path file) {
          return DownloadAndUnzip.loadable(file.getFileName().toString())
              && !file.getFileName().toString().toLowerCase().endsWith(".rzx");
        }

        public byte[] payload(Path file) throws IOException {
          return SnapshotFactory.payloadOf(file.toFile());
        }

        public boolean inColour(Path file) {
          return Spec256Peripheral.hasColours(file.toString());
        }
      });
      try {
        library.load(file());
      } catch (IOException unreadable) {
        System.err.println("the library could not be read: " + unreadable.getMessage());
      }
    }
    return library;
  }

  /**
   * Who a file is, as far as the catalogue can tell, or null when it cannot tell. Whatever comes
   * out of here is written down, so the next thing that asks about the same file - its pokes, its
   * details, its map - gets the same answer without paying for it again.
   */
  public static GameSummary whoIs(Path file) {
    try {
      GameLibrary.Copy copy = library().identify(file, CERTAINTY);
      library().save(file());
      return copy.game();
    } catch (IOException unreadable) {
      System.err.println("could not identify " + file + ": " + unreadable.getMessage());
      return null;
    }
  }
}
