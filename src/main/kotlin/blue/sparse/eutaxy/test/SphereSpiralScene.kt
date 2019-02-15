package blue.sparse.eutaxy.test

import blue.sparse.eutaxy.test.VoxelScene.Companion.sphere
import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.eutaxy.voxel.World
import blue.sparse.math.cos
import blue.sparse.math.sin
import blue.sparse.math.vectors.floats.HSBtoRGB
import blue.sparse.math.vectors.floats.Vector3f
import blue.sparse.math.vectors.floats.dot
import blue.sparse.math.vectors.floats.normalize
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.random.Random

class SphereSpiralScene : VoxelScene {
    override fun generate(world: World) {
        val lightDirection2 = normalize(Vector3f(0.3f, 1f, -0.4f))

        val count = 32
        val radius = 1024
        val angle = ((PI * 2) / count).toFloat()

//        thread {
            for (i in 0 until count) {
                println(i)
                for (ir in 1..8) {
                    val irf = ir / 16f

                    val x = sin(i * angle + irf * 1.5f) * radius * irf
                    val z = cos(i * angle + irf * 1.5f) * radius * irf
                    val y = Random.nextFloat() * 32f//sin(irf * PI.toFloat() * 2 + i * 0.25f) * 32f
                    val p = i / count.toFloat()
                    val color = Vector3f(p, 1f, 1f).HSBtoRGB()

                    val origin = Vector3f(x, y + 32, z)
                    val size = 28f * irf + 4f
                    sphere(world, origin.toIntVector(), size) {
                        val f = (it.toFloatVector() - origin) / size
                        val d = dot(lightDirection2, normalize(f)) * 0.4f + 0.6f
                        Voxel(color * d)
                    }
                }
            }
//        }
    }
}