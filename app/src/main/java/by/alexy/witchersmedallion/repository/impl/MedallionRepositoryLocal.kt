package by.alexy.witchersmedallion.repository.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import by.alexy.witchersmedallion.domain.MedallionCalibrationSettings
import by.alexy.witchersmedallion.repository.MedallionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "medallion_settings")

@Singleton
class MedallionRepositoryLocal @Inject constructor(
    @ApplicationContext private val context: Context,
) : MedallionRepository {

    companion object {
        private val MAC_ADDRESSES = stringSetPreferencesKey("mac_addresses")
        private val CALIBRATION_COLD = intPreferencesKey("calibration_cold_rssi")
        private val CALIBRATION_WARM = intPreferencesKey("calibration_warm_rssi")
        private val CALIBRATION_HOT = intPreferencesKey("calibration_hot_rssi")
    }

    override suspend fun getRegisteredMacAddresses(): List<String> = context.dataStore.data.map { it[MAC_ADDRESSES]?.toList() ?: emptyList() }.first()

    override suspend fun updateRegisteredMacAddresses(macAddresses: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[MAC_ADDRESSES] = macAddresses.toSet()
        }
    }

    override suspend fun getMedallionRssi(): Int? = context.dataStore.data.map { it[CALIBRATION_HOT] }.first()

    override suspend fun getCalibrationSettings(): MedallionCalibrationSettings? = context.dataStore.data.map { prefs ->
        val cold = prefs[CALIBRATION_COLD]
        val warm = prefs[CALIBRATION_WARM]
        val hot = prefs[CALIBRATION_HOT]
        if (cold != null && warm != null && hot != null) {
            MedallionCalibrationSettings(cold, warm, hot)
        } else {
            null
        }
    }.first()

    override suspend fun setCalibrationSettings(settings: MedallionCalibrationSettings) {
        context.dataStore.edit { prefs ->
            prefs[CALIBRATION_COLD] = settings.coldRssi
            prefs[CALIBRATION_WARM] = settings.warmRssi
            prefs[CALIBRATION_HOT] = settings.hotRssi
        }
    }
}
