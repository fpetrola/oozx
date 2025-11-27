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

import java.util.Random;

public class Fdd {

    public  final int FDD_SHUGART = 1;
    private  Settings settings;

  public Fdd(Settings settings) {
    this.settings = settings;
  }

  // Enums
    public enum FddWrite {
        FDD_READ,
        FDD_WRITE
    }

    public enum FddDir {
        FDD_STEP_OUT,
        FDD_STEP_IN
    }

    // Error messages
    public  final String[] FDD_ERROR = {
        "OK",
        "invalid disk geometry",
        "read only disk",
        "disk not exist (disabled)",
        "unknown error code"
    };

    // FDD parameters
    public  final FddParams[] FDD_PARAMS = {
        new FddParams(0, 0, 0), // Disabled
        new FddParams(1, 1, 40), // Single-sided 40 track
        new FddParams(1, 2, 40), // Double-sided 80 track
        new FddParams(1, 1, 80), // Single-sided 40 track
        new FddParams(1, 2, 80)  // Double-sided 80 track
    };

    // FDD structure fields
    int fddHeads;           // Number of heads (0, 1, or 2)
    int fddCylinders;       // Number of cylinders
    int cHead;              // Current head (0 or 1)
    int cCylinder;          // Current cylinder
    int upsidedown;         // Disk orientation (0 or 1)
    int unreadable;         // Unreadable disk flag
    int loaded;             // Disk loaded flag
    int autoGeom;           // Auto-detect geometry flag
    int selected;           // Drive selected flag
    int dskchg;             // Disk change flag
    int hdout;              // High-density output flag
    int ready;              // Drive ready flag
    int doReadWeak;         // Read weak sectors flag
    int index;              // Index pulse flag
    int tr00;               // Track 0 flag
    int wrprot;             // Write protect flag
    int type;               // Drive type (FDD_SHUGART, FDD_TYPE_NONE)
    int motoron;            // Motor on flag
    int loadhead;           // Head load flag
    int cBpt;               // Bytes per track
    UPDFdc fdc;             // Associated FDC
    Runnable fdcIndex;      // FDC index callback
    public Disk disk= new Disk();              // Disk data
    int data;               // Current data byte (with flags)
    int marks;              // Data marks (FM, weak)

    //  fields
    private  int fddMotor = 0; // To manage 'disk' icon
    private  int motorEvent;
    private  int indexEvent;
    private  final Random random = new Random();

    // Initialize FDD events
    public  int initEvents(Object context) {
//        motorEvent = Event.register(Fdd::fddEvent, "FDD motor on");
//        indexEvent = Event.register(Fdd::fddEvent, "FDD index");
//
//        UPDFdc.initEvents();
//        WDFdc.initEvents();

        return 0;
    }

    // Register startup
    public  void registerStartup() {
//        StartupManager.Module[] dependencies = {
//            StartupManager.Module.STARTUP_MANAGER_MODULE_EVENT,
//            StartupManager.Module.STARTUP_MANAGER_MODULE_SETUID
//        };
//        StartupManager.register(StartupManager.Module.STARTUP_MANAGER_MODULE_FDD,
//                                dependencies, Fdd::initEvents, null, null);
    }

    // Get error string
    public  String strerror(int error) {
        if (error >= FddConstants.FDD_LAST_ERROR) {
            error = FddConstants.FDD_LAST_ERROR - 1;
        }
        return FDD_ERROR[error];
    }

    // Set disk data
    private  void setData(Fdd d, int fact) {
        int head = d.upsidedown != 0 ? 1 - d.cHead : d.cHead;

        if (d.loaded == 0) {
            return;
        }

        if (d.unreadable != 0 || (d.disk.sides == 1 && head == 1) ||
            d.cCylinder >= d.disk.cylinders) {
            d.disk.track = null;
            d.disk.clocks = null;
            d.disk.fm = null;
            d.disk.weak = null;
            return;
        }

        Disk.setTrack(d.disk, head, d.cCylinder);
        d.cBpt = (d.disk.track[-3] & 0xFF) + 256 * (d.disk.track[-2] & 0xFF);
        if (fact > 0) {
            // Triangular distribution skip in bytes (±10%)
            d.disk.i += d.cBpt / fact + d.cBpt * (random.nextInt(10) + random.nextInt(10) - 9) / fact / 100;
            while (d.disk.i >= d.cBpt) {
                d.disk.i -= d.cBpt;
            }
        }
        d.index = d.disk.i != 0 ? 0 : 1;
    }

    // Initialize FDD
    public  int init(Fdd d, int type, FddParams dt, boolean reinit) {
        int upsidedown = d.upsidedown;
        int loaded = d.loaded;
        int selected = d.selected;
        int doReadWeak = d.doReadWeak;
        if (dt == null) {
            dt = FDD_PARAMS[0];
        }

        d.fddHeads = d.fddCylinders = d.cHead = d.cCylinder = 0;
        d.upsidedown = d.unreadable = d.loaded = d.autoGeom = d.selected = 0;
        d.dskchg = d.hdout = d.ready = d.doReadWeak = 0;
        if (type == FddConstants.FDD_TYPE_NONE) {
            d.index = d.tr00 = d.wrprot = 0;
        } else {
            d.index = d.tr00 = d.wrprot = 1;
        }
        d.type = type;
        d.fdcIndex = null;
        d.fdc = null;

        if (dt.heads < 0 || dt.heads > 2 || dt.cylinders < 0 || dt.cylinders > FddConstants.FDD_MAX_TRACK) {
            return d.status = FddConstants.FDD_GEOM;
        }

        if (dt.heads == 0) {
            d.autoGeom = 1;
        }
        d.fddHeads = dt.heads;
        d.fddCylinders = dt.cylinders == 80 ? settings.current.drive80MaxTrack : settings.current.drive40MaxTrack;
        if (reinit) {
            d.selected = selected;
            d.doReadWeak = doReadWeak;
        } else {
            unload(d);
        }
        if (reinit && loaded != 0) {
            unload(d);
            load(d, upsidedown != 0);
        } else {
            d.disk.data = null;
        }

        return d.status = FddConstants.FDD_OK;
    }

    // Turn motor on/off
    public  void motorOn(Fdd d, boolean on) {
        if (d.loaded == 0) {
            return;
        }
        int onValue = on ? 1 : 0;
        if (d.motoron == onValue) {
            return;
        }
        d.motoron = onValue;
        fddMotor += on ? 1 : -1;
//        UI.statusbarUpdate(Ui.Constants.UI_STATUSBAR_ITEM_DISK,
//                           fddMotor > 0 ? UIConstants.UI_STATUSBAR_STATE_ACTIVE : UIConstants.UI_STATUSBAR_STATE_INACTIVE);
//
//        Event.removeTypeUserData(motorEvent, d);
//        if (on) {
//            Event.addWithData(MachineCurrent.getInstance().timings.processorSpeed / 10 * 4, motorEvent, d);
//            if (d.loaded != 0) {
//                Event.addWithData(MachineCurrent.getInstance().timings.processorSpeed / 1000 * (d.indexPulse ? 10 : 190), indexEvent, d);
//            }
//        } else {
//            Event.addWithData(MachineCurrent.getInstance().timings.processorSpeed / 10 * 3, motorEvent, d);
//        }
    }

    // Load/unload head
    public  void headLoad(Fdd d, boolean load) {
        if (d.loaded == 0) {
            return;
        }
        int loadValue = load ? 1 : 0;
        if (d.loadhead == loadValue) {
            return;
        }
        d.loadhead = loadValue;
        setData(d, FddConstants.FDD_HEAD_FACT);
    }

    // Select drive
    public  void select(Fdd d, boolean select) {
        d.selected = select ? 1 : 0;
        if (d.type == FddConstants.FDD_SHUGART) {
            headLoad(d, d.selected != 0);
        }
    }

    // Load disk into FDD
    public  int load(Fdd d, boolean upsidedown) {
        if (d.type == FddConstants.FDD_TYPE_NONE) {
            return d.status = FddConstants.FDD_NONE;
        }

        if (d.disk.sides < 0 || d.disk.sides > 2 || d.disk.cylinders < 0 || d.disk.cylinders > FddConstants.FDD_MAX_TRACK) {
            return d.status = FddConstants.FDD_GEOM;
        }

        if (d.autoGeom != 0) {
            d.fddHeads = d.disk.sides;
            d.fddCylinders = d.disk.cylinders > settings.current.drive40MaxTrack ?
                settings.current.drive80MaxTrack : settings.current.drive40MaxTrack;
        }

        if (d.disk.cylinders > d.fddCylinders + FddConstants.FDD_TRACK_TRESHOLD) {
            d.unreadable = 1;
//            UI.error(UIConstants.UI_ERROR_WARNING,
//                     "This %d track disk image is incompatible with the configured %d track drive. Use disk options to select a compatible drive.",
//                     d.disk.cylinders, d.fddCylinders);
        }

        d.upsidedown = upsidedown ? 1 : 0;
        d.wrprot = d.disk.wrprot;
        d.loaded = 1;
        if (d.type == FddConstants.FDD_SHUGART && d.selected != 0) {
            headLoad(d, true);
        }

        d.doReadWeak = d.disk.haveWeak;
        setData(d, FddConstants.FDD_LOAD_FACT);
        d.ready = (d.motoron != 0 && d.loaded != 0) ? 1 : 0;
//        if (d.disk.density == DiskConstants.DISK_HD) {
//            d.hdout = 1;
//        }

        return d.status = FddConstants.FDD_OK;
    }

    // Unload disk from FDD
    public  void unload(Fdd d) {
        d.ready = d.loaded = d.dskchg = d.hdout = 0;
        d.index = d.wrprot = 1;
        motorOn(d, false);
        if (d.type == FddConstants.FDD_SHUGART && d.selected != 0) {
            headLoad(d, false);
        }
    }

    // Set current head
    public  void setHead(Fdd d, int head) {
        if (d.fddHeads == 1) {
            return;
        }
        head = head > 0 ? 1 : 0;
        if (d.cHead == head) {
            return;
        }
        d.cHead = head;
        setData(d, 0);
    }

    // Step to next/previous track
    public  void step(Fdd d, FddDir direction) {
        if (direction == FddDir.FDD_STEP_OUT) {
            if (d.cCylinder > 0) {
                d.cCylinder--;
            }
        } else {
            if (d.cCylinder < d.fddCylinders - 1) {
                d.cCylinder++;
            }
        }
        d.tr00 = d.cCylinder == 0 ? 1 : 0;
        setData(d, FddConstants.FDD_STEP_FACT);
        if (d.loaded != 0 && d.selected != 0) {
            d.dskchg = 1;
        }
    }

    // Read/write next byte from/to sector
    private  int readWriteData(Fdd d, FddWrite write) {
        if (d.selected == 0 || d.ready == 0 || d.loadhead == 0 || d.disk.track == null) {
            if (d.loaded != 0 && d.motoron != 0) {
                if (d.disk.i >= d.cBpt) {
                    d.disk.i = 0;
                }
                if (write == FddWrite.FDD_READ) {
                    d.data = 0x100;
                }
                d.disk.i++;
                d.index = d.disk.i >= d.cBpt ? 1 : 0;
            }
            return d.status = FddConstants.FDD_OK;
        }

        if (d.disk.i >= d.cBpt) {
            d.disk.i = 0;
        }
        if (write == FddWrite.FDD_WRITE) {
            if (d.disk.wrprot != 0) {
                d.disk.i++;
                d.index = d.disk.i >= d.cBpt ? 1 : 0;
                return d.status = FddConstants.FDD_RDONLY;
            }
            d.disk.track[d.disk.i] = (byte) (d.data & 0x00FF);
//            if ((d.data & 0xFF00) != 0) {
//                Bitmap.set(d.disk.clocks, d.disk.i);
//            } else {
//                Bitmap.reset(d.disk.clocks, d.disk.i);
//            }
//            if ((d.marks & 0x01) != 0) {
//                Bitmap.set(d.disk.fm, d.disk.i);
//            } else {
//                Bitmap.reset(d.disk.fm, d.disk.i);
//            }
//            Bitmap.reset(d.disk.weak, d.disk.i);
            d.disk.dirty = 1;
        } else {
            d.data = d.disk.track[d.disk.i] & 0xFF;
//            if (Bitmap.test(d.disk.clocks, d.disk.i)) {
//                d.data |= 0xFF00;
//            }
//            d.marks = 0;
//            if (Bitmap.test(d.disk.fm, d.disk.i)) {
//                d.marks |= 0x01;
//            }
//            if (Bitmap.test(d.disk.weak, d.disk.i)) {
//                d.marks |= 0x02;
//                d.data = (d.data & random.nextInt(0xFF)) | random.nextInt(0xFF);
//            }
        }
        d.disk.i++;
        d.index = d.disk.i >= d.cBpt ? 1 : 0;

        return d.status = FddConstants.FDD_OK;
    }

    // Read next byte from sector
    public  int readData(Fdd d) {
        return readWriteData(d, FddWrite.FDD_READ);
    }

    // Write next byte to sector
    public  int writeData(Fdd d) {
        return readWriteData(d, FddWrite.FDD_WRITE);
    }

    // Flip disk
    public  void flip(Fdd d, boolean upsidedown) {
        if (d.loaded == 0) {
            return;
        }
        d.upsidedown = upsidedown ? 1 : 0;
        setData(d, FddConstants.FDD_LOAD_FACT);
    }

    // Set write protect
    public  void wrprot(Fdd d, boolean wrprot) {
        if (d.loaded == 0) {
            return;
        }
        d.wrprot = d.disk.wrprot = wrprot ? 1 : 0;
    }

    // Wait for index hole
    public  void waitIndexHole(Fdd d) {
        if (d.selected == 0 || d.ready == 0) {
            return;
        }
        d.disk.i = 0;
        d.index = 1;
    }

    // Handle FDD events
    private  void fddEvent(long lastTstates, int event, Object userData) {
        Fdd d = (Fdd) userData;

        if (event == motorEvent) {
            d.ready = (d.motoron != 0 && d.loaded != 0) ? 1 : 0;
            return;
        }

        d.indexPulse = !d.indexPulse;
        if (!d.indexPulse && d.fdc != null) {
            d.fdcIndex.run();
            d.fdc = null;
        }

//        if (d.motoron != 0 && d.loaded != 0) {
//            Event.addWithData(lastTstates + MachineCurrent.getInstance().timings.processorSpeed / 1000 * (d.indexPulse ? 10 : 190), indexEvent, d);
//        }
    }

    // Additional field for index pulse
    boolean indexPulse;
    int status; // FDD status
}