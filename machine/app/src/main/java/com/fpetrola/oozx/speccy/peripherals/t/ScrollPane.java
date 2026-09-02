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

import java.awt.EventQueue;

import javax.swing.BoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

public class ScrollPane extends JFrame {

	private GalleryPane contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ScrollPane frame = new ScrollPane();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public ScrollPane() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		System.out.println("Frame bounds: "+getBounds());
		System.out.println("Frame insets: "+getInsets());
		createCompontents();
	}

	private void createCompontents() {
		contentPane = new GalleryPane();
		this.addComponentListener(contentPane); // resize listener to the frame
		JScrollPane scrollPane = new JScrollPane(contentPane);
		JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
		BoundedRangeModel brm = verticalScrollBar.getModel();
		contentPane.setBrm(brm);
		verticalScrollBar.addAdjustmentListener(contentPane);
		setContentPane(scrollPane);
		
	}

}
