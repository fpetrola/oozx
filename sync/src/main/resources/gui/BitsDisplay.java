package gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class BitsDisplay extends javax.swing.JComponent
{
    private BufferedImage tvImage;
   
    public BitsDisplay()
    {
	initComponents();

	Dimension screenSize= new Dimension(1500, 1500);
	setMaximumSize(screenSize);
	setMinimumSize(screenSize);
	setPreferredSize(screenSize);
    }

    public void paintComponent(Graphics gc)
    {
	Graphics2D gc2= (Graphics2D) gc;
    }

    private void initComponents()
    {
	setDoubleBuffered(false);
    }
}
