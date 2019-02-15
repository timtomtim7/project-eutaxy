package blue.sparse.eutaxy.network

import blue.sparse.eutaxy.net.Server
import blue.sparse.eutaxy.network.packet.*
import blue.sparse.eutaxy.voxel.World

class VoxelServer(val world: World) {

    val server = Server(4096, "127.0.0.1", false, VoxelPackets.registry)

    init {
        server.registry.addHandler("packets") { client, packet ->
            when(packet) {
                is ConnectPacket -> {
                    world.chunks.forEach {
                        client.sendPacket(ChunkPacket(it))
                    }
                    client.sendPacket(RerenderWorldPacket())
                }
                is EditPacket -> packet.apply(world)
            }

            if(packet is RebroadcastPacket) {
                server.clients.forEach {
                    if(it != client)
                        it.sendPacket(packet)
                }
            }
        }

    }

}