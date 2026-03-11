package com.fpetrola.z80.bytecode.tests.rzzx;

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.minizx.MiniZXScreen;
import com.fpetrola.z80.minizx.emulation.MiniZXWithEmulationBase;
import com.fpetrola.z80.opcodes.references.WordNumber;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;

public class JetSetWilly2Converted2_R {
  // Generated memory properties
  static private int EMPTY_ROOM_SCREEN_BUFFER = 28672; // 28672
  static private int TITLE_SCREEN_BOTTOM = 23040; // 23040
  static private int ROOM_LAYOUT = 32768; // 32768
  static private int SCREEN_PIXEL_BUFFER = 16384; // 16384
  static private int ATTRIBUTE_COPY_SIZE = 4096; // 4096
  static private int TITLE_SCREEN_ATTR_TOP = 38912; // 38912
  static private int SCREEN_ATTRIBUTE_BUFFER = 22528; // 22528
  static private int UNKNOWN_39424 = 39424; // 39424
  static private int CODES_AREA = 40448; // 40448
  static private int mem_24064 = 24064; // 24064
  static private int GAME_PIXEL_BUFFER = 24576; // 24576
  static private int ATTRIBUTES_MEMORY = 23552; // 23552
  static private int SCREEN_BUFFER_ADDRESS_LOOKUP_TABLE = 33280; // 33280
  static private int TEMP_BUFFER_20480 = 20480; // 20480
  static private int TEMP_BUFFER_18432 = 18432; // 18432
  static private int MEMORY_SIZE = 65536; // 65536
  static private int mem_16385 = 16385; // 16385
  static private int mem_20481 = 20481; // 20481
  static private int mem_23553 = 23553; // 23553
  static private int mem_22529 = 22529; // 22529
  static private int mem_18461 = 18461; // 18461
  static private int mem_18462 = 18462; // 18462
  static private int CHECK_WILLY_CELL = 38430; // 38430
  static private int CURRENT_ROOM = 33824; // 33824
  static private int LEFT_RIGHT_MOVEMENT_TABLE = 33825; // 33825
  static private int TRIANGLE_UDGS = 33841; // 33841
  static private int FOOT_BARREL_GRAPHIC = 40000; // 40000
  static private int mem_18498 = 18498; // 18498
  static private int mem_18501 = 18501; // 18501
  static private int mem_18504 = 18504; // 18504
  static private int mem_18507 = 18507; // 18507
  static private int PRESS_ENTER_TEXT = 33876; // 33876
  static private int DRAW_SPRITE = 37974; // 37974
  static private int mem_34399 = 34399; // 34399
  static private int MESSAGE_BUFFER = 23136; // 23136
  static private int mem_20576 = 20576; // 20576
  static private int MARIA_GRAPHIC = 40032; // 40032
  static private int mem_23137 = 23137; // 23137
  static private int OR_BYTES_TO_BUFFER = 38504; // 38504
  static private int mem_26734 = 26734; // 26734
  static private int mem_20592 = 20592; // 20592
  static private int mem_23672 = 23672; // 23672
  static private int mem_20601 = 20601; // 20601
  static private int PRINT_MESSAGE = 38528; // 38528
  static private int ROOM_NAME = 32896; // 32896
  static private int mem_18575 = 18575; // 18575
  static private int PRINT_CHAR = 38545; // 38545
  static private int COPY_BYTES = 38555; // 38555
  static private int mem_38043 = 38043; // 38043
  static private int CODE_ENTRY_ROUTINE = 34463; // 34463
  static private int mem_20640 = 20640; // 20640
  static private int ROOM_TILES = 32928; // 32928
  static private int PLAY_THEME_TUNE = 38562; // 38562
  static private int mem_38061 = 38061; // 38061
  static private int ENTER_ROOM_ABOVE = 38064; // 38064
  static private int WALL_TILE = 32946; // 32946
  static private int COLLISION_FATAL_1 = 37047; // 37047
  static private int COLLISION_FATAL_2 = 37048; // 37048
  static private int NASTY_TILE = 32955; // 32955
  static private int MOVE_ROPE_AND_GUARDIANS = 37056; // 37056
  static private int DISPLAY_CODE_ENTRY_SCREEN = 34499; // 34499
  static private int RAMP_TILE = 32964; // 32964
  static private int mem_35526 = 35526; // 35526
  static private int CHECK_ENTER_FIRE = 38601; // 38601
  static private int mem_16586 = 16586; // 16586
  static private int mem_22730 = 22730; // 22730
  static private int mem_22731 = 22731; // 22731
  static private int mem_22732 = 22732; // 22732
  static private int mem_22733 = 22733; // 22733
  static private int CONVEYOR_TILE = 32973; // 32973
  static private int mem_38095 = 38095; // 38095
  static private int mem_18639 = 18639; // 18639
  static private int mem_33488 = 33488; // 33488
  static private int mem_16594 = 16594; // 16594
  static private int mem_22738 = 22738; // 22738
  static private int mem_22739 = 22739; // 22739
  static private int mem_22740 = 22740; // 22740
  static private int mem_22741 = 22741; // 22741
  static private int CONVEYOR_DIRECTION = 32982; // 32982
  static private int CONVEYOR_ATTR_ADDRESS = 32983; // 32983
  static private int mem_32984 = 32984; // 32984
  static private int CONVEYOR_LENGTH = 32985; // 32985
  static private int RAMP_DEFINITION = 32986; // 32986
  static private int RAMP_ATTR_ADDRESS = 32987; // 32987
  static private int mem_32988 = 32988; // 32988
  static private int RAMP_LENGTH = 32989; // 32989
  static private int PLAY_INTRO_SOUND = 38622; // 38622
  static private int BORDER_COLOUR = 32990; // 32990
  static private int ITEM_GRAPHIC = 32993; // 32993
  static private int ROOM_EXITS = 33001; // 33001
  static private int mem_33002 = 33002; // 33002
  static private int CYCLE_INK_PAPER_COLOURS = 35563; // 35563
  static private int mem_33003 = 33003; // 33003
  static private int mem_33004 = 33004; // 33004
  static private int ENTITY_SPECIFICATIONS = 33008; // 33008
  static private int mem_38134 = 38134; // 38134
  static private int MOVE_CONVEYOR = 38137; // 38137
  static private int mem_2301 = 2301; // 2301
  static private int KEYBOARD_READ_2 = 36605; // 36605
  static private int KEYBOARD_PORT = 65278; // 65278
  static private int KEYBOARD_PORT_2 = 32510; // 32510
  static private int CLEAR_SCREEN_SIZE = 6911; // 6911
  static private int NUMBER_KEY_GRAPHICS = 39680; // 39680
  static private int TEMP_22784 = 22784; // 22784
  static private int UDG_BASE = 1792; // 1792
  static private int WILLY_SPRITE_GRAPHIC = 40192; // 40192
  static private int KEYBOARD_READ = 48896; // 48896
  static private int STACK_BASE = 65280; // 65280
  static private int TEMP_24320 = 24320; // 24320
  static private int ENTITY_BUFFERS = 33024; // 33024
  static private int DRAW_CURRENT_ROOM = 36147; // 36147
  static private int SPECIAL_ROOM_HANDLER = 38196; // 38196
  static private int READ_KEYBOARD_CODE_ENTRY = 34620; // 34620
  static private int mem_40256 = 40256; // 40256
  static private int mem_22864 = 22864; // 22864
  static private int mem_22867 = 22867; // 22867
  static private int ITEMS_COLLECTED_TIME_TEXT = 34132; // 34132
  static private int mem_22870 = 22870; // 22870
  static private int mem_22873 = 22873; // 22873
  static private int ATTRIBUTE_BUFFER_ADDRESS = 36189; // 36189
  static private int FILL_ATTRIBUTE_BUFFER_24064 = 36203; // 36203
  static private int mem_23918 = 23918; // 23918
  static private int mem_23919 = 23919; // 23919
  static private int GAME_TEXT = 34164; // 34164
  static private int OVER_TEXT = 34168; // 34168
  static private int ITEMS_COLLECTED = 34172; // 34172
  static private int CURRENT_TIME = 34175; // 34175
  static private int mem_34687 = 34687; // 34687
  static private int CODE_ENTRY_ATTR = 39808; // 39808
  static private int CHECK_TOILET = 38276; // 38276
  static private int SEVEN_AM_TEXT = 34181; // 34181
  static private int ENTER_CODE_TEXT = 34187; // 34187
  static private int DRAW_REMAINING_LIVES = 35211; // 35211
  static private int mem_23950 = 23950; // 23950
  static private int mem_23951 = 23951; // 23951
  static private int SORRY_TRY_CODE_TEXT = 34219; // 34219
  static private int mem_23988 = 23988; // 23988
  static private int mem_23996 = 23996; // 23996
  static private int mem_23997 = 23997; // 23997
  static private int DRAW_ROPE_ARROWS_GUARDIANS = 37310; // 37310
  static private int COPY_ROOM_ATTRIBUTE = 36288; // 36288
  static private int SET_WILLY_ATTRIBUTES = 38344; // 38344
  static private int MINUTE_COUNTER = 34251; // 34251
  static private int LIVES_REMAINING = 34252; // 34252
  static private int SCREEN_FLASH_COUNTER = 34253; // 34253
  static private int KEMPSTON_JOYSTICK = 34254; // 34254
  static private int WILLY_Y = 34255; // 34255
  static private int mem_22991 = 22991; // 22991
  static private int mem_22992 = 22992; // 22992
  static private int WILLY_DIRECTION_FLAGS = 34256; // 34256
  static private int AIRBORNE_STATUS = 34257; // 34257
  static private int DRAW_ITEMS_AND_COLLECT = 37841; // 37841
  static private int WILLY_ANIMATION_FRAME = 34258; // 34258
  static private int WILLY_ATTR_BUFFER_ADDR = 34259; // 34259
  static private int MOVE_WILLY_1 = 36307; // 36307
  static private int JUMPING_ANIMATION_COUNTER = 34261; // 34261
  static private int ROPE_STATUS = 34262; // 34262
  static private int WILLY_STATE_ON_ENTRY = 34263; // 34263
  static private int mem_24028 = 24028; // 24028
  static private int mem_24029 = 24029; // 24029
  static private int ITEMS_REMAINING_256_MINUS = 34270; // 34270
  static private int GAME_MODE = 34271; // 34271
  static private int INACTIVITY_TIMER = 34272; // 34272
  static private int mem_65504 = 65504; // 65504
  static private int MUSIC_NOTE_INDEX = 34273; // 34273
  static private int MUSIC_FLAGS = 34274; // 34274
  static private int WRITETYPER_KEY_COUNTER = 34275; // 34275
  static private int TEMPORARY_VARIABLE = 34276; // 34276
  static private int mem_34279 = 34279; // 34279
  static private int mem_23023 = 23023; // 23023
  static private int mem_23024 = 23024; // 23024
  static private int TITLE_SCREEN_TUNE_DATA = 34299; // 34299
  static private int mem_45054 = 45054; // 45054
  static private int mem_61438 = 61438; // 61438
  static private int mem_63486 = 63486; // 63486
  static private int mem_64510 = 64510; // 64510
  static private int mem_57342 = 57342; // 57342
  static private int FIRST_ITEM_INDEX = 41983; // 41983
  static private int CLEAR_SCREEN_SIZE_2 = 6143; // 6143
  static private int PIXEL_COPY_SIZE = 2047; // 2047
  static private int FULL_SCREEN_SIZE = 4095; // 4095
  static private int MEMORY_END = 65535; // 65535

  public static int[] mem = new int[0x10000];
  static public IO<WordNumber> io;
  private final Stack<Integer> stack = new Stack<>();

  public int carry(int f) {
    return f & 1;
  }

  public void push(int value) {
    stack.push(value);
  }

  public int pop() {
    return stack.pop();
  }

  protected Function<Integer, Integer> getMemFunction() {
    return index -> mem[index];
  }

  public void init() {
    this.mem = new int[MEMORY_SIZE];
    MiniZX.createScreen(((MiniZXIO) io).getMiniZXKeyboard(), new MiniZXScreen(this.getMemFunction()));
    final byte[] rom = MiniZXWithEmulationBase.createROM();
    final byte[] bytes = MiniZXWithEmulationBase.gzipDecompressFromBase64(this.getProgramBytes());
    for (int i = 0; i < MEMORY_SIZE; ++i) {
      mem[i] = ((i < SCREEN_PIXEL_BUFFER) ? rom[i] : bytes[i]) & 0xff;
    }
  }

  public static JFrame createScreen(KeyListener keyListener, Container miniZXScreen1) {
    JFrame frame = new JFrame("Mini ZX Spectrum");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setContentPane(miniZXScreen1);
    frame.setLocationRelativeTo(null);
    frame.setSize(512, 384);
    frame.pack();
    frame.setVisible(true);
    frame.addKeyListener(keyListener);
    return frame;
  }

  public JetSetWilly2Converted2_R(MiniZXIO<WordNumber> rzxPlayerIO, Predicate<Integer> interruptionCondition) {
    io = rzxPlayerIO;
    init();
  }

  public String getProgramBytes() {
    return "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";
  }

  public static int _rrc(int a) {
    return ((a & 0xff) >> 1) | (((a & 0x1) << 7) & 0xff);
  }

  public static int _rlc(int a) {
    return ((a << 1) & 0xfe) | ((a & 0xff) >> 7);
  }

  public int[] _rl(int a, int f) {
    int lastCarry = f & 0x1;
    f = (a & 128) >> 7;
    return new int[]{((a << 1) & 0xfe) | lastCarry, f};
  }

  public static int cp(int value1, int value2) {
    return value1 - value2;
  }

  public static int inc(int value1) {
    return (value1 + 1) & 0xff;
  }

  public static int inc16(int value1) {
    return (value1 + 1) & 0xffff;
  }

  public static int dec(int value1) {
    return (value1 - 1) & 0xff;
  }

  public int dec16(int value1) {
    return (value1 - 1) & 0xffff;
  }

  public static int add(int value1, int value2) {
    return (value1 + value2) & 0xff;
  }

  public static int add16(int value1, int value2) {
    return (value1 + value2) & 0xffff;
  }

  public static int flagZ(int value1) {
    return value1 << 1;
  }

  public int sra(int value1) {
    return (value1 >> 1) | (value1 & 0x80);
  }

  public static int in(int port) {
    return io.in(WordNumber.createValue(port)).intValue();
  }

  public void $0() {
  }

  public void $CODE_ENTRY_ROUTINE() {
    int HL;
    int BC;
    int DE;
    int IX;
    int A;
    int F;
    int H;
    int L;
    int B;
    int C;
    int D;
    int E;
    HL = SCREEN_PIXEL_BUFFER;
    DE = mem_16385;
    BC = CLEAR_SCREEN_SIZE;
    mem[HL] = 0;
    while ((BC--) != 0)
      mem[DE++] = mem[HL++];

    IX = ENTER_CODE_TEXT;
    int[] r1 = $DISPLAY_CODE_ENTRY_SCREEN(IX);
    A = r1[0];
    F = r1[1];
    if (F != 0) {
      IX = SORRY_TRY_CODE_TEXT;
      int[] r2 = $DISPLAY_CODE_ENTRY_SCREEN(IX);
      A = r2[0];
      F = r2[1];
      if (F != 0) {
      }
    }
    while (true) {
      label325:
      while (true) {
        extracted();
        H = 164 & 0xff;
        A = mem[FIRST_ITEM_INDEX];
        L = A;
        mem[ITEMS_REMAINING_256_MINUS] = A;
        do {
          mem[(H << 8) | L] = mem[(H << 8) | L] | 64;
          int var31 = inc(L);
          L = var31;
          F = var31;
        } while (F != 0);
        HL = MUSIC_FLAGS;
        mem[HL] = mem[HL] | 1;
        label207:
        while (true) {
          DE = clearScreen();

          copyAttributes(DE);

          HL = MESSAGE_BUFFER;
          DE = mem_23137;
          BC = 31;
          mem[HL] = 70;
          while ((BC--) != 0)
            mem[DE++] = mem[HL++];

          IX = PRESS_ENTER_TEXT;
          DE = mem_20576;
          C = 32;
          $PRINT_MESSAGE(C, DE, IX);
          DE = SCREEN_ATTRIBUTE_BUFFER;
          do {
            A = mem[DE];
            A = A | A;
            F = flagZ(A);
            if (F != 0) {
              if (A != 211) {
                if (A != 9) {
                  if (A != 45) {
                    if (A != 36) {
                      C = 0;
                      if (A != 8) {
                        if (A != 41) {
                          if (A != 44) {
                            if (A != 5) {
                              C = 16;
                            }
                          } else {
                            A = 37;
                            mem[DE] = A;
                          }
                        }
                      }
                      A = DE & 0xff;
                      A = A & 1;
                      A = _rlc(A);
                      A = _rlc(A);
                      A = _rlc(A);
                      A = A | C;
                      C = A;
                      B = 0;
                      HL = TRIANGLE_UDGS;
                      HL = add16(HL, (B << 8) | C);
                      push(DE);
                      F = (DE >> 8) & 1;
                      D = 64;
                      if (F != 0) {
                        D = 72;
                      }
                      B = 8;
                      DE = (D << 8) | (DE & 0xff);
                      DE = $COPY_BYTES(B, DE, HL);
                      DE = pop();
                    }
                  }
                }
              }
            }
            DE = inc16(DE);
            A = DE >> 8;
            F = cp(A, 90);
          } while (F != 0);
          BC = 31;
          A = 0;
          do {
            E = in(BC);
            A = A | E;
            BC = (BC & 0xff) | (((((BC >> 8) - 1) & 255) & 0xff) << 8);
          } while ((BC >> 8) != 0);
          A = A & 32;
          F = flagZ(A);
          if (F == 0) {
            A = 1;
            mem[KEMPSTON_JOYSTICK] = A;
          }
          HL = TITLE_SCREEN_TUNE_DATA;
          F = $PLAY_THEME_TUNE(HL);
          if (F != 0) {
            break;
          }
          A = 0;
          mem[TEMPORARY_VARIABLE] = A;
          do {
            $CYCLE_INK_PAPER_COLOURS();
            HL = MESSAGE_BUFFER;
            DE = mem_23137;
            BC = 31;
            mem[HL] = 79;
            while ((BC--) != 0)
              mem[DE++] = mem[HL++];

            A = mem[TEMPORARY_VARIABLE];
            IX = PRESS_ENTER_TEXT;
            E = A;
            D = 0;
            IX = add16(IX, DE);
            DE = mem_20576;
            C = 32;
            $PRINT_MESSAGE(C, DE, IX);
            A = mem[TEMPORARY_VARIABLE];
            A = A & 31;
            A = add(A, 50);
            $PLAY_INTRO_SOUND(A);
            BC = mem_45054;
            A = in(BC);
            A = A & 1;
            if (A != 1) {
              break label207;
            }
            A = mem[TEMPORARY_VARIABLE];
            A = inc(A);
            F = cp(A, 224);
            mem[TEMPORARY_VARIABLE] = A;
          } while (F != 0);
        }
        HL = SEVEN_AM_TEXT;
        DE = CURRENT_TIME;
        BC = 6;
        while ((BC--) != 0)
          mem[DE++] = mem[HL++];

        HL = UNKNOWN_39424;
        DE = TITLE_SCREEN_BOTTOM;
        BC = 256;
        while ((BC--) != 0)
          mem[DE++] = mem[HL++];

        while (true) {
          while (true) {
            A = mem[CURRENT_ROOM];
            A = A | 192;
            F = flagZ(A);
            H = A & 0xff;
            L = 0;
            DE = ROOM_LAYOUT;
            BC = 256;
            HL = (H << 8) | L;
            while ((BC--) != 0)
              mem[DE++] = mem[HL++];

            copyEntities();
            HL = WILLY_Y;
            DE = WILLY_STATE_ON_ENTRY;
            BC = 7;
            while ((BC--) != 0)
              mem[DE++] = mem[HL++];

            $DRAW_CURRENT_ROOM();
            HL = TEMP_BUFFER_20480;
            DE = mem_20481;
            BC = PIXEL_COPY_SIZE;
            mem[HL] = 0;
            while ((BC--) != 0)
              mem[DE++] = mem[HL++];

            IX = ROOM_NAME;
            C = 32;
            DE = TEMP_BUFFER_20480;
            $PRINT_MESSAGE(C, DE, IX);
            IX = ITEMS_COLLECTED_TIME_TEXT;
            DE = mem_20576;
            C = 32;
            $PRINT_MESSAGE(C, DE, IX);
            A = mem[BORDER_COLOUR];
            C = 254;
            A = A ^ A;
            F = flagZ(A);
            mem[ROPE_STATUS] = A;
            while (true) {
              label363:
              {
                $DRAW_REMAINING_LIVES();
                HL = mem_24064;
                DE = ATTRIBUTES_MEMORY;
                BC = 512;
                while ((BC--) != 0)
                  mem[DE++] = mem[HL++];

                HL = EMPTY_ROOM_SCREEN_BUFFER;
                DE = GAME_PIXEL_BUFFER;
                BC = ATTRIBUTE_COPY_SIZE;
                while ((BC--) != 0)
                  mem[DE++] = mem[HL++];

                $MOVE_ROPE_AND_GUARDIANS();
                A = mem[GAME_MODE];
                F = cp(A, 3);
                label283:
                {
                  label282:
                  {
                    label412:
                    {
                      label279:
                      {
                        label358:
                        {
                          try {
                            if (F != 0) {
                              $MOVE_WILLY_1();
                            }
                          } catch (RuntimeException var645) {
                            int var99 = Integer.parseInt(var645.getMessage());
                            if (var99 == COLLISION_FATAL_2) {
                              break label412;
                            }
                            if (var99 == mem_38043) {
                              break;
                            }
                            if (var99 == mem_38061) {
                              break;
                            }
                            if (var99 == mem_38134) {
                              break;
                            }
                            if (var99 == mem_38095) {
                              break label358;
                            }
                          }
                          A = mem[WILLY_Y];
                          F = cp(A, 225);
                          try {
                            if (F >= 0) {
                              $ENTER_ROOM_ABOVE();
                            }
                            break label279;
                          } catch (RuntimeException var644) {
                            if (Integer.parseInt(var644.getMessage()) != mem_38095) {
                              break label279;
                            }
                          }
                        }
                        break;
                      }
                      A = mem[GAME_MODE];
                      F = cp(A, 3);
                      try {
                        if (F != 0) {
                          $SET_WILLY_ATTRIBUTES();
                        }
                      } catch (RuntimeException var643) {
                        if (Integer.parseInt(var643.getMessage()) == COLLISION_FATAL_2) {
                          break label412;
                        }
                      }
                      A = mem[GAME_MODE];
                      if (A == 2) {
                        $CHECK_TOILET();
                      }
                      try {
                        $SPECIAL_ROOM_HANDLER();
                      } catch (RuntimeException var642) {
                        if (Integer.parseInt(var642.getMessage()) == COLLISION_FATAL_2) {
                          break label412;
                        }
                      }
                      try {
                        $DRAW_ROPE_ARROWS_GUARDIANS();
                        break label282;
                      } catch (RuntimeException var641) {
                        if (Integer.parseInt(var641.getMessage()) != COLLISION_FATAL_2) {
                          break label282;
                        }
                      }
                    }
                    A = 255;
                    // mem[AIRBORNE_STATUS] = var100;
                    break label283;
                  }
                  $MOVE_CONVEYOR();
                  $DRAW_ITEMS_AND_COLLECT();
                }
                HL = GAME_PIXEL_BUFFER;
                DE = SCREEN_PIXEL_BUFFER;
                BC = ATTRIBUTE_COPY_SIZE;
                while ((BC--) != 0)
                  mem[DE++] = mem[HL++];

                A = mem[GAME_MODE];
                A = A & 2;
                F = flagZ(A);
                A = _rrc(A);
                HL = WILLY_ANIMATION_FRAME;
                A = A | mem[HL];
                F = flagZ(A);
                mem[HL] = A;
                A = mem[SCREEN_FLASH_COUNTER];
                A = A | A;
                F = flagZ(A);
                if (F != 0) {
                  int var549 = dec(A);
                  A = var549;
                  F = var549;
                  mem[SCREEN_FLASH_COUNTER] = A;
                  A = _rlc(A);
                  A = _rlc(A);
                  A = _rlc(A);
                  A = A & 56;
                  F = flagZ(A);
                  HL = ATTRIBUTES_MEMORY;
                  DE = mem_23553;
                  BC = 511;
                  mem[HL] = A;
                  while ((BC--) != 0)
                    mem[DE++] = mem[HL++];

                }
                HL = ATTRIBUTES_MEMORY;
                DE = SCREEN_ATTRIBUTE_BUFFER;
                BC = 512;
                while ((BC--) != 0)
                  mem[DE++] = mem[HL++];

                IX = CURRENT_TIME;
                DE = mem_20601;
                C = 6;
                $PRINT_MESSAGE(C, DE, IX);
                IX = ITEMS_COLLECTED;
                DE = mem_20592;
                C = 3;
                $PRINT_MESSAGE(C, DE, IX);
                A = mem[MINUTE_COUNTER];
                A = inc(A);
                mem[MINUTE_COUNTER] = A;
                if (F == 0) {
                  IX = CURRENT_TIME;
                  mem[IX + 4] = inc(mem[IX + 4]);
                  A = mem[IX + 4];
                  if (A == 58) {
                    mem[IX + 4] = 48;
                    mem[IX + 3] = inc(mem[IX + 3]);
                    A = mem[IX + 3];
                    if (A == 54) {
                      mem[IX + 3] = 48;
                      A = mem[IX];
                      if (A == 49) {
                        mem[IX + 1] = inc(mem[IX + 1]);
                        A = mem[IX + 1];
                        if (A == 51) {
                          A = mem[IX + 5];
                          if (A == 112) {
                            continue label325;
                          }
                          mem[IX] = 32;
                          mem[IX + 1] = 49;
                          mem[IX + 5] = 112;
                        }
                      } else {
                        mem[IX + 1] = inc(mem[IX + 1]);
                        A = mem[IX + 1];
                        if (A == 58) {
                          mem[IX + 1] = 48;
                          mem[IX] = 49;
                        }
                      }
                    }
                  }
                }
                BC = KEYBOARD_PORT;
                A = in(BC);
                E = A;
                B = 127;
                A = in((B << 8) | (BC & 0xff));
                A = A | E;
                F = flagZ(A);
                A = A & 1;
                F = flagZ(A);
                if (F == 0) {
                  continue label325;
                }
                A = mem[INACTIVITY_TIMER];
                mem[INACTIVITY_TIMER] = A;
                if (F != 0) {
                  B = 253;
                  A = in((B << 8) | (BC & 0xff));
                  A = A & 31;
                  F = flagZ(A);
                  if (A == 31) {
                    break label363;
                  }
                  DE = 0;
                }
                while (true) {
                  B = 2;
                  A = in((B << 8) | C);
                  A = A & 31;
                  F = flagZ(A);
                  if (A != 31) {
                    HL = UNKNOWN_39424;
                    DE = TITLE_SCREEN_BOTTOM;
                    BC = 256;
                    while ((BC--) != 0)
                      mem[DE++] = mem[HL++];

                    A = mem[BORDER_COLOUR];
                    break;
                  }
                  int var492 = inc(E);
                  E = var492;
                  F = var492;
                  D = (DE >> 8) & 0xff;
                  if (F == 0) {
                    int var494 = inc(D);
                    D = var494;
                    F = var494;
                    if (F == 0) {
                      A = mem[WRITETYPER_KEY_COUNTER];
                      if (A != 10) {
                        $CYCLE_INK_PAPER_COLOURS();
                      }
                    }
                  }
                }
              }
              A = mem[AIRBORNE_STATUS];
              if (A == 255) {
                A = 71;
                do {
                  HL = SCREEN_ATTRIBUTE_BUFFER;
                  DE = mem_22529;
                  BC = 511;
                  mem[HL] = A;
                  while ((BC--) != 0)
                    mem[DE++] = mem[HL++];

                  E = A;
                  A = ~A;
                  F = A;
                  A = A & 7;
                  F = flagZ(A);
                  A = _rlc(A);
                  A = _rlc(A);
                  A = _rlc(A);
                  A = A | 7;
                  F = flagZ(A);
                  D = A;
                  C = E;
                  C = _rrc(C);
                  C = _rrc(C);
                  C = _rrc(C);
                  A = A | 16;
                  F = flagZ(A);
                  A = A ^ A;
                  F = flagZ(A);
                  do {
                    A = A ^ 24;
                    F = flagZ(A);
                    B = D;
                    do {
                      B = (B - 1) & 255;
                    } while (B != 0);
                    int var186 = dec(C);
                    C = var186;
                    F = var186;
                  } while (F != 0);
                  A = E;
                  int var189 = dec(A);
                  A = var189;
                  F = var189;
                  F = cp(A, 63);
                } while (F != 0);
                HL = LIVES_REMAINING;
                A = mem[HL];
                A = A | A;
                F = flagZ(A);
                if (F == 0) {
                  HL = SCREEN_PIXEL_BUFFER;
                  DE = mem_16385;
                  BC = FULL_SCREEN_SIZE;
                  mem[HL] = 0;
                  while ((BC--) != 0)
                    mem[DE++] = mem[HL++];

                  A = A ^ A;
                  F = flagZ(A);
                  mem[TEMPORARY_VARIABLE] = A;
                  DE = mem_40256;
                  HL = mem_18575;
                  C = 0;
                  r1 = $DRAW_SPRITE(C, DE, HL);
                  F = r1[0];
                  DE = MARIA_GRAPHIC;
                  HL = mem_18639;
                  C = 0;
                  r1 = $DRAW_SPRITE(C, DE, HL);
                  F = r1[0];
                  do {
                    A = mem[TEMPORARY_VARIABLE];
                    C = A;
                    B = 130;
                    A = mem[(B << 8) | C];
                    A = A | 15;
                    F = flagZ(A);
                    L = A;
                    BC = inc16((B << 8) | C);
                    A = mem[BC];
                    int var216 = A - 32;
                    A = var216 & 255;
                    F = var216;
                    H = A & 0xff;
                    DE = FOOT_BARREL_GRAPHIC;
                    C = 0;
                    r1 = $DRAW_SPRITE(C, DE, HL);
                    F = r1[0];
                    A = mem[TEMPORARY_VARIABLE];
                    A = ~A;
                    F = A;
                    E = A;
                    A = A ^ A;
                    F = flagZ(A);
                    BC = 64;
                    do {
                      A = A ^ 24;
                      F = flagZ(A);
                      B = E;
                      do {
                        B = (B - 1) & 255;
                      } while (B != 0);
                      int var233 = dec(C);
                      C = var233;
                      F = var233;
                    } while (F != 0);
                    HL = SCREEN_ATTRIBUTE_BUFFER;
                    DE = mem_22529;
                    BC = 511;
                    A = mem[TEMPORARY_VARIABLE];
                    A = A & 12;
                    F = flagZ(A);
                    A = _rlc(A);
                    A = A | 71;
                    F = flagZ(A);
                    mem[HL] = A;
                    while ((BC--) != 0)
                      mem[DE++] = mem[HL++];

                    A = A & 250;
                    F = flagZ(A);
                    A = A | 2;
                    F = flagZ(A);
                    mem[mem_22991] = A;
                    mem[mem_22992] = A;
                    mem[mem_23023] = A;
                    mem[mem_23024] = A;
                    A = mem[TEMPORARY_VARIABLE];
                    int var257 = add(A, 4);
                    A = var257;
                    F = var257;
                    mem[TEMPORARY_VARIABLE] = A;
                    F = cp(A, 196);
                  } while (F != 0);
                  IX = GAME_TEXT;
                  C = 4;
                  DE = mem_16586;
                  $PRINT_MESSAGE(C, DE, IX);
                  IX = OVER_TEXT;
                  C = 4;
                  DE = mem_16594;
                  $PRINT_MESSAGE(C, DE, IX);
                  BC = 0;
                  B = 0;
                  C = 0;
                  D = 6;
                  while (true) {
                    do {
                      B = (B - 1) & 255;
                    } while (B != 0);
                    A = C;
                    A = A & 7;
                    F = flagZ(A);
                    A = A | 64;
                    F = flagZ(A);
                    mem[mem_22730] = A;
                    int var271 = inc(A);
                    A = var271;
                    F = var271;
                    A = A & 7;
                    F = flagZ(A);
                    A = A | 64;
                    F = flagZ(A);
                    mem[mem_22731] = A;
                    int var280 = inc(A);
                    A = var280;
                    F = var280;
                    A = A & 7;
                    F = flagZ(A);
                    A = A | 64;
                    F = flagZ(A);
                    mem[mem_22732] = A;
                    int var289 = inc(A);
                    A = var289;
                    F = var289;
                    A = A & 7;
                    F = flagZ(A);
                    A = A | 64;
                    F = flagZ(A);
                    mem[mem_22733] = A;
                    int var298 = inc(A);
                    A = var298;
                    F = var298;
                    A = A & 7;
                    F = flagZ(A);
                    A = A | 64;
                    F = flagZ(A);
                    mem[mem_22738] = A;
                    int var307 = inc(A);
                    A = var307;
                    F = var307;
                    A = A & 7;
                    F = flagZ(A);
                    A = A | 64;
                    F = flagZ(A);
                    mem[mem_22739] = A;
                    int var316 = inc(A);
                    A = var316;
                    F = var316;
                    A = A & 7;
                    F = flagZ(A);
                    A = A | 64;
                    F = flagZ(A);
                    mem[mem_22740] = A;
                    int var325 = inc(A);
                    A = var325;
                    F = var325;
                    A = A & 7;
                    F = flagZ(A);
                    A = A | 64;
                    F = flagZ(A);
                    mem[mem_22741] = A;
                    int var334 = dec(C);
                    C = var334;
                    F = var334;
                    if (F == 0) {
                      int var336 = dec(D);
                      D = var336;
                      F = var336;
                      if (F == 0) {
                        continue label325;
                      }
                    }
                  }
                }
                int var339 = dec(mem[HL]);
                mem[HL] = var339;
                F = var339;
                HL = WILLY_STATE_ON_ENTRY;
                DE = WILLY_Y;
                BC = 7;
                while ((BC--) != 0)
                  mem[DE++] = mem[HL++];

                break;
              }
              A = 191;
              HL = MUSIC_FLAGS;
              A = in((A << 8) | 254);
              A = A & 31;
              F = flagZ(A);
              if (A != 31) {
                F = mem[HL] & 1;
                if (F == 0) {
                  A = mem[HL];
                  A = A ^ 3;
                  F = flagZ(A);
                  mem[HL] = A;
                }
              } else {
                mem[HL] = mem[HL] & (-2);
              }
              F = mem[HL] & 2;
              if (F == 0) {
                A = A ^ A;
                F = flagZ(A);
                mem[INACTIVITY_TIMER] = A;
                A = mem[MUSIC_NOTE_INDEX];
                int var447 = inc(A);
                A = var447;
                F = var447;
                mem[MUSIC_NOTE_INDEX] = A;
                A = A & 126;
                F = flagZ(A);
                A = _rrc(A);
                E = A;
                D = 0;
                HL = mem_34399;
                HL = add16(HL, (D << 8) | E);
                A = mem[LIVES_REMAINING];
                A = _rlc(A);
                A = _rlc(A);
                int var463 = A - 28;
                A = var463 & 255;
                F = var463;
                A = (-A) & 255;
                int var469 = add(A, mem[HL]);
                A = var469;
                F = var469;
                D = A;
                A = mem[BORDER_COLOUR];
                E = D;
                BC = 3;
                do {
                  do {
                    int var474 = dec(E);
                    E = var474;
                    F = var474;
                    if (F == 0) {
                      E = D;
                      A = A ^ 24;
                      F = flagZ(A);
                    }
                    BC = (BC & 0xff) | (((((BC >> 8) - 1) & 255) & 0xff) << 8);
                  } while ((BC >> 8) != 0);
                  int var477 = dec(BC & 0xff);
                  BC = (BC & 0xff00) | var477;
                  F = var477;
                } while (F != 0);
              }
              BC = mem_61438;
              A = in(BC);
              F = A & 2;
              if (F == 0) {
                A = A & 16;
                F = flagZ(A);
                A = A ^ 16;
                F = flagZ(A);
                A = _rlc(A);
                D = A;
                A = mem[WRITETYPER_KEY_COUNTER];
                if (A == 10) {
                  BC = mem_63486;
                  A = in(BC);
                  A = ~A;
                  F = A;
                  A = A & 31;
                  F = flagZ(A);
                  A = A | D;
                  F = flagZ(A);
                  mem[CURRENT_ROOM] = A;
                  break;
                }
              }
              A = mem[WRITETYPER_KEY_COUNTER];
              if (A != 10) {
                A = mem[CURRENT_ROOM];
                if (A == 28) {
                  A = mem[WILLY_Y];
                  if (A == 208) {
                    A = mem[WRITETYPER_KEY_COUNTER];
                    A = _rlc(A);
                    E = A;
                    D = 0;
                    IX = mem_34279;
                    IX = add16(IX, (D << 8) | E);
                    BC = mem_64510;
                    A = in(BC);
                    A = A & 31;
                    if (A != mem[IX]) {
                      if (A != 31) {
                        if (A != mem[IX + (-2)]) {
                          A = 0;
                          mem[WRITETYPER_KEY_COUNTER] = A;
                        }
                      }
                    } else {
                      B = 223;
                      A = in(BC);
                      A = A & 31;
                      if (A != mem[IX + 1]) {
                        if (A != 31) {
                          if (A != mem[IX + (-1)]) {
                            A = 0;
                            mem[WRITETYPER_KEY_COUNTER] = A;
                          }
                        }
                      } else {
                        A = mem[WRITETYPER_KEY_COUNTER];
                        A = inc(A);
                        mem[WRITETYPER_KEY_COUNTER] = A;
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private void copyEntities() {
    int DE;
    int A;
    int BC;
    int L;
    int HL;
    int C;
    int IX;
    int F;
    int H;
    IX = ENTITY_SPECIFICATIONS;
    DE = ENTITY_BUFFERS;
    A = 8;
    do {
      L = mem[IX];
      L = L & (-129);
      H = 20 & 0xff;
      HL = add16((H << 8) | L, (H << 8) | L);
      HL = add16(HL, HL);
      HL = add16(HL, HL);
      BC = 2;
      while ((BC--) != 0)
        mem[DE++] = mem[HL++];

      C = mem[IX + 1];
      mem[HL] = C;
      BC = 6;
      while ((BC--) != 0)
        mem[DE++] = mem[HL++];

      IX = inc16(IX);
      IX = inc16(IX);
      int var88 = dec(A);
      A = var88;
      F = var88;
    } while (F != 0);
  }

  private void copyAttributes(int DE) {
    int BC;
    int HL;
    HL = TITLE_SCREEN_ATTR_TOP;
    BC = 768;
    while ((BC--) != 0)
      mem[DE++] = mem[HL++];
  }

  private int clearScreen() {
    int BC;
    int DE;
    int HL;
    HL = SCREEN_PIXEL_BUFFER;
    DE = mem_16385;
    BC = CLEAR_SCREEN_SIZE_2;
    mem[HL] = 0;
    while ((BC--) != 0)
      mem[DE++] = mem[HL++];
    return DE;
  }

  private void extracted() {
    int A;
    int HL;
    A = 0;
    mem[KEMPSTON_JOYSTICK] = A;
    mem[MUSIC_NOTE_INDEX] = A;
    mem[SCREEN_FLASH_COUNTER] = A;
    mem[AIRBORNE_STATUS] = A;
    mem[MINUTE_COUNTER] = A;
    mem[INACTIVITY_TIMER] = A;
    mem[GAME_MODE] = A;
    A = 7;
    mem[LIVES_REMAINING] = A;
    A = 208;
    mem[WILLY_Y] = A;
    A = 33;
    mem[CURRENT_ROOM] = A;
    HL = mem_23988;
    int var16 = HL;
    mem[WILLY_ATTR_BUFFER_ADDR] = var16 & 0xff;
    mem[WILLY_ATTR_BUFFER_ADDR + 1] = var16 >>> 8;
    HL = ITEMS_COLLECTED;
    mem[HL] = 48;
    HL = inc16(HL);
    mem[HL] = 48;
    HL = inc16(HL);
    mem[HL] = 48;
  }

  public static int[] $DISPLAY_CODE_ENTRY_SCREEN(int i) {
    int i0 = 0;
    int i1 = 0;
    $PRINT_MESSAGE(32, TEMP_BUFFER_18432, i);
    int[] a = $DRAW_SPRITE(0, NUMBER_KEY_GRAPHICS, mem_18498);
    int[] a0 = $DRAW_SPRITE(0, a[1], mem_18501);
    int[] a1 = $DRAW_SPRITE(0, a0[1], mem_18504);
    int[] a2 = $DRAW_SPRITE(0, a1[1], mem_18507);
    int i2 = TEMP_22784;
    int i3 = CODE_ENTRY_ATTR;
    int i4 = 128;
    label0:
    while (true) {
      int i5 = i4 + -1;
      if (i4 == 0) {
        i0 = add(mem[mem_23672], 37);
        mem[mem_23672] = i0;
        if (i0 > 179) {
          i0 = i0 - 180 & 255;
        }
        int i6 = add(mem[CODES_AREA | i0], i0);
        mem[TEMPORARY_VARIABLE] = i6;
        int i7 = 47;
        while (true) {
          i7 = inc(i7);
          if (i0 > 18) {
            i0 = i0 - 18 & 255;
          } else {
            $PRINT_CHAR(i7, mem_18462);
            i1 = add(i0, 65);
            $PRINT_CHAR(i1, mem_18461);
            while (true) {
              int i8 = mem_22864;
              do {
                label2:
                {
                  RuntimeException a3 = null;
                  label1:
                  {
                    try {
                      int[] a4 = $READ_KEYBOARD_CODE_ENTRY(i8);
                      i1 = a4[0];
                      i0 = a4[1];
                      i8 = a4[2];
                    } catch (RuntimeException a5) {
                      a3 = a5;
                      break label1;
                    }
                    break label2;
                  }
                  if (Integer.parseInt(a3.getMessage()) == mem_34687) {
                    break label0;
                  }
                }
                i8 = inc16(inc16(inc16(i8)));
                i1 = i8 & 255;
              } while (i1 != 92);
            }
          }
        }
      } else {
        int[] a6 = mem;
        int i9 = i2 + 1;
        int[] a7 = mem;
        int i10 = i3 + 1;
        a6[i2] = a7[i3];
        i2 = i9;
        i3 = i10;
        i4 = i5;
      }
    }
    int i11 = i1 | i0;
    int[] a8 = new int[2];
    a8[0] = i11;
    a8[1] = cp(i11, mem[TEMPORARY_VARIABLE]);
    return a8;
  }

  public static int[] $READ_KEYBOARD_CODE_ENTRY(int i) {
    while (true) {
      int i0 = in(mem_63486) & 15;
      if (i0 == 15) {
        label0:
        while (true) {
          int i1 = 0;
          if ((in(KEYBOARD_READ | 254) & 1) == 0) {
            int i2 = mem[mem_22873] & 127;
            if (i2 != 7) {
              int i3 = i2 - 8 & 255 & 24;
              int i4 = _rrc(_rrc(_rrc(i3)));
              int i5 = mem[mem_22867] - 8 & 255 & 24;
              int i6 = _rlc(i5) | i4;
              int i7 = mem[mem_22870] - 8 & 255 & 24;
              int i8 = mem[mem_22864] - 8 & 255 & 24;
              _rlc(_rlc(_rlc(i8)));
              throw new RuntimeException(mem_34687 + "");
            }
          }
          int i9 = i + 0;
          mem[i9] = mem[i9] | 128;
          int i10 = i + 1;
          mem[i10] = mem[i10] | 128;
          int i11 = i + 32;
          mem[i11] = mem[i11] | 128;
          int i12 = i + 33;
          mem[i12] = mem[i12] | 128;
          int i13 = in(mem_63486) & 15;
          if (i13 != 14) {
            if (i13 != 13) {
              if (i13 != 11) {
                if (i13 != 7) {
                  continue label0;
                }
                i1 = 32;
              } else {
                i1 = 24;
              }
            } else {
              i1 = 16;
            }
          } else {
            i1 = 8;
          }
          mem[i + 0] = i1;
          mem[i + 1] = i1;
          mem[i + 32] = i1;
          mem[i + 33] = i1;
          int i14 = 24;
          while (true) {
            i14 = i14 & 255 | ((i14 >> 8) - 1 & 255 & 255) << 8;
            if (i14 >> 8 == 0) {
              int i15 = dec(i14 & 255);
              i14 = i14 & STACK_BASE | i15;
              if (i15 == 0) {
                int[] a = new int[3];
                a[0] = i13;
                a[1] = 24;
                a[2] = i;
                return a;
              }
            }
          }
        }
      }
    }
  }

  public void $DRAW_REMAINING_LIVES() {
    int i = mem[LIVES_REMAINING];
    int i0 = i | i;
    if (flagZ(i0) != 0) {
      int i1 = mem_20640;
      do {
        this.push(i1);
        this.push(i0 << 8 | 0);
        int i2 = _rlc(_rlc(_rlc(mem[MUSIC_NOTE_INDEX]))) & 96;
        int[] a = $DRAW_SPRITE(0, WILLY_SPRITE_GRAPHIC | i2, i1);
        int i3 = this.pop() >> 8;
        i1 = inc16(inc16(this.pop()));
        i0 = i3 - 1 & 255;
      } while (i0 != 0);
    }
  }

  public void $CYCLE_INK_PAPER_COLOURS() {
    int i = SCREEN_ATTRIBUTE_BUFFER;
    while (true) {
      mem[i] = add(mem[i], 24) & 184 | add(mem[i], 3) & 7;
      i = inc16(i);
      if (i >> 8 == 91) {
        return;
      }
    }
  }

  public void $DRAW_CURRENT_ROOM() {
    this.$FILL_ATTRIBUTE_BUFFER_24064();
    mem[ATTRIBUTE_BUFFER_ADDRESS] = 112;
    this.$DRAW_ROOM_TILES(mem_24064);
    mem[ATTRIBUTE_BUFFER_ADDRESS] = 120;
    this.$DRAW_ROOM_TILES(TEMP_24320);
  }

  public void $DRAW_ROOM_TILES(int i) {
    int i0 = 0;
    label0:
    while (true) {
      int i1 = mem[i + 0];
      int i2 = 54;
      int i3 = ROOM_TILES;
      label1:
      while (true) {
        int i4 = i2 + -1;
        if (i2 != 0) {
          int[] a = mem;
          int i5 = i3 + 1;
          if (a[i3] == i1) {
            i3 = i5;
          } else {
            i2 = i4;
            i3 = i5;
            continue label1;
          }
        }
        int i6 = mem[ATTRIBUTE_BUFFER_ADDRESS];
        int i7 = 8;
        while (true) {
          int i8 = mem[i3];
          mem[i6 << 8 | i0] = i8;
          i3 = inc16(i3);
          i6 = inc(i6);
          i7 = i7 - 1 & 255;
          if (i7 == 0) {
            int i9 = inc16(i);
            i0 = inc(i0);
            if (i0 == 0) {
              return;
            }
            i = i9;
            continue label0;
          }
        }
      }
    }
  }

  public void $FILL_ATTRIBUTE_BUFFER_24064() {
    int i = mem_24064;
    int i0 = ROOM_LAYOUT;
    while (true) {
      int i1 = this.$COPY_ROOM_ATTRIBUTE(_rlc(_rlc(mem[i0])), i);
      int i2 = this.$COPY_ROOM_ATTRIBUTE(_rrc(_rrc(_rrc(_rrc(mem[i0])))), i1);
      int i3 = this.$COPY_ROOM_ATTRIBUTE(_rrc(_rrc(mem[i0])), i2);
      i = this.$COPY_ROOM_ATTRIBUTE(mem[i0], i3);
      i0 = inc16(i0);
      if (flagZ(i0 & 255 & 128) != 0) {
        int i4 = mem[CONVEYOR_LENGTH];
        int i5 = i4 | i4;
        if (flagZ(i5) != 0) {
          int i6 = (mem[CONVEYOR_ATTR_ADDRESS + 1] << 8) + mem[CONVEYOR_ATTR_ADDRESS];
          int i7 = mem[CONVEYOR_TILE];
          do {
            mem[i6] = i7;
            i6 = inc16(i6);
            i5 = i5 - 1 & 255;
          } while (i5 != 0);
        }
        int i8 = mem[RAMP_LENGTH];
        if (flagZ(i8 | i8) != 0) {
          int i9 = (mem[RAMP_ATTR_ADDRESS + 1] << 8) + mem[RAMP_ATTR_ADDRESS];
          int i11 = add(_rlc(mem[RAMP_DEFINITION] & 1), 223);
          int i12 = mem[RAMP_LENGTH];
          int i13 = mem[RAMP_TILE];
          do {
            mem[i9] = i13;
            i9 = add16(i9, STACK_BASE | i11);
            i12 = i12 - 1 & 255;
          } while (i12 != 0);
        }
        return;
      }
    }
  }

  public int $COPY_ROOM_ATTRIBUTE(int i, int i0) {
    int i1 = i & 3;
    int i2 = add(add(_rlc(_rlc(_rlc(i1))), i1), 160);
    int i3 = mem[ROOM_LAYOUT | i2];
    mem[i0 + 0] = i3;
    return inc16(i0);
  }

  public void $MOVE_WILLY_1() {
    int i = dec(mem[ROPE_STATUS]) & 128;
    label2:
    {
      int i0 = 0;
      int i1 = 0;
      int i2 = 0;
      int i3 = 0;
      label8:
      {
        if (i == 0) {
          i0 = 0;
          i1 = 0;
          break label8;
        }
        label9:
        {
          if (mem[AIRBORNE_STATUS] != 1) {
            i0 = 0;
            break label9;
          }
          int i5 = mem[JUMPING_ANIMATION_COUNTER] & 254;
          int i6 = add(i5 - 8 & 255, mem[WILLY_Y]);
          mem[WILLY_Y] = i6;
          if (i6 >= 240) {
            this.$ENTER_ROOM_ABOVE();
            return;
          }
          int[] a = this.$UPDATE_WILLY_ATTRIBUTE(i6);
          int i7 = a[0];
          int i8 = a[1];
          int i9 = mem[WALL_TILE];
          label10:
          {
            label12:
            {
              label11:
              {
                if (i9 != mem[i7 << 8 | i8]) {
                  break label11;
                }
                break label12;
              }
              int i11 = inc16(i7 << 8 | i8);
              if (i9 != mem[i11]) {
                break label10;
              }
            }
            int i12 = add(mem[WILLY_Y], 16) & 240;
            mem[WILLY_Y] = i12;
            int[] a0 = this.$UPDATE_WILLY_ATTRIBUTE(i12);
            mem[AIRBORNE_STATUS] = 2;
            mem[WILLY_DIRECTION_FLAGS] = mem[WILLY_DIRECTION_FLAGS] & -3;
            return;
          }
          int i13 = inc(mem[JUMPING_ANIMATION_COUNTER]);
          mem[JUMPING_ANIMATION_COUNTER] = i13;
          int i14 = i13 - 8;
          int i15 = i14 & 255;
          if (i14 < 0) {
            i15 = -i15 & 255;
          }
          int i16 = _rlc(_rlc(_rlc(inc(i15))));
          int i17 = mem[BORDER_COLOUR];
          i0 = 32;
          do {
            i17 = i17 ^ 24;
            flagZ(i17);
            int i18 = i16;
            do {
              i18 = i18 - 1 & 255;
            } while (i18 != 0);
            i0 = dec(i0);
          } while (i0 != 0);
          int i19 = mem[JUMPING_ANIMATION_COUNTER];
          if (i19 == 18) {
            mem[AIRBORNE_STATUS] = 6;
            return;
          }
          if (i19 == 16) {
            break label9;
          }
          if (i19 == 13) {
            break label9;
          }
          break label2;
        }
        int i20 = flagZ(mem[WILLY_Y] & 14);
        label6:
        {
          if (i20 != 0) {
            break label6;
          }
          int i21 = add16((mem[WILLY_ATTR_BUFFER_ADDR + 1] << 8) + mem[WILLY_ATTR_BUFFER_ADDR], 64);
          if ((i21 >> 8 & 2) != 0) {
            int i22 = mem[mem_33004];
            mem[CURRENT_ROOM] = i22;
            mem[WILLY_Y] = 0;
            if (mem[AIRBORNE_STATUS] < 11) {
              mem[AIRBORNE_STATUS] = 2;
            }
            mem[WILLY_ATTR_BUFFER_ADDR] = mem[WILLY_ATTR_BUFFER_ADDR] & 31;
            mem[WILLY_ATTR_BUFFER_ADDR + 1] = 92;
            throw new RuntimeException(mem_38134 + "");
          }
          if (mem[NASTY_TILE] == mem[i21]) {
            break label6;
          }
          int i25 = inc16(i21);
          if (mem[NASTY_TILE] == mem[i25]) {
            break label6;
          }
          int i27 = cp(mem[ROOM_TILES], mem[i25]);
          i1 = this.dec16(i25);
          label7:
          {
            if (i27 == 0) {
              break label7;
            }
            break label8;
          }
          if (mem[ROOM_TILES] == mem[i1]) {
            break label6;
          }
          break label8;
        }
        if (mem[AIRBORNE_STATUS] == 1) {
          break label2;
        }
        mem[WILLY_DIRECTION_FLAGS] = mem[WILLY_DIRECTION_FLAGS] & -3;
        int i28 = mem[AIRBORNE_STATUS];
        int i29 = i28;
        if (flagZ(i29) == 0) {
          mem[AIRBORNE_STATUS] = 2;
          return;
        }
        int i30 = inc(i29);
        if (i30 == 16) {
          i30 = 12;
        }
        mem[AIRBORNE_STATUS] = i30;
        int i31 = _rlc(_rlc(_rlc(_rlc(i30))));
        int i32 = mem[BORDER_COLOUR];
        int i33 = 32;
        while (true) {
          i32 = i32 ^ 24;
          int i34 = i31;
          while (true) {
            i34 = i34 - 1 & 255;
            if (i34 == 0) {
              i33 = dec(i33);
              if (i33 == 0) {
                int i35 = add(mem[WILLY_Y], 8);
                mem[WILLY_Y] = i35;
                int[] a1 = this.$UPDATE_WILLY_ATTRIBUTE(i35);
                return;
              }
              break;
            }
          }
        }
      }
      label5:
      if ((dec(mem[ROPE_STATUS]) & 128) == 0) {
        i2 = 255;
      } else {
        int i36 = mem[AIRBORNE_STATUS];
        if (i36 >= 12) {
          throw new RuntimeException(COLLISION_FATAL_2 + "");
        }
        int i37 = 0;
        mem[AIRBORNE_STATUS] = i37;
        int i38 = mem[CONVEYOR_TILE];
        label3:
        {
          label4:
          {
            if (i38 == mem[i1]) {
              break label4;
            }
            int i40 = inc16(i1);
            if (i38 != mem[i40]) {
              break label3;
            }
          }
          i2 = mem[CONVEYOR_DIRECTION] - 3 & 255;
          break label5;
        }
        i2 = 255;
      }
      int i41 = in(mem_57342) & 31;
      int i42 = i41 | 32;
      int i43 = i42 & i2;
      int i44 = mem[GAME_MODE] & 2;
      int i45 = _rrc(i44) ^ i43;
      int i46 = in(mem_64510) & 31;
      int i47 = _rlc(i46) | 1;
      int i48 = i47 & i45;
      int i49 = _rrc(in(231 << 8 | mem_64510 & 255)) | 247;
      int i50 = i49 & i48;
      int i51 = in(239 << 8 | mem_64510 & 255) | 251;
      int i52 = i51 & i50;
      int i53 = _rrc(in(239 << 8 | i0)) | 251;
      int i54 = i53 & i52;
      int i55 = mem[KEMPSTON_JOYSTICK];
      if (flagZ(i55 | i55) != 0) {
        int i56 = in(31) & 3;
        i54 = (i56 ^ -1) & i54;
      }
      int i57 = i54 & 42;
      if (i57 == 42) {
        i3 = 0;
      } else {
        int i58 = 0;
        mem[INACTIVITY_TIMER] = i58;
        i3 = 4;
      }
      int i59 = i54 & 21;
      if (i59 != 21) {
        i3 = i3 | 8;
        int i60 = 0;
        mem[INACTIVITY_TIMER] = i60;
      }
      mem[WILLY_DIRECTION_FLAGS] = mem[add16(LEFT_RIGHT_MOVEMENT_TABLE, 0 << 8 | add(mem[WILLY_DIRECTION_FLAGS], i3))];
      int i63 = in(KEYBOARD_PORT_2) & 31;
      label0:
      {
        if (i63 != 31) {
          break label0;
        }
        if ((in(239 << 8 | KEYBOARD_PORT_2 & 255) & 1) == 0) {
          break label0;
        }
        int i65 = mem[KEMPSTON_JOYSTICK];
        int i66 = flagZ(i65 | i65);
        label1:
        {
          if (i66 != 0) {
            break label1;
          }
          break label2;
        }
        if ((in(31) & 16) != 0) {
          break label0;
        }
        break label2;
      }
      if ((mem[GAME_MODE] & 2) == 0) {
        mem[JUMPING_ANIMATION_COUNTER] = 0;
        mem[INACTIVITY_TIMER] = 0;
        mem[AIRBORNE_STATUS] = inc(0);
        if ((dec(mem[ROPE_STATUS]) & 128) == 0) {
          mem[ROPE_STATUS] = 240;
          mem[WILLY_Y] = mem[WILLY_Y] & 240;
          mem[WILLY_DIRECTION_FLAGS] = mem[WILLY_DIRECTION_FLAGS] | 2;
          return;
        }
      }
    }
    if (flagZ(mem[WILLY_DIRECTION_FLAGS] & 2) == 0) {
      return;
    }
    if ((dec(mem[ROPE_STATUS]) & 128) == 0) {
      return;
    }
    if (flagZ(mem[WILLY_DIRECTION_FLAGS] & 1) == 0) {
      int i71 = 0;
      int i72 = mem[WILLY_ANIMATION_FRAME];
      if (i72 != 3) {
        mem[WILLY_ANIMATION_FRAME] = inc(i72);
        return;
      }
      int i74 = mem[AIRBORNE_STATUS];
      if (flagZ(i74 | i74) != 0) {
        i71 = 0;
      } else {
        int i75 = (mem[WILLY_ATTR_BUFFER_ADDR + 1] << 8) + mem[WILLY_ATTR_BUFFER_ADDR];
        int i76 = dec(mem[RAMP_DEFINITION]) | 157;
        int i77 = i76 ^ 191;
        int i78 = add16(i75, 0 << 8 | i77);
        if (mem[RAMP_TILE] != mem[i78]) {
          i71 = 0;
        } else {
          int i79 = mem[RAMP_DEFINITION];
          i71 = (flagZ(i79 | i79) == 0) ? 32 : mem_65504;
        }
      }
      int i80 = inc16(inc16(add16((mem[WILLY_ATTR_BUFFER_ADDR + 1] << 8) + mem[WILLY_ATTR_BUFFER_ADDR], i71)));
      if (flagZ(i80 & 255 & 31) == 0) {
        mem[CURRENT_ROOM] = mem[mem_33002];
        mem[WILLY_ATTR_BUFFER_ADDR] = mem[WILLY_ATTR_BUFFER_ADDR] & 224;
        throw new RuntimeException(mem_38061 + "");
      }
      int i83 = mem[WALL_TILE];
      int i84 = add16(i80, 32);
      if (i83 == mem[i84]) {
        return;
      }
      int i85 = add(mem[WILLY_Y], this.sra(i71 & 255));
      if (flagZ(i85 & 15) != 0) {
        int i86 = mem[WALL_TILE];
        int i87 = add16(i84, 32);
        if (i86 == mem[i87]) {
          return;
        }
        i84 = i87 - 32 - this.carry(flagZ(i86 | i86)) & MEMORY_END;
      }
      int i89 = mem[WALL_TILE];
      int i90 = i84 - 32 - this.carry(flagZ(i89)) & MEMORY_END;
      if (i89 == mem[i90]) {
        return;
      }
      int i91 = this.dec16(i90);
      mem[WILLY_ATTR_BUFFER_ADDR] = i91 & 255;
      mem[WILLY_ATTR_BUFFER_ADDR + 1] = i91 >>> 8;
      mem[WILLY_ANIMATION_FRAME] = 0;
      mem[WILLY_Y] = i85;
      return;
    } else {
      int i93 = 0;
      int i95 = mem[WILLY_ANIMATION_FRAME];
      if (flagZ(i95) != 0) {
        int i96 = dec(i95);
        mem[WILLY_ANIMATION_FRAME] = i96;
        return;
      }
      if (mem[AIRBORNE_STATUS] != 0) {
        i93 = 0;
      } else {
        int i97 = (mem[WILLY_ATTR_BUFFER_ADDR + 1] << 8) + mem[WILLY_ATTR_BUFFER_ADDR];
        int i98 = dec(mem[RAMP_DEFINITION]) | 161;
        int i99 = i98 ^ 224;
        int i100 = add16(i97, 0 << 8 | i99);
        if (mem[RAMP_TILE] != mem[i100]) {
          i93 = 0;
        } else {
          int i101 = mem[RAMP_DEFINITION];
          i93 = (flagZ(i101 | i101) != 0) ? 32 : mem_65504;
        }
      }
      int i102 = (mem[WILLY_ATTR_BUFFER_ADDR + 1] << 8) + mem[WILLY_ATTR_BUFFER_ADDR];
      if (flagZ(i102 & 255 & 31) == 0) {
        int i103 = mem[ROOM_EXITS];
        mem[CURRENT_ROOM] = i103;
        mem[WILLY_ATTR_BUFFER_ADDR] = (mem[WILLY_ATTR_BUFFER_ADDR] | 31) & 254;
        throw new RuntimeException(mem_38043 + "");
      }
      int i106 = add16(this.dec16(add16(i102, i93)), 32);
      if (mem[WALL_TILE] == mem[i106]) {
        return;
      }
      int i107 = add(mem[WILLY_Y], this.sra(i93 & 255));
      int i108 = i107 & 15;
      if (flagZ(i108) != 0) {
        int i109 = mem[WALL_TILE];
        int i110 = add16(i106, 32);
        if (i109 == mem[i110]) {
          return;
        }
        i108 = i109;
        i106 = i110 - 32 - this.carry(flagZ(i108)) & MEMORY_END;
      }
      int i111 = i106 - 32 - this.carry(flagZ(i108 | i108)) & MEMORY_END;
      mem[WILLY_ATTR_BUFFER_ADDR] = i111 & 255;
      mem[WILLY_ATTR_BUFFER_ADDR + 1] = i111 >>> 8;
      mem[WILLY_Y] = i107;
      mem[WILLY_ANIMATION_FRAME] = 3;
      return;
    }
  }

  public int[] $UPDATE_WILLY_ATTRIBUTE(int i) {
    int i1 = 0;
    int[] a = this._rl(i & 240, flagZ(i1));
    int i4 = i1 + 92 + (this.carry(a[1]) & 255) & 255;
    int i7 = i4 << 8 | (mem[WILLY_ATTR_BUFFER_ADDR] & 31 | a[0]);
    mem[WILLY_ATTR_BUFFER_ADDR] = i7 & 255;
    mem[WILLY_ATTR_BUFFER_ADDR + 1] = i7 >>> 8;
    int[] a0 = new int[2];
    a0[0] = i4;
    a0[1] = mem[WILLY_ATTR_BUFFER_ADDR] & 31 | a[0];
    return a0;
  }

  public void $MOVE_ROPE_AND_GUARDIANS() {
    int i = ENTITY_BUFFERS;
    while (true) {
      int i0 = mem[i];
      if (i0 == 255) {
        return;
      }
      int i1 = i0 & 3;
      label0:
      if (flagZ(i1) != 0) {
        if (i1 == 1) {
          if ((mem[i] & 128) != 0) {
            int i2 = add(mem[i], 32) | 128;
            mem[i] = i2;
            if (i2 < 160) {
              int i3 = mem[i + 2] & 31;
              if (i3 == mem[i + 7]) {
                mem[i] = 97;
              } else {
                int i4 = i + 2;
                mem[i4] = inc(mem[i4]);
              }
            }
          } else {
            mem[i] = mem[i] - 32 & 255 & 127;
            if ((mem[i] - 32 & 255 & 127) > 96) {
              if ((mem[i + 2] & 31) == mem[i + 6]) {
                mem[i] = 129;
              } else {
                int i8 = i + 2;
                mem[i8] = dec(mem[i8]);
              }
            }
          }
        } else if (i1 == 2) {
          int i10 = mem[i] ^ 8;
          mem[i] = i10;
          if (flagZ(i10 & 24) != 0) {
            int i11 = add(mem[i], 32);
            mem[i] = i11;
          }
          int i12 = add(mem[i + 3], mem[i + 4]);
          mem[i + 3] = i12;
          label2:
          {
            if (i12 >= mem[i + 7]) {
              break label2;
            }
            int i14 = cp(i12, mem[i + 6]);
            {
              label1:
              {
                if (i14 == 0) {
                  break label1;
                }
                if (i14 >= 0) {
                  break label0;
                }
              }
              int i15 = mem[i + 6];
              mem[i + 3] = i15;
              break label2;
            }
          }
          int i16 = -mem[i + 4] & 255;
          mem[i + 4] = i16;
        } else {
          int i17 = 0;
          if ((mem[i] & 128) == 0) {
            int i18 = mem[i + 1];
            if ((i18 & 128) != 0) {
              i17 = add(i18, 2);
              if (i17 < 146) {
                i17 = add(i17, 2);
              }
            } else {
              i17 = i18 - 2 & 255;
              if (i17 < 20) {
                i17 = i17 - 2 & 255;
                if (flagZ(i17) == 0) {
                  i17 = 128;
                }
              }
            }
          } else {
            int i20 = mem[i + 1];
            if ((i20 & 128) == 0) {
              i17 = add(i20, 2);
              if (i17 < 18) {
                i17 = add(i17, 2);
              }
            } else {
              i17 = i20 - 2 & 255;
              if (i17 < 148) {
                i17 = i17 - 2 & 255;
                if (i17 == 128) {
                  i17 = 0;
                }
              }
            }
          }
          mem[i + 1] = i17;
          int i21 = i17 & 127;
          if (i21 == mem[i + 7]) {
            int i22 = mem[i] ^ 128;
            mem[i] = i22;
          }
        }
      }
      i = add16(i, 8);
    }
  }

  public void $DRAW_ROPE_ARROWS_GUARDIANS() {
    int i = ENTITY_BUFFERS;
    int i0 = 0;
    int i1 = 0;
    int i2 = 0;
    while (true) {
      int i3 = mem[i + 0];
      if (i3 == 255) {
        return;
      }
      int i4 = i3 & 7;
      if (flagZ(i4) != 0) {
        if (i4 == 3) {
          mem[i + 9] = 0;
          int i5 = mem[i + 2];
          mem[i + 3] = i5;
          mem[i + 5] = 128;
          int i6 = SCREEN_BUFFER_ADDRESS_LOOKUP_TABLE;
          while (true) {
            add(mem[i6 + 0], mem[i + 3]);
            int dummy = mem[i6 + 1];
            int i7 = mem[ROPE_STATUS];
            int i8 = i7 | i7;
            int i9 = flagZ(i8);
            label0:
            {
              label1:
              {
                if (i9 != 0) {
                  break label1;
                }
                int i10 = flagZ(mem[i + 5] & mem[i0]);
                {
                  if (i10 == 0) {
                    break label0;
                  }
                  i8 = mem[i + 9];
                  mem[ROPE_STATUS] = i8;
                  int i11 = i + 11;
                  mem[i11] = mem[i11] | 1;
                  break label1;
                }
              }
              if (i8 == mem[i + 9] && (mem[i + 11] & 1) != 0) {
                int i12 = mem[i + 3];
                int i13 = mem[i + 5];
                if (i13 > 4 && i13 > 16) {
                  dec(i12);
                  if (i13 > 64) {
                  }
                }
                mem[WILLY_ANIMATION_FRAME] = i2 & 255;
                mem[WILLY_ATTR_BUFFER_ADDR] = i2 >>> 8;
                int i14 = (i6 & 255) - 16 & 255;
                mem[WILLY_Y] = i14;
                this.push(i0);
                int[] a = this.$UPDATE_WILLY_ATTRIBUTE(i14);
                i0 = this.pop();
              }
            }
            int i15 = mem[i + 5] | mem[i0];
            mem[i0] = i15;
            add(mem[i + 9], mem[i + 1]);
            int dummy2 = mem[i0];
            i6 = add16(i6, i1);
            int i16 = mem[i0];
            int i17 = i16 | i16;
            if (flagZ(i17) != 0) {
              if ((mem[i + 1] & 128) == 0) {
                do {
                  int i18 = i + 5;
                  mem[i18] = _rrc(mem[i18]);
                  if ((mem[i + 5] & 128) != 0) {
                    int i19 = i + 3;
                    int i20 = inc(mem[i19]);
                    mem[i19] = i20;
                  }
                  i17 = i17 - 1 & 255;
                } while (i17 != 0);
              } else {
                do {
                  int i21 = i + 5;
                  mem[i21] = _rlc(mem[i21]);
                  if ((mem[i + 5] & 1) != 0) {
                    int i22 = i + 3;
                    int i23 = dec(mem[i22]);
                    mem[i22] = i23;
                  }
                  i17 = i17 - 1 & 255;
                } while (i17 != 0);
              }
            }
            if (mem[i + 9] == mem[i + 4]) {
              break;
            }
            int i24 = i + 9;
            int i25 = inc(mem[i24]);
            mem[i24] = i25;
          }
          int i26 = mem[ROPE_STATUS];
          if ((i26 & 128) == 0) {
            if ((mem[i + 11] & 1) != 0) {
              int i27 = mem[WILLY_DIRECTION_FLAGS];
              if ((i27 & 2) != 0) {
                int i28 = _rrc(i27) ^ mem[i + 0];
                int i29 = _rlc(_rlc(i28)) & 2;
                int i30 = add(dec(i29), mem[ROPE_STATUS]);
                mem[ROPE_STATUS] = i30;
                int i31 = mem[mem_33003];
                if (mem[CURRENT_ROOM] == i31 && mem[ROPE_STATUS] < 15) {
                  mem[ROPE_STATUS] = 15;
                }
                if (mem[ROPE_STATUS] > mem[i + 4]) {
                  mem[ROPE_STATUS] = 240;
                  int i32 = mem[WILLY_Y] & 248;
                  mem[WILLY_Y] = i32;
                  int i33 = i32 ^ i32;
                  mem[AIRBORNE_STATUS] = i33;
                }
                i0 = ROPE_STATUS;
              }
            }
          } else {
            int i34 = inc(i26);
            mem[ROPE_STATUS] = i34;
            int i35 = i + 11;
            mem[i35] = mem[i35] & -2;
          }
        } else if (i4 == 4) {
          int i36 = 0;
          if ((mem[i + 0] & 128) != 0) {
            int i37 = i + 4;
            int i38 = inc(mem[i37]);
            mem[i37] = i38;
            i36 = 244;
          } else {
            int i39 = i + 4;
            int i40 = dec(mem[i39]);
            mem[i39] = i40;
            i36 = 44;
          }
          int i41 = mem[i + 4];
          if (i41 == i36) {
            int i42 = mem[BORDER_COLOUR];
            i2 = 640;
            while (true) {
              i42 = i42 ^ 24;
              do {
                i2 = i2 & 255 | ((i2 >> 8) - 1 & 255 & 255) << 8;
              } while (i2 >> 8 != 0);
              int i43 = i2 & 255 | (i2 & 255 & 255) << 8;
              int i44 = dec(i43 & 255);
              i2 = i43 & STACK_BASE | i44;
              if (i44 == 0) {
                break;
              }
            }
          } else if (flagZ(i41 & 224) == 0) {
            int i45 = mem[i + 2];
            int i46 = add(mem[SCREEN_BUFFER_ADDRESS_LOOKUP_TABLE | i45], mem[i + 4]);
            int i47 = i45 & 128;
            int i48 = _rlc(i47) | 92;
            int i49 = i48 & 255;
            mem[i + 5] = 0;
            int i50 = mem[i49 << 8 | i46] & 7;
            if (i50 == 7) {
              int i51 = i + 5;
              int i52 = dec(mem[i51]);
              mem[i51] = i52;
            }
            int i53 = mem[i49 << 8 | i46] | 7;
            mem[i49 << 8 | i46] = i53;
            int i54 = inc16(SCREEN_BUFFER_ADDRESS_LOOKUP_TABLE | i45);
            int i55 = dec(mem[i54] & 255) & 255;
            int i56 = mem[i + 6];
            mem[i55 << 8 | i46] = i56;
            int i57 = inc(i55) & 255;
            if (flagZ(mem[i57 << 8 | i46] & mem[i + 5]) != 0) {
              throw new RuntimeException(COLLISION_FATAL_2 + "");
            }
            mem[i57 << 8 | i46] = 255;
            int i58 = inc(i57) & 255;
            int i59 = mem[i + 6];
            mem[i58 << 8 | i46] = i59;
          }
        } else {
          int i60 = mem[i + 3];
          int i63 = add(mem[i + 2] & 31, mem[SCREEN_BUFFER_ADDRESS_LOOKUP_TABLE | i60]);
          int i66 = (_rlc(i60) & 1 | 92) & 255;
          int i67 = mem[i + 1] & 15;
          int i70 = mem[i66 << 8 | i63] & 56 ^ add(i67, 56) & 71;
          mem[i66 << 8 | i63] = i70;
          int i71 = inc16(i66 << 8 | i63);
          mem[i71] = i70;
          int i72 = add16(i71, 31);
          mem[i72] = i70;
          int i73 = inc16(i72);
          mem[i73] = i70;
          if (flagZ(mem[i + 3] & 14) != 0) {
            int i74 = add16(i73, 31);
            mem[i74] = i70;
            mem[inc16(i74)] = i70;
          }
          int i78 = (mem[i + 1] & mem[i] | mem[i + 2]) & 224;
          int i79 = mem[i + 5];
          int i80 = mem[i + 3];
          int i82 = mem[i + 2] & 31 | mem[SCREEN_BUFFER_ADDRESS_LOOKUP_TABLE | i80];
          i0 = (mem[inc16(SCREEN_BUFFER_ADDRESS_LOOKUP_TABLE | i80)] & 255) << 8 | i82;
          int[] a0 = $DRAW_SPRITE(1, i79 << 8 | i78, i0);
          int i84 = a0[0];
          if (i84 != 0) {
            throw new RuntimeException(COLLISION_FATAL_2 + "");
          }
        }
      }
      i = add16(i, 8);
      i1 = 8;
    }
  }

  public void $DRAW_ITEMS_AND_COLLECT() {
    int i = mem[FIRST_ITEM_INDEX];
    int i0 = 164;
    while (true) {
      int i1 = mem[i0 << 8 | i] & -129;
      int i2 = mem[CURRENT_ROOM] | 64;
      if (i2 == i1) {
        int i3 = _rlc(mem[i0 << 8 | i]) & 1;
        int i4 = add(i3, 92);
        int i5 = inc(i0) & 255;
        int i6 = mem[i5 << 8 | i];
        i0 = dec(i5) & 255;
        int i7 = mem[i4 << 8 | i6] & 7;
        if (i7 == 7) {
          int i8 = ITEMS_COLLECTED;
          while (true) {
            int i9 = i8 + 2;
            int i10 = inc(mem[i9]);
            mem[i9] = i10;
            if (mem[i8 + 2] != 58) {
              break;
            }
            mem[i8 + 2] = 48;
            i8 = this.dec16(i8);
          }
          int i11 = mem[BORDER_COLOUR];
          int i12 = 128;
          do {
            i11 = i11 ^ 24;
            int i13 = 144 - i12 & 255;
            do {
              i13 = i13 - 1 & 255;
            } while (i13 != 0);
            i12 = dec(dec(i12));
          } while (i12 != 0);
          int i14 = inc(mem[ITEMS_REMAINING_256_MINUS]);
          mem[ITEMS_REMAINING_256_MINUS] = i14;
          if (i14 == 0) {
            mem[GAME_MODE] = 1;
          }
          mem[i0 << 8 | i] = mem[i0 << 8 | i] & -65;
        } else {
          int i15 = add(mem[MINUTE_COUNTER], i) & 3;
          int i16 = add(i15, 3);
          int i17 = mem[i4 << 8 | i6] & 248;
          int i18 = i17 | i16;
          mem[i4 << 8 | i6] = i18;
          int i19 = _rlc(_rlc(_rlc(_rlc(mem[i0 << 8 | i])))) & 8;
          int i20 = add(i19, 96);
          this.push(i0 << 8 | i);
          $COPY_BYTES(8, i20 << 8 | i6, ITEM_GRAPHIC);
          i = this.pop();
          i0 = i >> 8;
        }
      }
      i = inc(i);
      if (i == 0) {
        return;
      }
    }
  }

  public static int[] $DRAW_SPRITE(int i, int i0, int i1) {
    int i2 = 16;
    while (true) {
      int i3 = i & 1;
      int i4 = mem[i0];
      if (i3 != 0) {
        int i5 = flagZ(i4 & mem[i1]);
        if (i5 != 0) {
          int[] a = new int[2];
          a[0] = i5;
          a[1] = i0;
          return a;
        }
        i4 = mem[i0] | mem[i1];
      }
      mem[i1] = i4;
      int i6 = i1 & STACK_BASE | inc(i1 & 255);
      int i7 = inc16(i0);
      int i8 = i & 1;
      int i9 = mem[i7];
      if (i8 != 0) {
        int i10 = flagZ(i9 & mem[i6]);
        if (i10 != 0) {
          int[] a0 = new int[2];
          a0[0] = i10;
          a0[1] = i7;
          return a0;
        }
        i9 = mem[i7] | mem[i6];
      }
      mem[i6] = i9;
      int i11 = i6 & STACK_BASE | dec(i6 & 255);
      int i12 = i11 & 255 | inc(i11 >> 8) << 8;
      int i13 = inc16(i7);
      int i14 = i12 >> 8 & 7;
      if (flagZ(i14) == 0) {
        int i15 = i12 & 255 | ((i12 >> 8) - 8 & 255) << 8;
        int i16 = add(i15 & 255, 32);
        i12 = i15 & STACK_BASE | i16;
        i14 = i16 & 224;
        if (flagZ(i14) == 0) {
          i14 = add(i12 >> 8, 8);
          i12 = i12 & 255 | i14 << 8;
        }
      }
      i2 = i2 - 1 & 255;
      if (i2 == 0) {
        int i17 = flagZ(i14 ^ i14);
        int[] a1 = new int[2];
        a1[0] = i17;
        a1[1] = i13;
        return a1;
      }
      i0 = i13;
      i1 = i12;
    }
  }

  public void $ENTER_ROOM_ABOVE() {
    mem[CURRENT_ROOM] = mem[mem_33003];
    mem[WILLY_ATTR_BUFFER_ADDR] = add(mem[WILLY_ATTR_BUFFER_ADDR] & 31, 160);
    mem[WILLY_ATTR_BUFFER_ADDR + 1] = 93;
    mem[WILLY_Y] = 208;
    mem[AIRBORNE_STATUS] = 0;
    throw new RuntimeException(mem_38095 + "");
  }

  public void $MOVE_CONVEYOR() {
    int i = (mem[CONVEYOR_ATTR_ADDRESS + 1] << 8) + mem[CONVEYOR_ATTR_ADDRESS];
    int i0 = add(_rlc(_rlc(_rlc(i >> 8 & 1))), 112) & 255;
    int i1 = mem[CONVEYOR_LENGTH];
    if (flagZ(i1) != 0) {
      int i2 = 0;
      int i3 = 0;
      int i4 = 0;
      if (flagZ(mem[CONVEYOR_DIRECTION]) == 0) {
        i2 = _rlc(_rlc(mem[i0 << 8 | i & 255]));
        i3 = inc(inc(i0) & 255) & 255;
        i4 = _rrc(_rrc(mem[i3 << 8 | i & 255]));
      } else {
        i2 = _rrc(_rrc(mem[i0 << 8 | i & 255]));
        i3 = inc(inc(i0) & 255) & 255;
        i4 = _rlc(_rlc(mem[i3 << 8 | i & 255]));
      }
      int i5 = i & 255;
      int i6 = 0;
      do {
        mem[i0 << 8 | i5] = i2;
        mem[i3 << 8 | i6] = i4;
        i6 = inc(i & 255);
        i5 = inc(i5);
        i1 = i1 - 1 & 255;
      } while (i1 != 0);
    }
  }

  public void $SPECIAL_ROOM_HANDLER() {
    if (mem[CURRENT_ROOM] != 35) {
      if (mem[CURRENT_ROOM] == 33) {
        int i = mem[MINUTE_COUNTER] & 1;
        int i0 = _rrc(_rrc(_rrc(i)));
        if (mem[GAME_MODE] == 3) {
          i0 = i0 | 64;
        }
        this.$OR_BYTES_TO_BUFFER(16, 28, 166, i0, mem_33488);
        mem[mem_23996] = 7;
        mem[mem_23997] = 7;
        mem[mem_24028] = 7;
        mem[mem_24029] = 7;
      }
    } else {
      if (flagZ(mem[GAME_MODE]) != 0) {
        if ((mem[WILLY_ATTR_BUFFER_ADDR] & 31) < 6) {
          mem[GAME_MODE] = 2;
        }
      } else {
        int i3 = mem[MINUTE_COUNTER] & 2;
        flagZ(_rrc(_rrc(_rrc(_rrc(i3)))) | 128);
        int i4 = mem[WILLY_Y];
        if (i4 != 208 && i4 < 192) {
        }
        int[] a = $DRAW_SPRITE(1, 0, mem_26734);
        int i5 = a[0];
        if (i5 != 0) {
          throw new RuntimeException(COLLISION_FATAL_2 + "");
        }
        mem[mem_23918] = 69;
        mem[mem_23919] = 69;
        mem[mem_23950] = 7;
        mem[mem_23951] = 7;
      }
    }
  }

  public void $CHECK_TOILET() {
    if (mem[CURRENT_ROOM] == 33) {
      int i = mem[WILLY_ATTR_BUFFER_ADDR];
      if (i == 188) {
        mem[MINUTE_COUNTER] = 0;
        mem[GAME_MODE] = 3;
      }
    }
  }

  public void $SET_WILLY_ATTRIBUTES() {
    int i = 0;
    int i0 = 0;
    int i1 = (mem[WILLY_ATTR_BUFFER_ADDR + 1] << 8) + mem[WILLY_ATTR_BUFFER_ADDR];
    int i2 = mem[RAMP_DEFINITION] & 1;
    int i3 = add16(i1, ((0 | add(i2, 64)) != 0) ? 1 : 0);
    if (mem[RAMP_TILE] != mem[i3]) {
      i = 0;
    } else {
      int i4 = mem[AIRBORNE_STATUS];
      if (flagZ(i4 | i4) != 0) {
        i = 0;
      } else {
        int i5 = mem[WILLY_ANIMATION_FRAME] & 3;
        int i6 = _rlc(_rlc(i5));
        int i7 = mem[RAMP_DEFINITION] & 1;
        int i8 = dec(i7) ^ 12;
        int i9 = i8 ^ i6;
        i = i9 & 12;
      }
    }
    int i10 = (mem[WILLY_ATTR_BUFFER_ADDR + 1] << 8) + mem[WILLY_ATTR_BUFFER_ADDR];
    try {
      this.$CHECK_WILLY_CELL(15, i10);
    } catch (RuntimeException a) {
      if (Integer.parseInt(a.getMessage()) == COLLISION_FATAL_1) {
        throw new RuntimeException(COLLISION_FATAL_2 + "");
      }
    }
    int i11 = inc16(i10);
    this.$CHECK_WILLY_CELL(15, i11);
    int i12 = add16(i11, 31);
    this.$CHECK_WILLY_CELL(15, i12);
    int i13 = inc16(i12);
    this.$CHECK_WILLY_CELL(15, i13);
    int i14 = add(mem[WILLY_Y], i);
    int i15 = add16(i13, 31);
    this.$CHECK_WILLY_CELL(i14, i15);
    this.$CHECK_WILLY_CELL(i14, inc16(i15));
    int i16 = SCREEN_BUFFER_ADDRESS_LOOKUP_TABLE | add(mem[WILLY_Y], i);
    int i17 = mem[WILLY_DIRECTION_FLAGS] & 1;
    int i18 = _rrc(i17);
    int i19 = mem[WILLY_ANIMATION_FRAME] & 3;
    int i20 = _rrc(_rrc(_rrc(i19))) | i18;
    if (mem[CURRENT_ROOM] != 29) {
      i0 = 157;
    } else {
      i20 = i20 ^ 128;
      i0 = 182;
    }
    int i21 = mem[WILLY_ATTR_BUFFER_ADDR] & 31;
    this.$OR_BYTES_TO_BUFFER(16, i21, i0, i20, i16);
  }

  public void $CHECK_WILLY_CELL(int i, int i0) {
    if (mem[ROOM_TILES] == mem[i0] && flagZ(i & 15) != 0) {
      int i1 = mem[ROOM_TILES] | 7;
      mem[i0] = i1;
    }
    if (mem[NASTY_TILE] == mem[i0]) {
      throw new RuntimeException(COLLISION_FATAL_1 + "");
    }
  }

  public void $OR_BYTES_TO_BUFFER(int i, int i0, int i1, int i2, int i3) {
    int i4 = i1 << 8 | i2;
    while (true) {
      int i5 = mem[i3 + 0];
      int i6 = mem[i3 + 1] & 255;
      int i7 = i5 | i0;
      int i8 = i6 << 8 | i7;
      int i9 = mem[i4] | mem[i8];
      mem[i8] = i9;
      int i10 = inc16(i8);
      int i11 = inc16(i4);
      int i12 = mem[i11] | mem[i10];
      mem[i10] = i12;
      int i13 = inc16(inc16(i3));
      i4 = inc16(i11);
      int i14 = i - 1 & 255;
      if (i14 == 0) {
        return;
      }
      i = i14;
      i3 = i13;
    }
  }

  public static void $PRINT_MESSAGE(int length, int displayAddress, int messageAddress) {
    while (true) {
      int i2 = $PRINT_CHAR(mem[messageAddress], displayAddress);
      int i3 = inc16(messageAddress);
      int i4 = i2 & STACK_BASE | inc(i2 & 255);
      int i5 = i4 & 255 | ((i4 >> 8) - 8 & 255) << 8;
      int i6 = dec(length);
      if (i6 == 0) {
        return;
      }
      length = i6;
      displayAddress = i5;
      messageAddress = i3;
    }
  }

  public static int $PRINT_CHAR(int character, int displayAddress) {
    int i1 = character | 128;
    int i2 = add16(UDG_BASE | i1, UDG_BASE | i1);
    int i3 = add16(i2, i2);
    return $COPY_BYTES(8, displayAddress, add16(i3, i3));
  }

  public static int $COPY_BYTES(int count, int displayAddress, int sourceAddress) {
    while (true) {
      int i2 = mem[sourceAddress];
      mem[displayAddress] = i2;
      int i3 = inc16(sourceAddress);
      int i4 = displayAddress & 255 | inc(displayAddress >> 8) << 8;
      int i5 = count - 1 & 255;
      if (i5 == 0) {
        return i4;
      }
      count = i5;
      displayAddress = i4;
      sourceAddress = i3;
    }
  }

  public int $PLAY_THEME_TUNE(int i) {
    while (true) {
      int i0 = mem[i];
      int i1 = cp(i0, 255);
      if (i1 == 0) {
        return i1;
      }
      int i2 = 0;
      int i3 = mem[i];
      int i4 = i3;
      int i5 = 100;
      while (true) {
        int i6 = dec(i4);
        if (i6 != 0) {
          i4 = i6;
        } else {
          i2 = i2 ^ 24;
          i6 = flagZ(i2);
          i4 = i3;
        }
        i5 = i5 & 255 | ((i5 >> 8) - 1 & 255 & 255) << 8;
        if (i5 >> 8 == 0) {
          int i7 = i2 << 8 | i6;
          int i8 = cp(i5 & 255, 50);
          if (i8 == 0) {
            int[] a = this._rl(i3, i8);
            i3 = a[0];
          }
          i2 = i7 >> 8;
          int i9 = dec(i5 & 255);
          i5 = i5 & STACK_BASE | i9;
          if (i9 == 0) {
            int i10 = this.$CHECK_ENTER_FIRE();
            if (i10 != 0) {
              return i10;
            }
            i = inc16(i);
            break;
          }
        }
      }
    }
  }

  public int $CHECK_ENTER_FIRE() {
    int i0 = mem[KEMPSTON_JOYSTICK];
    if (flagZ(i0) != 0) {
      int i1 = in(i0 << 8 | 31) & 16;
      if (i1 != 0) {
        return i1;
      }
    }
    return cp(in(mem_45054) & 1, 1);
  }

  public void $PLAY_INTRO_SOUND(int i) {
    int i0 = i;
    while (true) {
      int i1 = i;
      while (true) {
        if (i0 == i1) {
        }
        i1 = i1 - 1 & 255;
        if (i1 == 0) {
          i0 = dec(i0);
          if (i0 == 0) {
            return;
          }
          break;
        }
      }
    }
  }
}
