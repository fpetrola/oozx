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

package io.korhner.asciimg.image.converter;

import io.korhner.asciimg.image.AsciiImgCache;
import io.korhner.asciimg.image.character_fit_strategy.BestCharacterFitStrategy;
import io.korhner.asciimg.image.matrix.GrayscaleMatrix;

import java.util.Map.Entry;

/**
 * Converts ascii art to String.
 */
public class AsciiToStringConverter extends AsciiConverter<StringBuffer> {

	/**
	 * Instantiates a new ascii to string converter.
	 *
	 * @param characterCacher
	 *            the character cacher
	 * @param characterFitStrategy
	 *            the character fit strategy
	 */
	public AsciiToStringConverter(final AsciiImgCache characterCacher,
			final BestCharacterFitStrategy characterFitStrategy) {
		super(characterCacher, characterFitStrategy);
	}

	/**
	 * Creates an empty string buffer;
	 * 
	 * @see AsciiConverter#initializeOutput(int,
	 *      int)
	 */
	@Override
	protected StringBuffer initializeOutput(final int imageWidth,
			final int imageHeight) {
		return new StringBuffer();
	}

	/**
	 * @see AsciiConverter#finalizeOutput(int[],
	 *      int, int)
	 */
	@Override
	protected void finalizeOutput(final int[] sourceImagePixels,
			final int imageWidth, int imageHeight) {

	}

	/**
	 * Append choosen character to StringBuffer.
	 * 
	 * @see AsciiConverter#addCharacterToOutput(Entry,
	 *      int[], int, int, int)
	 */
	@Override
	public void addCharacterToOutput(
			final Entry<Character, GrayscaleMatrix> characterEntry,
			final int[] sourceImagePixels, final int tileX, final int tileY,
			final int imageWidth) {

		this.output.append(characterEntry.getKey());

		// append new line at the end of the row
		if ((tileX + 1)
				* this.characterCache.getCharacterImageSize().getWidth() == imageWidth) {
			this.output.append(System.lineSeparator());
		}

	}

}
