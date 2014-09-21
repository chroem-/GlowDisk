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

package net.chroem.glowdisk.virtualutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import net.chroem.glowdisk.virtualutils.AllocatedSpaceMarker;
import net.chroem.glowdisk.virtualutils.FreeSpaceMarker;
import net.chroem.glowdisk.virtualutils.VirtualFile.FilePathMarker;


public abstract class VirtualDisk {
	
	
	//private final String rootPath;
	@Expose private final int size;
	
	protected final ArrayList<VirtualFile> toBeDeleted = new ArrayList<VirtualFile>();
	
	private static VirtualDisk primaryDisk;
	@Expose private final VirtualFile root;
	protected final VirtualFile manifest;
	
	protected ArrayList<FreeSpaceMarker> emptySpace = new ArrayList<FreeSpaceMarker>();	
	
	public VirtualDisk(int size) {
		//this.rootPath = root.replace(File.separator, "");
		VirtualDisk.primaryDisk = this;
		this.size = size;
		this.root = VirtualFile.generateRoot(this);
		this.manifest = VirtualFile.generateManifest();
		emptySpace.add(FreeSpaceMarker.generateRoot(this));
		Runtime.getRuntime().addShutdownHook(new Thread() {
			
			@Override
			public void run() {
				for (int i = toBeDeleted.size() - 1; i > -1; i--) {
					VirtualFile file = toBeDeleted.get(i);
					if (!file.deleted) {
						file.delete();
					}
				}
				updateFileManifest();
			}
			
		});
		
	}
	
	public abstract OutputStream getOutputStream(VirtualFile file) throws IOException;
	public abstract InputStream getInputStream(VirtualFile file);
	protected abstract void updateFileManifest();
	

	public static boolean isEnabled() {
		return VirtualDisk.primaryDisk != null;
	}
	
	public static VirtualDisk getPrimaryDisk() {
		return VirtualDisk.primaryDisk;
	}
	
	public int getFreeSpace() {
		int freeSpace = 0;
		for (FreeSpaceMarker marker : emptySpace) {
			freeSpace += marker.getSize();
		}
		return freeSpace;
	}
	
	public int getReservedSpace() {
		int reservedSpace = 0;
		for (FreeSpaceMarker marker : emptySpace) {
			reservedSpace += marker.getReserved();
		}
		return reservedSpace;
	}
	
	public int getUnreservedSpace() {
		return getFreeSpace() - getReservedSpace();
	}
	
	public int getSize() {
		return this.size;
	}
	
	//recursively allocate!
	
	
	//add a data record segment
	private FreeSpaceMarker allocateSpace(int startIndex, int endIndex) {
		return allocateSpaceWithReserved(startIndex, endIndex, null);
	}
	
	protected static FreeSpaceMarker allocateSpaceWithReserved(int startIndex, int endIndex, AllocatedSpaceMarker allocatedMarker) {
		VirtualDisk disk = allocatedMarker.parent.getContainingDisk();
		for (int i = 0; i < disk.emptySpace.size(); i++) {
			FreeSpaceMarker marker = disk.emptySpace.get(i);
			if (marker.leftBound < startIndex && marker.rightBound > endIndex) {
				int oldRightBound = marker.rightBound;
				marker.rightBound = startIndex - 1;
				FreeSpaceMarker newMarker = new FreeSpaceMarker(endIndex + 1, oldRightBound, allocatedMarker);
				disk.emptySpace.add(i + 1, newMarker);
				return newMarker;
			}
		}
		return null;
	}
	

	protected int getDefaultSegmentSize() {
		return getFreeSpace() / 10  ;
	}
	
	public VirtualFile getRoot() {
		return this.root;
	}
	
	public VirtualFile mkdirs(String path) {
		String[] pathSegments = path.split(File.separator);

		if (pathSegments[0].equals("")) {
			pathSegments = Arrays.copyOfRange(pathSegments, 1, pathSegments.length);
		}

		if (pathSegments[pathSegments.length - 1].equals("")) {
			pathSegments = Arrays.copyOfRange(pathSegments, 0, pathSegments.length - 1);
		}
		FilePathMarker marker = this.root.getLastValidChild(pathSegments);

		return marker.tail.addChildDirectories(pathSegments, marker.index);
	}
		
}

	

