package configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;











































@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="RecentFilesType", propOrder={"recentFile0", "recentFile1", "recentFile2", "recentFile3", "recentFile4", "lastSnapshotDir", "lastTapeDir"})
public class RecentFilesType
{

  @XmlElement(required=true)
  protected String recentFile0;

  @XmlElement(required=true)
  protected String recentFile1;

  @XmlElement(required=true)
  protected String recentFile2;

  @XmlElement(required=true)
  protected String recentFile3;

  @XmlElement(required=true)
  protected String recentFile4;

  @XmlElement(required=true)
  protected String lastSnapshotDir;

  @XmlElement(required=true)
  protected String lastTapeDir;

  public String getRecentFile0()
  {
    return this.recentFile0;
  }








  public void setRecentFile0(String value)
  {
    this.recentFile0 = value;
  }








  public String getRecentFile1()
  {
    return this.recentFile1;
  }








  public void setRecentFile1(String value)
  {
    this.recentFile1 = value;
  }








  public String getRecentFile2()
  {
    return this.recentFile2;
  }








  public void setRecentFile2(String value)
  {
    this.recentFile2 = value;
  }








  public String getRecentFile3()
  {
    return this.recentFile3;
  }








  public void setRecentFile3(String value)
  {
    this.recentFile3 = value;
  }








  public String getRecentFile4()
  {
    return this.recentFile4;
  }








  public void setRecentFile4(String value)
  {
    this.recentFile4 = value;
  }








  public String getLastSnapshotDir()
  {
    return this.lastSnapshotDir;
  }








  public void setLastSnapshotDir(String value)
  {
    this.lastSnapshotDir = value;
  }








  public String getLastTapeDir()
  {
    return this.lastTapeDir;
  }








  public void setLastTapeDir(String value)
  {
    this.lastTapeDir = value;
  }
}


