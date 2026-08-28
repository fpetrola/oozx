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
 * ESCRIBE UNA GRABACION EXTENDIDA: el mismo estado inicial, mas frames al final.
 *
 * <p>El archivo que sale es <b>el original con la cola pegada</b>: cabecera, creador y bloque
 * de snapshot se copian byte a byte ({@link RzxFile#getPrefix()}) y lo unico que se genera de
 * nuevo es UN bloque de entrada 0x80 con los frames viejos seguidos de los nuevos. Reproducirlo
 * vuelve a recorrer todo desde el principio y despues sigue por donde se jugo.
 *
 * <p><b>Dos modos, y el default es el compatible.</b> La spec permite varios bloques de entrada
 * -- cada uno continua al anterior -- pero no todos los reproductores saben encadenarlos, asi
 * que por defecto ({@link Mode#CONTINUE_BLOCK}) todo se funde en UN bloque, que es la forma que
 * cualquiera sabe leer. {@link Mode#NEW_BLOCK} deja el archivo original intacto byte a byte y le
 * pega un bloque aparte: mas fiel a lo que ya estaba, menos portable.
 *
 * <p>Los dos modos asumen que los bloques 0x80 son lo ULTIMO del archivo, que es como los
 * escriben los grabadores reales. Un bloque de otro tipo despues del primer 0x80 sobrevive en
 * {@code NEW_BLOCK} y se pierde al fundir.
 *
 * <p><b>Los frames se escriben literales, nunca con el marcador de repeticion.</b> El lector
 * toma {@code inCounter} grande como "repetir el frame anterior", asi que un frame con esa
 * cantidad de lecturas seria irrecuperable: {@link #writeExtended} lo rechaza en vez de emitir
 * algo que no se puede volver a leer.
 */
public class RzxWriter {
  public enum Mode {
    /**
     * UN SOLO BLOQUE 0x80 con los frames viejos y los nuevos. Es el default porque es lo que
     * cualquier reproductor sabe leer, incluso los que no encadenan bloques.
     */
    CONTINUE_BLOCK,
    /**
     * EL ARCHIVO ORIGINAL INTACTO y un bloque 0x80 nuevo pegado atras. No re-escribe un solo
     * byte de lo que ya estaba grabado, pero depende de que el reproductor continue de un
     * bloque al siguiente.
     */
    NEW_BLOCK
  }

  /**
   * El lector trata {@code inCounter >= 10000} como marcador de frame repetido (la spec usa
   * 0xFFFF). Mientras esa sea la convencion del lector, este es el techo real de un frame.
   */
  public static final int MAX_IN_COUNTER = 10000;

  private RzxWriter() {
  }

  /**
   * @param original grabacion ya parseada, de la que salen el prefijo y los frames de base
   * @param extra    frames grabados a continuacion (puede estar vacio: entonces es una copia)
   * @param out      archivo a escribir
   */
  public static void writeExtended(RzxFile original, List<InputRecordingBlock.Frame> extra, Path out)
      throws IOException {
    writeExtended(original, extra, out, Mode.CONTINUE_BLOCK);
  }

  /**
   * @param original grabacion ya parseada, de la que salen el prefijo y los frames de base
   * @param extra    frames grabados a continuacion (puede estar vacio: entonces es una copia)
   * @param out      archivo a escribir
   * @param mode     fundir todo en un bloque, o pegar uno nuevo dejando el original intacto
   */
  public static void writeExtended(RzxFile original, List<InputRecordingBlock.Frame> extra, Path out,
                                   Mode mode) throws IOException {
    writeExtended(original, -1, extra, out, mode);
  }

  /**
   * @param baseFrames cuantos frames del original conservar, o -1 por todos. Existe porque lo
   *                   jugado no siempre continua al FINAL de la grabacion: si se corto en el
   *                   medio, los frames posteriores al corte no los recorrio nadie y pegarlos
   *                   daria un archivo que al reproducirse pasa por un tramo que la sesion viva
   *                   nunca vio.
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

    // los frames viejos vienen del parser y ya se leyeron una vez; los nuevos, no. Validar
    // SOLO los nuevos deja pasar un original raro, y validar todo cuesta un recorrido: se
    // valida todo, que es lo que hace encodeFrames.
    byte[] payload = deflate(encodeFrames(frames));

    try (OutputStream os = Files.newOutputStream(out)) {
      os.write(prefix);
      if (nuevo)
        os.write(original.getTail());
      os.write(0x80);
      // longitud del bloque ENTERA, contando el id y la longitud misma:
      // 1 + 4 + 4 (frames) + 1 (reservado) + 4 (tStates) + 4 (flags) = 18 de cabecera
      writeInt(os, 18 + payload.length);
      writeInt(os, frames.size());
      os.write(0);
      // el contador de tStates del bloque nuevo arranca en cero: continua al anterior.
      writeInt(os, nuevo ? 0 : (int) block.tStates);
      // protegido = 0 a proposito: la grabacion nueva se puede volver a extender.
      writeInt(os, 0x02);
      os.write(payload);
    }
  }

  /**
   * EL TRAMO VIVO ADELANTE Y LA GRABACION ENTERA DETRAS, contra el mismo snapshot.
   *
   * <p>Existe porque cubrir lo que una grabacion NO tiene no siempre se puede agregando al final:
   * si el juego termina en una pantalla de la que no se vuelve -- JSW se va al toilet y se queda
   * ahi -- lo que falta hay que jugarlo ANTES. Y tiene que ser UNA ejecucion continua desde UN
   * snapshot: el formato admite snapshots intercalados (Fuse los usa para rollback,
   * {@code rzxtool -f} los saca) pero un estado de maquina que aparece de la nada es memoria que
   * cambia sin que ninguna instruccion la escriba, y las tecnicas de la cadena le inventarian
   * procedencia -- el mismo problema que los pokes.
   *
   * <p>Que los frames grabados sigan sirviendo despues de un tramo vivo NO es obvio: sus INs solo
   * reproducen la misma partida si la maquina llega al empalme en un estado equivalente. Medido
   * sobre jsw-full con 300 frames vivos ociosos en el titulo: cero desvio.
   *
   * @param prefix lo jugado en vivo, que arranco en el frame CERO de la grabacion
   */
  public static void writePrepended(RzxFile original, List<InputRecordingBlock.Frame> prefix, Path out)
      throws IOException {
    writePrepended(original, prefix, 0, out);
  }

  /**
   * @param fromFrame desde que frame de la grabacion seguir; lo anterior se DESCARTA. Existe
   *                  porque el empalme es por ZONA DE CODIGO y no por numero: los INs grabados
   *                  solo reproducen la misma partida si los recibe la misma rutina que los grabo.
   *                  jsw-full arranca con 591 frames de entrada de codigos ({@code $87b2} = 34738),
   *                  y despues de un game over el juego no vuelve ahi sino a la pantalla de inicio,
   *                  que es posterior: pegar contra el frame 0 le sirve la entrada de codigos a un
   *                  juego que ya paso esa pantalla.
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
   * LA FORMA GENERAL: {@code frames[0..cut)} + lo jugado + {@code frames[resume..]}, contra el
   * mismo snapshot y en una sola ejecucion continua. Agregar al final y anteponer son casos suyos.
   *
   * <p>Los dos numeros son distintos A PROPOSITO. Se corta donde uno quiera empezar a jugar; se
   * reanuda donde la grabacion este en la MISMA zona de codigo en la que quedo lo jugado, porque
   * los INs grabados solo reproducen la misma partida si los recibe la rutina que los grabo. Cual
   * es ese frame NO lo decide esta clase: se busca por el PC del borde del frame y se confirma con
   * el gate de sincronia, que sobre un empalme equivocado se desmadra en el acto.
   *
   * @param cut     hasta que frame de la grabacion se reprodujo (exclusivo)
   * @param live    lo jugado
   * @param resume  desde que frame sigue la grabacion; entre {@code cut} y este se DESCARTA
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
