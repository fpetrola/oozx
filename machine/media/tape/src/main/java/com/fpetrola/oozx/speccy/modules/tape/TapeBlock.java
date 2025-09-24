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

package com.fpetrola.oozx.speccy.modules.tape;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * One block of a tape image, as the cassette browser lists it.
 * <p>
 * The blocks are read from the file rather than from {@link com.fpetrola.oozx.speccy.modules.tape.Tape}:
 * its own getBlockType and getBlockInfo read a resource bundle, "utilities/Bundle", that is not in
 * this project, so calling them throws MissingResourceException. Offsets here match the ones the
 * player uses, which is what lets progress within a block be worked out from its position.
 *
 * @param index   position in the tape, counting from 0, as the player numbers them
 * @param id      TZX block id, or -1 for a block of a TAP, which has no ids
 * @param type    short name of the kind of block
 * @param details what is worth knowing about this one
 * @param start   offset of the block in the image
 * @param end     offset just past it
 */
public record TapeBlock(int index, int id, String type, String details, int start, int end) {

  public int length() {
    return end - start;
  }

  /** Whether the counted, length-prefixed strings of an archive info block fill it exactly. */
  private static boolean stringsFit(byte[] image, int at, int length) {
    if (at >= image.length) {
      return false;
    }
    int strings = image[at] & 0xFF;
    int inside = 1;
    for (int i = 0; i < strings; i++) {
      if (at + inside + 1 >= image.length || inside + 2 > length) {
        return false;
      }
      inside += 2 + (image[at + inside + 1] & 0xFF);
    }
    return inside <= length;
  }

  /** Reads a .tzx or .tap, or returns an empty list when the file cannot be understood. */
  public static List<TapeBlock> read(File file) {
    byte[] image;
    try {
      image = Files.readAllBytes(file.toPath());
    } catch (IOException e) {
      return List.of();
    }

    String name = file.getName().toLowerCase();
    if (name.endsWith(".tap")) {
      return readTap(image);
    }
    if (name.endsWith(".tzx")) {
      return readTzx(image);
    }
    return List.of();
  }

  /** What a tap's blocks are, and what a tzx calls the same thing. */
  private static final int STANDARD_DATA = 0x10;

  private static List<TapeBlock> readTap(byte[] image) {
    List<TapeBlock> blocks = new ArrayList<>();
    int offset = 0;
    while (offset + 2 <= image.length) {
      int length = word(image, offset);
      // A block that says it is longer than what is left is a file cut short or not a tap at
      // all. The deck refuses the whole file, so a listing that showed the blocks before it
      // would be offering a tape that cannot be put in.
      if (offset + 2 + length > image.length) {
        return List.of();
      }
      int end = offset + 2 + length;
      blocks.add(new TapeBlock(blocks.size(), STANDARD_DATA, "Standard data",
          describeStandard(image, offset + 2, length), offset, end));
      offset = end;
    }
    return blocks;
  }

  private static List<TapeBlock> readTzx(byte[] image) {
    if (image.length < 10 || image[0] != 'Z') {
      return List.of();
    }

    List<TapeBlock> blocks = new ArrayList<>();
    // Past the ZXTape! signature, which is not a block. The player numbers its blocks the same
    // way, so the row it reports as playing is the row the format calls by that number.
    int offset = 10;
    while (offset < image.length) {
      int id = image[offset] & 0xFF;
      int start = offset;
      String type;
      String details;

      switch (id) {
        case 0x10 -> {
          int length = word(image, offset + 3);
          type = "Standard speed data";
          details = describeStandard(image, offset + 5, length)
              + ", pause " + word(image, offset + 1) + "ms";
          offset += length + 5;
        }
        case 0x11 -> {
          int length = triple(image, offset + 16);
          type = "Turbo speed data";
          details = length + " bytes, pilot " + word(image, offset + 11) + " pulses, "
              + "zero " + word(image, offset + 7) + "T, one " + word(image, offset + 9) + "T"
              + ", pause " + word(image, offset + 14) + "ms";
          offset += length + 19;
        }
        case 0x12 -> {
          type = "Pure tone";
          details = word(image, offset + 3) + " pulses of " + word(image, offset + 1) + "T";
          offset += 5;
        }
        case 0x13 -> {
          int count = image[offset + 1] & 0xFF;
          type = "Pulse sequence";
          details = count + " pulses";
          offset += count * 2 + 2;
        }
        case 0x14 -> {
          int length = triple(image, offset + 8);
          type = "Pure data";
          details = length + " bytes, zero " + word(image, offset + 1) + "T, one "
              + word(image, offset + 3) + "T, pause " + word(image, offset + 6) + "ms";
          offset += length + 11;
        }
        case 0x15 -> {
          int length = triple(image, offset + 6);
          type = "Direct recording";
          details = length + " bytes";
          offset += length + 9;
        }
        case 0x18, 0x19 -> {
          int length = quad(image, offset + 1);
          type = id == 0x18 ? "CSW recording" : "Generalized data";
          details = length + " bytes";
          offset += length + 5;
        }
        case 0x20 -> {
          int pause = word(image, offset + 1);
          type = pause == 0 ? "Stop the tape" : "Pause";
          details = pause == 0 ? "waits for the tape to be started again" : pause + "ms";
          offset += 3;
        }
        case 0x21 -> {
          int length = image[offset + 1] & 0xFF;
          type = "Group start";
          details = text(image, offset + 2, length);
          offset += length + 2;
        }
        case 0x22 -> {
          type = "Group end";
          details = "";
          offset += 1;
        }
        case 0x23 -> {
          type = "Jump to block";
          details = "relative " + (short) word(image, offset + 1);
          offset += 3;
        }
        case 0x24 -> {
          type = "Loop start";
          details = word(image, offset + 1) + " repetitions";
          offset += 3;
        }
        case 0x25 -> {
          type = "Loop end";
          details = "";
          offset += 1;
        }
        case 0x2A -> {
          type = "Stop the tape if 48K";
          details = "";
          offset += 5;
        }
        case 0x2B -> {
          type = "Set signal level";
          details = (image[offset + 5] & 0xFF) == 0 ? "low" : "high";
          offset += 6;
        }
        case 0x30 -> {
          int length = image[offset + 1] & 0xFF;
          type = "Text description";
          details = text(image, offset + 2, length);
          offset += length + 2;
        }
        case 0x31 -> {
          int length = image[offset + 2] & 0xFF;
          type = "Message";
          details = text(image, offset + 3, length);
          offset += length + 3;
        }
        case 0x32 -> {
          int length = word(image, offset + 1);
          type = "Archive info";
          details = "";
          // Its strings have to fit what it says it is: one that declares more than it carries
          // is the file being truncated, and everything after it is at the wrong offset.
          if (!stringsFit(image, offset + 3, length)) {
            return List.of();
          }
          offset += length + 3;
        }
        case 0x33 -> {
          int count = image[offset + 1] & 0xFF;
          type = "Hardware type";
          details = count + " entries";
          offset += count * 3 + 2;
        }
        case 0x35 -> {
          int length = quad(image, offset + 17);
          type = "Custom info";
          details = text(image, offset + 1, 16).trim();
          offset += length + 21;
        }
        default -> {
          // An id nothing in the format defines: the player refuses the whole file rather than
          // guess how long the block is, and a listing that showed one block would be saying the
          // file is readable when the deck will not take it.
          return List.of();
        }
      }

      // A block whose length runs off the end of the file: what follows is not a block, and
      // reading it as one invents blocks out of whatever bytes happen to be there.
      if (offset > image.length) {
        return List.of();
      }
      blocks.add(new TapeBlock(blocks.size(), id, type, details, start, offset));
    }
    return blocks;
  }

  /** A standard block is a header or the data that follows one; the flag byte says which. */
  private static String describeStandard(byte[] image, int dataStart, int length) {
    if (length < 2 || dataStart + length > image.length) {
      return length + " bytes";
    }
    int flag = image[dataStart] & 0xFF;
    if (flag != 0x00 || length < 19) {
      return (length - 2) + " bytes of data";
    }

    String kind = switch (image[dataStart + 1] & 0xFF) {
      case 0 -> "Program";
      case 1 -> "Number array";
      case 2 -> "Character array";
      case 3 -> "Bytes";
      default -> "Header";
    };
    String name = text(image, dataStart + 2, 10).trim();
    int dataLength = word(image, dataStart + 12);
    int start = word(image, dataStart + 14);
    return kind + " \"" + name + "\", " + dataLength + " bytes"
        + (kind.equals("Bytes") ? " at " + start : "");
  }

  /** Tape text is plain ASCII; anything else is shown as a dot rather than a stray glyph. */
  private static String text(byte[] image, int offset, int length) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < length && offset + i < image.length; i++) {
      int c = image[offset + i] & 0xFF;
      result.append(c >= 32 && c < 127 ? (char) c : '.');
    }
    return result.toString();
  }

  private static int word(byte[] image, int offset) {
    return offset + 1 < image.length ? (image[offset] & 0xFF) | ((image[offset + 1] & 0xFF) << 8) : 0;
  }

  private static int triple(byte[] image, int offset) {
    return word(image, offset) | ((offset + 2 < image.length ? image[offset + 2] & 0xFF : 0) << 16);
  }

  private static int quad(byte[] image, int offset) {
    return triple(image, offset) | ((offset + 3 < image.length ? image[offset + 3] & 0xFF : 0) << 24);
  }
}
