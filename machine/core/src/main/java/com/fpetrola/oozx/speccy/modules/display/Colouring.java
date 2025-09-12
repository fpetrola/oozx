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


package com.fpetrola.oozx.speccy.modules.display;

/**
 * What colours an attribute byte asks for. Three bits of ink, three of paper, one of bright that
 * lifts the ink into the top eight colours, and one of flash - and while the flash is turned over,
 * a cell that has it reads with ink and paper the other way round.
 * <p>
 * Which half of the sixteen-frame turn it is on is one thing for the whole screen, which is why
 * flashing cells are in step, and it is set here rather than passed in with every cell.
 */
public final class Colouring {
  /** Whether flashing cells currently read the other way round. */
  public boolean reversed;

  public byte ink(byte attribute) {
    return flashes(attribute) && reversed ? paperBits(attribute) : inkBits(attribute);
  }

  public byte paper(byte attribute) {
    return flashes(attribute) && reversed ? inkBits(attribute) : paperBits(attribute);
  }

  public static boolean flashes(byte attribute) {
    return (attribute & 0x80) != 0;
  }

  /** The three ink bits, with bright lifting them into the top eight colours. */
  private static byte inkBits(byte attribute) {
    return (byte) ((attribute & 0x07) + ((attribute & 0x40) >> 3));
  }

  /** The three paper bits, with bright already sitting above them. */
  private static byte paperBits(byte attribute) {
    return (byte) ((attribute & (0x0f << 3)) >> 3);
  }
}
