/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.fpetrola.oozx.fuse.modules.tape;


/**
 *
 * @author jsanchez
 */
public interface TapeStateListener {
    public void stateChanged(final Tape.TapeState state);
}
