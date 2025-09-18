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

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class SvgIconLoader {

  public static ImageIcon loadSvgAsImageIcon(String svgFilePath, int width, int height) {
    try (InputStream is = SvgIconLoader0.class.getResourceAsStream(svgFilePath)) {
      BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

      ImageTranscoder transcoder = new ImageTranscoder() {
        public BufferedImage createImage(int w, int h) {
          return bufferedImage;
        }

        public void writeImage(BufferedImage img, TranscoderOutput output) throws TranscoderException {
          // Not needed for rendering to a pre-existing BufferedImage
        }
      };
      transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) width);
      transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) height);

      TranscoderInput input = new TranscoderInput(is);
      TranscoderOutput output = new TranscoderOutput(); // Output to the pre-existing BufferedImage

      transcoder.transcode(input, output);

      return new ImageIcon(bufferedImage);

    } catch (Exception e) {
      throw new RuntimeException("Error loading SVG icon: " + svgFilePath, e);
    }
  }
}