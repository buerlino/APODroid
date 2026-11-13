package io.github.buerlino.apodroid.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MakerTest {
    @Test
    fun readsRealManufacturers() {
        assertEquals(Maker.XIAOMI, maker("Xiaomi"))
        assertEquals(Maker.HUAWEI, maker("HONOR"))
        assertEquals(Maker.SAMSUNG, maker("samsung"))
        assertEquals(Maker.OPPO, maker("OnePlus"))
        assertEquals(Maker.LENOVO, maker("LENOVO"))
        assertEquals(Maker.STOCK, maker("Fairphone"))
        assertEquals(Maker.STOCK, maker("Google"))
    }

    @Test
    fun readsTheSubBrandsAnyCase() {
        assertEquals(Maker.XIAOMI, maker("Redmi"))
        assertEquals(Maker.XIAOMI, maker("POCO"))
        assertEquals(Maker.OPPO, maker("OPPO"))
        assertEquals(Maker.OPPO, maker("realme"))
        assertEquals(Maker.VIVO, maker("vivo"))
        assertEquals(Maker.VIVO, maker("iQOO"))
        assertEquals(Maker.HUAWEI, maker("HUAWEI"))
        assertEquals(Maker.HUAWEI, maker(" Honor "))
    }

    @Test
    fun anythingElseIsStock() {
        assertEquals(Maker.STOCK, maker(""))
        assertEquals(Maker.STOCK, maker(null))
        assertEquals(Maker.STOCK, maker("motorola"))
        assertEquals(Maker.STOCK, maker("Xiaomi Communications"))
    }
}
