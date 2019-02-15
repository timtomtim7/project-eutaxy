package blue.sparse.eutaxy.network.packet

import blue.sparse.eutaxy.net.packet.Packet
import blue.sparse.eutaxy.net.packet.PacketRegistry
import blue.sparse.eutaxy.net.packet.PacketSerializer
import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.eutaxy.voxel.World
import blue.sparse.eutaxy.voxel.chunks.ChunkParent
import blue.sparse.eutaxy.voxel.chunks.EmptyChunk
import blue.sparse.eutaxy.voxel.chunks.VoxelChunk
import blue.sparse.math.vectors.ints.Vector3i
import org.xerial.snappy.Snappy
import java.nio.ByteBuffer

class ChunkPacket(
    val position: Vector3i,
    val data: IntArray
) : Packet {

    constructor(chunk: VoxelChunk): this(chunk.parentRelativePosition, IntArray(64 * 64 * 64)) {
        var i = 0
        for(x in 0 until 64) {
            for (y in 0 until 64) {
                for (z in 0 until 64) {
                    data[i++] = chunk[x, y, z].color
                }
            }
        }
    }

    fun apply(world: World) {
        var newChunk: VoxelChunk

        val newParent = object: ChunkParent {
            override fun replace(chunk: VoxelChunk) {
                newChunk = chunk
            }

            override fun getRelative(chunk: VoxelChunk, x: Int, y: Int, z: Int): Voxel {
                throw UnsupportedOperationException()
            }
        }
        newChunk = EmptyChunk(newParent, position, 64)

        var i = 0
        for(x in 0 until 64) {
            for(y in 0 until 64)  {
                for(z in 0 until 64) {
                    val value = data[i++]
                    if(value == 0)
                        continue
                    newChunk[x, y, z] = Voxel(value)
                }
            }
        }

        newChunk.parent = world
        world.replaceNoQueue(newChunk)
    }

    private fun getIndex(x: Int, y: Int, z: Int): Int {
        return x + (y * 64) + (z * 64 * 64)
    }

    companion object Serializer : PacketSerializer<ChunkPacket> {
        val buffer1 = ThreadLocal.withInitial { ByteBuffer.allocateDirect(64 * 64 * 64 * 4 + 3 * 4) }
        val buffer2 = ThreadLocal.withInitial { ByteBuffer.allocateDirect(64 * 64 * 64 * 4 + 3 * 4) }

        override fun deserialize(
            registeredPacket: PacketRegistry.RegisteredPacket<*>,
            buffer: ByteBuffer
        ): ChunkPacket {
            val uncompressed = buffer1.get()
            Snappy.uncompress(buffer, uncompressed)

            val volume = 64 * 64 * 64
            val data = IntArray(volume)
            val intBuffer = uncompressed.asIntBuffer()
            val position = Vector3i(intBuffer.get(), intBuffer.get(), intBuffer.get())
            intBuffer.get(data)

            return ChunkPacket(position, data)
        }

        override fun serialize(
            registeredPacket: PacketRegistry.RegisteredPacket<ChunkPacket>,
            packet: ChunkPacket
        ): ByteBuffer {
            val uncompressed = buffer1.get()
            uncompressed.clear()
            val intBuffer = uncompressed.asIntBuffer()
            intBuffer.put(packet.position.x)
            intBuffer.put(packet.position.y)
            intBuffer.put(packet.position.z)
            intBuffer.put(packet.data)

            val compressed = buffer2.get()
            compressed.clear()

            Snappy.compress(uncompressed, compressed)
            return compressed
        }

    }

}