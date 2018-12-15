package blue.sparse.eutaxy

import blue.sparse.engine.SparseGame
import blue.sparse.engine.asset.Asset
import blue.sparse.engine.math.int
import blue.sparse.engine.render.camera.FirstPerson
import blue.sparse.engine.render.resource.bind
import blue.sparse.engine.render.resource.shader.ShaderProgram
import blue.sparse.engine.render.scene.component.ShaderSkybox
import blue.sparse.engine.render.scene.component.Skybox
import blue.sparse.engine.window.input.MouseButton
import blue.sparse.eutaxy.render.ChunkModel
import blue.sparse.eutaxy.util.Perlin
import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.eutaxy.voxel.chunks.ChunkParent
import blue.sparse.eutaxy.voxel.chunks.DetailedChunk
import blue.sparse.eutaxy.voxel.chunks.VoxelChunk
import blue.sparse.math.vectors.floats.*
import java.awt.Color
import java.io.File
import kotlin.random.Random

class Eutaxy : SparseGame() {

	val shader = ShaderProgram(Asset["shaders/fragment.fs"], Asset["shaders/vertex.vs"])
//	val texture = Texture(Asset["textures/default/diffuse.png"])

	private val models = ArrayList<ChunkModel>()

//	val testMesh = run {
//		val parent = object : ChunkParent {
//			override fun replace(id: Long, chunk: VoxelChunk) {}
//		}
//
//		val chunk = DetailedChunk(parent, 0L, 256)
//		for (x in 0..chunk.size) {
//			for(z in 0..chunk.size) {
//				val ty = Perlin.noise(x.toFloat(), 0f, z.toFloat(), 2, 0.05f, 0.05f) * 8 + chunk.size / 2
//				for(y in 0..ty.toInt()) {
//					chunk[x, y, z] = Voxel(0xFFFFFFFF.toInt())
//				}
//			}
//		}
//
//		chunk.generateModel()
//	}

//	val chunk: DetailedChunk

	init {
		scene.add(Skybox(Asset["textures/skybox.png"]))
//		scene.add(ShaderSkybox(Asset["shaders/normal_sky.fs"]))

		camera.apply {
			moveTo(normalize(Vector3f(1f, 1f, 1f)) * 10f)
			lookAt(Vector3f(0f))
			controller = FirstPerson(this)
		}

//		scene.add(ModelComponent(testMesh.first.toModel(), arrayOf(testMesh.second)))

		window.vSync = false

		val parent = object : ChunkParent {
			override fun replace(id: Long, chunk: VoxelChunk) {}
		}

//		chunk = DetailedChunk(parent, 0L, 64)
//		for(x in 0 until chunk.size) {
//			for (z in 0 until chunk.size) {
//				chunk[x, 0, z] = Voxel(255, 255, 255)
//			}
//		}
		val folder = File("chunks")
		folder.mkdirs()
		for(cx in 0 until 16) {
			for(cz in 0 until 16) {
				println("$cx $cz")
				val chunk = DetailedChunk(parent, 0L, 128)
				val file = File(folder, "${cx}_$cz.euc")
				if(file.exists()) {
					chunk.fromFile(file)
					println("Read data $cx $cz")
				}else{
					for (x in 0..chunk.size) {
						val rx = cx * chunk.size + x
						for(z in 0..chunk.size) {
							val rz = cz * chunk.size + z

							val ty = if(rx == 16 && rz == 16)
								chunk.size - 1f
							else
								Perlin.noise(rx.toFloat(), 0f, rz.toFloat(), 8, 0.5f, 0.005f) * 64 + 48

							for(y in 0..ty.toInt()) {
								val hue = Perlin.noise(rx.toFloat(), y.toFloat(), rz.toFloat(), 1, 0.1f, 0.003f)

								chunk[x, y, z] = Voxel(Color.HSBtoRGB(hue, 0.8f, 0.5f) or 0xFF000000.toInt())
//							chunk[x, y, z] = Voxel(100, 128 + Random.nextInt(32), 80)
							}
						}
					}
					println("Write data $cx $cz")
					chunk.toFile(file)
				}

				val r = chunk.size / 4f
				models.add(chunk.generateModel(Vector3f(cx * r, 0f, cz * r)))
			}
		}

//		scene.add(ModelComponent(WavefrontModelLoader.load(Asset["models/smooth_sphere.obj"]), arrayOf(texture)))
	}

	override fun update(delta: Float) {
		super.update(delta)

//		if(input[MouseButton.RIGHT].pressed || input[MouseButton.RIGHT].heldTime > 0.3f) {
//			val origin = (camera.transform.translation * 16f)
//			val direction = camera.transform.rotation.forward
//
//			val pos = origin.clone()
//			val step = 1f / 4f
//			for(i in 0..128) {
//				val voxel = chunk[pos.toIntVector()]
//				if(voxel?.isEmpty == false) {
//					val prev = pos - (direction * step)
//					val toPlaceInt = prev.toIntVector()
//					chunk[toPlaceInt] = Voxel(0, 85, 255)
//
//					val model = models.firstOrNull() ?: return
//					models.remove(model)
//					model.delete()
//					break
//				}
//
//				pos += direction * step
//			}

//			println(pos)
//			chunk[pos] = Voxel(0, 85, 255)
//			val model = models.firstOrNull() ?: return
//			models.remove(model)
//			model.delete()
//		}

	}

	override fun render(delta: Float) {
		scene.render(delta, camera, shader)
//		if(models.isEmpty()) {
//			models.add(chunk.generateModel(Vector3f(0f)))
//		}

		shader.bind {
			models.forEach { it.render(camera, shader) }
		}

	}
}