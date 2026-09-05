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

package model.interfaces;

// Core Components
public interface IZ80CPU {
    void reset();
    void executeCycle();
    int getPC();
    void setPC(int pc);
    byte getRegisterA();
    void setRegisterA(int value);
    byte in(int port);
    void out(int port, byte value);
    byte readMemory(int address);
    void writeMemory(int address, byte value, boolean contended);
    int getTStates();
    void setTStates(int tStates);
    void addTStates2(int tStates);
    void executeInstruction(String opcode, int... operands); // Simulate specific instructions

    void setHL(int i);

    void setSP(int i);

    void setDE(int i);

    void setBC(int i);

    int getHL();

    int getDE();

    void setIR(int i);

    void setB(int b);

    int getB();

    void setZeroFlag(boolean b);

    int getBC();

    void step();

    void setIX(int value);

    boolean isZeroFlag();

    boolean getInterruptEnable();

    int getSP();
}

