package com.uvg.agroconecta.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class StarRatingTest {

    @Test
    fun `integer rating uses full and empty stars`() {
        assertEquals(
            listOf(
                StarFill.FULL,
                StarFill.FULL,
                StarFill.FULL,
                StarFill.FULL,
                StarFill.EMPTY
            ),
            starFillsFor(4.0)
        )
    }

    @Test
    fun `decimal rating uses a half star`() {
        assertEquals(
            listOf(
                StarFill.FULL,
                StarFill.FULL,
                StarFill.FULL,
                StarFill.HALF,
                StarFill.EMPTY
            ),
            starFillsFor(3.5)
        )
    }

    @Test
    fun `distributor without reviews uses empty stars`() {
        assertEquals(List(5) { StarFill.EMPTY }, starFillsFor(null))
    }

    @Test
    fun `zero or absent rating uses empty stars`() {
        assertEquals(List(5) { StarFill.EMPTY }, starFillsFor(0.0))
        assertEquals(List(5) { StarFill.EMPTY }, starFillsFor(null))
    }
}
