package blue.sparse.eutaxy

import blue.sparse.engine.SparseGame
import blue.sparse.engine.asset.Asset
import blue.sparse.engine.asset.AssetManager
import blue.sparse.engine.errors.glCall
import blue.sparse.engine.render.StateManager
import blue.sparse.engine.render.resource.bind
import blue.sparse.engine.render.resource.shader.ShaderProgram
import blue.sparse.engine.render.scene.component.ShaderSkybox
import blue.sparse.engine.render.scene.component.Skybox
import blue.sparse.engine.window.Window
import blue.sparse.engine.window.input.Key
import blue.sparse.engine.window.input.MouseButton
import blue.sparse.eutaxy.network.VoxelClient
import blue.sparse.eutaxy.network.packet.EditSinglePacket
import blue.sparse.eutaxy.network.packet.EditSpherePacket
import blue.sparse.eutaxy.render.PostProcessing
import blue.sparse.eutaxy.test.*
import blue.sparse.eutaxy.util.AssetProviderURL
import blue.sparse.eutaxy.util.FirstPersonUnbiased
import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.eutaxy.voxel.World
import blue.sparse.math.matrices.Matrix4f
import blue.sparse.math.vectors.floats.Vector3f
import blue.sparse.math.vectors.floats.floor
import blue.sparse.math.vectors.floats.normalize
import blue.sparse.math.vectors.ints.Vector3i
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11.*
import java.awt.Color

class Eutaxy : SparseGame() {

	val shader = ShaderProgram(Asset["shaders/voxel.fs"], Asset["shaders/voxel.vs"])

	val world = World(6)
//	val voxelScene = NormalsScene()
	val client = VoxelClient(world, "localhost", 4096)


	init {
		StateManager.depthClamp = true

//		scene.add(Skybox(Asset["textures/skybox.png"]))
		scene.add(ShaderSkybox(Asset["shaders/gradient_sky.fs"]))
		AssetManager.registerProvider(AssetProviderURL)

		camera.apply {
			moveTo(normalize(Vector3f(1f, 1f, 1f)) * 10f)
			lookAt(Vector3f(0f))
			controller = FirstPersonUnbiased(this, movementSpeed = 5f)
		}

		window.vSync = false

//		voxelScene.generate(world)
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

	fun editAndSendSphere(position: Vector3i, radius: Float, voxel: Voxel, mask: Voxel? = null, invertMask: Boolean = false) {
		val packet = EditSpherePacket(position, radius, voxel, mask ?: Voxel.empty, mask != null, invertMask)
		packet.apply(world)
		client.client.sendPacket(packet)
	}

	fun editAndSendSingle(position: Vector3i, voxel: Voxel) {
		val packet = EditSinglePacket(position, voxel)
		packet.apply(world)
		client.client.sendPacket(packet)
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
				editAndSendSphere(it, 8.5f, Voxel(Color.HSBtoRGB(GLFW.glfwGetTime().toFloat(), 1f, 1f)))
			}
		}

		if (input[Key.F].pressed || input[Key.F].heldTime > 0.5f) {
			val targets = getTargetBlocks()
			targets?.second?.let {
				editAndSendSphere(it, 8.5f, Voxel.empty, null, false)
			}
		}

		if (input[Key.C].pressed || input[Key.C].heldTime > 0.5f) {
			val targets = getTargetBlocks()
			targets?.second?.let {
				if (input[Key.LEFT_CONTROL].held) {
					editAndSendSingle(it, Voxel(Color.HSBtoRGB(GLFW.glfwGetTime().toFloat(), 1f, 1f)))
//					world[it] = Voxel(Color.HSBtoRGB(GLFW.glfwGetTime().toFloat(), 1f, 1f))
				} else {
					editAndSendSphere(
						it,
						10.5f,
						Voxel(Color.HSBtoRGB(GLFW.glfwGetTime().toFloat(), 1f, 1f)),
						Voxel.empty,
						true
					)
//					VoxelScene.sphere(world, it, 10.5f) { v ->
//						if (world[v].isEmpty)
//							Voxel.empty
//						else
//							Voxel(Color.HSBtoRGB(GLFW.glfwGetTime().toFloat(), 1f, 1f))
//					}
				}
			}
		}

		if (input[MouseButton.RIGHT].pressed || input[MouseButton.RIGHT].heldTime > 0.5f) {
			val targets = getTargetBlocks()
			targets?.first?.let {
				editAndSendSingle(it, Voxel(255, 255, 255))
//				world[it] = Voxel(255, 255, 255)
			}
		}

		if (input[MouseButton.LEFT].pressed || input[MouseButton.LEFT].heldTime > 0.5f) {
			val targets = getTargetBlocks()
			targets?.second?.let {
				editAndSendSingle(it, Voxel.empty)
//				world[it] = Voxel.empty
			}
		}

//		if(input[Key.L].pressed) {
//			voxelScene.recalculateNormals(world)
//		}
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