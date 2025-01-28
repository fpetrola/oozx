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

package com.fpetrola.z80.bytecode.tests.oojsw;

import java.util.ArrayList;
import java.util.List;

class Entity {
    int type;
    int x;
    int y;
    int[] buffer;

    Entity(int type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.buffer = new int[12]; // Assuming a buffer size of 12 bytes
    }
}

class ScreenBuffer {
    int[] buffer;

    ScreenBuffer() {
        this.buffer = new int[24576]; // Assuming a screen buffer size of 24576 bytes
    }

    void setPixel(int x, int y, int value) {
        int address = y * 256 + x; // Simplified address calculation
        buffer[address] = value;
    }

    int getPixel(int x, int y) {
        int address = y * 256 + x; // Simplified address calculation
        return buffer[address];
    }
}



public class Main {
    public static void main(String[] args) {
        Game game = new Game();

        // Add entities to the game
        game.addEntity(new Entity(3, 10, 10)); // Rope
        game.addEntity(new Entity(4, 20, 20)); // Arrow
        game.addEntity(new Entity(5, 30, 30)); // Guardian

        // Run the game
        game.run();
    }
}