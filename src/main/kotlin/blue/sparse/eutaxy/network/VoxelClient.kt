package blue.sparse.eutaxy.network

import blue.sparse.eutaxy.net.Client
import blue.sparse.eutaxy.network.packet.*
import blue.sparse.eutaxy.voxel.World
import java.util.*

class VoxelClient(val world: World, address: String, port: Int) {

    val client = Client(address, port, VoxelPackets.registry)

    init {
        client.registry.addHandler("packets") { _, packet ->
            when(packet) {
                is ChunkPacket -> packet.apply(world)
                is EditPacket -> packet.apply(world)
                is RerenderWorldPacket -> world.queueAll()
            }
        }

        client.sendPacket(ConnectPacket(UUID.randomUUID().toString()))
    }

}