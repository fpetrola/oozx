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

import java.io.*;
import java.util.concurrent.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ThumbnailApp
{
    private DefaultListModel<Thumbnail> model = new DefaultListModel<Thumbnail>();
    private JList<Thumbnail> list = new JList<Thumbnail>(model);
    private Set<File> filesToBeLoaded = new HashSet<>();
    private ExecutorService service;

    public ThumbnailApp()
    {
        int processors = Runtime.getRuntime().availableProcessors();
        service = Executors.newFixedThreadPool( processors - 2 );
    }

    public JPanel createContentPane()
    {
        JPanel cp = new JPanel( new BorderLayout() );

        list.setCellRenderer( new ThumbnailRenderer<Thumbnail>() );
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(-1);
        Icon empty = new EmptyIcon(160, 160);
        Thumbnail prototype = new Thumbnail(new File("PortugalSpain-000.JPG"), empty);
        list.setPrototypeCellValue( prototype );

        JScrollPane scrollPane = new JScrollPane( list );
        cp.add(scrollPane, BorderLayout.CENTER);

        scrollPane.getViewport().addChangeListener((e) ->
        {
            int first = list.getFirstVisibleIndex();
            int last = list.getLastVisibleIndex();
            System.out.println(first + " : " + last);

            if (first == -1) return;

            for (int i = first; i <= last; i++)
            {
                Thumbnail thumbnail = model.elementAt(i);
                File file = thumbnail.getFile();

                if (filesToBeLoaded.contains(file))
                {
                    filesToBeLoaded.remove(file);
                    service.submit( new ThumbnailWorker(thumbnail.getFile(), model, i) );
                }
            }

            if (filesToBeLoaded.isEmpty())
                service.shutdown();
        });

        return cp;
    }

    public void loadImages(File directory)
    {
        new Thread( () -> createThumbnails(directory) ).start();
    }

    private void createThumbnails(File directory)
    {
        try
        {
            File[] files = directory.listFiles((d, f) -> {return f.endsWith(".jpg");});

            for (File file: files)
            {
                filesToBeLoaded.add( file );
                Thumbnail thumbnail = new Thumbnail(file, null);
                model.addElement( thumbnail );
            }
        }
        catch(Exception e) { e.printStackTrace(); }
    }

    private static void createAndShowGUI()
    {
        ThumbnailApp app = new ThumbnailApp();

        JFrame frame = new JFrame("ListDrop");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane( app.createContentPane() );
        frame.setSize(1600, 900);
        frame.setVisible(true);

//      File directory = new File("C:/Users/netro/Pictures/TravelSun/2019_01_Cuba");
        File directory = new File("/home/fernando/Pictures/");
        app.loadImages( directory );
    }
    public static void main(String[] args)
    {
        javax.swing.SwingUtilities.invokeLater(() -> createAndShowGUI());
    }
}