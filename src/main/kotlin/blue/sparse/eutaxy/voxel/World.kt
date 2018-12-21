package blue.sparse.eutaxy.voxel

import blue.sparse.engine.render.camera.Camera
import blue.sparse.engine.render.resource.shader.ShaderProgram
import blue.sparse.eutaxy.render.model.ChunkModel
import blue.sparse.eutaxy.render.model.OfflineChunkModel
import blue.sparse.eutaxy.voxel.chunks.ChunkParent
import blue.sparse.eutaxy.voxel.chunks.EmptyChunk
import blue.sparse.eutaxy.voxel.chunks.VoxelChunk
import blue.sparse.math.vectors.floats.Vector3f
import blue.sparse.math.vectors.ints.Vector3i
import java.util.concurrent.ConcurrentLinkedQueue

class World(val chunkSizeBits: Int) : ChunkParent {

	val chunkSize = 1 shl chunkSizeBits

	private val positionBitMask = chunkSize - 1

	private val loaded = HashMap<Vector3i, VoxelChunk>()
	private val models = HashMap<Vector3i, ChunkModel>()
	private val queued = ConcurrentLinkedQueue<Vector3i>()

	operator fun get(x: Int, y: Int, z: Int): Voxel {
		return getChunkAtBlock(x, y, z)[x and positionBitMask, y and positionBitMask, z and positionBitMask]
	}

	operator fun set(x: Int, y: Int, z: Int, voxel: Voxel) {
		val chunk = getChunkAtBlock(x, y, z)
		chunk[x and positionBitMask, y and positionBitMask, z and positionBitMask] = voxel

		val toQueue = chunk.parentRelativePosition
		if(toQueue !in queued)
			queued.add(toQueue)
	}

	operator fun get(position: Vector3i): Voxel {
		return get(position.x, position.y, position.z)
	}

	operator fun set(position: Vector3i, voxel: Voxel) {
		set(position.x, position.y, position.z, voxel)
	}

	private fun getChunkAtBlock(x: Int, y: Int, z: Int): VoxelChunk {
		return getChunk(x shr chunkSizeBits, y shr chunkSizeBits, z shr chunkSizeBits)
	}

	private fun getChunk(x: Int, y: Int, z: Int): VoxelChunk {
		val pos = Vector3i(x, y, z)
		return loaded.getOrPut(pos) { EmptyChunk(this, pos, chunkSize) }
	}

	override fun replace(chunk: VoxelChunk) {
		if (chunkSize != chunk.size)
			throw IllegalArgumentException("Replacement chunk size must match the world's chunk size.")
		loaded[chunk.parentRelativePosition] = chunk
		val toQueue = chunk.parentRelativePosition
		if(toQueue !in queued)
			queued.add(toQueue)
	}

	private fun processQueue() {
		val position = queued.poll() ?: return
		models.remove(position)?.delete()

		val chunk = loaded[position] ?: return
		if(chunk is EmptyChunk)
			return

		val worldPos = Vector3f((position.x shl chunkSizeBits).toFloat(), (position.y shl chunkSizeBits).toFloat(), (position.z shl chunkSizeBits).toFloat()) / 4f
		models[position] = OfflineChunkModel(chunk).generateModel(worldPos)
	}

	fun render(camera: Camera, shader: ShaderProgram) {
		processQueue()

		shader.uniforms["uTexture"] = 0
		models.values.forEach { it.render(camera, shader) }
	}

}