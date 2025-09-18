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

import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.PNGTranscoder;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class SvgIconLoader0 {

    public static Icon loadSvgIcon(String path, int size) {
        try (InputStream is = SvgIconLoader0.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new FileNotFoundException("SVG not found: " + path);
            }

            TranscoderInput input = new TranscoderInput(is);
            ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(pngBytes);

            PNGTranscoder transcoder = new PNGTranscoder();
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) size);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) size);

            transcoder.transcode(input, output);

            byte[] imageData = pngBytes.toByteArray();
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));

            return new ImageIcon(img);

        } catch (Exception e) {
            throw new RuntimeException("Error loading SVG icon: " + path, e);
        }
    }
}
