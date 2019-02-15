package blue.sparse.eutaxy.voxel.editing

import blue.sparse.math.FloatTransform
import blue.sparse.math.vectors.floats.Vector3f
import blue.sparse.math.vectors.floats.floor
import blue.sparse.math.vectors.ints.Vector3i

class TransformedRegion(
    val originalRegion: Region,
    transform: FloatTransform,
    invert: Boolean = false,
    val useOriginalOrigin: Boolean = true
) :
    Region {
    override val world = originalRegion.world

    private val matrix = if (invert) transform.inverseMatrix else transform.matrix
    private val inverse = if (invert) transform.matrix else transform.inverseMatrix

    override val min: Vector3i
    override val max: Vector3i

    init {
        val lll = originalRegion.min.toFloatVector()
        val hhh = originalRegion.max.toFloatVector()
        val hhl = Vector3f(hhh.x, hhh.y, lll.z)
        val hll = Vector3f(hhh.x, lll.y, lll.z)
        val llh = Vector3f(lll.x, lll.y, hhh.z)
        val lhh = Vector3f(lll.x, hhh.y, hhh.z)
        val lhl = Vector3f(lll.x, hhh.y, lll.z)
        val hlh = Vector3f(hhh.x, lll.y, hhh.z)

        val transformed = setOf(lll, hhh, hhl, hll, llh, lhh, lhl, hlh).map(this::invert)

        val minX = transformed.minBy { it.x }!!.x
        val minY = transformed.minBy { it.y }!!.y
        val minZ = transformed.minBy { it.z }!!.z
        min = floor(Vector3f(minX, minY, minZ)).toIntVector()

        val maxX = transformed.maxBy { it.x }!!.x
        val maxY = transformed.maxBy { it.y }!!.y
        val maxZ = transformed.maxBy { it.z }!!.z
        max = floor(Vector3f(maxX, maxY, maxZ)).toIntVector()
    }

    override fun contains(position: Vector3f): Boolean {
        return transform(position) in originalRegion
    }

    private fun transform(position: Vector3f): Vector3f {
        if (useOriginalOrigin)
            return (inverse * (position - originalRegion.min.toFloatVector())) + originalRegion.min.toFloatVector()

        return inverse * position
    }

    private fun invert(position: Vector3f): Vector3f {
        if (useOriginalOrigin)
            return (matrix * (position - originalRegion.min.toFloatVector())) + originalRegion.min.toFloatVector()

        return matrix * position
    }
}