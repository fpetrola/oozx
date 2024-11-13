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

package io.korhner.asciimg.image.matrix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A class for for creating mutliple tiles from an input grayscale matrix.
 */
public class TiledGrayscaleMatrix {

	/** The tiles. */
	private final List<GrayscaleMatrix> tiles;

	/** Width of a tile. */
	private final int tileWidth;

	/** Height of a tile. */
	private final int tileHeight;

	/** Number of tiles on x axis. */
	private final int tilesX;

	/** Number of tiles on y axis. */
	private final int tilesY;

	/**
	 * Instantiates a new tiled grayscale matrix.
	 *
	 * @param matrix
	 *            the source matrix
	 * @param tileWidth
	 *            the tile width
	 * @param tileHeight
	 *            the tile height
	 */
	public TiledGrayscaleMatrix(final GrayscaleMatrix matrix,
			final int tileWidth, final int tileHeight) {

		if (matrix.getWidth() < tileWidth || matrix.getHeight() < tileHeight) {
			throw new IllegalArgumentException(
					"Tile size must be smaller than original matrix!");
		}

		if (tileWidth <= 0 || tileHeight <= 0) {
			throw new IllegalArgumentException("Illegal tile size!");
		}

		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;

		// we won't allow partial tiles
		this.tilesX = matrix.getWidth() / tileWidth;
		this.tilesY = matrix.getHeight() / tileHeight;
		int roundedWidth = tilesX * tileWidth;
		int roundedHeight = tilesY * tileHeight;

		tiles = new ArrayList<GrayscaleMatrix>(roundedWidth * roundedHeight);

		// create each tile as a subregion from source matrix
		for (int i = 0; i < tilesY; i++) {
			for (int j = 0; j < tilesX; j++) {
				tiles.add(GrayscaleMatrix.createFromRegion(matrix, tileWidth,
						tileHeight, this.tileWidth * j, this.tileHeight * i));
			}
		}
	}

	/**
	 * Gets the tile at a specific index.
	 *
	 * @param index
	 *            tile index
	 * @return the tile
	 */
	public GrayscaleMatrix getTile(final int index) {
		return this.tiles.get(index);
	}

	/**
	 * Gets the number of tiles.
	 *
	 * @return the number of tiles
	 */
	public int getTileCount() {
		return this.tiles.size();
	}

	/**
	 * Gets the tile y size.
	 *
	 * @return the tile y size
	 */
	public int getTileHeight() {
		return this.tileHeight;
	}

	/**
	 * Gets the number of tiles on x axis.
	 *
	 * @return number of tiles on x axis
	 */
	public int getTilesX() {
		return this.tilesX;
	}

	/**
	 * Gets the number of tiles on y axis.
	 *
	 * @return number of tiles on y axis
	 */
	public int getTilesY() {
		return this.tilesY;
	}

	/**
	 * Gets the tile width.
	 *
	 * @return tile width
	 */
	public int getTileWidth() {
		return this.tileWidth;
	}
}
