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

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.Iterator;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.swing.*;

public class ThumbnailWorker extends SwingWorker<Image, Void>
{
    private File file;
    private DefaultListModel<Thumbnail> model;
    private int index;

    public ThumbnailWorker(File file, DefaultListModel<Thumbnail> model, int index)
    {
        this.file = file;
        this.model = model;
        this.index = index;
    }

    @Override
    protected Image doInBackground() throws IOException
    {
//      Image image = ImageIO.read( file );

        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("jpg");
        ImageReader reader = readers.next();
        ImageReadParam irp = reader.getDefaultReadParam();
//      irp.setSourceSubsampling(10, 10, 0, 0);
        irp.setSourceSubsampling(5, 5, 0, 0);
        ImageInputStream stream = new FileImageInputStream( file );
        reader.setInput(stream);
        Image image = reader.read(0, irp);

        int width = 160;
        int height = 90;

        if (image.getHeight(null) > image.getWidth(null))
        {
            width = 90;
            height = 160;
        }

        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2d.drawImage(image, 0, 0, width, height, null);
        g2d.dispose();
        image = null;

        return scaled;
   }

   @Override
   protected void done()
   {
       try
       {
           ImageIcon icon = new ImageIcon( get() );
           Thumbnail thumbnail = model.get( index );
           thumbnail.setIcon( icon );
           model.set(index, thumbnail);
//         System.out.println("finished: " + file);
       }
       catch (Exception e)
       {
           e.printStackTrace();
       }
   }
}