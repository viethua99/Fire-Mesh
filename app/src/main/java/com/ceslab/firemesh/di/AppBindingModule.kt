package com.ceslab.firemesh.di

import com.ceslab.firemesh.presentation.group_list.GroupListFragment
import com.ceslab.firemesh.presentation.main.activity.MainActivity
import com.ceslab.firemesh.presentation.network_list.NetworkListFragment
import com.ceslab.firemesh.presentation.provision_dialog.ProvisionBottomDialog
import com.ceslab.firemesh.presentation.scan.ScanFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [ViewModelModule::class])
abstract class AppBindingModule {
    @ContributesAndroidInjector
    abstract fun mainActivity() : MainActivity

    @ContributesAndroidInjector
    abstract fun scanFragment(): ScanFragment

    @ContributesAndroidInjector
    abstract fun networkListFragment(): NetworkListFragment

    @ContributesAndroidInjector
    abstract fun groupListFragment(): GroupListFragment


    @ContributesAndroidInjector
    abstract fun provisionBottomDialog(): ProvisionBottomDialog

}