package by.alexy.witchersmedallion.domain

data class BleScanConfig(
    val minRssi: Int = Int.MIN_VALUE,
    val scanDurationMs: Long = 30_000L
)
