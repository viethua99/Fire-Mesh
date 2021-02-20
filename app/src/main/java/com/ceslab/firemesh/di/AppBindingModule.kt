package com.ceslab.firemesh.di

import com.ceslab.firemesh.presentation.group_list.GroupListFragment
import com.ceslab.firemesh.presentation.group_list.dialog.AddGroupDialog
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.subnet.SubnetFragment
import com.ceslab.firemesh.presentation.subnet_list.SubnetListFragment
import com.ceslab.firemesh.presentation.subnet_list.dialog.AddSubnetDialog
import com.ceslab.firemesh.presentation.node.NodeFragment
import com.ceslab.firemesh.presentation.ota_list.dialog.OTAConfigDialog
import com.ceslab.firemesh.presentation.node.node_config.NodeConfigFragment
import com.ceslab.firemesh.presentation.node.node_info.NodeInfoFragment
import com.ceslab.firemesh.presentation.node_list.NodeListFragment
import com.ceslab.firemesh.presentation.ota_list.OTAListFragment
import com.ceslab.firemesh.presentation.provision_list.dialog.ProvisionBottomDialog
import com.ceslab.firemesh.presentation.provision_list.ProvisionListFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [ViewModelModule::class])
abstract class AppBindingModule {

    //ACTIVITIES
    @ContributesAndroidInjector
    abstract fun mainActivity() : MainActivity

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
    abstract fun nodeConfigFragment(): NodeConfigFragment

    @ContributesAndroidInjector
    abstract fun nodeInfoFragment(): NodeInfoFragment

    @ContributesAndroidInjector
    abstract fun nodeFragment(): NodeFragment

    @ContributesAndroidInjector
    abstract fun nodeListFragment(): NodeListFragment

    @ContributesAndroidInjector
    abstract fun otaListFragment(): OTAListFragment

    //DIALOGS
    @ContributesAndroidInjector
    abstract fun provisionBottomDialog(): ProvisionBottomDialog


    @ContributesAndroidInjector
    abstract fun otaConfigDialog(): OTAConfigDialog

    @ContributesAndroidInjector
    abstract fun addGroupDialog(): AddGroupDialog

    @ContributesAndroidInjector
    abstract fun addNetworkDialog(): AddSubnetDialog

}