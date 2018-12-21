package blue.sparse.eutaxy.voxel.chunks

import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.math.vectors.ints.Vector3i

class DetailedChunk(parent: ChunkParent, parentRelativePosition: Vector3i, size: Int) : VoxelChunk(parent, parentRelativePosition, size) {

	private var solidCount = 0
	private val data = IntArray(volume)

	override fun get(index: Int): Voxel {
		if (index < 0 || index >= data.size)
			return Voxel(0)

		return Voxel(data[index])
	}

	override fun set(index: Int, voxel: Voxel) {
		if (index < 0 || index >= data.size)
			return

		val newID = voxel.color
		val oldID = data[index]
		if (oldID != newID) {
			data[index] = newID

			if (voxel.isEmpty)
				solidCount--
			else
				solidCount++
		}

		if (solidCount == 0) {
			parent.replace(EmptyChunk(parent, parentRelativePosition, size))
		}
	}

}
