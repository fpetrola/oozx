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
package com.fpetrola.oozx.speccy.devices;

import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.oozx.speccy.peripherals.Pluggable;
import com.fpetrola.oozx.speccy.modules.sound.Dac;
import com.fpetrola.oozx.speccy.modules.sound.DacDevice;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;

/**
 * A box with a DAC in it, on the desk: a meter for what is coming out of it, and its volume.
 * Expanded, the last byte the program sent, which is all such a box ever holds.
 */
public class DacFrame<P extends Peripheral & Pluggable & DacDevice> extends DeviceFrame<P> {

  private static final int REFRESH_MILLIS = 40;

  private final LevelMeter meter = new LevelMeter();
  private final JSlider volume = new JSlider(0, 100, 100);
  private final JLabel last = new JLabel();
  private final Timer refresh = new Timer(REFRESH_MILLIS, e -> refresh());
  private final double fullScale;

  /** @param fullScale the synth level a byte of 255 comes to, for the meter's top */
  public DacFrame(String name, Class<? extends P> kind, double fullScale) {
    super(name, kind);
    this.fullScale = fullScale;
    setSize(420, 160);
    volume.setToolTipText("How loud this box is in the mix");
    volume.addChangeListener(e -> {
      if (device() != null) {
        device().setVolume(volume.getValue());
      }
    });
    controls.add(meter);
    controls.add(volume);

    JPanel inside = new JPanel(new BorderLayout());
    inside.add(last, BorderLayout.NORTH);
    assemble(inside);
    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        refresh.stop();
      }
    });
    refresh.start();
    plugged(null);
  }

  @Override
  protected void plugged(P device) {
    volume.setEnabled(device != null);
    if (device != null) {
      volume.setValue(device.volume());
    }
    refresh();
  }

  private void refresh() {
    Dac dac = device() == null ? null : device().dac();
    if (dac == null) {
      meter.show(0);
      last.setText("not plugged into a machine");
      return;
    }
    meter.show(Math.abs(dac.level()) / fullScale);
    last.setText(String.format("last byte sent: %d", dac.level() / 128 + (fullScale > 20000 ? 128 : 0)));
  }

  @Override
  protected String expandTip() {
    return "Show the last byte sent, or just the meter";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window, which is what plugs the box in";
  }
}
