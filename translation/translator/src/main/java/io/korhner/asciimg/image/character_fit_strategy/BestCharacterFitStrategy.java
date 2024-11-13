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

package io.korhner.asciimg.image.character_fit_strategy;

import io.korhner.asciimg.image.matrix.GrayscaleMatrix;

/**
 * Encapsulates the algorith for choosing best fit character.
 */
public interface BestCharacterFitStrategy {

	/**
	 * Returns the error between the character and tile matrices. The character
	 * with minimun error wins.
	 *
	 * @param character
	 *            the character
	 * @param tile
	 *            the tile
	 * @return error. Less values mean better fit. Least value character will be
	 *         chosen as best fit.
	 */
	float calculateError(final GrayscaleMatrix character,
			final GrayscaleMatrix tile);
}
