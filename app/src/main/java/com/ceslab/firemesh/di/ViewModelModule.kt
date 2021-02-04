package com.ceslab.firemesh.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ceslab.firemesh.factory.ViewModelFactory
import com.ceslab.firemesh.factory.ViewModelKey
import com.ceslab.firemesh.presentation.group_list.GroupListViewModel
import com.ceslab.firemesh.presentation.main.activity.MainActivityViewModel
import com.ceslab.firemesh.presentation.network_list.NetworkListViewModel
import com.ceslab.firemesh.presentation.provision_dialog.ProvisionBottomDialog
import com.ceslab.firemesh.presentation.provision_dialog.ProvisionDialogViewModel
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
    @ViewModelKey(GroupListViewModel::class)
    abstract fun bindGroupListViewModel(groupListViewModel: GroupListViewModel):ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProvisionDialogViewModel::class)
    abstract fun bindProvisionDialogViewModel(provisionDialogViewModel: ProvisionDialogViewModel):ViewModel
}