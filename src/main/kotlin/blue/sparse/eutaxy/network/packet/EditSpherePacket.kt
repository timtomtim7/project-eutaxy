package blue.sparse.eutaxy.network.packet

import blue.sparse.eutaxy.test.VoxelScene
import blue.sparse.eutaxy.voxel.Voxel
import blue.sparse.eutaxy.voxel.World
import blue.sparse.math.vectors.ints.Vector3i

data class EditSpherePacket(
    val position: Vector3i,
    val radius: Float,
    val voxel: Voxel,
    val mask: Voxel,
    val useMask: Boolean,
    val invertMask: Boolean
) : RebroadcastPacket, EditPacket {

    override fun apply(world: World) {
        VoxelScene.sphere(world, position, radius) {
            if(useMask) {
                val old = world[it]
                if((old == mask) xor invertMask)
                    voxel
                else
                    old
            }else{
                voxel
            }
        }
    }

}