package com.ceslab.firemesh.meshmodule.bluetoothmesh

import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import com.siliconlab.bluetoothmesh.adk.data_model.network.Network
import com.siliconlab.bluetoothmesh.adk.data_model.network.NetworkCreationException
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.GroupCreationException
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.Subnet
import com.siliconlab.bluetoothmesh.adk.data_model.subnet.SubnetCreationException
import timber.log.Timber

/**
 * Created by Viet Hua on 11/23/2020.
 */

class MeshNetworkManager(val bluetoothMeshManager: BluetoothMeshManager) {

    var network: Network? = null
    var subnet: Subnet? = null
    var group: Group? = null

    init {
        setupNetwork()
        setupSubnet()
        setupGroup()
        bluetoothMeshManager.currentNetwork = network
    }

    fun createSubnet(name: String): Subnet? {
        return network?.createSubnet(name)
    }

    fun createGroup(name: String, subnet: Subnet): Group? {
        return subnet.createGroup(name, null, null)
    }

    private fun setupNetwork() {
        Timber.d("setupNetwork")
        val networkList = bluetoothMeshManager.bluetoothMesh.networks
        if (networkList.isEmpty()) {
            try {
                network = bluetoothMeshManager.bluetoothMesh.createNetworkWithoutSubnet("Network")
            } catch (e: NetworkCreationException) {
                Timber.e(e.toString())
            }
        } else {
            network = networkList.iterator()
                .next() //Get the first element of a Set (first and only network)
        }

    }

    private fun setupSubnet() {
        Timber.d("setupSubnet")
        val subnetList = network?.subnets!!
        if (subnetList.isEmpty()) {
            try {
                subnet = network?.createSubnet("Demo Subnet")
            } catch (e: SubnetCreationException) {
                Timber.e(e.toString())
            }
        } else {
            subnet = subnetList.iterator().next() //Get the first element of a Set (first subnet)
        }
    }

    private fun setupGroup() {
        Timber.d("setupGroup")
        val groupList = subnet?.groups!!
        if (groupList.isEmpty()) {
            try {
                group = subnet?.createGroup("Demo Group", null, null)
            } catch (e: GroupCreationException) {
                Timber.e(e.toString())

            }
        }
    }
}