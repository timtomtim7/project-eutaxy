package blue.sparse.eutaxy.test

import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.eutaxy.voxel.World
import blue.sparse.math.vectors.floats.lengthSquared
import blue.sparse.math.vectors.ints.Vector3i
import kotlin.math.ceil

interface VoxelScene {
    fun generate(world: World)

    companion object {
        inline fun sphere(world: World, origin: Vector3i, radius: Float, apply: (Vector3i) -> Voxel) {
            val ri = ceil(radius).toInt()
            for (x in -ri..ri) {
                for (y in -ri..ri) {
                    for (z in -ri..ri) {
                        val v = Vector3i(x, y, z)
                        if (lengthSquared(v.toFloatVector()) >= radius * radius)
                            continue

                        val position = origin + v
                        world[position] = apply(position)
                    }
                }
            }
        }
    }
}