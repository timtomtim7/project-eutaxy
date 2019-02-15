package blue.sparse.eutaxy.network.packet

import blue.sparse.eutaxy.voxel.World

interface EditPacket {
    fun apply(world: World)
}