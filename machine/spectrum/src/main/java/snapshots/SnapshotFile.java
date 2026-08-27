/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package snapshots;

import java.io.File;

/**
 *
 * @author jsanchez
 */
public interface SnapshotFile {
    SpectrumState load(File filename) throws SnapshotException;
    boolean save(File filename, SpectrumState state) throws SnapshotException;
}
