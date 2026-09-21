/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.speccy.tools.keyboard;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.input.Input;
import com.fpetrola.oozx.speccy.modules.keyboard.SpectrumKey;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import org.junit.jupiter.api.Test;

import com.fpetrola.oozx.speccy.devices.EmulatorWindow;

import javax.swing.JInternalFrame;
import java.awt.Rectangle;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The window shows the keys the machine in front reads as held, and every key has a place on the picture. */
class KeyboardFrameTest {

  /** A machine's window, which is how a window clipped onto it finds the machine. */
  private static JInternalFrame machineWindow(Speccy speccy) {
    class Window extends JInternalFrame implements EmulatorWindow {
      Window() {
        super("machine");
      }

      public Speccy machine() {
        return speccy;
      }

      public javax.swing.JComponent picture() {
        return null;
      }
    }
    return new Window();
  }
  @Test
  void itShowsTheKeysTheMachineInFrontHasDown() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    JInternalFrame machine = machineWindow(speccy);
    KeyboardFrame window = new KeyboardFrame();
    window.setMachineWindow(machine);

    Input.of(speccy).keyboard().press(SpectrumKey.CAPS_SHIFT);
    Input.of(speccy).keyboard().press(SpectrumKey.ZERO);
    assertEquals(EnumSet.of(SpectrumKey.CAPS_SHIFT, SpectrumKey.ZERO), window.held(), "caps shift and 0, as the matrix reads them");

    Input.of(speccy).keyboard().release(SpectrumKey.ZERO);
    assertEquals(EnumSet.of(SpectrumKey.CAPS_SHIFT), window.held(), "and 0 let go");

    window.setMachineWindow(null);
    assertTrue(window.held().isEmpty(), "with no machine there is nothing held");
    window.refresh();
    assertTrue(window.getTitle().contains("no machine"), "the title says so: " + window.getTitle());
  }

  /** Clicking the picture types on the machine, which is the same keys coming back as held. */
  @Test
  void typingOnThePictureReachesTheMachine() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    JInternalFrame machine = machineWindow(speccy);
    KeyboardFrame window = new KeyboardFrame();
    window.setMachineWindow(machine);

    window.type(SpectrumKey.Q, true);
    assertEquals(EnumSet.of(SpectrumKey.Q), window.held(), "Q went down on the machine");
    window.type(SpectrumKey.Q, false);
    assertTrue(window.held().isEmpty(), "and came back up");
  }

  /** The point clicked has to find the key drawn under it, at the middle of every one of them. */
  @Test
  void eachPlaceOnThePictureFindsItsOwnKey() {
    for (SpectrumKey key : SpectrumKey.values()) {
      Rectangle place = KeyboardFrame.placeOf(key);
      assertEquals(key, KeyboardFrame.at((int) place.getCenterX(), (int) place.getCenterY()),
          "the middle of " + key);
    }
    assertEquals(null, KeyboardFrame.at(5, 5), "the corner of the case is no key");
  }

  /**
   * A key goes down towards the camera, not straight down the picture: the photograph shows the
   * right face of the keys on the left and the left face of the ones on the right.
   */
  @Test
  void aKeyGoesDownTowardsTheCamerasAxis() {
    Rectangle leftmost = KeyboardFrame.placeOf(SpectrumKey.Q);
    Rectangle rightmost = KeyboardFrame.placeOf(SpectrumKey.P);

    assertTrue(KeyboardFrame.sunk(leftmost).getCenterX() > leftmost.getCenterX(), "a key on the left goes right");
    assertTrue(KeyboardFrame.sunk(rightmost).getCenterX() < rightmost.getCenterX(), "and one on the right goes left");
    assertEquals(leftmost.width, KeyboardFrame.sunk(leftmost).width,
        "and the face keeps its size: it slides down its own wall, it does not shrink away");

    // what it must not do is slide: the picture shows a wall of five pixels at the edges and none
    // up or down, so a key that travelled any distance would read as moved rather than pressed
    assertTrue(Math.abs(KeyboardFrame.sunk(leftmost).getCenterX() - leftmost.getCenterX()) < 8,
        "and none of them travels far sideways");
    assertTrue(Math.abs(KeyboardFrame.sunk(leftmost).getCenterY() - leftmost.getCenterY()) < 3,
        "nor up the picture, which is what looked slid");
  }

  /** The shifts are held for the next key, because one pointer cannot hold two keys at once. */
  @Test
  void clickingAShiftHoldsItForWhateverIsTypedNext() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    JInternalFrame machine = machineWindow(speccy);
    KeyboardFrame window = new KeyboardFrame();
    window.setMachineWindow(machine);

    window.clicked(SpectrumKey.SYMBOL_SHIFT, true);
    window.clicked(SpectrumKey.SYMBOL_SHIFT, false);
    assertEquals(EnumSet.of(SpectrumKey.SYMBOL_SHIFT), window.held(), "it stays down once the click is over");

    window.clicked(SpectrumKey.P, true);
    assertEquals(EnumSet.of(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.P), window.held(), "symbol shift and P, which is a quote");
    window.clicked(SpectrumKey.P, false);
    assertTrue(window.held().isEmpty(), "and letting P go lets the shift go with it");
    assertTrue(window.stuck().isEmpty(), "with nothing left held for the next key");

    window.clicked(SpectrumKey.CAPS_SHIFT, true);
    window.clicked(SpectrumKey.CAPS_SHIFT, false);
    window.clicked(SpectrumKey.CAPS_SHIFT, true);
    window.clicked(SpectrumKey.CAPS_SHIFT, false);
    assertTrue(window.held().isEmpty(), "clicking a held shift again lets it go");
  }

  /**
   * The hole has to swallow the whole key and not just the lit top face the table holds: a key
   * reaches six pixels further at the sides and two or three at the ends, and those were left
   * standing where the key had been.
   */
  @Test
  void theHoleSwallowsMoreThanTheLitTopFace() {
    for (SpectrumKey key : SpectrumKey.values()) {
      Rectangle place = KeyboardFrame.placeOf(key);
      Rectangle hole = KeyboardFrame.holeFor(place);
      Rectangle whole = KeyboardFrame.keyOn(place);
      assertTrue(whole.contains(place), key + " is not bigger than its own lit top face");
      assertTrue(hole.contains(whole), key + " leaves its wall or its edge showing");
      assertTrue(hole.contains(KeyboardFrame.sunk(place)), key + " sinks outside its hole");
      assertTrue(hole.y < place.y && hole.x < place.x, key + " leaves the walls of the key standing");
    }
  }

  /**
   * And it has to swallow it lopsidedly. A key only shows the wall on the side that looks at the
   * camera's axis, so a hole that grew evenly put black where there had never been any key.
   */
  @Test
  void theHoleReachesFurtherOnTheSideTheWallIsOn() {
    Rectangle onTheLeft = KeyboardFrame.placeOf(SpectrumKey.Q);
    Rectangle itsHole = KeyboardFrame.holeFor(onTheLeft);
    assertTrue(onTheLeft.x - itsHole.x < itsHole.x + itsHole.width - (onTheLeft.x + onTheLeft.width),
        "a key on the left shows its right wall, so its hole reaches further to the right");

    Rectangle onTheRight = KeyboardFrame.placeOf(SpectrumKey.P);
    Rectangle otherHole = KeyboardFrame.holeFor(onTheRight);
    assertTrue(onTheRight.x - otherHole.x > otherHole.x + otherHole.width - (onTheRight.x + onTheRight.width),
        "and one on the right reaches further to the left");
  }

  /** A key with no place on the picture would silently never light, so the table has to be complete. */
  @Test
  void everyKeyOfTheMachineHasAPlaceOnThePicture() {
    JInternalFrame machine = new JInternalFrame("machine");
    KeyboardFrame window = new KeyboardFrame();
    window.setMachineWindow(machine);
    for (SpectrumKey key : SpectrumKey.values()) {
      assertTrue(KeyboardFrame.placeOf(key) != null, key + " is not on the picture");
    }
  }
}
