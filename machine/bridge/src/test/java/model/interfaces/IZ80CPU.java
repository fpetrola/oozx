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

