package blue.sparse.eutaxy.render.model

import blue.sparse.engine.render.resource.Texture
import blue.sparse.engine.render.resource.model.VertexArray
import blue.sparse.engine.render.resource.model.VertexBuffer
import blue.sparse.engine.render.resource.model.VertexLayout
import blue.sparse.engine.util.ColorFormat
import blue.sparse.eutaxy.render.texture.OfflineChunkTexture
import blue.sparse.eutaxy.voxel.World
import blue.sparse.eutaxy.voxel.chunks.VoxelChunk
import blue.sparse.extensions.toByteBuffer
import blue.sparse.math.vectors.floats.*
import blue.sparse.math.vectors.ints.*
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

class OfflineChunkModel(val chunk: VoxelChunk) {

	@Volatile
	var generated: Boolean = false
		private set

	private var textureWidth: Int = 0
	private var textureHeight: Int = 0
	private lateinit var texture: ByteBuffer
	private lateinit var buffer: ByteBuffer

	fun generate() {
		val buffer = VertexBuffer()
		val atlas = OfflineChunkTexture()
		val quads = generateQuads()

		fillBufferAndAtlasWithQuads(buffer, atlas, quads)

		this.buffer = buffer.toByteBuffer()
		this.textureWidth = atlas.size.x
		this.textureHeight = atlas.size.y
		this.texture = atlas.renderToBuffer()

		this.generated = true
	}

	fun upload(position: Vector3f): ChunkModel {
		val layout = VertexLayout()
		layout.add<Vector3f>() // Position
		layout.add<Vector2i>() // Texture Coordinate
		layout.add<Vector3f>() // Normal

		val glArray = VertexArray()
		glArray.add(buffer, layout)

		val glTexture = Texture(textureWidth, textureHeight, texture).apply {
			nearestFiltering()
			clampToEdge()
		}

		return ChunkModel(position, Vector3f(chunk.size.toFloat() * World.VOXEL_SIZE), glArray, glTexture)
	}

	private fun fillBufferAndAtlasWithQuads(buffer: VertexBuffer, atlas: OfflineChunkTexture, quads: List<Quad>) {
		for (quad in quads) {
			val side = quad.side
			val sideVector = side.vector
			val ic = side.axisIndex
			val ia = indexA[ic]
			val ib = indexB[ic]


			val shape = quad.shape
			var c = shape.c.toFloat()
			if (side.ordinal < 3)
				c += 1f

			val minA = shape.minA.toFloat()
			val maxA = shape.maxA.toFloat() + 1f
			val minB = shape.minB.toFloat()
			val maxB = shape.maxB.toFloat() + 1f
			val v00 = rearrange(Vector3f(minA, minB, c), ia, ib, ic) * World.VOXEL_SIZE
			val v10 = rearrange(Vector3f(maxA, minB, c), ia, ib, ic) * World.VOXEL_SIZE
			val v11 = rearrange(Vector3f(maxA, maxB, c), ia, ib, ic) * World.VOXEL_SIZE
			val v01 = rearrange(Vector3f(minA, maxB, c), ia, ib, ic) * World.VOXEL_SIZE

			val sprite = atlas.addSprite(quad.colors.width, quad.colors.height, quad.colors.colors)
			val texCoords = Vector4i(sprite.min + 1, sprite.max - 1)
			val t11 = texCoords.zw
			val t01 = texCoords.xw
			val t00 = texCoords.xy
			val t10 = texCoords.zy

			if (side.ordinal >= 3) {
				buffer.add(v00, t00, sideVector)
				buffer.add(v10, t10, sideVector)
				buffer.add(v01, t01, sideVector)

				buffer.add(v10, t10, sideVector)
				buffer.add(v11, t11, sideVector)
				buffer.add(v01, t01, sideVector)
			} else {
				buffer.add(v01, t01, sideVector)
				buffer.add(v10, t10, sideVector)
				buffer.add(v00, t00, sideVector)

				buffer.add(v01, t01, sideVector)
				buffer.add(v11, t11, sideVector)
				buffer.add(v10, t10, sideVector)
			}
		}
	}

	private enum class Side(val axisIndex: Int, val offsetX: Int, val offsetY: Int, val offsetZ: Int) {
		POSITIVE_X(0, 1, 0, 0),
		POSITIVE_Y(1, 0, 1, 0),
		POSITIVE_Z(2, 0, 0, 1),
		NEGATIVE_X(0, -1, 0, 0),
		NEGATIVE_Y(1, 0, -1, 0),
		NEGATIVE_Z(2, 0, 0, -1);

		val inverse: Side
			get() = values()[(ordinal + 3) % 6]
		val vector: Vector3f
			get() = Vector3f(offsetX.toFloat(), offsetY.toFloat(), offsetZ.toFloat())

	}

	private fun isFaceVisible(x: Int, y: Int, z: Int, side: Side): Boolean {
		if (chunk[x, y, z].isEmpty)
			return false
		val newX = x + side.offsetX
		val newY = y + side.offsetY
		val newZ = z + side.offsetZ
		if(newX < 0 || newY < 0 || newZ < 0 || newX >= chunk.size || newY >= chunk.size || newZ >= chunk.size)
			return chunk.parent.getRelative(chunk, newX, newY, newZ).isEmpty
		if (chunk[newX, newY, newZ].isEmpty)
			return true
		return false
	}

	private data class QuadShape(
		val c: Int,
		var minA: Int = Int.MAX_VALUE,
		var minB: Int = Int.MAX_VALUE,
		var maxA: Int = Int.MIN_VALUE,
		var maxB: Int = Int.MIN_VALUE
	) {

		val width: Int
			get() = maxA - minA

		val height: Int
			get() = maxB - minB

		val isValid: Boolean
			get() = minA != Int.MAX_VALUE && minB != Int.MAX_VALUE && maxA != Int.MIN_VALUE && maxB != Int.MIN_VALUE

		fun refine(a: Int, b: Int) {
			minA = min(a, minA)
			minB = min(b, minB)
			maxA = max(a, maxA)
			maxB = max(b, maxB)
		}

	}

	private class QuadColors(val width: Int, val height: Int) {

		val colors = IntArray(width * height)

		operator fun set(x: Int, y: Int, color: Int) {
			colors[x + (y * width)] = color
		}

		operator fun get(x: Int, y: Int): Int {
			return colors[x + (y * width)]
		}

		fun subsection(startX: Int, startY: Int, newWidth: Int, newHeight: Int): QuadColors {
			// TODO: This was commented out. It appears to be for good reason, there were holes in models when this was uncommented.
//			if (newWidth == width && newHeight == height)
//				return this

			val new = QuadColors(newWidth, newHeight)

			for (x in 0 until newWidth)
				for (y in 0 until newHeight)
					new[x, y] = this[x + startX, y + startY]

			return new
		}

		fun subsection(shape: QuadShape): QuadColors {
			return subsection(shape.minA, shape.minB, shape.width + 1, shape.height + 1)
		}

		fun toImage(): BufferedImage {
			val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
			result.setRGB(0, 0, width, height, colors, 0, width)
			return result
		}

		fun clear() {
			colors.fill(0)
		}

		override fun toString(): String {
			return "QuadColors(width=$width, height=$height)"
		}

	}

	private data class Quad(val shape: QuadShape, val colors: QuadColors, val side: Side)

	private fun generateQuads(): List<Quad> {
		val quads = ArrayList<Quad>()

		val negativeColors = QuadColors(chunk.size, chunk.size)
		val positiveColors = QuadColors(chunk.size, chunk.size)

		generateQuadsForSideAndInverse(Side.POSITIVE_X, quads, positiveColors, negativeColors)
		generateQuadsForSideAndInverse(Side.POSITIVE_Y, quads, positiveColors, negativeColors)
		generateQuadsForSideAndInverse(Side.POSITIVE_Z, quads, positiveColors, negativeColors)

		return quads
	}

	private fun generateQuadsForSideAndInverse(
		side: Side,
		target: MutableCollection<Quad>,
		positiveColors: QuadColors,
		negativeColors: QuadColors
	) {
		val ic = side.axisIndex
		val ia = indexA[ic]
		val ib = indexB[ic]

		val inverseSide = side.inverse

		val size = chunk.size

		val v = Vector3i(0)

		for (c in 0 until size) {
			v[ic] = c

			val positiveShape = QuadShape(c)
			val negativeShape = QuadShape(c)

			for (a in 0 until size) {
				for (b in 0 until size) {
					v[ia] = a
					v[ib] = b
//					v[ic] = c

					if (isFaceVisible(v.x, v.y, v.z, side)) {
						val voxel = chunk[v.x, v.y, v.z]

						positiveShape.refine(a, b)
						positiveColors[a, b] = voxel.color
					}

					if (isFaceVisible(v.x, v.y, v.z, inverseSide)) {
						val voxel = chunk[v.x, v.y, v.z]

						negativeShape.refine(a, b)
						negativeColors[a, b] = voxel.color
					}
				}
			}

			if (positiveShape.isValid) {
				target.add(Quad(positiveShape, positiveColors.subsection(positiveShape), side))
				positiveColors.clear()
			}

			if (negativeShape.isValid) {
				target.add(Quad(negativeShape, negativeColors.subsection(negativeShape), inverseSide))
				negativeColors.clear()
			}
		}
	}

	private fun rearrange(vector: Vector3f, a: Int, b: Int, c: Int): Vector3f {
		val x = vector[a]
		val y = vector[b]
		val z = vector[c]

//		val result = Vector3f(0f)
		vector.x = x
		vector.y = y
		vector.z = z
		return vector
	}

	companion object {
		@JvmStatic
		private val indexA = intArrayOf(2, 0, 1)

		@JvmStatic
		private val indexB = intArrayOf(1, 2, 0)
	}
}