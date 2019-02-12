package blue.sparse.eutaxy.render.texture

import blue.sparse.engine.render.resource.Texture
import blue.sparse.math.vectors.ints.Vector2i
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

class OfflineChunkTexture {

	private val sprites = ArrayList<Sprite>()
	val size = Vector2i(0, 0)

	fun addSprite(width: Int, height: Int, colors: IntArray): Sprite {
		val size = Vector2i(width, height) + 2
		val position = findAvailableSpace(size)

//		texture.subImage(image, position)
		val sprite = Sprite(position, size, colors)
		sprites.add(sprite)

		return sprite
	}

	fun renderToBuffer(): ByteBuffer {
		val sizeX = max(size.x, 1)
		val sizeY = max(size.y, 1)
		val result = ByteBuffer.allocateDirect(sizeX * sizeY * 4)//.order(ByteOrder.nativeOrder())

		for(sprite in sprites) {
			val minX = sprite.min.x + 1
			val minY = sprite.min.y + 1
			val width = sprite.size.x - 2
			val height = sprite.size.y - 2
			val colors = sprite.colors

			for(x in 0 until width) {
				for(y in 0 until height) {
					val color = colors[x + y * width]

					val bufferIndex = ((x + minX) + (y + minY) * sizeX)

					// Reorder from ARGB to RGBA
					val alpha = (color shr 24) and 0xFF
					result.putInt(bufferIndex * 4, (color shl 8) or alpha)
				}
			}
		}

		result.flip()
		return result
	}

	fun renderToImage(): BufferedImage {
		val result = BufferedImage(max(size.x, 1), max(size.y, 1), BufferedImage.TYPE_INT_ARGB)

		for (sprite in sprites) {
			// Offset min by 1 to leave 1 pixel border
			// Subtract 2 from size to balance the 2 added to size for the 1 pixel border
			result.setRGB(
				sprite.min.x + 1,
				sprite.min.y + 1,
				sprite.size.x - 2,
				sprite.size.y - 2,
				sprite.colors,
				0,
				sprite.size.x - 2
			)
		}

//		val f = File("atlas.png")
//		if(!f.exists())
//			ImageIO.write(result, "PNG", f)

		return result
	}

	fun renderToTexture(): Texture {
		return Texture(renderToImage()).apply {
			bind(0) {
				nearestFiltering()
				clampToEdge()
			}
		}
	}

	private fun findAvailableSpace(spriteSize: Vector2i): Vector2i {
		val min = Vector2i(0)
		var free = sprites.isEmpty()//fits(min, min + spriteSize)

		if (free)
			return min

		for (sprite in sprites.asReversed()) {
			min.x = sprite.max.x
			min.y = sprite.min.y
			free = fits(min, min + spriteSize)
			if (free) break

			min.x = sprite.min.x
			min.y = sprite.max.y
			free = fits(min, min + spriteSize)
			if (free) break

			min.x = sprite.max.x
			min.y = sprite.max.y
			free = fits(min, min + spriteSize)
			if (free) break
		}

		if (!free) {
			if (size.x > size.y) {
				size.y += spriteSize.y
			} else {
				size.x += spriteSize.x
			}
			return findAvailableSpace(spriteSize)
		}

		return min
	}

	private fun fits(min: Vector2i, max: Vector2i): Boolean {
		if (min.x < 0 || min.y < 0 || max.x > size.x || max.y > size.y)
			return false

		if (findIntersectingSprite(min, max) != null)
			return false

		return true
	}

	private fun findIntersectingSprite(min: Vector2i, max: Vector2i): Sprite? {
		val leftA = min.x
		val rightA = max.x
		val topA = min.y
		val bottomA = max.y

		for (sprite in sprites) {
			val leftB = sprite.min.x
			val rightB = sprite.max.x
			val topB = sprite.min.y
			val bottomB = sprite.max.y

			if (leftA < rightB && rightA > leftB && topA < bottomB && bottomA > topB)
				return sprite
		}

		return null
	}

	inner class Sprite(pos: Vector2i, size: Vector2i, val colors: IntArray) {
		val size = size.clone()

		val min = pos.clone()
//			get() = field.clone()

		val max = pos + size
//			get() = field.clone()

//		val textureCoords: Vector4f
//			get() = Vector4f(
//				min.toFloatVector() / size.toFloatVector(),
//				max.toFloatVector() / size.toFloatVector()
//			)

//		val atlas = this@OfflineChunkTexture
	}

}