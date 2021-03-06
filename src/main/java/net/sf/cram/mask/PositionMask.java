/*******************************************************************************
 * Copyright 2012 EMBL-EBI, Hinxton outstation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.sf.cram.mask;

public interface PositionMask {

	public boolean isMasked(int position);

	public int[] getMaskedPositions();

	public boolean isEmpty();

	public int getMaskedCount();

	public int getMinMaskedPosition();

	public int getMaxMaskedPosition();

	public byte[] toByteArrayUsing(byte mask, byte nonMask);
}
