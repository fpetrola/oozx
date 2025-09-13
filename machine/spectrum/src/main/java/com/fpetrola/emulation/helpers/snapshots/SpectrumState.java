/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fpetrola.emulation.helpers.snapshots;

import com.fpetrola.emulation.helpers.machine.Keyboard.JoystickModel;
import com.fpetrola.emulation.helpers.machine.MachineTypes;

/**
 *
 * @author jsanchez
 */ 
public class SpectrumState {
    private MachineTypes spectrumModel;
    private Z80State z80;
    private MemoryState memory;
    private AY8912State ay8912;
    private int tstates, portFE, earBit, port7ffd, port1ffd, portFD;
    private byte numMicrodrives = 1;
    private boolean ULAPlusEnabled, ULAPlusActive, issue2, multiface, connectedLec;
    private boolean connectedIF1, enabledAY, enabledAYon48k;
    private JoystickModel joystick;
    private int[] ULAPlusPalette;
    private int paletteGroup;
    
    public SpectrumState () {
    }
    
    public MachineTypes getSpectrumModel() {
        return spectrumModel;
    }

    public void setSpectrumModel(MachineTypes spectrumModel) {
        this.spectrumModel = spectrumModel;
    }

    public Z80State getZ80State() {
        return z80;
    }

    public void setZ80State(Z80State z80) {
        this.z80 = z80;
    }

    public MemoryState getMemoryState() {
        return memory;
    }

    public void setMemoryState(MemoryState memory) {
        this.memory = memory;
    }

    public AY8912State getAY8912State() {
        return ay8912;
    }

    public void setAY8912State(AY8912State ay8912) {
        this.ay8912 = ay8912;
    }

    public int getTstates() {
        return tstates;
    }

    public void setTstates(int tstates) {
        this.tstates = tstates;
    }

    public int getEarBit() {
        return earBit;
    }

    public void setEarBit(int earBit) {
        this.earBit = earBit & 0xff;
    }

    public int getPort7ffd() {
        return port7ffd;
    }

    public void setPort7ffd(int port7ffd) {
        this.port7ffd = port7ffd & 0xff;
    }

    public int getPort1ffd() {
        return port1ffd;
    }

    public void setPort1ffd(int port1ffd) {
        this.port1ffd = port1ffd & 0xff;
    }

    public boolean isULAPlusEnabled() {
        return ULAPlusEnabled;
    }

    public void setULAPlusEnabled(boolean ULAplusOn) {
        this.ULAPlusEnabled = ULAplusOn;
    }

    public boolean isULAPlusActive() {
        return ULAPlusActive;
    }

    public void setULAPlusActive(boolean ULAPlusActive) {
        this.ULAPlusActive = ULAPlusActive;
    }

    public int getPaletteGroup() {
        return paletteGroup;
    }

    public void setPaletteGroup(int paletteGroup) {
        this.paletteGroup = paletteGroup & 0xff;
    }

    public int[] getULAPlusPalette() {
        return ULAPlusPalette;
    }

    public void setULAPlusPalette(int[] UlaPlusPalette) {
        this.ULAPlusPalette = UlaPlusPalette;
    }

    public boolean isIssue2() {
        return issue2;
    }

    public void setIssue2(boolean issue2) {
        this.issue2 = issue2;
    }

    public boolean isMultiface() {
        return multiface;
    }

    public void setMultiface(boolean multiface) {
        this.multiface = multiface;
    }

    public boolean isConnectedIF1() {
        return connectedIF1;
    }

    public void setConnectedIF1(boolean connectedIF1) {
        this.connectedIF1 = connectedIF1;
    }

    public byte getNumMicrodrives() {
        return numMicrodrives;
    }

    public void setNumMicrodrives(byte numMicrodrives) {
        if (numMicrodrives < 1 || numMicrodrives > 8)
            numMicrodrives = 8;
        
        this.numMicrodrives = numMicrodrives;
    }

    public JoystickModel getJoystick() {
        return joystick;
    }

    public void setJoystick(JoystickModel joystick) {
        this.joystick = joystick;
    }

    public int getPortFE() {
        return portFE;
    }

    public void setPortFE(int portFE) {
        this.portFE = portFE;
    }
    
    public int getBorder() {
        return portFE & 0x07;
    }

    public void setBorder(int color) {
        portFE &= 0xF8;
        this.portFE |= color;
    }

    public boolean isEnabledAY() {
        return enabledAY;
    }

    public void setEnabledAY(boolean ayEnabled) {
        this.enabledAY = ayEnabled;
    }

    public boolean isEnabledAYon48k() {
        return enabledAYon48k;
    }

    public void setEnabledAYon48k(boolean enabledAYon48k) {
        this.enabledAYon48k = enabledAYon48k;
    }

    public boolean isConnectedLec() {
        return connectedLec;
    }

    public void setConnectedLec(boolean connectedLec) {
        this.connectedLec = connectedLec;
    }

    public int getPortFD() {
        return portFD;
    }

    public void setPortFD(int portFD) {
        this.portFD = portFD;
    }
}
