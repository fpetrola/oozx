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

            // Transcoder prepara el SVG → PNG (en memoria)
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
