package blue.sparse.eutaxy.voxel.editing

import blue.sparse.math.vectors.ints.Vector3i

class DefaultRegionIterator(val region: Region) : Iterator<Vector3i> {
    private val cuboid = CuboidRegion(region.world, region.min, region.max)
    private var index = 0

    override fun hasNext(): Boolean {
        if (index >= cuboid.size) return false

        while (cuboid[index] !in region) {
            index++
            if (index >= cuboid.size) return false
        }

        return true
    }

    override fun next(): Vector3i {
        var n = cuboid[index++]

        while (n !in region)
            n = cuboid[index++]

        return n
    }
}