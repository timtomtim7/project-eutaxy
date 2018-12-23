package blue.sparse.eutaxy.voxel.chunks

import blue.sparse.eutaxy.voxel.Voxel

interface ChunkParent {

	fun replace(chunk: VoxelChunk)

	fun getRelative(chunk: VoxelChunk, x: Int, y: Int, z: Int): Voxel

}