package blue.sparse.eutaxy.render.model

import blue.sparse.engine.render.camera.Camera
import blue.sparse.engine.render.resource.Texture
import blue.sparse.engine.render.resource.model.VertexArray
import blue.sparse.engine.render.resource.shader.ShaderProgram
import blue.sparse.math.matrices.Matrix4f
import blue.sparse.math.vectors.floats.Vector3f

class ChunkModel(val position: Vector3f, val vertexArray: VertexArray, val texture: Texture) {

	private val modelMatrix = Matrix4f.translation(position)
	val triCount = vertexArray.size / 3

	fun render(camera: Camera, shader: ShaderProgram) {
		texture.bind()
		shader.uniforms["uViewProj"] = camera.viewProjectionMatrix
		shader.uniforms["uModel"] = modelMatrix
		shader.uniforms["uAtlasSize"] = texture.size.toFloatVector()
		vertexArray.render()
	}

	fun delete() {
		vertexArray.delete()
		texture.delete()
	}

}