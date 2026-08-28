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


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;

import static java.lang.Integer.reverseBytes;
import static java.lang.Short.reverseBytes;

public class RzxParser {
  private final int HEADER_SIGNATURE = 0x21585A52; // "RZX!" in little-endian
  private SnapshotBlock snapshotBlock;
  private InputRecordingBlock inputRecordingBlock;
  /**
   * DONDE EMPIEZA EL BLOQUE 0x80 dentro del archivo, para poder quedarse con todo lo de antes
   * sin volver a serializarlo (ver {@link RzxFile#getPrefix()}). Se toma en {@link #parseBlocks}
   * ANTES de leer el id del bloque, y solo sirve si el stream sabe decir su posicion.
   */
  private int inputBlockOffset = -1;
  private java.util.function.IntSupplier position;

  public SnapshotBlock parseSnapshotBlock(DataInputStream dis, int blockLength) throws IOException {
    SnapshotBlock snapshotBlock = new SnapshotBlock();

    // Read flags
    int flags = reverseBytes(dis.readInt());
    // Check if the data is compressed or external
    snapshotBlock.setCompressed((flags & 0x02) != 0);
    snapshotBlock.setExternalData((flags & 0x01) != 0);

    // Read snapshot extension (ASCIIZ[4])
    byte[] extensionBytes = new byte[4];
    dis.readFully(extensionBytes);
    snapshotBlock.setSnapshotExtension(new String(extensionBytes).trim());

    // Read uncompressed snapshot length
    snapshotBlock.setUncompressedLength(reverseBytes(dis.readInt()));

    if (snapshotBlock.isExternalData()) {
      // External data (Snapshot descriptor)
      snapshotBlock.setSnapshotData(parseSnapshotDescriptor(dis));
    } else {
      int snapshotLength = blockLength - 17;
      if (snapshotBlock.isCompressed()) {
        // Compressed snapshot data
        snapshotBlock.setSnapshotData(parseCompressedData(dis, snapshotLength, snapshotBlock.getUncompressedLength()));
      } else {
        // Uncompressed snapshot data
        snapshotBlock.setSnapshotData(new byte[snapshotLength]);
        dis.readFully(snapshotBlock.getSnapshotData());
      }
    }

    return snapshotBlock;
  }

  private byte[] parseSnapshotDescriptor(DataInputStream dis) throws IOException {
    int checksum = dis.readInt(); // Read checksum
    ByteArrayOutputStream descriptorStream = new ByteArrayOutputStream();

    // Read ASCIIZ[N] (Snapshot filename)
    byte b;
    while ((b = dis.readByte()) != 0) {
      descriptorStream.write(b);
    }

    return descriptorStream.toByteArray();
  }

  private byte[] parseCompressedData(DataInputStream dis, int snapshotLength, int uncompressedLength) throws IOException {
    byte[] snapshotBytes = new byte[snapshotLength];
    dis.readFully(snapshotBytes);

    try (InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(snapshotBytes))) {
      ByteArrayOutputStream decompressedData = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int bytesRead;

      while ((bytesRead = inflater.read(buffer)) != -1) {
        decompressedData.write(buffer, 0, bytesRead);
      }

      byte[] byteArray = decompressedData.toByteArray();
      if (byteArray.length != uncompressedLength)
        throw new RuntimeException("error decompressing");
      return byteArray;
    }
  }

  public RzxHeader parseHeader(DataInputStream stream) throws IOException {
    RzxHeader header = new RzxHeader();

    // Read and validate the signature
    byte[] signatureBytes = new byte[4];
    stream.readFully(signatureBytes);
    header.signature = new String(signatureBytes, StandardCharsets.US_ASCII);
    if (!header.signature.equals("RZX!")) {
      throw new IOException("Invalid RZX signature: " + header.signature);
    }

    // Read revision numbers
    header.majorRevision = stream.readByte();
    header.minorRevision = stream.readByte();

    // Read flags
    header.flags = reverseBytes(stream.readInt());

    return header;
  }

  public CreatorInfo parseCreatorInfo(DataInputStream stream) throws IOException {
    CreatorInfo creatorInfo = new CreatorInfo();

    // Skip block ID and length
    stream.readByte();
    int blockLength = reverseBytes(stream.readInt());

    // Read creator's ID
    byte[] idBytes = new byte[20];
    stream.readFully(idBytes);
    creatorInfo.creatorId = new String(idBytes, StandardCharsets.US_ASCII).trim();

    // Read version numbers
    creatorInfo.majorVersion = reverseBytes(stream.readShort()) & 0xFFFF;
    creatorInfo.minorVersion = reverseBytes(stream.readShort()) & 0xFFFF;

    // Read custom data
    int customDataLength = blockLength - 29; // Subtract fixed-length fields
    if (customDataLength > 0) {
      creatorInfo.customData = new byte[customDataLength];
      stream.readFully(creatorInfo.customData);
    }

    return creatorInfo;
  }

  public void parseBlocks(DataInputStream stream) throws IOException {
    while (stream.available() > 0) {
      int blockStart = position == null ? -1 : position.getAsInt();
      byte blockID = stream.readByte();
      int blockLength = reverseBytes(stream.readInt());

      if (blockID == 0x30) {
        snapshotBlock = parseSnapshotBlock(stream, blockLength);
      } else if (blockID == (byte) 0x80) {
        if (inputBlockOffset < 0)
          inputBlockOffset = blockStart;
        parseInputRecordingBlock(stream, blockLength);
      } else {
        byte[] bytes = new byte[blockLength - 5];
        stream.readFully(bytes);
      }
    }
  }

  /**
   * VARIOS BLOQUES 0x80 SE ENCADENAN. La spec permite mas de un bloque de entrada y cada uno
   * continua al anterior, asi que los frames se ACUMULAN en un solo {@link InputRecordingBlock}
   * en vez de pisarse -- que es lo que pasaba antes, y dejaba ilegible cualquier archivo con
   * mas de un bloque (entre ellos los que escribe {@link RzxWriter.Mode#NEW_BLOCK}).
   *
   * <p>Y por eso los frames se leen ACOTADOS por la longitud del bloque y no hasta que se acabe
   * el stream: leer hasta el fin del archivo funciona solo si el bloque es el ultimo, y con dos
   * bloques el primero se comia al segundo.
   */
  private void parseInputRecordingBlock(DataInputStream stream, int blockLength) throws IOException {
    boolean first = inputRecordingBlock == null;
    if (first)
      inputRecordingBlock = new InputRecordingBlock();

    // Read number of frames
    int numberOfFrames = reverseBytes(stream.readInt());
    inputRecordingBlock.numberOfFrames = first ? numberOfFrames
        : inputRecordingBlock.numberOfFrames + numberOfFrames;

    // Reserved byte
    stream.readByte();

    // Read T-STATES counter
    long tStates = reverseBytes(stream.readInt());
    if (first)
      inputRecordingBlock.tStates = tStates;

    // Read flags
    int flags = reverseBytes(stream.readInt());
    boolean compressed = (flags & 0x02) != 0;
    if (first) {
      inputRecordingBlock.isProtected = (flags & 0x01) != 0;
      inputRecordingBlock.isCompressed = compressed;
    }

    // el bloque son 18 bytes de cabecera (id, longitud, frames, reservado, tStates, flags)
    byte[] payload = new byte[blockLength - 18];
    stream.readFully(payload);
    stream = new DataInputStream(compressed
        ? new InflaterInputStream(new ByteArrayInputStream(payload))
        : new ByteArrayInputStream(payload));
    InputRecordingBlock.Frame lastFrame = null;

    // Parse frames (simplified for now)
    while (stream.available() > 0) {
      InputRecordingBlock.Frame frame = new InputRecordingBlock.Frame();
      frame.fetchCounter = reverseBytes(stream.readShort()) & 0xFFFF;
      frame.inCounter = reverseBytes(stream.readShort()) & 0xFFFF;

      if (frame.inCounter < 10000) { // Not a repeated frame
        frame.returnValues = new byte[frame.inCounter];
        stream.readFully(frame.returnValues);
      } else {
        frame.inCounter = lastFrame.inCounter;
        frame.returnValues = Arrays.copyOf(lastFrame.returnValues, lastFrame.returnValues.length);
      }

      lastFrame = frame;
      inputRecordingBlock.frames.add(frame);
    }
  }

  // El snapshot embebido se PARSEA aca (SnapshotBlock) pero convertirlo en un
  // SpectrumState de jspeccy no: eso vive en RzxSnapshots, del lado de translator, que es
  // donde esta su unico consumidor. Era lo unico que ataba el lector de RZX al modulo sync.

  public RzxFile parseFile(String name) {
    try {
      // ENTERO EN MEMORIA y no por stream: el lector necesita saber en que byte empieza el
      // bloque de entrada para que el escritor pueda copiar todo lo anterior sin tocarlo. La
      // grabacion mas larga que tenemos son 167 KB.
      byte[] all = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(name));
      ByteArrayInputStream raw = new ByteArrayInputStream(all);
      position = () -> all.length - raw.available();
      try (DataInputStream stream = new DataInputStream(raw)) {
        RzxHeader header = parseHeader(stream);
        CreatorInfo creatorInfo = parseCreatorInfo(stream);
        parseBlocks(stream);

        RzxFile rzxFile = new RzxFile(header, creatorInfo, snapshotBlock, inputRecordingBlock);
        if (inputBlockOffset >= 0) {
          rzxFile.setPrefix(Arrays.copyOf(all, inputBlockOffset));
          rzxFile.setTail(Arrays.copyOfRange(all, inputBlockOffset, all.length));
        }
        return rzxFile;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
