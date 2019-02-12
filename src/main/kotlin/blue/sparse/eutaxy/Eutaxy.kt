package blue.sparse.eutaxy

import blue.sparse.engine.SparseGame
import blue.sparse.engine.asset.Asset
import blue.sparse.engine.asset.AssetManager
import blue.sparse.engine.errors.glCall
import blue.sparse.engine.render.StateManager
import blue.sparse.engine.render.resource.bind
import blue.sparse.engine.render.resource.shader.ShaderProgram
import blue.sparse.engine.render.scene.component.Skybox
import blue.sparse.engine.window.Window
import blue.sparse.engine.window.input.Key
import blue.sparse.engine.window.input.MouseButton
import blue.sparse.eutaxy.render.PostProcessing
import blue.sparse.eutaxy.util.AssetProviderURL
import blue.sparse.eutaxy.util.FirstPersonUnbiased
import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.eutaxy.voxel.World
import blue.sparse.math.matrices.Matrix4f
import blue.sparse.math.vectors.floats.*
import blue.sparse.math.vectors.ints.Vector3i
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Eutaxy : SparseGame() {

	val shader = ShaderProgram(Asset["shaders/voxel.fs"], Asset["shaders/voxel.vs"])

	val world = World(6)

	init {
		StateManager.depthClamp = true

		scene.add(Skybox(Asset["textures/skybox.png"]))
		AssetManager.registerProvider(AssetProviderURL)

		camera.apply {
			moveTo(normalize(Vector3f(1f, 1f, 1f)) * 10f)
			lookAt(Vector3f(0f))
			controller = FirstPersonUnbiased(this)
		}

		window.vSync = false

//		val image = Asset["textures/bricks/diffuse.jpg"].readImage()
//		val displaceMap = Asset["textures/bricks/displacement.jpg"].readImage()
//		val normalMap = Asset["textures/bricks/normal.jpg"].readImage()
//
//		//normalize(vec3(0.3,1.0,-0.4))
//		val lightDirection = normalize(Vector3f(0.3f, -0.4f, 1.0f))
//
//		val count = 1
//		val scale = 2
////		val imageWidth = clamp(image.width / scale, 0, 2048)
////		val imageHeight = clamp(image.height / scale, 0, 2048)
//		val imageWidth = image.width / scale
//		val imageHeight = image.height / scale
//
//		println("$imageWidth * $imageHeight")
//
//		var voxelCount = 0
//
//		println()
//		for (cx in 0 until count) {
//			for (cz in 0 until count) {
//				for (x in 0 until imageWidth) {
//					print("\r${(x / imageWidth.toDouble()) * 100}%")
//					for (z in 0 until imageHeight) {
//						val imageX = x * scale
//						val imageY = (imageHeight - z - 1) * scale
//						val color = image.getRGB(imageX, imageY)
//						if (color == 0)
//							continue
//
//						val offsetRGB = Color(displaceMap.getRGB(imageX, imageY))
//						val offset = (offsetRGB.red + offsetRGB.green + offsetRGB.blue) / 3
//
//						val normal = normalize(normalMap.getRGB(imageX, imageY).vectorFromIntRGB() * 2f - 1f)
//						val brightness = dot(normal, lightDirection)
//
//						val voxel = Voxel(color or 0xFF000000.toInt()) * Vector3f(brightness)
////						val voxel = Voxel(Vector3f(brightness))
//
//						for (i in 0 until (offset / (3 * scale)) + 1) {
//							val wx = x + (cx * imageWidth)
//							val wy = i
//							val wz = z + (cz * imageHeight)
//							world[wx, wy, wz] = voxel
////							world[wx, wz, -wy] = voxel
//							voxelCount++
//						}
//					}
//				}
//			}
//		}

//		println("\nVoxel count: $voxelCount")
		val lightDirection2 = normalize(Vector3f(0.3f, 1f, -0.4f))

		thread {
			val count = 32
			val radius = 512
			val angle = ((PI * 2) / count).toFloat()
			for(i in 0 until count) {
				val x = sin(i * angle) * radius
				val z = cos(i * angle) * radius
				val p = i / count.toFloat()
				val color = Vector3f(p, 1f, 1f).HSBtoRGB()

				val origin = Vector3f(x, 0f, z)
				val size = 32f
				sphere(origin.toIntVector(), size) {
					val f = (it.toFloatVector() - origin) / size
					val d = dot(lightDirection2, normalize(f)) * 0.5f + 0.5f
					Voxel(color * d)
				}
			}
		}

	}

	private fun getTargetBlocks(): Pair<Vector3i, Vector3i>? {
		val origin = (camera.transform.translation) * World.VOXELS_PER_UNIT
		val direction = camera.transform.rotation.forward

		val pos = origin.clone()
		val step = 1f / 10f
		for (i in 0..1024) {
			val voxel = world[floor(pos).toIntVector()]
			if (!voxel.isEmpty) {
				val prev = pos - (direction * step)
				val toPlaceInt = floor(prev).toIntVector()
				val toBreakInt = floor(pos).toIntVector()

				return toPlaceInt to toBreakInt
			}

			pos += direction * step
		}

		return null
	}

	inline fun sphere(origin: Vector3i, radius: Float, apply: (Vector3i) -> Voxel) {
		val ri = ceil(radius).toInt()
		for (x in -ri..ri) {
			for (y in -ri..ri) {
				for (z in -ri..ri) {
					val v = Vector3i(x, y, z)
					if (lengthSquared(v.toFloatVector()) >= radius * radius)
						continue

					val position = origin + v
					world[position] = apply(position)
				}
			}
		}
	}

	override fun update(delta: Float) {
		if (window.resized) {
			camera.projection = Matrix4f.perspective(100f, window.aspectRatio, 0.1f, 1000f)
			PostProcessing.resetFrameBuffer()
		}

		val notGrabbed = window.cursorMode == Window.CursorMode.NORMAL
		super.update(delta)
		if (notGrabbed)
			return

		if (input[Key.R].pressed || input[Key.R].heldTime > 0.5f) {
			val targets = getTargetBlocks()
			targets?.first?.let {
				sphere(it, 8.5f) {
					Voxel(Color.HSBtoRGB(0.58888f, Random.nextFloat() * 0.1f + 0.15f, 0.75f))
				}
			}
		}

		if (input[Key.F].pressed || input[Key.F].heldTime > 0.5f) {
			val targets = getTargetBlocks()
			targets?.second?.let {
				sphere(it, 8.5f) { Voxel.empty }
			}
		}

		if (input[Key.C].pressed || input[Key.C].heldTime > 0.5f) {
			val targets = getTargetBlocks()
			targets?.second?.let {
				if (input[Key.LEFT_CONTROL].held) {
					world[it] = Voxel(Color.HSBtoRGB(GLFW.glfwGetTime().toFloat(), 1f, 1f))
				} else {
					sphere(it, 10.5f) { v ->
						if (world[v].isEmpty)
							Voxel.empty
						else
							Voxel(Color.HSBtoRGB(GLFW.glfwGetTime().toFloat(), 1f, 1f))
					}
				}
			}
		}

		if (input[MouseButton.RIGHT].pressed || input[MouseButton.RIGHT].heldTime > 0.5f) {
			val targets = getTargetBlocks()
			targets?.first?.let {
//				world[it] = Voxel(Color.HSBtoRGB(GLFW.glfwGetTime().toFloat(), 1f, 1f))
				world[it] = Voxel(255, 255, 255)
			}
		}

		if (input[MouseButton.LEFT].pressed || input[MouseButton.LEFT].heldTime > 0.5f) {
			val targets = getTargetBlocks()
			targets?.second?.let {
				world[it] = Voxel.empty
			}
		}
	}

	override fun render(delta: Float) {
		PostProcessing.frameBuffer.bind {
			glClearColor(1f, 1f, 1f, 1f)
			clear()

			val wireframe = input[Key.G].held
			if (wireframe) {
				glCall { glLineWidth(3f) }
				glCall { glPolygonMode(GL_FRONT_AND_BACK, GL_LINE) }
				glCall { glDisable(GL_CULL_FACE) }
			} else {
				scene.render(delta, camera, shader)
			}

			shader.bind {
				uniforms["uCameraPosition"] = camera.transform.translation
				//				uniforms["uTexture"] = 0
				world.render(camera, shader)
			}

			if (wireframe) {
				glCall { glPolygonMode(GL_FRONT_AND_BACK, GL_FILL) }
				glCall { glEnable(GL_CULL_FACE) }
			}
		}

		PostProcessing.render()

	}
}