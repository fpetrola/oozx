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