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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import net.chroem.glowdisk.virtualutils.AllocatedSpaceMarker;


public class VirtualFile {
	protected ArrayList<AllocatedSpaceMarker> dataSegments = new ArrayList<AllocatedSpaceMarker>();
	private ArrayList<VirtualFile> children = new ArrayList<VirtualFile>();
	private VirtualFile parent;
	private String name;
	
	private VirtualDisk containingDisk;
	
	private final boolean isRoot;
	
	private final boolean isDirectory;
	protected boolean hasData = false;
	
	protected boolean deleted = false;
	private boolean deleteOnExit = false;
	
	private long lastModified = System.currentTimeMillis();
	private boolean writeable = true;
	
	private final String path;
	protected int dataSize = 0;
	
	
	//deprecated
	private VirtualFile(String path, boolean isDirectory) throws IllegalArgumentException, FileNotFoundException {
		if (path == null) throw new IllegalArgumentException("Path cannot be null!"); 
		this.path = path;
		String[] pathSegments = path.split(File.separator);
		//String rootIdentifier = pathSegments[(pathSegments[0].equals("")) ? 1 : 0];
		String[] parentIdentifiers = Arrays.copyOfRange(pathSegments, ((pathSegments[0].equals("")) ? 1 : 0), pathSegments.length - 1);
		this.name = pathSegments[pathSegments.length - 1];
		this.containingDisk = VirtualDisk.getPrimaryDisk();
		this.parent = (parentIdentifiers.length < 1) ? containingDisk.getRoot() : containingDisk.getRoot().getChild(parentIdentifiers);
		this.isRoot = false;
		
		this.isDirectory = isDirectory;
		parent.addChild(this);
	}
	
	public VirtualFile(VirtualFile parent, String name, boolean isDirectory) throws IllegalArgumentException {
		if (parent.getPath() != null) {
		this.path = (parent.getPath().endsWith(File.separator)) ? parent.getPath() + name : parent.getPath() + File.separator + name; 
		} else {
			this.path = File.separator + name;
		}
			
		this.parent = parent;
		this.name = name;
		this.isDirectory = isDirectory;
		this.isRoot = false;
		if (parent == null || name == null) {
			throw new IllegalArgumentException("Neither parent nor name can be null!");
		}
		
		parent.addChild(this);
		this.containingDisk = parent.getContainingDisk();
	}
	
	private VirtualFile(VirtualDisk disk) {
		//this.path = disk.getRootPath();
		//this.name = disk.getRootPath();
		this.isRoot = true;
		this.path = "";
		this.isDirectory = true;
		this.containingDisk = disk; 
	}
	
	//unused
	private VirtualFile() {
		this.path = null;
		this.name = null;
		this.isDirectory = false;
		this.isRoot = false;
	}

	//given its own method to imply use with caution
	protected static VirtualFile generateRoot(VirtualDisk containingDisk) {
		return new VirtualFile(containingDisk);
	}
	
	public String getPath() {
		return this.path;
	}
	
	public int getDataSize() {
		return dataSize;
	}
	
	public void delete() {
		this.deleted = true;
		if (isDirectory) {
			for (VirtualFile file : children) {
				file.delete();
			}
		} else {
			for (AllocatedSpaceMarker marker : dataSegments) {
				marker.deallocate();
			}
		}
		parent.removeChild(this);
	}
	
	public void addChild(VirtualFile file) {
		children.add(file);
	}
	
	protected void removeChild(VirtualFile file) {
		children.remove(file);
	}
	
	public String getName() {
		return this.name;
	}
	
	//update later to use an index to avoid making tons of array copies
	public VirtualFile getChild(String[] path) throws FileNotFoundException {
		//String name = path[0];
		if (path.length == 0 /*path.length == 1 && !isRoot &&this.getName().equals(name)*/ ) { //!!!!!!!!!!!!!!!!!!!!!
			return this;
		} else {
			for (VirtualFile file : children) {
				if (file.getName().equals(path[0])) {
					return file.getChild(Arrays.copyOfRange(path, 1, path.length));
				}
			}
		}
		throw new FileNotFoundException();
	}
	
	public VirtualFile getParent() {
		return this.parent;
	}
	
	public VirtualDisk getContainingDisk() {
		return this.containingDisk;
	}
	
	public  boolean changeSize(int sizeToChange) throws IllegalArgumentException {
		/*System.out.println("Allocated size: " + getAllocatedSize());
		System.out.println("Disk free space: " + this.getContainingDisk().getFreeSpace());
		System.out.println("Size to change: " + sizeToChange);
		*/
		if (sizeToChange > this.getContainingDisk().getFreeSpace() || getAllocatedSize() + sizeToChange < 0) throw new IllegalArgumentException("Cannot grow the file larger than the disk or shrink its size below zero!");
		if (this.hasData && sizeToChange > 0) {
			////////////////////////////////////////////////////////
			AllocatedSpaceMarker endMarker = getEndMarker();
			int freeSpace = endMarker.followingUnallocatedZone.getSize() - 1;
			if (sizeToChange > freeSpace) {
				endMarker.grow(freeSpace);
				int sizeAllocated = AllocatedSpaceMarker.addNewAllocatedZoneToParent(this, sizeToChange);
				return (sizeToChange > (freeSpace + sizeAllocated)) ? changeSize(sizeToChange - (freeSpace + sizeAllocated)) : true;
			} else {
				endMarker.grow(sizeToChange);
				return true;
			}
			////////////////////////////////////////////////////////
		} else if (this.hasData && sizeToChange < 0) {
			AllocatedSpaceMarker endMarker = getEndMarker();
			int size = endMarker.getSize();
			if (size + sizeToChange < 0) {
				endMarker.deallocate();
				return changeSize(sizeToChange + size);
			} else {
				endMarker.shrink(sizeToChange * -1);
				return true;
			}
		} else if (sizeToChange > 0){
			
			int newZoneSize = AllocatedSpaceMarker.addNewAllocatedZoneToParent(this, sizeToChange);
			this.hasData = true;
			if (newZoneSize < sizeToChange) {
				
				return changeSize(sizeToChange - newZoneSize);
			} else {
				return true;
			}
		
		}
		return false;
	}
	
	
	
	protected AllocatedSpaceMarker getEndMarker() {
		return dataSegments.get(dataSegments.size() - 1);
	}
	
	protected int getAllocatedSize() {
		int size = 0;
		for (AllocatedSpaceMarker marker : dataSegments) {
			size += marker.getSize();
		}
		return size;
	}
	
	public boolean isDirectory(){
		return this.isDirectory;
	}
	
	public VirtualFile[] getChildren() {
		return (VirtualFile[]) this.children.toArray();
	}
	
	protected FilePathMarker getLastValidChild(String[] path) {
		return getLastValidChild(new FilePathMarker(path));
	}
	
	private FilePathMarker getLastValidChild(FilePathMarker marker) {
		if (marker.index + 1 == marker.path.length) {
			marker.tail = this;
			return marker;
		}
		for (VirtualFile child : children) {
			if (child.name.equals(marker.path[marker.index])) {
				marker.index++;
				return child.getLastValidChild(marker);
			}
		}
		marker.tail = this;
		return marker;
	}
	
	protected static class FilePathMarker {
		final String[] path;
		int index;
		VirtualFile tail;
		
		public FilePathMarker(String[] path) {
			this.path = path;
			this.index = 0;
		}
		
	}
	
	protected boolean isRoot() {
		return this.isRoot;
	}
	
	protected VirtualFile addChildDirectories(String[] path, int index) {
		VirtualFile file = new VirtualFile(this, path[index], true);
		if (index + 1 == path.length) {
			return file;
		} else {
			return file.addChildDirectories(path , index + 1);
		}
	}
	
	public void deleteOnExit() {
		if (!deleteOnExit) {
			containingDisk.toBeDeleted.add(this);
		}
		this.deleteOnExit = true;
	}
	
	public long getLastModified() {
		return this.lastModified;
	}
	
	public void setLastModified(long time) {
		this.lastModified = time;
	}
	
	public boolean getWriteable() {
		return this.writeable;
	}
	
	public void setWriteable(boolean writeable) {
		this.writeable = writeable;
	}
	
	
	
	
	
	
}
