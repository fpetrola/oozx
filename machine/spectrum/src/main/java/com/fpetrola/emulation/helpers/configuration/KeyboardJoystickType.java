/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.emulation.helpers.configuration;

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

