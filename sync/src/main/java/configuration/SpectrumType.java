package configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


































































































@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="SpectrumType", propOrder={"ayEnabled48K", "mutedSound", "loadingNoise", "ulAplus", "defaultModel", "framesInt", "zoomed", "zoom", "multifaceEnabled", "mf128On48K", "hifiSound", "hibernateMode", "lecEnabled", "emulate128KBug", "zoomMethod", "filterMethod", "scanLines", "borderSize"})
public class SpectrumType
{

  @XmlElement(name="AYEnabled48k", defaultValue="false")
  protected boolean ayEnabled48K;

  @XmlElement(defaultValue="false")
  protected boolean mutedSound;

  @XmlElement(defaultValue="true")
  protected boolean loadingNoise;

  @XmlElement(name="ULAplus", defaultValue="false")
  protected boolean ulAplus;

  @XmlElement(defaultValue="1")
  protected int defaultModel;

  @XmlElement(defaultValue="2")
  protected int framesInt;

  @XmlElement(defaultValue="false")
  protected boolean zoomed;

  @XmlElement(defaultValue="2")
  protected int zoom;

  @XmlElement(defaultValue="false")
  protected boolean multifaceEnabled;

  @XmlElement(name="mf128on48K", defaultValue="false")
  protected boolean mf128On48K;

  @XmlElement(defaultValue="false")
  protected boolean hifiSound;

  @XmlElement(defaultValue="false")
  protected boolean hibernateMode;

  @XmlElement(defaultValue="false")
  protected boolean lecEnabled;

  @XmlElement(name="emulate128kBug", defaultValue="false")
  protected boolean emulate128KBug;

  @XmlElement(defaultValue="0")
  protected int zoomMethod;

  @XmlElement(defaultValue="0")
  protected int filterMethod;

  @XmlElement(defaultValue="false")
  protected boolean scanLines;

  @XmlElement(defaultValue="1")
  protected int borderSize;

  public boolean isAYEnabled48K()
  {
    return this.ayEnabled48K;
  }




  public void setAYEnabled48K(boolean value)
  {
    this.ayEnabled48K = value;
  }




  public boolean isMutedSound()
  {
    return this.mutedSound;
  }




  public void setMutedSound(boolean value)
  {
    this.mutedSound = value;
  }




  public boolean isLoadingNoise()
  {
    return this.loadingNoise;
  }




  public void setLoadingNoise(boolean value)
  {
    this.loadingNoise = value;
  }




  public boolean isULAplus()
  {
    return this.ulAplus;
  }




  public void setULAplus(boolean value)
  {
    this.ulAplus = value;
  }




  public int getDefaultModel()
  {
    return this.defaultModel;
  }




  public void setDefaultModel(int value)
  {
    this.defaultModel = value;
  }




  public int getFramesInt()
  {
    return this.framesInt;
  }




  public void setFramesInt(int value)
  {
    this.framesInt = value;
  }




  public boolean isZoomed()
  {
    return this.zoomed;
  }




  public void setZoomed(boolean value)
  {
    this.zoomed = value;
  }




  public int getZoom()
  {
    return this.zoom;
  }




  public void setZoom(int value)
  {
    this.zoom = value;
  }




  public boolean isMultifaceEnabled()
  {
    return this.multifaceEnabled;
  }




  public void setMultifaceEnabled(boolean value)
  {
    this.multifaceEnabled = value;
  }




  public boolean isMf128On48K()
  {
    return this.mf128On48K;
  }




  public void setMf128On48K(boolean value)
  {
    this.mf128On48K = value;
  }




  public boolean isHifiSound()
  {
    return this.hifiSound;
  }




  public void setHifiSound(boolean value)
  {
    this.hifiSound = value;
  }




  public boolean isHibernateMode()
  {
    return this.hibernateMode;
  }




  public void setHibernateMode(boolean value)
  {
    this.hibernateMode = value;
  }




  public boolean isLecEnabled()
  {
    return this.lecEnabled;
  }




  public void setLecEnabled(boolean value)
  {
    this.lecEnabled = value;
  }




  public boolean isEmulate128KBug()
  {
    return this.emulate128KBug;
  }




  public void setEmulate128KBug(boolean value)
  {
    this.emulate128KBug = value;
  }




  public int getZoomMethod()
  {
    return this.zoomMethod;
  }




  public void setZoomMethod(int value)
  {
    this.zoomMethod = value;
  }




  public int getFilterMethod()
  {
    return this.filterMethod;
  }




  public void setFilterMethod(int value)
  {
    this.filterMethod = value;
  }




  public boolean isScanLines()
  {
    return this.scanLines;
  }




  public void setScanLines(boolean value)
  {
    this.scanLines = value;
  }




  public int getBorderSize()
  {
    return this.borderSize;
  }




  public void setBorderSize(int value)
  {
    this.borderSize = value;
  }
}

