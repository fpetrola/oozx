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

package com.fpetrola.oozx.speccy.desktop;

import com.fpetrola.oozx.plugins.Plugins;

import dev.crystal.plugins.swing.PluginsPanel;

import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import java.awt.BorderLayout;
import java.util.function.Consumer;

/**
 * What the environment is made of, shown by whoever manages it: what each jar brought, what
 * there is of each kind, and what can still be had. This window is only where that is put.
 * <p>
 * The one thing it does on its own is read the folder: a jar copied in by hand is plugged in at
 * the moment somebody asks what is in, and the menus are told so they say it too.
 */
public class PluginsInternalFrame extends JInternalFrame {

  private final PluginsPanel panel = new PluginsPanel(Plugins.managing());

  /** @param arrived told when what is plugged in may have changed, so the menus can say so */
  public PluginsInternalFrame(Consumer<Void> arrived) {
    super("Plugins", true, true, true, true);
    add(panel, BorderLayout.CENTER);
    setBounds(60, 60, 700, 460);
    addInternalFrameListener(new InternalFrameAdapter() {
      public void internalFrameActivated(InternalFrameEvent shown) {
        Plugins.readWhatArrived();
        panel.refresh();
        arrived.accept(null);
      }

      public void internalFrameDeactivated(InternalFrameEvent left) {
        arrived.accept(null);
      }
    });
  }

}
