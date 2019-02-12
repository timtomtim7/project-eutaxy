package blue.sparse.eutaxy.voxel.chunks

import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.math.vectors.ints.Vector3i

abstract class VoxelChunk(val parent: ChunkParent, parentRelativePosition: Vector3i, val size: Int): Iterable<Voxel> {

	val parentRelativePosition: Vector3i = parentRelativePosition
		get() = field.clone()

	val volume: Int = size * size * size

	abstract operator fun get(x: Int, y: Int, z: Int): Voxel
	abstract operator fun set(x: Int, y: Int, z: Int, voxel: Voxel)

	operator fun get(position: Vector3i): Voxel {
		return get(position.x, position.y, position.z)
	}

	operator fun set(position: Vector3i, voxel: Voxel) {
		set(position.x, position.y, position.z, voxel)
	}

	override fun iterator(): Iterator<Voxel> {
		return iterator {
			for(x in 0 until size) for(y in 0 until size) for(z in 0 until size)
				yield(get(x, y, z))
		}
	}

}