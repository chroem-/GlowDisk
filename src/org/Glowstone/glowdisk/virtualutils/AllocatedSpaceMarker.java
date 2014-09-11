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

package org.Glowstone.glowdisk.virtualutils;

import java.util.ArrayList;



public class AllocatedSpaceMarker {
	int beginIndex;
	int endIndex;
	
	//boolean flexible;
	
	public final VirtualFile parent;
	public  FreeSpaceMarker followingUnallocatedZone;
	
	private ArrayList<FreeSpaceMarker> emptySpace;
	
	//unused
	private AllocatedSpaceMarker(){
		this.parent = null;
		this.followingUnallocatedZone = null;
	}
	
	private AllocatedSpaceMarker(int beginIndex, int endIndex, VirtualFile parent) {
		this.beginIndex = beginIndex;
		this.endIndex = endIndex;
		this.parent = parent;
		emptySpace = parent.getContainingDisk().emptySpace;
	}
	
	//will try to make an allocated zone of the desired size, but not necessarily
	//returns the size of the zone actually allocated
	protected static int addNewAllocatedZoneToParent(VirtualFile parent, int desiredSize) {
		VirtualDisk disk = parent.getContainingDisk();
		
		boolean ignoreReservedSpace =  ((float) disk.getReservedSpace() / (float) disk.getFreeSpace()) > .9; //more than 90% of free space is reserved
		FreeSpaceMarker best = null;
		int size = 0;
		int beginIndex;
		if (ignoreReservedSpace) {
			//find largest unallocated region
			for (FreeSpaceMarker marker : disk.emptySpace) {
				if (marker.getSize() > size) {
					best = marker;
					size = marker.getSize();
				}
			}
			beginIndex = best.leftBound + 1;
		} else {
			//find largest unreserved region
			for (FreeSpaceMarker marker : disk.emptySpace) {
				if (marker.getUnreserved() > size) {
					best = marker;
					size = marker.getUnreserved();
				}
			}
			beginIndex = best.leftBound + 1 + best.getReserved();
		}
		
		int newSegmentSize = desiredSize;
		if (best.rightBound - beginIndex < newSegmentSize) {
			newSegmentSize = best.rightBound - beginIndex;
		}
		AllocatedSpaceMarker allocatedMarker = new AllocatedSpaceMarker(beginIndex, beginIndex + newSegmentSize, parent);
		FreeSpaceMarker marker = VirtualDisk.allocateSpaceWithReserved(beginIndex, beginIndex + newSegmentSize, allocatedMarker);
		allocatedMarker.setFollowingUnallocatedZone(marker);
		parent.dataSegments.add(allocatedMarker);
		return allocatedMarker.getSize();
	}

	protected int getSize() {
		return endIndex - beginIndex;
	}
	
	protected void setFollowingUnallocatedZone(FreeSpaceMarker marker) {
		this.followingUnallocatedZone = marker;
	}

	
	protected boolean deallocate() {
		int index = emptySpace.indexOf(followingUnallocatedZone);
		FreeSpaceMarker leftMarker = emptySpace.get(index - 1);
		if (leftMarker.rightBound + 1 == beginIndex && followingUnallocatedZone.leftBound - 1 == endIndex) {
			emptySpace.remove(index);
			leftMarker.rightBound = followingUnallocatedZone.rightBound;
			return true;
		}
		return false;
	}
	
	//extend a data record segment
	protected boolean grow(int sizeToGrow) {
		///////////////////////////////////////////////////////////////////
		if (followingUnallocatedZone.leftBound == endIndex + 1 && sizeToGrow < followingUnallocatedZone.getSize()) {
			followingUnallocatedZone.leftBound += sizeToGrow;
			this.endIndex += sizeToGrow;
			return true;
		}
		return false;
		///////////////////////////////////////////////////////////////////
	}
	
	//shorten a data record segment
	protected boolean shrink(int sizeToShrink) {
		FreeSpaceMarker leftMarker = emptySpace.get(emptySpace.indexOf(followingUnallocatedZone) - 1);
		if (followingUnallocatedZone.leftBound == endIndex + 1 && followingUnallocatedZone.leftBound - leftMarker.rightBound > sizeToShrink) {
			followingUnallocatedZone.leftBound -= sizeToShrink;
			this.endIndex -= sizeToShrink;
			return true;
		}
		return false;
	}
	
	public boolean isFlexible() {
		return parent.getEndMarker() == this;
	}
	
	
}