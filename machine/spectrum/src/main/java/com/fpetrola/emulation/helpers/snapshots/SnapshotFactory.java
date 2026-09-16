/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fpetrola.emulation.helpers.snapshots;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;

/**
 *
 * @author jsanchez
 */
public class SnapshotFactory {
    public static SnapshotFile getSnapshot(File file) {
        String name = file.getName().toLowerCase();
        switch (name.substring(name.lastIndexOf("."))) {
            case ".sna":
                return new SnapshotSNA();
            case ".z80":
                return new SnapshotZ80();
            case ".szx":
                return new SnapshotSZX();
            case ".sp":
                return new SnapshotSP();
            default:
                return null;
        }
    }

    /**
     * The game's own bytes inside a file: the RAM of a snapshot, and anything else as it came.
     * <p>
     * It matters for recognising a game across formats. A .z80 is compressed, so a snapshot and a
     * tape of the same game share almost nothing byte for byte - Manic Miner scored 0.22 against
     * its own tape - while the RAM the snapshot unpacks to holds the tape's data verbatim: 122 of
     * its 129 blocks of 256 bytes, and the score goes to 0.93. A tape needs nothing done to it,
     * which is why only this side of it is undone.
     */
    public static byte[] payloadOf(File file) throws IOException {
        SnapshotFile format = getSnapshot(file);
        if (format == null) {
            return Files.readAllBytes(file.toPath());
        }
        try {
            MemoryState memory = format.load(file).getMemoryState();
            ByteArrayOutputStream ram = new ByteArrayOutputStream();
            for (int page = 0; page < 8; page++) {
                if (memory.getPageRam(page) != null) {
                    ram.write(memory.getPageRam(page));
                }
            }
            return ram.toByteArray();
        } catch (SnapshotException | RuntimeException unreadable) {
            // A snapshot that cannot be opened is still a file somebody has, and its bytes are a
            // worse fingerprint than its RAM but a better one than nothing.
            return Files.readAllBytes(file.toPath());
        }
    }
}
