/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package com.fpetrola.oozx.speccy.windows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.net.MalformedURLException;
import java.net.URL;

public class LazyImageIconLoader extends SwingWorker<ImageIcon, Void> {
  private JLabel targetLabel;
  private MouseAdapter mouseAdapter;
  private URL imageUrl;

  public LazyImageIconLoader(JLabel label, URL url) {
    this.targetLabel = label;
    this.imageUrl = url;
    // Optionally set a placeholder icon immediately
    // targetLabel.setIcon(new ImageIcon(getClass().getResource("/path/to/placeholder.png")));
    targetLabel.setText("Loading image...");
  }

  public LazyImageIconLoader(JLabel label, String url, MouseAdapter mouseAdapter) {
    this.targetLabel = label;
    this.mouseAdapter = mouseAdapter;
    try {
      this.imageUrl = new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    targetLabel.setText("Loading image...");
  }

  @Override
  protected ImageIcon doInBackground() throws Exception {
    // Load the image in a background thread
    Image image = new ImageIcon(imageUrl).getImage();
    int width = image.getWidth(null);
    int height = image.getHeight(null);
    if (width > 0 && height > 0) {
      float scale = 1f;
      image = image.getScaledInstance((int) (width / scale), (int) (height / scale), Image.SCALE_FAST);
    }
    return new ImageIcon(image);
  }

  @Override
  protected void done() {
    try {
      ImageIcon loadedIcon = get(); // Get the result from doInBackground
      targetLabel.addMouseListener(mouseAdapter);

      targetLabel.setIcon(loadedIcon);
      targetLabel.setText(null); // Clear loading text
    } catch (Exception e) {
      // Handle potential exceptions during image loading
      e.printStackTrace();
      targetLabel.setText("Error loading image");
    }
  }
}