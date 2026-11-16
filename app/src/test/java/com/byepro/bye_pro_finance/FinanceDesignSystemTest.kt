package com.byepro.bye_pro_finance

import com.byepro.bye_pro_finance.ui.FinanceDesignSystem
import com.byepro.bye_pro_finance.ui.FinanceDesignTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceDesignSystemTest {

    @Test
    fun approvedGeometryRemainsCentralized() {
        assertEquals(16, FinanceDesignTokens.PAGE_HORIZONTAL_PADDING)
        assertEquals(12, FinanceDesignTokens.PAGE_VERTICAL_PADDING)
        assertEquals(12, FinanceDesignTokens.SECTION_SPACING)
        assertEquals(16, FinanceDesignTokens.CARD_PADDING)
        assertEquals(12, FinanceDesignTokens.INPUT_VERTICAL_PADDING)
        assertEquals(14, FinanceDesignTokens.INPUT_HORIZONTAL_PADDING)
        assertEquals(18, FinanceDesignTokens.BUTTON_HORIZONTAL_PADDING)
        assertEquals(12, FinanceDesignTokens.COMPONENT_HORIZONTAL_PADDING)
        assertEquals(8, FinanceDesignTokens.COMPONENT_VERTICAL_PADDING)

        assertEquals(16, FinanceDesignTokens.CARD_RADIUS)
        assertEquals(12, FinanceDesignTokens.CONTROL_RADIUS)
        assertEquals(20, FinanceDesignTokens.DIALOG_RADIUS)

        assertEquals(56, FinanceDesignTokens.APP_BAR_HEIGHT)
        assertEquals(48, FinanceDesignTokens.BUTTON_HEIGHT)
        assertEquals(44, FinanceDesignTokens.TOUCH_TARGET_SIZE)
        assertEquals(68, FinanceDesignTokens.NAVIGATION_BAR_HEIGHT)
        assertEquals(4, FinanceDesignTokens.NAVIGATION_HORIZONTAL_PADDING)
        assertEquals(4, FinanceDesignTokens.NAVIGATION_VERTICAL_PADDING)
        assertEquals(2, FinanceDesignTokens.NAVIGATION_ITEM_PADDING)
        assertEquals(38, FinanceDesignTokens.NAVIGATION_ICON_SLOT_WIDTH)
        assertEquals(30, FinanceDesignTokens.NAVIGATION_ICON_SLOT_HEIGHT)
        assertEquals(20, FinanceDesignTokens.NAVIGATION_LABEL_HEIGHT)
        assertEquals(60, FinanceDesignTokens.NAVIGATION_CONTENT_HEIGHT)
        assertEquals(48, FinanceDesignTokens.SECTION_TAB_HEIGHT)
        assertEquals(36, FinanceDesignTokens.SECTION_HEADER_HEIGHT)

        assertEquals(64, FinanceDesignTokens.METRIC_TILE_MIN_HEIGHT)
        assertEquals(64, FinanceDesignTokens.RECORD_ROW_HEIGHT)
        assertEquals(40, FinanceDesignTokens.RECORD_ICON_BOX_SIZE)
        assertEquals(20, FinanceDesignTokens.STANDARD_ICON_SIZE)
        assertEquals(1, FinanceDesignTokens.DIVIDER_HEIGHT)
        assertEquals(56, FinanceDesignTokens.LIST_DIVIDER_START_INSET)
        assertEquals(24, FinanceDesignTokens.EMPTY_STATE_VERTICAL_PADDING)
        assertEquals(80, FinanceDesignTokens.MULTILINE_INPUT_HEIGHT)
        assertEquals(64, FinanceDesignTokens.WELCOME_ICON_SIZE)
        assertEquals(72, FinanceDesignTokens.SWIPE_THRESHOLD)
        assertEquals(76, FinanceDesignTokens.SECTION_TAB_WIDTH)
        assertEquals(64, FinanceDesignTokens.SECTION_TAB_HEIGHT)
        assertEquals(22, FinanceDesignTokens.SECTION_TAB_ICON_SIZE)
        assertEquals(12, FinanceDesignTokens.SECTION_TAB_RADIUS)
        assertEquals(4, FinanceDesignTokens.SECTION_TAB_GAP)
        assertEquals(30f, FinanceDesignTokens.TEXT_ENTRY_TITLE_SP, 0f)
        assertEquals(12f, FinanceDesignTokens.TEXT_ENTRY_KICKER_SP, 0f)
        assertEquals(28f, FinanceDesignTokens.TEXT_BALANCE_SP, 0f)
        assertEquals(44, FinanceDesignTokens.ENTRY_BRAND_ROW_HEIGHT)
        assertEquals(36, FinanceDesignTokens.ENTRY_BRAND_ICON_SIZE)
        assertEquals(184, FinanceDesignTokens.ENTRY_ARTWORK_HEIGHT)
        assertEquals(112, FinanceDesignTokens.ENTRY_LOGIN_ARTWORK_HEIGHT)
        assertEquals(64, FinanceDesignTokens.ENTRY_ORBIT_MARK_SIZE)
        assertEquals(36, FinanceDesignTokens.ENTRY_ORBIT_CENTER_RADIUS)
        assertEquals(52, FinanceDesignTokens.ENTRY_FIELD_HEIGHT)
        assertEquals(32, FinanceDesignTokens.ENTRY_ERROR_HEIGHT)
        assertEquals(32, FinanceDesignTokens.ENTRY_FOOTER_HEIGHT)
        assertEquals(160L, FinanceDesignTokens.MOTION_DURATION_MS)
    }

    @Test
    fun lightAndDarkPalettesUseSeparateSurfaces() {
        assertNotEquals(
            FinanceDesignSystem.dark.background,
            FinanceDesignSystem.light.background
        )
        assertNotEquals(
            FinanceDesignSystem.dark.surface,
            FinanceDesignSystem.light.surface
        )
        assertNotEquals(
            FinanceDesignSystem.dark.text,
            FinanceDesignSystem.light.text
        )
        assertNotEquals(
            FinanceDesignSystem.dark.accent,
            FinanceDesignSystem.light.accent
        )
    }

    @Test
    fun importantTextColorsMeetReadableContrastInBothModes() {
        assertTrue(contrast(FinanceDesignSystem.light.text, FinanceDesignSystem.light.surface) >= 4.5)
        assertTrue(contrast(FinanceDesignSystem.light.secondaryText, FinanceDesignSystem.light.surface) >= 4.5)
        assertTrue(contrast(FinanceDesignSystem.light.accent, FinanceDesignSystem.light.surface) >= 4.5)
        assertTrue(contrast(FinanceDesignSystem.light.onAccent, FinanceDesignSystem.light.accent) >= 4.5)

        assertTrue(contrast(FinanceDesignSystem.dark.text, FinanceDesignSystem.dark.surface) >= 4.5)
        assertTrue(contrast(FinanceDesignSystem.dark.secondaryText, FinanceDesignSystem.dark.surface) >= 4.5)
        assertTrue(contrast(FinanceDesignSystem.dark.accent, FinanceDesignSystem.dark.surface) >= 4.5)
        assertTrue(contrast(FinanceDesignSystem.dark.onAccent, FinanceDesignSystem.dark.accent) >= 4.5)
    }

    @Test
    fun semanticTypographyAndSpacingRemainCentralized() {
        assertEquals(2, FinanceDesignTokens.SPACE_2)
        assertEquals(4, FinanceDesignTokens.SPACE_4)
        assertEquals(5, FinanceDesignTokens.SPACE_5)
        assertEquals(6, FinanceDesignTokens.SPACE_6)
        assertEquals(8, FinanceDesignTokens.SPACE_8)
        assertEquals(12, FinanceDesignTokens.SPACE_12)
        assertEquals(14, FinanceDesignTokens.SPACE_14)
        assertEquals(16, FinanceDesignTokens.SPACE_16)
        assertEquals(18, FinanceDesignTokens.SPACE_18)
        assertEquals(24, FinanceDesignTokens.SPACE_24)

        assertEquals(11f, FinanceDesignTokens.TEXT_NAVIGATION_SP, 0f)
        assertEquals(12f, FinanceDesignTokens.TEXT_CAPTION_SP, 0f)
        assertEquals(13f, FinanceDesignTokens.TEXT_LABEL_SP, 0f)
        assertEquals(14f, FinanceDesignTokens.TEXT_BODY_SP, 0f)
        assertEquals(15f, FinanceDesignTokens.TEXT_INPUT_SP, 0f)
        assertEquals(15f, FinanceDesignTokens.TEXT_ACTION_SP, 0f)
        assertEquals(15f, FinanceDesignTokens.TEXT_AMOUNT_SP, 0f)
        assertEquals(16f, FinanceDesignTokens.TEXT_VALUE_SP, 0f)
        assertEquals(16f, FinanceDesignTokens.TEXT_SECTION_TITLE_SP, 0f)
        assertEquals(20f, FinanceDesignTokens.TEXT_PAGE_TITLE_SP, 0f)
    }

    @Test
    fun chartAndSharedComponentsUseApprovedGeometry() {
        assertEquals(24, FinanceDesignTokens.CHART_LABEL_ROW_HEIGHT)
        assertEquals(7, FinanceDesignTokens.CHART_BAR_HEIGHT)
        assertEquals(5f, FinanceDesignTokens.CHART_BAR_RADIUS_DP, 0f)
        assertEquals(5, FinanceDesignTokens.CHART_BAR_TOP_GAP)
        assertEquals(72, FinanceDesignTokens.CHART_HORIZONTAL_INSET)
        assertEquals(3, FinanceDesignTokens.CHART_MIN_VISIBLE_FILL)
        assertEquals(FinanceDesignTokens.TOUCH_TARGET_SIZE, 44)
        assertEquals(FinanceDesignTokens.NAVIGATION_BAR_HEIGHT, 68)
    }

    private fun contrast(foreground: Int, background: Int): Double {
        fun channel(value: Int): Double {
            val sRgb = value / 255.0
            return if (sRgb <= 0.04045) sRgb / 12.92
            else Math.pow((sRgb + 0.055) / 1.055, 2.4)
        }

        fun component(color: Int, shift: Int): Double =
            channel((color ushr shift) and 0xFF)

        fun luminance(color: Int): Double =
            0.2126 * component(color, 16) +
                0.7152 * component(color, 8) +
                0.0722 * component(color, 0)

        val a = luminance(foreground)
        val b = luminance(background)
        val lighter = maxOf(a, b)
        val darker = minOf(a, b)
        return (lighter + 0.05) / (darker + 0.05)
    }

}
