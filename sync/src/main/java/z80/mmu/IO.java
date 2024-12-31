package z80.mmu;

public interface IO<T> {

    /**
     * Read 8-bit data from the given port
     *
     * @param port port to read the data
     * @return value available at the port
     */
    T in(T port);

    /**
     * Write 8-bit data into given port
     *
     * @param port target port
     * @param value to be written
     */
    void out(T port, T value);
}
