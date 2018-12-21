package blue.sparse.eutaxy

import blue.sparse.engine.SparseGame
import blue.sparse.engine.asset.Asset
import blue.sparse.engine.render.camera.FirstPerson
import blue.sparse.engine.render.resource.Texture
import blue.sparse.engine.render.resource.bind
import blue.sparse.engine.render.resource.shader.ShaderProgram
import blue.sparse.engine.render.scene.component.Skybox
import blue.sparse.engine.window.input.Key
import blue.sparse.engine.window.input.MouseButton
import blue.sparse.eutaxy.render.PostProcessing
import blue.sparse.eutaxy.util.Perlin
import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.eutaxy.voxel.World
import blue.sparse.math.vectors.floats.*
import blue.sparse.math.vectors.ints.Vector3i
import blue.sparse.math.vectors.ints.lengthSquared
import java.awt.Color
import kotlin.math.ceil
import kotlin.random.Random

class Eutaxy : SparseGame() {

	val shader = ShaderProgram(Asset["shaders/voxel.fs"], Asset["shaders/voxel.vs"])
	val texture = Texture(Asset["textures/developer/diffuse.png"])

//	private val models = ArrayList<ChunkModel>()

//	var chunk: VoxelChunk
	val world = World(6)

	init {
		scene.add(Skybox(Asset["textures/skybox.png"]))
//		scene.add(ShaderSkybox(Asset["shaders/normal_sky.fs"]))

		camera.apply {
			moveTo(normalize(Vector3f(1f, 1f, 1f)) * 10f)
			lookAt(Vector3f(0f))
			controller = FirstPerson(this)
		}

		window.vSync = false

		for(rx in 0 until 1024) {
			println(rx)
			for(rz in 0 until 1024) {
				val ty = Perlin.noise(rx.toFloat(), 0f, rz.toFloat(), 6, 0.4f, 0.01f) * 16 + 48

				for (y in 0..ty.toInt()) {
					val saturation = Perlin.noise(rx.toFloat(), y.toFloat(), rz.toFloat(), 2, 0.1f, 0.01f)

					world[rx, y, rz] = Voxel(Color.HSBtoRGB(
						0.363888f,
						saturation * 0.4f + 0.6f,
						Random.nextFloat() * 0.1f + 0.5f
					) or 0xFF000000.toInt())
				}
			}
		}

//		val image = Asset["oak_planks.png"].readImage()
//		for(cx in 0 until 64) {
//			for(cz in 0 until 64) {
//				for(x in 0 until image.width) {
//					for(z in 0 until image.height) {
//						val rx = cx * image.width + x
//						val rz = cz * image.height + z
//
//						val voxel = Voxel(image.getRGB(x, z))
//						for(y in 0 until 16) {
//							world[rx, y, rz] = voxel
//						}
//					}
//				}
//			}
//		}

//		for(x in 0 until 512) {
//			for(z in 0 until 512) {
//				world[x, 0, z] = Voxel(200, 200, 200 + Random.nextInt(55))
//			}
//		}

//		chunk = DetailedChunk(parent, Vector3i(0), 64)
//		for(x in 0 until chunk.size)
//			for(z in 0 until chunk.size)
//				chunk[x, 0, z] = Voxel(255, 255, 255)
	}

	private fun getTargetBlocks(): Pair<Vector3i, Vector3i>? {
		val origin = (camera.transform.translation) * 4f
		val direction = camera.transform.rotation.forward

		val pos = origin.clone()
		val step = 1f / 4f
		for(i in 0..512) {
			val voxel = world[pos.toIntVector()]
			if(!voxel.isEmpty) {
				val prev = pos - (direction * step)
				val toPlaceInt = prev.toIntVector()
				val toBreakInt = pos.toIntVector()

				return toPlaceInt to toBreakInt
			}

			pos += direction * step
		}

		return null
	}

//	fun deleteModels() {
//		models.removeAll {
//			it.delete()
//			true
//		}
//	}

	private var lastTargets = getTargetBlocks()
	private var time = 0f

	inline fun sphere(origin: Vector3i, radius: Float, apply: (Vector3i) -> Voxel) {
		val ri = ceil(radius).toInt()
		for(x in -ri..ri) {
			for(y in -ri..ri) {
				for(z in -ri..ri) {
					val v = Vector3i(x, y, z)
					if(lengthSquared(v.toFloatVector()) >= radius * radius)
						continue

					val position = origin + v
					world[position] = apply(position)
				}
			}
		}
	}

	override fun update(delta: Float) {
		super.update(delta)
		time += delta

		if(input[Key.R].pressed) {
			val targets = getTargetBlocks()
			targets?.first?.let {
				sphere(it, 100f) {
					Voxel(Color.HSBtoRGB(0.58888f, Random.nextFloat() * 0.1f + 0.15f, 0.75f))
				}
			}
		}

		if(input[MouseButton.RIGHT].held) {
			val targets = getTargetBlocks()
			targets?.first?.let {
				sphere(it, 2.5f) {
					Voxel(Color.HSBtoRGB(0.58888f, Random.nextFloat() * 0.1f + 0.15f, 0.75f))
				}
			}
		}

		if(input[MouseButton.LEFT].held) {
			val targets = getTargetBlocks()
			targets?.first?.let {
//				if(distance(it.toFloatVector() / 4f, camera.transform.translation) > 8f) {
					sphere(it, 2.5f) { Voxel.empty }
//				}
			}
		}
	}

	override fun render(delta: Float) {

//		if(models.isEmpty()) {
//			models.add(OfflineChunkModel(chunk).generateModel(Vector3f(0f)))
//		}

		PostProcessing.frameBuffer.bind {
			clear()

//			glCall { glPolygonMode(GL_FRONT_AND_BACK, GL_FILL) }
//			glCall { glEnable(GL_CULL_FACE) }
			scene.render(delta, camera, shader)

//			val wireframeButton = input[Key.F]
//			if (wireframeButton.held) {
//				glCall { glPolygonMode(GL_FRONT_AND_BACK, GL_LINE) }
//				glCall { glDisable(GL_CULL_FACE) }
//			}

			shader.bind {
				uniforms["uTexture"] = 0
				world.render(camera, shader)

//				models.forEach { it.render(camera, shader) }
			}
		}

		PostProcessing.render()

	}
}