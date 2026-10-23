package com.kisss.visualtime.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SetupLayoutPolicyTest {
    @Test
    fun normalScreenKeepsHourglassSizeAndBalancesFrameGaps() {
        val placement = SetupLayoutPolicy.calculate(
            width = 1_080,
            height = 2_400,
            topPanelHeight = 400,
            bottomPanelRestingHeight = 300,
            minimumGap = 24,
        )

        assertEquals(2_400, placement.hourglassCanvasHeight)
        assertEquals(0, placement.hourglassCanvasY)
        assertBalancedGaps(placement, topPanelHeight = 400, bottomPanelHeight = 300, height = 2_400)
    }

    @Test
    fun compactScreenShrinksHourglassBeforePanelsCanOverlapIt() {
        val minimumGap = 16
        val placement = SetupLayoutPolicy.calculate(
            width = 400,
            height = 700,
            topPanelHeight = 200,
            bottomPanelRestingHeight = 150,
            minimumGap = minimumGap,
        )

        assertTrue(placement.hourglassCanvasHeight < 700)
        assertBalancedGaps(placement, topPanelHeight = 200, bottomPanelHeight = 150, height = 700)
        assertTrue(placement.frameTop - 200 >= minimumGap - 1f)
        assertTrue(550 - placement.frameBottom >= minimumGap - 1f)
    }

    private fun assertBalancedGaps(
        placement: SetupLayoutPlacement,
        topPanelHeight: Int,
        bottomPanelHeight: Int,
        height: Int,
    ) {
        val topPanelBottom = placement.topPanelY + topPanelHeight
        val bottomPanelTop = placement.bottomPanelRestingY
        val topGap = placement.frameTop - topPanelBottom
        val bottomGap = bottomPanelTop - placement.frameBottom

        assertTrue(abs(topGap - bottomGap) <= 1.5f)
        assertTrue(placement.topPanelY >= 0)
        assertTrue(placement.bottomPanelRestingY + bottomPanelHeight <= height)
    }
}
