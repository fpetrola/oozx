/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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
}
