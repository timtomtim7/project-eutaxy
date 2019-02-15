package blue.sparse.eutaxy.voxel.editing

import blue.sparse.math.vectors.ints.Vector3i

interface RandomAccessRegion : Region, RandomAccess {
    val size: Int get() = width * height * depth

    override fun iterator(): Iterator<Vector3i> = RegionIterator(this)
    operator fun get(index: Int): Vector3i

    class RegionIterator(val region: RandomAccessRegion) : Iterator<Vector3i> {
        private var index: Int = 0

        override fun hasNext() = index < region.size
        override fun next() = region[index++]
    }
}