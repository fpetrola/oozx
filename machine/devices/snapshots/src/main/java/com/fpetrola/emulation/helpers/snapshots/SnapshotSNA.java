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

import com.fpetrola.emulation.helpers.machine.Keyboard.JoystickModel;
import com.fpetrola.emulation.helpers.machine.MachineTypes;
import z80core.IntMode;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jsanchez
 */
public class SnapshotSNA implements SnapshotFile {

    @Override
    public boolean reads(File file) {
        return SnapshotFile.named(file, ".sna");
    }

    @Override
    public String label() {
        return "SNA snapshot";
    }

    private BufferedInputStream fIn;
    private BufferedOutputStream fOut;
    private SpectrumState spectrum;
    private Z80State z80;
    private MemoryState memory;
    private AY8912State ay8912;
    
    @Override
    public SpectrumState load(File filename) throws SnapshotException {
        spectrum = new SpectrumState();
        
        try {
            try {
                fIn = new BufferedInputStream(new FileInputStream(filename));
            } catch (FileNotFoundException ex) {
                throw new SnapshotException("OPEN_FILE_ERROR", ex);
            }

            int snaLen = fIn.available();
            switch (snaLen) {
                case 49179:
                    spectrum.setSpectrumModel(MachineTypes.SPECTRUM48K);
                    break;
                case 131103:
                case 147487: // 128k with one RAM page saved twice
                    spectrum.setSpectrumModel(MachineTypes.SPECTRUM128K);
                    break;
                default:
                    throw new SnapshotException("FILE_SIZE_ERROR");
            }

            byte[] snaHeader = new byte[27];
            int count = 0;
            while (count != -1 && count < snaHeader.length) {
                count += fIn.read(snaHeader, count, snaHeader.length - count);
            }

            if (count != snaHeader.length) {
                throw new SnapshotException("FILE_READ_ERROR");
            }

            z80 = new Z80State();
            spectrum.setZ80State(z80);
            
            z80.setRegI(snaHeader[0]);
            z80.setRegLx(snaHeader[1]);
            z80.setRegHx(snaHeader[2]);
            z80.setRegEx(snaHeader[3]);
            z80.setRegDx(snaHeader[4]);
            z80.setRegCx(snaHeader[5]);
            z80.setRegBx(snaHeader[6]);
            z80.setRegFx(snaHeader[7]);
            z80.setRegAx(snaHeader[8]);
            z80.setRegL(snaHeader[9]);
            z80.setRegH(snaHeader[10]);
            z80.setRegE(snaHeader[11]);
            z80.setRegD(snaHeader[12]);
            z80.setRegC(snaHeader[13]);
            z80.setRegB(snaHeader[14]);
            z80.setRegIY((snaHeader[15] & 0xff) | (snaHeader[16] << 8));
            z80.setRegIX((snaHeader[17] & 0xff) | (snaHeader[18] << 8));

            // Bit 2 of this byte is IFF2, the SNA format's saved interrupt-enable flag.
            boolean intEnabled = (snaHeader[19] & 0x04) != 0;
            z80.setIFF1(intEnabled);
            z80.setIFF2(intEnabled);

            z80.setRegR(snaHeader[20]);
            z80.setRegF(snaHeader[21]);
            z80.setRegA(snaHeader[22]);
            z80.setRegSP((snaHeader[23] & 0xff) | (snaHeader[24] << 8));
            switch (snaHeader[25] & 0x03) {
                case 0:
                    z80.setIM(IntMode.IM0);
                    break;
                case 1:
                    z80.setIM(IntMode.IM1);
                    break;
                case 2:
                    z80.setIM(IntMode.IM2);
                    break;
            }
            
            spectrum.setBorder(snaHeader[26]);

            memory = new MemoryState();
            spectrum.setMemoryState(memory);
            
            byte[] buffer = new byte[0x4000];

            count = 0;
            while (count != -1 && count < 0x4000) {
                count += fIn.read(buffer, count, 0x4000 - count);
            }

            if (count != 0x4000) {
                throw new SnapshotException("FILE_READ_ERROR");
            }
            memory.setPageRam(5, buffer);

            buffer = new byte[0x4000];
            count = 0;
            while (count != -1 && count < 0x4000) {
                count += fIn.read(buffer, count, 0x4000 - count);
            }

            if (count != 0x4000) {
                throw new SnapshotException("FILE_READ_ERROR");
            }
            memory.setPageRam(2, buffer);

            if (snaLen == 49179) {
                // A 48K .sna has no PC field: it is popped off the stack on start, so SP must
                // point into RAM with a word to pop, not into ROM or off the top of memory.
                int sp = z80.getRegSP();
                if (sp < 0x4000 || sp == 0xffff) {
                    throw new SnapshotException("FILE_READ_ERROR");
                }

                buffer = new byte[0x4000];
                count = 0;
                while (count != -1 && count < 0x4000) {
                    count += fIn.read(buffer, count, 0x4000 - count);
                }

                if (count != 0x4000) {
                    throw new SnapshotException("FILE_READ_ERROR");
                }
                memory.setPageRam(0, buffer);

                z80.setRegPC(0x72); // ROM address of the RETN that starts the program
                spectrum.setEnabledAY(false);
            } else {
                boolean[] loaded = new boolean[8];

                // The page this last 16K belongs to is only known once port 0x7ffd is read
                // below, so it is kept in a temporary buffer until then.
                buffer = new byte[0x4000];
                count = 0;
                while (count != -1 && count < 0x4000) {
                    count += fIn.read(buffer, count, 0x4000 - count);
                }

                if (count != 0x4000) {
                    throw new SnapshotException("FILE_READ_ERROR");
                }

                loaded[2] = loaded[5] = true;
                z80.setRegPC(fIn.read() | (fIn.read() << 8));
                spectrum.setPort7ffd(fIn.read());
                // If page 2 or 5 was also mapped at 0xC000, the buffer just read is a
                // redundant second copy of a page already loaded and must be discarded.
                int page = spectrum.getPort7ffd() & 0x07;
                if (page != 2 && page != 5) {
                    memory.setPageRam(page, buffer);
                    loaded[page] = true;
                }

                int trDos = fIn.read(); // 1 means the TR-DOS ROM was paged in, unsupported here
                if (trDos == 0x01) {
                    throw new SnapshotException("NOT_SNAPSHOT_FILE");
                }

                for (page = 0; page < 8; page++) {
                    if (!loaded[page]) {
                        buffer = new byte[0x4000];
                        count = 0;
                        while (count != -1 && count < 0x4000) {
                            count += fIn.read(buffer, count, 0x4000 - count);
                        }
                        if (count != 0x4000) {
                            throw new SnapshotException("FILE_READ_ERROR");
                        }
                        memory.setPageRam(page, buffer);
                    }
                    // SNA carries no AY register state; a zeroed register set is used instead.
                    spectrum.setEnabledAY(true);
                    spectrum.setEnabledAYon48k(false);
                    int[] regAY = new int[16];
                    ay8912 = new AY8912State();
                    spectrum.setAY8912State(ay8912);
                    ay8912.setAddressLatch(0);
                    ay8912.setRegAY(regAY);
                }

            }

            // Neither issue-2 nor joystick model is stored in a SNA file.
            spectrum.setIssue2(false);
            spectrum.setJoystick(JoystickModel.NONE);
            spectrum.setTstates(0);
        } catch (IOException ex) {
            throw new SnapshotException("FILE_READ_ERROR", ex);
        } finally {
            try {
                if (fIn != null)
                    fIn.close();
            } catch (IOException ex) {
                throw new SnapshotException("FILE_READ_ERROR", ex);
            }
        }

        return spectrum;
    }

    @Override
    public boolean save(File filename, SpectrumState state) throws SnapshotException {
        spectrum = state;
        z80 = spectrum.getZ80State();
        memory = spectrum.getMemoryState();

        // SP must have room below it to push the PC that SNA saves on the stack instead of a field.
        if (spectrum.getSpectrumModel().codeModel == MachineTypes.CodeModel.SPECTRUM48K
                && z80.getRegSP() < 0x4002) {
            throw new SnapshotException("SNA_REGSP_ERROR");
        }

        // SNA supports the 16/48K and 128K models only, not the +2/+3 family.
        if (spectrum.getSpectrumModel().codeModel != MachineTypes.CodeModel.SPECTRUM48K &&
                spectrum.getSpectrumModel() != MachineTypes.SPECTRUM128K) {
            throw new SnapshotException("SNA_DONT_SUPPORT_PLUS3");
        }

        try {
            try {
                fOut = new BufferedOutputStream(new FileOutputStream(filename));
            } catch (FileNotFoundException ex) {
                throw new SnapshotException("OPEN_FILE_ERROR", ex);
            }

            byte[] snaHeader = new byte[27];
            snaHeader[0] = (byte) z80.getRegI();
            snaHeader[1] = (byte) z80.getRegLx();
            snaHeader[2] = (byte) z80.getRegHx();
            snaHeader[3] = (byte) z80.getRegEx();
            snaHeader[4] = (byte) z80.getRegDx();
            snaHeader[5] = (byte) z80.getRegCx();
            snaHeader[6] = (byte) z80.getRegBx();
            snaHeader[7] = (byte) z80.getRegFx();
            snaHeader[8] = (byte) z80.getRegAx();
            snaHeader[9] = (byte) z80.getRegL();
            snaHeader[10] = (byte) z80.getRegH();
            snaHeader[11] = (byte) z80.getRegE();
            snaHeader[12] = (byte) z80.getRegD();
            snaHeader[13] = (byte) z80.getRegC();
            snaHeader[14] = (byte) z80.getRegB();
            snaHeader[15] = (byte) z80.getRegIY();
            snaHeader[16] = (byte) (z80.getRegIY() >>> 8);
            snaHeader[17] = (byte) z80.getRegIX();
            snaHeader[18] = (byte) (z80.getRegIX() >>> 8);
            
            snaHeader[19] = (byte) (z80.isIFF2() ?  0x04 : 0x00);

            snaHeader[20] = (byte) z80.getRegR();
            snaHeader[21] = (byte) z80.getRegF();
            snaHeader[22] = (byte) z80.getRegA();

            int regSP = z80.getRegSP();
            if (spectrum.getSpectrumModel().codeModel == MachineTypes.CodeModel.SPECTRUM48K) {
                regSP = (regSP - 2) & 0xffff;
            }

            snaHeader[23] = (byte) regSP;
            snaHeader[24] = (byte) (regSP >>> 8);
            snaHeader[25] = (byte) z80.getIM().ordinal();
            snaHeader[26] = (byte) spectrum.getBorder();

            fOut.write(snaHeader, 0, snaHeader.length);

            byte[] buffer;
            if (spectrum.getSpectrumModel().codeModel == MachineTypes.CodeModel.SPECTRUM48K) {
                buffer = new byte[0xC000];
                System.arraycopy(memory.getPageRam(5), 0, buffer, 0, 0x4000);
                System.arraycopy(memory.getPageRam(2), 0, buffer, 0x4000, 0x4000);
                System.arraycopy(memory.getPageRam(0), 0, buffer, 0x8000, 0x4000);
                
                regSP -= 0x4000;
                buffer[regSP] = (byte) z80.getRegPC();
                regSP = (regSP + 1) & 0xffff;
                buffer[regSP] = (byte) (z80.getRegPC() >>> 8);
                fOut.write(buffer, 0, buffer.length);
            }

            if (spectrum.getSpectrumModel() == MachineTypes.SPECTRUM128K) {
                buffer = memory.getPageRam(5);
                fOut.write(buffer, 0, buffer.length);
                buffer = memory.getPageRam(2);
                fOut.write(buffer, 0, buffer.length);
                buffer = memory.getPageRam((spectrum.getPort7ffd() & 0x07));
                fOut.write(buffer, 0, buffer.length);

                boolean[] saved = new boolean[8];
                saved[2] = saved[5] = true;
                fOut.write(z80.getRegPC());
                fOut.write(z80.getRegPC() >>> 8);
                fOut.write(spectrum.getPort7ffd());
                fOut.write(0x00); // TR-DOS ROM never paged in
                saved[spectrum.getPort7ffd() & 0x07] = true;
                for (int page = 0; page < 8; page++) {
                    if (!saved[page]) {
                        buffer = memory.getPageRam(page);
                        fOut.write(buffer, 0, buffer.length);
                    }
                }
            }

        } catch (IOException ex) {
            throw new SnapshotException("FILE_WRITE_ERROR", ex);
        } finally {
            try {
                if (fOut != null)
                    fOut.close();
            } catch (IOException ex) {
                Logger.getLogger(SnapshotSNA.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return true;
    }

}
