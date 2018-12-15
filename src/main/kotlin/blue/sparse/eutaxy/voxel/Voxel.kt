package blue.sparse.eutaxy.voxel

inline class Voxel(val color: Int) {
	constructor(r: Int, g: Int, b: Int): this(0xFF000000.toInt() or (r shl 16) or (g shl 8) or b)

	val isEmpty: Boolean
		get() = color == 0
}