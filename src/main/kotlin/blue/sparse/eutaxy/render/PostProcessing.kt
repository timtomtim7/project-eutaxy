package blue.sparse.eutaxy.render

import blue.sparse.engine.SparseEngine
import blue.sparse.engine.asset.Asset
import blue.sparse.engine.render.resource.FrameBuffer
import blue.sparse.engine.render.resource.Texture
import blue.sparse.engine.render.resource.bind
import blue.sparse.engine.render.resource.model.VertexArray
import blue.sparse.engine.render.resource.model.VertexBuffer
import blue.sparse.engine.render.resource.model.VertexLayout
import blue.sparse.engine.render.resource.shader.ShaderProgram
import blue.sparse.math.matrices.Matrix4f
import blue.sparse.math.vectors.floats.Vector2f
import org.lwjgl.opengl.GL11

object PostProcessing {

	private val viewProjection = Matrix4f.orthographic(-1f, 1f, -1f, 1f, -1f, 1f)

	private val array = VertexArray()
	private val layout = VertexLayout()
	private val buffer = VertexBuffer()
	//	private val model = IndexedModel(array, intArrayOf(0, 1, 2, 0, 2, 3))

	private val shader = ShaderProgram(Asset["shaders/screen.fs"], Asset["shaders/screen.vs"])

	val frameBuffer = FrameBuffer().apply {
		val window = SparseEngine.window
		addColorAttachment(Texture((window.width * 1.5).toInt(), (window.height * 1.5).toInt()))
		addDepthBuffer()
	}

	init {
		layout.add<Vector2f>()
		layout.add<Vector2f>()

		buffer.add(Vector2f(-1f, -1f), Vector2f(0f, 0f))
		buffer.add(Vector2f(-1f, 1f), Vector2f(0f, 1f))
		buffer.add(Vector2f(1f, 1f), Vector2f(1f, 1f))
		buffer.add(Vector2f(1f, -1f), Vector2f(1f, 0f))

		array.add(buffer, layout)
		array.setIndices(intArrayOf(0, 1, 2, 0, 2, 3))
	}

	fun render() {
		val window = SparseEngine.window
		GL11.glViewport(0, 0, window.width, window.height)
		bind(shader, frameBuffer.colorAttachments[0]) {
			shader.uniforms["uViewProj"] = viewProjection
			array.render()
		}
	}

	fun delete() {
		shader.delete()
		array.delete()
	}

}