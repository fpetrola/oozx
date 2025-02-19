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
 * GRABAR LO QUE SE JUEGA DESPUES DE LA GRABACION. Sin CPU: el bucle del emulador es un
 * contador de fetches y un predicado, asi que se lo puede escribir a mano y afirmar sobre los
 * dos numeros que un frame RZX necesita -- cuantos fetches duro y que bytes devolvio cada IN.
 *
 * <p>Lo que estos tests protegen es la CONTABILIDAD DEL BORDE, que es donde esto se rompe en
 * silencio: si el frame vivo se cierra con una regla y se reproduce con otra, el desvio es de
 * un fetch por frame y se acumula. Es el mismo error que ya costo medir en Abu Simbel (ver el
 * comentario del acknowledge en {@link RZXPlayerIO#getInterruptionCondition()}).
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

  /** jsw-full parseada UNA vez: son 85.369 frames y cada test la usaria de nuevo. */
  private static RzxFile completa;

  private static synchronized RzxFile completa() {
    if (completa == null)
      completa = new RzxParser().parseFile(source().toString());
    return completa;
  }

  /**
   * UNA GRABACION CORTA, hecha con los primeros frames de {@code jsw-full} por nuestro propio
   * escritor. El arnes de estos tests recorre el bucle del emulador fetch por fetch, asi que
   * reproducir los 85.369 frames enteros -- 600 millones de vueltas -- haria eternos a los dos
   * tests que llegan hasta el final. Con 2.400 frames el fixture pesa lo que pesaba jsw_r2.
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
   * Corre el bucle del emulador a mano: un fetch por paso, {@code lecturasPorFrame} lecturas de
   * puerto justo despues de cada interrupcion, y devuelve el indice de fetch de cada borde.
   */
  private static int[] correr(RZXPlayerIO io, int frames, int lecturasPorFrame,
                              int pasosMaximos) {
    return correr(io, frames, lecturasPorFrame, 0, pasosMaximos);
  }

  /**
   * {@code desde} es el indice de fetch por el que va la maquina, y NO es opcional despues de
   * reproducir: el borde del frame se mide contra ese contador absoluto, asi que volver a
   * empezar de cero no cierra ningun frame nunca mas.
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
   * EL BYTE GRABADO ES EL BYTE QUE SE DEVOLVIO. Con ninguna tecla apretada el puerto del
   * teclado vale $FF (bits 0-4 en uno, 5 y 7 no existen, 6 es el EAR) -- que es justo el valor
   * que medio catalogo compara para decidir "no hay tecla".
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
   * EL FETCH DEL ACKNOWLEDGE. El presupuesto del frame siguiente empieza DESPUES del fetch que
   * acepta la interrupcion, asi que dos bordes consecutivos estan separados por
   * {@code fetchCounter + 1} cuando la CPU acepta y por {@code fetchCounter} cuando no. Grabar
   * con una regla y reproducir con la otra es un fetch de desvio POR FRAME.
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
   * EL CASO ENTERO: reproducir la grabacion hasta el ultimo frame, seguir jugando, y guardar.
   * El archivo que sale tiene los frames grabados y los jugados a continuacion, y arranca
   * del mismo snapshot -- o sea que reproducirlo vuelve a recorrer todo y despues sigue.
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
   * EL FRAME VIVO DURA LO QUE DURABAN LOS FRAMES DEL CORTE, no lo que duraban los del arranque.
   *
   * <p>Un frame de RZX se mide en FETCHES, pero un reproductor de verdad ademas lleva su reloj de
   * T-states, y avisa si el frame no entra en uno (Fuse: "RZX frame is longer than 79000
   * tstates"). Cuantos fetches entran en un frame real depende de QUE esta ejecutando el juego:
   * la pantalla de titulo y el juego andando tienen mezclas de instrucciones distintas. Promediar
   * los primeros 2.000 frames -- el titulo -- y aplicarselo a un tramo de juego daba +12% en
   * jsw-full (7.302 contra los 6.527 que hacia el juego ahi), o sea ~78.200 T-states: pegado al
   * umbral, y Fuse avisaba.
   *
   * <p>La MEDIANA de los frames vecinos al corte es la medida principista, no un parche: la
   * grabacion se hizo en una maquina real a 50 Hz, asi que el frame tipico de esa vecindad ES un
   * frame de 69.888 T-states expresado en fetches.
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

    // el PRIMERO es el frame del corte, que estaba a medio correr: termina con SU presupuesto
    assertEquals(viejos.get(corte).fetchCounter, io.getRecordedFrames().get(0).fetchCounter,
        "el frame del corte dura lo que decia la grabacion");

    // la mediana recien gobierna del SEGUNDO en adelante, que ya es un frame vivo entero
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
   * EL FRAME DEL CORTE SE GRABA ENTERO, NO SOLO SU COLA.
   *
   * <p>{@code F1} cae en medio de un frame que ya sirvio parte de sus lecturas DESDE LA GRABACION.
   * Si el frame vivo anota solo lo que paso despues del corte pero se escribe como si fuera el
   * frame entero, el archivo queda INVALIDO: al reproducirlo el juego pide las lecturas completas
   * y el archivo ofrece menos. No es perdida de fidelidad, es un error duro, y los reproductores de
   * verdad lo cazan:
   *
   * <pre>
   * ZXSpin: 0 INs expected / 6 INs executed in Frame 4876
   * Fuse:   more INs during frame 4875 than stored in RZX file (0)
   * </pre>
   *
   * <p>Medido sobre jsw-full: cortando en el frame 4875 —que en el original tiene 7 lecturas y
   * 8.099 fetches— se escribia {@code ins=0 fetch=6621}. Las dos cosas mal: le faltaban las 7
   * lecturas y ademas le pisaba la duracion al frame que estaba a medio correr.
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

    // el frame del corte arranca y consume 3 de sus lecturas grabadas; RECIEN AHI se aprieta F1
    java.util.function.IntPredicate corte = io.getInterruptionCondition();
    for (int k = 0; k < 3; k++)
      io.in(0xfefe);
    io.goLive();
    assertEquals(CORTE, io.getLiveStartFrame());

    // y se termina el frame: el resto de sus lecturas ya salen del teclado vivo
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
   * CORTAR EN EL MEDIO (F1) NO PUEDE ARRASTRAR LA GRABACION ENTERA. Lo vivo continua desde el
   * frame donde se corto, no desde el final: pegarle los 2379 frames del original produciria un
   * archivo que al reproducirse recorre frames que la sesion viva nunca vio. Se guardan los
   * reproducidos y despues los jugados.
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
   * El modo que pega un bloque nuevo copia el original TAL CUAL, asi que no puede recortarlo:
   * pedirle eso tiene que fallar y no escribir un archivo que miente.
   */
  @Test
  void el_modo_bloque_nuevo_no_puede_guardar_un_corte_en_el_medio() throws Exception {
    RzxFile original = grabacion();
    RZXPlayerIO io = new RZXPlayerIO();
    io.setup(original);
    io.setAcceptsInterrupt(() -> false);
    io.setRecordLive(true);
    io.goLive();  // corta en el frame 0, con 2379 frames por delante
    correr(io, 2, 1, 500_000);

    File out = File.createTempFile("rzx-corte-bloque-nuevo", ".rzx");
    out.deleteOnExit();
    assertThrows(IllegalArgumentException.class,
        () -> io.saveExtendedTo(out.toPath(), com.fpetrola.z80.ide.rzx.RzxWriter.Mode.NEW_BLOCK));
  }

  /**
   * Reproduce {@code cuantos} frames de verdad: consume las lecturas que cada frame grabo y
   * avanza fetches hasta que el predicado lo cierre. Es el bucle del emulador sin la CPU.
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
   * EL TRAMO VIVO VA ANTES Y LA GRABACION ENTERA DESPUES.
   *
   * <p>Para cubrir lo que una grabacion NO tiene, agregar al final no siempre sirve: si el juego
   * termina en una pantalla de la que no se vuelve -- JSW se va al toilet y se queda ahi -- no hay
   * forma de seguir jugando. Lo que falta hay que jugarlo ANTES.
   *
   * <p>Y tiene que ser UNA SOLA ejecucion continua desde UN SOLO snapshot. El formato admite
   * snapshots intercalados (Fuse los usa para rollback, y {@code rzxtool -f} los saca), pero para
   * esta cadena no sirven: un estado de maquina que aparece de la nada es memoria que cambia sin
   * que ninguna instruccion la haya escrito, y las tecnicas le inventarian procedencia -- el mismo
   * problema que los pokes.
   *
   * <p>Medido: 300 frames vivos ociosos en el titulo seguidos de los 85.369 de jsw-full reproducen
   * con cero desvio. El bucle del titulo esta atado al frame y escanea el teclado a ritmo fijo
   * (556 lecturas por frame), asi que los frames grabados encajan solos.
   */
  @Test
  void el_tramo_vivo_va_antes_y_la_grabacion_entera_despues() throws Exception {
    RzxFile original = grabacion();
    List<InputRecordingBlock.Frame> viejos = original.getInputRecordingBlock().frames;

    RZXPlayerIO io = new RZXPlayerIO();
    io.setup(original);
    io.setAcceptsInterrupt(() -> false);
    io.setRecordLive(true);
    io.goLive();                       // desde el frame cero: -Dplay=true
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
   * EL EMPALME NO SIEMPRE ES EL FRAME CERO: se empalma por ZONA DE CODIGO.
   *
   * <p>Los INs grabados solo reproducen la misma partida si los recibe la MISMA rutina que los
   * grabo. jsw-full arranca en la pantalla de codigos -- 591 frames dando vueltas en {@code $87b2}
   * (34738, el loader que este repo ya conoce) -- y despues de un game over el juego NO vuelve
   * ahi, vuelve a la pantalla de inicio, que es posterior. Anteponer contra el frame 0 le sirve la
   * entrada de codigos a un juego que ya paso esa pantalla, y se desmadra.
   *
   * <p>Por eso el prefijo se pega contra el frame donde la grabacion esta en la MISMA zona en la
   * que termino lo jugado, y los frames anteriores se descartan.
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
   * SANDWICH: lo jugado va EN EL MEDIO y la grabacion sigue despues.
   *
   * <p>Es la forma general, y las otras dos son casos suyos: agregar al final es reanudar en el
   * ultimo frame, anteponer es cortar en el cero. Existe porque cubrir lo que falta no siempre se
   * puede al final -- JSW termina en el toilet y no vuelve -- ni al principio: cortar en el frame
   * cero obliga a entrar los codigos a mano, que son los primeros 591 frames de jsw-full. Cortando
   * DESPUES de esa pantalla los codigos los reproduce la grabacion.
   *
   * <p>Los dos numeros son distintos a proposito: se corta donde uno quiera jugar y se reanuda
   * donde la grabacion este en la MISMA zona de codigo en la que termino lo jugado. Cual es ese
   * frame no lo sabe esta clase: se busca con el PC del borde y se confirma con el gate.
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
   * ANTEPONER SOLO VALE SI EL VIVO ARRANCO EN EL FRAME CERO. Si se corto en el medio, el tramo
   * jugado continua el estado de ESE punto, no el del snapshot: ponerlo adelante daria un archivo
   * que arranca de un estado que ese tramo nunca vio.
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
   * SIN EL FLAG, EL FINAL SIGUE SIENDO UNA EXCEPCION: hay tres runners que usan
   * {@code "rzx finished"} como senal de fin de corrida, y continuar en vivo los dejaria
   * corriendo para siempre.
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
