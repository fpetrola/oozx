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

package com.fpetrola.oozx.speccy.modules.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fpetrola.oozx.speccy.modules.joystick.Joystick;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Everything the person at the keyboard reaches the machine through. */
@Singleton
public class InputSetup {
  private Pad joystick1 = new Pad();
  private Pad joystick2 = new Pad();
  private Keys keyboard = new Keys();
  /** Whether the arrow keys are read shifted, which is what a Spectrum's own cursor keys are. */
  private boolean arrowsShifted;
  /** Whether a Kempston joystick interface is fitted. */
  private boolean kempstonJoystick;
  /** Whether a Kempston mouse is fitted. */
  private boolean kempstonMouse;
  /** A Recreated ZX Spectrum is a keyboard that sends its own codes rather than a keyboard's. */
  private boolean recreatedSpectrum;

  public Pad joystick1() { return joystick1; }
  public Pad joystick2() { return joystick2; }
  public Keys keyboard() { return keyboard; }
  public boolean arrowsShifted() { return arrowsShifted; }
  public boolean kempstonJoystick() { return kempstonJoystick; }
  public boolean kempstonMouse() { return kempstonMouse; }
  public boolean recreatedSpectrum() { return recreatedSpectrum; }
  public void setJoystick1(Pad chosen) { joystick1.take(chosen); }
  public void setJoystick2(Pad chosen) { joystick2.take(chosen); }
  public void setKeyboard(Keys chosen) { keyboard.take(chosen); }
  public void setArrowsShifted(boolean on) { arrowsShifted = on; }
  public void setKempstonJoystick(boolean on) { kempstonJoystick = on; }
  public void setKempstonMouse(boolean on) { kempstonMouse = on; }
  public void setRecreatedSpectrum(boolean on) { recreatedSpectrum = on; }

  /** The sticks by the socket the joystick numbers them with: the two pads, then the keyboard's. */
  public List<Stick> sticks() {
    return List.of(joystick1, joystick2, keyboard);
  }

  /**
   * What a joystick is to the machine: which of the joysticks a Spectrum understands it answers
   * as. What moves it differs - a pad has buttons, the keyboard has keys - and that is what the
   * two shapes below add.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static abstract class Stick {
    private Joystick.JoystickType output;

    public Joystick.JoystickType getOutput() { return output; }
    public void setOutput(Joystick.JoystickType chosen) { output = chosen; }

    /** What was chosen, all of it: a key it leaves out is one this does not have any more. */
    void take(Stick chosen) {
      if (chosen != null) output = chosen.output;
    }
  }

  /** A pad plugged into the host. */
  public static class Pad extends Stick {
  }

  /**
   * The keys of whoever is typing used as a joystick: four directions and a fire. Kept by the
   * name of the key rather than by its number, so a file someone reads says "INPUT_KEY_a".
   */
  public static class Keys extends Stick {
    private Input.InputKey up;
    private Input.InputKey down;
    private Input.InputKey left;
    private Input.InputKey right;
    private Input.InputKey fire;
    /** Nobody is holding a pad, so the arrows and space stand in for one. Not a setting. */
    private transient boolean noPad;

    public Input.InputKey getUp() { return up; }
    public Input.InputKey getDown() { return down; }
    public Input.InputKey getLeft() { return left; }
    public Input.InputKey getRight() { return right; }
    public Input.InputKey getFire() { return fire; }
    public void setUp(Input.InputKey key) { up = key; }
    public void setDown(Input.InputKey key) { down = key; }
    public void setLeft(Input.InputKey key) { left = key; }
    public void setRight(Input.InputKey key) { right = key; }
    public void setFire(Input.InputKey key) { fire = key; }

    /** Told from outside because who is holding what is not the file's to say. */
    public void standInForAPad(boolean stand) {
      noPad = stand;
    }

    /**
     * Which way a key of the host pushes the stick, or none when it is not the joystick's and
     * the machine's own keyboard should see it.
     * <p>
     * A direction nobody chose is the arrow that looks like it, and fire is space, while no pad
     * is plugged in: with nothing else holding the Kempston, the keys are what is left to hold
     * it. Space under fire is what Fuse does too, and Ctrl fires as well: many keyboards cannot
     * report space with two arrows held, and a modifier is wired apart from the rest.
     * <p>
     * Those stand-in keys are only taken while {@code standingIn} says they are wanted - the
     * machine is reading its Kempston port - because space is a key of the Spectrum too, and a
     * game that asks for it to start would never see it if the stand-in took it all the time.
     * A key the file chose is the joystick's either way: that was somebody's decision.
     */
    public Optional<Joystick.JoystickButton> pushes(Input.InputKey key, boolean standingIn) {
      boolean standIn = noPad && standingIn;
      return Stream.of(
              new Binding(up, Input.InputKey.INPUT_KEY_Up, Joystick.JoystickButton.JOYSTICK_BUTTON_UP),
              new Binding(down, Input.InputKey.INPUT_KEY_Down, Joystick.JoystickButton.JOYSTICK_BUTTON_DOWN),
              new Binding(left, Input.InputKey.INPUT_KEY_Left, Joystick.JoystickButton.JOYSTICK_BUTTON_LEFT),
              new Binding(right, Input.InputKey.INPUT_KEY_Right, Joystick.JoystickButton.JOYSTICK_BUTTON_RIGHT),
              new Binding(fire, Input.InputKey.INPUT_KEY_space, Joystick.JoystickButton.JOYSTICK_BUTTON_FIRE),
              new Binding(fire, Input.InputKey.INPUT_KEY_Control_L, Joystick.JoystickButton.JOYSTICK_BUTTON_FIRE))
          .filter(binding -> binding.takes(key, standIn)).map(Binding::button).findFirst();
    }

    /** A button of the stick, the key chosen for it and the one that stands in when none was. */
    private record Binding(Input.InputKey chosen, Input.InputKey standIn, Joystick.JoystickButton button) {
      boolean takes(Input.InputKey key, boolean standingIn) {
        boolean bound = chosen != null || standingIn;
        return bound && key != null && key != Input.InputKey.INPUT_KEY_NONE && key == (chosen != null ? chosen : standIn);
      }
    }

    void take(Keys chosen) {
      super.take(chosen);
      if (chosen == null) return;
      up = chosen.up;
      down = chosen.down;
      left = chosen.left;
      right = chosen.right;
      fire = chosen.fire;
    }
  }
}
