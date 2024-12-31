package z80core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class ZXLogger
{

    static int counter;
    public static Writer writer;
    private static String name;
 
    private static Writer initWriter()
    {
	try
	{
	    return new FileWriter(new File(name));
	}
	catch (IOException e)
	{
	    throw new RuntimeException(e);
	}
    }

    public static void write(CharSequence string)
    {
	if (writer != null)
	    try
	    {
		if (counter < 5000)
		{
		    counter++;
		    writer.append(string);
		    writer.flush();
		}
	    }
	    catch (IOException e)
	    {
		throw new RuntimeException(e);
	    }
    }

    public static void logOpCode(int opCode)
    {
	write("opCode: " + opCode + "\n");
    }

    public static void logState(String stepName, Z80 z80)
    {
//	int[] registers= new int[] { z80.getRegAF().toInt(), z80.getRegBC().toInt(), z80.getRegDE().toInt(), z80.getRegHL().toInt(), z80.getRegIX().toInt(), z80.getRegIY().toInt(), z80.getRegPC().toInt(), z80.getRegSP().toInt() };
//
//	write("registers: " + Arrays.toString(registers) + " (" + stepName + ")\n");
    }

    public static void reset(String name, Z80 z80)
    {
	ZXLogger.name= name;
	writer= initWriter();
	logState("reset", z80);
    }
}
