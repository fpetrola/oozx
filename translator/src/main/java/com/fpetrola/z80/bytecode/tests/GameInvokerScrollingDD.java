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
import com.fpetrola.z80.opcodes.references.WordNumber;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GameInvokerScrollingDD {
  private static GameTile<WordNumber, MiniZX> mainGameTile = createGame(new TileSpec(33, 9, 3));
  private static List<GameTile<WordNumber, MiniZX>> gameList;
  private static boolean roomChanged;

  public static void main(String[] args) throws InstantiationException {

    List<TileSpec> tileSpecs = new ArrayList<>();

    int[][] rows = {
        {47, 47, 47, 47, 47, 47, 47, 47, 47, 50, 47, 47, 47, 47, 47, 47, 47, 47},
        {47, 47, 47, 47, 47, 47, 48, 18, 17, 16, 15, 14, 44, 47, 47, 47, 47, 47},
        {47, 47, 47, 47, 47, 43, 42, 41, 40, 39, 38, 47, 47, 47, 47, 47, 47, 47},

        {47, 47, 47, 57, 56, 37, 36, 35, 34, 33, 32, 47, 47, 47, 47, 47, 47, 47},
        {47, 47, 47, 55, 54, 31, 30, 29, 28, 27, 26, 47, 47, 13, 12, 47, 47, 47},
        {47, 47, 47, 53, 52, 25, 24, 23, 22, 21, 20, 11, 10, 9, 8, 7, 47, 47},
        {60, 59, 58, 51, 49, 47, 47, 47, 47, 47, 47, 19, 5, 4, 3, 2, 1, 0},
        {47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 47, 6, 45, 46, 47, 47, 47},
    };

    for (int j = 0; j < rows.length; j++) {
      for (int k = 0; k < rows[j].length; k++) {
        int room = rows[j][k];
        tileSpecs.add(new TileSpec(room, k, j));
      }
    }


    gameList = tileSpecs.stream().map(ts -> createGame(ts)).toList();

    ScrollingScreenComponent scrollingScreenComponent = new ScrollingScreenComponent(gameList, mainGameTile);
    JFrame screen = MiniZX.createScreen(((DefaultMiniZXIO) SpectrumApplication.io).miniZXKeyboard, scrollingScreenComponent);
    screen.addKeyListener(new KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() == '1') {
          scrollingScreenComponent.scale += 0.07f;
        }
        if (e.getKeyChar() == '2') {
          scrollingScreenComponent.scale -= 0.07f;
        }
      }
    });


    System.setProperty("jdk.virtualThreadScheduler.parallelism", "2000");
    System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", "1000");


    Thread.startVirtualThread(() -> ((DD0) mainGameTile.zxGame).$C804());
    gameList.forEach(g -> Thread.startVirtualThread(() -> ((DD0) g.zxGame).$C804()));
  }

  private static GameTile<WordNumber, MiniZX> createGame(TileSpec tileSpec) {
    DD0 zxGame1 = new DD0() {

      @Override
      public void $CA5B() {
        //super.$CA5B();
      }

      public void $C804() {
        if (getMem()[33824] == 47) {
          for (int i = 16384; i < 23296; i++) {
            wMem(i, 0, -1);
          }
          return;
        }

        if (!isMain()) {
          for (int i = 0x633A; i < 0x648A; i++) {
            getMem()[i] = 0;
          }
          for (int i = 0x648A; i < 0x65DA; i++) {
            getMem()[i] = 0;
          }
          getMem()[33824] = tileSpec.room;
        }
        super.$C804();
      }


      public int mem(int address, int pc) {

//        if (pc == 35401 && !isMain()) {
//          delay(1L);
//          throw new StackException(37048);
//        }
        int value = super.mem(address, pc);

        if (pc == 0xDE55) {
          delay(30L);
          while (!tileSpec.visible) {
            delay(10000L);
          }
        }

        if (roomChanged && isMain() && pc == 0xCBBD) {
          roomChanged = false;
          mainGameTile.x = tileSpec.x;
          mainGameTile.y = tileSpec.y;
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

      private void delay(long millis) {
        try {
          Thread.sleep(Duration.ofMillis(millis));
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }

      public void wMem(int address, int value, int pc) {
        value = changeEnterRoom(value, pc);

        if (!isMain() && pc == 0xCBBD) {
          A= tileSpec.room;
          value=A;
        }

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
//        if (isMain() && pc == 38039) {
//          value += 1;
//        }
//
//        if (isMain() && pc == 38057){
//          value -= 1;
//        }
        return value;
      }

      private boolean isMain() {
        return this == mainGameTile.zxGame;
      }

      public int in(int port, int pc) {
        if (isMain())
          return super.in(port, pc);
        else
          return 1;
      }

      private void doDelay() {
        delay(5);
        while (!tileSpec.visible) {
          delay(100L);
        }
      }
    };

    zxGame1.getMem()[33824] = tileSpec.room;
    zxGame1.getMem()[34257] = 0;

    return new GameTile<>(zxGame1, zxGame1.getZxScreenComponent(), tileSpec);
  }
}
