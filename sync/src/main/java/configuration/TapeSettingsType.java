package configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;







































@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="TapeSettingsType", propOrder={"enableLoadTraps", "accelerateLoading", "enableSaveTraps", "highSamplingFreq", "flashLoad", "autoLoadTape", "invertedEar"})
public class TapeSettingsType
{

  @XmlElement(defaultValue="true")
  protected boolean enableLoadTraps;

  @XmlElement(defaultValue="true")
  protected boolean accelerateLoading;

  @XmlElement(defaultValue="true")
  protected boolean enableSaveTraps;

  @XmlElement(defaultValue="false")
  protected boolean highSamplingFreq;

  @XmlElement(defaultValue="false")
  protected boolean flashLoad;

  @XmlElement(defaultValue="true")
  protected boolean autoLoadTape;

  @XmlElement(defaultValue="false")
  protected boolean invertedEar;

  public boolean isEnableLoadTraps()
  {
    return this.enableLoadTraps;
  }




  public void setEnableLoadTraps(boolean value)
  {
    this.enableLoadTraps = value;
  }




  public boolean isAccelerateLoading()
  {
    return this.accelerateLoading;
  }




  public void setAccelerateLoading(boolean value)
  {
    this.accelerateLoading = value;
  }




  public boolean isEnableSaveTraps()
  {
    return this.enableSaveTraps;
  }




  public void setEnableSaveTraps(boolean value)
  {
    this.enableSaveTraps = value;
  }




  public boolean isHighSamplingFreq()
  {
    return this.highSamplingFreq;
  }




  public void setHighSamplingFreq(boolean value)
  {
    this.highSamplingFreq = value;
  }




  public boolean isFlashLoad()
  {
    return this.flashLoad;
  }




  public void setFlashLoad(boolean value)
  {
    this.flashLoad = value;
  }




  public boolean isAutoLoadTape()
  {
    return this.autoLoadTape;
  }




  public void setAutoLoadTape(boolean value)
  {
    this.autoLoadTape = value;
  }




  public boolean isInvertedEar()
  {
    return this.invertedEar;
  }




  public void setInvertedEar(boolean value)
  {
    this.invertedEar = value;
  }
}


