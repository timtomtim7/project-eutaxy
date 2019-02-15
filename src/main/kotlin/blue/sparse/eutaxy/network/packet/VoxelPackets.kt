package blue.sparse.eutaxy.network.packet

import blue.sparse.eutaxy.net.packet.PacketRegistry

object VoxelPackets {

    val registry = PacketRegistry()

    init {
        registry.add<ConnectPacket>()
        registry.add(ChunkPacket.Serializer)
        registry.add<RerenderWorldPacket>()
        registry.add<EditSinglePacket>()
        registry.add<EditSpherePacket>()
    }

}