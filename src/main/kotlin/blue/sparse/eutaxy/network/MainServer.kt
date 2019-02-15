package blue.sparse.eutaxy.network

import blue.sparse.eutaxy.test.SphereSpiralScene
import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.eutaxy.voxel.World
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmName

class NotVoxel(val color: Int)

fun main() {
    val clazz1 = Voxel::class
    val clazz2 = NotVoxel::class

    println(clazz1)
    println(clazz1.simpleName)
    println(clazz1.qualifiedName)
    println(clazz1.jvmName)
    println(clazz1.java.canonicalName)
    println(clazz1.primaryConstructor)

    println("----------")

    println(clazz2)
    println(clazz2.simpleName)
    println(clazz2.qualifiedName)
    println(clazz2.jvmName)
    println(clazz2.java.canonicalName)
    println(clazz2.primaryConstructor)

//    println("Creating world...")
//    val world = World(6)
//    println("Generating scene...")
//    SphereSpiralScene().generate(world)
//    println("Starting server...")
//    val server = VoxelServer(world)
////    val time = measureTimeMillis {
////        val packet = ChunkPacket(world.getChunk(0, 0, 0))
////        ChunkPacket.Serializer.serialize(VoxelPackets.registry[ChunkPacket::class]!!, packet)
////    }
////    println("Took ${time}ms to generate chunk packet")
//
//    println("Main thread done.")
}