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

public class FreeSpaceMarker {
	protected int leftBound;
	protected int rightBound;
	
	private final VirtualDisk containingDisk;
	
	AllocatedSpaceMarker precedingDataSegment;
	
	/*public FreeSpaceMarker(int leftIndex, int rightIndex) {
		this.leftBound = leftIndex;
	}*/
	
	protected FreeSpaceMarker(int leftIndex, int rightIndex, AllocatedSpaceMarker reservedFor) {
		this.leftBound = leftIndex;
		this.rightBound = rightIndex;
		precedingDataSegment = reservedFor;
		containingDisk = precedingDataSegment.parent.getContainingDisk();
	}
	
	private FreeSpaceMarker(int leftIndex, int rightIndex, VirtualDisk containingDisk) {
		this.leftBound = leftIndex;
		this.rightBound = rightIndex;
		this.containingDisk = containingDisk;
	}
	
	protected static FreeSpaceMarker generateRoot(VirtualDisk containingDisk) {
		return new FreeSpaceMarker(0, containingDisk.getSize() - 1, containingDisk);
	}
	
	public int getSize() {
		return rightBound - leftBound;
	}
	
	public int getReserved() {
		if (containsReservedZone()) {
			int reservedSize = (int) (( (4.0 * (double) precedingDataSegment.parent.getAllocatedSize() * (double) containingDisk.getFreeSpace())) / ((double) containingDisk.getSize()));
			int zoneSize = getSize();
			return ((float) reservedSize / (float) zoneSize < .5) ? reservedSize : zoneSize;
		} else {
			return 0;
		}
	}
	
	public int getUnreserved() {
		return getSize() - getReserved();
	}
	
	public boolean containsReservedZone() {
		boolean containsReservedZone = precedingDataSegment != null;
		if (containsReservedZone) {
			containsReservedZone = precedingDataSegment.isFlexible();
		}
		return containsReservedZone;
	}
	
}