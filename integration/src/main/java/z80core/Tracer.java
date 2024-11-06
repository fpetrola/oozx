package z80core;

import java.util.Arrays;

public class Tracer
{
	enum Registers8bit
	{
		A, B, C, D, E, F, H, L, I, R, IXH, IXL, IYH, IYL
	}

	enum Registers16bit
	{
		AF, BC, DE, HL, PC, SP, IX, IY
	}

	private int[][] traceMemory= new int[100000000][];

	public int getReg8BitAddressBit(Registers8bit register, int bitNumber)
	{
		return 0;
	}
	
	public int getReg8BitAddress(Registers8bit register)
	{
		return 0;
	}
	
	public int getReg16BitAddress(Registers16bit register)
	{
		return 0;
	}


	public void addTrace8Bit(int destinationAddress, int sourceAddress)
	{
		long nanoTime= System.nanoTime();

		while (System.nanoTime() - nanoTime < 60000L)
			;

		for (int i= 0; i < 8; i++)
		{
			int[] src= traceMemory[sourceAddress * 8 + i];
			int[] dest= traceMemory[destinationAddress * 8 + i];
			if (dest == null)
				src= new int[0];
			if (dest == null)
				dest= new int[0];
			int destLength= dest.length;
			dest= Arrays.copyOf(dest, destLength + src.length);
			System.arraycopy(src, 0, dest, destLength, src.length);
		}
	}

	public void addTrace16Bit(int destinationAddress, int sourceAddress)
	{
	}

	public void addTraceRotateLeft8Bit(int getregAAddress)
	{
		// TODO Auto-generated method stub

	}

	public int getCarryFlagAddress()
	{
		return 0;
	}

	public void addTraceBit(int dest, int source)
	{
		// TODO Auto-generated method stub

	}

	public void addTraceRotateRight8Bit(int reg8BitAddress)
	{
		// TODO Auto-generated method stub
		
	}

	public void addTraceRotateLeft8BitUsingCarry(int reg8BitAddress)
	{
		// TODO Auto-generated method stub
		
	}

	public void addTraceRotateRight8BitUsingCarry(int reg8BitAddress)
	{
		// TODO Auto-generated method stub
		
	}
}
