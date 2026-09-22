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
    /**
     * The format written here, which is the one the emulator itself writes: what it saves of a
     * machine to put it back later is a .z80, so a build without it could not keep its own
     * state. The rest - SNA, SZX, SP - arrive in a jar like everything else.
     */
    private static final java.util.List<SnapshotFile> WRITTEN_HERE =
            java.util.List.of(new SnapshotZ80());

    /**
     * Where the formats that arrived are asked for, told from outside.
     * <p>
     * This is under the emulator rather than over it, so it cannot go looking: whoever can look
     * says where to ask, the way the screen is told what ways of drawing there are. Asked each
     * time rather than kept, because a reader plugged in while the emulator runs is a reader
     * from that moment - keeping the answer is what made a snapshot need the emulator started
     * again before its reader counted.
     */
    private static volatile java.util.function.Supplier<java.util.List<SnapshotFile>> arrived =
        java.util.List::of;

    /** Said by whoever can go looking. Asked again whenever a file has to be read. */
    public static void alsoRead(java.util.function.Supplier<java.util.List<SnapshotFile>> formats) {
        arrived = formats == null ? java.util.List::of : formats;
    }


    /**
     * The readers on the path, found once. Plain ServiceLoader and not the plugin mechanism:
     * this is under it and cannot see it, and a reader sitting on the path is a reader this
     * build has whether or not anybody remembered to say so.
     */
    private static java.util.List<SnapshotFile> onThePath;

    private static synchronized java.util.List<SnapshotFile> onThePath() {
        if (onThePath == null) {
            java.util.List<SnapshotFile> found = new java.util.ArrayList<>();
            java.util.ServiceLoader.load(SnapshotFile.class).forEach(found::add);
            onThePath = found;
        }
        return onThePath;
    }

    /** Every format there is: the one written here, the ones on the path, and whatever arrived. */
    public static java.util.List<SnapshotFile> formats() {
        java.util.List<SnapshotFile> all = new java.util.ArrayList<>(WRITTEN_HERE);
        java.util.Set<Class<?>> already = new java.util.HashSet<>();
        WRITTEN_HERE.forEach(one -> already.add(one.getClass()));
        for (SnapshotFile one : onThePath()) {
            if (already.add(one.getClass())) {
                all.add(one);
            }
        }
        for (SnapshotFile one : arrived.get()) {
            if (already.add(one.getClass())) {
                all.add(one);
            }
        }
        return all;
    }

    /**
     * Which format reads that file, or null if none does.
     * <p>
     * Each one says whether the file is its own, rather than this knowing the extensions of all
     * of them: a format that arrives in a jar brings the answer with it.
     */
    public static SnapshotFile getSnapshot(File file) {
        for (SnapshotFile format : formats()) {
            if (format.reads(file)) {
                return format.fresh();
            }
        }
        return null;
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
