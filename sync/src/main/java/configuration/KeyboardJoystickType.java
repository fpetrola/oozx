package configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;










































@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="KeyboardJoystickType", propOrder={"joystickModel", "mapPCKeys", "issue2"})
public class KeyboardJoystickType
{

  @XmlElement(name="JoystickModel", defaultValue="0")
  protected int joystickModel;

  @XmlElement(defaultValue="false")
  protected boolean mapPCKeys;

  @XmlElement(name="Issue2", defaultValue="false")
  protected boolean issue2;

  public int getJoystickModel()
  {
    return this.joystickModel;
  }




  public void setJoystickModel(int value)
  {
    this.joystickModel = value;
  }




  public boolean isMapPCKeys()
  {
    return this.mapPCKeys;
  }




  public void setMapPCKeys(boolean value)
  {
    this.mapPCKeys = value;
  }




  public boolean isIssue2()
  {
    return this.issue2;
  }




  public void setIssue2(boolean value)
  {
    this.issue2 = value;
  }
}

