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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fpetrola.emulation.helpers.snapshots;

/**
 *
 * @author jsanchez
 */
public class MemoryState {
    private byte[][] ram = new byte[8][];
    private byte[] IF2Rom;
    private byte[] mfRam;
    private final byte[][] lecRam = new byte[16][];
    private int portFD;
    private boolean IF1RomPaged, IF2RomPaged;
    private boolean multifacePaged, multifaceLocked, mf128on48k;
    
    public MemoryState () {
    }
    
    public byte[] getPageRam(int page) {
        return getRam()[page];
    }
    
    public void setPageRam(int page, byte[] memory) {
        getRam()[page] = memory;
    }
    
    public byte[] getIF2Rom() {
        return IF2Rom;
    }
    
    public void setIF2Rom(byte[] memory) {
        IF2Rom = memory;
    }
    
    public byte[] getMultifaceRam() {
        return mfRam;
    }
    
    public void setMultifaceRam(byte[] memory) {
        mfRam = memory;
    }

    public boolean isIF2RomPaged() {
        return IF2RomPaged;
    }

    public void setIF2RomPaged(boolean IF2RomPaged) {
        this.IF2RomPaged = IF2RomPaged;
    }

    public boolean isMultifacePaged() {
        return multifacePaged;
    }

    public void setMultifacePaged(boolean multifacePaged) {
        this.multifacePaged = multifacePaged;
    }

    public boolean isMultifaceLocked() {
        return multifaceLocked;
    }

    public void setMultifaceLocked(boolean multifaceLocked) {
        this.multifaceLocked = multifaceLocked;
    }

    public boolean isMf128on48k() {
        return mf128on48k;
    }

    public void setMf128on48k(boolean mf128on48k) {
        this.mf128on48k = mf128on48k;
    }

    public boolean isIF1RomPaged() {
        return IF1RomPaged;
    }

    public void setIF1RomPaged(boolean IF1RomPaged) {
        this.IF1RomPaged = IF1RomPaged;
    }
    
    // A page never loaded from the snapshot reads as zero, matching untouched RAM on real hardware.
    public byte readByte(int page, int address) {
        byte[] held = getRam()[page];
        return held == null ? 0 : held[address];
    }

    public int getPortFD() {
        return portFD;
    }

    public void setPortFD(int pageLEC) {
        this.portFD = pageLEC;
    }

    public boolean isLecPaged() {
        return (portFD & 0x80) != 0;
    }
    
    public byte[] getLecPageRam(int page) {
        if (lecRam[page] == null) {
            return null;
        }
        
        return lecRam[page];
    }
    
    public void setLecPageRam(int page, byte[] ram) {
        lecRam[page] = ram;
    }

    public byte[][] getRam() {
      return ram;
    }

    public void setRam(byte[][] ram) {
      this.ram = ram;
    }
}
