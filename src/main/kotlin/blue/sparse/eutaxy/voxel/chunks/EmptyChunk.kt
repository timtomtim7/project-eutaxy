package blue.sparse.eutaxy.voxel.chunks

import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.math.vectors.ints.Vector3i

class EmptyChunk(parent: ChunkParent, parentRelativePosition: Vector3i, size: Int) :
	VoxelChunk(parent, parentRelativePosition, size) {

//	init {
//		println("Created empty chunk")
//	}

	override fun get(x: Int, y: Int, z: Int): Voxel {
		return Voxel.empty
	}

	override fun set(x: Int, y: Int, z: Int, voxel: Voxel) {
		if (voxel.isEmpty)
			return

//		val replacement = DetailedChunk(parent, parentRelativePosition, size)
		val replacement = if (size > 32) {
			DividedChunk(parent, parentRelativePosition, size)
		} else {
			DetailedChunk(parent, parentRelativePosition, size)
		}

		replacement[x, y, z] = voxel
		parent.replace(replacement)
	}
}
