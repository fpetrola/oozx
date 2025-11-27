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