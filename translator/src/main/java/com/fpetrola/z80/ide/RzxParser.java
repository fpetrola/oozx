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

package com.fpetrola.z80.ide;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RzxParser {

    private static final int HEADER_SIGNATURE = 0x21585A52; // "RZX!" in little-endian

    public static class RzxHeader {
        public String signature;
        public byte majorRevision;
        public byte minorRevision;
        public int flags;
    }

    public static class CreatorInfo {
        public String creatorId;
        public int majorVersion;
        public int minorVersion;
        public byte[] customData;
    }

    public static class InputRecordingBlock {
        public int numberOfFrames;
        public long tStates;
        public boolean isProtected;
        public boolean isCompressed;
        public List<Frame> frames = new ArrayList<>();

        public static class Frame {
            public int fetchCounter;
            public int inCounter;
            public byte[] returnValues;
        }
    }

    public static RzxHeader parseHeader(DataInputStream stream) throws IOException {
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
        header.flags = Integer.reverseBytes(stream.readInt());

        return header;
    }

    public static CreatorInfo parseCreatorInfo(DataInputStream stream) throws IOException {
        CreatorInfo creatorInfo = new CreatorInfo();

        // Skip block ID and length
        stream.readByte();
        int blockLength = Integer.reverseBytes(stream.readInt());

        // Read creator's ID
        byte[] idBytes = new byte[20];
        stream.readFully(idBytes);
        creatorInfo.creatorId = new String(idBytes, StandardCharsets.US_ASCII).trim();

        // Read version numbers
        creatorInfo.majorVersion = Short.reverseBytes(stream.readShort()) & 0xFFFF;
        creatorInfo.minorVersion = Short.reverseBytes(stream.readShort()) & 0xFFFF;

        // Read custom data
        int customDataLength = blockLength - 29; // Subtract fixed-length fields
        if (customDataLength > 0) {
            creatorInfo.customData = new byte[customDataLength];
            stream.readFully(creatorInfo.customData);
        }

        return creatorInfo;
    }

    public static InputRecordingBlock parseInputRecordingBlock(DataInputStream stream) throws IOException {
        InputRecordingBlock block = new InputRecordingBlock();

        // Skip block ID and length
        stream.readByte();
        int blockLength = Integer.reverseBytes(stream.readInt());

        // Read number of frames
        block.numberOfFrames = Integer.reverseBytes(stream.readInt());

        // Reserved byte
        stream.readByte();

        // Read T-STATES counter
        block.tStates = Integer.reverseBytes(stream.readInt());

        // Read flags
        int flags = Integer.reverseBytes(stream.readInt());
        block.isProtected = (flags & 0x01) != 0;
        block.isCompressed = (flags & 0x02) != 0;

        // Parse frames (simplified for now)
        while (stream.available() > 0) {
            InputRecordingBlock.Frame frame = new InputRecordingBlock.Frame();
            frame.fetchCounter = Short.reverseBytes(stream.readShort()) & 0xFFFF;
            frame.inCounter = Short.reverseBytes(stream.readShort()) & 0xFFFF;

            if (frame.inCounter != 65535) { // Not a repeated frame
                frame.returnValues = new byte[frame.inCounter];
                stream.readFully(frame.returnValues);
            }

            block.frames.add(frame);
        }

        return block;
    }

    public static void main(String[] args) {
        try (FileInputStream fis = new FileInputStream("/home/fernando/detodo/desarrollo/m/zx/roms/recordings/dynamitedan/dynamitedan.rzx");
             DataInputStream stream = new DataInputStream(new BufferedInputStream(fis))) {

            // Parse header
            RzxHeader header = parseHeader(stream);
            System.out.println("Header: " + header.signature + " v" + header.majorRevision + "." + header.minorRevision);

            // Parse creator info
            CreatorInfo creatorInfo = parseCreatorInfo(stream);
            System.out.println("Creator: " + creatorInfo.creatorId + " v" + creatorInfo.majorVersion + "." + creatorInfo.minorVersion);

            // Parse input recording block
            InputRecordingBlock inputBlock = parseInputRecordingBlock(stream);
            System.out.println("Number of Frames: " + inputBlock.numberOfFrames);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
