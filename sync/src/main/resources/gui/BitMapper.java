package gui;

import tv.porst.jhexview.IDataChangedListener;
import tv.porst.jhexview.IDataProvider;

public class BitMapper
{

    private IDataProvider dataProvider;

    public BitMapper(IDataProvider dataProvider)
    {
	this.dataProvider= dataProvider;
    }

    public int getSize()
    {
	return dataProvider.getDataLength() * 8;
    }

    public boolean getBitAt(int bit)
    {
	int index= bit / 8;
	int bitPosition= (8 - bit % 8) - 1;

	return (dataProvider.getData(index, 1)[0] & (1 << bitPosition)) != 0;
    }
    
    public static void main(String[] args)
    {
	BitMapper bitMapper= new BitMapper(new IDataProvider()
	{
	    
	    @Override
	    public void setData(long offset, byte[] data)
	    {
		// TODO Auto-generated method stub
		
	    }
	    
	    @Override
	    public void removeListener(IDataChangedListener listener)
	    {
		// TODO Auto-generated method stub
		
	    }
	    
	    @Override
	    public boolean keepTrying()
	    {
		// TODO Auto-generated method stub
		return false;
	    }
	    
	    @Override
	    public boolean isEditable()
	    {
		// TODO Auto-generated method stub
		return false;
	    }
	    
	    @Override
	    public boolean hasData(long start, int length)
	    {
		// TODO Auto-generated method stub
		return false;
	    }
	    
	    @Override
	    public int getDataLength()
	    {
		// TODO Auto-generated method stub
		return 0;
	    }
	    
	    @Override
	    public byte[] getData(long offset, int length)
	    {
		return new byte[]{(byte) 0xFF};
	    }
	    
	    @Override
	    public byte[] getData()
	    {
		// TODO Auto-generated method stub
		return null;
	    }
	    
	    @Override
	    public void addListener(IDataChangedListener hexView)
	    {
		// TODO Auto-generated method stub
		
	    }
	});
	
	bitMapper.getBitAt(0);
	bitMapper.getBitAt(1);
	bitMapper.getBitAt(7);
	bitMapper.getBitAt(8);
    }

}
