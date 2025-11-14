/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package com.fpetrola.oozx;

public class Disk {
    public static final int DISK_FLAG_PLUS3_CPC = 1;
    static final int DISK_HD = 1; // Placeholder for density
    public int flag;
    int sides;         // Number of sides (1 or 2)
    int cylinders;     // Number of cylinders
    int wrprot;        // Write protect flag
    int haveWeak;      // Weak sectors flag
    int density;       // Disk density
    byte[] track;      // Track data
    Byte clocks;     // Clock bits
    Byte fm;         // FM bits
    Byte weak;       // Weak bits
    int i;             // Current index in track
    int dirty;         // Dirty flag
    byte[] data;       // Disk data

    public static void setTrack(Disk disk, int head, int cylinder) {
        // Placeholder for setting track
        disk.track = disk.data != null ? disk.data : new byte[0];
    }

    public static void preformat(Disk disk) {
        // Placeholder
    }
}