package configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;












































@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="JSpeccySettingsType", propOrder={"spectrumSettings", "memorySettings", "tapeSettings", "keyboardJoystickSettings", "ay8912Settings", "recentFilesSettings", "interface1Settings", "emulatorSettings"})
public class JSpeccySettings
{

  @XmlElement(name="SpectrumSettings", required=true)
  protected SpectrumType spectrumSettings;

  @XmlElement(name="MemorySettings", required=true)
  protected MemoryType memorySettings;

  @XmlElement(name="TapeSettings", required=true)
  protected TapeSettingsType tapeSettings;

  @XmlElement(name="KeyboardJoystickSettings", required=true)
  protected KeyboardJoystickType keyboardJoystickSettings;

  @XmlElement(name="AY8912Settings", required=true)
  protected AY8912Type ay8912Settings;

  @XmlElement(name="RecentFilesSettings", required=true)
  protected RecentFilesType recentFilesSettings;

  @XmlElement(name="Interface1Settings", required=true)
  protected Interface1Type interface1Settings;

  @XmlElement(name="EmulatorSettings", required=true)
  protected EmulatorSettingsType emulatorSettings;

  public SpectrumType getSpectrumSettings()
  {
    return this.spectrumSettings;
  }








  public void setSpectrumSettings(SpectrumType value)
  {
    this.spectrumSettings = value;
  }








  public MemoryType getMemorySettings()
  {
    return this.memorySettings;
  }








  public void setMemorySettings(MemoryType value)
  {
    this.memorySettings = value;
  }








  public TapeSettingsType getTapeSettings()
  {
    return this.tapeSettings;
  }








  public void setTapeSettings(TapeSettingsType value)
  {
    this.tapeSettings = value;
  }








  public KeyboardJoystickType getKeyboardJoystickSettings()
  {
    return this.keyboardJoystickSettings;
  }








  public void setKeyboardJoystickSettings(KeyboardJoystickType value)
  {
    this.keyboardJoystickSettings = value;
  }








  public AY8912Type getAY8912Settings()
  {
    return this.ay8912Settings;
  }








  public void setAY8912Settings(AY8912Type value)
  {
    this.ay8912Settings = value;
  }








  public RecentFilesType getRecentFilesSettings()
  {
    return this.recentFilesSettings;
  }








  public void setRecentFilesSettings(RecentFilesType value)
  {
    this.recentFilesSettings = value;
  }








  public Interface1Type getInterface1Settings()
  {
    return this.interface1Settings;
  }








  public void setInterface1Settings(Interface1Type value)
  {
    this.interface1Settings = value;
  }








  public EmulatorSettingsType getEmulatorSettings()
  {
    return this.emulatorSettings;
  }








  public void setEmulatorSettings(EmulatorSettingsType value)
  {
    this.emulatorSettings = value;
  }
}

