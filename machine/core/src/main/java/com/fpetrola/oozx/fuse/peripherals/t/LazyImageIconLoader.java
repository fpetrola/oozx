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

package com.fpetrola.oozx.fuse.peripherals.t;

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