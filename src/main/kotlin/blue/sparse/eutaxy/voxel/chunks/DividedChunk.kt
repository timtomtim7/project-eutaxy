package blue.sparse.eutaxy.voxel.chunks

import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.math.vectors.ints.Vector3i

class DividedChunk(parent: ChunkParent, parentRelativePosition: Vector3i, size: Int, fill: Voxel = Voxel.empty) :
	VoxelChunk(parent, parentRelativePosition, size), ChunkParent {

	val divSize = size / 2

	private val divisions: Array<VoxelChunk> = arrayOf(
		createChunk(Vector3i(0, 0, 0), fill),
		createChunk(Vector3i(1, 0, 0), fill),
		createChunk(Vector3i(0, 1, 0), fill),
		createChunk(Vector3i(1, 1, 0), fill),
		createChunk(Vector3i(0, 0, 1), fill),
		createChunk(Vector3i(1, 0, 1), fill),
		createChunk(Vector3i(0, 1, 1), fill),
		createChunk(Vector3i(1, 1, 1), fill)
	)

	private fun createChunk(pos: Vector3i, fill: Voxel): VoxelChunk {
		return if (fill.isEmpty) {
			EmptyChunk(this, pos, divSize)
		} else {
			FullChunk(this, pos, divSize, fill)
		}
	}

	override fun get(x: Int, y: Int, z: Int): Voxel {
		val cx = x / divSize
		val cy = y / divSize
		val cz = z / divSize
		val index = getChunkIndex(cx, cy, cz)
		if (index !in divisions.indices)
			return Voxel.empty

		return divisions[index][x % divSize, y % divSize, z % divSize]
	}

	override fun set(x: Int, y: Int, z: Int, voxel: Voxel) {
		val cx = x / divSize
		val cy = y / divSize
		val cz = z / divSize
		val index = getChunkIndex(cx, cy, cz)
		if (index !in divisions.indices)
			return

		divisions[index][x % divSize, y % divSize, z % divSize] = voxel

		if (divisions.all { it is EmptyChunk })
			parent.replace(EmptyChunk(parent, parentRelativePosition, size))
	}

	override fun replace(chunk: VoxelChunk) {
		val pos = chunk.parentRelativePosition
		val index = getChunkIndex(pos.x, pos.y, pos.z)
		divisions[index] = chunk
	}

	override fun getRelative(chunk: VoxelChunk, x: Int, y: Int, z: Int): Voxel {
		if (x >= 0 && y >= 0 && z >= 0 && x < divSize && y < divSize && z < divSize)
			return chunk[x, y, z]

		val pos = chunk.parentRelativePosition
		val rx = (pos.x shl 1) + x
		val ry = (pos.y shl 1) + y
		val rz = (pos.z shl 1) + z

		return get(rx, ry, rz)
	}

	private fun getChunkIndex(x: Int, y: Int, z: Int): Int {
		return x + (y * 2) + (z * 4)
	}
}