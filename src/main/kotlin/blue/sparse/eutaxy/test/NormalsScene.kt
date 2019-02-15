package blue.sparse.eutaxy.test

import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.eutaxy.voxel.World
import blue.sparse.eutaxy.voxel.editing.CuboidRegion
import blue.sparse.eutaxy.voxel.editing.TransformedRegion
import blue.sparse.math.FloatTransform
import blue.sparse.math.vectors.floats.*
import blue.sparse.math.vectors.ints.Vector3i
import kotlin.concurrent.thread
import kotlin.random.Random

class NormalsScene : VoxelScene {

	lateinit var transformed: TransformedRegion

	val lightDirection = normalize(Vector3f(0.3f, 1f, -0.4f))
	val r = { Random.nextFloat() * 2f - 1f }
	val color = Vector3f(Random.nextFloat(), 1f, 1f).HSBtoRGB()
	val voxel = Voxel(color)

	override fun generate(world: World) {

		val size = 32
		val cuboidRegion = CuboidRegion(world, Vector3i(-size, -size, -size), Vector3i(size, size, size))

		val transform = FloatTransform().apply {
			rotateRad(
					Vector3f(r(), r(), r()),
					(Random.nextFloat() * Math.PI).toFloat()
			)
		}
		transformed = cuboidRegion.transform(transform, false, false)
		transformed.forEach {
			world[it] = voxel
		}

		recalculateNormals(world)
	}

	fun recalculateNormals(world: World) {
		thread {
			println("Recalculating...")
			for(x in -256..256) {
				for(y in -256..256) {
					for(z in -256..256) {
						val pos = Vector3i(x, y, z)
						val normal = calculateNormal(world, pos) ?: continue
						val brightness = dot(normal, lightDirection) * 0.5f + 0.5f
						world[pos] = voxel * Vector3f(brightness)
					}
				}
			}
			println("Done recalculating.")
		}
	}

	fun calculateNormal(world: World, position: Vector3i): Vector3f? {
		if (world[position].isEmpty)
			return null

		val sum = Vector3f(0f)
		var count = 0
		val size = 3

		for (x in -size..size) {
			for (y in -size..size) {
				for (z in -size..size) {
					val offset = Vector3i(x, y, z)

					val pos = position + offset
					if (!world[pos].isEmpty)
						continue

					sum += normalize(offset.toFloatVector())
					count++
				}
			}
		}

		if (count == 0)
			return null

		return normalize(sum / count.toFloat())
	}
}