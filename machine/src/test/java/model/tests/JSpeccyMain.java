package model.tests;

import java.util.Optional;

public class JSpeccyMain {
//
//  private static JSpeccy jSpeccy;
//
//  public static void main(String[] args) {
//    JSpeccyCommand jSpeccyCommand = getJSpeccyCommand(new TestDriver());
//    jSpeccyCommand.run();
//  }
//
//  public static JSpeccyCommand getJSpeccyCommand(TestDriver testDriver) {
//    jSpeccy = new JSpeccy() {
//      @Override
//      public void initSpectrumProperty() {
//        spectrum = new Spectrum(settings) {
//          @Override
//          public int fetchOpcode(int address) {
//            if (testDriver.isExecuting()) {
//              if (!testDriver.isSkipFetch()) {
//                return super.fetchOpcode(address);
//              }else
//                return memory.readByte(address) & 0xff;
//            } else
//              try {
//                while (true) {
//                  Integer opcode1 = testDriver.getOpcode(address);
//                  if (opcode1 != null && opcode1 != -1) {
//                    address= spectrum.z80.getRegPC();
//                    System.out.printf("%h: %2h%n", address, opcode1);
//
//                    int tstates = clock.getTstates();
//
//                    processFetch(address);
//
////                  return memory.readByte(address) & 0xff;
//                    return opcode1;
//                  } else {
////                  System.out.println("eh2");
//                  }
//                }
//              } catch (Exception e) {
//                System.out.println("finished main");
//                spectrum.z80.setHalted(true);
//                jSpeccy.stopEmulation();
//                jSpeccy.dispose();
//                throw e;
//              }
//
//          }
//
//          private void processFetch(int address) {
//            if (!testDriver.isSkipFetch()) {
//              if (contendedRamPage[address >>> 14]) {
//                clock.addTstates(delayTstates[clock.getTstates()] + 4);
//              } else {
//                clock.addTstates(4);
//              }
////                      System.out.println("tstates added in main: " + (clock.getTstates() - tstates));
//            }
//          }
//
//          public void execDone() {
//            testDriver.execDone();
//          }
//        };
//
//        spectrum.z80.setExecDone(true);
//        spectrum.z80.setRegPC(0xA000);
//
////        for (int i = 0; i < 0xFFFF; i++) {
////          spectrum.z80.setBreakpoint(i, true);
////        }
//      }
//    };
//
//    JSpeccyCommand jSpeccyCommand = new JSpeccyCommand(jSpeccy) {
//      public void run() {
//        jSpeccy.run(this);
//        testDriver.setMemory(jSpeccy.spectrum.getMemory());
//        testDriver.setSpectrum(jSpeccy.spectrum);
////        jSpeccy.setVisible(true);
////        jSpeccy.spectrum.infinite= false;
//      }
//    };
//
//    jSpeccyCommand.setModel(Optional.empty());
//    jSpeccyCommand.setUlaplus(Optional.empty());
//    jSpeccyCommand.setAutoload(Optional.empty());
//    jSpeccyCommand.setBorderSize(Optional.empty());
//    jSpeccyCommand.setConfirmActions(Optional.empty());
//    jSpeccyCommand.setHifi(Optional.empty());
//    jSpeccyCommand.setJoystick(Optional.empty());
//    jSpeccyCommand.setFastload(Optional.empty());
//    jSpeccyCommand.setZoom(Optional.empty());
//    jSpeccyCommand.setIssue2(Optional.empty());
//    jSpeccyCommand.setLec(Optional.empty());
//    jSpeccyCommand.setAcceleratedLoading(Optional.empty());
//    jSpeccyCommand.setBug128k(Optional.empty());
//    JSpeccyCommand.If1Group if1Group = new JSpeccyCommand.If1Group();
//    if1Group.setIf1(Optional.empty());
//    jSpeccyCommand.setIf1Group(if1Group);
//    JSpeccyCommand.MultifaceGroup multifaceGroup = new JSpeccyCommand.MultifaceGroup();
//    multifaceGroup.multiface = Optional.empty();
//    multifaceGroup.mf128on48k = Optional.empty();
//    jSpeccyCommand.setMultifaceGroup(multifaceGroup);
//    jSpeccyCommand.setMapPCkeys(Optional.empty());
//    jSpeccyCommand.setRecreatedZX(Optional.empty());
//    jSpeccyCommand.setScanlines(Optional.empty());
//    jSpeccyCommand.setZoomFilter(Optional.empty());
//    jSpeccyCommand.setZoom(Optional.empty());
//    jSpeccyCommand.setSilence(Optional.empty());
//    jSpeccyCommand.setSaveTrap(Optional.empty());
//    jSpeccyCommand.setLoadTrap(Optional.empty());
//    jSpeccyCommand.setZoom(Optional.empty());
//    jSpeccyCommand.setZoom(Optional.empty());
//    return jSpeccyCommand;
//  }
//
}
