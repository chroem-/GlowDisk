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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.chroem.glowdisk.virtualutils.MemoryBackedVirtualDisk;
import net.chroem.glowdisk.virtualutils.VirtualDisk;

public class Main {

	public static void main(String[] arguments) {
		System.out.println("Setting up benchmark...");
			try {
				
				GlowFile nativeFile = new GlowFile("/home/chroem/Desktop/Glowdisk/test.mkv");
				initialize(nativeFile);
				
				GlowFile glowFile2 = new GlowFile("/home/chroem/Desktop/Glowdisk/test2.mkv");
				System.out.println("Benchmarking storage-backed GlowFile...");
				System.out.println("Completed in " + ((double) benchmark(nativeFile, glowFile2) / 1000.0) + "s");
				
				VirtualDisk disk = new MemoryBackedVirtualDisk(new File("/home/chroem/Desktop/Glowdisk/glowdisk.gldsk"), (int) (1024L * 1024L * 1024L * 1.5));
				GlowFile memoryFile = new GlowFile("test.mkv");
				glowFile2 = new GlowFile("test2.mkv");
				initialize(memoryFile);
				System.out.println("Benchmarking memory-backed GlowFile...");
				System.out.println("Completed in " + ((double) benchmark(memoryFile, glowFile2) / 1000.0) + "s");
				
				transfer(memoryFile, new FileOutputStream(new File("/home/chroem/Desktop/methuselah.mkv")));
				
			} catch (Exception e) {
				e.printStackTrace();
			} 
					
			try {
				VirtualDisk disk = new MemoryBackedVirtualDisk(new File("/home/chroem/Desktop/Glowdisk/glowdisk.gldsk"));
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	}
	
	public static void initialize(GlowFile file) {
		try {
			File interjection = new File("/home/chroem/Desktop/RequiemForMethuselah.mkv");
			InputStream input = new FileInputStream(interjection);
			OutputStream output = new GlowFileOutputStream(file);
			byte[] bytes = new byte[4096];
			int len;
			while ( (len = input.read(bytes)) > 0) {
				output.write(bytes, 0, len);
			}
			input.close();
			output.close();	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static long benchmark(GlowFile file1, GlowFile file2) {
		try {
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < 20; i++) {
				GlowFileInputStream input = new GlowFileInputStream(file1);
				GlowFileOutputStream output = new GlowFileOutputStream(file2);
				byte[] bytes = new byte[4096];
				int len;
				while ( (len = input.read(bytes)) > 0) {
					output.write(bytes, 0, len);
				}
				input.close();
				output.close();		
			}
			return System.currentTimeMillis() - startTime;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	
	public static void transfer(GlowFile file1, OutputStream output) {
		try{
				GlowFileInputStream input = new GlowFileInputStream(file1);
				byte[] bytes = new byte[4096];
				int len = 0;
				while ((len = input.read(bytes)) > 0) {
					output.write(bytes, 0, len);	
				}
				input.close();
				output.close();	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
