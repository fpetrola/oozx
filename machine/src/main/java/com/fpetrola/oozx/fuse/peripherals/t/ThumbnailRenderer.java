/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

import java.awt.Component;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ThumbnailRenderer<E> extends JLabel implements ListCellRenderer<E>
{
    public ThumbnailRenderer()
    {
        setOpaque(true);
        setHorizontalAlignment(CENTER);
        setVerticalAlignment(CENTER);
        setHorizontalTextPosition( JLabel.CENTER );
        setVerticalTextPosition( JLabel.BOTTOM );
        setBorder( new EmptyBorder(4, 4, 4, 4) );
    }

    /*
     *  Display the Thumbnail Icon and file name.
     */
    public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus)
    {
        if (isSelected)
        {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        }
         else
        {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        //Set the icon and filename

        Thumbnail thumbnail = (Thumbnail)value;
        setIcon( thumbnail.getIcon() );
        setText( thumbnail.getFile().getName() );

//      System.out.println(thumbnail.getFileName());

        return this;
    }
}