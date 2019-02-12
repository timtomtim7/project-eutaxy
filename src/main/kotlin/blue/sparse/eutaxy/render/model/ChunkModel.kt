package blue.sparse.eutaxy.render.model

import blue.sparse.engine.render.camera.Camera
import blue.sparse.engine.render.resource.Texture
import blue.sparse.engine.render.resource.model.VertexArray
import blue.sparse.engine.render.resource.shader.ShaderProgram
import blue.sparse.math.matrices.Matrix4f
import blue.sparse.math.vectors.floats.Vector3f
import blue.sparse.math.vectors.floats.Vector4f

class ChunkModel(val position: Vector3f, val size: Vector3f, val vertexArray: VertexArray, val texture: Texture) {

	private val modelMatrix = Matrix4f.translation(position)
	val triCount = vertexArray.size / 3

	val aabb = run {
		val min = Vector3f(0f)
		val max = size

		arrayOf(
			Vector4f(min.x, min.y, min.z, 1f),
			Vector4f(min.x, min.y, max.z, 1f),
			Vector4f(min.x, max.y, min.z, 1f),
			Vector4f(min.x, max.y, max.z, 1f),
			Vector4f(max.x, min.y, min.z, 1f),
			Vector4f(max.x, min.y, max.z, 1f),
			Vector4f(max.x, max.y, min.z, 1f),
			Vector4f(max.x, max.y, max.z, 1f)
		)
	}

	fun checkFrustum(viewProjection: Matrix4f): Boolean {
		val mvp = viewProjection * modelMatrix
		val transformed = aabb.map { mvp * it }

		val c = IntArray(6)
		for (p in transformed) {
			if (p.x < -p.w) c[0]++
			if (p.x > p.w) c[1]++
			if (p.y < -p.w) c[2]++
			if (p.y > p.w) c[3]++
			if (p.z < -p.w) c[4]++
			if (p.z > p.w) c[5]++
		}

		return c.none { it == 8 }
	}

	fun render(camera: Camera, shader: ShaderProgram): Boolean {
		val viewProj = camera.viewProjectionMatrix
		if (!checkFrustum(viewProj))
			return false

		texture.bind()
		shader.uniforms["uViewProj"] = viewProj
		shader.uniforms["uModel"] = modelMatrix
		shader.uniforms["uAtlasSize"] = texture.size.toFloatVector()
		vertexArray.render()

		return true
	}

	fun delete() {
		vertexArray.delete()
		texture.delete()
	}

}