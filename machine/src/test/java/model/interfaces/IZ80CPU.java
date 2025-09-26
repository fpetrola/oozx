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
}

