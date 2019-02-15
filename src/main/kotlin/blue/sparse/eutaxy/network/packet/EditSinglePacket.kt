package blue.sparse.eutaxy.network.packet

import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.eutaxy.voxel.World
import blue.sparse.math.vectors.ints.Vector3i

data class EditSinglePacket(
    val position: Vector3i,
    val voxel: Voxel
) : RebroadcastPacket, EditPacket {

    override fun apply(world: World) {
        world[position] = voxel
    }

}