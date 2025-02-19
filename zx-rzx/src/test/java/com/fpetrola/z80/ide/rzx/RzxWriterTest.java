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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extending a recording: the output file starts from the same snapshot as the original, with
 * the old frames followed by the new ones. The test is a round-trip, since the only judge of
 * the format on hand is our own reader: if {@link RzxParser} reads back exactly the frames
 * {@link RzxWriter} wrote, the block is well formed.
 *
 * <p>Beyond the count, each frame's bytes are checked too: a block with a miscalculated length,
 * or a compression flag that doesn't match, would still yield a frame list — garbage, but a
 * list — since the reader just runs until the stream ends.
 */
class RzxWriterTest {

  /**
   * The recording the tests run against, taken from the module's own test resources.
   * <p>
   * It used to be found by walking up to whatever directory had an {@code rzx/} in it, which tied
   * the module to the layout of the repository it was living in. It is meant to be consumed from
   * elsewhere now, so it carries its own fixture.
   */
  private static Path source() {
    try {
      return Path.of(RzxWriterTest.class.getResource("/rzx/jsw-full.rzx").toURI());
    } catch (Exception e) {
      throw new IllegalStateException("the jsw-full.rzx test resource is missing", e);
    }
  }

  private static void assertSameFrames(List<InputRecordingBlock.Frame> expected,
                                       List<InputRecordingBlock.Frame> actual) {
    assertEquals(expected.size(), actual.size(), "cantidad de frames");
    for (int i = 0; i < expected.size(); i++) {
      InputRecordingBlock.Frame e = expected.get(i), a = actual.get(i);
      assertEquals(e.fetchCounter, a.fetchCounter, "fetchCounter del frame " + i);
      assertEquals(e.inCounter, a.inCounter, "inCounter del frame " + i);
      assertArrayEquals(e.returnValues, a.returnValues, "valores IN del frame " + i);
    }
  }

  @Test
  void reescribir_sin_agregar_nada_devuelve_la_misma_grabacion() throws Exception {
    RzxFile original = new RzxParser().parseFile(source().toString());
    assertNotNull(original, "no pude leer " + source());

    File out = File.createTempFile("rzx-roundtrip", ".rzx");
    out.deleteOnExit();
    RzxWriter.writeExtended(original, List.of(), out.toPath());

    RzxFile back = new RzxParser().parseFile(out.getPath());
    assertNotNull(back, "no pude releer lo que escribi");
    assertSameFrames(original.getInputRecordingBlock().frames, back.getInputRecordingBlock().frames);
  }

  @Test
  void el_snapshot_del_extendido_es_el_del_original() throws Exception {
    RzxFile original = new RzxParser().parseFile(source().toString());

    File out = File.createTempFile("rzx-snapshot", ".rzx");
    out.deleteOnExit();
    RzxWriter.writeExtended(original, List.of(), out.toPath());

    RzxFile back = new RzxParser().parseFile(out.getPath());
    assertArrayEquals(original.getSnapshotBlock().getSnapshotData(),
        back.getSnapshotBlock().getSnapshotData(), "el estado inicial tiene que ser el mismo");
    assertEquals(original.getSnapshotBlock().getUncompressedLength(),
        back.getSnapshotBlock().getUncompressedLength());
  }

  @Test
  void los_frames_nuevos_van_a_continuacion_de_los_viejos() throws Exception {
    RzxFile original = new RzxParser().parseFile(source().toString());
    List<InputRecordingBlock.Frame> viejos = original.getInputRecordingBlock().frames;

    List<InputRecordingBlock.Frame> nuevos = new ArrayList<>();
    nuevos.add(frame(17000, new byte[]{(byte) 0xff, (byte) 0xbf}));
    nuevos.add(frame(16999, new byte[]{(byte) 0xfe}));
    nuevos.add(frame(17001, new byte[0]));

    File out = File.createTempFile("rzx-extendido", ".rzx");
    out.deleteOnExit();
    RzxWriter.writeExtended(original, nuevos, out.toPath());

    List<InputRecordingBlock.Frame> back =
        new RzxParser().parseFile(out.getPath()).getInputRecordingBlock().frames;

    assertEquals(viejos.size() + 3, back.size(), "viejos + nuevos");
    assertSameFrames(viejos, back.subList(0, viejos.size()));
    assertSameFrames(nuevos, back.subList(viejos.size(), back.size()));
  }

  /**
   * Past {@code inCounter >= 10000} the count stops being a count: on the way back in it says
   * "this frame is the one before again". A frame that truly read that many times cannot be
   * written down at all, so the writer refuses instead of producing a file nobody can read.
   */
  @Test
  void aFrameWithTooManyReadsIsRefused() throws Exception {
    RzxFile original = new RzxParser().parseFile(source().toString());
    File out = File.createTempFile("rzx-imposible", ".rzx");
    out.deleteOnExit();

    assertThrows(IllegalArgumentException.class,
        () -> RzxWriter.writeExtended(original, List.of(frame(17000, new byte[10000])), out.toPath()));
  }

  /**
   * NEW_BLOCK mode: the original file stays byte-for-byte intact, and live play goes into a
   * separate 0x80 block, as the spec allows (each input block continues the previous one) —
   * but not every player chains blocks, which is why this isn't the default mode.
   */
  @Test
  void el_modo_bloque_nuevo_deja_el_original_intacto_y_le_pega_un_bloque() throws Exception {
    RzxFile original = new RzxParser().parseFile(source().toString());
    List<InputRecordingBlock.Frame> viejos = original.getInputRecordingBlock().frames;
    List<InputRecordingBlock.Frame> nuevos = List.of(
        frame(17000, new byte[]{(byte) 0xff}),
        frame(16999, new byte[]{(byte) 0xbf, (byte) 0xfe}));

    File out = File.createTempFile("rzx-dos-bloques", ".rzx");
    out.deleteOnExit();
    RzxWriter.writeExtended(original, nuevos, out.toPath(), RzxWriter.Mode.NEW_BLOCK);

    byte[] antes = Files.readAllBytes(source());
    byte[] despues = Files.readAllBytes(out.toPath());
    assertTrue(despues.length > antes.length, "el archivo tiene que haber crecido");
    assertArrayEquals(antes, java.util.Arrays.copyOf(despues, antes.length),
        "el original tiene que quedar tal cual: el bloque nuevo va DESPUES");

    List<InputRecordingBlock.Frame> back =
        new RzxParser().parseFile(out.getPath()).getInputRecordingBlock().frames;
    assertEquals(viejos.size() + 2, back.size(), "los dos bloques se leen encadenados");
    assertSameFrames(viejos, back.subList(0, viejos.size()));
    assertSameFrames(nuevos, back.subList(viejos.size(), back.size()));
  }

  /** The default mode continues the existing block, which every player can read. */
  @Test
  void por_defecto_se_continua_el_bloque_existente() throws Exception {
    RzxFile original = new RzxParser().parseFile(source().toString());
    List<InputRecordingBlock.Frame> nuevos = List.of(frame(17000, new byte[]{(byte) 0xff}));

    File porDefecto = File.createTempFile("rzx-defecto", ".rzx");
    File explicito = File.createTempFile("rzx-explicito", ".rzx");
    porDefecto.deleteOnExit();
    explicito.deleteOnExit();

    RzxWriter.writeExtended(original, nuevos, porDefecto.toPath());
    RzxWriter.writeExtended(original, nuevos, explicito.toPath(), RzxWriter.Mode.CONTINUE_BLOCK);

    assertArrayEquals(Files.readAllBytes(porDefecto.toPath()), Files.readAllBytes(explicito.toPath()));
  }

  private static InputRecordingBlock.Frame frame(int fetches, byte[] ins) {
    InputRecordingBlock.Frame f = new InputRecordingBlock.Frame();
    f.fetchCounter = fetches;
    f.inCounter = ins.length;
    f.returnValues = ins;
    return f;
  }
}
