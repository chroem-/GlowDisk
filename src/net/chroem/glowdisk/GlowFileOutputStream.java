/**
 * 
    This file is a part of the GlowDisk utility.
    Copyright (C) 2014 chroem

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package net.chroem.glowdisk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.util.Arrays;

import net.chroem.glowdisk.virtualutils.AllocatedSpaceMarker;
import net.chroem.glowdisk.virtualutils.MemoryBackedVirtualDisk;
import net.chroem.glowdisk.virtualutils.VirtualFile;

//dumby wrapper class just for the purpose of being similar to the Java API
public class GlowFileOutputStream extends OutputStream{

	private final OutputStream out;
	
	//unused
	private GlowFileOutputStream(){
		this.out = null;
	}
	
	public GlowFileOutputStream(GlowFile file) throws FileNotFoundException, IOException {
		this.out = file.getOutputStream();
	}
	
	@Override
	public void write(int b) throws IOException {
		out.write(b);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		out.write(b);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException{
		out.write(b, off, len);
	}
	
	@Override 
	public void flush() throws IOException {	
		out.flush();
	}
	
	@Override
	public void close() throws IOException {
		out.close();
	}
	
}
