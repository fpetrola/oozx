/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package z80core;

import java.util.Map;

/**
 *
 * @author jsanchez
 */
public interface MemIoOps {
    int fetchOpcode(int address);

    int peek8(int address);
    void poke8(int address, int value);
    int peek16(int address);
    void poke16(int address, int word);

    int inPort(int port);
    void outPort(int port, int value);

    void contendedStates(int address, int tstates);

    int peek82(int address);

    void poke82(int address, int value);

    int peek83(int i);
    int peek84(int i);

    Map<Integer, Integer> getLastWrittenMap();
}
