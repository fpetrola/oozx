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

import com.fpetrola.z80.minizx.StackException;

public class JetSetWillyOO2 extends JetSetWillyOO {
  public static void main(String[] args) {
    new JetSetWillyOO2().$35090();
  }

  public void $37310() {
    if (false) {
      super.$37310();
    } else {
      JSWEntityList entityList = getEntityList();
      JSWEntity jswEntity = entityList.first();
      ScreenBufferLookupTable screenBufferLookupTable = new ScreenBufferLookupTable();

      while (true) {
        int type = jswEntity.getType();
        if (type == 255) {
          return;
        }

        if ((type & 7) == 1 || (type & 7) == 2) {
          int lookup = screenBufferLookupTable.lookup(jswEntity.getY());
          AttributeHandler ah = jswEntity.getCurrentLocationAtAttributeBuffer(jswEntity.getX(), lookup);
          int newAttribute = (ah.getValue() & 56) ^ jswEntity.getAttribute();
          ah.setValue(newAttribute);
          ah.nextColumn();
          ah.setValue(newAttribute);
          ah.nextRow();
          ah.setValue(newAttribute);
          ah.nextColumn();
          ah.setValue(newAttribute);
          if ((jswEntity.getY() & 14) != 0) {
            ah.nextRow();
            ah.setValue(newAttribute);
            ah.nextColumn();
            ah.setValue(newAttribute);
          }

          FrameSprite frameSprite = jswEntity.getAnimationFrameGraphic();
          ScreenBlock screenBlock = jswEntity.getScreenBufferCurrentLocation();

          if ($37974_2(frameSprite, screenBlock, 1)) {
            throw new StackException(37048);
          }
        }

        jswEntity = entityList.next();
      }
    }
  }

  public boolean $37974_2(FrameSprite frameSprite, ScreenBlock screenBlock, int drawingMode) {
    int counter = 16;

    do {
      int frameLine = frameSprite.getValue();
      if ((drawingMode & 1) != 0) {
        if ((frameLine & screenBlock.getValue()) != 0)
          return false;

        frameLine = frameSprite.getValue() | screenBlock.getValue();
      }

      screenBlock.setValue(frameLine);
      screenBlock.nextColumn();
      frameSprite.next();
      frameLine = frameSprite.getValue();
      if ((drawingMode & 1) != 0) {
        if ((frameLine & screenBlock.getValue()) != 0)
          return true;

        frameLine = frameSprite.getValue() | screenBlock.getValue();
      }

      screenBlock.setValue(frameLine);
      screenBlock.nextRow();
      frameSprite.next();
      if (screenBlock.isBottonLine()) {
        if ((screenBlock.nextBelow() & 224) == 0) {
          screenBlock.adjustMiddle();
        }
      }

    } while (--counter != 0);

    return false;
  }

  private JSWEntityList getEntityList() {
    return new JSWEntityList(() -> {
      IX(33024);
    });
  }

  public boolean $37974(FrameSprite frameSprite, ScreenBlock screenBlock, int drawingMode) {
    DE(frameSprite.getAddress());
    HL(screenBlock.getAddress());
    C = drawingMode;
    $37974();
    return F != 0;
  }

  public class AttributeBuffer {
  }

  public class AttributeHandler {
    public AttributeHandler(Runnable o) {
      o.run();
    }

    void setValue(int c) {
      wMem(HL(), c, 37373);
    }

    void nextRow() {
      HL(HL() + DE() & 65535);
    }

    void nextColumn() {
      HL(HL() + 1 & 65535);
    }

    int getValue() {
      return mem(HL(), 37368);
    }
  }

  public class FrameSprite {
    private final Runnable o;
    private boolean initialized;

    public FrameSprite(Runnable o) {
      this.o = o;
    }

    public int getAddress() {
      o.run();
      return DE();
    }

    private int getValue() {
      if (!initialized) {
        o.run();
        initialized = true;
      }
      return mem(DE(), 37978);
    }

    private void next() {
      DE(DE() + 1 & 65535);
    }
  }

  public class JSWEntity {
    int getX() {
      return mem(IX() + 2, 37341);
    }

    int getAttribute() {
      A = mem(IX() + 1, 37358);
      A = A & 15;
      A = A + 56 & 255;
      A = A & 71;
      return A;
    }

    int getY() {
      return mem(IX() + 3, 37380);
    }

    FrameSprite getAnimationFrameGraphic() {
      return new FrameSprite(() -> {
        A = mem(IX() + 1, 37393);
        A = A & mem(IX() + 0, 37396);
        A = A | mem(IX() + 2, 37399);
        A = A & 224;
        E = A;
        D = mem(IX() + 5, 37405);
        int de = DE();
      });
    }

    ScreenBlock getScreenBufferCurrentLocation() {

      return new ScreenBlock(() -> {
        H = 130;
        L = mem(IX() + 3, 37410);
        A = mem(IX() + 2, 37413);
        A = A & 31;
        A = A | mem(HL(), 37418);
        HL(HL() + 1 & 65535);
        H = mem(HL(), 37420);
        L = A;
        int hl = HL();
      });
    }

    AttributeHandler getCurrentLocationAtAttributeBuffer(int x, int lookup) {
      return new AttributeHandler(() -> {
        L = lookup;
        A = x;
        A = A & 31;
        A = A + L & 255;
        L = A;
        A = E;
        A = rlc(A);
        A = A & 1;
        A = A | 92;
        F = A << 1;
        H = A;

        DE(31);
      });
    }

    public int getType() {
      return mem(IX() + 0, 37393);
    }
  }

  public class JSWEntityList {

    private final Runnable runnable;

    public JSWEntityList(Runnable o) {
      this.runnable = o;
    }

    public JSWEntity next() {
      IX(IX() + 8 & 65535);
      return new JSWEntity();
    }

    public JSWEntity first() {
      runnable.run();
      return new JSWEntity();
    }
  }

  public class ScreenBlock {
    private final Runnable o;
    private boolean initialized;

    public ScreenBlock(Runnable o) {
      this.o = o;
    }

    public int getAddress() {
      o.run();
      return HL();
    }

    private int getValue() {
      if (!initialized) {
        o.run();
        initialized = true;
      }
      return mem(HL(), 37981);
    }

    private void setValue(int a) {
      wMem(HL(), a, 37985);
    }

    private void nextColumn() {
      L = L + 1 & 255;
    }

    private void nextRow() {
      L = L - 1 & 255;
      H = H + 1 & 255;
    }

    private boolean isBottonLine() {
      A = H;
      A = A & 7;
      F = A << 1;
      return F == 0;
    }

    private int nextBelow() {
      H = H - 8 & 255;
      L = L + 32 & 255;
      return L;
    }

    private void adjustMiddle() {
      H = H + 8 & 255;
    }
  }

  public class ScreenBufferLookupTable {
    public int lookup(int e) {
      E = e;
      D = 130;
      return mem(DE(), 37339);
    }
  }
}
