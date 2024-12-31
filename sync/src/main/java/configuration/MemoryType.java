package configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


















































@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="MemoryType", propOrder={"romsDirectory", "rom48K", "rom128K0", "rom128K1", "romPlus20", "romPlus21", "romPlus30", "romPlus31", "romPlus32", "romPlus33", "romMF1", "romMF128", "romMFPlus3", "romIF1"})
public class MemoryType
{

  @XmlElement(name="RomsDirectory", required=true)
  protected String romsDirectory;

  @XmlElement(name="Rom48k", required=true, defaultValue="spectrum.rom")
  protected String rom48K;

  @XmlElement(name="Rom128k0", required=true, defaultValue="128-0.rom")
  protected String rom128K0;

  @XmlElement(name="Rom128k1", required=true, defaultValue="128-1.rom")
  protected String rom128K1;

  @XmlElement(name="RomPlus20", required=true, defaultValue="plus2-0.rom")
  protected String romPlus20;

  @XmlElement(name="RomPlus21", required=true, defaultValue="plus2-1.rom")
  protected String romPlus21;

  @XmlElement(name="RomPlus30", required=true, defaultValue="plus3-0.rom")
  protected String romPlus30;

  @XmlElement(name="RomPlus31", required=true, defaultValue="plus3-1.rom")
  protected String romPlus31;

  @XmlElement(name="RomPlus32", required=true, defaultValue="plus3-2.rom")
  protected String romPlus32;

  @XmlElement(name="RomPlus33", required=true, defaultValue="plus3-3.rom")
  protected String romPlus33;

  @XmlElement(name="RomMF1", required=true, defaultValue="mf1.rom")
  protected String romMF1;

  @XmlElement(name="RomMF128", required=true, defaultValue="mf128.rom")
  protected String romMF128;

  @XmlElement(name="RomMFPlus3", required=true, defaultValue="mfplus3.rom")
  protected String romMFPlus3;

  @XmlElement(name="RomIF1", required=true, defaultValue="if1.rom")
  protected String romIF1;

  public String getRomsDirectory()
  {
    return this.romsDirectory;
  }








  public void setRomsDirectory(String value)
  {
    this.romsDirectory = value;
  }








  public String getRom48K()
  {
    return this.rom48K;
  }








  public void setRom48K(String value)
  {
    this.rom48K = value;
  }








  public String getRom128K0()
  {
    return this.rom128K0;
  }








  public void setRom128K0(String value)
  {
    this.rom128K0 = value;
  }








  public String getRom128K1()
  {
    return this.rom128K1;
  }








  public void setRom128K1(String value)
  {
    this.rom128K1 = value;
  }








  public String getRomPlus20()
  {
    return this.romPlus20;
  }








  public void setRomPlus20(String value)
  {
    this.romPlus20 = value;
  }








  public String getRomPlus21()
  {
    return this.romPlus21;
  }








  public void setRomPlus21(String value)
  {
    this.romPlus21 = value;
  }








  public String getRomPlus30()
  {
    return this.romPlus30;
  }








  public void setRomPlus30(String value)
  {
    this.romPlus30 = value;
  }








  public String getRomPlus31()
  {
    return this.romPlus31;
  }








  public void setRomPlus31(String value)
  {
    this.romPlus31 = value;
  }








  public String getRomPlus32()
  {
    return this.romPlus32;
  }








  public void setRomPlus32(String value)
  {
    this.romPlus32 = value;
  }








  public String getRomPlus33()
  {
    return this.romPlus33;
  }








  public void setRomPlus33(String value)
  {
    this.romPlus33 = value;
  }








  public String getRomMF1()
  {
    return this.romMF1;
  }








  public void setRomMF1(String value)
  {
    this.romMF1 = value;
  }








  public String getRomMF128()
  {
    return this.romMF128;
  }








  public void setRomMF128(String value)
  {
    this.romMF128 = value;
  }








  public String getRomMFPlus3()
  {
    return this.romMFPlus3;
  }








  public void setRomMFPlus3(String value)
  {
    this.romMFPlus3 = value;
  }








  public String getRomIF1()
  {
    return this.romIF1;
  }








  public void setRomIF1(String value)
  {
    this.romIF1 = value;
  }
}
