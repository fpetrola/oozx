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

package com.fpetrola.z80.bytecode;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.util.ArrayList;
import java.util.List;

public class JSW {

  public void $90C0() {
    List<Guardian> guardians = new ArrayList<>();

    guardians.forEach(guardian -> {
      if ((guardian.direction & 3) != 0) {
        label46:
        {
          if (guardian.direction - 1 != 0) {
            if (guardian.direction - 2 == 0) {
              guardian.direction ^= 8;
              if ((guardian.direction & 24) != 0) {
                guardian.direction += 32;
              }

              guardian.y += guardian.yAcceleration;
              if (0 == guardian.y - guardian.maxCoord) {
                if (guardian.y - guardian.minCoord != 0) {
                  break label46;
                }
                guardian.y = guardian.minCoord;
              }

              guardian.yAcceleration = 0;
              break label46;
            }
          }

          if (guardian.direction == 0) {
            guardian.direction = (guardian.direction - 32) & 127;
            if (0 != guardian.direction - 96) {
              if ((guardian.x & 31) - guardian.minCoord == 0) {
                guardian.direction = 129;
              }
            }
          } else {
            guardian.direction = (guardian.direction + 32) | 128;
            if (0 == guardian.direction - 160) {
              if ((guardian.x & 31) - guardian.maxCoord == 0) {
                guardian.direction = 97;
              }
            }
          }
        }
      }
    });
  }
}
