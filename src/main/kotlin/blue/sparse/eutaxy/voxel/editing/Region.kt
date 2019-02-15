package blue.sparse.eutaxy.voxel.editing

import blue.sparse.eutaxy.voxel.World
import blue.sparse.math.FloatTransform
import blue.sparse.math.vectors.floats.Vector3f
import blue.sparse.math.vectors.ints.Vector3i

interface Region : Iterable<Vector3i> {
    val world: World
    val min: Vector3i
    val max: Vector3i

    val width: Int get() = (max.x - min.x) + 1
    val height: Int get() = (max.y - min.y) + 1
    val depth: Int get() = (max.z - min.z) + 1

    operator fun contains(position: Vector3f): Boolean
    operator fun contains(position: Vector3i) = contains(position.toFloatVector() + 0.5f)

    override fun iterator(): Iterator<Vector3i> = DefaultRegionIterator(this)

    fun transform(
        transform: FloatTransform,
        invert: Boolean = false,
        useOriginalOrigin: Boolean = true
    ): TransformedRegion {
        return TransformedRegion(this, transform, invert, useOriginalOrigin)
    }
}