package com.ceslab.firemesh.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.factory.ViewModelFactory
import com.ceslab.firemesh.factory.ViewModelKey
import com.ceslab.firemesh.presentation.group_list.GroupListViewModel
import com.ceslab.firemesh.presentation.group_list.dialog.add_group.AddGroupViewModel
import com.ceslab.firemesh.presentation.group_list.dialog.edit_group.EditGroupViewModel
import com.ceslab.firemesh.presentation.main.activity.MainActivityViewModel
import com.ceslab.firemesh.presentation.subnet.SubnetViewModel
import com.ceslab.firemesh.presentation.subnet_list.SubnetListViewModel
import com.ceslab.firemesh.presentation.subnet_list.dialog.add_subnet.AddSubnetViewModel
import com.ceslab.firemesh.presentation.node.NodeViewModel
import com.ceslab.firemesh.presentation.node.node_config.NodeConfigViewModel
import com.ceslab.firemesh.presentation.node.node_config.dialog.ModelConfigViewModel
import com.ceslab.firemesh.presentation.node.node_info.NodeInfoViewModel
import com.ceslab.firemesh.presentation.node_list.NodeListViewModel
import com.ceslab.firemesh.presentation.node_list.dialog.DeleteNodeDialogViewModel
import com.ceslab.firemesh.presentation.ota_list.OTAListViewModel
import com.ceslab.firemesh.presentation.provision_list.dialog.ProvisionDialogViewModel
import com.ceslab.firemesh.presentation.provision_list.ProvisionViewModel
import com.ceslab.firemesh.presentation.subnet_list.dialog.edit_subnet.EditSubnetViewModel
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
    @ViewModelKey(OTAListViewModel::class)
    abstract fun bindOTAListViewModel(otaListViewModel: OTAListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProvisionViewModel::class)
    abstract fun bindProvisionViewModel(provisionViewModel: ProvisionViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SubnetListViewModel::class)
    abstract fun bindSubnetListViewModel(subnetListViewModel: SubnetListViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SubnetViewModel::class)
    abstract fun bindSubnetViewModel(subnetViewModel: SubnetViewModel):ViewModel

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
    @ViewModelKey(EditGroupViewModel::class)
    abstract fun bindEditGroupDialogViewModel(editGroupViewModel: EditGroupViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AddSubnetViewModel::class)
    abstract fun bindAddSubnetDialogViewModel(addSubnetViewModel: AddSubnetViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ModelConfigViewModel::class)
    abstract fun bindModelConfigViewModel(modelConfigViewModel: ModelConfigViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EditSubnetViewModel::class)
    abstract fun bindEditSubnetDialogViewModel(editSubnetViewModel: EditSubnetViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DeleteNodeDialogViewModel::class)
    abstract fun bindDeleteNodeDialogViewModel(deleteNodeDialogViewModel: DeleteNodeDialogViewModel):ViewModel
}