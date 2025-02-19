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

public class RzxFile {
  private final RzxHeader header;
  private final CreatorInfo creatorInfo;
  private final SnapshotBlock snapshotBlock;
  private final InputRecordingBlock inputRecordingBlock;
  /**
   * LOS BYTES DEL ARCHIVO ANTERIORES AL BLOQUE DE ENTRADA (0x80): cabecera, creador y el
   * bloque de snapshot, TAL CUAL vinieron del disco. Existen para extender la grabacion sin
   * re-serializar el estado inicial -- que es la parte que no sabemos escribir y ademas la que
   * no hace falta tocar: el archivo extendido arranca del MISMO snapshot y solo cambia la
   * lista de frames. Vale para las dos formas de snapshot, embebido y descriptor externo,
   * porque no los interpreta. {@code null} si el archivo no traia bloque 0x80.
   */
  private byte[] prefix;
  /**
   * LOS BLOQUES DE ENTRADA TAL CUAL VINIERON DEL DISCO: desde el primer 0x80 hasta el fin del
   * archivo. Sirven para el modo que agrega un bloque nuevo sin tocar lo que ya estaba --
   * {@code prefix + tail} son el archivo original entero.
   */
  private byte[] tail;

  public RzxFile(RzxHeader header, CreatorInfo creatorInfo, SnapshotBlock snapshotBlock, InputRecordingBlock inputRecordingBlock) {
    this.header = header;
    this.creatorInfo = creatorInfo;
    this.snapshotBlock = snapshotBlock;
    this.inputRecordingBlock = inputRecordingBlock;
  }

  public RzxHeader getHeader() {
    return header;
  }

  public CreatorInfo getCreatorInfo() {
    return creatorInfo;
  }

  public SnapshotBlock getSnapshotBlock() {
    return snapshotBlock;
  }

  public InputRecordingBlock getInputRecordingBlock() {
    return inputRecordingBlock;
  }

  public byte[] getPrefix() {
    return prefix;
  }

  public void setPrefix(byte[] prefix) {
    this.prefix = prefix;
  }

  public byte[] getTail() {
    return tail;
  }

  public void setTail(byte[] tail) {
    this.tail = tail;
  }
}
