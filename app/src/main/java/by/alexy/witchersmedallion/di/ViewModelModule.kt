package by.alexy.witchersmedallion.di

import by.alexy.witchersmedallion.repository.MedallionRepository
import by.alexy.witchersmedallion.repository.bluetooth.BleRepository
import by.alexy.witchersmedallion.repository.bluetooth.impl.BleRepositoryImpl
import by.alexy.witchersmedallion.repository.impl.MedallionRepositoryStub
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelModule {
    @Binds
    abstract fun bindBleRepository(
        impl: BleRepositoryImpl
    ): BleRepository

    @Binds
    abstract fun bindMedallionRepository(
        impl: MedallionRepositoryStub
    ): MedallionRepository

}
