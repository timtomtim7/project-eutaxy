package blue.sparse.eutaxy.voxel.chunks

import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.math.vectors.ints.Vector3i
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class EmptyChunk(parent: ChunkParent, parentRelativePosition: Vector3i, size: Int) : VoxelChunk(parent, parentRelativePosition, size) {

	override fun get(index: Int): Voxel {
		return Voxel.empty
	}

	override fun set(index: Int, voxel: Voxel) {
		if(voxel.isEmpty)
			return
		val replacement = DetailedChunk(parent, parentRelativePosition, size)
		replacement[index] = voxel
		parent.replace(replacement)

	}

}
