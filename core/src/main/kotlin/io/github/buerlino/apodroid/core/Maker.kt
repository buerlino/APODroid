package io.github.buerlino.apodroid.core

/**
 * The phone's maker, for the settings paths the page names (the pause hint's battery line, the
 * detector's line): each family has its own App info and battery screens (on-time-0.7.0.md,
 * What stops the job). [STOCK] is every other maker, with Android's own paths (Pixel, the FP6).
 */
enum class Maker { XIAOMI, SAMSUNG, OPPO, VIVO, HUAWEI, LENOVO, STOCK }

/**
 * The maker for `Build.MANUFACTURER` (e.g. "Xiaomi", "HONOR", "samsung", "OnePlus", "LENOVO"),
 * case-insensitive, with the family's sub-brands: Redmi and POCO are Xiaomi's, realme and OnePlus
 * Oppo's (ColorOS, OxygenOS), iQOO Vivo's, Honor Huawei's (MagicOS keeps EMUI's paths).
 */
fun maker(manufacturer: String?): Maker = when (manufacturer?.trim()?.lowercase()) {
    "xiaomi", "redmi", "poco" -> Maker.XIAOMI
    "samsung" -> Maker.SAMSUNG
    "oppo", "realme", "oneplus" -> Maker.OPPO
    "vivo", "iqoo" -> Maker.VIVO
    "huawei", "honor" -> Maker.HUAWEI
    "lenovo" -> Maker.LENOVO
    else -> Maker.STOCK
}
