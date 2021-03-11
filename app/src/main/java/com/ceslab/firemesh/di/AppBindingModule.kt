package com.ceslab.firemesh.di

import com.ceslab.firemesh.presentation.group.GroupFragment
import com.ceslab.firemesh.presentation.group_list.GroupListFragment
import com.ceslab.firemesh.presentation.group_list.dialog.add_group.AddGroupDialog
import com.ceslab.firemesh.presentation.group_list.dialog.edit_group.EditGroupDialog
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.subnet.SubnetFragment
import com.ceslab.firemesh.presentation.subnet_list.SubnetListFragment
import com.ceslab.firemesh.presentation.subnet_list.dialog.add_subnet.AddSubnetDialog
import com.ceslab.firemesh.presentation.node.NodeFragment
import com.ceslab.firemesh.presentation.node.node_config.NodeConfigFragment
import com.ceslab.firemesh.presentation.node.node_config.dialog.ModelConfigDialog
import com.ceslab.firemesh.presentation.node.node_info.NodeInfoFragment
import com.ceslab.firemesh.presentation.node_list.NodeListFragment
import com.ceslab.firemesh.presentation.node_list.dialog.DeleteNodeDialog
import com.ceslab.firemesh.presentation.ota_list.OTAListActivity
import com.ceslab.firemesh.presentation.ota_config.OTAConfigActivity
import com.ceslab.firemesh.presentation.provision_list.dialog.ProvisionBottomDialog
import com.ceslab.firemesh.presentation.provision_list.ProvisionListFragment
import com.ceslab.firemesh.presentation.subnet_list.dialog.edit_subnet.EditSubnetDialog
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [ViewModelModule::class])
abstract class AppBindingModule {

    //ACTIVITIES
    @ContributesAndroidInjector
    abstract fun mainActivity() : MainActivity

    @ContributesAndroidInjector
    abstract fun otaListActivity() : OTAListActivity

    @ContributesAndroidInjector
    abstract fun otaConfigActivity() : OTAConfigActivity

    //FRAGMENTS
    @ContributesAndroidInjector
    abstract fun provisionListFragment(): ProvisionListFragment

    @ContributesAndroidInjector
    abstract fun subnetListFragment(): SubnetListFragment

    @ContributesAndroidInjector
    abstract fun subnetFragment(): SubnetFragment

    @ContributesAndroidInjector
    abstract fun groupListFragment(): GroupListFragment

    @ContributesAndroidInjector
    abstract fun groupFragment(): GroupFragment

    @ContributesAndroidInjector
    abstract fun nodeConfigFragment(): NodeConfigFragment

    @ContributesAndroidInjector
    abstract fun nodeInfoFragment(): NodeInfoFragment

    @ContributesAndroidInjector
    abstract fun nodeFragment(): NodeFragment

    @ContributesAndroidInjector
    abstract fun nodeListFragment(): NodeListFragment

    //DIALOGS
    @ContributesAndroidInjector
    abstract fun provisionBottomDialog(): ProvisionBottomDialog

    @ContributesAndroidInjector
    abstract fun addGroupDialog(): AddGroupDialog

    @ContributesAndroidInjector
    abstract fun editGroupDialog(): EditGroupDialog

    @ContributesAndroidInjector
    abstract fun addSubnetDialog(): AddSubnetDialog

    @ContributesAndroidInjector
    abstract fun editSubnetDialog(): EditSubnetDialog

    @ContributesAndroidInjector
    abstract fun deleteNodeDialog(): DeleteNodeDialog

    @ContributesAndroidInjector
    abstract fun modelConfigDialog(): ModelConfigDialog

}