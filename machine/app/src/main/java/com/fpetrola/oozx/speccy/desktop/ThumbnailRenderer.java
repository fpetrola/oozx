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