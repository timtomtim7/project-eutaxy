package blue.sparse.eutaxy

import blue.sparse.engine.SparseGame
import blue.sparse.engine.asset.Asset
import blue.sparse.engine.asset.AssetManager
import blue.sparse.engine.errors.glCall
import blue.sparse.engine.render.StateManager
import blue.sparse.engine.render.camera.FirstPerson
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
import blue.sparse.math.clamp
import blue.sparse.math.matrices.Matrix4f
import blue.sparse.math.vectors.floats.*
import blue.sparse.math.vectors.ints.Vector3i
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import kotlin.math.ceil
import kotlin.random.Random

class Eutaxy : SparseGame() {

	val shader = ShaderProgram(Asset["shaders/voxel.fs"], Asset["shaders/voxel.vs"])

	private val cobblestone = Asset["cobblestone.png"].readImage()
	private val gold = Asset["gold_block.png"].readImage()
	private val planks = Asset["oak_planks.png"].readImage()
	private val iron = Asset["iron_block.png"].readImage()

	val world = World(6)

	init {
		StateManager.depthClamp = true

//		val field = window.javaClass.getDeclaredField("id")
//		field.isAccessible = true
//		val id = field.getLong(window)
//		println(id)

		scene.add(Skybox(Asset["textures/skybox.png"]))
//		scene.add(ShaderSkybox(Asset["shaders/normal_sky.fs"]))
		AssetManager.registerProvider(AssetProviderURL)

		camera.apply {
			moveTo(normalize(Vector3f(1f, 1f, 1f)) * 10f)
			lookAt(Vector3f(0f))
			controller = FirstPersonUnbiased(this)
		}

		window.vSync = false

//		for(rx in 0 until 1024) {
//			if(rx % 128 == 0)
//				println(rx)
//			for(rz in 0 until 1024) {
////				val ty = Perlin.noise(rx.toFloat(), 0f, rz.toFloat(), 6, 0.5f, 0.02f) * 32 + 48
//				var ty = Perlin.noise(rx.toFloat() - 2348, 0f, rz.toFloat() - 5398, 8, 0.48f, 0.00045f) * (-0.5f - 1f) / 2f + (1f + -0.5f) / 2
//				if(ty < 0)
//					ty *= 0.5f
////				ty *= 752
//				ty *= 512
//
////				val color = Random.nextInt(64) + 127
//				for (y in 0..ty.toInt()) {
//					val f = Perlin.noise(rx.toFloat(), y.toFloat(), rz.toFloat(), 2, 0.1f, 0.02f)
//					val color = Color.HSBtoRGB(f, 0.7f, 0.7f)
//					world[rx, y, rz] = Voxel(color)
////
////					world[rx, y, rz] = Voxel(Color.HSBtoRGB(
////						0.363888f,
////						saturation * 0.4f + 0.6f,
////						Random.nextFloat() * 0.05f + 0.5f
////					) or 0xFF000000.toInt())
//				}
//			}
//		}


//		for(cx in 0 until 64) {
//			println(cx)
//			for(cz in 0 until 64) {
//				for(cy in 0 until 2) {
//					placeMinecraftBlock(Vector3i(cx * 16, cy * 16, cz * 16), if(Math.random() < 0.25) iron else cobblestone)
//					placeMinecraftBlock(Vector3i(cy * 16, cx * 16, cz * 16), if(Math.random() < 0.25) gold else cobblestone)
//				}
//			}
//		}

//		val g = Asset["https://sparse.blue/api/images/tri?colorMin=333333&colorMax=555555&random=0&res=1024&count=12"].readImage()
		val image = Asset["textures/bricks/diffuse.jpg"].readImage()
		val displaceMap = Asset["textures/bricks/displacement.jpg"].readImage()
		val normalMap = Asset["textures/bricks/normal.jpg"].readImage()

		//normalize(vec3(0.3,1.0,-0.4))
		val lightDirection = normalize(Vector3f(0.3f, -0.4f, 1.0f))

		val count = 1
		val scale = 2
//		val imageWidth = clamp(image.width / scale, 0, 2048)
//		val imageHeight = clamp(image.height / scale, 0, 2048)
		val imageWidth = image.width / scale
		val imageHeight = image.height / scale

		println("$imageWidth * $imageHeight")

		var voxelCount = 0

		println()
		for (cx in 0 until count) {
			for (cz in 0 until count) {
				for (x in 0 until imageWidth) {
					print("\r${(x / imageWidth.toDouble()) * 100}%")
					for (z in 0 until imageHeight) {
						val imageX = x * scale
						val imageY = (imageHeight - z - 1) * scale
						val color = image.getRGB(imageX, imageY)
						if (color == 0)
							continue

						val offsetRGB = Color(displaceMap.getRGB(imageX, imageY))
						val offset = (offsetRGB.red + offsetRGB.green + offsetRGB.blue) / 3

						val normal = normalize(normalMap.getRGB(imageX, imageY).vectorFromIntRGB() * 2f - 1f)
						val brightness = dot(normal, lightDirection)

						val voxel = Voxel(color or 0xFF000000.toInt()) * Vector3f(brightness)
//						val voxel = Voxel(Vector3f(brightness))

						for (i in 0 until (offset / (3 * scale)) + 1) {
//						for(i in 0 until 64) {
							val wx = x + (cx * imageWidth)
							val wy = i
							val wz = z + (cz * imageHeight)
							world[wx, wz, wy] = voxel
//							world[wx, wz, -wy] = voxel
							voxelCount++
						}
					}
				}
			}
		}

		println("\nVoxel count: $voxelCount")

//		loadBuffers(File("buffers/0.pbuf"), File("buffers/0.nbuf"))

//		for(i in 0..10) {
//			println("Loading file $i")
//			loadBuffers(File("buffers/$i.pbuf"), File("buffers/$i.nbuf"))
//		}

//		loadMinecraftExport(File("C:\\Users\\Tom\\Desktop\\IntelliJ\\Projects\\Minecraft\\Bukkit Servers\\SparseMC Testing\\voxel.euc"))
	}

	private fun loadBuffers(positionBufferFile: File, normalBufferFile: File) {
		val lightDirection = normalize(Vector3f(0.3f, -0.4f, 1.0f))

		println()
		val positionData =
			ByteBuffer.wrap(positionBufferFile.readBytes()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
//		val normalData = ByteBuffer.wrap(normalBufferFile.readBytes()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
		while (positionData.position() < positionData.limit()) {
			val x = positionData.get()
			val y = positionData.get()
			val z = positionData.get()
			val a = positionData.get() // alpha?
			if (a == 0f)
				continue

//			val nx = normalData.get()
//			val ny = normalData.get()
//			val nz = normalData.get()
//			normalData.get() // alpha?

//			val normal = Vector3f(nx, ny, nz)
//			val brightness = dot(normal, lightDirection)
			val brightness = length(Vector3f(x, y, z) / 64f)

			print("\r${(positionData.position().toDouble() / positionData.limit()) * 100}%")

			world[(Vector3f(x, y, z) * 16f).toIntVector()] = Voxel(Vector3f(brightness))
		}
		println()
	}

	private fun loadMinecraftExport(file: File) {
		val input = DataInputStream(GZIPInputStream(file.inputStream()).buffered())

		try {
			while (true) {
				val cx = input.readInt()
				val cz = input.readInt()

				println("Reading $cx $cz")

				val rcx = cx shl 4
				val rcz = cz shl 4
				val alphaMask = 0xFF000000.toInt()
				for (x in 0 until 16) {
					for (y in 0 until 256) {
						for (z in 0 until 16) {
							val color = input.readInt()
							if (y < 50)
								continue
							if (color == 0)
								continue
							val newColor = Random(color).nextInt() or alphaMask

							world[rcx + x, y, rcz + z] = Voxel(newColor)
						}
					}
				}
			}
		} catch (e: EOFException) {

		}

		input.close()

	}


	private fun placeMinecraftBlock(position: Vector3i, image: BufferedImage) {
		val mask = 0xF.inv()
		val rx = position.x and mask
		val ry = position.y and mask
		val rz = position.z and mask

		for (x in 1 until 15) {
			for (y in 1 until 15) {
				for (z in 1 until 15) {
					val rgb = image.getRGB(Random.nextInt(16), Random.nextInt(16))
					world[rx + x, ry + y, rz + z] = Voxel(rgb)
				}
			}
		}

		for (x in 0 until 16) {
			for (y in 0 until 16) {
				val voxel = Voxel(image.getRGB(x, y))
				world[rx + x, ry + y, rz + 0] = voxel
				world[rx + x, ry + 0, rz + y] = voxel
				world[rx + 0, ry + x, rz + y] = voxel
				world[rx + x, ry + y, rz + 15] = voxel
				world[rx + 15, ry + x, rz + y] = voxel
				world[rx + x, ry + 15, rz + y] = voxel
			}
		}
	}

	private fun removeMinecraftBlock(position: Vector3i) {
		val mask = 0xF.inv()
		val rx = position.x and mask
		val ry = position.y and mask
		val rz = position.z and mask

		for (x in 0 until 16) {
			for (y in 0 until 16) {
				for (z in 0 until 16) {
					world[rx + x, ry + y, rz + z] = Voxel.empty
				}
			}
		}
	}

	private fun getTargetBlocks(): Pair<Vector3i, Vector3i>? {
		val origin = (camera.transform.translation) * World.VOXELS_PER_UNIT
		val direction = camera.transform.rotation.forward

		val pos = origin.clone()
		val step = 1f / 2f
		for (i in 0..512) {
			val voxel = world[pos.toIntVector()]
			if (!voxel.isEmpty) {
				val prev = pos - (direction * step)
				val toPlaceInt = prev.toIntVector()
				val toBreakInt = pos.toIntVector()

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
				sphere(it, 6.5f) {
					Voxel(Color.HSBtoRGB(0.58888f, Random.nextFloat() * 0.1f + 0.15f, 0.75f))
				}
			}
		}

		if (input[Key.F].pressed || input[Key.F].heldTime > 0.5f) {
			val targets = getTargetBlocks()
			targets?.second?.let {
				sphere(it, 16.5f) { Voxel.empty }
			}
		}

		if (input[Key.C].pressed || input[Key.C].heldTime > 0.5f) {
			val targets = getTargetBlocks()
			targets?.second?.let {
				if (input[Key.LEFT_CONTROL].held) {
					world[it] = Voxel(Color.HSBtoRGB(GLFW.glfwGetTime().toFloat(), 1f, 1f))
				} else {
					sphere(it, 21.5f) { v ->
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
//				if (input[Key.LEFT_CONTROL].held) {
					world[it] = Voxel(Color.HSBtoRGB(GLFW.glfwGetTime().toFloat(), 1f, 1f))
//				} else {
//					placeMinecraftBlock(it, gold)
//				}
			}
		}

		if (input[MouseButton.LEFT].pressed || input[MouseButton.LEFT].heldTime > 0.5f) {
			val targets = getTargetBlocks()
			targets?.second?.let {
//				if (input[Key.LEFT_CONTROL].held) {
					world[it] = Voxel.empty
//				} else {
//					removeMinecraftBlock(it)
//				}
//				if(distance(it.toFloatVector() / 4f, camera.transform.translation) > 8f) {
//					sphere(it, 5.5f) { Voxel.empty }
//				}
			}
		}
	}

	override fun render(delta: Float) {
		PostProcessing.frameBuffer.bind {
			glClearColor(1f, 1f, 1f, 1f)
			clear()

//			glCall { glPolygonMode(GL_FRONT_AND_BACK, GL_FILL) }
//			glCall { glEnable(GL_CULL_FACE) }

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