/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package com.fpetrola.oozx.speccy.peripherals.t;

import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.sound.AudioIn;

import java.util.function.Function;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.*;

/**
 * A real cassette player, watched: what is coming in the sound card, drawn as it arrives.
 * <p>
 * Clipped onto a machine like the cassette deck beside it, because it is the same piece of
 * equipment with a different cassette in it - one made of air rather than of a file. It does
 * not feed the machine yet; this is the eye on the signal that reading it will need, and
 * reading a tape you cannot see go wrong is guesswork.
 */
public class AudioInInternalFrame extends AttachedFrame {

  /** Fast enough that a tape looks like it is moving rather than stepping. */
  private static final int REFRESH_MILLIS = 33;

  /** The ear line of the machine this is clipped to, which is where the lead goes. */
  private final Function<JInternalFrame, Tape> deckOf;

  private final AudioIn audio = new AudioIn();
  private final Waveform waveform = new Waveform();
  private final JComboBox<String> inputs = new JComboBox<>();
  private final JToggleButton listen =
      EmulatorInternalFrame.iconToggle("25B6.svg", "Listen", "Listen to the input");
  private final JToggleButton followButton = new JToggleButton("Follow");
  private final JToggleButton hear = EmulatorInternalFrame.iconToggle("1F509.svg", "Hear",
      "Hear what is coming in - with a microphone this will howl");
  private final JLabel reading = new JLabel();

  public AudioInInternalFrame(Function<JInternalFrame, Tape> deckOf) {
    super("Audio in");
    this.deckOf = deckOf;
    setSize(820, 360);

    for (String input : AudioIn.inputs()) {
      inputs.addItem(input);
    }
    inputs.setToolTipText("Which input the cassette player is plugged into");
    inputs.setPreferredSize(new Dimension(190, 26));

    listen.addActionListener(e -> listen(listen.isSelected()));

    followButton.setSelected(true);
    followButton.setToolTipText("Keep the newest sound in view");
    followButton.addActionListener(e -> waveform.chasingHead = followButton.isSelected());

    hear.addActionListener(e -> hear(hear.isSelected()));

    controls.add(listen);
    controls.add(hear);
    controls.add(inputs);
    // Written rather than drawn: none of the pictures this application has means "closer", and
    // a wrong picture is worse than a word. The wheel does the same over the waveform itself.
    controls.add(zoom("\u2212", "Wider - more time across the window", 2));
    controls.add(zoom("+", "Closer - fewer samples to a pixel", 0.5));
    controls.add(followButton);
    controls.add(Box.createHorizontalStrut(10));
    controls.add(reading);

    assemble(waveform);
    // The point of this window is the picture, so it opens showing it rather than folded down
    // to its buttons the way a deck that is only a transport bar does.
    setCompact(false);

    Timer refresh = new Timer(REFRESH_MILLIS, e -> {
      waveform.repaint();
      say();
    });
    refresh.start();
    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        refresh.stop();
        if (wired != null) {
          wired.takeEarFrom(null);
        }
        audio.close();
      }
    });
    say();
  }

  /**
   * Clipped on or carried away, which is the lead going in or coming out.
   * <p>
   * Clipped onto a machine, what comes in the sound card drives that machine's ear line, so a
   * real cassette player loads it the way the deck holding a file does. Carried away, the
   * machine goes back to reading its own tape.
   */
  @Override
  protected void attachmentChanged() {
    Tape plugged = isAttached() ? deckOf.apply(getMachineWindow()) : null;
    if (plugged == wired) {
      return;
    }
    if (wired != null) {
      wired.takeEarFrom(null);
    }
    wired = plugged;
    if (wired != null) {
      wired.takeEarFrom(audio::high);
    }
    say();
  }

  private Tape wired;

  @Override
  protected String expandTip() {
    return "Show the waveform, or just the controls";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window";
  }

  private void listen(boolean wanted) {
    try {
      if (wanted) {
        audio.open((String) inputs.getSelectedItem());
        waveform.leftSample = 0;
      } else {
        audio.close();
      }
    } catch (Exception unavailable) {
      listen.setSelected(false);
      JOptionPane.showMessageDialog(this, "That input could not be opened.\n\n"
          + unavailable.getMessage(), "Audio in", JOptionPane.ERROR_MESSAGE);
    }
    say();
  }

  private void hear(boolean wanted) {
    try {
      audio.hear(wanted);
    } catch (Exception unavailable) {
      hear.setSelected(false);
      JOptionPane.showMessageDialog(this, "There is nothing to play it through.\n\n"
          + unavailable.getMessage(), "Audio in", JOptionPane.ERROR_MESSAGE);
    }
  }

  /** Where the view is and how close, in the units somebody watching a tape thinks in. */
  private void say() {
    double seconds = waveform.leftSample / (double) AudioIn.RATE;
    double window = waveform.samplesPerPixel * Math.max(1, waveform.getWidth())
        / (double) AudioIn.RATE;
    // The level is here so that choosing an input is a matter of watching rather than guessing:
    // several of them open and deliver silence for ever, and silence looks like a quiet room.
    // Dead silence for a whole second is not a quiet room, it is the wrong input: several of
    // them open and deliver zeroes for ever, and the two look identical without saying so.
    double loudest = audio.peak(1);
    String level = !audio.isListening() ? "stopped"
        : loudest == 0 ? "no signal - try another input"
            : String.format("level %3.0f%%", loudest * 100);
    reading.setText(String.format("%s   %s   %.3fs   %.1f ms per pixel   %.2fs in view",
        wired == null ? "not plugged into a machine" : "into the machine's ear", level, seconds,
        waveform.samplesPerPixel * 1000.0 / AudioIn.RATE, window));
  }

  private JButton zoom(String mark, String tip, double by) {
    JButton button = new JButton(mark);
    button.setToolTipText(tip);
    button.addActionListener(e -> waveform.zoom(by, waveform.getWidth() / 2));
    return button;
  }

  /**
   * The sound itself, drawn a column of pixels at a time.
   * <p>
   * Each column is the loudest and the quietest of everything that falls in it, which is what
   * makes a waveform readable at any distance: zoomed out it is the shape of the tape, zoomed
   * in it becomes the individual pulses, and neither is a different drawing.
   */
  private class Waveform extends JComponent {

    /** How much sound one pixel across stands for: the zoom. */
    private double samplesPerPixel = 64;

    /** The sample at the left-hand edge: the pan. */
    private long leftSample;

    /** Whether the view keeps up with the head, or stays where it was left. */
    private boolean chasingHead = true;

    private int draggingFrom = -1;
    private long draggingLeft;

    Waveform() {
      setBackground(new Color(0x101418));
      addMouseWheelListener(turned -> zoom(turned.getWheelRotation() > 0 ? 2 : 0.5, turned.getX()));
      addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent pressed) {
          draggingFrom = pressed.getX();
          draggingLeft = leftSample;
          // Taking hold of the picture is saying where to look, so it stops chasing the head.
          chasingHead = false;
          followButton.setSelected(false);
        }
      });
      addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent dragged) {
          if (draggingFrom >= 0) {
            leftSample = draggingLeft - (long) ((dragged.getX() - draggingFrom) * samplesPerPixel);
            repaint();
          }
        }
      });
    }

    /** Closer or wider, about the sound under that column rather than about the left edge. */
    void zoom(double by, int aboutX) {
      long under = leftSample + (long) (aboutX * samplesPerPixel);
      samplesPerPixel = Math.max(1.0 / 16, Math.min(4096, samplesPerPixel * by));
      leftSample = under - (long) (aboutX * samplesPerPixel);
      repaint();
    }

    @Override
    protected void paintComponent(Graphics pen) {
      int width = getWidth(), height = getHeight(), middle = height / 2;
      pen.setColor(getBackground());
      pen.fillRect(0, 0, width, height);

      if (chasingHead) {
        leftSample = Math.max(0, audio.written() - (long) (width * samplesPerPixel));
      }

      pen.setColor(new Color(0x1E2A33));
      pen.drawLine(0, middle, width, middle);

      pen.setColor(new Color(0x5FD08A));
      for (int x = 0; x < width; x++) {
        long from = leftSample + (long) (x * samplesPerPixel);
        long to = leftSample + (long) ((x + 1) * samplesPerPixel);
        short low = 0, high = 0;
        for (long at = from; at < Math.max(from + 1, to); at++) {
          short value = audio.at(at);
          if (value < low) low = value;
          if (value > high) high = value;
        }
        pen.drawLine(x, middle - high * middle / Short.MAX_VALUE,
            x, middle - low * middle / Short.MAX_VALUE);
      }
    }
  }
}
