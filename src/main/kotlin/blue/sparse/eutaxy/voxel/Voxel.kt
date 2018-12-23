package blue.sparse.eutaxy.voxel

import blue.sparse.math.vectors.floats.Vector3f
import blue.sparse.math.vectors.floats.toIntRGB
import blue.sparse.math.vectors.floats.vectorFromIntRGB

inline class Voxel(val color: Int) {
	constructor(r: Int, g: Int, b: Int): this(0xFF000000.toInt() or (r shl 16) or (g shl 8) or b)

	constructor(rgb: Vector3f): this(0xFF000000.toInt() or rgb.toIntRGB())

	constructor(r: Float, g: Float, b: Float): this(Vector3f(r, g, b))

	operator fun times(rgb: Vector3f): Voxel {
		return Voxel(floatRGB * rgb)
	}

	val floatRGB: Vector3f
		get() = color.vectorFromIntRGB()

	val r: Int
		get() = (color shr 16) and 0xFF

	val g: Int
		get() = (color shr 8) and 0xFF

	val b: Int
		get() = color and 0xFF

	val isEmpty: Boolean
		get() = color == 0

	companion object {
		val empty = Voxel(0)
	}
}