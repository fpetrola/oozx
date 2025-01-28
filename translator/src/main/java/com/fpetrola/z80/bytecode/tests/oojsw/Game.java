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

class Game {
    List<Entity> entities;
    ScreenBuffer screenBuffer;
    int[] attributeBuffer; // Simulates the attribute buffer at 23552
    int ropeStatusIndicator; // Simulates the rope status indicator at 34262
    int willyAnimationFrame; // Simulates Willy's animation frame at 34258
    int willyX; // Simulates Willy's x-coordinate
    int willyY; // Simulates Willy's y-coordinate

    Game() {
        entities = new ArrayList<>();
        screenBuffer = new ScreenBuffer();
        attributeBuffer = new int[768]; // 32x24 attribute buffer
        ropeStatusIndicator = 0;
        willyAnimationFrame = 0;
        willyX = 0;
        willyY = 0;
    }

    void addEntity(Entity entity) {
        entities.add(entity);
    }

    void drawEntities() {
        for (Entity entity : entities) {
            switch (entity.type) {
                case 3: // Rope
                    drawRope(entity);
                    break;
                case 4: // Arrow
                    drawArrow(entity);
                    break;
                default: // Guardian (type 1 or 2)
                    drawGuardian(entity);
                    break;
            }
        }
    }

    void drawRope(Entity rope) {
        int segmentCounter = 0;
        int drawingByte = 128; // Initial drawing byte (bit 7 set)

        while (segmentCounter < rope.buffer[4]) { // Loop through rope segments
            int x = rope.buffer[2] + segmentCounter; // Calculate x-coordinate
            int y = rope.buffer[3]; // y-coordinate remains constant

            // Check if Willy is on the rope
            if (ropeStatusIndicator == segmentCounter && (rope.buffer[11] & 1) != 0) {
                willyAnimationFrame = determineWillyAnimationFrame(drawingByte);
                willyX = x;
                willyY = y - 16; // Adjust Willy's y-coordinate
            }

            // Draw the rope segment
            screenBuffer.setPixel(x, y, 1); // Draw a pixel for the rope
            segmentCounter++;

            // Rotate the drawing byte based on rope direction
            if ((rope.buffer[1] & 128) != 0) { // Rope swinging right to left
                drawingByte = rotateLeft(drawingByte);
                if ((drawingByte & 1) != 0) {
                    rope.buffer[2]--; // Adjust x-coordinate
                }
            } else { // Rope swinging left to right
                drawingByte = rotateRight(drawingByte);
                if ((drawingByte & 128) != 0) {
                    rope.buffer[2]++; // Adjust x-coordinate
                }
            }
        }

        // Handle Willy's movement along the rope
        if ((rope.buffer[11] & 1) != 0) { // Willy is on the rope
            if (ropeStatusIndicator >= rope.buffer[4]) { // Willy dropped off the bottom
                ropeStatusIndicator = 240;
                willyY = (willyY & 0xF8); // Round y-coordinate to nearest multiple of 8
            } else {
                ropeStatusIndicator += (willyAnimationFrame == 1) ? 1 : -1; // Move Willy up or down
            }
        }
    }

    void drawArrow(Entity arrow) {
        // Update arrow position
        if ((arrow.buffer[0] & 128) != 0) { // Arrow moving left to right
            arrow.buffer[4]++;
        } else { // Arrow moving right to left
            arrow.buffer[4]--;
        }

        // Check if arrow is on-screen
        if ((arrow.buffer[4] & 224) != 0) { // Arrow is off-screen
            return;
        }

        // Draw the arrow
        int x = arrow.buffer[4];
        int y = arrow.buffer[2];
        screenBuffer.setPixel(x, y, 2); // Draw the arrow shaft

        // Check for collision with Willy
        if (attributeBuffer[y * 32 + x] == 7) { // Collision with white INK
            killWilly();
        }
    }

    void drawGuardian(Entity guardian) {
        int x = guardian.buffer[2] & 31; // Extract x-coordinate (bits 0-4)
        int y = guardian.buffer[3]; // y-coordinate
        int inkColor = (guardian.buffer[1] & 7); // Extract INK color (bits 0-2)
        int bright = (guardian.buffer[1] & 8) << 3; // Extract BRIGHT value (bit 3)

        // Calculate attribute value
        int attributeValue = (attributeBuffer[y * 32 + x] & 56) | inkColor | bright;
        attributeBuffer[y * 32 + x] = attributeValue;

        // Draw the guardian sprite
        int spriteIndex = (guardian.buffer[0] & 224) >> 5; // Extract sprite index (bits 5-7)
        drawSprite(x, y, spriteIndex);

        // Check for collision with Willy
        if (checkCollision(x, y)) {
            killWilly();
        }
    }

    void drawSprite(int x, int y, int spriteIndex) {
        // Simplified sprite drawing logic
        screenBuffer.setPixel(x, y, 3); // Draw guardian sprite
    }

    boolean checkCollision(int x, int y) {
        // Simplified collision detection
        return (x == willyX && y == willyY);
    }

    void killWilly() {
        System.out.println("Willy has been killed!");
        // Reset Willy's position or end the game
        willyX = 0;
        willyY = 0;
    }

    int rotateLeft(int value) {
        return ((value << 1) | (value >> 7)) & 255;
    }

    int rotateRight(int value) {
        return ((value >> 1) | (value << 7)) & 255;
    }

    int determineWillyAnimationFrame(int drawingByte) {
        if ((drawingByte & 3) != 0) return 1;
        if ((drawingByte & 12) != 0) return 0;
        if ((drawingByte & 48) != 0) return 3;
        return 2;
    }

    void run() {
        drawEntities();
    }
}