/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.z80.ide.rzx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

/**
 * Writes an extended recording: header, creator and snapshot block copied byte-for-byte from
 * the original ({@link RzxFile#getPrefix()}), followed by a single generated 0x80 input block
 * holding the old frames plus the new ones.
 *
 * <p>{@link Mode#CONTINUE_BLOCK} (the default) merges everything into one block because not
 * every player chains multiple input blocks; {@link Mode#NEW_BLOCK} leaves the original bytes
 * untouched and appends a separate block instead, which is more faithful but less portable.
 * Both modes assume the 0x80 blocks are the last thing in the file, as real recorders write
 * them; a later block of another type survives under {@code NEW_BLOCK} but is lost when merged.
 *
 * <p>Frames are always written literally, never with the repeat-frame marker: {@link
 * #writeExtended} rejects a frame whose {@code inCounter} would be read back as "repeat the
 * previous frame" rather than emit a file that cannot be parsed again.
 */
public class RzxWriter {
  public enum Mode {
    /** One 0x80 block with old and new frames merged; readable by any player. */
    CONTINUE_BLOCK,
    /** Original file untouched, with a new 0x80 block appended; requires block chaining. */
    NEW_BLOCK
  }

  /** The parser treats {@code inCounter >= 10000} as a repeated-frame marker (spec uses 0xFFFF). */
  public static final int MAX_IN_COUNTER = 10000;

  private RzxWriter() {
  }

  /**
   * @param original parsed recording, source of the prefix and base frames
   * @param extra    frames recorded afterwards (may be empty, making this a plain copy)
   * @param out      file to write
   */
  public static void writeExtended(RzxFile original, List<InputRecordingBlock.Frame> extra, Path out)
      throws IOException {
    writeExtended(original, extra, out, Mode.CONTINUE_BLOCK);
  }

  /**
   * @param original parsed recording, source of the prefix and base frames
   * @param extra    frames recorded afterwards (may be empty, making this a plain copy)
   * @param out      file to write
   * @param mode     merge into one block, or append a new one leaving the original untouched
   */
  public static void writeExtended(RzxFile original, List<InputRecordingBlock.Frame> extra, Path out,
                                   Mode mode) throws IOException {
    writeExtended(original, -1, extra, out, mode);
  }

  /**
   * @param baseFrames how many original frames to keep, or -1 for all. Needed because live
   *                   play does not always resume at the recording's end: if it was cut
   *                   midway, the frames after the cut were never actually run, so keeping
   *                   them would produce a file that replays a stretch the live session
   *                   never saw.
   */
  public static void writeExtended(RzxFile original, int baseFrames,
                                   List<InputRecordingBlock.Frame> extra, Path out,
                                   Mode mode) throws IOException {
    byte[] prefix = original.getPrefix();
    if (prefix == null)
      throw new IllegalArgumentException(
          "la grabacion original no trae el prefijo del archivo: se parseo sin bloque 0x80, o por"
              + " un camino que no es RzxParser.parseFile");

    InputRecordingBlock block = original.getInputRecordingBlock();
    boolean nuevo = mode == Mode.NEW_BLOCK;
    List<InputRecordingBlock.Frame> frames;
    int base = baseFrames < 0 ? block.frames.size() : Math.min(baseFrames, block.frames.size());
    if (nuevo) {
      if (base != block.frames.size())
        throw new IllegalArgumentException("NEW_BLOCK copia el bloque original tal cual y no lo"
            + " puede recortar: se pidieron " + base + " de " + block.frames.size()
            + " frames. Para guardar un corte en el medio va CONTINUE_BLOCK.");
      frames = new ArrayList<>(extra);
    } else {
      frames = new ArrayList<>(block.frames.subList(0, base));
      frames.addAll(extra);
    }

    byte[] payload = deflate(encodeFrames(frames));

    try (OutputStream os = Files.newOutputStream(out)) {
      os.write(prefix);
      if (nuevo)
        os.write(original.getTail());
      os.write(0x80);
      // block length includes its own id and length fields: 1 + 4 + 4 (frame count)
      // + 1 (reserved) + 4 (tStates) + 4 (flags) = 18 bytes of header
      writeInt(os, 18 + payload.length);
      writeInt(os, frames.size());
      os.write(0);
      writeInt(os, nuevo ? 0 : (int) block.tStates);
      writeInt(os, 0x02); // protected=0, so the written recording can be extended again
      os.write(payload);
    }
  }

  /**
   * Prepends a live-played prefix to the whole original recording, against the same snapshot.
   * This is needed when a recording cannot be extended at the end (e.g. the game is stuck on
   * a screen with no way back), so the missing play has to come first instead; it must still
   * be one continuous run from a single snapshot, since a machine state appearing without any
   * instruction writing it cannot be attributed to code, same issue as pokes. The recorded
   * frames only stay valid after a live prefix if the splice lands in an equivalent machine
   * state; measured on jsw-full with 300 idle live frames on the title screen: no divergence.
   *
   * @param prefix live-played frames, starting at recording frame zero
   */
  public static void writePrepended(RzxFile original, List<InputRecordingBlock.Frame> prefix, Path out)
      throws IOException {
    writePrepended(original, prefix, 0, out);
  }

  /**
   * @param fromFrame recording frame to resume from; earlier frames are discarded. The splice
   *                  point is a code zone, not a frame number: recorded INs only replay the
   *                  same game if the same routine consumes them. In jsw-full, frames 0-590 are
   *                  the code-entry screen ({@code $87b2} = 34738); after a game over the game
   *                  returns to the title screen instead, which is later, so splicing at frame
   *                  0 would feed code-entry input to a game already past that screen.
   */
  public static void writePrepended(RzxFile original, List<InputRecordingBlock.Frame> prefix,
                                    int fromFrame, Path out) throws IOException {
    List<InputRecordingBlock.Frame> viejos = original.getInputRecordingBlock().frames;
    if (fromFrame < 0 || fromFrame > viejos.size())
      throw new IllegalArgumentException("fromFrame=" + fromFrame + " fuera de la grabacion, que"
          + " tiene " + viejos.size() + " frames");
    List<InputRecordingBlock.Frame> frames = new ArrayList<>(prefix);
    frames.addAll(viejos.subList(fromFrame, viejos.size()));
    writeExtended(original, 0, frames, out, Mode.CONTINUE_BLOCK);
  }

  /**
   * General case: {@code frames[0..cut)} + live play + {@code frames[resume..]}, one continuous
   * run against the same snapshot; appending and prepending are special cases of this. {@code
   * cut} and {@code resume} are deliberately independent: play stops wherever the user chose,
   * but resumes only where the recording re-enters the same code zone the live play left,
   * since recorded INs only replay correctly if the routine that made them consumes them. This
   * class does not pick that frame; it is found by the frame-boundary PC and confirmed by the
   * sync gate, which desyncs immediately on a wrong splice.
   *
   * @param cut     recording frame played up to (exclusive)
   * @param live    the live-played frames
   * @param resume  recording frame to resume from; frames between {@code cut} and this are dropped
   */
  public static void writeSpliced(RzxFile original, int cut, List<InputRecordingBlock.Frame> live,
                                  int resume, Path out) throws IOException {
    List<InputRecordingBlock.Frame> viejos = original.getInputRecordingBlock().frames;
    if (cut < 0 || cut > viejos.size())
      throw new IllegalArgumentException("cut=" + cut + " fuera de la grabacion de "
          + viejos.size() + " frames");
    if (resume < 0 || resume > viejos.size())
      throw new IllegalArgumentException("resume=" + resume + " fuera de la grabacion de "
          + viejos.size() + " frames");
    List<InputRecordingBlock.Frame> frames = new ArrayList<>(live);
    frames.addAll(viejos.subList(resume, viejos.size()));
    writeExtended(original, cut, frames, out, Mode.CONTINUE_BLOCK);
  }

  private static byte[] encodeFrames(List<InputRecordingBlock.Frame> frames) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    for (int i = 0; i < frames.size(); i++) {
      InputRecordingBlock.Frame f = frames.get(i);
      byte[] ins = f.returnValues == null ? new byte[0] : f.returnValues;
      if (ins.length != f.inCounter)
        throw new IllegalArgumentException("frame " + i + ": inCounter=" + f.inCounter
            + " pero trae " + ins.length + " valores");
      if (ins.length >= MAX_IN_COUNTER)
        throw new IllegalArgumentException("frame " + i + ": " + ins.length
            + " lecturas de puerto, y desde " + MAX_IN_COUNTER
            + " el lector lo toma por repeticion del frame anterior");
      if (f.fetchCounter < 0 || f.fetchCounter > 0xFFFF)
        throw new IllegalArgumentException("frame " + i + ": fetchCounter=" + f.fetchCounter
            + " no entra en 16 bits");
      writeShort(bos, f.fetchCounter);
      writeShort(bos, ins.length);
      bos.write(ins, 0, ins.length);
    }
    return bos.toByteArray();
  }

  private static byte[] deflate(byte[] data) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (DeflaterOutputStream dos = new DeflaterOutputStream(bos)) {
      dos.write(data);
    }
    return bos.toByteArray();
  }

  private static void writeInt(OutputStream os, int v) throws IOException {
    os.write(v & 0xff);
    os.write((v >>> 8) & 0xff);
    os.write((v >>> 16) & 0xff);
    os.write((v >>> 24) & 0xff);
  }

  private static void writeShort(ByteArrayOutputStream os, int v) {
    os.write(v & 0xff);
    os.write((v >>> 8) & 0xff);
  }
}
