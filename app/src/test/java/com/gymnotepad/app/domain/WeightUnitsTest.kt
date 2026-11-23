package com.gymnotepad.app.domain

import com.gymnotepad.app.data.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class WeightUnitsTest {
    @Test fun `formats and parses kilograms`() {
        assertEquals("82.4", 82_400L.toWeightText(WeightUnit.KILOGRAMS))
        assertEquals(82_400L, "82,4".toWeightGrams(WeightUnit.KILOGRAMS))
    }

    @Test fun `formats and parses pounds`() {
        assertEquals("181.66", 82_400L.toWeightText(WeightUnit.POUNDS))
        assertEquals(82_400L, "181.66".toWeightGrams(WeightUnit.POUNDS))
    }
}
