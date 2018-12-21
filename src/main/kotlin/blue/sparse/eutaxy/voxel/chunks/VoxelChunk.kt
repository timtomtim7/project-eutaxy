package blue.sparse.eutaxy.voxel.chunks

import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.math.vectors.ints.Vector3i

abstract class VoxelChunk(val parent: ChunkParent, parentRelativePosition: Vector3i, val size: Int): Iterable<Voxel> {

	val parentRelativePosition: Vector3i = parentRelativePosition
		get() = field.clone()

	val volume: Int = size * size * size

	abstract operator fun get(index: Int): Voxel
	abstract operator fun set(index: Int, voxel: Voxel)

	operator fun get(x: Int, y: Int, z: Int): Voxel {
		return get(getIndex(x, y, z))
	}

	operator fun set(x: Int, y: Int, z: Int, voxel: Voxel) {
		set(getIndex(x, y, z), voxel)
	}

	operator fun get(position: Vector3i): Voxel {
		return get(position.x, position.y, position.z)
	}

	operator fun set(position: Vector3i, voxel: Voxel) {
		set(position.x, position.y, position.z, voxel)
	}

	fun getIndex(x: Int, y: Int, z: Int): Int {
		if(x < 0 || x >= size || y < 0 || y >= size || z < 0 || z >= size)
			return -1
		return x + (y * size) + (z * size * size)
	}

	override fun iterator(): Iterator<Voxel> {
		return (0 until volume).asSequence().map(this::get).iterator()
	}

}