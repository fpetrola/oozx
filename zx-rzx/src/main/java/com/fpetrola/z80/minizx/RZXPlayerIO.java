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
import com.fpetrola.z80.minizx.emulation.OutListener;
import com.fpetrola.z80.registers.Register;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;


public class RZXPlayerIO implements MiniZXIO {
  public MiniZXKeyboard miniZXKeyboard;
  private Register pc;
  /**
   * LIVE mode: the recording is abandoned and the player takes over — INs read the real
   * keyboard matrix instead of the recorded values, and frame interrupts fire every
   * {@link #liveFetches} fetches (the recording's own average frame length, so the game
   * keeps its familiar pace). One way: once the input diverges there is no going back.
   */
  private volatile boolean live;
  /**
   * MODO STREAM: la entrada grabada se sirve como una secuencia continua en vez de estrictamente
   * por frame. Existe para el juego TRADUCIDO a Java, que no reproduce el conteo de fetches del
   * Z80 —sus {@code pc(addr, rdelta)} miden longitudes de instruccion, no ciclos M1— asi que la
   * alineacion por frame lo deja sin entrada: consume mas lecturas de las que el frame grabo,
   * recibe el ultimo valor repetido, y se queda para siempre en la pantalla de codigos de JSW.
   * Medido con 20 millones de instrucciones: 379 pixeles encendidos contra 1.363 en stream.
   *
   * <p><b>Por defecto esta APAGADO</b> y eso es deliberado: para el emulador, que si reproduce
   * los fetches, la alineacion estricta es lo correcto y esta medida —arrastrar las lecturas que
   * un frame no consumio hacia que el siguiente leyera un barrido viejo de teclado, y el desfase
   * se amplificaba solo (Abu Simbel: 26% de frames sincronizados)—. Son dos consumidores con
   * fidelidades distintas, no una politica mejor que la otra.
   */
  private boolean streamed;
  private long liveFetches = 17000;
  /**
   * GRABAR LO QUE SE JUEGA EN VIVO, para escribir despues una grabacion extendida: el mismo
   * snapshot inicial, los frames viejos, y a continuacion los jugados. Cada frame vivo aporta
   * los dos unicos numeros que el formato pide -- cuantos fetches duro y que bytes devolvio
   * cada IN -- y los dos los produce esta misma clase, asi que la cola es reproducible por
   * construccion: al reproducirla, el mismo emulador desde el mismo estado consume las mismas
   * lecturas en los mismos bordes. Ver {@link com.fpetrola.z80.ide.rzx.RzxWriter}.
   */
  private boolean recordLive;
  /**
   * QUE PASA CUANDO SE ACABA LA GRABACION. Por defecto {@link #changeFrame()} tira
   * {@code "rzx finished"}, y eso NO se puede cambiar por las buenas: hay runners que usan esa
   * excepcion como senal de fin de corrida. Con el flag puesto, en cambio, el final es el
   * comienzo del modo vivo -- que es como se extiende una grabacion.
   */
  private boolean continueLiveAtEnd;
  private final List<InputRecordingBlock.Frame> recordedFrames = new ArrayList<>();
  private final java.io.ByteArrayOutputStream liveIns = new java.io.ByteArrayOutputStream();
  /** la grabacion de la que salimos: de ahi vienen el snapshot y los frames viejos al guardar. */
  private RzxFile source;
  /**
   * EL FRAME DONDE EMPEZO EL VIVO. Lo jugado continua DESDE AHI, no desde el final de la
   * grabacion: si se corto en el medio (F1), los frames posteriores al corte no los recorrio
   * nadie y arrastrarlos daria un archivo que al reproducirse pasa por un tramo que la sesion
   * viva nunca vio. -1 mientras se este reproduciendo.
   *
   * <p>El frame DEL CORTE se graba entero -- sus lecturas de antes del corte incluidas, y con su
   * duracion original -- porque escribirlo con solo la cola produce un archivo INVALIDO que los
   * reproductores de verdad rechazan ("more INs during frame N than stored in RZX file"). Ver
   * {@link #anotar(int)} y {@link #frameBudget}.
   */
  private int liveStartFrame = -1;
  /**
   * CUANTOS FETCHES DURA EL FRAME EN CURSO. Se fija al EMPEZAR cada frame -- el de la grabacion
   * mientras se reproduce, {@link #liveFetches} una vez en vivo -- y {@link #goLive()} NO lo toca:
   * el frame que estaba a medio correr cuando se apreto F1 termina con SU presupuesto.
   *
   * <p>Sin esto, el frame del corte salia con la duracion del frame vivo (medido en jsw-full:
   * 6.621 donde la grabacion decia 8.099), o sea que el archivo le mentia la duracion a un frame
   * que ya venia corriendo.
   */
  private long frameBudget;
  private int currentFrameIndex;
  private InputRecordingBlock.Frame currentFrame;
  private SimpleQueue<Byte> inputs = new SimpleQueue<>(1000000);
  private InputRecordingBlock inputRecordingBlock;
  private List<InputRecordingBlock.Frame> frames;
  private long lastCount;
  private byte lastPoll;
  private List<OutListener> outListeners = new ArrayList<>();
  private int fetchCounter;
  public static boolean stop;
  private static final boolean DEBUG_SYNC = Boolean.getBoolean("rzx.debug");
  /** -Drzx.advance=ins: avance por demanda de INs solamente, sin cursor por fetches. */
  private static final boolean ADVANCE_BY_INS = "ins".equals(System.getProperty("rzx.advance"));
  private static final boolean POLL_GUARD = !"false".equals(System.getProperty("rzx.pollguard"));

  /**
   * SINCRONIZACIÓN medida contra la propia grabación: cada frame del RZX dice cuántas
   * lecturas de puerto hizo la máquina que grabó. Si nuestra reproducción es fiel, el
   * emulador tiene que consumir EXACTAMENTE esa cantidad en ese frame. Los desvíos son
   * el diagnóstico: {@code framesShort} = el juego leyó MENOS de lo grabado (el sobrante
   * se arrastra al frame siguiente), {@code framesOver} = leyó MÁS (se quedó sin valores
   * y le servimos relleno). {@code firstDesync} es el frame donde empezó a irse.
   */
  public int framesExact, framesShort, framesOver, firstDesync = -1;
  public long insConsumed, insRecorded;
  private int consumedThisFrame;
  /** desvío por frame (consumidos - grabados) → cuántos frames: un ±1 sistemático es un
   *  problema de BORDE (el corte del frame), un desvío grande y variable es divergencia
   *  de ejecución. */
  public final java.util.Map<Integer, Integer> deltaHist = new java.util.TreeMap<>();
  /** desde qué PC se pidió cada lectura de puerto: señala la rutina responsable. */
  public final java.util.Map<Integer, Integer> inPcHist = new java.util.HashMap<>();
  /** dónde estaba el PC cuando CORTAMOS el frame: una reproducción fiel corta siempre en
   *  el mismo puñado de sitios (el HALT o el bucle de espera del juego); cortes dispersos
   *  adentro de una rutina significan que el contador de fetches no coincide. */
  public final java.util.Map<Integer, Integer> intPcHist = new java.util.HashMap<>();
  /**
   * Los primeros frames, uno por uno: {@code {frame, delta, pc donde cortó}}. El histograma dice
   * CUÁNTO se va y este dice CUÁNDO empieza y QUÉ estaba ejecutando — que es la diferencia entre
   * "se desincroniza" y "se desincroniza al entrar en la ROM".
   */
  public final java.util.List<int[]> frameLog = new java.util.ArrayList<>();
  public static final int FRAME_LOG_MAX = 4000;
  /**
   * CADA LECTURA DE PUERTO DE UNA VENTANA DE FRAMES: {@code {frame, orden, pc, puerto, valor}}.
   * El delta por frame dice CUÁNTAS lecturas sobran o faltan; esto dice CUÁL. La diferencia
   * importa: una lectura que falta AL FINAL del frame es el borde cayendo antes, y una que falta
   * EN EL MEDIO es otro camino de ejecución. {@code -Dzx.in.dump=305-315}.
   */
  public final java.util.List<int[]> inLog = new java.util.ArrayList<>();
  private static final int[] IN_DUMP = parseWindow(System.getProperty("zx.in.dump"));

  private static int[] parseWindow(String spec) {
    if (spec == null)
      return null;
    String[] parts = spec.split("-");
    return new int[]{Integer.parseInt(parts[0]),
        Integer.parseInt(parts[parts.length - 1])};
  }

  public RZXPlayerIO() {
    miniZXKeyboard = new MiniZXKeyboard();
  }

  private final List<OutListener> inListeners = new ArrayList<>();

  public void addInListener(OutListener inListener) {
    inListeners.add(inListener);
  }

  public void addOutListener(OutListener outListener) {
    outListeners.add(outListener);
  }

  public int getCurrentFrameIndex() {
    return currentFrameIndex;
  }

  /** el contador de fetches del emulador, tal como lo ve el predicado de interrupcion. */
  public int getFetchCounter() {
    return fetchCounter;
  }

  /** donde EMPEZO el frame en curso, en fetches: {@code fetchCounter - lastCount} es la altura. */
  public long getFrameStart() {
    return lastCount;
  }

  /** cuantas lecturas de puerto lleva consumidas el frame en curso. */
  public int getConsumedThisFrame() {
    return consumedThisFrame;
  }

  /** último byte escrito al puerto $FE: de ahí sale el EAR que se lee de vuelta. */
  private int lastFe = 0x18;

  public void out(int port, int value) {
    if ((port & 0xff) == 0xfe)
      lastFe = value & 0xff;
    // estaba comentado: los listeners de OUT no se disparaban nunca, asi que quien los
    // registraba no recibia un solo evento y no habia como saberlo
    for (OutListener l : outListeners)
      l.outAt(port, value);
  }

  /**
   * El byte que devuelve el puerto del teclado EN VIVO. Los bits 0-4 son la media fila; el
   * 5 y el 7 van en 1 (no existen) y el 6 es el EAR, que en un 48K issue 3 devuelve lo
   * último que se escribió al altavoz.
   *
   * <p>Se medía mal: {@code & 191} forzaba el bit 6 a 0 SIEMPRE, así que en vivo nunca
   * devolvíamos $FF. Contra las lecturas GRABADAS —el oráculo— eso es al revés de lo que
   * espera medio catálogo: Abu Simbel recibe $FF en el 95% de sus lecturas y Wally $BF en
   * el 84%. Un juego que compara el byte entero con $FF para decidir "no hay tecla" ve
   * una tecla apretada para siempre, y deja de responder.
   */
  public static int liveInValue(int port, MiniZXKeyboard kb, int lastFe) {
    // PUERTO IMPAR = KEMPSTON, y hay que contestar QUE NO HAY JOYSTICK, que es el bit 5
    // ENCENDIDO. Devolviamos 0 con el argumento de que Kempston sin movimiento lee 0 -- cierto,
    // pero solo si hay uno conectado. JSW pregunta primero si lo hay (34968): junta 256 lecturas
    // del puerto 31 con OR, hace AND 32, y si da cero concluye que HAY joystick y prende el
    // indicador Kempston en 34254. Con nuestros 256 ceros concluia siempre que si.
    //
    // Medido: 34254 vale 0 en jsw-full -grabacion de una maquina real- y valia 1 en todo lo que
    // grabamos en vivo. Eso cambia POR DONDE lee la entrada el juego, o sea cuantos IN hace por
    // frame, que es justo de lo que depende la contabilidad de un RZX: una grabacion viva y una
    // real dejaban de ser continuables entre si por este byte.
    //
    // $20 y no $ff: bit 5 encendido (no hay joystick) con las direcciones en cero, asi que un
    // juego que lea Kempston sin preguntar tampoco ve movimiento inventado.
    if ((port & 1) != 0)
      return 0x20;
    int ear = (lastFe & 0x10) != 0 ? 0x40 : 0;
    return (kb.readKeyboardPort(port, true) & 0x1f) | 0xa0 | ear;
  }

  /** lo enciende quien reproduce con un motor que NO cuenta fetches como el Z80. */
  public void setStreamed(boolean streamed) {
    this.streamed = streamed;
  }

  public boolean isStreamed() {
    return streamed;
  }

  public boolean isLive() {
    return live;
  }

  /**
   * CUANTOS FRAMES MIRA PARA DECIDIR CUANTO DURA UN FRAME VIVO. Los vecinos AL CORTE, no los del
   * arranque: cuantos fetches entran en un frame real depende de que esta ejecutando el juego, y
   * el titulo y el juego andando tienen mezclas de instrucciones distintas.
   */
  public static final int LIVE_WINDOW = 200;

  public void goLive() {
    if (live || frames == null)
      return;
    liveFetches = medianFrameLength(currentFrameIndex);
    miniZXKeyboard.reset();
    liveStartFrame = currentFrameIndex;
    live = true;
  }

  /**
   * LA MEDIANA DE LOS FRAMES VECINOS AL CORTE, en fetches.
   *
   * <p>Un frame de RZX se mide en fetches, pero un reproductor de verdad ademas lleva su reloj de
   * T-states y avisa si el frame no entra en uno (Fuse: <em>"RZX frame is longer than 79000
   * tstates"</em>). Este emulador NO cuenta T-states -- la maquinaria de {@code fuse.tstates}
   * esta desconectada -- asi que el presupuesto del frame vivo se estima, y la grabacion misma es
   * el mejor estimador que hay: se hizo en una maquina real a 50 Hz, o sea que el frame TIPICO de
   * la vecindad del corte ES un frame de 69.888 T-states expresado en fetches.
   *
   * <p>Mediana y no promedio porque el promedio se lo lleva cualquier frame raro. Y vecinos al
   * corte y no los primeros 2.000: medido en jsw-full, promediar el arranque daba 7.302 fetches
   * donde el juego en el corte hacia 6.527 -- +12%, ~78.200 T-states, y Fuse avisaba.
   */
  private long medianFrameLength(int cut) {
    int end = Math.min(cut, frames.size());
    int start = Math.max(0, end - LIVE_WINDOW);
    if (end - start < 1) {
      // se corto en el frame cero (-Dplay=true): no hay vecindad previa, se toma la del arranque
      start = 0;
      end = Math.min(LIVE_WINDOW, frames.size());
    }
    if (end - start < 1)
      return liveFetches;
    int[] lengths = new int[end - start];
    for (int i = start; i < end; i++)
      lengths[i - start] = frames.get(i).fetchCounter;
    java.util.Arrays.sort(lengths);
    return Math.max(1000, lengths[lengths.length / 2]);
  }

  public synchronized int in(int port) {
    int value = enElPuerto(port);
    for (OutListener l : inListeners)
      l.outAt(port, value);
    return value;
  }

  private int enElPuerto(int port) {
    if (live) {
      int value = liveInValue(port, miniZXKeyboard, lastFe);
      anotar(value);
      return value;
    }
    if (currentFrame == null)
      return 0;
    return performIn(port);
  }

  private int lastPort;

  private int performIn(int port) {
    lastPort = port;
    byte value = getNextInput();
    anotar(value);
    return value;
  }

  /**
   * TAMBIEN SE ANOTAN LAS LECTURAS DE LA REPRODUCCION, no solo las vivas. Parece de mas -- los
   * frames reproducidos no se graban, se copian del original -- pero es lo que hace que el frame
   * DEL CORTE quede entero: cuando F1 cae en el medio, ese frame ya sirvio parte de sus lecturas
   * desde la grabacion, y sin ellas el frame vivo declara menos lecturas de las que el juego hace.
   * Los reproductores de verdad lo cazan y se plantan (Fuse: "more INs during frame N than stored
   * in RZX file"). El buffer se tira al cerrar cada frame reproducido, asi que solo sobrevive el
   * pedazo del frame en curso, que es justo lo que hace falta.
   */
  private void anotar(int value) {
    if (recordLive)
      liveIns.write(value & 0xff);
  }

  private byte getNextInput() {
    if ( stop)
      throw new RuntimeException("stop");
    if (inputs.isEmpty()) {
      // El emulador pidió MÁS lecturas de las que este frame grabó. Los valores del frame
      // siguiente son del frame siguiente: servirlos ahora adelanta la entrada y ADEMÁS
      // desacopla el índice de frame del contador de fetches (que sigue midiendo contra
      // el frame viejo). Se repite el último valor y se sigue; el frame avanza sólo
      // cuando se cumplen sus fetches.
      if (DEBUG_SYNC)
        System.out.println("rzx-sync: frame " + currentFrameIndex
            + " ran out of inputs BEFORE its interrupt (emulator consumed too many INs)");
      if (streamed) {
        if (ADVANCE_BY_INS) {
          // EL MODELO DE AVANCE ORIGINAL (539228262 "rzx working"): la grabacion camina SOLO a
          // demanda de INs, saltando los frames que no grabaron ninguna lectura hasta dar con
          // uno que tenga material. Asi no hace falta ningun cursor por conteo de fetches --
          // pc() no participa del avance. El bucle siempre termina: pasado el ultimo frame,
          // changeFrame tira "rzx finished".
          while (inputs.isEmpty()) {
            ++currentFrameIndex;
            changeFrame();
          }
        } else {
        // la secuencia sigue en el frame siguiente: se avanza y se sirve de ahi
        ++currentFrameIndex;
        changeFrame();
        }
      } else {
        consumedThisFrame++;
        if (pc != null)
          inPcHist.merge(pc.read(), 1, Integer::sum);
        return lastPoll;
      }
    }
    consumedThisFrame++;
    if (pc != null)
      inPcHist.merge(pc.read(), 1, Integer::sum);
    // POLL SOLO SI HAY: SimpleQueue#poll no tiene guarda -- devuelve data[head] y decrementa
    // igual, asi que sacar de una cola vacia deja counter en NEGATIVO y su isEmpty()
    // (counter == 0) no vuelve a ser cierto NUNCA. Un frame grabado con cero lecturas alcanza
    // para disparar eso, y a partir de ahi el avance por agotamiento -- el unico que le queda a
    // la reproduccion cuando no hay borde de frame por fetches -- queda muerto y ademas se
    // sirven valores rancios del buffer circular. Medido sin pc(): la grabacion camina sola
    // hasta el frame 592 y se clava ahi con counter en -44.044.
    //
    // -Drzx.pollguard=false devuelve el comportamiento viejo, para medir contra el.
    Byte poll = (POLL_GUARD && inputs.isEmpty()) ? null : inputs.poll();
    if (poll == null)
      return lastPoll;
    else
      lastPoll = poll;
    if (IN_DUMP != null && currentFrameIndex >= IN_DUMP[0] && currentFrameIndex <= IN_DUMP[1])
      inLog.add(new int[]{currentFrameIndex, consumedThisFrame,
          pc == null ? -1 : pc.read(), lastPort, poll & 0xff});
    return poll;
  }

  /**
   * THE RECORDING IS PAST ITS LAST FRAME. A harness that answers some port on the recording's
   * behalf (absent hardware, floating bus) must stop answering HERE and let every later
   * {@code in()} reach {@link #getNextInput}: the queue may still hold the final frame's
   * leftover values, draining them is the tail's job, and "rzx finished" is thrown from there
   * and nowhere else. Both halves are measured on JSW -- answering past this point left the
   * replay spinning at the final frame until the 2-billion-step cap (twice: once waiting for
   * the queue to empty on its own, which nothing does), with the end state already correct.
   */
  public boolean finished() {
    return frames != null && currentFrameIndex >= frames.size();
  }

  public MiniZXKeyboard getMiniZXKeyboard() {
    return miniZXKeyboard;
  }

  /**
   * SI LA CPU PUEDE ACEPTAR LA INTERRUPCIÓN en el momento en que el frame cierra. El
   * acknowledge es un ciclo M1 y por lo tanto un fetch: un frame donde la interrupción se acepta
   * cuesta uno más que uno donde el juego la tenía deshabilitada. Si el grabador y nosotros no
   * contamos ese fetch del mismo lado del borde, el desfase aparece SÓLO en los frames donde eso
   * cambia -- que es la forma que tiene el desvío de Abu Simbel, y por eso corrar todos los
   * frames por igual no lo arregló.
   */
  private java.util.function.BooleanSupplier acceptsInterrupt;

  public void setAcceptsInterrupt(java.util.function.BooleanSupplier acceptsInterrupt) {
    this.acceptsInterrupt = acceptsInterrupt;
  }

  public void setPc(Register pc) {
    this.pc = pc;
  }

  public void setup(RzxFile rzxFile) {
    source = rzxFile;
    inputRecordingBlock = rzxFile.getInputRecordingBlock();
    frames = inputRecordingBlock.frames;
    currentFrameIndex = 0;
    // el contador que llega acá son FETCHES desde el arranque del replay (empieza en 0);
    // los tStates del bloque son otra unidad — usarlos retrasaba la primera interrupción
    // tantos fetches como tStates trajera la grabación
    lastCount = 0;
    lastPoll = 0;
    fetchCounter = 0;
    inputs.clear();
    changeFrame();
  }

  private void changeFrame() {
    if (currentFrameIndex < frames.size()) {
      printFrameCount();

      // Los valores IN pertenecen AL FRAME: los que este frame no llegó a consumir se
      // DESCARTAN. Arrastrarlos hacía que el frame siguiente leyera el teclado de un
      // barrido viejo, y el desfase se amplificaba solo (medido en Abu Simbel: 26% de
      // frames sincronizados, con desvíos simétricos -4/+4 entre frames vecinos).
      if (!streamed)
        inputs.clear();
      currentFrame = frames.get(currentFrameIndex);
      frameBudget = currentFrame.fetchCounter;
      for (int i = 0; i < currentFrame.returnValues.length; i++) {
        inputs.add(currentFrame.returnValues[i]);
      }
    } else {
      if (inputs.isEmpty()) {
        if (continueLiveAtEnd) {
          // se imprime SIEMPRE y una sola vez: es un cambio de modo, y sin esta linea la
          // transicion no se puede observar desde afuera -- el contador de frames sigue
          // avanzando igual, asi que "termino" y "sigue en vivo" se ven identicos
          System.out.println("rzx terminado en el frame " + currentFrameIndex
              + ": el teclado es tuyo, se graba lo que juegues (Ctrl+G guarda)");
          goLive();
          frameBudget = liveFetches;
          return;
        }
        throw new RuntimeException("rzx finished");
      }
      inputs.add((byte) 0);
    }
  }

  private void printFrameCount() {
    if (DEBUG_SYNC && currentFrameIndex % 1000 == 0)
      System.out.println(currentFrameIndex);
  }

  /** anotar cada frame jugado en vivo, para poder guardar la grabacion extendida. */
  public void setRecordLive(boolean recordLive) {
    this.recordLive = recordLive;
  }

  public boolean isRecordLive() {
    return recordLive;
  }

  /** al agotarse los frames, seguir en vivo en vez de tirar {@code "rzx finished"}. */
  public void setContinueLiveAtEnd(boolean continueLiveAtEnd) {
    this.continueLiveAtEnd = continueLiveAtEnd;
  }

  public boolean isContinueLiveAtEnd() {
    return continueLiveAtEnd;
  }

  /** el frame donde el jugador tomo el control, o -1 si todavia se esta reproduciendo. */
  public int getLiveStartFrame() {
    return liveStartFrame;
  }

  /** los frames jugados en vivo hasta ahora, en orden. */
  public List<InputRecordingBlock.Frame> getRecordedFrames() {
    return recordedFrames;
  }

  private void closeLiveFrame(int fetches) {
    InputRecordingBlock.Frame frame = new InputRecordingBlock.Frame();
    frame.fetchCounter = fetches;
    frame.returnValues = liveIns.toByteArray();
    frame.inCounter = frame.returnValues.length;
    liveIns.reset();
    recordedFrames.add(frame);
  }

  /**
   * ESCRIBE LA GRABACION EXTENDIDA: el snapshot y los frames del original, y a continuacion lo
   * jugado. El escritor valida cada frame nuevo, asi que un frame que no se pueda volver a leer
   * -- demasiadas lecturas, fetchCounter que no entra en 16 bits -- falla aca y no en silencio.
   */
  public void saveExtendedTo(java.nio.file.Path out) throws java.io.IOException {
    saveExtendedTo(out, com.fpetrola.z80.ide.rzx.RzxWriter.Mode.CONTINUE_BLOCK);
  }

  public void saveExtendedTo(java.nio.file.Path out, com.fpetrola.z80.ide.rzx.RzxWriter.Mode mode)
      throws java.io.IOException {
    if (source == null)
      throw new IllegalStateException("no hay grabacion de origen: falto setup(RzxFile)");
    com.fpetrola.z80.ide.rzx.RzxWriter.writeExtended(source, liveStartFrame, recordedFrames, out, mode);
  }

  /**
   * GUARDA LO JUGADO ANTES DE LA GRABACION, con la grabacion entera a continuacion. Ver
   * {@link com.fpetrola.z80.ide.rzx.RzxWriter#writePrepended}.
   *
   * <p>Solo vale si el vivo arranco en el frame CERO ({@code -Dplay=true}): un tramo jugado desde
   * el medio continua el estado de ESE punto, y ponerlo adelante daria un archivo que arranca de
   * un estado que ese tramo nunca vio.
   */
  /**
   * GUARDA EL SANDWICH: lo reproducido hasta el corte, lo jugado, y la grabacion desde
   * {@code resume}. Es la forma general -- ver
   * {@link com.fpetrola.z80.ide.rzx.RzxWriter#writeSpliced}.
   */
  public void saveSplicedTo(java.nio.file.Path out, int resume) throws java.io.IOException {
    if (source == null)
      throw new IllegalStateException("no hay grabacion de origen: falto setup(RzxFile)");
    if (liveStartFrame < 0)
      throw new IllegalStateException("todavia no se jugo nada");
    com.fpetrola.z80.ide.rzx.RzxWriter.writeSpliced(source, liveStartFrame, recordedFrames, resume, out);
  }

  public void savePrependedTo(java.nio.file.Path out) throws java.io.IOException {
    savePrependedTo(out, 0);
  }

  /**
   * @param fromFrame desde que frame de la grabacion seguir. Ver
   *                  {@link com.fpetrola.z80.ide.rzx.RzxWriter#writePrepended(RzxFile, java.util.List, int, java.nio.file.Path)}:
   *                  el empalme es por zona de codigo, no por numero de frame.
   */
  public void savePrependedTo(java.nio.file.Path out, int fromFrame) throws java.io.IOException {
    if (source == null)
      throw new IllegalStateException("no hay grabacion de origen: falto setup(RzxFile)");
    if (liveStartFrame != 0)
      throw new IllegalStateException("anteponer exige haber jugado desde el frame cero"
          + " (-Dplay=true), y el vivo arranco en el " + liveStartFrame
          + ": ese tramo continua el estado de ahi, no el del snapshot");
    com.fpetrola.z80.ide.rzx.RzxWriter.writePrepended(source, recordedFrames, fromFrame, out);
  }

  public Predicate<Integer> getInterruptionCondition() {
    return (i) -> {
      fetchCounter = i;
      if (live) {
        if (i - lastCount + 1 > frameBudget) {
          // EL MISMO CIERRE QUE LA RAMA GRABADA, y no `lastCount = i`: el fetch del
          // acknowledge pertenece al frame que interrumpe, asi que el presupuesto del
          // siguiente empieza DESPUES de el. Cerrar el frame vivo con una regla y
          // reproducirlo con la otra es un fetch de desvio POR FRAME, acumulativo -- que es
          // exactamente la forma del desvio que costo medir en Abu Simbel (ver el comentario
          // del acknowledge mas abajo). Congelado en LiveRecordingTest.
          if (recordLive)
            closeLiveFrame((int) (i - lastCount));
          ++currentFrameIndex;
          frameBudget = liveFetches;
          lastCount = i + (acceptsInterrupt != null && acceptsInterrupt.getAsBoolean() ? 1 : 0);
          return true;
        }
        return false;
      }
      if (currentFrame != null)
        if (i - lastCount + 1 > frameBudget) {
          // cierre del frame: ¿consumimos las MISMAS lecturas que la grabación?
          insConsumed += consumedThisFrame;
          insRecorded += currentFrame.inCounter;
          deltaHist.merge(consumedThisFrame - currentFrame.inCounter, 1, Integer::sum);
          int intPc = pc == null ? -1 : pc.read();
          if (pc != null)
            intPcHist.merge(intPc, 1, Integer::sum);
          if (frameLog.size() < FRAME_LOG_MAX)
            frameLog.add(new int[]{currentFrameIndex,
                consumedThisFrame - currentFrame.inCounter, intPc,
                acceptsInterrupt == null ? -1 : acceptsInterrupt.getAsBoolean() ? 1 : 0});
          if (consumedThisFrame == currentFrame.inCounter)
            framesExact++;
          else {
            if (consumedThisFrame < currentFrame.inCounter)
              framesShort++;
            else
              framesOver++;
            if (firstDesync < 0)
              firstDesync = currentFrameIndex;
          }
          consumedThisFrame = 0;
          liveIns.reset();
          if (DEBUG_SYNC && !inputs.isEmpty())
            System.out.println("rzx-sync: frame " + currentFrameIndex
                + " reached its interrupt with unconsumed inputs (emulator consumed too few INs)");
          ++currentFrameIndex;
          changeFrame();
          // EL ACKNOWLEDGE ES EL ÚLTIMO FETCH DEL FRAME QUE INTERRUMPE, no el primero del
          // siguiente. Es un ciclo M1 -- incrementa R como cualquier fetch -- y el grabador lo
          // cuenta del lado del frame que cierra, así que el presupuesto del siguiente empieza
          // DESPUÉS de él. Y sólo existe cuando la CPU acepta: un frame con las interrupciones
          // deshabilitadas no lo tiene, que es por qué correr todos los frames por igual no
          // servía y por qué JSW -que corre con IFF1 en cero de punta a punta- nunca lo notó.
          //
          // Medido sobre Abu Simbel: el primer desvío pasa del frame 309 al 13.704 y los
          // exactos de 33,4% a 76,5% sobre 20.000 frames. JSW queda idéntico en 100%.
          lastCount = i + (acceptsInterrupt != null && acceptsInterrupt.getAsBoolean() ? 1 : 0);
          return true;
        } else
          return false;

      return false;
    };
  }
}
