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
package com.fpetrola.emulation.helpers.machine;

/**
 * 
 * @author jsanchez
 */ 
public enum MachineTypes { 

    SPECTRUM16K("ZX Spectrum 16K", CodeModel.SPECTRUM48K, 8, 69888),
    SPECTRUM48K("ZX Spectrum 48K", CodeModel.SPECTRUM48K, 8, 69888),
    SPECTRUM128K("ZX Spectrum 128K", CodeModel.SPECTRUM128K, 8, 70908),
    SPECTRUMPLUS2("Amstrad ZX Spectrum +2", CodeModel.SPECTRUM128K, 8, 70908),
    SPECTRUMPLUS2A("ZX Spectrum +2A", CodeModel.SPECTRUMPLUS3, 8, 70908),
    SPECTRUMPLUS3("ZX Spectrum +3", CodeModel.SPECTRUMPLUS3, 8, 70908),
    /*
     * The rest of the machines a snapshot can name, as libspectrum numbers them: what a format needs
     * of each is which RAM it has and how long its frame is. The rest of their timings are the
     * machines' own.
     */
    SPECTRUMPLUS3E("ZX Spectrum +3e", CodeModel.SPECTRUMPLUS3, 8, 70908),
    PENTAGON128("Pentagon 128", CodeModel.SPECTRUM128K, 8, 71680),
    TC2048("Timex TC2048", CodeModel.SPECTRUM48K, 3, 69888),
    TC2068("Timex TC2068", CodeModel.SPECTRUM48K, 3, 69888),
    SCORPION("Scorpion ZS-256", CodeModel.SPECTRUMPLUS3, 16, 69888),
    SPECTRUMSE("ZX Spectrum SE", CodeModel.SPECTRUM128K, 9, 69888),
    TS2068("Timex TS2068", CodeModel.SPECTRUM48K, 3, 58688),
    PENTAGON512("Pentagon 512", CodeModel.SPECTRUM128K, 32, 71680),
    PENTAGON1024("Pentagon 1024", CodeModel.SPECTRUM128K, 64, 71680),
    SPECTRUM48KNTSC("ZX Spectrum 48K (NTSC)", CodeModel.SPECTRUM48K, 3, 59136),
    SPECTRUM128KE("ZX Spectrum 128Ke", CodeModel.SPECTRUM128K, 8, 70908);

    /** Which of the three the code of a snapshot was written for, and the model that is that one itself. */
    public enum CodeModel {
        SPECTRUM48K { public MachineTypes base() { return MachineTypes.SPECTRUM48K; } },
        SPECTRUM128K { public MachineTypes base() { return MachineTypes.SPECTRUM128K; } },
        SPECTRUMPLUS3 { public MachineTypes base() { return MachineTypes.SPECTRUMPLUS3; } };

        public abstract MachineTypes base();
    }

  public CodeModel codeModel;
    private String longModelName;
    public int tstatesFrame;
    private int ramBanks = 8;

    MachineTypes(String longModelName, CodeModel codeModel, int ramBanks, int tstatesFrame) {
        this.longModelName = longModelName;
        this.codeModel = codeModel;
        this.ramBanks = ramBanks;
        this.tstatesFrame = tstatesFrame;
    }

    /** The banks of 16K of RAM a paged one has; one without paging has those of a 48K. */
    public int ramBanks() {
        return ramBanks;
    }

    public String getLongModelName() {
        return longModelName;
    }

}
