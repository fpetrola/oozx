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

import com.fpetrola.z80.minizx.DefaultMiniZXIO;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.SpectrumApplication;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class GameInvokerScrolling {


  private static GameTile mainGameTile = createGame(new TileSpec(33, 9, 3));
  private static List<GameTile> gameList;
  private static boolean roomChanged;

  public static void main(String[] args) throws InstantiationException {

    List<TileSpec> tileSpecs = new ArrayList<>();

//    int room = 25;
//    int i = 5;
//    for (int x = 0; x < i; x++) {
//      for (int y = 0; y < i; y++) {
//        tileSpecs.add(new TileSpec(room++, x, y));
//      }
//    }

    int[][] rows = {
        {47, 47, 47, 47, 47, 47, 47, 47, 47, 50, 47, 47, 47, 47, 47, 47, 47, 47},
        {47, 47, 47, 47, 47, 47, 48, 18, 17, 16, 15, 14, 44, 47, 47, 47, 47, 47},
        {47, 47, 47, 47, 47, 43, 42, 41, 40, 39, 38, 47, 47, 47, 47, 47, 47, 47},

        {47, 47, 47, 57, 56, 37, 36, 35, 34, 33, 32, 47, 47, 47, 47, 47, 47, 47},
        {47, 47, 47, 55, 54, 31, 30, 29, 28, 27, 26, 47, 47, 13, 12, 47, 47, 47},
        {47, 47, 47, 53, 52, 25, 24, 23, 22, 21, 20, 11, 10,  9,  8,  7, 47, 47},
        {60, 59, 58, 51, 49, 47, 47, 47, 47, 47, 47, 19,  5, 4,   3,  2,  1,  0},
        {47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47,  6, 45, 46, 47, 47, 47},
    };

    for (int j = 0; j < rows.length; j++) {
      for (int k = 0; k < rows[j].length; k++) {
        int room = rows[j][k];
//        room= 57;
        tileSpecs.add(new TileSpec(room, k, j));
      }
    }


    gameList = tileSpecs.stream().map(ts -> createGame(ts)).toList();

    ScrollingScreenComponent scrollingScreenComponent = new ScrollingScreenComponent(gameList, mainGameTile);
    MiniZX.createScreen(((DefaultMiniZXIO) SpectrumApplication.io).miniZXKeyboard, scrollingScreenComponent);


    System.setProperty("jdk.virtualThreadScheduler.parallelism", "50");
    System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", "2000");


    Thread.startVirtualThread(() -> mainGameTile.zxGame.$35090());
    gameList.forEach(g -> Thread.startVirtualThread(() -> g.zxGame.$35090()));
  }

  private static GameTile createGame(TileSpec tileSpec) {
    ZxGame1 zxGame1 = new ZxGame1() {

      @Override
      public void $35090() {
        if (getMem()[33824] == 47) {
          for (int i = 16384; i < 23296; i++) {
            wMem(i, 0, -1);
          }
          return;
        }

        if (!isMain()) {
          for (int i = 40192; i < 40448; i++) {
            getMem()[i] = 0;
          }
          getMem()[33824] = tileSpec.room;
        }
//        getMem()[34255] = 200;
//        getMem()[34259] = 128;

        super.$35090();
      }
//
//      @Override
//      public void $35563() {
//      }
//
//      public void $37974() {
//        super.$37974();
//        getMem()[34251] = 999999999;
//        getMem()[34272] = 1000000;
//      }


//      @Override
//      public void $36288() {
//        A = A & 3;
//        F = A << 1;
//        C = A;
//        A = rlc(A);
//        A = rlc(A);
//        A = rlc(A);
//        A = A + C & 255;
//        int var11 = A + 160;
//        A = var11 & 255;
//        F = var11;
//        E = A;
//        D = 128;
//        A = mem(DE(), 36300);
//        wMem(IX(), A  & 0xF7, 36301);
//        IX(IX() + 1 & 65535);
//      }

      public void $37310() {
        super.$37310();
        try {
          Thread.sleep(Duration.ofMillis(40L));
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public int mem(int address, int pc) {
        int value = super.mem(address, pc);
        if (roomChanged && isMain() && address == 34271 && pc == 35328) {
          roomChanged = false;
          for (int i = 0; i < gameList.size(); i++) {
            TileSpec tileSpec1 = gameList.get(i).tileSpec;
            if (tileSpec1.room == getMem()[33824]) {
              mainGameTile.x = tileSpec1.x;
              mainGameTile.y = tileSpec1.y;
            }
          }
        }
        return value;
      }

      public void wMem(int address, int value, int pc) {
        value = changeEnterRoom(value, pc);

        if (isMain() && pc == 35205 && roomChanged) {
          for (int i = 0; i < gameList.size(); i++) {
            TileSpec tileSpec1 = gameList.get(i).tileSpec;
            if (tileSpec1.room == getMem()[33824]) {
              for (int i2 = 33024; i2 < 33089; i2++) {
                getMem()[i2] = gameList.get(i).zxGame.getMem()[i2];
              }
            }
          }

        }
        if (!isMain() && address == 34257) {
          value = 0;
        }
        if (address == 33824) {
          if (!isMain())
            value = tileSpec.room;
          else
            roomChanged = true;
        }
        wMem(address, value);
      }

      private int changeEnterRoom(int value, int pc) {
        if (isMain() && pc == 38039) {
          value += 1;
        }

//        if (isMain() && pc == 38057){
//          value -= 1;
//        }
        return value;
      }

      private boolean isMain() {
        return this == mainGameTile.zxGame;
      }

      public int in(int port) {
        if (isMain())
          return super.in(port);
        else
          return 1;
      }

      public int in(int port, int pc) {
        if (isMain())
          return super.in(port, pc);
        else
          return 1;
      }
    };

    zxGame1.getMem()[33824] = tileSpec.room;
    zxGame1.getMem()[34257] = 0;

    return new GameTile<>(zxGame1, zxGame1.getZxScreenComponent(), tileSpec);
  }
}
