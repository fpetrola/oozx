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

// Decompiled with: FernFlower
// Class Version: 17
public class JSW3 {
  public int initial;
  public int[] memory;

  public void $90C0() {
    int IX_L37056 = 33024;

    while (true) {
      int var2 = this.memory[IX_L37056];
      int F_L37063 = 65535;
      F_L37063 = var2 - 255;
      if (F_L37063 == 0) {
        return;
      }

      int A_L37063 = var2 & 3;
      if (A_L37063 != 0) {
        int F_L37065 = A_L37063 - 1;
        if (F_L37065 != 0) {
          F_L37065 = A_L37063 - 2;
          if (F_L37065 != 0) {
            int var35 = this.memory[IX_L37056];
            int var36 = var35 & 128;
            this.memory[IX_L37056] = var36;
            if (var35 != 0) {
              int var42 = IX_L37056 + 1;
              A_L37063 = this.memory[var42];
              F_L37065 = A_L37063 & 128;
              if (F_L37065 != 0) {
                A_L37063 -= 2;
                F_L37065 = A_L37063 - 148;
                if (F_L37065 < 0) {
                  A_L37063 -= 2;
                  F_L37065 = A_L37063 - 128;
                  if (F_L37065 == 0) {
                    A_L37063 ^= A_L37063;
                  }
                }
              } else {
                A_L37063 += 2;
                F_L37065 = A_L37063 - 18;
                if (F_L37065 < 0) {
                  A_L37063 += 2;
                }
              }
            } else {
              int var37 = IX_L37056 + 1;
              A_L37063 = this.memory[var37];
              F_L37065 = A_L37063 & 128;
              if (F_L37065 == 0) {
                A_L37063 -= 2;
                F_L37065 = A_L37063 - 20;
                if (F_L37065 < 0) {
                  A_L37063 -= 2;
                  A_L37063 |= A_L37063;
                  if (A_L37063 == 0) {
                  }
                }
              } else {
                A_L37063 += 2;
                F_L37065 = A_L37063 - 146;
                if (F_L37065 < 0) {
                  A_L37063 += 2;
                }
              }
            }

            int var38 = IX_L37056 + 1;
            this.memory[var38] = A_L37063;
            A_L37063 &= 127;
            int var39 = IX_L37056 + 7;
            int var40 = this.memory[var39];
            F_L37065 = A_L37063 - var40;
            int var41 = IX_L37056 + 7;
            int var94 = this.memory[var41];
            if (F_L37065 == 0) {
              A_L37063 = this.memory[IX_L37056];
              A_L37063 ^= 128;
              this.memory[IX_L37056] = A_L37063;
            }
          } else {
            label81:
            {
              A_L37063 = this.memory[IX_L37056];
              A_L37063 ^= 8;
              this.memory[IX_L37056] = A_L37063;
              A_L37063 &= 24;
              if (A_L37063 != 0) {
                A_L37063 = this.memory[IX_L37056];
                A_L37063 += 32;
                this.memory[IX_L37056] = A_L37063;
              }

              int var18 = IX_L37056 + 3;
              A_L37063 = this.memory[var18];
              int var19 = IX_L37056 + 4;
              int var20 = this.memory[var19];
              A_L37063 += var20;
              int var21 = IX_L37056 + 4;
              int var89 = this.memory[var21];
              int var22 = IX_L37056 + 4;
              var89 = this.memory[var22];
              int var23 = IX_L37056 + 4;
              var89 = this.memory[var23];
              int var24 = IX_L37056 + 3;
              this.memory[var24] = A_L37063;
              int var25 = IX_L37056 + 7;
              int var26 = this.memory[var25];
              F_L37065 = A_L37063 - var26;
              int var27 = IX_L37056 + 7;
              var89 = this.memory[var27];
              if (F_L37065 < 0) {
                int var30 = IX_L37056 + 6;
                int var31 = this.memory[var30];
                F_L37065 = A_L37063 - var31;
                int var32 = IX_L37056 + 6;
                var89 = this.memory[var32];
                if (F_L37065 != 0 && F_L37065 >= 0) {
                  break label81;
                }

                int var33 = IX_L37056 + 6;
                A_L37063 = this.memory[var33];
                int var34 = IX_L37056 + 3;
                this.memory[var34] = A_L37063;
              }

              int var28 = IX_L37056 + 4;
              A_L37063 = this.memory[var28];
              int var29 = IX_L37056 + 4;
              this.memory[var29] = A_L37063;
            }
          }
        } else {
          int var8 = this.memory[IX_L37056];
          int var9 = var8 & 128;
          this.memory[IX_L37056] = var9;
          if (var8 == 0) {
            A_L37063 = this.memory[IX_L37056];
            A_L37063 -= 32;
            A_L37063 &= 127;
            this.memory[IX_L37056] = A_L37063;
            F_L37065 = A_L37063 - 96;
            if (F_L37065 >= 0) {
              int var14 = IX_L37056 + 2;
              A_L37063 = this.memory[var14];
              A_L37063 &= 31;
              int var15 = IX_L37056 + 6;
              int var16 = this.memory[var15];
              F_L37065 = A_L37063 - var16;
              int var17 = IX_L37056 + 6;
              int var10000 = this.memory[var17];
              if (F_L37065 != 0) {
                var10000 = IX_L37056 + 2;
                var10000 = IX_L37056 + 2;
              } else {
                this.memory[IX_L37056] = 129;
              }
            }
          } else {
            A_L37063 = this.memory[IX_L37056];
            A_L37063 += 32;
            A_L37063 |= 128;
            this.memory[IX_L37056] = A_L37063;
            F_L37065 = A_L37063 - 160;
            if (F_L37065 < 0) {
              int var10 = IX_L37056 + 2;
              A_L37063 = this.memory[var10];
              A_L37063 &= 31;
              int var11 = IX_L37056 + 7;
              int var12 = this.memory[var11];
              F_L37065 = A_L37063 - var12;
              int var13 = IX_L37056 + 7;
              int var87 = this.memory[var13];
              if (F_L37065 != 0) {
                var87 = IX_L37056 + 2;
              } else {
                this.memory[IX_L37056] = 97;
              }
            }
          }
        }
      }

      int DE_L37302 = 8;
      IX_L37056 += DE_L37302;
    }
  }
}
