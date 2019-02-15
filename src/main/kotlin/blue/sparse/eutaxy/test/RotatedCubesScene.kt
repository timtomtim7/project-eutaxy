package blue.sparse.eutaxy.test

import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.eutaxy.voxel.World
import blue.sparse.eutaxy.voxel.editing.CuboidRegion
import blue.sparse.math.FloatTransform
import blue.sparse.math.vectors.floats.*
import blue.sparse.math.vectors.ints.Vector3i
import kotlin.random.Random

class RotatedCubesScene : VoxelScene {
    override fun generate(world: World) {
        val lightDirection = normalize(Vector3f(0.3f, 1f, -0.4f))

        val size = 128
        val cuboidRegion = CuboidRegion(world, Vector3i(-size, -size, -size), Vector3i(size, size, size))
        val r = { Random.nextFloat() * 2f - 1f }

        val color = Vector3f(Random.nextFloat(), 1f, 1f).HSBtoRGB()
        val transform = FloatTransform().apply {
            rotateRad(
                Vector3f(r(), r(), r()),
                (Random.nextFloat() * Math.PI).toFloat()
            )
        }

        val transformed = cuboidRegion.transform(transform, false, false)
        transformed.forEach {
            val g = (it.toFloatVector()) / size.toFloat()
            val l = length(g)
            val d = dot(lightDirection, normalize(g)) * 0.5f + 0.5f
            if (l < 1.4f) {
                world[it] = Voxel(color * d)
            } else {
                val dl = l - 1.4f
                world[it] = Voxel(color * (d * (1f - dl)))
            }
        }
    }

}