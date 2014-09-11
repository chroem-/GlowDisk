#GlowDisk#

##What is it?##
GlowDisk is an adaptation of some code I had laying around, with the goal of serving as an integrated RAMdisk utility for the GlowStone project.  The project is licensed under the LGPLv3.

##Why use it instead of traditional RAMdisks?##
There are a number of reasons why GlowDisk makes more sense for Minecraft severs.  With RAMdisk mode enabled, GlowDisk will store files in a MappedByteBuffer that if managed by the operating system.  Because this region of memory resides outside the JVM, there is no performance overhead.  Additionally, the operating system will constantly and asynchronously back up the RAMdisk's state to a corresponding .gldsk file on the local hard drive.  Even if the program using GlowDisk unexpectedly shuts down, the operating system will make sure that the RAMdisk state is backed up to storage after it closes.  Additionally, GlowDisk is designed with the goal of being able to transparently switch between its own RAM-based filesystem and the native filesystem.  GlowDisk will (eventually, once fully implemented,) automatically detect whether or not a system is suitable for using a RAMdisk, and automatically create one.  Conversely, it will also automatically detect if the system is running low on memory and fall back to the native filesystem.

##Is it fast?##
###Yes.###
Tests so far indicate at least a 20-50x speed improvement over SSD's.  That factor should increase with the size of the files being handled.

##GlowDisk is not finished yet!##
This is just a preliminary release.

###TODO###
*	Iron out some kinks in the file tree
*	Persist file tree structure to disk
*	Allow chaining of MappedByteBuffers for RAMdisks larger than 2GB
*	Add support for random access IO (FileChannel and RandomAccessFile)
*	Finish the IO wrapper classes
*	QA testing and code cleanup
