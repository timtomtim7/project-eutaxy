package blue.sparse.eutaxy.test

import blue.sparse.eutaxy.util.Perlin
import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.eutaxy.voxel.World
import blue.sparse.math.vectors.floats.HSBtoRGB
import blue.sparse.math.vectors.floats.Vector3f
import blue.sparse.math.vectors.floats.floor
import blue.sparse.math.vectors.floats.normalize

class PerlinSnakeScene: VoxelScene {
    override fun generate(world: World) {
        val pos = Vector3f(0f)
        for(i in 1..8192) {
            var x = Perlin.noise(i.toFloat(), 0f, 0f, 1, 1f, 0.1f) * 10f
            var y = Perlin.noise(x, i.toFloat(), 0f, 1, 1f, 0.1f) * 10f
            var z = Perlin.noise(x, y, i.toFloat(), 1, 1f, 0.1f) * 10f

            if(x + y + z == 0f)
                z = 1f

            pos += normalize(Vector3f(x, y, z))
//            println(pos)

            val voxel = Voxel(Vector3f(i.toFloat() / 8192f, 1f, 1f).HSBtoRGB())
            VoxelScene.sphere(world, floor(pos).toIntVector(), 8.5f) { voxel }
        }
    }

}