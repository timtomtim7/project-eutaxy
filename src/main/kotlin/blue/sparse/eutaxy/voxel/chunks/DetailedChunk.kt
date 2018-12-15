package blue.sparse.eutaxy.voxel.chunks

import blue.sparse.engine.render.resource.model.VertexArray
import blue.sparse.engine.render.resource.model.VertexBuffer
import blue.sparse.engine.render.resource.model.VertexLayout
import blue.sparse.eutaxy.render.ChunkModel
import blue.sparse.eutaxy.render.texture.OfflineChunkTexture
import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.math.vectors.floats.*
import blue.sparse.math.vectors.ints.Vector3i
import java.awt.image.BufferedImage
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.max
import kotlin.math.min

class DetailedChunk(parent: ChunkParent, id: Long, size: Int) : VoxelChunk(parent, id, size) {

	private var solidCount = 0
	private val data = IntArray(volume)

	override fun get(index: Int): Voxel {
		if (index < 0 || index >= data.size)
			return Voxel(0)

		return Voxel(data[index])
	}

	override fun set(index: Int, voxel: Voxel) {
		if (index < 0 || index >= data.size)
			return

		val newID = voxel.color
		val oldID = data[index]
		if (oldID != newID) {
			data[index] = newID

			if (newID == 0)
				solidCount--
			else
				solidCount++
		}
	}

	fun fill(voxel: Voxel?) {
		data.fill(voxel?.color ?: 0)
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
		if (get(x, y, z).isEmpty)
			return false
		if (get(x + side.offsetX, y + side.offsetY, z + side.offsetZ).isEmpty)
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
			if (newWidth == width && newHeight == height)
				return this

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

		override fun toString(): String {
			return "QuadColors(width=$width, height=$height)"
		}
	}

	private data class Quad(val shape: QuadShape, val colors: QuadColors, val side: Side)

	private fun generateQuads(): List<Quad> {
		val quads = ArrayList<Quad>()

		generateQuadsForSideAndInverse(Side.POSITIVE_X, quads)
		generateQuadsForSideAndInverse(Side.POSITIVE_Y, quads)
		generateQuadsForSideAndInverse(Side.POSITIVE_Z, quads)

		return quads
	}

	private fun generateQuadsForSideAndInverse(side: Side, target: MutableCollection<Quad>) {
		val ic = side.axisIndex
		val ia = intArrayOf(2, 0, 1)[ic]
		val ib = intArrayOf(1, 2, 0)[ic]

		val inverseSide = side.inverse

		for (c in 0 until size) {
			val positiveShape = QuadShape(c)
			val negativeShape = QuadShape(c)
			val negativeColors = QuadColors(size, size)
			val positiveColors = QuadColors(size, size)
			val v = Vector3i(0)

			for (a in 0 until size) {
				for (b in 0 until size) {
					v[ia] = a
					v[ib] = b
					v[ic] = c

					if (isFaceVisible(v.x, v.y, v.z, side)) {
						val voxel = get(v.x, v.y, v.z)

						positiveShape.refine(a, b)
						positiveColors[a, b] = voxel.color
					}

					if (isFaceVisible(v.x, v.y, v.z, inverseSide)) {
						val voxel = get(v.x, v.y, v.z)

						negativeShape.refine(a, b)
						negativeColors[a, b] = voxel.color
					}
				}
			}

			if (positiveShape.isValid)
				target.add(Quad(positiveShape, positiveColors.subsection(positiveShape), side))

			if (negativeShape.isValid)
				target.add(Quad(negativeShape, negativeColors.subsection(negativeShape), inverseSide))
		}
	}

	fun generateModel(position: Vector3f): ChunkModel {
		val quads = generateQuads()
		val atlas = OfflineChunkTexture()

		val array = VertexArray()
		val layout = VertexLayout()
		layout.add<Vector3f>() // Position
		layout.add<Vector2f>() // Texture Coordinate
		layout.add<Vector3f>() // Normal

		val buffer = VertexBuffer()

		for (quad in quads) {
			val side = quad.side
			val sideVector = side.vector
			val ic = side.axisIndex
			val ia = intArrayOf(2, 0, 1)[ic]
			val ib = intArrayOf(1, 2, 0)[ic]


			val shape = quad.shape
			var c = shape.c.toFloat()
			if (side.ordinal < 3)
				c += 1f

			val minA = shape.minA.toFloat()
			val maxA = shape.maxA.toFloat() + 1f
			val minB = shape.minB.toFloat()
			val maxB = shape.maxB.toFloat() + 1f
			val v00 = rearrange(Vector3f(minA, minB, c), ia, ib, ic) / 4f
			val v10 = rearrange(Vector3f(maxA, minB, c), ia, ib, ic) / 4f
			val v11 = rearrange(Vector3f(maxA, maxB, c), ia, ib, ic) / 4f
			val v01 = rearrange(Vector3f(minA, maxB, c), ia, ib, ic) / 4f

			val sprite = atlas.addSprite(quad.colors.toImage())
			val texCoords = Vector4f(sprite.min.toFloatVector(), sprite.max.toFloatVector())
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

		array.add(buffer, layout)

		return ChunkModel(position, array, atlas.renderToTexture())
	}

	private fun rearrange(vector: Vector3f, a: Int, b: Int, c: Int): Vector3f {
		val result = Vector3f(0f)
		result.x = vector[a]
		result.y = vector[b]
		result.z = vector[c]
		return result
	}

	fun fromFile(file: File) {
		val inp = GZIPInputStream(file.inputStream())
		val buffer = ByteBuffer.wrap(inp.readAllBytes())
		inp.close()
		buffer.asIntBuffer().get(data)
	}

	fun toFile(file: File) {
		val out = GZIPOutputStream(file.outputStream())
		val buffer = ByteBuffer.allocate(data.size * 4)
		buffer.asIntBuffer().put(data)
		out.write(buffer.array())
		out.close()
	}
}
