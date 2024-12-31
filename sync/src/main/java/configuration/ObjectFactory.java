package configuration;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;
























@XmlRegistry
public class ObjectFactory
{
  private static final QName _JSpeccySettings_QNAME = new QName("http://xml.netbeans.org/schema/JSpeccy", "JSpeccySettings");











  public JSpeccySettings createJSpeccySettingsType()
  {
    return new JSpeccySettings();
  }




  public TapeSettingsType createTapeSettingsType()
  {
    return new TapeSettingsType();
  }




  public KeyboardJoystickType createKeyboardJoystickType()
  {
    return new KeyboardJoystickType();
  }




  public EmulatorSettingsType createEmulatorSettingsType()
  {
    return new EmulatorSettingsType();
  }




  public RecentFilesType createRecentFilesType()
  {
    return new RecentFilesType();
  }




  public SpectrumType createSpectrumType()
  {
    return new SpectrumType();
  }




  public Interface1Type createInterface1Type()
  {
    return new Interface1Type();
  }




  public MemoryType createMemoryType()
  {
    return new MemoryType();
  }




  public AY8912Type createAY8912Type()
  {
    return new AY8912Type();
  }




  @XmlElementDecl(namespace="http://xml.netbeans.org/schema/JSpeccy", name="JSpeccySettings")
  public JAXBElement<JSpeccySettings> createJSpeccySettings(JSpeccySettings value)
  {
    return new JAXBElement(_JSpeccySettings_QNAME, JSpeccySettings.class, null, value);
  }
}

