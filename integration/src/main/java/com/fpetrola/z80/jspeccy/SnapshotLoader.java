package com.fpetrola.z80.jspeccy;

import com.fpetrola.z80.mmu.Memory;
import com.fpetrola.z80.mmu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import snapshots.*;

import java.io.File;

public class SnapshotLoader {
  public static <T extends WordNumber> byte[] setupStateWithSnapshot(RegistersSetter registersSetter, String fileName, MemorySetter memorySetter) {
    WordNumber[] data = new WordNumber[0x10000];

    try {
      File file = new File(fileName);

      SnapshotFile snap = new SnapshotZ80();
      SpectrumState snapState = snap.load(file);

      setZ80State(registersSetter, snapState.getZ80State());
//      registersBase.setZ80State(snapState.getZ80State());

      MemoryState memoryState = snapState.getMemoryState();

      byte[][] ram = memoryState.getRam();
      byte[] result = new byte[0x10000];

      int position = 16384;
      position = copyPage(ram, 5, position, result);
      position = copyPage(ram, 2, position, result);
      copyPage(ram, 0, position, result);

      memorySetter.setData(result);
      return result;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static <T extends WordNumber> int copyPage(byte[][] ram, int page, int position, byte[] result) {
    if (ram[page] != null)
      for (int i = 0; i < ram[page].length; i++) {
        result[position++] = ram[page][i];
      }
    return position;
  }

  private static <T extends WordNumber> int copyPage2(byte[][] ram, int page, int position, State<T> state1) {
    Memory<T> memory = state1.getMemory();
    if (ram[page] != null)
      for (int i = 0; i < ram[page].length; i++) {
        memory.write(WordNumber.createValue(position++), WordNumber.createValue(ram[page][i]));
      }
    return position;
  }

  public static <T extends WordNumber> void setZ80State(RegistersSetter<T> registersBase, Z80State state) {
    registersBase.setRegA(state.getRegA());
    registersBase.setFlags(state.getRegF());
    registersBase.setRegB(state.getRegB());
    registersBase.setRegC(state.getRegC());
    registersBase.setRegD(state.getRegD());
    registersBase.setRegE(state.getRegE());
    registersBase.setRegH(state.getRegH());
    registersBase.setRegL(state.getRegL());
    registersBase.setRegAx(state.getRegAx());
    registersBase.setRegFx(state.getRegFx());
    registersBase.setRegBx(state.getRegBx());
    registersBase.setRegCx(state.getRegCx());
    registersBase.setRegDx(state.getRegDx());
    registersBase.setRegEx(state.getRegEx());
    registersBase.setRegHx(state.getRegHx());
    registersBase.setRegLx(state.getRegLx());
    registersBase.setRegIX(state.getRegIX());
    registersBase.setRegIY(state.getRegIY());
    registersBase.setRegSP(state.getRegSP());
    registersBase.setRegPC(state.getRegPC());
    registersBase.setRegI(state.getRegI());
    registersBase.setRegR(state.getRegR());
    registersBase.setMemptr(state.getMemPtr());
    registersBase.setHalted(state.isHalted());
    registersBase.setFfIFF1(state.isIFF1());
    registersBase.setFfIFF2(state.isIFF2());
    registersBase.setModeINT(state.getIM().ordinal());
    registersBase.setActiveINT(state.isINTLine());
    registersBase.setPendingEI(state.isPendingEI());
    registersBase.setActiveNMI(state.isNMI());
    registersBase.setFlagQ(false);
    registersBase.setLastFlagQ(state.isFlagQ());
  }
}
