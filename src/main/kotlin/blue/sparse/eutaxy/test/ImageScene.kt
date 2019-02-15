package blue.sparse.eutaxy.test

import blue.sparse.engine.asset.Asset
import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.eutaxy.voxel.World
import blue.sparse.math.clamp
import blue.sparse.math.vectors.floats.Vector3f
import blue.sparse.math.vectors.floats.dot
import blue.sparse.math.vectors.floats.normalize
import blue.sparse.math.vectors.floats.vectorFromIntRGB
import java.awt.Color

class ImageScene: VoxelScene {
    override fun generate(world: World) {
        val image = Asset["textures/bricks/diffuse.jpg"].readImage()
        val displaceMap = Asset["textures/bricks/displacement.jpg"].readImage()
        val normalMap = Asset["textures/bricks/normal.jpg"].readImage()

        //normalize(vec3(0.3,1.0,-0.4))
        val lightDirection = normalize(Vector3f(0.3f, -0.4f, 1.0f))

        val count = 1
        val scale = 3
        val imageWidth = clamp(image.width / scale, 0, 2048)
        val imageHeight = clamp(image.height / scale, 0, 2048)
//		val imageWidth = image.width / scale
//		val imageHeight = image.height / scale

        println("$imageWidth * $imageHeight")

        var voxelCount = 0

        println()
        for (cx in 0 until count) {
            for (cz in 0 until count) {
                for (x in 0 until imageWidth) {
                    print("\r${(x / imageWidth.toDouble()) * 100}%")
                    for (z in 0 until imageHeight) {
                        val imageX = x * scale
                        val imageY = (imageHeight - z - 1) * scale
                        val color = image.getRGB(imageX, imageY)
                        if (color == 0)
                            continue

                        val offsetRGB = Color(displaceMap.getRGB(imageX, imageY))
                        val offset = (offsetRGB.red + offsetRGB.green + offsetRGB.blue) / 3

                        val normal = normalize(normalMap.getRGB(imageX, imageY).vectorFromIntRGB() * 2f - 1f)
                        val brightness = dot(normal, lightDirection)

                        val voxel = Voxel(color or 0xFF000000.toInt()) * Vector3f(brightness)
//						val voxel = Voxel(Vector3f(brightness))

                        for (i in 0 until (offset / (3 * scale)) + 1) {
                            val wx = x + (cx * imageWidth)
                            val wy = i
                            val wz = z + (cz * imageHeight)
                            world[wx, wy, wz] = voxel
//							world[wx, wz, -wy] = voxel
                            voxelCount++
                        }
                    }
                }
            }
        }
    }
}