package com.fpetrola.oozx.fuse.peripherals.t;

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
