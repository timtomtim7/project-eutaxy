package blue.sparse.eutaxy.voxel.chunks

interface ChunkParent {

    fun replace(id: Long, chunk: VoxelChunk)

}