package com.fpetrola.z80.minizx.emulation;

/**
 * Alguien que quiere enterarse de cada acceso a un puerto.
 *
 * <p>Vivía en el emulador —`com.fpetrola.z80.minizx.emulation.OutListener<T>`, con el valor
 * genérico— y en `0.0.2-alu` no existe: esa rama dejó `IO` en lo mínimo, `int in(int)` y
 * `void out(int, int)`, sin mecanismo de escucha.
 *
 * <p>Se muda acá y no se pierde nada, porque el mecanismo nunca fue del emulador: quien lleva
 * las listas y las notifica es {@link com.fpetrola.z80.minizx.RZXPlayerIO}, que es de este
 * módulo. Del emulador sólo venía prestada la interfaz.
 *
 * <p>El paquete se conserva a propósito para que los que la importan no cambien —
 * `TaintReplay` la nombra completa— y porque los paquetes `com.fpetrola.z80.minizx.*` no se
 * renombran: las variantes generadas del juego los importan por nombre.
 *
 * <p>El valor es `int` y no un tipo genérico: es lo que alu mueve por los puertos, y el único
 * consumidor de esto —la atribución de puertos a rutinas del taint— ya trabajaba en enteros
 * (hacía `port.intValue() & 0xff` sobre lo que le llegaba).
 */
public interface OutListener {
  void outAt(int port, int value);
}
