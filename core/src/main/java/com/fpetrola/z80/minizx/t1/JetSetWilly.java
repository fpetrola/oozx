/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.z80.minizx.t1;

import com.fpetrola.z80.minizx.MiniZX;

public class JetSetWilly extends MiniZX {
  public static void main(String[] args) {
    JetSetWilly jetSetWilly = new JetSetWilly();
//    jetSetWilly.setSyncChecker(new DefaultSyncChecker());
//    jetSetWilly.$34762(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0, 0, 0, 0);
    jetSetWilly.$34762();
  }

  public String getProgramBytes() {
    return "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";
  }

  public void $34762() {
    label241:
    while(true) {
      A = 0;
      mem[34254] = A;
      mem[34273] = A;
      mem[34253] = A;
      mem[34257] = A;
      mem[34251] = A;
      mem[34272] = A;
      mem[34271] = A;
      A = 7;
      mem[34252] = A;
      A = 208;
      mem[34255] = A;
      A = 33;
      mem[33824] = A;
      HL(23988);
      int var1 = HL();
      wMem16(34259, var1);
      HL(34172);
      int var2 = HL();
      mem[var2] = 48;
      int var3 = HL() + 1 & 65535;
      HL(var3);
      int var4 = HL();
      mem[var4] = 48;
      int var5 = HL() + 1 & 65535;
      HL(var5);
      int var6 = HL();
      mem[var6] = 48;
      H = 164;
      int var7 = mem[41983];
      A = var7;
      L = A;
      mem[34270] = A;

      do {
        int var8 = HL();
        int var9 = mem[var8] | 64;
        int var10 = HL();
        mem[var10] = var9;
        int var11 = L + 1 & 255;
        L = var11;
      } while(L != 0);

      HL(34274);
      int var12 = HL();
      int var13 = mem[var12] | 1;
      int var14 = HL();
      mem[var14] = var13;

      label233:
      while(true) {
        HL(16384);
        DE(16385);
        BC(6143);
        int var15 = HL();
        mem[var15] = 0;
        ldir();
        HL(38912);
        BC(768);
        ldir();
        HL(23136);
        DE(23137);
        BC(31);
        int var16 = HL();
        mem[var16] = 70;
        ldir();
        IX(33876);
        DE(20576);
        C = 32;
        $38528();
        DE(22528);

        do {
          int var17 = DE();
          int var18 = mem[var17];
          A = var18;
          if(A << 1 != 0 && A != 211 && A != 9 && A != 45 && A != 36) {
            C = 0;
            if(A != 8 && A != 41) {
              if(A != 44) {
                if(A != 5) {
                  C = 16;
                }
              } else {
                A = 37;
                int var223 = DE();
                mem[var223] = A;
              }
            }

            A = E;
            int var210 = A & 1;
            A = var210;
            int var211 = A;
            int var212 = rlc(var211);
            A = var212;
            int var213 = A;
            int var214 = rlc(var213);
            A = var214;
            int var215 = A;
            int var216 = rlc(var215);
            A = var216;
            int var217 = A | C;
            A = var217;
            C = A;
            B = 0;
            HL(33841);
            int var218 = BC();
            int var219 = HL() + var218 & 65535;
            HL(var219);
            int var220 = DE();
            push(var220);
            int var221 = D & 1;
            F = var221;
            D = 64;
            if(F != 0) {
              D = 72;
            }

            B = 8;
            $38555();
            int var222 = pop();
            DE(var222);
          }

          int var19 = DE() + 1 & 65535;
          DE(var19);
          A = D;
        } while(A != 90);

        BC(31);
        A = 0;

        do {
          int var20 = BC();
          int var21 = in(var20);
          E = var21;
          int var22 = A | E;
          A = var22;
          int var23 = B - 1 & 255;
          B = var23;
        } while(B != 0);

        int var24 = A & 32;
        A = var24;
        if(A << 1 == 0) {
          A = 1;
          mem[34254] = A;
        }

        HL(34299);
        $38562();
        if(F != 0) {
          break;
        }

        A = 0;
        mem[34276] = A;

        while(true) {
          $35563();
          HL(23136);
          DE(23137);
          BC(31);
          int var197 = HL();
          mem[var197] = 79;
          ldir();
          int var198 = mem[34276];
          A = var198;
          IX(33876);
          E = A;
          D = 0;
          int var199 = DE();
          int var200 = IX() + var199 & 65535;
          IX(var200);
          DE(20576);
          C = 32;
          $38528();
          int var201 = mem[34276];
          A = var201;
          int var202 = A & 31;
          A = var202;
          int var203 = A + 50 & 255;
          A = var203;
          $38622();
          BC(45054);
          int var204 = BC();
          int var205 = in(var204);
          A = var205;
          int var206 = A & 1;
          A = var206;
          if(A != 1) {
            break label233;
          }

          int var207 = mem[34276];
          A = var207;
          int var208 = A + 1 & 255;
          A = var208;
          int var209 = A - 224;
          F = var209;
          mem[34276] = A;
          if(F == 0) {
            break;
          }
        }
      }

      HL(34181);
      DE(34175);
      BC(6);
      ldir();
      HL(39424);
      DE(23040);
      BC(256);
      ldir();

      while(true) {
        while(true) {
          label252: {
            while(true) {
              int var25 = mem[33824];
              A = var25;
              int var26 = A | 192;
              A = var26;
              H = A;
              L = 0;
              DE(32768);
              BC(256);
              ldir();
              IX(33008);
              DE(33024);
              A = 8;

              do {
                int var27 = IX();
                int var28 = mem[var27];
                L = var28;
                int var29 = L & -129;
                L = var29;
                H = 20;
                int var30 = HL();
                int var31 = HL() + var30 & 65535;
                HL(var31);
                int var32 = HL();
                int var33 = HL() + var32 & 65535;
                HL(var33);
                int var34 = HL();
                int var35 = HL() + var34 & 65535;
                HL(var35);
                BC(2);
                ldir();
                int var36 = IX() + 1;
                int var37 = mem[var36];
                C = var37;
                int var38 = HL();
                mem[var38] = C;
                BC(6);
                ldir();
                int var39 = IX() + 1 & 65535;
                IX(var39);
                int var40 = IX() + 1 & 65535;
                IX(var40);
                int var41 = A - 1 & 255;
                A = var41;
              } while(A != 0);

              label248: {
                HL(34255);
                DE(34263);
                BC(7);
                ldir();
                $36147();
                HL(20480);
                DE(20481);
                BC(2047);
                int var42 = HL();
                mem[var42] = 0;
                ldir();
                IX(32896);
                C = 32;
                DE(20480);
                $38528();
                IX(34132);
                DE(20576);
                C = 32;
                $38528();
                int var43 = mem[32990];
                A = var43;
                C = 254;
                A = 0;
                mem[34262] = A;
                $35211();
                HL(24064);
                DE(23552);
                BC(512);
                ldir();
                HL(28672);
                DE(24576);
                BC(4096);
                ldir();
                $37056();
                int var44 = mem[34271];
                A = var44;
                if(A != 3) {
                  $36307();
                  if(isNextPC(37048)) {
                    break label248;
                  }

                  if(isNextPC(38043) || isNextPC(38061) || isNextPC(38134) || isNextPC(38095)) {
                    continue;
                  }
                }

                int var45 = mem[34255];
                A = var45;
                if(A >= 225) {
                  $38064();
                  if(isNextPC(38095)) {
                    continue;
                  }
                }

                int var46 = mem[34271];
                A = var46;
                if(A != 3) {
                  $38344();
                  if(isNextPC(37048)) {
                    break label248;
                  }
                }

                int var47 = mem[34271];
                A = var47;
                if(A == 2) {
                  $38276();
                }

                int var48 = A - 2;
                F = var48;
                $38196();
                if(!isNextPC(37048)) {
                  $37310();
                  if(!isNextPC(37048)) {
                    $38137();
                    $37841();
                    break;
                  }
                }
              }

              A = 255;
              mem[34257] = A;
              break;
            }

            HL(24576);
            DE(16384);
            BC(4096);
            ldir();
            int var49 = mem[34271];
            A = var49;
            int var50 = A & 2;
            A = var50;
            int var51 = A;
            int var52 = rrc(var51);
            A = var52;
            HL(34258);
            int var53 = HL();
            int var54 = mem[var53];
            int var55 = A | var54;
            A = var55;
            int var56 = HL();
            mem[var56] = A;
            int var57 = mem[34253];
            A = var57;
            if(A << 1 != 0) {
              int var188 = A - 1 & 255;
              A = var188;
              mem[34253] = A;
              int var189 = A;
              int var190 = rlc(var189);
              A = var190;
              int var191 = A;
              int var192 = rlc(var191);
              A = var192;
              int var193 = A;
              int var194 = rlc(var193);
              A = var194;
              int var195 = A & 56;
              A = var195;
              HL(23552);
              DE(23553);
              BC(511);
              int var196 = HL();
              mem[var196] = A;
              ldir();
            }

            HL(23552);
            DE(22528);
            BC(512);
            ldir();
            IX(34175);
            DE(20601);
            C = 6;
            $38528();
            IX(34172);
            DE(20592);
            C = 3;
            $38528();
            int var58 = mem[34251];
            A = var58;
            int var59 = A + 1 & 255;
            A = var59;
            F = A;
            mem[34251] = A;
            if(F == 0) {
              IX(34175);
              int var161 = IX() + 4;
              int var162 = mem[var161] + 1 & 255;
              mem[var161] = var162;
              int var163 = IX() + 4;
              int var164 = mem[var163];
              A = var164;
              if(A == 58) {
                int var165 = IX() + 4;
                mem[var165] = 48;
                int var166 = IX() + 3;
                int var167 = mem[var166] + 1 & 255;
                mem[var166] = var167;
                int var168 = IX() + 3;
                int var169 = mem[var168];
                A = var169;
                if(A == 54) {
                  int var170 = IX() + 3;
                  mem[var170] = 48;
                  int var171 = IX();
                  int var172 = mem[var171];
                  A = var172;
                  if(A == 49) {
                    int var179 = IX() + 1;
                    int var180 = mem[var179] + 1 & 255;
                    mem[var179] = var180;
                    int var181 = IX() + 1;
                    int var182 = mem[var181];
                    A = var182;
                    if(A == 51) {
                      int var183 = IX() + 5;
                      int var184 = mem[var183];
                      A = var184;
                      if(A == 112) {
                        continue label241;
                      }

                      int var185 = IX();
                      mem[var185] = 32;
                      int var186 = IX() + 1;
                      mem[var186] = 49;
                      int var187 = IX() + 5;
                      mem[var187] = 112;
                    }
                  } else {
                    int var173 = IX() + 1;
                    int var174 = mem[var173] + 1 & 255;
                    mem[var173] = var174;
                    int var175 = IX() + 1;
                    int var176 = mem[var175];
                    A = var176;
                    if(A == 58) {
                      int var177 = IX() + 1;
                      mem[var177] = 48;
                      int var178 = IX();
                      mem[var178] = 49;
                    }
                  }
                }
              }
            }

            BC(65278);
            int var60 = BC();
            int var61 = in(var60);
            A = var61;
            E = A;
            B = 127;
            int var62 = BC();
            int var63 = in(var62);
            A = var63;
            int var64 = A | E;
            A = var64;
            int var65 = A & 1;
            A = var65;
            if(A << 1 == 0) {
              continue label241;
            }

            int var66 = mem[34272];
            A = var66;
            int var67 = A + 1 & 255;
            A = var67;
            F = A;
            mem[34272] = A;
            if(F != 0) {
              B = 253;
              int var158 = BC();
              int var159 = in(var158);
              A = var159;
              int var160 = A & 31;
              A = var160;
              if(A == 31) {
                break label252;
              }

              DE(0);
            }

            while(true) {
              B = 2;
              int var68 = BC();
              int var69 = in(var68);
              A = var69;
              int var70 = A & 31;
              A = var70;
              if(A != 31) {
                HL(39424);
                DE(23040);
                BC(256);
                ldir();
                int var71 = mem[32990];
                A = var71;
                break;
              }

              int var154 = E + 1 & 255;
              E = var154;
              if(E == 0) {
                int var155 = D + 1 & 255;
                D = var155;
                if(D == 0) {
                  int var156 = mem[34275];
                  A = var156;
                  if(A != 10) {
                    $35563();
                  }

                  int var157 = A - 10;
                  F = var157;
                }
              }
            }
          }

          int var72 = mem[34257];
          A = var72;
          if(A != 255) {
            $35615();
            BC(63486);
            int var149 = BC();
            int var150 = in(var149);
            A = var150;
            int var151 = ~A;
            A = var151;
            int var152 = A & 31;
            A = var152;
            int var153 = A | D;
            A = var153;
            mem[33824] = A;
          } else {
            A = 71;

            do {
              HL(22528);
              DE(22529);
              BC(511);
              int var73 = HL();
              mem[var73] = A;
              ldir();
              E = A;
              int var74 = ~A;
              A = var74;
              int var75 = A & 7;
              A = var75;
              int var76 = A;
              int var77 = rlc(var76);
              A = var77;
              int var78 = A;
              int var79 = rlc(var78);
              A = var79;
              int var80 = A;
              int var81 = rlc(var80);
              A = var81;
              int var82 = A | 7;
              A = var82;
              D = A;
              C = E;
              int var83 = C;
              int var84 = rrc(var83);
              C = var84;
              int var85 = C;
              int var86 = rrc(var85);
              C = var86;
              int var87 = C;
              int var88 = rrc(var87);
              C = var88;
              int var89 = A | 16;
              A = var89;
              A = 0;

              do {
                int var90 = A ^ 24;
                A = var90;
                B = D;

                do {
                  int var91 = B - 1 & 255;
                  B = var91;
                } while(B != 0);

                int var92 = C - 1 & 255;
                C = var92;
              } while(C != 0);

              A = E;
              int var93 = A - 1 & 255;
              A = var93;
            } while(A != 63);

            HL(34252);
            int var94 = HL();
            int var95 = mem[var94];
            A = var95;
            if(A << 1 == 0) {
              HL(16384);
              DE(16385);
              BC(4095);
              int var96 = HL();
              mem[var96] = 0;
              ldir();
              A = 0;
              mem[34276] = A;
              DE(40256);
              HL(18575);
              C = 0;
              $37974();
              DE(40032);
              HL(18639);
              C = 0;
              $37974();

              do {
                int var97 = mem[34276];
                A = var97;
                C = A;
                B = 130;
                int var98 = BC();
                int var99 = mem[var98];
                A = var99;
                int var100 = A | 15;
                A = var100;
                L = A;
                int var101 = BC() + 1 & 65535;
                BC(var101);
                int var102 = BC();
                int var103 = mem[var102];
                A = var103;
                int var104 = A - 32 & 255;
                A = var104;
                H = A;
                DE(40000);
                C = 0;
                $37974();
                int var105 = mem[34276];
                A = var105;
                int var106 = ~A;
                A = var106;
                E = A;
                A = 0;
                BC(64);

                do {
                  int var107 = A ^ 24;
                  A = var107;
                  B = E;

                  do {
                    int var108 = B - 1 & 255;
                    B = var108;
                  } while(B != 0);

                  int var109 = C - 1 & 255;
                  C = var109;
                } while(C != 0);

                HL(22528);
                DE(22529);
                BC(511);
                int var110 = mem[34276];
                A = var110;
                int var111 = A & 12;
                A = var111;
                int var112 = A;
                int var113 = rlc(var112);
                A = var113;
                int var114 = A | 71;
                A = var114;
                int var115 = HL();
                mem[var115] = A;
                ldir();
                int var116 = A & 250;
                A = var116;
                int var117 = A | 2;
                A = var117;
                mem[22991] = A;
                mem[22992] = A;
                mem[23023] = A;
                mem[23024] = A;
                int var118 = mem[34276];
                A = var118;
                int var119 = A + 4 & 255;
                A = var119;
                mem[34276] = A;
              } while(A != 196);

              IX(34164);
              C = 4;
              DE(16586);
              $38528();
              IX(34168);
              C = 4;
              DE(16594);
              $38528();
              BC(0);
              D = 6;

              while(true) {
                do {
                  int var120 = B - 1 & 255;
                  B = var120;
                } while(B != 0);

                A = C;
                int var121 = A & 7;
                A = var121;
                int var122 = A | 64;
                A = var122;
                mem[22730] = A;
                int var123 = A + 1 & 255;
                A = var123;
                int var124 = A & 7;
                A = var124;
                int var125 = A | 64;
                A = var125;
                mem[22731] = A;
                int var126 = A + 1 & 255;
                A = var126;
                int var127 = A & 7;
                A = var127;
                int var128 = A | 64;
                A = var128;
                mem[22732] = A;
                int var129 = A + 1 & 255;
                A = var129;
                int var130 = A & 7;
                A = var130;
                int var131 = A | 64;
                A = var131;
                mem[22733] = A;
                int var132 = A + 1 & 255;
                A = var132;
                int var133 = A & 7;
                A = var133;
                int var134 = A | 64;
                A = var134;
                mem[22738] = A;
                int var135 = A + 1 & 255;
                A = var135;
                int var136 = A & 7;
                A = var136;
                int var137 = A | 64;
                A = var137;
                mem[22739] = A;
                int var138 = A + 1 & 255;
                A = var138;
                int var139 = A & 7;
                A = var139;
                int var140 = A | 64;
                A = var140;
                mem[22740] = A;
                int var141 = A + 1 & 255;
                A = var141;
                int var142 = A & 7;
                A = var142;
                int var143 = A | 64;
                A = var143;
                mem[22741] = A;
                int var144 = C - 1 & 255;
                C = var144;
                if(C == 0) {
                  int var145 = D - 1 & 255;
                  D = var145;
                  if(D == 0) {
                    continue label241;
                  }
                }
              }
            }

            int var146 = HL();
            int var147 = mem[var146] - 1 & 255;
            int var148 = HL();
            mem[var148] = var147;
            HL(34263);
            DE(34255);
            BC(7);
            ldir();
          }
        }
      }
    }
  }

  public void $38528() {
    do {
      int var1 = IX();
      int var2 = mem[var1];
      A = var2;
      $38545();
      int var3 = IX() + 1 & 65535;
      IX(var3);
      int var4 = E + 1 & 255;
      E = var4;
      A = D;
      int var5 = A - 8 & 255;
      A = var5;
      D = A;
      int var6 = C - 1 & 255;
      C = var6;
    } while(C != 0);

  }

  public void $38555() {
    do {
      int var1 = HL();
      int var2 = mem[var1];
      A = var2;
      int var3 = DE();
      mem[var3] = A;
      int var4 = HL() + 1 & 65535;
      HL(var4);
      int var5 = D + 1 & 255;
      D = var5;
      int var6 = B - 1 & 255;
      B = var6;
    } while(B != 0);

  }

  public void $38562() {
    while(true) {
      int var1 = HL();
      int var2 = mem[var1];
      A = var2;
      if(A == 255) {
        return;
      }

      BC(100);
      A = 0;
      int var3 = HL();
      int var4 = mem[var3];
      E = var4;
      D = E;

      while(true) {
        int var5 = D - 1 & 255;
        D = var5;
        if(D == 0) {
          D = E;
          int var12 = A ^ 24;
          A = var12;
        }

        int var6 = B - 1 & 255;
        B = var6;
        if(B == 0) {
          A = C;
          if(A == 50) {
            int var9 = A - 50;
            F = var9;
            int var10 = E;
            int var11 = rl(var10);
            E = var11;
          }

          int var7 = C - 1 & 255;
          C = var7;
          if(C == 0) {
            $38601();
            if(F != 0) {
              return;
            }

            int var8 = HL() + 1 & 65535;
            HL(var8);
            break;
          }
        }
      }
    }
  }

  public void $35563() {
    HL(22528);
    int var1 = HL();
    int var2 = mem[var1];
    A = var2;
    int var3 = A & 7;
    A = var3;

    do {
      int var4 = HL();
      int var5 = mem[var4];
      A = var5;
      int var6 = A + 3 & 255;
      A = var6;
      int var7 = A & 7;
      A = var7;
      D = A;
      int var8 = HL();
      int var9 = mem[var8];
      A = var9;
      int var10 = A + 24 & 255;
      A = var10;
      int var11 = A & 184;
      A = var11;
      int var12 = A | D;
      A = var12;
      int var13 = HL();
      mem[var13] = A;
      int var14 = HL() + 1 & 65535;
      HL(var14);
      A = H;
    } while(A != 91);

    int var15 = A - 91;
    F = var15;
  }

  public void $38622() {
    E = A;
    C = 254;

    do {
      D = A;
      int var1 = D & -17;
      D = var1;
      int var2 = D & -9;
      D = var2;
      B = E;

      do {
        if(A == B) {
          D = 24;
        }

        int var3 = B - 1 & 255;
        B = var3;
      } while(B != 0);

      int var4 = A - 1 & 255;
      A = var4;
    } while(A != 0);

  }

  public void $36147() {
    $36203();
    IX(24064);
    A = 112;
    mem[36189] = A;
    $36171();
    IX(24320);
    A = 120;
    mem[36189] = A;
    $36171();
  }

  public void $35211() {
    int var1 = mem[34252];
    A = var1;
    HL(20640);
    if(A << 1 != 0) {
      B = A;

      do {
        C = 0;
        int var2 = HL();
        push(var2);
        int var3 = BC();
        push(var3);
        int var4 = mem[34273];
        A = var4;
        int var5 = A;
        int var6 = rlc(var5);
        A = var6;
        int var7 = A;
        int var8 = rlc(var7);
        A = var8;
        int var9 = A;
        int var10 = rlc(var9);
        A = var10;
        int var11 = A & 96;
        A = var11;
        E = A;
        D = 157;
        $37974();
        int var12 = pop();
        BC(var12);
        int var13 = pop();
        HL(var13);
        int var14 = HL() + 1 & 65535;
        HL(var14);
        int var15 = HL() + 1 & 65535;
        HL(var15);
        int var16 = B - 1 & 255;
        B = var16;
      } while(B != 0);

    }
  }

  public void $37056() {
    IX(33024);

    while(true) {
      int var1 = IX();
      int var2 = mem[var1];
      A = var2;
      if(A == 255) {
        return;
      }

      int var3 = A & 3;
      A = var3;
      if(A << 1 != 0) {
        if(A != 1) {
          if(A != 2) {
            int var59 = IX();
            if((mem[var59] & 128) != 0) {
              int var74 = IX() + 1;
              int var75 = mem[var74];
              A = var75;
              if((A & 128) != 0) {
                int var78 = A - 2 & 255;
                A = var78;
                if(A < 148) {
                  int var79 = A - 2 & 255;
                  A = var79;
                  if(A == 128) {
                    A = 0;
                  }
                }
              } else {
                int var76 = A + 2 & 255;
                A = var76;
                if(A < 18) {
                  int var77 = A + 2 & 255;
                  A = var77;
                }
              }
            } else {
              int var60 = IX() + 1;
              int var61 = mem[var60];
              A = var61;
              if((A & 128) == 0) {
                int var72 = A - 2 & 255;
                A = var72;
                if(A < 20) {
                  int var73 = A - 2 & 255;
                  A = var73;
                  if(A << 1 == 0) {
                    A = 128;
                  }
                }
              } else {
                int var62 = A + 2 & 255;
                A = var62;
                if(A < 146) {
                  int var71 = A + 2 & 255;
                  A = var71;
                }
              }
            }

            int var63 = IX() + 1;
            mem[var63] = A;
            int var64 = A & 127;
            A = var64;
            int var65 = IX() + 7;
            int var66 = mem[var65];
            if(A == var66) {
              int var67 = IX();
              int var68 = mem[var67];
              A = var68;
              int var69 = A ^ 128;
              A = var69;
              int var70 = IX();
              mem[var70] = A;
            }
          } else {
            label81: {
              int var33 = IX();
              int var34 = mem[var33];
              A = var34;
              int var35 = A ^ 8;
              A = var35;
              int var36 = IX();
              mem[var36] = A;
              int var37 = A & 24;
              A = var37;
              if(A << 1 != 0) {
                int var55 = IX();
                int var56 = mem[var55];
                A = var56;
                int var57 = A + 32 & 255;
                A = var57;
                int var58 = IX();
                mem[var58] = A;
              }

              int var38 = IX() + 3;
              int var39 = mem[var38];
              A = var39;
              int var40 = IX() + 4;
              int var41 = mem[var40];
              int var42 = A + var41 & 255;
              A = var42;
              int var43 = IX() + 3;
              mem[var43] = A;
              int var44 = IX() + 7;
              int var45 = mem[var44];
              if(A < var45) {
                int var50 = IX() + 6;
                int var51 = mem[var50];
                if(A != var51 && A >= var51) {
                  break label81;
                }

                int var52 = IX() + 6;
                int var53 = mem[var52];
                A = var53;
                int var54 = IX() + 3;
                mem[var54] = A;
              }

              int var46 = IX() + 4;
              int var47 = mem[var46];
              A = var47;
              int var48 = -A & 255;
              A = var48;
              int var49 = IX() + 4;
              mem[var49] = A;
            }
          }
        } else {
          int var6 = IX();
          if((mem[var6] & 128) == 0) {
            int var20 = IX();
            int var21 = mem[var20];
            A = var21;
            int var22 = A - 32 & 255;
            A = var22;
            int var23 = A & 127;
            A = var23;
            int var24 = IX();
            mem[var24] = A;
            if(A >= 96) {
              int var25 = IX() + 2;
              int var26 = mem[var25];
              A = var26;
              int var27 = A & 31;
              A = var27;
              int var28 = IX() + 6;
              int var29 = mem[var28];
              if(A != var29) {
                int var31 = IX() + 2;
                int var32 = mem[var31] - 1 & 255;
                mem[var31] = var32;
              } else {
                int var30 = IX();
                mem[var30] = 129;
              }
            }
          } else {
            int var7 = IX();
            int var8 = mem[var7];
            A = var8;
            int var9 = A + 32 & 255;
            A = var9;
            int var10 = A | 128;
            A = var10;
            int var11 = IX();
            mem[var11] = A;
            if(A < 160) {
              int var12 = IX() + 2;
              int var13 = mem[var12];
              A = var13;
              int var14 = A & 31;
              A = var14;
              int var15 = IX() + 7;
              int var16 = mem[var15];
              if(A != var16) {
                int var18 = IX() + 2;
                int var19 = mem[var18] + 1 & 255;
                mem[var18] = var19;
              } else {
                int var17 = IX();
                mem[var17] = 97;
              }
            }
          }
        }
      }

      DE(8);
      int var4 = DE();
      int var5 = IX() + var4 & 65535;
      IX(var5);
    }
  }

  public void $36307() {
    label188: {
      int var1 = mem[34262];
      A = var1;
      int var2 = A - 1 & 255;
      A = var2;
      if((A & 128) != 0) {
        label180: {
          int var176 = mem[34257];
          A = var176;
          if(A == 1) {
            $36323();
            if(A != 13) {
              break label188;
            }
          }

          int var177 = mem[34255];
          A = var177;
          int var178 = A & 14;
          A = var178;
          if(A << 1 == 0) {
            int var199 = mem16(34259);
            HL(var199);
            DE(64);
            int var200 = DE();
            int var201 = HL() + var200 & 65535;
            HL(var201);
            if((H & 2) != 0) {
              int var202 = mem[33004];
              A = var202;
              mem[33824] = A;
              A = 0;
              mem[34255] = A;
              int var203 = mem[34257];
              A = var203;
              if(A < 11) {
                $38115();
              }

              int var204 = mem[34259];
              A = var204;
              int var205 = A & 31;
              A = var205;
              mem[34259] = A;
              A = 92;
              mem[34260] = A;
              nextAddress = 38134;
              return;
            }

            int var206 = mem[32955];
            A = var206;
            int var207 = HL();
            int var208 = mem[var207];
            if(A != var208) {
              int var209 = HL() + 1 & 65535;
              HL(var209);
              int var210 = mem[32955];
              A = var210;
              int var211 = HL();
              int var212 = mem[var211];
              if(A != var212) {
                int var213 = mem[32928];
                A = var213;
                int var214 = HL();
                int var215 = mem[var214];
                int var216 = A - var215;
                F = var216;
                int var217 = HL() - 1 & 65535;
                HL(var217);
                if(F != 0) {
                  break label180;
                }

                int var218 = HL();
                int var219 = mem[var218];
                if(A != var219) {
                  break label180;
                }
              }
            }
          }

          int var179 = mem[34257];
          A = var179;
          if(A != 1) {
            HL(34256);
            int var180 = HL();
            int var181 = mem[var180] & -3;
            int var182 = HL();
            mem[var182] = var181;
            int var183 = mem[34257];
            A = var183;
            if(A << 1 == 0) {
              A = 2;
              mem[34257] = A;
              return;
            }

            int var184 = A + 1 & 255;
            A = var184;
            if(A == 16) {
              A = 12;
            }

            mem[34257] = A;
            int var185 = A;
            int var186 = rlc(var185);
            A = var186;
            int var187 = A;
            int var188 = rlc(var187);
            A = var188;
            int var189 = A;
            int var190 = rlc(var189);
            A = var190;
            int var191 = A;
            int var192 = rlc(var191);
            A = var192;
            D = A;
            C = 32;
            int var193 = mem[32990];
            A = var193;

            do {
              int var194 = A ^ 24;
              A = var194;
              B = D;

              do {
                int var195 = B - 1 & 255;
                B = var195;
              } while(B != 0);

              int var196 = C - 1 & 255;
              C = var196;
            } while(C != 0);

            int var197 = mem[34255];
            A = var197;
            int var198 = A + 8 & 255;
            A = var198;
            mem[34255] = A;
            $36508();
            return;
          }
          break label188;
        }
      }

      E = 255;
      int var3 = mem[34262];
      A = var3;
      int var4 = A - 1 & 255;
      A = var4;
      if((A & 128) != 0) {
        int var170 = mem[34257];
        A = var170;
        if(A >= 12) {
          nextAddress = 37048;
          return;
        }

        A = 0;
        mem[34257] = A;
        int var171 = mem[32973];
        A = var171;
        int var172 = HL();
        int var173 = mem[var172];
        if(A != var173) {
          $36592();
        }

        int var174 = mem[32982];
        A = var174;
        int var175 = A - 3 & 255;
        A = var175;
        E = A;
      }

      BC(57342);
      int var5 = BC();
      int var6 = in(var5);
      A = var6;
      int var7 = A & 31;
      A = var7;
      int var8 = A | 32;
      A = var8;
      int var9 = A & E;
      A = var9;
      E = A;
      int var10 = mem[34271];
      A = var10;
      int var11 = A & 2;
      A = var11;
      int var12 = A;
      int var13 = rrc(var12);
      A = var13;
      int var14 = A ^ E;
      A = var14;
      E = A;
      BC(64510);
      int var15 = BC();
      int var16 = in(var15);
      A = var16;
      int var17 = A & 31;
      A = var17;
      int var18 = A;
      int var19 = rlc(var18);
      A = var19;
      int var20 = A | 1;
      A = var20;
      int var21 = A & E;
      A = var21;
      E = A;
      B = 231;
      int var22 = BC();
      int var23 = in(var22);
      A = var23;
      int var24 = A;
      int var25 = rrc(var24);
      A = var25;
      int var26 = A | 247;
      A = var26;
      int var27 = A & E;
      A = var27;
      E = A;
      B = 239;
      int var28 = BC();
      int var29 = in(var28);
      A = var29;
      int var30 = A | 251;
      A = var30;
      int var31 = A & E;
      A = var31;
      E = A;
      int var32 = BC();
      int var33 = in(var32);
      A = var33;
      int var34 = A;
      int var35 = rrc(var34);
      A = var35;
      int var36 = A | 251;
      A = var36;
      int var37 = A & E;
      A = var37;
      E = A;
      int var38 = mem[34254];
      A = var38;
      if(A << 1 != 0) {
        BC(31);
        int var165 = BC();
        int var166 = in(var165);
        A = var166;
        int var167 = A & 3;
        A = var167;
        int var168 = ~A;
        A = var168;
        int var169 = A & E;
        A = var169;
        E = A;
      }

      C = 0;
      A = E;
      int var39 = A & 42;
      A = var39;
      if(A != 42) {
        C = 4;
        A = 0;
        mem[34272] = A;
      }

      A = E;
      int var40 = A & 21;
      A = var40;
      if(A != 21) {
        int var164 = C | 8;
        C = var164;
        A = 0;
        mem[34272] = A;
      }

      int var41 = mem[34256];
      A = var41;
      int var42 = A + C & 255;
      A = var42;
      C = A;
      B = 0;
      HL(33825);
      int var43 = BC();
      int var44 = HL() + var43 & 65535;
      HL(var44);
      int var45 = HL();
      int var46 = mem[var45];
      A = var46;
      mem[34256] = A;
      BC(32510);
      int var47 = BC();
      int var48 = in(var47);
      A = var48;
      int var49 = A & 31;
      A = var49;
      if(A == 31) {
        B = 239;
        int var159 = BC();
        int var160 = in(var159);
        A = var160;
        if((A & 1) != 0) {
          int var161 = mem[34254];
          A = var161;
          if(A << 1 == 0) {
            break label188;
          }

          BC(31);
          int var162 = BC();
          int var163 = in(var162);
          A = var163;
          if((A & 16) == 0) {
            break label188;
          }
        }
      }

      int var50 = mem[34271];
      A = var50;
      if((A & 2) == 0) {
        A = 0;
        mem[34261] = A;
        mem[34272] = A;
        int var150 = A + 1 & 255;
        A = var150;
        mem[34257] = A;
        int var151 = mem[34262];
        A = var151;
        int var152 = A - 1 & 255;
        A = var152;
        if((A & 128) == 0) {
          A = 240;
          mem[34262] = A;
          int var153 = mem[34255];
          A = var153;
          int var154 = A & 240;
          A = var154;
          int var155 = A << 1;
          F = var155;
          mem[34255] = A;
          HL(34256);
          int var156 = HL();
          int var157 = mem[var156] | 2;
          int var158 = HL();
          mem[var158] = var157;
          return;
        }
      }
    }

    int var51 = mem[34256];
    A = var51;
    int var52 = A & 2;
    A = var52;
    if(A << 1 != 0) {
      int var53 = mem[34262];
      A = var53;
      int var54 = A - 1 & 255;
      A = var54;
      if((A & 128) != 0) {
        int var55 = mem[34256];
        A = var55;
        int var56 = A & 1;
        A = var56;
        if(A << 1 != 0) {
          int var106 = mem[34258];
          A = var106;
          if(A << 1 != 0) {
            int var149 = A - 1 & 255;
            A = var149;
            F = A;
            mem[34258] = A;
          } else {
            int var107 = mem[34257];
            A = var107;
            BC(0);
            if(A == 0) {
              int var138 = mem16(34259);
              HL(var138);
              BC(0);
              int var139 = mem[32986];
              A = var139;
              int var140 = A - 1 & 255;
              A = var140;
              int var141 = A | 161;
              A = var141;
              int var142 = A ^ 224;
              A = var142;
              E = A;
              D = 0;
              int var143 = DE();
              int var144 = HL() + var143 & 65535;
              HL(var144);
              int var145 = mem[32964];
              A = var145;
              int var146 = HL();
              int var147 = mem[var146];
              if(A == var147) {
                BC(32);
                int var148 = mem[32986];
                A = var148;
                if(A << 1 == 0) {
                  BC(65504);
                }
              }
            }

            int var108 = mem16(34259);
            HL(var108);
            A = L;
            int var109 = A & 31;
            A = var109;
            if(A << 1 != 0) {
              int var114 = BC();
              int var115 = HL() + var114 & 65535;
              HL(var115);
              int var116 = HL() - 1 & 65535;
              HL(var116);
              DE(32);
              int var117 = DE();
              int var118 = HL() + var117 & 65535;
              HL(var118);
              int var119 = mem[32946];
              A = var119;
              int var120 = HL();
              int var121 = mem[var120];
              if(A != var121) {
                int var122 = mem[34255];
                A = var122;
                int var123 = C >> 1;
                int var124 = C & 128;
                int var125 = var123 | var124;
                C = var125;
                int var126 = A + C & 255;
                A = var126;
                B = A;
                int var127 = A & 15;
                A = var127;
                if(A << 1 != 0) {
                  int var131 = mem[32946];
                  A = var131;
                  int var132 = DE();
                  int var133 = HL() + var132 & 65535;
                  HL(var133);
                  int var134 = HL();
                  int var135 = mem[var134];
                  if(A == var135) {
                    return;
                  }

                  int var136 = DE();
                  int var137 = HL() - var136 & 65535;
                  HL(var137);
                }

                int var128 = DE();
                int var129 = HL() - var128 & 65535;
                HL(var129);
                int var130 = HL();
                wMem16(34259, var130);
                A = B;
                mem[34255] = A;
                A = 3;
                mem[34258] = A;
              }
            } else {
              int var110 = mem[33001];
              A = var110;
              mem[33824] = A;
              int var111 = mem[34259];
              A = var111;
              int var112 = A | 31;
              A = var112;
              int var113 = A & 254;
              A = var113;
              mem[34259] = A;
              nextAddress = 38043;
            }
          }
        } else {
          int var57 = mem[34258];
          A = var57;
          if(A != 3) {
            int var105 = A + 1 & 255;
            A = var105;
            F = A;
            mem[34258] = A;
          } else {
            int var58 = mem[34257];
            A = var58;
            BC(0);
            if(A << 1 == 0) {
              int var94 = mem16(34259);
              HL(var94);
              int var95 = mem[32986];
              A = var95;
              int var96 = A - 1 & 255;
              A = var96;
              int var97 = A | 157;
              A = var97;
              int var98 = A ^ 191;
              A = var98;
              E = A;
              D = 0;
              int var99 = DE();
              int var100 = HL() + var99 & 65535;
              HL(var100);
              int var101 = mem[32964];
              A = var101;
              int var102 = HL();
              int var103 = mem[var102];
              if(A == var103) {
                BC(32);
                int var104 = mem[32986];
                A = var104;
                if(A << 1 != 0) {
                  BC(65504);
                }
              }
            }

            int var59 = mem16(34259);
            HL(var59);
            int var60 = BC();
            int var61 = HL() + var60 & 65535;
            HL(var61);
            int var62 = HL() + 1 & 65535;
            HL(var62);
            int var63 = HL() + 1 & 65535;
            HL(var63);
            A = L;
            int var64 = A & 31;
            A = var64;
            if(A << 1 != 0) {
              DE(32);
              int var68 = mem[32946];
              A = var68;
              int var69 = DE();
              int var70 = HL() + var69 & 65535;
              HL(var70);
              int var71 = HL();
              int var72 = mem[var71];
              if(A != var72) {
                int var73 = mem[34255];
                A = var73;
                int var74 = C >> 1;
                int var75 = C & 128;
                int var76 = var74 | var75;
                C = var76;
                int var77 = A + C & 255;
                A = var77;
                B = A;
                int var78 = A & 15;
                A = var78;
                if(A << 1 != 0) {
                  int var87 = mem[32946];
                  A = var87;
                  int var88 = DE();
                  int var89 = HL() + var88 & 65535;
                  HL(var89);
                  int var90 = HL();
                  int var91 = mem[var90];
                  if(A == var91) {
                    return;
                  }

                  int var92 = DE();
                  int var93 = HL() - var92 & 65535;
                  HL(var93);
                }

                int var79 = mem[32946];
                A = var79;
                int var80 = DE();
                int var81 = HL() - var80 & 65535;
                HL(var81);
                int var82 = HL();
                int var83 = mem[var82];
                if(A != var83) {
                  int var84 = HL() - 1 & 65535;
                  HL(var84);
                  int var85 = HL();
                  wMem16(34259, var85);
                  A = 0;
                  int var86 = A << 1;
                  F = var86;
                  mem[34258] = A;
                  A = B;
                  mem[34255] = A;
                }
              }
            } else {
              int var65 = mem[33002];
              A = var65;
              mem[33824] = A;
              int var66 = mem[34259];
              A = var66;
              int var67 = A & 224;
              A = var67;
              mem[34259] = A;
              nextAddress = 38061;
            }
          }
        }
      }
    }
  }

  public void $38064() {
    int var1 = mem[33003];
    A = var1;
    mem[33824] = A;
    int var2 = mem[34259];
    A = var2;
    int var3 = A & 31;
    A = var3;
    int var4 = A + 160 & 255;
    A = var4;
    mem[34259] = A;
    A = 93;
    mem[34260] = A;
    A = 208;
    mem[34255] = A;
    A = 0;
    mem[34257] = A;
    nextAddress = 38095;
  }

  public void $38344() {
    int var1 = mem16(34259);
    HL(var1);
    B = 0;
    int var2 = mem[32986];
    A = var2;
    int var3 = A & 1;
    A = var3;
    int var4 = A + 64 & 255;
    A = var4;
    E = A;
    D = 0;
    int var5 = DE();
    int var6 = HL() + var5 & 65535;
    HL(var6);
    int var7 = mem[32964];
    A = var7;
    int var8 = HL();
    int var9 = mem[var8];
    if(A == var9) {
      $38366();
    }

    int var10 = mem16(34259);
    HL(var10);
    DE(31);
    C = 15;
    $38430();
    if(isNextPC(37047)) {
      nextAddress = 37048;
    } else {
      int var11 = HL() + 1 & 65535;
      HL(var11);
      $38430();
      if(isNextPC(37047)) {
        nextAddress = 37048;
      } else {
        int var12 = DE();
        int var13 = HL() + var12 & 65535;
        HL(var13);
        $38430();
        int var14 = HL() + 1 & 65535;
        HL(var14);
        $38430();
        if(isNextPC(37047)) {
          nextAddress = 37048;
        } else {
          int var15 = mem[34255];
          A = var15;
          int var16 = A + B & 255;
          A = var16;
          C = A;
          int var17 = DE();
          int var18 = HL() + var17 & 65535;
          HL(var18);
          $38430();
          int var19 = HL() + 1 & 65535;
          HL(var19);
          $38430();
          if(isNextPC(37047)) {
            nextAddress = 37048;
          } else {
            int var20 = mem[34255];
            A = var20;
            int var21 = A + B & 255;
            A = var21;
            IXH = 130;
            IXL = A;
            int var22 = mem[34256];
            A = var22;
            int var23 = A & 1;
            A = var23;
            int var24 = A;
            int var25 = rrc(var24);
            A = var25;
            E = A;
            int var26 = mem[34258];
            A = var26;
            int var27 = A & 3;
            A = var27;
            int var28 = A;
            int var29 = rrc(var28);
            A = var29;
            int var30 = A;
            int var31 = rrc(var30);
            A = var31;
            int var32 = A;
            int var33 = rrc(var32);
            A = var33;
            int var34 = A | E;
            A = var34;
            E = A;
            D = 157;
            int var35 = mem[33824];
            A = var35;
            if(A == 29) {
              $38490();
            }

            B = 16;
            int var36 = mem[34259];
            A = var36;
            int var37 = A & 31;
            A = var37;
            C = A;
            $38504();
          }
        }
      }
    }
  }

  public void $38276() {
    int var1 = mem[33824];
    A = var1;
    if(A == 33) {
      int var2 = mem[34259];
      A = var2;
      if(A == 188) {
        A = 0;
        int var3 = A << 1;
        F = var3;
        mem[34251] = A;
        A = 3;
        mem[34271] = A;
      }
    }
  }

  public void $38196() {
    int var1 = mem[33824];
    A = var1;
    if(A == 35) {
      int var14 = mem[34271];
      A = var14;
      if(A << 1 == 0) {
        int var17 = mem[34251];
        A = var17;
        int var18 = A & 2;
        A = var18;
        int var19 = A;
        int var20 = rrc(var19);
        A = var20;
        int var21 = A;
        int var22 = rrc(var21);
        A = var22;
        int var23 = A;
        int var24 = rrc(var23);
        A = var24;
        int var25 = A;
        int var26 = rrc(var25);
        A = var26;
        int var27 = A | 128;
        A = var27;
        E = A;
        int var28 = mem[34255];
        A = var28;
        if(A != 208) {
          $38228();
          E = 224;
        }

        D = 156;
        HL(26734);
        C = 1;
        $37974();
        if(F != 0) {
          nextAddress = 37048;
        } else {
          HL(17733);
          int var29 = HL();
          wMem16(23918, var29);
          HL(1799);
          int var30 = HL();
          wMem16(23950, var30);
        }
      } else {
        int var15 = mem[34259];
        A = var15;
        int var16 = A & 31;
        A = var16;
        if(A < 6) {
          A = 2;
          mem[34271] = A;
        }
      }
    } else {
      int var2 = mem[33824];
      A = var2;
      if(A == 33) {
        int var3 = mem[34251];
        A = var3;
        int var4 = A & 1;
        A = var4;
        int var5 = A;
        int var6 = rrc(var5);
        A = var6;
        int var7 = A;
        int var8 = rrc(var7);
        A = var8;
        int var9 = A;
        int var10 = rrc(var9);
        A = var10;
        E = A;
        int var11 = mem[34271];
        A = var11;
        if(A == 3) {
          $38320();
        }

        D = 166;
        IX(33488);
        BC(4124);
        $38504();
        HL(1799);
        int var12 = HL();
        wMem16(23996, var12);
        int var13 = HL();
        wMem16(24028, var13);
      }
    }
  }

  public void $37310() {
    IX(33024);

    while(true) {
      int var1 = IX();
      int var2 = mem[var1];
      A = var2;
      if(A == 255) {
        return;
      }

      int var3 = A & 7;
      A = var3;
      if(A << 1 != 0) {
        if(A != 3) {
          if(A != 4) {
            int var162 = IX() + 3;
            int var163 = mem[var162];
            E = var163;
            D = 130;
            int var164 = DE();
            int var165 = mem[var164];
            A = var165;
            L = A;
            int var166 = IX() + 2;
            int var167 = mem[var166];
            A = var167;
            int var168 = A & 31;
            A = var168;
            int var169 = A + L & 255;
            A = var169;
            L = A;
            A = E;
            int var170 = A;
            int var171 = rlc(var170);
            A = var171;
            int var172 = A & 1;
            A = var172;
            int var173 = A | 92;
            A = var173;
            H = A;
            DE(31);
            int var174 = IX() + 1;
            int var175 = mem[var174];
            A = var175;
            int var176 = A & 15;
            A = var176;
            int var177 = A + 56 & 255;
            A = var177;
            int var178 = A & 71;
            A = var178;
            C = A;
            int var179 = HL();
            int var180 = mem[var179];
            A = var180;
            int var181 = A & 56;
            A = var181;
            int var182 = A ^ C;
            A = var182;
            C = A;
            int var183 = HL();
            mem[var183] = C;
            int var184 = HL() + 1 & 65535;
            HL(var184);
            int var185 = HL();
            mem[var185] = C;
            int var186 = DE();
            int var187 = HL() + var186 & 65535;
            HL(var187);
            int var188 = HL();
            mem[var188] = C;
            int var189 = HL() + 1 & 65535;
            HL(var189);
            int var190 = HL();
            mem[var190] = C;
            int var191 = IX() + 3;
            int var192 = mem[var191];
            A = var192;
            int var193 = A & 14;
            A = var193;
            if(A << 1 != 0) {
              int var216 = DE();
              int var217 = HL() + var216 & 65535;
              HL(var217);
              int var218 = HL();
              mem[var218] = C;
              int var219 = HL() + 1 & 65535;
              HL(var219);
              int var220 = HL();
              mem[var220] = C;
            }

            C = 1;
            int var194 = IX() + 1;
            int var195 = mem[var194];
            A = var195;
            int var196 = IX();
            int var197 = mem[var196];
            int var198 = A & var197;
            A = var198;
            int var199 = IX() + 2;
            int var200 = mem[var199];
            int var201 = A | var200;
            A = var201;
            int var202 = A & 224;
            A = var202;
            E = A;
            int var203 = IX() + 5;
            int var204 = mem[var203];
            D = var204;
            H = 130;
            int var205 = IX() + 3;
            int var206 = mem[var205];
            L = var206;
            int var207 = IX() + 2;
            int var208 = mem[var207];
            A = var208;
            int var209 = A & 31;
            A = var209;
            int var210 = HL();
            int var211 = mem[var210];
            int var212 = A | var211;
            A = var212;
            int var213 = HL() + 1 & 65535;
            HL(var213);
            int var214 = HL();
            int var215 = mem[var214];
            H = var215;
            L = A;
            $37974();
            if(F != 0) {
              nextAddress = 37048;
              return;
            }
          } else {
            int var111 = IX();
            if((mem[var111] & 128) == 0) {
              int var160 = IX() + 4;
              int var161 = mem[var160] - 1 & 255;
              mem[var160] = var161;
              C = 44;
            } else {
              int var112 = IX() + 4;
              int var113 = mem[var112] + 1 & 255;
              mem[var112] = var113;
              C = 244;
            }

            int var114 = IX() + 4;
            int var115 = mem[var114];
            A = var115;
            if(A != C) {
              int var116 = A & 224;
              A = var116;
              if(A << 1 == 0) {
                int var117 = IX() + 2;
                int var118 = mem[var117];
                E = var118;
                D = 130;
                int var119 = DE();
                int var120 = mem[var119];
                A = var120;
                int var121 = IX() + 4;
                int var122 = mem[var121];
                int var123 = A + var122 & 255;
                A = var123;
                L = A;
                A = E;
                int var124 = A & 128;
                A = var124;
                int var125 = A;
                int var126 = rlc(var125);
                A = var126;
                int var127 = A | 92;
                A = var127;
                H = A;
                int var128 = IX() + 5;
                mem[var128] = 0;
                int var129 = HL();
                int var130 = mem[var129];
                A = var130;
                int var131 = A & 7;
                A = var131;
                if(A == 7) {
                  int var154 = IX() + 5;
                  int var155 = mem[var154] - 1 & 255;
                  mem[var154] = var155;
                }

                int var132 = HL();
                int var133 = mem[var132];
                A = var133;
                int var134 = A | 7;
                A = var134;
                int var135 = HL();
                mem[var135] = A;
                int var136 = DE() + 1 & 65535;
                DE(var136);
                int var137 = DE();
                int var138 = mem[var137];
                A = var138;
                H = A;
                int var139 = H - 1 & 255;
                H = var139;
                int var140 = IX() + 6;
                int var141 = mem[var140];
                A = var141;
                int var142 = HL();
                mem[var142] = A;
                int var143 = H + 1 & 255;
                H = var143;
                int var144 = HL();
                int var145 = mem[var144];
                A = var145;
                int var146 = IX() + 5;
                int var147 = mem[var146];
                int var148 = A & var147;
                A = var148;
                if(A << 1 != 0) {
                  nextAddress = 37048;
                  return;
                }

                int var149 = HL();
                mem[var149] = 255;
                int var150 = H + 1 & 255;
                H = var150;
                int var151 = IX() + 6;
                int var152 = mem[var151];
                A = var152;
                int var153 = HL();
                mem[var153] = A;
              }
            } else {
              BC(640);
              int var156 = mem[32990];
              A = var156;

              do {
                int var157 = A ^ 24;
                A = var157;

                do {
                  int var158 = B - 1 & 255;
                  B = var158;
                } while(B != 0);

                B = C;
                int var159 = C - 1 & 255;
                C = var159;
              } while(C != 0);
            }
          }
        } else {
          IY(33280);
          int var6 = IX() + 9;
          mem[var6] = 0;
          int var7 = IX() + 2;
          int var8 = mem[var7];
          A = var8;
          int var9 = IX() + 3;
          mem[var9] = A;
          int var10 = IX() + 5;
          mem[var10] = 128;

          while(true) {
            label112: {
              int var11 = IY();
              int var12 = mem[var11];
              A = var12;
              int var13 = IX() + 3;
              int var14 = mem[var13];
              int var15 = A + var14 & 255;
              A = var15;
              L = A;
              int var16 = IY() + 1;
              int var17 = mem[var16];
              H = var17;
              int var18 = mem[34262];
              A = var18;
              if(A << 1 == 0) {
                int var102 = IX() + 5;
                int var103 = mem[var102];
                A = var103;
                int var104 = HL();
                int var105 = mem[var104];
                int var106 = A & var105;
                A = var106;
                if(A << 1 == 0) {
                  break label112;
                }

                int var107 = IX() + 9;
                int var108 = mem[var107];
                A = var108;
                mem[34262] = A;
                int var109 = IX() + 11;
                int var110 = mem[var109] | 1;
                mem[var109] = var110;
              }

              int var19 = IX() + 9;
              int var20 = mem[var19];
              if(A == var20) {
                int var92 = IX() + 11;
                if((mem[var92] & 1) != 0) {
                  int var93 = IX() + 3;
                  int var94 = mem[var93];
                  B = var94;
                  int var95 = IX() + 5;
                  int var96 = mem[var95];
                  A = var96;
                  C = 1;
                  if(A >= 4) {
                    C = 0;
                    if(A >= 16) {
                      int var101 = B - 1 & 255;
                      B = var101;
                      C = 3;
                      if(A >= 64) {
                        C = 2;
                      }
                    }
                  }

                  int var97 = BC();
                  wMem16(34258, var97);
                  A = IYL;
                  int var98 = A - 16 & 255;
                  A = var98;
                  mem[34255] = A;
                  int var99 = HL();
                  push(var99);
                  $36508();
                  int var100 = pop();
                  HL(var100);
                }
              }
            }

            int var21 = IX() + 5;
            int var22 = mem[var21];
            A = var22;
            int var23 = HL();
            int var24 = mem[var23];
            int var25 = A | var24;
            A = var25;
            int var26 = HL();
            mem[var26] = A;
            int var27 = IX() + 9;
            int var28 = mem[var27];
            A = var28;
            int var29 = IX() + 1;
            int var30 = mem[var29];
            int var31 = A + var30 & 255;
            A = var31;
            L = A;
            int var32 = L | 128;
            L = var32;
            H = 131;
            int var33 = HL();
            int var34 = mem[var33];
            E = var34;
            D = 0;
            int var35 = DE();
            int var36 = IY() + var35 & 65535;
            IY(var36);
            int var37 = L & -129;
            L = var37;
            int var38 = HL();
            int var39 = mem[var38];
            A = var39;
            if(A << 1 != 0) {
              B = A;
              int var77 = IX() + 1;
              if((mem[var77] & 128) != 0) {
                do {
                  int var85 = IX() + 5;
                  int var86 = mem[var85];
                  int var87 = rlc(var86);
                  mem[var85] = var87;
                  int var88 = IX() + 5;
                  if((mem[var88] & 1) != 0) {
                    int var90 = IX() + 3;
                    int var91 = mem[var90] - 1 & 255;
                    mem[var90] = var91;
                  }

                  int var89 = B - 1 & 255;
                  B = var89;
                } while(B != 0);
              } else {
                do {
                  int var78 = IX() + 5;
                  int var79 = mem[var78];
                  int var80 = rrc(var79);
                  mem[var78] = var80;
                  int var81 = IX() + 5;
                  if((mem[var81] & 128) != 0) {
                    int var83 = IX() + 3;
                    int var84 = mem[var83] + 1 & 255;
                    mem[var83] = var84;
                  }

                  int var82 = B - 1 & 255;
                  B = var82;
                } while(B != 0);
              }
            }

            int var40 = IX() + 9;
            int var41 = mem[var40];
            A = var41;
            int var42 = IX() + 4;
            int var43 = mem[var42];
            if(A == var43) {
              int var44 = mem[34262];
              A = var44;
              if((A & 128) != 0) {
                int var72 = A + 1 & 255;
                A = var72;
                mem[34262] = A;
                int var73 = IX() + 11;
                int var74 = mem[var73] & -2;
                mem[var73] = var74;
              } else {
                int var45 = IX() + 11;
                if((mem[var45] & 1) != 0) {
                  int var46 = mem[34256];
                  A = var46;
                  if((A & 2) != 0) {
                    int var47 = A;
                    int var48 = rrc(var47);
                    A = var48;
                    int var49 = IX();
                    int var50 = mem[var49];
                    int var51 = A ^ var50;
                    A = var51;
                    int var52 = A;
                    int var53 = rlc(var52);
                    A = var53;
                    int var54 = A;
                    int var55 = rlc(var54);
                    A = var55;
                    int var56 = A & 2;
                    A = var56;
                    int var57 = A - 1 & 255;
                    A = var57;
                    HL(34262);
                    int var58 = HL();
                    int var59 = mem[var58];
                    int var60 = A + var59 & 255;
                    A = var60;
                    int var61 = HL();
                    mem[var61] = A;
                    int var62 = mem[33003];
                    A = var62;
                    C = A;
                    int var63 = mem[33824];
                    A = var63;
                    if(A == C) {
                      $37780();
                      int var71 = HL();
                      mem[var71] = 12;
                    }

                    int var64 = HL();
                    int var65 = mem[var64];
                    A = var65;
                    int var66 = IX() + 4;
                    int var67 = mem[var66];
                    if(A >= var67 && A != var67) {
                      int var68 = HL();
                      mem[var68] = 240;
                      int var69 = mem[34255];
                      A = var69;
                      int var70 = A & 248;
                      A = var70;
                      mem[34255] = A;
                      A = 0;
                      mem[34257] = A;
                    }
                  }
                }
              }
              break;
            }

            int var75 = IX() + 9;
            int var76 = mem[var75] + 1 & 255;
            mem[var75] = var76;
          }
        }
      }

      DE(8);
      int var4 = DE();
      int var5 = IX() + var4 & 65535;
      IX(var5);
    }
  }

  public void $38137() {
    int var1 = mem16(32983);
    HL(var1);
    A = H;
    int var2 = A & 1;
    A = var2;
    int var3 = A;
    int var4 = rlc(var3);
    A = var4;
    int var5 = A;
    int var6 = rlc(var5);
    A = var6;
    int var7 = A;
    int var8 = rlc(var7);
    A = var8;
    int var9 = A + 112 & 255;
    A = var9;
    H = A;
    E = L;
    D = H;
    int var10 = mem[32985];
    A = var10;
    if(A << 1 != 0) {
      B = A;
      int var11 = mem[32982];
      A = var11;
      if(A << 1 == 0) {
        int var31 = HL();
        int var32 = mem[var31];
        A = var32;
        int var33 = A;
        int var34 = rlc(var33);
        A = var34;
        int var35 = A;
        int var36 = rlc(var35);
        A = var36;
        int var37 = H + 1 & 255;
        H = var37;
        int var38 = H + 1 & 255;
        H = var38;
        int var39 = HL();
        int var40 = mem[var39];
        C = var40;
        int var41 = C;
        int var42 = rrc(var41);
        C = var42;
        int var43 = C;
        int var44 = rrc(var43);
        C = var44;
      } else {
        int var12 = HL();
        int var13 = mem[var12];
        A = var13;
        int var14 = A;
        int var15 = rrc(var14);
        A = var15;
        int var16 = A;
        int var17 = rrc(var16);
        A = var17;
        int var18 = H + 1 & 255;
        H = var18;
        int var19 = H + 1 & 255;
        H = var19;
        int var20 = HL();
        int var21 = mem[var20];
        C = var21;
        int var22 = C;
        int var23 = rlc(var22);
        C = var23;
        int var24 = C;
        int var25 = rlc(var24);
        C = var25;
      }

      do {
        int var26 = DE();
        mem[var26] = A;
        int var27 = HL();
        mem[var27] = C;
        int var28 = L + 1 & 255;
        L = var28;
        int var29 = E + 1 & 255;
        E = var29;
        int var30 = B - 1 & 255;
        B = var30;
      } while(B != 0);

    }
  }

  public void $37841() {
    H = 164;
    int var1 = mem[41983];
    A = var1;
    L = A;

    do {
      int var2 = HL();
      int var3 = mem[var2];
      C = var3;
      int var4 = C & -129;
      C = var4;
      int var5 = mem[33824];
      A = var5;
      int var6 = A | 64;
      A = var6;
      if(A == C) {
        int var8 = HL();
        int var9 = mem[var8];
        A = var9;
        int var10 = A;
        int var11 = rlc(var10);
        A = var11;
        int var12 = A & 1;
        A = var12;
        int var13 = A + 92 & 255;
        A = var13;
        D = A;
        int var14 = H + 1 & 255;
        H = var14;
        int var15 = HL();
        int var16 = mem[var15];
        E = var16;
        int var17 = H - 1 & 255;
        H = var17;
        int var18 = DE();
        int var19 = mem[var18];
        A = var19;
        int var20 = A & 7;
        A = var20;
        if(A != 7) {
          int var21 = mem[34251];
          A = var21;
          int var22 = A + L & 255;
          A = var22;
          int var23 = A & 3;
          A = var23;
          int var24 = A + 3 & 255;
          A = var24;
          C = A;
          int var25 = DE();
          int var26 = mem[var25];
          A = var26;
          int var27 = A & 248;
          A = var27;
          int var28 = A | C;
          A = var28;
          int var29 = DE();
          mem[var29] = A;
          int var30 = HL();
          int var31 = mem[var30];
          A = var31;
          int var32 = A;
          int var33 = rlc(var32);
          A = var33;
          int var34 = A;
          int var35 = rlc(var34);
          A = var35;
          int var36 = A;
          int var37 = rlc(var36);
          A = var37;
          int var38 = A;
          int var39 = rlc(var38);
          A = var39;
          int var40 = A & 8;
          A = var40;
          int var41 = A + 96 & 255;
          A = var41;
          D = A;
          int var42 = HL();
          push(var42);
          HL(32993);
          B = 8;
          $38555();
          int var43 = pop();
          HL(var43);
        } else {
          IX(34172);

          while(true) {
            int var44 = IX() + 2;
            int var45 = mem[var44] + 1 & 255;
            mem[var44] = var45;
            int var46 = IX() + 2;
            int var47 = mem[var46];
            A = var47;
            if(A != 58) {
              int var48 = mem[32990];
              A = var48;
              C = 128;

              do {
                int var49 = A ^ 24;
                A = var49;
                E = A;
                A = 144;
                int var50 = A - C & 255;
                A = var50;
                B = A;
                A = E;

                do {
                  int var51 = B - 1 & 255;
                  B = var51;
                } while(B != 0);

                int var52 = C - 1 & 255;
                C = var52;
                int var53 = C - 1 & 255;
                C = var53;
              } while(C != 0);

              int var54 = mem[34270];
              A = var54;
              int var55 = A + 1 & 255;
              A = var55;
              F = A;
              mem[34270] = A;
              if(F == 0) {
                A = 1;
                mem[34271] = A;
              }

              int var56 = HL();
              int var57 = mem[var56] & -65;
              int var58 = HL();
              mem[var58] = var57;
              break;
            }

            int var59 = IX() + 2;
            mem[var59] = 48;
            int var60 = IX() - 1 & 65535;
            IX(var60);
          }
        }
      }

      int var7 = L + 1 & 255;
      L = var7;
    } while(L != 0);

  }

  public void $35615() {
    B = 191;
    HL(34274);
    int var1 = BC();
    int var2 = in(var1);
    A = var2;
    int var3 = A & 31;
    A = var3;
    if(A != 31) {
      int var61 = HL();
      if((mem[var61] & 1) == 0) {
        int var62 = HL();
        int var63 = mem[var62];
        A = var63;
        int var64 = A ^ 3;
        A = var64;
        int var65 = HL();
        mem[var65] = A;
      }
    } else {
      int var4 = HL();
      int var5 = mem[var4] & -2;
      int var6 = HL();
      mem[var6] = var5;
    }

    int var7 = HL();
    if((mem[var7] & 2) == 0) {
      A = 0;
      mem[34272] = A;
      int var39 = mem[34273];
      A = var39;
      int var40 = A + 1 & 255;
      A = var40;
      mem[34273] = A;
      int var41 = A & 126;
      A = var41;
      int var42 = A;
      int var43 = rrc(var42);
      A = var43;
      E = A;
      D = 0;
      HL(34399);
      int var44 = DE();
      int var45 = HL() + var44 & 65535;
      HL(var45);
      int var46 = mem[34252];
      A = var46;
      int var47 = A;
      int var48 = rlc(var47);
      A = var48;
      int var49 = A;
      int var50 = rlc(var49);
      A = var50;
      int var51 = A - 28 & 255;
      A = var51;
      int var52 = -A & 255;
      A = var52;
      int var53 = HL();
      int var54 = mem[var53];
      int var55 = A + var54 & 255;
      A = var55;
      D = A;
      int var56 = mem[32990];
      A = var56;
      E = D;
      BC(3);

      do {
        do {
          int var57 = E - 1 & 255;
          E = var57;
          if(E == 0) {
            E = D;
            int var60 = A ^ 24;
            A = var60;
          }

          int var58 = B - 1 & 255;
          B = var58;
        } while(B != 0);

        int var59 = C - 1 & 255;
        C = var59;
      } while(C != 0);
    }

    BC(61438);
    int var8 = BC();
    int var9 = in(var8);
    A = var9;
    if((A & 2) == 0) {
      int var34 = A & 16;
      A = var34;
      int var35 = A ^ 16;
      A = var35;
      int var36 = A;
      int var37 = rlc(var36);
      A = var37;
      D = A;
      int var38 = mem[34275];
      A = var38;
      if(A == 10) {
        $35720();
      }
    }

    int var10 = mem[34275];
    A = var10;
    if(A != 10) {
      int var11 = mem[33824];
      A = var11;
      if(A == 28) {
        int var12 = mem[34255];
        A = var12;
        if(A == 208) {
          int var13 = mem[34275];
          A = var13;
          int var14 = A;
          int var15 = rlc(var14);
          A = var15;
          E = A;
          D = 0;
          IX(34279);
          int var16 = DE();
          int var17 = IX() + var16 & 65535;
          IX(var17);
          BC(64510);
          int var18 = BC();
          int var19 = in(var18);
          A = var19;
          int var20 = A & 31;
          A = var20;
          int var21 = IX();
          int var22 = mem[var21];
          if(A != var22) {
            if(A != 31) {
              int var32 = IX();
              int var33 = mem[var32];
              if(A != var33) {
                A = 0;
                mem[34275] = A;
              }
            }
          } else {
            B = 223;
            int var23 = BC();
            int var24 = in(var23);
            A = var24;
            int var25 = A & 31;
            A = var25;
            int var26 = IX() + 1;
            int var27 = mem[var26];
            if(A != var27) {
              if(A != 31) {
                int var30 = IX();
                int var31 = mem[var30];
                if(A != var31) {
                  A = 0;
                  mem[34275] = A;
                }
              }
            } else {
              int var28 = mem[34275];
              A = var28;
              int var29 = A + 1 & 255;
              A = var29;
              mem[34275] = A;
            }
          }
        }
      }
    }
  }

  public void $35720() {
    BC(63486);
    int var1 = BC();
    int var2 = in(var1);
    A = var2;
    int var3 = ~A;
    A = var3;
    int var4 = A & 31;
    A = var4;
    int var5 = A | D;
    A = var5;
    mem[33824] = A;
  }

  public void $37974() {
    B = 16;

    do {
      int var1 = C & 1;
      F = var1;
      int var2 = DE();
      int var3 = mem[var2];
      A = var3;
      if(F != 0) {
        int var29 = HL();
        int var30 = mem[var29];
        int var31 = A & var30;
        A = var31;
        if(A << 1 != 0) {
          return;
        }

        int var32 = DE();
        int var33 = mem[var32];
        A = var33;
        int var34 = HL();
        int var35 = mem[var34];
        int var36 = A | var35;
        A = var36;
      }

      int var4 = HL();
      mem[var4] = A;
      int var5 = L + 1 & 255;
      L = var5;
      int var6 = DE() + 1 & 65535;
      DE(var6);
      int var7 = C & 1;
      F = var7;
      int var8 = DE();
      int var9 = mem[var8];
      A = var9;
      if(F != 0) {
        int var21 = HL();
        int var22 = mem[var21];
        int var23 = A & var22;
        A = var23;
        if(A << 1 != 0) {
          return;
        }

        int var24 = DE();
        int var25 = mem[var24];
        A = var25;
        int var26 = HL();
        int var27 = mem[var26];
        int var28 = A | var27;
        A = var28;
      }

      int var10 = HL();
      mem[var10] = A;
      int var11 = L - 1 & 255;
      L = var11;
      int var12 = H + 1 & 255;
      H = var12;
      int var13 = DE() + 1 & 65535;
      DE(var13);
      A = H;
      int var14 = A & 7;
      A = var14;
      if(A << 1 == 0) {
        A = H;
        int var17 = A - 8 & 255;
        A = var17;
        H = A;
        A = L;
        int var18 = A + 32 & 255;
        A = var18;
        L = A;
        int var19 = A & 224;
        A = var19;
        if(A << 1 == 0) {
          A = H;
          int var20 = A + 8 & 255;
          A = var20;
          H = A;
        }
      }

      int var15 = B - 1 & 255;
      B = var15;
    } while(B != 0);

    A = 0;
    int var16 = A << 1;
    F = var16;
  }

  public void $36203() {
    HL(32768);
    IX(24064);

    do {
      int var1 = HL();
      int var2 = mem[var1];
      A = var2;
      int var3 = A;
      int var4 = rlc(var3);
      A = var4;
      int var5 = A;
      int var6 = rlc(var5);
      A = var6;
      $36288();
      int var7 = HL();
      int var8 = mem[var7];
      A = var8;
      int var9 = A;
      int var10 = rrc(var9);
      A = var10;
      int var11 = A;
      int var12 = rrc(var11);
      A = var12;
      int var13 = A;
      int var14 = rrc(var13);
      A = var14;
      int var15 = A;
      int var16 = rrc(var15);
      A = var16;
      $36288();
      int var17 = HL();
      int var18 = mem[var17];
      A = var18;
      int var19 = A;
      int var20 = rrc(var19);
      A = var20;
      int var21 = A;
      int var22 = rrc(var21);
      A = var22;
      $36288();
      int var23 = HL();
      int var24 = mem[var23];
      A = var24;
      $36288();
      int var25 = HL() + 1 & 65535;
      HL(var25);
      A = L;
      int var26 = A & 128;
      A = var26;
    } while(A << 1 == 0);

    int var27 = mem[32985];
    A = var27;
    if(A << 1 != 0) {
      int var41 = mem16(32983);
      HL(var41);
      B = A;
      int var42 = mem[32973];
      A = var42;

      do {
        int var43 = HL();
        mem[var43] = A;
        int var44 = HL() + 1 & 65535;
        HL(var44);
        int var45 = B - 1 & 255;
        B = var45;
      } while(B != 0);
    }

    int var28 = mem[32989];
    A = var28;
    if(A << 1 != 0) {
      int var29 = mem16(32987);
      HL(var29);
      int var30 = mem[32986];
      A = var30;
      int var31 = A & 1;
      A = var31;
      int var32 = A;
      int var33 = rlc(var32);
      A = var33;
      int var34 = A + 223 & 255;
      A = var34;
      E = A;
      D = 255;
      int var35 = mem[32989];
      A = var35;
      B = A;
      int var36 = mem[32964];
      A = var36;

      do {
        int var37 = HL();
        mem[var37] = A;
        int var38 = DE();
        int var39 = HL() + var38 & 65535;
        HL(var39);
        int var40 = B - 1 & 255;
        B = var40;
      } while(B != 0);

    }
  }

  public void $36171() {
    C = 0;

    do {
      E = C;
      int var1 = IX();
      int var2 = mem[var1];
      A = var2;
      HL(32928);
      BC(54);
      cpir();
      C = E;
      B = 8;
      int var3 = mem[36189];
      D = var3;

      do {
        int var4 = HL();
        int var5 = mem[var4];
        A = var5;
        int var6 = DE();
        mem[var6] = A;
        int var7 = HL() + 1 & 65535;
        HL(var7);
        int var8 = D + 1 & 255;
        D = var8;
        int var9 = B - 1 & 255;
        B = var9;
      } while(B != 0);

      int var10 = IX() + 1 & 65535;
      IX(var10);
      int var11 = C + 1 & 255;
      C = var11;
    } while(C != 0);

  }

  public void $36288() {
    int var1 = A & 3;
    A = var1;
    C = A;
    int var2 = A;
    int var3 = rlc(var2);
    A = var3;
    int var4 = A;
    int var5 = rlc(var4);
    A = var5;
    int var6 = A;
    int var7 = rlc(var6);
    A = var7;
    int var8 = A + C & 255;
    A = var8;
    int var9 = A + 160 & 255;
    A = var9;
    E = A;
    D = 128;
    int var10 = DE();
    int var11 = mem[var10];
    A = var11;
    int var12 = IX();
    mem[var12] = A;
    int var13 = IX() + 1 & 65535;
    IX(var13);
  }

  public void $36323() {
    int var1 = mem[34261];
    A = var1;
    int var2 = A & 254;
    A = var2;
    int var3 = A - 8 & 255;
    A = var3;
    HL(34255);
    int var4 = HL();
    int var5 = mem[var4];
    int var6 = A + var5 & 255;
    A = var6;
    int var7 = HL();
    mem[var7] = A;
    if(A < 240) {
      int var8 = A - 240;
      F = var8;
      $36508();
      int var9 = mem[32946];
      A = var9;
      int var10 = HL();
      int var11 = mem[var10];
      if(A != var11) {
        int var12 = HL() + 1 & 65535;
        HL(var12);
        int var13 = HL();
        int var14 = mem[var13];
        if(A != var14) {
          int var15 = mem[34261];
          A = var15;
          int var16 = A + 1 & 255;
          A = var16;
          mem[34261] = A;
          int var17 = A - 8;
          int var18 = var17 & 255;
          A = var18;
          if(var17 < 0) {
            int var31 = -A & 255;
            A = var31;
          }

          int var19 = A + 1 & 255;
          A = var19;
          int var20 = A;
          int var21 = rlc(var20);
          A = var21;
          int var22 = A;
          int var23 = rlc(var22);
          A = var23;
          int var24 = A;
          int var25 = rlc(var24);
          A = var25;
          D = A;
          C = 32;
          int var26 = mem[32990];
          A = var26;

          do {
            int var27 = A ^ 24;
            A = var27;
            B = D;

            do {
              int var28 = B - 1 & 255;
              B = var28;
            } while(B != 0);

            int var29 = C - 1 & 255;
            C = var29;
          } while(C != 0);

          int var30 = mem[34261];
          A = var30;
          if(A != 18) {
            if(A != 16) {
              $36401();
            }
          }
        }
      }
    }
  }

  public void $36508() {
    int var1 = A & 240;
    A = var1;
    L = A;
    A = 0;
    int var2 = A << 1;
    F = var2;
    int var3 = L;
    int var4 = rl(var3);
    L = var4;
    int var5 = A + 92;
    int var6 = carry(F) & 255;
    int var7 = var5 + var6;
    A = var7;
    H = A;
    int var8 = mem[34259];
    A = var8;
    int var9 = A & 31;
    A = var9;
    int var10 = A | L;
    A = var10;
    int var11 = A << 1;
    F = var11;
    L = A;
    int var12 = HL();
    wMem16(34259, var12);
  }

  public void $36401() {
    if(A == 13) {
      ;
    }
  }

  public void $36592() {
    int var1 = HL() + 1 & 65535;
    HL(var1);
    int var2 = HL();
    int var3 = mem[var2];
    if(A == var3) {
      ;
    }
  }

  public void $38115() {
    A = 2;
    mem[34257] = A;
  }

  public void $37780() {
    int var1 = HL();
    int var2 = mem[var1];
    A = var2;
    if(A < 12) {
      $37785();
    }
  }

  public void $37785() {
    int var1 = HL();
    mem[var1] = 12;
  }

  public void $38228() {
    E = 192;
    if(A < 192) {
      $38234();
    }
  }

  public void $38234() {
    E = 224;
  }

  public void $38320() {
    int var1 = E | 64;
    E = var1;
  }

  public void $38504() {
    do {
      int var1 = IX();
      int var2 = mem[var1];
      A = var2;
      int var3 = IX() + 1;
      int var4 = mem[var3];
      H = var4;
      int var5 = A | C;
      A = var5;
      L = A;
      int var6 = DE();
      int var7 = mem[var6];
      A = var7;
      int var8 = HL();
      int var9 = mem[var8];
      int var10 = A | var9;
      A = var10;
      int var11 = HL();
      mem[var11] = A;
      int var12 = HL() + 1 & 65535;
      HL(var12);
      int var13 = DE() + 1 & 65535;
      DE(var13);
      int var14 = DE();
      int var15 = mem[var14];
      A = var15;
      int var16 = HL();
      int var17 = mem[var16];
      int var18 = A | var17;
      A = var18;
      int var19 = A << 1;
      F = var19;
      int var20 = HL();
      mem[var20] = A;
      int var21 = IX() + 1 & 65535;
      IX(var21);
      int var22 = IX() + 1 & 65535;
      IX(var22);
      int var23 = DE() + 1 & 65535;
      DE(var23);
      int var24 = B - 1 & 255;
      B = var24;
    } while(B != 0);

  }

  public void $38366() {
    int var1 = mem[34257];
    A = var1;
    if(A << 1 == 0) {
      int var2 = mem[34258];
      A = var2;
      int var3 = A & 3;
      A = var3;
      int var4 = A;
      int var5 = rlc(var4);
      A = var5;
      int var6 = A;
      int var7 = rlc(var6);
      A = var7;
      B = A;
      int var8 = mem[32986];
      A = var8;
      int var9 = A & 1;
      A = var9;
      int var10 = A - 1 & 255;
      A = var10;
      int var11 = A ^ 12;
      A = var11;
      int var12 = A ^ B;
      A = var12;
      int var13 = A & 12;
      A = var13;
      B = A;
    }
  }

  public void $38430() {
    int var1 = mem[32928];
    A = var1;
    int var2 = HL();
    int var3 = mem[var2];
    if(A == var3) {
      $38436();
    }

    int var4 = mem[32955];
    A = var4;
    int var5 = HL();
    int var6 = mem[var5];
    if(A == var6) {
      nextAddress = 37047;
    } else {
      int var7 = A - var6;
      F = var7;
    }
  }

  public void $38490() {
    D = 182;
    A = E;
    int var1 = A ^ 128;
    A = var1;
    E = A;
  }

  public void $38436() {
    A = C;
    int var1 = A & 15;
    A = var1;
    if(A << 1 != 0) {
      int var2 = mem[32928];
      A = var2;
      int var3 = A | 7;
      A = var3;
      int var4 = HL();
      mem[var4] = A;
    }
  }

  public void $38545() {
    H = 7;
    L = A;
    int var1 = L | 128;
    L = var1;
    int var2 = HL();
    int var3 = HL() + var2 & 65535;
    HL(var3);
    int var4 = HL();
    int var5 = HL() + var4 & 65535;
    HL(var5);
    int var6 = HL();
    int var7 = HL() + var6 & 65535;
    HL(var7);
    B = 8;
    $38555();
  }

  public void $38601() {
    int var1 = mem[34254];
    A = var1;
    if(A << 1 != 0) {
      int var6 = in(31);
      A = var6;
      if((A & 16) != 0) {
        return;
      }
    }

    BC(45054);
    int var2 = BC();
    int var3 = in(var2);
    A = var3;
    int var4 = A & 1;
    A = var4;
    int var5 = A - 1;
    F = var5;
  }
}
