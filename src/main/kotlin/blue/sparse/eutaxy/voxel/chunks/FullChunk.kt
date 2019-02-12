package blue.sparse.eutaxy.voxel.chunks

import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.math.vectors.ints.Vector3i

class FullChunk(parent: ChunkParent, parentRelativePosition: Vector3i, size: Int, var fullVoxel: Voxel) :
	VoxelChunk(parent, parentRelativePosition, size) {

	override fun get(x: Int, y: Int, z: Int): Voxel {
		return fullVoxel
	}

	override fun set(x: Int, y: Int, z: Int, voxel: Voxel) {
		if (voxel == fullVoxel)
			return

		val replacement = if (size > 32) {
			DividedChunk(parent, parentRelativePosition, size, fullVoxel)
		} else {
			DetailedChunk(parent, parentRelativePosition, size, fullVoxel)
		}

		replacement[x, y, z] = voxel
		parent.replace(replacement)
	}
}
