/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.fpetrola.oozx;

import com.google.inject.Singleton;

import com.fpetrola.oozx.speccy.modules.Joystick;

@Singleton
public class Settings {

  public void setString(String startMachine, String id) {

  }

  public class SettingsInfo {
    public boolean accelerateLoader;
    public boolean aspectHint;
    public boolean autoLoad;
    public boolean autosaveSettings;
    public boolean beta128;
    public boolean beta12848Boot;
    public String betadiskFile;
    public boolean bwTv;
    public boolean competitionCode;
    public boolean competitionMode;
    public boolean confirmActions;
    public boolean covox;
    public String dckFile;
    public String debuggerCommand;
    public boolean detectLoader;
    public boolean didaktik80;
    public String didaktik80diskFile;
    public boolean disciple;
    public String disciplediskFile;
    public boolean diskAskMerge;
    public String diskTryMerge;
    public boolean divideEnabled;
    public String divideMasterFile;
    public String divideSlaveFile;
    public boolean divideWp;
    public boolean divmmcEnabled;
    public String divmmcFile;
    public boolean divmmcWp;
    public boolean doublescanMode;
    public int drive40MaxTrack = 42;
    public int drive80MaxTrack = 83;
    public String driveBeta128aType;
    public String driveBeta128bType;
    public String driveBeta128cType;
    public String driveBeta128dType;
    public String driveDidaktik80aType;
    public String driveDidaktik80bType;
    public String driveDisciple1Type;
    public String driveDisciple2Type;
    public String driveOpus1Type;
    public String driveOpus2Type;
    public String drivePlus3aType;
    public String drivePlus3bType;
    public String drivePlusd1Type;
    public String drivePlusd2Type;
    public boolean embedSnapshot;
    public int emulationSpeed = 20000;
    public boolean fastload;
    public int fbMode;
    public int frameRate;
    public boolean fullScreen;
    public boolean fuller;
    public String if2File;
    public boolean interface1;
    public boolean interface2;
    public boolean issue2;
    public boolean joyKempston;
    public boolean joyPrompt;
    public String joystick1;
    public int joystick1Fire1;
    public int joystick1Fire10;
    public int joystick1Fire11;
    public int joystick1Fire12;
    public int joystick1Fire13;
    public int joystick1Fire14;
    public int joystick1Fire15;
    public int joystick1Fire2;
    public int joystick1Fire3;
    public int joystick1Fire4;
    public int joystick1Fire5;
    public int joystick1Fire6;
    public int joystick1Fire7;
    public int joystick1Fire8;
    public int joystick1Fire9;
    public Joystick.JoystickType joystick1Output;
    public String joystick2;
    public int joystick2Fire1;
    public int joystick2Fire10;
    public int joystick2Fire11;
    public int joystick2Fire12;
    public int joystick2Fire13;
    public int joystick2Fire14;
    public int joystick2Fire15;
    public int joystick2Fire2;
    public int joystick2Fire3;
    public int joystick2Fire4;
    public int joystick2Fire5;
    public int joystick2Fire6;
    public int joystick2Fire7;
    public int joystick2Fire8;
    public int joystick2Fire9;
    public Joystick.JoystickType joystick2Output;
    public int joystickKeyboardDown;
    public int joystickKeyboardFire;
    public int joystickKeyboardLeft;
    public Joystick.JoystickType joystickKeyboardOutput;
    public int joystickKeyboardRight;
    public int joystickKeyboardUp;
    public boolean kempstonMouse;
    public boolean keyboardArrowsShifted;
    public boolean lateTimings;
    public String mdrFile;
    public String mdrFile2;
    public String mdrFile3;
    public String mdrFile4;
    public String mdrFile5;
    public String mdrFile6;
    public String mdrFile7;
    public String mdrFile8;
    public int mdrLen = 180;
    public boolean mdrRandomLen = true;
    public boolean melodik;
    public boolean mouseSwapButtons;
    public String movieCompr;
    public String movieStart;
    public boolean movieStopAfterRzx;
    public boolean multiface1;
    public boolean multiface128;
    public boolean multiface1Stealth;
    public boolean multiface3;
    public boolean opus;
    public String opusdiskFile;
    public boolean palTv2x;
    public String phantomTypistMode;
    public String playbackFile;
    public boolean plus3DetectSpeedlock;
    public String plus3diskFile;
    public boolean plusd;
    public String plusddiskFile;
    public boolean printer;
    public String printerGraphicsFilename;
    public String printerTextFilename;
    public boolean rawSNet;
    public String recordFile;
    public boolean recreatedSpectrum;
    public String rom1280 = "128-0.rom";
    public String rom1281 = "128-1.rom";
    public String rom16;
    public String rom48 = "48.rom";
    public String romBeta128 = "trdos.rom";
    public String romDidaktik80 = "didaktik80.rom";
    public String romDisciple = "disciple.rom";
    public String romDivide;
    public String romInterface1 = "if1-2.rom";
    public String romMultiface1 = "mf1.rom";
    public String romMultiface128 = "mf128.rom";
    public String romMultiface3 = "mf3.rom";
    public String romOpus = "opus.rom";
    public String romPentagon10240;
    public String romPentagon10241;
    public String romPentagon10242;
    public String romPentagon10243;
    public String romPentagon5120;
    public String romPentagon5121;
    public String romPentagon5122;
    public String romPentagon5123;
    // Standing in until the Pentagon's own ROMs are here: it is a 128 clone and boots on these.
    // Its third ROM is TR-DOS, which needs the disk side that does not exist yet.
    public String romPentagon0 = "128-0.rom";
    public String romPentagon1 = "128-1.rom";
    public String romPentagon2 = "trdos.rom";
    public String romPlus20 = "plus2-0.rom";
    public String romPlus21 = "plus2-1.rom";
    public String romPlus2a0 = "plus3-0.rom";
    public String romPlus2a1 = "plus3-1.rom";
    public String romPlus2a2 = "plus3-2.rom";
    public String romPlus2a3 = "plus3-3.rom";
    public String romPlus30 = "plus3-0.rom";
    public String romPlus31 = "plus3-1.rom";
    public String romPlus32 = "plus3-2.rom";
    public String romPlus33 = "plus3-3.rom";
    public String romPlus3e0 = "plus3e-0.rom";
    public String romPlus3e1 = "plus3e-1.rom";
    public String romPlus3e2 = "plus3e-2.rom";
    public String romPlus3e3 = "plus3e-3.rom";
    public String romPlusd = "plusd.rom";
    public String romScorpion0;
    public String romScorpion1;
    public String romScorpion2;
    public String romScorpion3;
    public String romSpecSe0;
    public String romSpecSe1;
    public String romSpeccyboot;
    public String romTc2048;
    public String romTc20680;
    public String romTc20681;
    public String romTs20680;
    public String romTs20681;
    public String romTtx2000s;
    public String romUsource;
    public boolean rs232Handshake;
    public String rs232Rx;
    public String rs232Tx;
    public boolean rzxAutosaves;
    public boolean rzxCompression;
    public String sdlFullscreenMode;
    public boolean simpleideActive;
    public String simpleideMasterFile;
    public String simpleideSlaveFile;
    public boolean sltTraps;
    public String snapshot;
    public String snet;
    public boolean sound = true;
    public String soundDevice = "buffer=8192,frames=4,verbose";
    public boolean soundForce8bit;
    public int soundFreq = 44100;
    public boolean soundLoad= true;
    public String speakerType;
    public boolean speccyboot;
    public String speccybootTap;
    public boolean specdrum;
    public boolean spectranet;
    public boolean spectranetDisable;
    public String startMachine;
    public String startScalerMode;
    public boolean statusbar;
    public String stereoAy;
    public boolean strictAspectHint;
    public String svgaModes;
    public String tapeFile;
    public boolean tapeTraps;
    public String teletextAddr1;
    public String teletextAddr2;
    public String teletextAddr3;
    public String teletextAddr4;
    public int teletextPort1;
    public int teletextPort2;
    public int teletextPort3;
    public int teletextPort4;
    public boolean ttx2000s;
    public boolean unittests;
    public boolean usource;
    public int volumeAy;
    public int volumeBeeper;
    public int volumeCovox = 100;
    public int volumeSpecdrum = 100;
    public boolean writableRoms;
    public boolean z80IsCmos;
    public boolean zxataspActive;
    public String zxataspMasterFile;
    public String zxataspSlaveFile;
    public boolean zxataspUpload;
    public boolean zxataspWp;
    public boolean zxcfActive;
    public String zxcfPriFile;
    public boolean zxcfUpload;
    public boolean zxmmcEnabled;
    public String zxmmcFile;
    public boolean zxprinter;
    public int volumeAY;

    public int getSoundFreq() {
      return soundFreq;
    }
  }

  public SettingsInfo current = new SettingsInfo();
  public SettingsInfo defaults = new SettingsInfo();

  public final int SETTINGS_ROM_COUNT = 30;

  public void defaults(SettingsInfo settings) {
    throw new UnsupportedOperationException("settings_defaults not implemented");
  }

  public void copy(SettingsInfo dest, SettingsInfo src) {
    throw new UnsupportedOperationException("settings_copy not implemented");
  }

  public String[] getRomSetting(SettingsInfo settings, int which, boolean isPeripheral) {
    throw new UnsupportedOperationException("settings_get_rom_setting not implemented");
  }

  public void setString(String[] stringSetting, String value) {
    throw new UnsupportedOperationException("settings_set_string not implemented");
  }

  public int free(SettingsInfo settings) {
    throw new UnsupportedOperationException("settings_free not implemented");
  }

  public int writeConfig(SettingsInfo settings) {
    throw new UnsupportedOperationException("settings_write_config not implemented");
  }

}