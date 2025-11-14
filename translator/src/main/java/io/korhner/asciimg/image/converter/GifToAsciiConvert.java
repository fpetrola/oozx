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
import io.korhner.asciimg.utils.AnimatedGifEncoder;
import io.korhner.asciimg.utils.GifDecoder;

public class GifToAsciiConvert extends AsciiToImageConverter{

	public GifToAsciiConvert(AsciiImgCache characterCacher,
			BestCharacterFitStrategy characterFitStrategy) {
		super(characterCacher, characterFitStrategy);
	}
	
	/**
	 * 
	 * @param srcFilePath
	 * @param disFilePath
	 * @param delay－－the delay time(ms) between each frame
	 * @param repeat－－he number of times the set of GIF frames should be played.0 means play indefinitely. 
	 * @return
	 */
	public int  convertGitToAscii(String srcFilePath,String disFilePath,int delay,int repeat){
		GifDecoder decoder = new GifDecoder();
		int status = decoder.read(srcFilePath);
		if(status!=0){
			return -1;//srcfile not exist or open failed!
		}
		AnimatedGifEncoder e = new AnimatedGifEncoder();
		boolean openStatus = e.start(disFilePath);
		if(openStatus){
			e.setDelay(delay);   // 1 frame per delay(ms)
			e.setRepeat(repeat);
			// initialize converters
			int frameCount = decoder.getFrameCount();
			for(int i=0;i<frameCount;i++){
				//convert per frame
				e.addFrame(this.convertImage(decoder.getFrame(i)));
			}
			e.finish();
			return 1;//done!
		}
		return 0;//open disfile failed!
	}

}
