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

import com.fpetrola.oozx.plugins.Plugin;

import java.io.File;

/**
 * One snapshot format: what a machine written to a file looks like, read back and written.
 * <p>
 * A way in, so a format this build never heard of can arrive in a jar and be read like the rest.
 * Which files it reads is its own to say, because that is the part that differs: most go by the
 * name, and one that shares an extension with something else has to look inside.
 *
 * @author jsanchez
 */
@Plugin("format")
@dev.crystal.plugins.api.RoleInterface
public interface SnapshotFile {

    /** Whether this is the format of that file. */
    boolean reads(File file);

    SpectrumState load(File filename) throws SnapshotException;

    boolean save(File filename, SpectrumState state) throws SnapshotException;

    /**
     * One of these to read a file with.
     * <p>
     * These read into themselves - what a snapshot holds ends up in the reader's own fields while
     * it goes - so two files read by the same one leave each other's leavings behind, which reads
     * as a file that cannot be read at all. What is found is the format; what reads a file is one
     * of its own.
     */
    default SnapshotFile fresh() {
        try {
            return getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException cannotBeMadeAgain) {
            return this;
        }
    }

    /** What the format is called, for saying which one a file is. */
    default String label() {
        return getClass().getSimpleName();
    }

    /** Whether the name ends in one of these, which is how nearly every format is told apart. */
    static boolean named(File file, String... extensions) {
        String name = file.getName().toLowerCase();
        for (String extension : extensions) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
