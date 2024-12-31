package gui;

import java.awt.Point;

public class Test1
{
    public static void main(String[] args)
    {
	int i= 32;
	int j= 1;
	Point point= getPixelCoordinates(i, j);
	System.out.println("x: " + point.x + " - y:" + point.y);

    }

    public static Point getPixelCoordinates(int i, int j)
    {
	int x= (i & 0x1F) * 8 + (j % 8);
	int row= ((i >> 5) & 0x07) | ((i >> 8) & 0xF8);
	int line= ((i >> 8) & 0x07);
	int y= (row * 8) + line;
	
	return new Point(x, y);
    }
}