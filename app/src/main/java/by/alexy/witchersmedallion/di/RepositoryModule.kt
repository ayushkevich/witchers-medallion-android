package by.alexy.witchersmedallion.di

import by.alexy.witchersmedallion.repository.MedallionRepository
import by.alexy.witchersmedallion.repository.bluetooth.BleRepository
import by.alexy.witchersmedallion.repository.bluetooth.impl.BleRepositoryImpl
import by.alexy.witchersmedallion.repository.impl.BleMedallionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindBleRepository(impl: BleRepositoryImpl): BleRepository

    @Binds
    abstract fun bindMedallionRepository(impl: BleMedallionRepository): MedallionRepository
}
