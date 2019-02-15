package blue.sparse.eutaxy.voxel.editing

import blue.sparse.eutaxy.voxel.World
import blue.sparse.math.vectors.floats.Vector3f
import blue.sparse.math.vectors.floats.floor
import blue.sparse.math.vectors.ints.Vector3i
import kotlin.math.max
import kotlin.math.min

class CuboidRegion(override val world: World, a: Vector3i, b: Vector3i) : RandomAccessRegion {
    override val min = Vector3i(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z))
    override val max = Vector3i(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))

    override fun contains(position: Vector3f): Boolean {
        return floor(position).run { x >= min.x && y >= min.y && z >= min.z && x <= max.x && y <= max.y && z <= max.z }
    }

    override fun get(index: Int): Vector3i {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException(index.toString())

        val x = index % width
        val y = (index / width) % height
        val z = ((index / width) / height) % depth

        return min + Vector3i(x, y, z)
    }
}