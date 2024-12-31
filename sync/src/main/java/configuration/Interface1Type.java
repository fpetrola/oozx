package configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


















































@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="Interface1Type", propOrder={"connectedIF1", "microdriveUnits", "cartridgeSize"})
public class Interface1Type
{

  @XmlElement(defaultValue="false")
  protected boolean connectedIF1;

  @XmlElement(defaultValue="8")
  protected byte microdriveUnits;

  @XmlElement(defaultValue="180")
  protected int cartridgeSize;

  public boolean isConnectedIF1()
  {
    return this.connectedIF1;
  }




  public void setConnectedIF1(boolean value)
  {
    this.connectedIF1 = value;
  }




  public byte getMicrodriveUnits()
  {
    return this.microdriveUnits;
  }




  public void setMicrodriveUnits(byte value)
  {
    this.microdriveUnits = value;
  }




  public int getCartridgeSize()
  {
    return this.cartridgeSize;
  }




  public void setCartridgeSize(int value)
  {
    this.cartridgeSize = value;
  }
}

