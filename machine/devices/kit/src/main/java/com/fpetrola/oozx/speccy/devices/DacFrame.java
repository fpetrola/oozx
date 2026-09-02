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
package com.fpetrola.oozx.speccy.devices;

import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.oozx.speccy.peripherals.Pluggable;
import com.fpetrola.oozx.speccy.sound.Dac;
import com.fpetrola.oozx.speccy.sound.DacDevice;

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
