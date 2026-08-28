package com.fpetrola.z80.minizx.emulation;

import com.fpetrola.z80.registers.RRegister;

/**
 * El registro R que AVISA cuando lo incrementan.
 *
 * <p>Repone una conducta que `0.0.2-alu` perdió, y de la que depende que una grabación RZX
 * avance. El reproductor no cuenta el tiempo en ciclos ni mira el PC: cuenta **fetches**, y los
 * cuenta escuchando el incremento del registro R, que el Z80 sube una vez por cada opcode
 * leído. {@code DefaultEmulator} engancha ahí su {@code fetchCounter} y de ese número sale
 * cuándo termina un frame y cuándo entra la interrupción.
 *
 * <p>En el fork, {@code Plain8BitRegister.increment()} hacía {@code incrementing(data)} antes de
 * sumar, así que el oyente se enteraba. En alu ese método sigue notificando igual —pero
 * {@link RRegister} lo SOBRESCRIBE para manejar el bit 7 y en el camino se le cae el aviso:
 *
 * <pre>
 *   public void increment() { //TODO: revisar regRBit7
 *     data = (data + 1 &amp; 0x7f) | regRBit7;
 *   }
 * </pre>
 *
 * <p>Y como {@code DefaultFetchNextOpcodeInstruction} llama a {@code registerR.increment()} en
 * cada fetch, el contador se queda en cero: la grabación no avanza de frame, la corrida no
 * termina nunca y lo que se ve desde afuera es un OutOfMemory a los dos minutos. Compila
 * perfecto — esto no lo encuentra el compilador, lo encuentra el gate.
 *
 * <p>Se repone acá y no se parchea alu: el emulador es una dependencia externa y esto es lo que
 * ESTE repo necesita de él. El bit 7 se sigue manejando como alu quiere; lo único que se agrega
 * es el aviso.
 */
public class ObservableRRegister extends RRegister {

  @Override
  public void increment() {
    int antes = data;
    super.increment();
    // el fork avisaba ANTES de sumar y con el valor viejo; incrementing() le suma uno al
    // notificar, así que el oyente recibe el mismo número por los dos caminos. De todos modos
    // quien escucha sólo mira la bandera isIncrement.
    incrementing(antes);
  }
}
