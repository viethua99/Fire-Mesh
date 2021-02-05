package com.ceslab.firemesh.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.factory.ViewModelFactory
import com.ceslab.firemesh.factory.ViewModelKey
import com.ceslab.firemesh.presentation.group_list.GroupListViewModel
import com.ceslab.firemesh.presentation.group_list.dialog.AddGroupViewModel
import com.ceslab.firemesh.presentation.main.activity.MainActivityViewModel
import com.ceslab.firemesh.presentation.network.NetworkViewModel
import com.ceslab.firemesh.presentation.network_list.NetworkListViewModel
import com.ceslab.firemesh.presentation.network_list.dialog.AddNetworkViewModel
import com.ceslab.firemesh.presentation.node.NodeViewModel
import com.ceslab.firemesh.presentation.node.node_config.NodeConfigViewModel
import com.ceslab.firemesh.presentation.node.node_info.NodeInfoViewModel
import com.ceslab.firemesh.presentation.node_list.NodeListViewModel
import com.ceslab.firemesh.presentation.scan.dialog.ProvisionDialogViewModel
import com.ceslab.firemesh.presentation.scan.ScanViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {
    @Binds
    abstract fun bindViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(MainActivityViewModel::class)
    abstract fun bindMainActivityModel(mainActivityViewModel: MainActivityViewModel): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(ScanViewModel::class)
    abstract fun bindScanViewModel(scanViewModel: ScanViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NetworkListViewModel::class)
    abstract fun bindNetworkListViewModel(networkListViewModel: NetworkListViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NetworkViewModel::class)
    abstract fun bindNetworkViewModel(networkViewModel: NetworkViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GroupListViewModel::class)
    abstract fun bindGroupListViewModel(groupListViewModel: GroupListViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NodeConfigViewModel::class)
    abstract fun bindNodeConfigViewModel(nodeConfigViewModel: NodeConfigViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NodeInfoViewModel::class)
    abstract fun bindNodeInfoViewModel(nodeInfoViewModel: NodeInfoViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NodeViewModel::class)
    abstract fun bindNodeViewModel(nodeViewModel: NodeViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NodeListViewModel::class)
    abstract fun bindNodeListViewModel(nodeListViewModel: NodeListViewModel):ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(ProvisionDialogViewModel::class)
    abstract fun bindProvisionDialogViewModel(provisionDialogViewModel: ProvisionDialogViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddGroupViewModel::class)
    abstract fun bindAddGroupDialogViewModel(addGroupViewModel: AddGroupViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddNetworkViewModel::class)
    abstract fun bindAddNetworkDialogViewModel(addNetworkViewModel: AddNetworkViewModel):ViewModel
}