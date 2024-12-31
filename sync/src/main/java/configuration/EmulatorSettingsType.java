package configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name= "EmulatorSettingsType", propOrder= { "confirmActions", "autosaveConfigOnExit" })
public class EmulatorSettingsType
{

    @XmlElement(defaultValue= "true")
    protected boolean confirmActions;

    @XmlElement(defaultValue= "false")
    protected boolean autosaveConfigOnExit;

    public boolean isConfirmActions()
    {
	return this.confirmActions;
    }

    public void setConfirmActions(boolean value)
    {
	this.confirmActions= value;
    }

    public boolean isAutosaveConfigOnExit()
    {
	return this.autosaveConfigOnExit;
    }

    public void setAutosaveConfigOnExit(boolean value)
    {
	this.autosaveConfigOnExit= value;
    }
}
