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

import snapshots.*;

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
      byte blockID = stream.readByte();
      int blockLength = reverseBytes(stream.readInt());

      if (blockID == 0x30) {
        snapshotBlock = parseSnapshotBlock(stream, blockLength);
      } else if (blockID == (byte) 0x80) {
        parseInputRecordingBlock(stream);
      } else {
        byte[] bytes = new byte[blockLength - 5];
        stream.readFully(bytes);
      }
    }
  }

  private void parseInputRecordingBlock(DataInputStream stream) throws IOException {
    inputRecordingBlock = new InputRecordingBlock();

    // Read number of frames
    inputRecordingBlock.numberOfFrames = reverseBytes(stream.readInt());

    // Reserved byte
    stream.readByte();

    // Read T-STATES counter
    inputRecordingBlock.tStates = reverseBytes(stream.readInt());

    // Read flags
    int flags = reverseBytes(stream.readInt());
    inputRecordingBlock.isProtected = (flags & 0x01) != 0;
    inputRecordingBlock.isCompressed = (flags & 0x02) != 0;

    if (inputRecordingBlock.isCompressed) {
      stream = new DataInputStream(new InflaterInputStream(stream));
    }
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

  public static void main(String[] args) {
    String name;
    name = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/eawally/eawally.rzx";
    name = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/dynamitedan/dynamitedan.rzx";

    RzxFile rzxFile = new RzxParser().parseFile(name);
    loadSnapshot(rzxFile);
  }

  public static SpectrumState loadSnapshot(RzxFile rzxFile) {
    SnapshotZ80 snap = new SnapshotZ80();
    SnapshotSZX snap2 = new SnapshotSZX();


    try {
      File tempFile = File.createTempFile("snapshot", "zx", null);
      FileOutputStream fos = new FileOutputStream(tempFile);
      byte[] snapshotData = rzxFile.getSnapshotBlock().getSnapshotData();
      fos.write(snapshotData);
      return snap.load(tempFile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public RzxFile parseFile(String name) {
    try (FileInputStream fis = new FileInputStream(name);
         DataInputStream stream = new DataInputStream(new BufferedInputStream(fis))) {

      RzxHeader header = parseHeader(stream);
      CreatorInfo creatorInfo = parseCreatorInfo(stream);
      parseBlocks(stream);

      RzxFile rzxFile = new RzxFile(header, creatorInfo, snapshotBlock, inputRecordingBlock);
      return rzxFile;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
