/**
 * Copyright (C) 2011-2013 Michael Vogt <michu@neophob.com>
 *
 * This file is part of PixelController.
 *
 * PixelController is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PixelController is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PixelController.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.neophob.sematrix.output;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.neophob.sematrix.output.lpd6803.Lpd6803Common;
import com.neophob.sematrix.output.lpd6803.WriteDataException;

/**
 * verify caching
 * @author michu
 *
 */
public class PixelControllerDeviceTest extends Lpd6803Common {
	
    @Test
    public void testChaching() {
    	byte[] b = new byte[64];    	
    	assertTrue(didFrameChange((byte)0, b));
    	assertFalse(didFrameChange((byte)0, b));
    	b[0]=5;
    	assertTrue(didFrameChange((byte)0, b));    	
    }

	@Override
	protected void writeData(byte[] cmdfull) throws WriteDataException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean waitForAck() {
		// TODO Auto-generated method stub
		return false;
	}
    
}
