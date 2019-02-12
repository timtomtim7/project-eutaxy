package blue.sparse.eutaxy.voxel.chunks

import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.math.vectors.ints.Vector3i

class DetailedChunk(parent: ChunkParent, parentRelativePosition: Vector3i, size: Int, fill: Voxel = Voxel.empty)
	: VoxelChunk(parent, parentRelativePosition, size) {

	private var solidCount = 0
	private val data = IntArray(volume) { fill.color }

	override operator fun get(x: Int, y: Int, z: Int): Voxel {
		return get(getIndex(x, y, z))
	}

	override operator fun set(x: Int, y: Int, z: Int, voxel: Voxel) {
		set(getIndex(x, y, z), voxel)
	}

	operator fun get(index: Int): Voxel {
		if (index < 0 || index >= data.size)
			return Voxel(0)

		return Voxel(data[index])
	}

	operator fun set(index: Int, voxel: Voxel) {
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

	fun getIndex(x: Int, y: Int, z: Int): Int {
		if(x < 0 || x >= size || y < 0 || y >= size || z < 0 || z >= size)
			return -1
		return x + (y * size) + (z * size * size)
	}

}
