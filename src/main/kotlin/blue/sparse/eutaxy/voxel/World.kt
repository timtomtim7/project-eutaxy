package blue.sparse.eutaxy.voxel

import blue.sparse.engine.render.StateManager
import blue.sparse.engine.render.camera.Camera
import blue.sparse.engine.render.resource.shader.ShaderProgram
import blue.sparse.eutaxy.render.model.ChunkModel
import blue.sparse.eutaxy.render.model.OfflineChunkModel
import blue.sparse.eutaxy.voxel.chunks.ChunkParent
import blue.sparse.eutaxy.voxel.chunks.EmptyChunk
import blue.sparse.eutaxy.voxel.chunks.FullChunk
import blue.sparse.eutaxy.voxel.chunks.VoxelChunk
import blue.sparse.math.vectors.floats.Axis
import blue.sparse.math.vectors.floats.Vector3f
import blue.sparse.math.vectors.ints.Vector3i
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

class World(val chunkSizeBits: Int) : ChunkParent {

	val chunkSize = 1 shl chunkSizeBits

	private val positionBitMask = chunkSize - 1

	private val loaded = ConcurrentHashMap<Vector3i, VoxelChunk>()
	private val models = ConcurrentHashMap<Vector3i, ChunkModel>()

	private val queueToModel = ConcurrentHashMap.newKeySet<Vector3i>()
	private val waitingToModel = ConcurrentHashMap.newKeySet<Vector3i>()
	private val queueToUpload = ConcurrentLinkedQueue<OfflineChunkModel>()

	val chunks: Collection<VoxelChunk>
		get() = loaded.values

	override fun getRelative(chunk: VoxelChunk, x: Int, y: Int, z: Int): Voxel {
		if(x >= 0 && y >= 0 && z >= 0 && x < chunkSize && y < chunkSize && z < chunkSize)
			return chunk[x, y, z]

		val pos = chunk.parentRelativePosition
		val rx = (pos.x shl chunkSizeBits) + x
		val ry = (pos.y shl chunkSizeBits) + y
		val rz = (pos.z shl chunkSizeBits) + z

		return get(rx, ry, rz)
	}

	operator fun get(x: Int, y: Int, z: Int): Voxel {
		return getChunkAtBlock(x, y, z)[x and positionBitMask, y and positionBitMask, z and positionBitMask]
	}

	operator fun set(x: Int, y: Int, z: Int, voxel: Voxel) {
		val chunk = getChunkAtBlock(x, y, z)
		val rx = x and positionBitMask
		val ry = y and positionBitMask
		val rz = z and positionBitMask
		chunk[rx, ry, rz] = voxel

		queue(chunk.parentRelativePosition)
		if(rx == chunkSize - 1) queue(chunk.parentRelativePosition + Vector3i(1, 0, 0))
		if(rx == 0) 			queue(chunk.parentRelativePosition + Vector3i(-1, 0, 0))
		if(ry == chunkSize - 1) queue(chunk.parentRelativePosition + Vector3i(0, 1, 0))
		if(ry == 0) 			queue(chunk.parentRelativePosition + Vector3i(0, -1, 0))
		if(rz == chunkSize - 1) queue(chunk.parentRelativePosition + Vector3i(0, 0, 1))
		if(rz == 0) 			queue(chunk.parentRelativePosition + Vector3i(0, 0, -1))
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

	fun getChunk(x: Int, y: Int, z: Int): VoxelChunk {
		val pos = Vector3i(x, y, z)
		return loaded.getOrPut(pos) {
			EmptyChunk(this, pos, chunkSize)
		}
	}

	override fun replace(chunk: VoxelChunk) {
		replaceNoQueue(chunk)
		queue(chunk.parentRelativePosition)

		queue(chunk.parentRelativePosition + Vector3i(+1, 0, 0))
		queue(chunk.parentRelativePosition + Vector3i(-1, 0, 0))
		queue(chunk.parentRelativePosition + Vector3i(0, +1, 0))
		queue(chunk.parentRelativePosition + Vector3i(0, -1, 0))
		queue(chunk.parentRelativePosition + Vector3i(0, 0, +1))
		queue(chunk.parentRelativePosition + Vector3i(0, 0, -1))
	}

	fun replaceNoQueue(chunk: VoxelChunk) {
		if (chunkSize != chunk.size)
			throw IllegalArgumentException("Replacement chunk size must match the world's chunk size.")
		loaded[chunk.parentRelativePosition] = chunk
	}

	fun queueAll() {
		loaded.keys.forEach(::queue)
	}

	private fun queue(position: Vector3i) {
		if(position in loaded.keys && position !in queueToModel && position !in waitingToModel) {
			queueToModel.add(position)
		}
	}

	private fun processQueueToModel() {
		val position = queueToModel.firstOrNull() ?: return
		queueToModel.remove(position)
		val chunk = loaded[position] ?: return

		if(chunk is EmptyChunk) {
			models.remove(position)?.delete()
			return
		}

		if(position in waitingToModel)
			return

		waitingToModel.add(position)
		executors.submit {
			waitingToModel.remove(position)
			val offline = OfflineChunkModel(chunk)
			offline.generate()
			if(position in queueToModel)
				return@submit
			queueToUpload.add(offline)
		}
	}

	private fun processQueueToUpload() {
		val model = queueToUpload.poll() ?: return
		val position = model.chunk.parentRelativePosition
		if(position in queueToModel)
			return processQueueToUpload()

		val worldPos = Vector3f(
			(position.x shl chunkSizeBits).toFloat(),
			(position.y shl chunkSizeBits).toFloat(),
			(position.z shl chunkSizeBits).toFloat()
		) * World.VOXEL_SIZE

		models.remove(position)?.delete()
		models[position] = model.upload(worldPos)
	}

	fun render(camera: Camera, shader: ShaderProgram) {
		val start = System.currentTimeMillis()
		while(queueToModel.isNotEmpty() && System.currentTimeMillis() - start < 5L) {
			processQueueToModel()
		}
		while(queueToUpload.isNotEmpty() && System.currentTimeMillis() - start < 5L) {
			processQueueToUpload()
		}

		StateManager.activeTexture = 0

		for (model in models.values) {
			model.render(camera, shader)
		}
	}

	companion object {
		const val VOXELS_PER_UNIT = 6f
		const val VOXEL_SIZE = 1f / VOXELS_PER_UNIT

		private val executors = Executors.newFixedThreadPool(6) {
			Executors.defaultThreadFactory().newThread(it).apply {
				priority = Thread.NORM_PRIORITY + 1
				isDaemon = true
			}
		}
	}

}