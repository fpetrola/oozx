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

package com.fpetrola.z80.minizx;

import com.fpetrola.z80.ide.rzx.InputRecordingBlock;
import com.fpetrola.z80.ide.rzx.RzxFile;
import com.fpetrola.z80.ide.rzx.RzxParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Records live play after a recording ends. No CPU is involved: the emulator loop is just a
 * fetch counter and a predicate, so it can be driven by hand and checked against the two
 * numbers an RZX frame needs — fetch length and each IN's returned byte.
 *
 * <p>These tests guard frame-edge accounting, where this breaks silently: closing a live frame
 * with one rule and replaying with another drifts by one fetch per frame, cumulatively — the
 * same bug measured on Abu Simbel (see the acknowledge comment in
 * {@link RZXPlayerIO#getInterruptionCondition()}).
 */
class LiveRecordingTest {

  /**
   * The recording the tests run against, taken from the module's own test resources.
   * <p>
   * It used to be found by walking up to whatever directory had an {@code rzx/} in it, which tied
   * the module to the layout of the repository it was living in. It is meant to be consumed from
   * elsewhere now, so it carries its own fixture.
   */
  private static Path source() {
    try {
      return Path.of(LiveRecordingTest.class.getResource("/rzx/jsw-full.rzx").toURI());
    } catch (Exception e) {
      throw new IllegalStateException("the jsw-full.rzx test resource is missing", e);
    }
  }

  /** jsw-full parsed once: 85369 frames, and every test would otherwise reparse it. */
  private static RzxFile completa;

  private static synchronized RzxFile completa() {
    if (completa == null)
      completa = new RzxParser().parseFile(source().toString());
    return completa;
  }

  /**
   * A short recording made from the first frames of {@code jsw-full} by our own writer. These
   * tests drive the emulator loop fetch by fetch, so replaying all 85369 frames — 600M
   * iterations — would make the two tests that run to the end unbearably slow. At 2400 frames
   * this fixture is about the size jsw_r2 used to be.
   */
  private static RzxFile grabacion() {
    try {
      java.nio.file.Path out = Files.createTempFile("fixture", ".rzx");
      out.toFile().deleteOnExit();
      com.fpetrola.z80.ide.rzx.RzxWriter.writeExtended(completa(), 2400, List.of(), out,
          com.fpetrola.z80.ide.rzx.RzxWriter.Mode.CONTINUE_BLOCK);
      return new RzxParser().parseFile(out.toString());
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  private static RZXPlayerIO enVivo(boolean aceptaInterrupcion) {
    RZXPlayerIO io = new RZXPlayerIO();
    io.setup(grabacion());
    io.setAcceptsInterrupt(() -> aceptaInterrupcion);
    io.setRecordLive(true);
    io.goLive();
    return io;
  }

  /**
   * Drives the emulator loop by hand: one fetch per step, {@code lecturasPorFrame} port reads
   * right after each interrupt, returning the fetch index of each frame edge.
   */
  private static int[] correr(RZXPlayerIO io, int frames, int lecturasPorFrame,
                              int pasosMaximos) {
    return correr(io, frames, lecturasPorFrame, 0, pasosMaximos);
  }

  /**
   * {@code desde} is the fetch index the machine is already at, and is not optional after a
   * replay: the frame edge is measured against that absolute counter, so restarting from zero
   * would never close a frame again.
   */
  private static int[] correr(RZXPlayerIO io, int frames, int lecturasPorFrame,
                              int desde, int pasosMaximos) {
    java.util.function.IntPredicate corte = io.getInterruptionCondition();
    int[] bordes = new int[frames];
    int encontrados = 0, porLeer = lecturasPorFrame;
    for (int i = desde; i < desde + pasosMaximos && encontrados < frames; i++) {
      if (porLeer > 0) {
        io.in(0xfefe);
        porLeer--;
      }
      if (corte.test(i)) {
        bordes[encontrados++] = i;
        porLeer = lecturasPorFrame;
      }
    }
    assertEquals(frames, encontrados, "no se cerraron los frames esperados");
    return bordes;
  }

  @Test
  void cada_frame_vivo_queda_grabado_con_sus_lecturas() {
    RZXPlayerIO io = enVivo(false);
    correr(io, 4, 3, 500_000);

    List<InputRecordingBlock.Frame> grabados = io.getRecordedFrames();
    assertEquals(4, grabados.size());
    for (InputRecordingBlock.Frame f : grabados) {
      assertEquals(3, f.inCounter, "las tres lecturas del frame");
      assertEquals(3, f.returnValues.length);
      assertTrue(f.fetchCounter > 0 && f.fetchCounter <= 0xFFFF,
          "fetchCounter fuera de rango: " + f.fetchCounter);
    }
  }

  /**
   * The recorded byte is the byte that was returned. With no key pressed the keyboard port
   * reads $FF (bits 0-4 set, 5 and 7 don't exist, 6 is EAR) — the value most games compare
   * against to mean "no key".
   */
  @Test
  void lo_grabado_es_lo_que_devolvio_el_puerto() {
    RZXPlayerIO io = enVivo(false);
    java.util.function.IntPredicate corte = io.getInterruptionCondition();
    int devuelto = -1;
    for (int i = 0; i < 500_000 && io.getRecordedFrames().isEmpty(); i++) {
      if (devuelto < 0)
        devuelto = io.in(0xfefe) & 0xff;
      corte.test(i);
    }
    assertEquals(0xff, devuelto, "sin teclas apretadas el puerto del teclado da $FF");
    assertEquals(devuelto, io.getRecordedFrames().get(0).returnValues[0] & 0xff);
  }

  /**
   * The acknowledge fetch: the next frame's budget starts after the fetch that accepts the
   * interrupt, so two consecutive edges are {@code fetchCounter + 1} apart when the CPU
   * accepts and {@code fetchCounter} apart when it doesn't. Recording with one rule and
   * replaying with the other drifts by one fetch per frame.
   */
  @Test
  void el_borde_del_frame_cuenta_el_acknowledge() {
    for (boolean acepta : new boolean[]{false, true}) {
      RZXPlayerIO io = enVivo(acepta);
      int[] bordes = correr(io, 4, 1, 500_000);
      List<InputRecordingBlock.Frame> grabados = io.getRecordedFrames();

      for (int n = 1; n < bordes.length; n++)
        assertEquals(grabados.get(n).fetchCounter + (acepta ? 1 : 0), bordes[n] - bordes[n - 1],
            "separacion entre bordes con acknowledge=" + acepta);
    }
  }

  /**
   * The whole case: replay to the last frame, keep playing, and save. The output file has the
   * recorded frames followed by the played ones, from the same snapshot, so replaying it
   * retraces everything and then continues.
   */
  @Test
  void guardar_deja_los_frames_viejos_y_despues_los_nuevos() throws Exception {
    RzxFile original = grabacion();
    List<InputRecordingBlock.Frame> grabados = original.getInputRecordingBlock().frames;
    int viejos = grabados.size();

    RZXPlayerIO io = new RZXPlayerIO();
    io.setup(original);
    io.setAcceptsInterrupt(() -> false);
    io.setRecordLive(true);
    io.setContinueLiveAtEnd(true);

    int fetch = reproducir(io, grabados, viejos);
    assertTrue(io.isLive(), "al agotarse los frames tiene que pasar a vivo");
    assertEquals(viejos, io.getLiveStartFrame(), "el vivo arranca donde termino la grabacion");
    assertEquals(0, io.getRecordedFrames().size(), "reproducir no graba nada");

    correr(io, 5, 2, fetch, 500_000);

    File out = File.createTempFile("rzx-vivo", ".rzx");
    out.deleteOnExit();
    io.saveExtendedTo(out.toPath());

    List<InputRecordingBlock.Frame> back =
        new RzxParser().parseFile(out.getPath()).getInputRecordingBlock().frames;
    assertEquals(viejos + 5, back.size());
    for (int n = 0; n < 5; n++) {
      InputRecordingBlock.Frame esperado = io.getRecordedFrames().get(n);
      InputRecordingBlock.Frame leido = back.get(viejos + n);
      assertEquals(esperado.fetchCounter, leido.fetchCounter, "fetchCounter del frame vivo " + n);
      assertArrayEquals(esperado.returnValues, leido.returnValues, "lecturas del frame vivo " + n);
    }
  }

  /**
   * A live frame lasts as long as the frames at the cut did, not as long as the startup ones.
   *
   * <p>An RZX frame is measured in fetches, but a real player also tracks T-states and flags a
   * frame that overruns one (Fuse: "RZX frame is longer than 79000 tstates"). How many fetches
   * fit in a real frame depends on what the game is running: the title screen and actual
   * gameplay have different instruction mixes. Averaging the first 2000 (title) frames and
   * applying that to gameplay gave +12% on jsw-full (7302 vs. the 6527 the game ran there),
   * ~78200 T-states — over Fuse's limit.
   *
   * <p>The median of the frames next to the cut is the principled measure, not a patch: the
   * recording was made on a real machine at 50 Hz, so a typical frame there already is a
   * 69888-T-state frame expressed in fetches.
   */
  @Test
  void el_frame_vivo_dura_lo_que_duraban_los_frames_del_corte() {
    RzxFile original = grabacion();
    List<InputRecordingBlock.Frame> viejos = original.getInputRecordingBlock().frames;

    RZXPlayerIO io = new RZXPlayerIO();
    io.setup(original);
    io.setAcceptsInterrupt(() -> false);
    io.setRecordLive(true);

    int corte = 500;
    int fetch = reproducir(io, viejos, corte);
    io.goLive();
    correr(io, 2, 1, fetch, 4_000_000);

    // The first is the cut frame, mid-run: it finishes with its own budget.
    assertEquals(viejos.get(corte).fetchCounter, io.getRecordedFrames().get(0).fetchCounter,
        "el frame del corte dura lo que decia la grabacion");

    // The median only governs from the second frame on, which is a whole live frame.
    int[] vecinos = viejos.subList(corte - RZXPlayerIO.LIVE_WINDOW, corte).stream()
        .mapToInt(f -> f.fetchCounter).sorted().toArray();
    int mediana = vecinos[vecinos.length / 2];
    assertEquals(mediana, io.getRecordedFrames().get(1).fetchCounter,
        "el frame vivo tiene que durar la mediana de los " + RZXPlayerIO.LIVE_WINDOW
            + " frames anteriores al corte");

    long sumaArranque = 0;
    for (int n = 0; n < 2000 && n < viejos.size(); n++)
      sumaArranque += viejos.get(n).fetchCounter;
    int promedioArranque = (int) (sumaArranque / Math.min(2000, viejos.size()));
    assertNotEquals(promedioArranque, io.getRecordedFrames().get(1).fetchCounter,
        "y NO el promedio de los primeros 2.000, que es lo que hacia antes");
  }

  /**
   * The cut frame is recorded whole, not just its tail.
   *
   * <p>The cut lands mid-frame, in a frame that already served some of its reads from the
   * recording. If the live side only records what happened after the cut but writes it as the
   * whole frame, the file is invalid: on replay the game asks for the full read count and gets
   * fewer. Real players reject this outright:
   *
   * <pre>
   * ZXSpin: 0 INs expected / 6 INs executed in Frame 4876
   * Fuse:   more INs during frame 4875 than stored in RZX file (0)
   * </pre>
   *
   * <p>Measured on jsw-full: cutting at frame 4875 (7 reads and 8099 fetches in the original)
   * used to write {@code ins=0 fetch=6621} — both wrong, missing the 7 reads and also
   * overwriting the duration of a frame that was mid-run.
   */
  @Test
  void el_frame_del_corte_se_graba_entero() {
    RzxFile original = grabacion();
    List<InputRecordingBlock.Frame> viejos = original.getInputRecordingBlock().frames;
    final int CORTE = 250;                 // en jsw-full: fetch=8340, ins=556
    InputRecordingBlock.Frame elDelCorte = viejos.get(CORTE);
    assertTrue(elDelCorte.inCounter >= 4, "el frame del corte tiene que tener lecturas que perder");

    RZXPlayerIO io = new RZXPlayerIO();
    io.setup(original);
    io.setAcceptsInterrupt(() -> false);
    io.setRecordLive(true);

    int fetch = reproducir(io, viejos, CORTE);
    assertEquals(CORTE, io.getCurrentFrameIndex());

    // The cut frame starts and consumes 3 of its recorded reads; only then is F1 pressed.
    java.util.function.IntPredicate corte = io.getInterruptionCondition();
    for (int k = 0; k < 3; k++)
      io.in(0xfefe);
    io.goLive();
    assertEquals(CORTE, io.getLiveStartFrame());

    // The rest of the frame's reads now come from the live keyboard.
    int leidas = 3;
    for (int i = fetch; io.getRecordedFrames().isEmpty(); i++) {
      if (leidas < elDelCorte.inCounter) { io.in(0xfefe); leidas++; }
      corte.test(i);
    }

    InputRecordingBlock.Frame grabado = io.getRecordedFrames().get(0);
    assertEquals(elDelCorte.inCounter, grabado.inCounter,
        "el frame del corte tiene que llevar TODAS sus lecturas, las de antes del corte tambien");
    for (int k = 0; k < 3; k++)
      assertEquals(elDelCorte.returnValues[k], grabado.returnValues[k],
          "la lectura " + k + " es la que sirvio la GRABACION, antes del corte");
    assertEquals(elDelCorte.fetchCounter, grabado.fetchCounter,
        "y dura lo que decia la grabacion: el frame en curso termina con SU presupuesto, no con el vivo");
  }

  /**
   * Cutting midway (F1) must not drag in the whole recording: live play continues from the cut
   * frame, not the end, so appending the original's remaining 2379 frames would produce a file
   * that replays a stretch the live session never saw. Only replayed frames, then played ones,
   * are saved.
   */
  @Test
  void cortar_en_el_medio_guarda_solo_los_frames_reproducidos() throws Exception {
    RzxFile original = grabacion();
    List<InputRecordingBlock.Frame> viejos = original.getInputRecordingBlock().frames;
    assertTrue(viejos.size() > 20, "la grabacion de prueba tiene que dar para cortarla");

    RZXPlayerIO io = new RZXPlayerIO();
    io.setup(original);
    io.setAcceptsInterrupt(() -> false);
    io.setRecordLive(true);

    int fetch = reproducir(io, viejos, 10);
    assertEquals(10, io.getCurrentFrameIndex(), "diez frames reproducidos");
    assertEquals(0, io.getRecordedFrames().size(), "reproducir no graba nada");

    io.goLive();
    assertEquals(10, io.getLiveStartFrame(), "el vivo arranca en el frame 10");
    correr(io, 3, 2, fetch, 500_000);

    File out = File.createTempFile("rzx-cortado", ".rzx");
    out.deleteOnExit();
    io.saveExtendedTo(out.toPath());

    List<InputRecordingBlock.Frame> back =
        new RzxParser().parseFile(out.getPath()).getInputRecordingBlock().frames;
    assertEquals(13, back.size(), "los 10 reproducidos + los 3 jugados, y NADA del resto");
    assertSameFrames(viejos.subList(0, 10), back.subList(0, 10));
  }

  /**
   * The mode that appends a new block copies the original as-is, so it cannot trim it: asking
   * it to must fail rather than write a file that lies about its contents.
   */
  @Test
  void el_modo_bloque_nuevo_no_puede_guardar_un_corte_en_el_medio() throws Exception {
    RzxFile original = grabacion();
    RZXPlayerIO io = new RZXPlayerIO();
    io.setup(original);
    io.setAcceptsInterrupt(() -> false);
    io.setRecordLive(true);
    io.goLive();  // cuts at frame 0, with 2379 frames still ahead
    correr(io, 2, 1, 500_000);

    File out = File.createTempFile("rzx-corte-bloque-nuevo", ".rzx");
    out.deleteOnExit();
    assertThrows(IllegalArgumentException.class,
        () -> io.saveExtendedTo(out.toPath(), com.fpetrola.z80.ide.rzx.RzxWriter.Mode.NEW_BLOCK));
  }

  /**
   * Actually replays {@code cuantos} frames: consumes each frame's recorded reads and advances
   * fetches until the predicate closes it. The emulator loop without the CPU.
   */
  private static int reproducir(RZXPlayerIO io,
                                List<InputRecordingBlock.Frame> grabados, int cuantos) {
    java.util.function.IntPredicate corte = io.getInterruptionCondition();
    int i = 0;
    for (int n = 0; n < cuantos; n++) {
      for (int k = 0; k < grabados.get(n).inCounter; k++)
        io.in(0xfefe);
      while (!corte.test(i))
        i++;
      i++;
    }
    return i;
  }

  private static void assertSameFrames(List<InputRecordingBlock.Frame> esperados,
                                       List<InputRecordingBlock.Frame> leidos) {
    assertEquals(esperados.size(), leidos.size());
    for (int n = 0; n < esperados.size(); n++) {
      assertEquals(esperados.get(n).fetchCounter, leidos.get(n).fetchCounter, "fetchCounter " + n);
      assertArrayEquals(esperados.get(n).returnValues, leidos.get(n).returnValues, "lecturas " + n);
    }
  }

  /**
   * The live stretch goes before the recording, not after.
   *
   * <p>Appending doesn't cover every gap: if the game ends on a screen with no way back (JSW
   * goes to the toilet and stays there), the missing play has to come first instead.
   *
   * <p>It must still be one continuous run from one snapshot. The format allows interleaved
   * snapshots (Fuse uses them for rollback, and {@code rzxtool -f} extracts them), but they
   * don't work for this chain: a machine state appearing from nowhere is memory changed by no
   * instruction, and the chain's provenance techniques would fabricate an origin for it — the
   * same problem as pokes.
   *
   * <p>Measured: 300 idle live frames on the title screen followed by jsw-full's 85369 replay
   * with zero drift. The title loop is frame-locked and scans the keyboard at a fixed rate (556
   * reads per frame), so the recorded frames line up on their own.
   */
  @Test
  void el_tramo_vivo_va_antes_y_la_grabacion_entera_despues() throws Exception {
    RzxFile original = grabacion();
    List<InputRecordingBlock.Frame> viejos = original.getInputRecordingBlock().frames;

    RZXPlayerIO io = new RZXPlayerIO();
    io.setup(original);
    io.setAcceptsInterrupt(() -> false);
    io.setRecordLive(true);
    io.goLive();                       // from frame zero: -Dplay=true
    correr(io, 5, 2, 500_000);

    File out = File.createTempFile("rzx-antepuesto", ".rzx");
    out.deleteOnExit();
    io.savePrependedTo(out.toPath());

    List<InputRecordingBlock.Frame> back =
        new RzxParser().parseFile(out.getPath()).getInputRecordingBlock().frames;
    assertEquals(5 + viejos.size(), back.size(), "los vivos ADELANTE y la grabacion entera atras");
    assertSameFrames(io.getRecordedFrames(), back.subList(0, 5));
    assertSameFrames(viejos, back.subList(5, back.size()));
  }

  /**
   * The splice point isn't always frame zero: it splices by code zone.
   *
   * <p>Recorded INs only replay the same game if the same routine that made them consumes
   * them. jsw-full starts on the code-entry screen — 591 frames looping at {@code $87b2}
   * (34738, this repo's known loader) — and after a game over the game returns to the title
   * screen instead, which is later. Prepending against frame 0 would feed code-entry input to
   * a game already past that screen, and it desyncs.
   *
   * <p>So the prefix splices against the frame where the recording is in the same zone the
   * live play left, discarding earlier frames.
   */
  @Test
  void el_prefijo_se_puede_pegar_contra_un_frame_posterior() throws Exception {
    RzxFile original = grabacion();
    List<InputRecordingBlock.Frame> viejos = original.getInputRecordingBlock().frames;
    final int DESDE = 591;

    RZXPlayerIO io = new RZXPlayerIO();
    io.setup(original);
    io.setAcceptsInterrupt(() -> false);
    io.setRecordLive(true);
    io.goLive();
    correr(io, 4, 2, 500_000);

    File out = File.createTempFile("rzx-desde", ".rzx");
    out.deleteOnExit();
    io.savePrependedTo(out.toPath(), DESDE);

    List<InputRecordingBlock.Frame> back =
        new RzxParser().parseFile(out.getPath()).getInputRecordingBlock().frames;
    assertEquals(4 + viejos.size() - DESDE, back.size(),
        "los vivos adelante y la grabacion DESDE el " + DESDE + ", sin lo anterior");
    assertSameFrames(io.getRecordedFrames(), back.subList(0, 4));
    assertSameFrames(viejos.subList(DESDE, viejos.size()), back.subList(4, back.size()));
  }

  /**
   * Sandwich: live play goes in the middle, and the recording continues after.
   *
   * <p>This is the general case; appending is resuming at the last frame, and prepending is
   * cutting at zero. It's needed because covering a gap isn't always possible at the end (JSW
   * ends in the toilet and doesn't return) nor at the start: cutting at frame zero forces
   * entering the codes by hand, jsw-full's first 591 frames. Cutting after that screen lets the
   * recording replay the codes instead.
   *
   * <p>The cut and resume points are deliberately independent: cut wherever one wants to play,
   * resume where the recording re-enters the same code zone the live play left. This class
   * doesn't know that frame; it's found by the edge's PC and confirmed by the gate.
   */
  @Test
  void el_tramo_vivo_va_en_el_medio_y_la_grabacion_sigue_despues() throws Exception {
    RzxFile original = grabacion();
    List<InputRecordingBlock.Frame> viejos = original.getInputRecordingBlock().frames;
    final int CORTE = 300, REANUDA = 450;

    RZXPlayerIO io = new RZXPlayerIO();
    io.setup(original);
    io.setAcceptsInterrupt(() -> false);
    io.setRecordLive(true);

    int fetch = reproducir(io, viejos, CORTE);
    io.goLive();
    correr(io, 6, 2, fetch, 2_000_000);

    File out = File.createTempFile("rzx-sandwich", ".rzx");
    out.deleteOnExit();
    io.saveSplicedTo(out.toPath(), REANUDA);

    List<InputRecordingBlock.Frame> back =
        new RzxParser().parseFile(out.getPath()).getInputRecordingBlock().frames;
    assertEquals(CORTE + 6 + (viejos.size() - REANUDA), back.size(),
        "los reproducidos hasta el corte, lo jugado, y la grabacion desde el " + REANUDA);
    assertSameFrames(viejos.subList(0, CORTE), back.subList(0, CORTE));
    assertSameFrames(io.getRecordedFrames(), back.subList(CORTE, CORTE + 6));
    assertSameFrames(viejos.subList(REANUDA, viejos.size()), back.subList(CORTE + 6, back.size()));
  }

  /**
   * Prepending only works if live play started at frame zero. If it was cut midway, the played
   * stretch continues that point's state, not the snapshot's, so putting it first would produce
   * a file that starts from a state that stretch never saw.
   */
  @Test
  void anteponer_exige_haber_arrancado_en_el_frame_cero() throws Exception {
    RzxFile original = grabacion();
    RZXPlayerIO io = new RZXPlayerIO();
    io.setup(original);
    io.setAcceptsInterrupt(() -> false);
    io.setRecordLive(true);

    int fetch = reproducir(io, original.getInputRecordingBlock().frames, 10);
    io.goLive();
    correr(io, 2, 1, fetch, 500_000);

    File out = File.createTempFile("rzx-mal-antepuesto", ".rzx");
    out.deleteOnExit();
    assertThrows(IllegalStateException.class, () -> io.savePrependedTo(out.toPath()));
  }

  /**
   * Without the flag, the end still throws: three runners use {@code "rzx finished"} as their
   * end-of-run signal, and continuing live would leave them running forever.
   */
  @Test
  void el_final_de_la_grabacion_sigue_tirando_excepcion_por_defecto() {
    RZXPlayerIO io = new RZXPlayerIO();
    io.setup(grabacion());
    assertFalse(io.isContinueLiveAtEnd(), "por defecto NO se continua en vivo");
  }

  @Test
  void con_el_flag_el_final_de_la_grabacion_entra_en_vivo() {
    RZXPlayerIO io = new RZXPlayerIO();
    io.setup(grabacion());
    io.setContinueLiveAtEnd(true);
    io.setRecordLive(true);
    io.setAcceptsInterrupt(() -> false);

    java.util.function.IntPredicate corte = io.getInterruptionCondition();
    int total = io.getRecordedFrames().size();
    for (int i = 0; i < 200_000_000 && !io.isLive(); i++) {
      io.in(0xfefe);
      corte.test(i);
    }
    assertTrue(io.isLive(), "al agotarse los frames tiene que pasar a vivo, no tirar");
    assertEquals(0, total);
  }
}
