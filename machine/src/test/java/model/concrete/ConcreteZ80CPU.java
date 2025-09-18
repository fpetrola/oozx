package model.concrete;

import model.interfaces.ISpectrumBus;
import model.interfaces.IZ80CPU;

// Concrete implementations for testing
public class ConcreteZ80CPU implements IZ80CPU {
    private int pc;
    private byte registerA;
    private int hl, bc, de, sp;
    private ISpectrumBus bus;
    private int tStates;
    private String model = "48K"; // Default model

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public void reset() {
        pc = 0;
        tStates = 0;
    }

    @Override
    public void executeCycle() {
        tStates += 4;
    }

    @Override
    public int getPC() {
        return pc;
    }

    @Override
    public void setPC(int pc) {
        this.pc = pc;
    }

    @Override
    public byte getRegisterA() {
        return registerA;
    }

    @Override
    public void setRegisterA(int value) {

    }

    public void setHL(int hl) {
        this.hl = hl;
    }

    public void setSP(int sp) {
        this.sp = sp;
    }

    @Override
    public void setDE(int i) {

    }

    @Override
    public byte in(int port) {
        int delay = bus.getULA().getIOContentionDelay(port, tStates % (model.equals("128K") ? 70908 : 69888), model);
        tStates += delay + 4;
        return bus.readPort(port);
    }

    @Override
    public void out(int port, byte value) {
        int delay = bus.getULA().getIOContentionDelay(port, tStates % (model.equals("128K") ? 70908 : 69888), model);
        tStates += delay + 4;
        bus.writePort(port, value);
    }

    @Override
    public byte readMemory(int address) {
        int delay = bus.getMemory().getContentionDelay(address, tStates % (model.equals("128K") ? 70908 : 69888), model);
        tStates += delay + 3;
        return bus.readMemory(address);
    }

    @Override
    public void writeMemory(int address, byte value) {
        int delay = bus.getMemory().getContentionDelay(address, tStates % (model.equals("128K") ? 70908 : 69888), model);
        tStates += delay + 3;
        bus.writeMemory(address, value);
    }

    @Override
    public int getTStates() {
        return tStates;
    }

    @Override
    public void addTStates(int tStates) {
        this.tStates += tStates;
    }

    @Override
    public void executeInstruction(String opcode, int[] operands) {
        int initialTStates = tStates;
        int contentionDelay;
        switch (opcode) {
            case "LD (HL),A":
                contentionDelay = bus.getMemory().getContentionDelay(hl, tStates, model);
                tStates += 4; // Fetch
                tStates += contentionDelay + 3; // Write
                bus.writeMemory(hl, registerA);
                pc += 1;
                break;
            case "INC (HL)":
                contentionDelay = bus.getMemory().getContentionDelay(hl, tStates, model);
                tStates += 4; // Fetch
                tStates += contentionDelay + 3; // Read
                tStates += 1; // Modify
                contentionDelay = bus.getMemory().getContentionDelay(hl, tStates, model);
                tStates += contentionDelay + 3; // Write
                bus.writeMemory(hl, (byte) (bus.readMemory(hl) + 1));
                pc += 1;
                break;
            case "LDI":
                contentionDelay = bus.getMemory().getContentionDelay(hl, tStates, model);
                tStates += 4; // Fetch
                tStates += contentionDelay + 3; // Read HL
                contentionDelay = bus.getMemory().getContentionDelay(de, tStates, model);
                tStates += contentionDelay + 3; // Write DE
                tStates += 2; // Extra
                bus.writeMemory(de, bus.readMemory(hl));
                hl++; de++; bc--;
                pc += 2;
                break;
            case "IN A,(n)":
                contentionDelay = bus.getMemory().getContentionDelay(pc + 1, tStates, model);
                tStates += 4; // Fetch
                tStates += contentionDelay + 3; // Read n
                tStates += bus.getULA().getIOContentionDelay(operands[0], tStates, model) + 4; // I/O
                registerA = bus.readPort(operands[0]);
                pc += 2;
                break;
            case "CALL nn":
                contentionDelay = bus.getMemory().getContentionDelay(pc + 1, tStates, model);
                tStates += 4; // Fetch
                tStates += contentionDelay + 3; // Read low
                contentionDelay = bus.getMemory().getContentionDelay(pc + 2, tStates, model);
                tStates += contentionDelay + 3; // Read high
                tStates += 1; // Extra
                contentionDelay = bus.getMemory().getContentionDelay(sp - 1, tStates, model);
                tStates += contentionDelay + 3; // Write high
                contentionDelay = bus.getMemory().getContentionDelay(sp - 2, tStates, model);
                tStates += contentionDelay + 3; // Write low
                bus.writeMemory(sp - 1, (byte) ((pc + 3) >> 8));
                bus.writeMemory(sp - 2, (byte) (pc + 3));
                sp -= 2;
                pc = operands[0];
                break;
            default:
                tStates += 4; // Simplified for unhandled opcodes
                pc += 1;
        }
    }

    public void connectToBus(ISpectrumBus bus) {
        this.bus = bus;
    }
}

