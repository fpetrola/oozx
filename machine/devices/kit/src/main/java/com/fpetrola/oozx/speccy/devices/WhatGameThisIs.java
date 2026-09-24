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


package com.fpetrola.oozx.speccy.devices;

import dev.crystal.plugins.api.RoleInterface;

/**
 * Quien sabe de juegos: como se llama el que esta cargado y que se sabe de el.
 * <p>
 * El escritorio no lo sabe ni tiene por que. Abre archivos y arma maquinas; que un archivo sea
 * un juego publicado en 1984, con su caratula y su editor, es cosa de un catalogo, y un
 * catalogo es un jar que puede no estar. Sin nadie que conteste, el boton lo dice en vez de
 * abrir una ventana vacia.
 */
@RoleInterface
public interface WhatGameThisIs {

  /**
   * Que juego es el de ese archivo, con el nombre y el numero que le da el catalogo, o nada
   * cuando no se lo reconoce. Por el numero se encuentran los pokes de ese juego y no los de
   * otro que se llama igual.
   */
  default Desk.Game gameIn(String file) {
    return null;
  }

  /** Muestra lo que se sabe de ese juego, buscandolo por lo que traiga: su id, su nombre o su archivo. */
  void show(Desk.Game game);
}
