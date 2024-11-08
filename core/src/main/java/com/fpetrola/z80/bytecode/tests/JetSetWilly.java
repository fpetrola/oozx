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

package com.fpetrola.z80.bytecode.tests;

import com.fpetrola.z80.minizx.MiniZX;

public class JetSetWilly extends MiniZX {
  public String getProgramBytes() {
    return "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";
  }

  public void $34762() {
    label295:
    while (true) {
      A = 0;
      wMem(34254, A, 34763);
      wMem(34273, A, 34766);
      wMem(34253, A, 34769);
      wMem(34257, A, 34772);
      wMem(34251, A, 34775);
      wMem(34272, A, 34778);
      wMem(34271, A, 34781);
      A = 7;
      wMem(34252, A, 34786);
      A = 208;
      wMem(34255, A, 34791);
      A = 33;
      wMem(33824, A, 34796);
      HL(23988);
      int var1 = HL();
      wMem16(34259, var1, 34802);
      HL(34172);
      int var2 = HL();
      wMem(var2, 48, 34808);
      int var3 = HL() + 1 & 65535;
      HL(var3);
      int var4 = HL();
      wMem(var4, 48, 34811);
      int var5 = HL() + 1 & 65535;
      HL(var5);
      int var6 = HL();
      wMem(var6, 48, 34814);
      H = 164;
      int var7 = mem(41983, 34818);
      A = var7;
      L = A;
      wMem(34270, A, 34822);

      do {
        int var8 = HL();
        int var9 = mem(var8, 34825) | 64;
        int var10 = HL();
        wMem(var10, var9, 34825);
        int var11 = L + 1 & 255;
        L = var11;
      } while (L != 0);

      HL(34274);
      int var12 = HL();
      int var13 = mem(var12, 34833) | 1;
      int var14 = HL();
      wMem(var14, var13, 34833);

      label287:
      while (true) {
        HL(16384);
        DE(16385);
        BC(6143);
        int var15 = HL();
        wMem(var15, 0, 34844);
        ldir();
        HL(38912);
        BC(768);
        ldir();
        HL(23136);
        DE(23137);
        BC(31);
        int var16 = HL();
        wMem(var16, 70, 34865);
        ldir();
        IX(33876);
        DE(20576);
        C = 32;
        $38528();
        DE(22528);

        do {
          int var17 = DE();
          int var18 = mem(var17, 34884);
          A = var18;
          if (A << 1 != 0 && A != 211 && A != 9 && A != 45 && A != 36) {
            C = 0;
            if (A != 8 && A != 41) {
              if (A != 44) {
                if (A != 5) {
                  C = 16;
                }
              } else {
                A = 37;
                int var288 = DE();
                wMem(var288, A, 34928);
              }
            }

            A = E;
            int var275 = A & 1;
            A = var275;
            int var276 = A;
            int var277 = rlc(var276);
            A = var277;
            int var278 = A;
            int var279 = rlc(var278);
            A = var279;
            int var280 = A;
            int var281 = rlc(var280);
            A = var281;
            int var282 = A | C;
            A = var282;
            C = A;
            B = 0;
            HL(33841);
            int var283 = BC();
            int var284 = HL() + var283 & 65535;
            HL(var284);
            int var285 = DE();
            push(var285);
            int var286 = D & 1;
            F = var286;
            D = 64;
            if (F != 0) {
              D = 72;
            }

            B = 8;
            $38555();
            int var287 = pop();
            DE(var287);
          }

          int var19 = DE() + 1 & 65535;
          DE(var19);
          A = D;
        } while (A != 90);

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
        } while (B != 0);

        int var24 = A & 32;
        A = var24;
        if (A << 1 == 0) {
          A = 1;
          wMem(34254, A, 34981);
        }

        HL(34299);
        $38562();
        if (F != 0) {
          break;
        }

        A = 0;
        wMem(34276, A, 34994);

        while (true) {
          $35563();
          HL(23136);
          DE(23137);
          BC(31);
          int var262 = HL();
          wMem(var262, 79, 35009);
          ldir();
          int var263 = mem(34276, 35013);
          A = var263;
          IX(33876);
          E = A;
          D = 0;
          int var264 = DE();
          int var265 = IX() + var264 & 65535;
          IX(var265);
          DE(20576);
          C = 32;
          $38528();
          int var266 = mem(34276, 35033);
          A = var266;
          int var267 = A & 31;
          A = var267;
          int var268 = A + 50 & 255;
          A = var268;
          $38622();
          BC(45054);
          int var269 = BC();
          int var270 = in(var269);
          A = var270;
          int var271 = A & 1;
          A = var271;
          if (A != 1) {
            break label287;
          }

          int var272 = mem(34276, 35054);
          A = var272;
          int var273 = A + 1 & 255;
          A = var273;
          int var274 = A - 224;
          F = var274;
          wMem(34276, A, 35060);
          if (F == 0) {
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

      while (true) {
        while (true) {
          int var25 = mem(33824, 35090);
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
            int var28 = mem(var27, 35115);
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
            int var37 = mem(var36, 35130);
            C = var37;
            int var38 = HL();
            wMem(var38, C, 35133);
            BC(6);
            ldir();
            int var39 = IX() + 1 & 65535;
            IX(var39);
            int var40 = IX() + 1 & 65535;
            IX(var40);
            int var41 = A - 1 & 255;
            A = var41;
          } while (A != 0);

          HL(34255);
          DE(34263);
          BC(7);
          ldir();
          $36147();
          HL(20480);
          DE(20481);
          BC(2047);
          int var42 = HL();
          wMem(var42, 0, 35169);
          ldir();
          IX(32896);
          C = 32;
          DE(20480);
          $38528();
          IX(34132);
          DE(20576);
          C = 32;
          $38528();
          int var43 = mem(32990, 35197);
          A = var43;
          C = 254;
          A = 0;
          wMem(34262, A, 35205);

          while (true) {
            label306:
            {
              label226:
              {
                label302:
                {
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
                  int var44 = mem(34271, 35273);
                  A = var44;
                  if (A != 3) {
                    $36307();
                    if (isNextPC(37048)) {
                      break label302;
                    }

                    if (isNextPC(38043) || isNextPC(38061) || isNextPC(38134) || isNextPC(38095)) {
                      break;
                    }
                  }

                  int var45 = mem(34255, 35281);
                  A = var45;
                  if (A >= 225) {
                    $38064();
                    if (isNextPC(38095)) {
                      break;
                    }
                  }

                  int var46 = mem(34271, 35289);
                  A = var46;
                  if (A != 3) {
                    $38344();
                    if (isNextPC(37048)) {
                      break label302;
                    }
                  }

                  int var47 = mem(34271, 35297);
                  A = var47;
                  if (A == 2) {
                    $38276();
                  }

                  int var48 = A - 2;
                  F = var48;
                  $38196();
                  if (!isNextPC(37048)) {
                    $37310();
                    if (!isNextPC(37048)) {
                      $38137();
                      $37841();
                      break label226;
                    }
                  }
                }

                A = 255;
                wMem(34257, A, 37050);
              }

              HL(24576);
              DE(16384);
              BC(4096);
              ldir();
              int var49 = mem(34271, 35328);
              A = var49;
              int var50 = A & 2;
              A = var50;
              int var51 = A;
              int var52 = rrc(var51);
              A = var52;
              HL(34258);
              int var53 = HL();
              int var54 = mem(var53, 35337);
              int var55 = A | var54;
              A = var55;
              int var56 = HL();
              wMem(var56, A, 35338);
              int var57 = mem(34253, 35339);
              A = var57;
              if (A << 1 != 0) {
                int var253 = A - 1 & 255;
                A = var253;
                wMem(34253, A, 35346);
                int var254 = A;
                int var255 = rlc(var254);
                A = var255;
                int var256 = A;
                int var257 = rlc(var256);
                A = var257;
                int var258 = A;
                int var259 = rlc(var258);
                A = var259;
                int var260 = A & 56;
                A = var260;
                HL(23552);
                DE(23553);
                BC(511);
                int var261 = HL();
                wMem(var261, A, 35363);
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
              int var58 = mem(34251, 35401);
              A = var58;
              int var59 = A + 1 & 255;
              A = var59;
              F = A;
              wMem(34251, A, 35405);
              if (F == 0) {
                IX(34175);
                int var226 = IX() + 4;
                int var227 = mem(var226, 35414) + 1 & 255;
                wMem(var226, var227, 35414);
                int var228 = IX() + 4;
                int var229 = mem(var228, 35417);
                A = var229;
                if (A == 58) {
                  int var230 = IX() + 4;
                  wMem(var230, 48, 35424);
                  int var231 = IX() + 3;
                  int var232 = mem(var231, 35428) + 1 & 255;
                  wMem(var231, var232, 35428);
                  int var233 = IX() + 3;
                  int var234 = mem(var233, 35431);
                  A = var234;
                  if (A == 54) {
                    int var235 = IX() + 3;
                    wMem(var235, 48, 35438);
                    int var236 = IX();
                    int var237 = mem(var236, 35442);
                    A = var237;
                    if (A == 49) {
                      int var244 = IX() + 1;
                      int var245 = mem(var244, 35449) + 1 & 255;
                      wMem(var244, var245, 35449);
                      int var246 = IX() + 1;
                      int var247 = mem(var246, 35452);
                      A = var247;
                      if (A == 51) {
                        int var248 = IX() + 5;
                        int var249 = mem(var248, 35459);
                        A = var249;
                        if (A == 112) {
                          continue label295;
                        }

                        int var250 = IX();
                        wMem(var250, 32, 35467);
                        int var251 = IX() + 1;
                        wMem(var251, 49, 35471);
                        int var252 = IX() + 5;
                        wMem(var252, 112, 35475);
                      }
                    } else {
                      int var238 = IX() + 1;
                      int var239 = mem(var238, 35481) + 1 & 255;
                      wMem(var238, var239, 35481);
                      int var240 = IX() + 1;
                      int var241 = mem(var240, 35484);
                      A = var241;
                      if (A == 58) {
                        int var242 = IX() + 1;
                        wMem(var242, 48, 35491);
                        int var243 = IX();
                        wMem(var243, 49, 35495);
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
              if (A << 1 == 0) {
                continue label295;
              }

              int var66 = mem(34272, 35515);
              A = var66;
              int var67 = A + 1 & 255;
              A = var67;
              F = A;
              wMem(34272, A, 35519);
              if (F != 0) {
                B = 253;
                int var223 = BC();
                int var224 = in(var223);
                A = var224;
                int var225 = A & 31;
                A = var225;
                if (A == 31) {
                  break label306;
                }

                DE(0);
              }

              while (true) {
                B = 2;
                int var68 = BC();
                int var69 = in(var68);
                A = var69;
                int var70 = A & 31;
                A = var70;
                if (A != 31) {
                  HL(39424);
                  DE(23040);
                  BC(256);
                  ldir();
                  int var71 = mem(32990, 35602);
                  A = var71;
                  break;
                }

                int var219 = E + 1 & 255;
                E = var219;
                if (E == 0) {
                  int var220 = D + 1 & 255;
                  D = var220;
                  if (D == 0) {
                    int var221 = mem(34275, 35553);
                    A = var221;
                    if (A != 10) {
                      $35563();
                    }

                    int var222 = A - 10;
                    F = var222;
                  }
                }
              }
            }

            A = mem(34257, 35607);
            if (A == 255) {
              A = 71;

              do {
                HL(22528);
                DE(22529);
                BC(511);
                wMem(HL(), A, 35852);
                ldir();
                E = A;
                A = ~A;
                A = A & 7;
                A = rlc(A);
                A = rlc(A);
                A = rlc(A);
                A = A | 7;
                D = A;
                C = E;
                C = rrc(C);
                C = rrc(C);
                C = rrc(C);
                A = A | 16;
                A = 0;

                do {
                  A = A ^ 24;
                  B = D;

                  do {
                    B = B - 1 & 255;
                  } while (B != 0);

                  C = C - 1 & 255;
                } while (C != 0);

                A = E;
                A = A - 1 & 255;
              } while (A != 63);

              HL(34252);
              A = mem(HL(), 35894);
              if (A << 1 == 0) {
                HL(16384);
                DE(16385);
                BC(4095);
                int var96 = HL();
                wMem(var96, 0, 35923);
                ldir();
                A = 0;
                wMem(34276, A, 35928);
                DE(40256);
                HL(18575);
                C = 0;
                $37974();
                DE(40032);
                HL(18639);
                C = 0;
                $37974();

                do {
                  A = mem(34276, 35953);
                  C = A;
                  B = 130;
                  A = mem(BC(), 35959);
                  A = A | 15;
                  L = A;
                  BC(BC() + 1 & 65535);
                  A = mem(BC(), 35964);
                  A = A - 32 & 255;
                  H = A;
                  DE(40000);
                  C = 0;
                  $37974();
                  A = mem(34276, 35976);
                  A = ~A;
                  E = A;
                  A = 0;
                  BC(64);

                  do {
                    A = A ^ 24;
                    B = E;

                    do {
                      B = B - 1 & 255;
                    } while (B != 0);

                    C = C - 1 & 255;
                  } while (C != 0);

                  HL(22528);
                  DE(22529);
                  BC(511);
                  A = mem(34276, 36004);
                  A = A & 12;
                  A = rlc(A);
                  A = A | 71;
                  wMem(HL(), A, 36012);
                  ldir();
                  A = A & 250;
                  A = A | 2;
                  wMem(22991, A, 36019);
                  wMem(22992, A, 36022);
                  wMem(23023, A, 36025);
                  wMem(23024, A, 36028);
                  A = mem(34276, 36031);
                  A = A + 4 & 255;
                  wMem(34276, A, 36036);
                } while (A != 196);

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

                while (true) {
                  do {
                    B = B - 1 & 255;
                  } while (B != 0);

                  A = C;
                  A = A & 7;
                  A = A | 64;
                  wMem(22730, A, 36079);
                  A = A + 1 & 255;
                  A = A & 7;
                  A = A | 64;
                  wMem(22731, A, 36087);
                  A = A + 1 & 255;
                  A = A & 7;
                  A = A | 64;
                  wMem(22732, A, 36095);
                  A = A + 1 & 255;
                  A = A & 7;
                  A = A | 64;
                  wMem(22733, A, 36103);
                  A = A + 1 & 255;
                  A = A & 7;
                  A = A | 64;
                  wMem(22738, A, 36111);
                  A = A + 1 & 255;
                  A = A & 7;
                  A = A | 64;
                  wMem(22739, A, 36119);
                  A = A + 1 & 255;
                  A = A & 7;
                  A = A | 64;
                  wMem(22740, A, 36127);
                  A = A + 1 & 255;
                  A = A & 7;
                  A = A | 64;
                  wMem(22741, A, 36135);
                  C = C - 1 & 255;
                  if (C == 0) {
                    D = D - 1 & 255;
                    if (D == 0) {
                      continue label295;
                    }
                  }
                }
              }

              wMem(HL(), mem(HL(), 35899) - 1 & 255, 35899);
              HL(34263);
              DE(34255);
              BC(7);
              ldir();
              break;
            }

            B = 191;
            HL(34274);
            int var149 = BC();
            int var150 = in(var149);
            A = var150;
            int var151 = A & 31;
            A = var151;
            if (A != 31) {
              int var214 = HL();
              if ((mem(var214, 35628) & 1) == 0) {
                int var215 = HL();
                int var216 = mem(var215, 35632);
                A = var216;
                int var217 = A ^ 3;
                A = var217;
                int var218 = HL();
                wMem(var218, A, 35635);
              }
            } else {
              int var152 = HL();
              int var153 = mem(var152, 35638) & -2;
              int var154 = HL();
              wMem(var154, var153, 35638);
            }

            int var155 = HL();
            if ((mem(var155, 35640) & 2) == 0) {
              A = 0;
              wMem(34272, A, 35645);
              int var192 = mem(34273, 35648);
              A = var192;
              int var193 = A + 1 & 255;
              A = var193;
              wMem(34273, A, 35652);
              int var194 = A & 126;
              A = var194;
              int var195 = A;
              int var196 = rrc(var195);
              A = var196;
              E = A;
              D = 0;
              HL(34399);
              int var197 = DE();
              int var198 = HL() + var197 & 65535;
              HL(var198);
              int var199 = mem(34252, 35665);
              A = var199;
              int var200 = A;
              int var201 = rlc(var200);
              A = var201;
              int var202 = A;
              int var203 = rlc(var202);
              A = var203;
              int var204 = A - 28 & 255;
              A = var204;
              int var205 = -A & 255;
              A = var205;
              int var206 = HL();
              int var207 = mem(var206, 35674);
              int var208 = A + var207 & 255;
              A = var208;
              D = A;
              int var209 = mem(32990, 35676);
              A = var209;
              E = D;
              BC(3);

              do {
                do {
                  int var210 = E - 1 & 255;
                  E = var210;
                  if (E == 0) {
                    E = D;
                    int var213 = A ^ 24;
                    A = var213;
                  }

                  int var211 = B - 1 & 255;
                  B = var211;
                } while (B != 0);

                int var212 = C - 1 & 255;
                C = var212;
              } while (C != 0);
            }

            BC(61438);
            int var156 = BC();
            int var157 = in(var156);
            A = var157;
            if ((A & 2) == 0) {
              int var182 = A & 16;
              A = var182;
              int var183 = A ^ 16;
              A = var183;
              int var184 = A;
              int var185 = rlc(var184);
              A = var185;
              D = A;
              int var186 = mem(34275, 35712);
              A = var186;
              if (A == 10) {
                BC(63486);
                int var187 = BC();
                int var188 = in(var187);
                A = var188;
                int var189 = ~A;
                A = var189;
                int var190 = A & 31;
                A = var190;
                int var191 = A | D;
                A = var191;
                wMem(33824, A, 35729);
                break;
              }
            }

            int var158 = mem(34275, 35735);
            A = var158;
            if (A != 10) {
              int var159 = mem(33824, 35743);
              A = var159;
              if (A == 28) {
                int var160 = mem(34255, 35751);
                A = var160;
                if (A == 208) {
                  int var161 = mem(34275, 35759);
                  A = var161;
                  int var162 = A;
                  int var163 = rlc(var162);
                  A = var163;
                  E = A;
                  D = 0;
                  IX(34279);
                  int var164 = DE();
                  int var165 = IX() + var164 & 65535;
                  IX(var165);
                  BC(64510);
                  int var166 = BC();
                  int var167 = in(var166);
                  A = var167;
                  int var168 = A & 31;
                  A = var168;
                  int var169 = IX();
                  int var170 = mem(var169, 35779);
                  if (A != var170) {
                    if (A != 31) {
                      int var180 = IX();
                      int var181 = mem(var180, 35789);
                      if (A != var181) {
                        A = 0;
                        wMem(34275, A, 35796);
                      }
                    }
                  } else {
                    B = 223;
                    int var171 = BC();
                    int var172 = in(var171);
                    A = var172;
                    int var173 = A & 31;
                    A = var173;
                    int var174 = IX() + 1;
                    int var175 = mem(var174, 35808);
                    if (A != var175) {
                      if (A != 31) {
                        int var178 = IX();
                        int var179 = mem(var178, 35818);
                        if (A != var179) {
                          A = 0;
                          wMem(34275, A, 35825);
                        }
                      }
                    } else {
                      int var176 = mem(34275, 35831);
                      A = var176;
                      int var177 = A + 1 & 255;
                      A = var177;
                      wMem(34275, A, 35835);
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

  public void $35211() {
    A = mem(34252, 35211);
    HL(20640);
    if (A << 1 != 0) {
      B = A;

      do {
        C = 0;
        push(HL());
        push(BC());
        A = mem(34273, 35224);
        A = rlc(A);
        A = rlc(A);
        A = rlc(A);
        A = A & 96;
        E = A;
        D = 157;
        $37974();
        BC(pop());
        HL(pop());
        HL(HL() + 1 & 65535);
        HL(HL() + 1 & 65535);
        B = B - 1 & 255;
      } while (B != 0);

    }
  }

  public void $35563() {
    HL(22528);
    int var1 = HL();
    int var2 = mem(var1, 35566);
    A = var2;
    int var3 = A & 7;
    A = var3;

    do {
      int var4 = HL();
      int var5 = mem(var4, 35571);
      A = var5;
      int var6 = A + 3 & 255;
      A = var6;
      int var7 = A & 7;
      A = var7;
      D = A;
      int var8 = HL();
      int var9 = mem(var8, 35577);
      A = var9;
      int var10 = A + 24 & 255;
      A = var10;
      int var11 = A & 184;
      A = var11;
      int var12 = A | D;
      A = var12;
      int var13 = HL();
      wMem(var13, A, 35583);
      int var14 = HL() + 1 & 65535;
      HL(var14);
      A = H;
    } while (A != 91);

    int var15 = A - 91;
    F = var15;
  }

  public void $36147() {
    $36203();
    IX(24064);
    A = 112;
    wMem(36189, A, 36156);
    $36171();
    IX(24320);
    A = 120;
    wMem(36189, A, 36168);
    $36171();
  }

  public void $36171() {
    C = 0;

    do {
      E = C;
      int var1 = IX();
      int var2 = mem(var1, 36174);
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
        int var5 = mem(var4, 36190);
        A = var5;
        int var6 = DE();
        wMem(var6, A, 36191);
        int var7 = HL() + 1 & 65535;
        HL(var7);
        int var8 = D + 1 & 255;
        D = var8;
        int var9 = B - 1 & 255;
        B = var9;
      } while (B != 0);

      int var10 = IX() + 1 & 65535;
      IX(var10);
      int var11 = C + 1 & 255;
      C = var11;
    } while (C != 0);

  }

  public void $36203() {
    HL(32768);
    IX(24064);

    do {
      int var1 = HL();
      int var2 = mem(var1, 36210);
      A = var2;
      int var3 = A;
      int var4 = rlc(var3);
      A = var4;
      int var5 = A;
      int var6 = rlc(var5);
      A = var6;
      $36288();
      int var7 = HL();
      int var8 = mem(var7, 36216);
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
      int var18 = mem(var17, 36224);
      A = var18;
      int var19 = A;
      int var20 = rrc(var19);
      A = var20;
      int var21 = A;
      int var22 = rrc(var21);
      A = var22;
      $36288();
      int var23 = HL();
      int var24 = mem(var23, 36230);
      A = var24;
      $36288();
      int var25 = HL() + 1 & 65535;
      HL(var25);
      A = L;
      int var26 = A & 128;
      A = var26;
    } while (A << 1 == 0);

//    A = mem(32985, 36240);
    Conveyor conveyor = (Conveyor) objectMemory[32985];
    A = conveyor.value;
    conveyor.fillConveyorAttributes();

    int var28 = mem(32989, 36257);
    A = var28;
    if (A << 1 != 0) {
      int var29 = mem16(32987, 36262);
      HL(var29);
      int var30 = mem(32986, 36265);
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
      int var35 = mem(32989, 36276);
      A = var35;
      B = A;
      int var36 = mem(32964, 36280);
      A = var36;

      do {
        int var37 = HL();
        wMem(var37, A, 36283);
        int var38 = DE();
        int var39 = HL() + var38 & 65535;
        HL(var39);
        int var40 = B - 1 & 255;
        B = var40;
      } while (B != 0);

    }
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
    int var11 = mem(var10, 36300);
    A = var11;
    int var12 = IX();
    wMem(var12, A, 36301);
    int var13 = IX() + 1 & 65535;
    IX(var13);
  }

  public void $36307() {
    label216:
    {
      label213:
      {
        label228:
        {
          A = mem(34262, 36307);
          A = A - 1 & 255;
          if ((A & 128) != 0) {
            A = mem(34257, 36316);
            if (A == 1) {
              if (extracted4()) return;
              if (extracted3()) break label216;
              if (extracted()) return;
              if (extracted6()) break label213;
            }

            if (extracted7()) break label228;
            if (extracted1()) return;
            if (extracted2()) break label228;
          }

          E = 255;
          A = mem(34262, 36566);
          A = A - 1 & 255;
          if ((A & 128) != 0) if (extracted14()) return;

          extracted11();
          A = mem(34254, 36658);
          if (A << 1 != 0) extracted10();

          extracted12();
          A = in(BC());
          A = A & 31;
          if (A == 31) if (extracted5()) break label213;

          A = mem(34271, 36751);
          if ((A & 2) == 0) if (extracted9()) return;
          break label213;
        }

        A = mem(34257, 36450);
        if (A != 1) {
          extracted13();
          return;
        }
      }

      A = mem(34256, 36796);
      A = A & 2;
      if (A << 1 == 0) {
        return;
      }

      A = mem(34262, 36802);
      A = A - 1 & 255;
      if ((A & 128) == 0) {
        return;
      }

      Movement movement = (Movement) objectMemory[34256];
      A = movement.value;
      if (movement.move())
        return;

      A = mem(33002, 38046);
      wMem(33824, A, 38049);
      A = mem(34259, 38052);
      A = A & 224;
      wMem(34259, A, 38057);
      nextAddress = 38061;
      return;
    }

    A = mem(34255, 36540);
    A = A + 16 & 255;
    A = A & 240;
    wMem(34255, A, 36547);
    $36508();
    A = 2;
    wMem(34257, A, 36555);
    HL(34256);
    int var237 = HL();
    int var238 = mem(var237, 36561) & -3;
    int var239 = HL();
    wMem(var239, var238, 36561);
  }

  private boolean extracted14() {
    label227:
    {
      A = mem(34257, 36574);
      if (A >= 12) {
        nextAddress = 37048;
        return true;
      }

      A = 0;
      wMem(34257, A, 36583);
      A = mem(32973, 36586);
      if (A != mem(HL(), 36589)) {
        if (extracted8()) break label227;
      }

      //A = mem(32982, 36596);
      ConveyorMover o = (ConveyorMover) objectMemory[32982];
      A = o.value;
      A = A - 3 & 255;
      E = A;
    }
    return false;
  }

  private void extracted13() {
    HL(34256);
    int var183 = HL();
    int var184 = mem(var183, 36461) & -3;
    int var185 = HL();
    wMem(var185, var184, 36461);
    int var186 = mem(34257, 36463);
    A = var186;
    if (A << 1 == 0) {
      A = 2;
      wMem(34257, A, 36536);
      return;
    }

    int var187 = A + 1 & 255;
    A = var187;
    if (A == 16) {
      A = 12;
    }

    wMem(34257, A, 36477);
    int var188 = A;
    int var189 = rlc(var188);
    A = var189;
    int var190 = A;
    int var191 = rlc(var190);
    A = var191;
    int var192 = A;
    int var193 = rlc(var192);
    A = var193;
    int var194 = A;
    int var195 = rlc(var194);
    A = var195;
    D = A;
    C = 32;
    int var196 = mem(32990, 36487);
    A = var196;

    do {
      int var197 = A ^ 24;
      A = var197;
      B = D;

      do {
        int var198 = B - 1 & 255;
        B = var198;
      } while (B != 0);

      int var199 = C - 1 & 255;
      C = var199;
    } while (C != 0);

    int var200 = mem(34255, 36500);
    A = var200;
    int var201 = A + 8 & 255;
    A = var201;
    wMem(34255, A, 36505);
    $36508();
    return;
  }

  private void extracted12() {
    C = 0;
    A = E;
    A = A & 42;
    if (A != 42) {
      C = 4;
      A = 0;
      wMem(34272, A, 36686);
    }

    A = E;
    A = A & 21;
    if (A != 21) {
      C = C | 8;
      A = 0;
      wMem(34272, A, 36699);
    }

    A = mem(34256, 36702);
    A = A + C & 255;
    C = A;
    B = 0;
    HL(33825);
    HL(HL() + BC() & 65535);
    A = mem(HL(), 36713);
    wMem(34256, A, 36714);
    BC(32510);
  }

  private void extracted11() {
    BC(57342);
    A = in(BC());
    A = A & 31;
    A = A | 32;
    A = A & E;
    E = A;
    A = mem(34271, 36613);
    A = A & 2;
    A = rrc(A);
    A = A ^ E;
    E = A;
    BC(64510);
    A = in(BC());
    A = A & 31;
    A = rlc(A);
    A = A | 1;
    A = A & E;
    E = A;
    B = 231;
    A = in(BC());
    A = rrc(A);
    A = A | 247;
    A = A & E;
    E = A;
    B = 239;
    A = in(BC());
    A = A | 251;
    A = A & E;
    E = A;
    A = in(BC());
    A = rrc(A);
    A = A | 251;
    A = A & E;
    E = A;
  }

  private void extracted10() {
    BC(31);
    A = in(BC());
    A = A & 3;
    A = ~A;
    A = A & E;
    E = A;
  }

  private boolean extracted9() {
    A = 0;
    wMem(34261, A, 36759);
    wMem(34272, A, 36762);
    int var150 = A + 1 & 255;
    A = var150;
    wMem(34257, A, 36766);
    int var151 = mem(34262, 36769);
    A = var151;
    int var152 = A - 1 & 255;
    A = var152;
    if ((A & 128) == 0) {
      A = 240;
      wMem(34262, A, 36779);
      int var153 = mem(34255, 36782);
      A = var153;
      int var154 = A & 240;
      A = var154;
      int var155 = A << 1;
      F = var155;
      wMem(34255, A, 36787);
      HL(34256);
      int var156 = HL();
      int var157 = mem(var156, 36793) | 2;
      int var158 = HL();
      wMem(var158, var157, 36793);
      return true;
    }
    return false;
  }

  private boolean extracted5() {
    B = 239;
    int var159 = BC();
    A = in(var159);
    if ((A & 1) != 0) {
      A = mem(34254, 36736);
      if (A << 1 == 0) {
        return true;
      }

      BC(31);
      A = in(BC());
      if ((A & 16) == 0) {
        return true;
      }
    }
    return false;
  }

  private boolean extracted8() {
    HL(HL() + 1 & 65535);
    if (A != mem(HL(), 36593)) {
      return true;
    }
    return false;
  }

  private boolean extracted() {
    int var243 = mem(34261, 36355);
    A = var243;
    int var244 = A + 1 & 255;
    A = var244;
    wMem(34261, A, 36359);
    int var245 = A - 8;
    int var246 = var245 & 255;
    A = var246;
    if (var245 < 0) {
      int var259 = -A & 255;
      A = var259;
    }

    int var247 = A + 1 & 255;
    A = var247;
    int var248 = A;
    int var249 = rlc(var248);
    A = var249;
    int var250 = A;
    int var251 = rlc(var250);
    A = var251;
    int var252 = A;
    int var253 = rlc(var252);
    A = var253;
    D = A;
    C = 32;
    int var254 = mem(32990, 36376);
    A = var254;

    do {
      int var255 = A ^ 24;
      A = var255;
      B = D;

      do {
        int var256 = B - 1 & 255;
        B = var256;
      } while (B != 0);

      int var257 = C - 1 & 255;
      C = var257;
    } while (C != 0);
    int var258 = mem(34261, 36389);
    A = var258;
    if (A == 18) {
      A = 6;
      wMem(34257, A, 36530);
      return true;
    }
    return false;
  }

  private boolean extracted7() {
    int var180 = mem(34255, 36406);
    A = var180;
    int var181 = A & 14;
    A = var181;
    if (A << 1 != 0) {
      return true;
    }
    return false;
  }

  private boolean extracted6() {
    if (A != 16 && A != 13) {
      return true;
    }
    return false;
  }

  private boolean extracted4() {
    int var223 = mem(34261, 36323);
    A = var223;
    int var224 = A & 254;
    A = var224;
    int var225 = A - 8 & 255;
    A = var225;
    HL(34255);
    int var226 = HL();
    int var227 = mem(var226, 36333);
    int var228 = A + var227 & 255;
    A = var228;
    int var229 = HL();
    wMem(var229, A, 36334);
    if (A >= 240) {
      return true;
    }
    return false;
  }

  private boolean extracted3() {
    int var230 = A - 240;
    F = var230;
    $36508();
    int var231 = mem(32946, 36343);
    A = var231;
    int var232 = HL();
    int var233 = mem(var232, 36346);
    if (A == var233) {
      return true;
    }

    int var240 = HL() + 1 & 65535;
    HL(var240);
    int var241 = HL();
    int var242 = mem(var241, 36351);
    if (A == var242) {
      return true;
    }
    return false;
  }

  private boolean extracted2() {
    int var209 = mem(32955, 36425);
    A = var209;
    int var210 = HL();
    int var211 = mem(var210, 36428);
    if (A == var211) {
      return true;
    }

    int var212 = HL() + 1 & 65535;
    HL(var212);
    int var213 = mem(32955, 36432);
    A = var213;
    int var214 = HL();
    int var215 = mem(var214, 36435);
    if (A == var215) {
      return true;
    }

    int var216 = mem(32928, 36438);
    A = var216;
    int var217 = HL();
    int var218 = mem(var217, 36441);
    int var219 = A - var218;
    F = var219;
    int var220 = HL() - 1 & 65535;
    HL(var220);
    if (F == 0) {
      int var221 = HL();
      int var222 = mem(var221, 36446);
      if (A == var222) {
        return true;
      }
    }
    return false;
  }

  private boolean extracted1() {
    int var202 = mem16(34259, 36413);
    HL(var202);
    DE(64);
    int var203 = DE();
    int var204 = HL() + var203 & 65535;
    HL(var204);
    if ((H & 2) != 0) {
      int var205 = mem(33004, 38098);
      A = var205;
      wMem(33824, A, 38101);
      A = 0;
      wMem(34255, A, 38105);
      int var206 = mem(34257, 38108);
      A = var206;
      if (A < 11) {
        A = 2;
        wMem(34257, A, 38117);
      }

      int var207 = mem(34259, 38120);
      A = var207;
      int var208 = A & 31;
      A = var208;
      wMem(34259, A, 38125);
      A = 92;
      wMem(34260, A, 38130);
      nextAddress = 38134;
      return true;
    }
    return false;
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
    int var8 = mem(34259, 36517);
    A = var8;
    int var9 = A & 31;
    A = var9;
    int var10 = A | L;
    A = var10;
    int var11 = A << 1;
    F = var11;
    L = A;
    int var12 = HL();
    wMem16(34259, var12, 36524);
  }

  public void $37056() {
    IX(33024);

    while (true) {
      A = mem(IX(), 37060);
      if (A == 255) {
        return;
      }

      A = A & 3;
      if (A << 1 != 0) {
        if (A != 1) {
          if (A != 2) {
            if ((mem(IX(), 37081) & 128) != 0) {
              A = mem(IX() + 1, 37087);
              if ((A & 128) != 0) {
                A = A - 2 & 255;
                if (A < 148) {
                  A = A - 2 & 255;
                  if (A == 128) {
                    A = 0;
                  }
                }
              } else {
                A = A + 2 & 255;
                if (A < 18) {
                  A = A + 2 & 255;
                }
              }
            } else {
              A = mem(IX() + 1, 37119);
              if ((A & 128) == 0) {
                A = A - 2 & 255;
                if (A < 20) {
                  A = A - 2 & 255;
                  if (A << 1 == 0) {
                    A = 128;
                  }
                }
              } else {
                A = A + 2 & 255;
                if (A < 146) {
                  A = A + 2 & 255;
                }
              }
            }

            wMem(IX() + 1, A, 37149);
            A = A & 127;
            if (A == mem(IX() + 7, 37154)) {
              A = mem(IX(), 37160);
              A = A ^ 128;
              wMem(IX(), A, 37165);
            }
          } else {
            label81:
            {
              A = mem(IX(), 37247);
              A = A ^ 8;
              wMem(IX(), A, 37252);
              A = A & 24;
              if (A << 1 != 0) {
                A = mem(IX(), 37259);
                A = A + 32 & 255;
                wMem(IX(), A, 37264);
              }

              A = mem(IX() + 3, 37267);
              A = A + mem(IX() + 4, 37270) & 255;
              wMem(IX() + 3, A, 37273);
              if (A < mem(IX() + 7, 37276)) {
                int var51 = mem(IX() + 6, 37281);
                if (A != var51 && A >= var51) {
                  break label81;
                }

                A = mem(IX() + 6, 37288);
                wMem(IX() + 3, A, 37291);
              }

              A = mem(IX() + 4, 37294);
              A = -A & 255;
              wMem(IX() + 4, A, 37299);
            }
          }
        } else {
          if ((mem(IX(), 37171) & 128) == 0) {
            A = mem(IX(), 37177);
            A = A - 32 & 255;
            A = A & 127;
            wMem(IX(), A, 37184);
            if (A >= 96) {
              A = mem(IX() + 2, 37191);
              A = A & 31;
              if (A != mem(IX() + 6, 37196)) {
                wMem(IX() + 2, mem(IX() + 2, 37201) - 1 & 255, 37201);
              } else {
                wMem(IX(), 129, 37206);
              }
            }
          } else {
            A = mem(IX(), 37212);
            A = A + 32 & 255;
            A = A | 128;
            wMem(IX(), A, 37219);
            if (A < 160) {
              A = mem(IX() + 2, 37226);
              A = A & 31;
              if (A != mem(IX() + 7, 37231)) {
                int var18 = IX() + 2;
                wMem(var18, mem(var18, 37236) + 1 & 255, 37236);
              } else {
                wMem(IX(), 97, 37241);
              }
            }
          }
        }
      }

      DE(8);
      IX(IX() + DE() & 65535);
    }
  }

  public void $37310() {
    IX(33024);

    while (true) {
      int var1 = IX();
      int var2 = mem(var1, 37314);
      A = var2;
      if (A == 255) {
        return;
      }

      int var3 = A & 7;
      A = var3;
      if (A << 1 != 0) {
        if (A != 3) {
          if (A != 4) {
            int var164 = IX() + 3;
            int var165 = mem(var164, 37334);
            E = var165;
            D = 130;
            int var166 = DE();
            int var167 = mem(var166, 37339);
            A = var167;
            L = A;
            int var168 = IX() + 2;
            int var169 = mem(var168, 37341);
            A = var169;
            int var170 = A & 31;
            A = var170;
            int var171 = A + L & 255;
            A = var171;
            L = A;
            A = E;
            int var172 = A;
            int var173 = rlc(var172);
            A = var173;
            int var174 = A & 1;
            A = var174;
            int var175 = A | 92;
            A = var175;
            H = A;
            DE(31);
            int var176 = IX() + 1;
            int var177 = mem(var176, 37358);
            A = var177;
            int var178 = A & 15;
            A = var178;
            int var179 = A + 56 & 255;
            A = var179;
            int var180 = A & 71;
            A = var180;
            C = A;
            int var181 = HL();
            int var182 = mem(var181, 37368);
            A = var182;
            int var183 = A & 56;
            A = var183;
            int var184 = A ^ C;
            A = var184;
            C = A;
            int var185 = HL();
            wMem(var185, C, 37373);
            int var186 = HL() + 1 & 65535;
            HL(var186);
            int var187 = HL();
            wMem(var187, C, 37375);
            int var188 = DE();
            int var189 = HL() + var188 & 65535;
            HL(var189);
            int var190 = HL();
            wMem(var190, C, 37377);
            int var191 = HL() + 1 & 65535;
            HL(var191);
            int var192 = HL();
            wMem(var192, C, 37379);
            int var193 = IX() + 3;
            int var194 = mem(var193, 37380);
            A = var194;
            int var195 = A & 14;
            A = var195;
            if (A << 1 != 0) {
              int var218 = DE();
              int var219 = HL() + var218 & 65535;
              HL(var219);
              int var220 = HL();
              wMem(var220, C, 37388);
              int var221 = HL() + 1 & 65535;
              HL(var221);
              int var222 = HL();
              wMem(var222, C, 37390);
            }

            C = 1;
            int var196 = IX() + 1;
            int var197 = mem(var196, 37393);
            A = var197;
            int var198 = IX();
            int var199 = mem(var198, 37396);
            int var200 = A & var199;
            A = var200;
            int var201 = IX() + 2;
            int var202 = mem(var201, 37399);
            int var203 = A | var202;
            A = var203;
            int var204 = A & 224;
            A = var204;
            E = A;
            int var205 = IX() + 5;
            int var206 = mem(var205, 37405);
            D = var206;
            H = 130;
            int var207 = IX() + 3;
            int var208 = mem(var207, 37410);
            L = var208;
            int var209 = IX() + 2;
            int var210 = mem(var209, 37413);
            A = var210;
            int var211 = A & 31;
            A = var211;
            int var212 = HL();
            int var213 = mem(var212, 37418);
            int var214 = A | var213;
            A = var214;
            int var215 = HL() + 1 & 65535;
            HL(var215);
            int var216 = HL();
            int var217 = mem(var216, 37420);
            H = var217;
            L = A;
            $37974();
            if (F != 0) {
              nextAddress = 37048;
              return;
            }
          } else {
            int var113 = IX();
            if ((mem(var113, 37431) & 128) == 0) {
              int var162 = IX() + 4;
              int var163 = mem(var162, 37437) - 1 & 255;
              wMem(var162, var163, 37437);
              C = 44;
            } else {
              int var114 = IX() + 4;
              int var115 = mem(var114, 37444) + 1 & 255;
              wMem(var114, var115, 37444);
              C = 244;
            }

            A = mem(IX() + 4, 37449);
            if (A != C) {
              int var118 = A & 224;
              A = var118;
              if (A << 1 == 0) {
                int var119 = IX() + 2;
                int var120 = mem(var119, 37479);
                E = var120;
                D = 130;
                int var121 = DE();
                int var122 = mem(var121, 37484);
                A = var122;
                int var123 = IX() + 4;
                int var124 = mem(var123, 37485);
                int var125 = A + var124 & 255;
                A = var125;
                L = A;
                A = E;
                int var126 = A & 128;
                A = var126;
                int var127 = A;
                int var128 = rlc(var127);
                A = var128;
                int var129 = A | 92;
                A = var129;
                H = A;
                int var130 = IX() + 5;
                wMem(var130, 0, 37496);
                int var131 = HL();
                int var132 = mem(var131, 37500);
                A = var132;
                int var133 = A & 7;
                A = var133;
                if (A == 7) {
                  int var156 = IX() + 5;
                  int var157 = mem(var156, 37507) - 1 & 255;
                  wMem(var156, var157, 37507);
                }

                int var134 = HL();
                int var135 = mem(var134, 37510);
                A = var135;
                int var136 = A | 7;
                A = var136;
                int var137 = HL();
                wMem(var137, A, 37513);
                int var138 = DE() + 1 & 65535;
                DE(var138);
                int var139 = DE();
                int var140 = mem(var139, 37515);
                A = var140;
                H = A;
                int var141 = H - 1 & 255;
                H = var141;
                int var142 = IX() + 6;
                int var143 = mem(var142, 37518);
                A = var143;
                int var144 = HL();
                wMem(var144, A, 37521);
                int var145 = H + 1 & 255;
                H = var145;
                int var146 = HL();
                int var147 = mem(var146, 37523);
                A = var147;
                int var148 = IX() + 5;
                int var149 = mem(var148, 37524);
                int var150 = A & var149;
                A = var150;
                if (A << 1 != 0) {
                  nextAddress = 37048;
                  return;
                }

                int var151 = HL();
                wMem(var151, 255, 37530);
                int var152 = H + 1 & 255;
                H = var152;
                int var153 = IX() + 6;
                int var154 = mem(var153, 37533);
                A = var154;
                int var155 = HL();
                wMem(var155, A, 37536);
              }
            } else {
              BC(640);
              int var158 = mem(32990, 37458);
              A = var158;

              do {
                int var159 = A ^ 24;
                A = var159;

                do {
                  int var160 = B - 1 & 255;
                  B = var160;
                } while (B != 0);

                B = C;
                int var161 = C - 1 & 255;
                C = var161;
              } while (C != 0);
            }
          }
        } else {
          IY(33280);
          int var6 = IX() + 9;
          wMem(var6, 0, 37544);
          int var7 = IX() + 2;
          int var8 = mem(var7, 37548);
          A = var8;
          int var9 = IX() + 3;
          wMem(var9, A, 37551);
          int var10 = IX() + 5;
          wMem(var10, 128, 37554);

          while (true) {
            label114:
            {
              A = mem(IY(), 37558);
              A = A + mem(IX() + 3, 37561) & 255;
              L = A;
              H = mem(IY() + 1, 37565);
              A = mem(34262, 37568);
              if (A << 1 == 0) {
                A = mem(IX() + 5, 37574);
                A = A & mem(HL(), 37577);
                if (A << 1 == 0) {
                  break label114;
                }

                A = mem(IX() + 9, 37580);
                wMem(34262, A, 37583);
                wMem(IX() + 11, mem(IX() + 11, 37586) | 1, 37586);
              }

              if (A == mem(IX() + 9, 37590)) {
                if ((mem(IX() + 11, 37595) & 1) != 0) {
                  B = mem(IX() + 3, 37601);
                  A = mem(IX() + 5, 37604);
                  C = 1;
                  if (A >= 4) {
                    C = 0;
                    if (A >= 16) {
                      B = B - 1 & 255;
                      C = 3;
                      if (A >= 64) {
                        C = 2;
                      }
                    }
                  }

                  wMem16(34258, BC(), 37628);
                  A = IYL;
                  A = A - 16 & 255;
                  wMem(34255, A, 37636);
                  push(HL());
                  $36508();
                  HL(pop());
                }
              }
            }

            int var21 = IX() + 5;
            int var22 = mem(var21, 37646);
            A = var22;
            int var23 = HL();
            int var24 = mem(var23, 37649);
            int var25 = A | var24;
            A = var25;
            int var26 = HL();
            wMem(var26, A, 37650);
            int var27 = IX() + 9;
            int var28 = mem(var27, 37651);
            A = var28;
            int var29 = IX() + 1;
            int var30 = mem(var29, 37654);
            int var31 = A + var30 & 255;
            A = var31;
            L = A;
            int var32 = L | 128;
            L = var32;
            H = 131;
            int var33 = HL();
            int var34 = mem(var33, 37662);
            E = var34;
            D = 0;
            int var35 = DE();
            int var36 = IY() + var35 & 65535;
            IY(var36);
            int var37 = L & -129;
            L = var37;
            int var38 = HL();
            int var39 = mem(var38, 37669);
            A = var39;
            if (A << 1 != 0) {
              B = A;
              int var79 = IX() + 1;
              if ((mem(var79, 37674) & 128) != 0) {
                do {
                  int var87 = IX() + 5;
                  int var88 = mem(var87, 37680);
                  int var89 = rlc(var88);
                  wMem(var87, var89, 37680);
                  int var90 = IX() + 5;
                  if ((mem(var90, 37684) & 1) != 0) {
                    int var92 = IX() + 3;
                    int var93 = mem(var92, 37690) - 1 & 255;
                    wMem(var92, var93, 37690);
                  }

                  int var91 = B - 1 & 255;
                  B = var91;
                } while (B != 0);
              } else {
                do {
                  int var80 = IX() + 5;
                  int var81 = mem(var80, 37697);
                  int var82 = rrc(var81);
                  wMem(var80, var82, 37697);
                  int var83 = IX() + 5;
                  if ((mem(var83, 37701) & 128) != 0) {
                    int var85 = IX() + 3;
                    int var86 = mem(var85, 37707) + 1 & 255;
                    wMem(var85, var86, 37707);
                  }

                  int var84 = B - 1 & 255;
                  B = var84;
                } while (B != 0);
              }
            }

            A = mem(IX() + 9, 37712);
            if (A == mem(IX() + 4, 37715)) {
              A = mem(34262, 37726);
              if ((A & 128) != 0) {
                A = A + 1 & 255;
                wMem(34262, A, 37734);
                int var75 = IX() + 11;
                int var76 = mem(var75, 37737) & -2;
                wMem(var75, var76, 37737);
              } else {
                int var45 = IX() + 11;
                if ((mem(var45, 37743) & 1) != 0) {
                  Movement movement = (Movement) objectMemory[34256];
                  A = movement.value;
                  A = mem(34256, 37749);
                  movement.moveRope();
                }
              }
              break;
            }

            int var77 = IX() + 9;
            int var78 = mem(var77, 37720) + 1 & 255;
            wMem(var77, var78, 37720);
          }
        }
      }

      DE(8);
      int var4 = DE();
      int var5 = IX() + var4 & 65535;
      IX(var5);
    }
  }

  public void $37841() {
    H = 164;
    int var1 = mem(41983, 37843);
    A = var1;
    L = A;

    do {
      int var2 = HL();
      int var3 = mem(var2, 37847);
      C = var3;
      int var4 = C & -129;
      C = var4;
      int var5 = mem(33824, 37850);
      A = var5;
      int var6 = A | 64;
      A = var6;
      if (A == C) {
        int var8 = HL();
        int var9 = mem(var8, 37858);
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
        int var16 = mem(var15, 37866);
        E = var16;
        int var17 = H - 1 & 255;
        H = var17;
        int var18 = DE();
        int var19 = mem(var18, 37868);
        A = var19;
        int var20 = A & 7;
        A = var20;
        if (A != 7) {
          int var21 = mem(34251, 37936);
          A = var21;
          int var22 = A + L & 255;
          A = var22;
          int var23 = A & 3;
          A = var23;
          int var24 = A + 3 & 255;
          A = var24;
          C = A;
          int var25 = DE();
          int var26 = mem(var25, 37945);
          A = var26;
          int var27 = A & 248;
          A = var27;
          int var28 = A | C;
          A = var28;
          int var29 = DE();
          wMem(var29, A, 37949);
          int var30 = HL();
          int var31 = mem(var30, 37950);
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

          while (true) {
            int var44 = IX() + 2;
            int var45 = mem(var44, 37879) + 1 & 255;
            wMem(var44, var45, 37879);
            int var46 = IX() + 2;
            int var47 = mem(var46, 37882);
            A = var47;
            if (A != 58) {
              int var48 = mem(32990, 37897);
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
                } while (B != 0);

                int var52 = C - 1 & 255;
                C = var52;
                int var53 = C - 1 & 255;
                C = var53;
              } while (C != 0);

              A = mem(34270, 37918);
              A = A + 1 & 255;
              F = A;
              wMem(34270, A, 37922);
              if (F == 0) {
                A = 1;
                wMem(34271, A, 37929);
              }

              int var56 = HL();
              int var57 = mem(var56, 37932) & -65;
              int var58 = HL();
              wMem(var58, var57, 37932);
              break;
            }

            int var59 = IX() + 2;
            wMem(var59, 48, 37889);
            int var60 = IX() - 1 & 65535;
            IX(var60);
          }
        }
      }

      int var7 = L + 1 & 255;
      L = var7;
    } while (L != 0);

  }

  public void $37974() {
    B = 16;

    do {
      int var1 = C & 1;
      F = var1;
      int var2 = DE();
      int var3 = mem(var2, 37978);
      A = var3;
      if (F != 0) {
        int var29 = HL();
        int var30 = mem(var29, 37981);
        int var31 = A & var30;
        A = var31;
        if (A << 1 != 0) {
          return;
        }

        int var32 = DE();
        int var33 = mem(var32, 37983);
        A = var33;
        int var34 = HL();
        int var35 = mem(var34, 37984);
        int var36 = A | var35;
        A = var36;
      }

      int var4 = HL();
      wMem(var4, A, 37985);
      int var5 = L + 1 & 255;
      L = var5;
      int var6 = DE() + 1 & 65535;
      DE(var6);
      int var7 = C & 1;
      F = var7;
      int var8 = DE();
      int var9 = mem(var8, 37990);
      A = var9;
      if (F != 0) {
        int var21 = HL();
        int var22 = mem(var21, 37993);
        int var23 = A & var22;
        A = var23;
        if (A << 1 != 0) {
          return;
        }

        int var24 = DE();
        int var25 = mem(var24, 37995);
        A = var25;
        int var26 = HL();
        int var27 = mem(var26, 37996);
        int var28 = A | var27;
        A = var28;
      }

      int var10 = HL();
      wMem(var10, A, 37997);
      int var11 = L - 1 & 255;
      L = var11;
      int var12 = H + 1 & 255;
      H = var12;
      int var13 = DE() + 1 & 65535;
      DE(var13);
      A = H;
      int var14 = A & 7;
      A = var14;
      if (A << 1 == 0) {
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
        if (A << 1 == 0) {
          A = H;
          int var20 = A + 8 & 255;
          A = var20;
          H = A;
        }
      }

      int var15 = B - 1 & 255;
      B = var15;
    } while (B != 0);

    A = 0;
    int var16 = A << 1;
    F = var16;
  }

  public void $38064() {
    int var1 = mem(33003, 38064);
    A = var1;
    wMem(33824, A, 38067);
    int var2 = mem(34259, 38070);
    A = var2;
    int var3 = A & 31;
    A = var3;
    int var4 = A + 160 & 255;
    A = var4;
    wMem(34259, A, 38077);
    A = 93;
    wMem(34260, A, 38082);
    A = 208;
    wMem(34255, A, 38087);
    A = 0;
    wMem(34257, A, 38091);
    nextAddress = 38095;
  }

  public void $38137() {
    HL(mem16(32983, 38137));
    A = H;
    A = A & 1;
    A = rlc(A);
    A = rlc(A);
    A = rlc(A);
    A = A + 112 & 255;
    H = A;
    E = L;
    D = H;
    A = mem(32985, 38151);
    ((Conveyor) objectMemory[32985]).moveConveyor();
  }

  public void $38196() {
    A = mem(33824, 38196);
    if (A == 35) {
      A = mem(34271, 38203);
      if (A << 1 == 0) {
        int var18 = mem(34251, 38209);
        A = var18;
        int var19 = A & 2;
        A = var19;
        int var20 = A;
        int var21 = rrc(var20);
        A = var21;
        int var22 = A;
        int var23 = rrc(var22);
        A = var23;
        int var24 = A;
        int var25 = rrc(var24);
        A = var25;
        int var26 = A;
        int var27 = rrc(var26);
        A = var27;
        int var28 = A | 128;
        A = var28;
        E = A;
        int var29 = mem(34255, 38221);
        A = var29;
        if (A != 208) {
          E = 192;
          if (A < 192) {
            E = 224;
          }
        }

        D = 156;
        HL(26734);
        C = 1;
        $37974();
        if (F != 0) {
          nextAddress = 37048;
        } else {
          HL(17733);
          int var30 = HL();
          wMem16(23918, var30, 38252);
          HL(1799);
          int var31 = HL();
          wMem16(23950, var31, 38258);
        }
      } else {
        int var16 = mem(34259, 38262);
        A = var16;
        int var17 = A & 31;
        A = var17;
        if (A < 6) {
          A = 2;
          wMem(34271, A, 38272);
        }
      }
    } else {
      int var2 = mem(33824, 38298);
      A = var2;
      if (A == 33) {
        int var3 = mem(34251, 38304);
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
        int var11 = mem(34271, 38313);
        A = var11;
        if (A == 3) {
          int var14 = E | 64;
          E = var14;
        }

        D = 166;
        IX(33488);
        BC(4124);
        $38504();
        HL(1799);
        int var12 = HL();
        wMem16(23996, var12, 38337);
        int var13 = HL();
        wMem16(24028, var13, 38340);
      }
    }
  }

  public void $38276() {
    int var1 = mem(33824, 38276);
    A = var1;
    if (A == 33) {
      int var2 = mem(34259, 38282);
      A = var2;
      if (A == 188) {
        A = 0;
        int var3 = A << 1;
        F = var3;
        wMem(34251, A, 38289);
        A = 3;
        wMem(34271, A, 38294);
      }
    }
  }

  public void $38344() {
    HL(mem16(34259, 38344));
    B = 0;
    A = mem(32986, 38349);
    A = A & 1;
    A = A + 64 & 255;
    E = A;
    D = 0;
    HL(HL() + DE() & 65535);
    A = mem(32964, 38360);
    if (A == mem(HL(), 38363)) {
      A = mem(34257, 38366);
      if (A << 1 == 0) {
        A = mem(34258, 38372);
        A = A & 3;
        A = rlc(A);
        A = rlc(A);
        B = A;
        A = mem(32986, 38380);
        A = A & 1;
        A = A - 1 & 255;
        A = A ^ 12;
        A = A ^ B;
        A = A & 12;
        B = A;
      }
    }

    int var10 = mem16(34259, 38392);
    HL(var10);
    DE(31);
    C = 15;
    $38430();
    if (isNextPC(37047)) {
      nextAddress = 37048;
    } else {
      int var11 = HL() + 1 & 65535;
      HL(var11);
      $38430();
      if (isNextPC(37047)) {
        nextAddress = 37048;
      } else {
        int var12 = DE();
        int var13 = HL() + var12 & 65535;
        HL(var13);
        $38430();
        int var14 = HL() + 1 & 65535;
        HL(var14);
        $38430();
        if (isNextPC(37047)) {
          nextAddress = 37048;
        } else {
          int var15 = mem(34255, 38415);
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
          if (isNextPC(37047)) {
            nextAddress = 37048;
          } else {
            A = mem(34255, 38455);
            A = A + B & 255;
            IXH = 130;
            IXL = A;
            A = mem(34256, 38464);
            A = A & 1;
            A = rrc(A);
            E = A;
            A = mem(34258, 38471);
            A = A & 3;
            A = rrc(A);
            A = rrc(A);
            A = rrc(A);
            A = A | E;
            E = A;
            D = 157;
            A = mem(33824, 38483);
            if (A == 29) {
              D = 182;
              A = E;
              A = A ^ 128;
              E = A;
            }

            B = 16;
            A = mem(34259, 38498);
            A = A & 31;
            C = A;
            $38504();
          }
        }
      }
    }
  }

  public void $38430() {
    int var1 = mem(32928, 38430);
    A = var1;
    int var2 = HL();
    int var3 = mem(var2, 38433);
    if (A == var3) {
      A = C;
      int var8 = A & 15;
      A = var8;
      if (A << 1 != 0) {
        int var9 = mem(32928, 38441);
        A = var9;
        int var10 = A | 7;
        A = var10;
        int var11 = HL();
        wMem(var11, A, 38446);
      }
    }

    int var4 = mem(32955, 38447);
    A = var4;
    int var5 = HL();
    int var6 = mem(var5, 38450);
    if (A == var6) {
      nextAddress = 37047;
    } else {
      int var7 = A - var6;
      F = var7;
    }
  }

  public void $38504() {
    do {
      int var1 = IX();
      int var2 = mem(var1, 38504);
      A = var2;
      int var3 = IX() + 1;
      int var4 = mem(var3, 38507);
      H = var4;
      int var5 = A | C;
      A = var5;
      L = A;
      int var6 = DE();
      int var7 = mem(var6, 38512);
      A = var7;
      int var8 = HL();
      int var9 = mem(var8, 38513);
      int var10 = A | var9;
      A = var10;
      int var11 = HL();
      wMem(var11, A, 38514);
      int var12 = HL() + 1 & 65535;
      HL(var12);
      int var13 = DE() + 1 & 65535;
      DE(var13);
      int var14 = DE();
      int var15 = mem(var14, 38517);
      A = var15;
      int var16 = HL();
      int var17 = mem(var16, 38518);
      int var18 = A | var17;
      A = var18;
      int var19 = A << 1;
      F = var19;
      int var20 = HL();
      wMem(var20, A, 38519);
      int var21 = IX() + 1 & 65535;
      IX(var21);
      int var22 = IX() + 1 & 65535;
      IX(var22);
      int var23 = DE() + 1 & 65535;
      DE(var23);
      int var24 = B - 1 & 255;
      B = var24;
    } while (B != 0);

  }

  public void $38528() {
    do {
      int var1 = IX();
      int var2 = mem(var1, 38528);
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
    } while (C != 0);

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

  public void $38555() {
    do {
      int var1 = HL();
      int var2 = mem(var1, 38555);
      A = var2;
      int var3 = DE();
      wMem(var3, A, 38556);
      int var4 = HL() + 1 & 65535;
      HL(var4);
      int var5 = D + 1 & 255;
      D = var5;
      int var6 = B - 1 & 255;
      B = var6;
    } while (B != 0);

  }

  public void $38562() {
    while (true) {
      int var1 = HL();
      int var2 = mem(var1, 38562);
      A = var2;
      if (A == 255) {
        return;
      }

      BC(100);
      A = 0;
      int var3 = HL();
      int var4 = mem(var3, 38570);
      E = var4;
      D = E;

      while (true) {
        int var5 = D - 1 & 255;
        D = var5;
        if (D == 0) {
          D = E;
          int var12 = A ^ 24;
          A = var12;
        }

        int var6 = B - 1 & 255;
        B = var6;
        if (B == 0) {
          A = C;
          if (A == 50) {
            int var9 = A - 50;
            F = var9;
            int var10 = E;
            int var11 = rl(var10);
            E = var11;
          }

          int var7 = C - 1 & 255;
          C = var7;
          if (C == 0) {
            $38601();
            if (F != 0) {
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

  public void $38601() {
    int var1 = mem(34254, 38601);
    A = var1;
    if (A << 1 != 0) {
      int var6 = in(31);
      A = var6;
      if ((A & 16) != 0) {
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
        if (A == B) {
          D = 24;
        }

        int var3 = B - 1 & 255;
        B = var3;
      } while (B != 0);

      int var4 = A - 1 & 255;
      A = var4;
    } while (A != 0);

  }

  @Override
  protected void replaceWithObject(int address, int value) {
    if (address == 32982) objectMemory[address] = value << 1 == 0 ? new LeftMover(value) : new RightMover(value);
    if (address == 32985) objectMemory[address] = value << 1 == 0 ? new Conveyor(value) : new ActiveConveyor(value);
    if (address == 34256) {
      objectMemory[address] = (value & 1) << 1 == 0 ? new RightMovement(value) : new LeftMovement(value);
    }
  }

  private class LeftMover extends ConveyorMover {
    public LeftMover(int value) {
      super(value);
    }

    public void rotate() {
      A = mem(HL(), 38163);
      A = rlc(A);
      A = rlc(A);
      H = H + 1 & 255;
      H = H + 1 & 255;
      C = mem(HL(), 38170);
      C = rrc(C);
      C = rrc(C);
    }
  }

  private class RightMover extends ConveyorMover {
    public RightMover(int value) {
      super(value);
    }

    public void rotate() {
      A = mem(HL(), 38182);
      A = rrc(A);
      A = rrc(A);
      H = H + 1 & 255;
      H = H + 1 & 255;
      C = mem(HL(), 38189);
      C = rlc(C);
      C = rlc(C);
    }
  }

  private class ActiveConveyor extends Conveyor {
    public ActiveConveyor(int value) {
      super(value);
    }

    public void fillConveyorAttributes() {
      HL(mem16(32983, 36246));
      B = A;
      A = mem(32973, 36250);

      do {
        wMem(HL(), A, 36253);
        HL(HL() + 1 & 65535);
        B = B - 1 & 255;
      } while (B != 0);
    }

    public void moveConveyor() {
      B = A;
//      A = mem(32982, 38157);
      ConveyorMover conveyorMover = (ConveyorMover) objectMemory[32982];
      A = conveyorMover.value;
      conveyorMover.rotate();

      do {
        wMem(DE(), A, 38175);
        wMem(HL(), C, 38176);
        L = L + 1 & 255;
        E = E + 1 & 255;
        B = B - 1 & 255;
      } while (B != 0);
    }
  }

  private class Movement extends ZxObject {
    public Movement(int value) {
      super(value);
    }

    public boolean move() {
      return false;
    }

    public void moveRope() {
      if ((A & 2) != 0) {
        A = rrc(A);
        A = A ^ mem(IX(), 37757);
        A = rlc(A);
        A = rlc(A);
        A = A & 2;
        A = A - 1 & 255;
        HL(34262);
        A = A + mem(HL(), 37768) & 255;
        wMem(HL(), A, 37769);
        A = mem(33003, 37770);
        C = A;
        A = mem(33824, 37774);
        if (A == C) {
          A = mem(HL(), 37780);
          if (A < 12) {
            wMem(HL(), 12, 37785);
          }
        }

        A = mem(HL(), 37787);
        int var67 = mem(IX() + 4, 37788);
        if (A >= var67 && A != var67) {
          wMem(HL(), 240, 37795);
          A = mem(34255, 37797);
          A = A & 248;
          wMem(34255, A, 37802);
          A = 0;
          wMem(34257, A, 37806);
        }
      }
    }
  }

  private class LeftMovement extends Movement {
    public LeftMovement(int value) {
      super(value);
    }

    public boolean move() {
      {
        A = mem(34258, 36817);
        if (A << 1 != 0) {
          A = A - 1 & 255;
          F = A;
          wMem(34258, A, 36824);
          return true;
        }

        A = mem(34257, 36828);
        BC(0);
        if (A == 0) {
          HL(mem16(34259, 36838));
          BC(0);
          A = mem(32986, 36844);
          A = A - 1 & 255;
          A = A | 161;
          A = A ^ 224;
          E = A;
          D = 0;
          HL(HL() + DE() & 65535);
          A = mem(32964, 36856);
          if (A == mem(HL(), 36859)) {
            BC(32);
            A = mem(32986, 36865);
            if (A << 1 == 0) {
              BC(65504);
            }
          }
        }

        int var108 = mem16(34259, 36874);
        HL(var108);
        A = L;
        int var109 = A & 31;
        A = var109;
        if (A << 1 != 0) {
          int var114 = BC();
          int var115 = HL() + var114 & 65535;
          HL(var115);
          int var116 = HL() - 1 & 65535;
          HL(var116);
          DE(32);
          int var117 = DE();
          int var118 = HL() + var117 & 65535;
          HL(var118);
          int var119 = mem(32946, 36889);
          A = var119;
          int var120 = HL();
          int var121 = mem(var120, 36892);
          if (A == var121) {
            return true;
          }

          int var122 = mem(34255, 36894);
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
          if (A << 1 != 0) {
            int var131 = mem(32946, 36905);
            A = var131;
            int var132 = DE();
            int var133 = HL() + var132 & 65535;
            HL(var133);
            int var134 = HL();
            int var135 = mem(var134, 36909);
            if (A == var135) {
              return true;
            }

            int var136 = DE();
            int var137 = HL() - var136 & 65535;
            HL(var137);
          }

          int var128 = DE();
          int var129 = HL() - var128 & 65535;
          HL(var129);
          int var130 = HL();
          wMem16(34259, var130, 36917);
          A = B;
          wMem(34255, A, 36921);
          A = 3;
          wMem(34258, A, 36926);
          return true;
        }

        int var110 = mem(33001, 38026);
        A = var110;
        wMem(33824, A, 38029);
        int var111 = mem(34259, 38032);
        A = var111;
        int var112 = A | 31;
        A = var112;
        int var113 = A & 254;
        A = var113;
        wMem(34259, A, 38039);
        nextAddress = 38043;
        return true;
      }
    }
  }

  private class RightMovement extends Movement {
    public RightMovement(int value) {
      super(value);
    }

    @Override
    public boolean move() {
      A = mem(34258, 36930);
      if (A != 3) {
        A = A + 1 & 255;
        F = A;
        wMem(34258, A, 36938);
        return true;
      }

      A = mem(34257, 36942);
      BC(0);
      if (A << 1 == 0) {
        HL(mem16(34259, 36951));
        A = mem(32986, 36954);
        A = A - 1 & 255;
        A = A | 157;
        A = A ^ 191;
        E = A;
        D = 0;
        HL(HL() + DE() & 65535);
        A = mem(32964, 36966);
        if (A == mem(HL(), 36969)) {
          BC(32);
          A = mem(32986, 36975);
          if (A << 1 != 0) {
            BC(65504);
          }
        }
      }

      HL(mem16(34259, 36984));
      HL(HL() + BC() & 65535);
      HL(HL() + 1 & 65535);
      HL(HL() + 1 & 65535);
      A = L;
      A = A & 31;
      if (A << 1 != 0) {
        DE(32);
        int var68 = mem(32946, 36999);
        A = var68;
        int var69 = DE();
        int var70 = HL() + var69 & 65535;
        HL(var70);
        int var71 = HL();
        int var72 = mem(var71, 37003);
        if (A == var72) {
          return true;
        }

        int var73 = mem(34255, 37005);
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
        if (A << 1 != 0) {
          int var87 = mem(32946, 37016);
          A = var87;
          int var88 = DE();
          int var89 = HL() + var88 & 65535;
          HL(var89);
          int var90 = HL();
          int var91 = mem(var90, 37020);
          if (A == var91) {
            return true;
          }

          int var92 = DE();
          int var93 = HL() - var92 & 65535;
          HL(var93);
        }

        int var79 = mem(32946, 37025);
        A = var79;
        int var80 = DE();
        int var81 = HL() - var80 & 65535;
        HL(var81);
        int var82 = HL();
        int var83 = mem(var82, 37031);
        if (A == var83) {
          return true;
        }

        int var84 = HL() - 1 & 65535;
        HL(var84);
        int var85 = HL();
        wMem16(34259, var85, 37034);
        A = 0;
        int var86 = A << 1;
        F = var86;
        wMem(34258, A, 37038);
        A = B;
        wMem(34255, A, 37042);
        return true;
      }
      return false;
    }
  }
}
