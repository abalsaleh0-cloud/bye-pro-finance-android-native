package com.byepro.bye_pro_finance.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

data class FinanceNavigationItem(
    val title: String,
    val iconResource: Int,
    val selected: Boolean,
    val onClick: () -> Unit
)

object FinanceComponents {

    fun topBar(
        context: Context,
        isDark: Boolean,
        title: String,
        actions: List<Pair<Int, Pair<String, () -> Unit>>>
    ): LinearLayout {
        val palette = FinanceDesignSystem.colors(isDark)
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                FinanceDesignSystem.dp(context, FinanceDesignTokens.PAGE_HORIZONTAL_PADDING),
                0,
                FinanceDesignSystem.dp(context, FinanceDesignTokens.SPACE_8),
                0
            )
            setBackgroundColor(palette.background)
        }

        val heading = FinanceDesignSystem.text(
            context, title, FinanceDesignTokens.TEXT_PAGE_TITLE_SP, palette.text, bold = true
        )
        bar.addView(heading, LinearLayout.LayoutParams(0,
            FinanceDesignSystem.dp(context, FinanceDesignTokens.APP_BAR_HEIGHT), 1f))

        actions.forEach { (icon, descriptor) ->
            val action = FinanceDesignSystem.iconAction(
                context, isDark, icon, descriptor.first, descriptor.second
            )
            bar.addView(action, LinearLayout.LayoutParams(
                FinanceDesignSystem.dp(context, FinanceDesignTokens.TOUCH_TARGET_SIZE),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.TOUCH_TARGET_SIZE)
            ))
        }
        return bar
    }

    fun bottomNavigation(
        context: Context,
        isDark: Boolean,
        items: List<FinanceNavigationItem>
    ): LinearLayout {
        val palette = FinanceDesignSystem.colors(isDark)
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_HORIZONTAL_PADDING),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_HORIZONTAL_PADDING),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_HORIZONTAL_PADDING),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_HORIZONTAL_PADDING)
            )
            setBackgroundColor(palette.surface)
        }

        items.forEach { item ->
            val cell = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                foreground = FinanceDesignSystem.ripple(context, isDark)
                setOnClickListener { item.onClick() }
                setPadding(
                    FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_ITEM_PADDING),
                    FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_ITEM_PADDING),
                    FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_ITEM_PADDING),
                    FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_ITEM_PADDING)
                )
            }

            val iconHolder = FrameLayout(context).apply {
                // Telegram-style navigation has no filled selected pill:
                // selection is communicated by icon and label color only.
                setBackgroundColor(0x00000000)
                val icon = ImageView(context).apply {
                    setImageResource(item.iconResource)
                    setColorFilter(if (item.selected) palette.accent else palette.secondaryText)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
                addView(icon, FrameLayout.LayoutParams(
                    FinanceDesignSystem.dp(context, FinanceDesignTokens.STANDARD_ICON_SIZE),
                    FinanceDesignSystem.dp(context, FinanceDesignTokens.STANDARD_ICON_SIZE),
                    Gravity.CENTER
                ))
            }
            cell.addView(iconHolder, LinearLayout.LayoutParams(
                FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_ICON_SLOT_WIDTH),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_ICON_SLOT_HEIGHT)
            ))

            val label = FinanceDesignSystem.text(
                context,
                item.title,
                FinanceDesignTokens.TEXT_NAVIGATION_SP,
                if (item.selected) palette.accent else palette.secondaryText,
                bold = item.selected
            ).apply {
                gravity = Gravity.CENTER
                maxLines = 1
            }
            cell.addView(label, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_LABEL_HEIGHT)
            ))
            bar.addView(cell, LinearLayout.LayoutParams(
                0,
                FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_CONTENT_HEIGHT),
                1f
            ))
        }
        return bar
    }

    fun sectionHeader(
        context: Context,
        isDark: Boolean,
        title: String,
        trailing: String? = null
    ): LinearLayout {
        val palette = FinanceDesignSystem.colors(isDark)
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
            val heading = FinanceDesignSystem.sectionTitle(context, title, isDark)
            addView(heading, LinearLayout.LayoutParams(0,
                FinanceDesignSystem.dp(context, FinanceDesignTokens.SECTION_HEADER_HEIGHT), 1f))
            if (!trailing.isNullOrBlank()) {
                addView(FinanceDesignSystem.text(
                    context, trailing, FinanceDesignTokens.TEXT_CAPTION_SP, palette.accent, bold = true
                ))
            }
        }
    }

    fun metricRow(
        context: Context,
        isDark: Boolean,
        iconResource: Int,
        title: String,
        value: String,
        emphasized: Boolean = false
    ): LinearLayout {
        val palette = FinanceDesignSystem.colors(isDark)
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = FinanceDesignSystem.dp(
                context, FinanceDesignTokens.METRIC_TILE_MIN_HEIGHT
            )
            setPadding(
                FinanceDesignSystem.dp(context, FinanceDesignTokens.COMPONENT_HORIZONTAL_PADDING),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.SPACE_8),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.COMPONENT_HORIZONTAL_PADDING),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.SPACE_8)
            )
        }
        val icon = FinanceDesignSystem.iconBox(context, isDark, iconResource, emphasized)
        row.addView(icon, LinearLayout.LayoutParams(
            FinanceDesignSystem.dp(context, FinanceDesignTokens.RECORD_ICON_BOX_SIZE),
            FinanceDesignSystem.dp(context, FinanceDesignTokens.RECORD_ICON_BOX_SIZE)
        ))

        val label = FinanceDesignSystem.text(
            context, title, FinanceDesignTokens.TEXT_BODY_SP,
            if (emphasized) palette.accent else palette.text,
            bold = emphasized
        ).apply {
            setPadding(FinanceDesignSystem.dp(context, FinanceDesignTokens.COMPONENT_HORIZONTAL_PADDING), 0,
                FinanceDesignSystem.dp(context, FinanceDesignTokens.SPACE_6), 0)
            maxLines = 2
        }
        row.addView(label, LinearLayout.LayoutParams(0,
            FinanceDesignSystem.dp(context, FinanceDesignTokens.BUTTON_HEIGHT), 1f))

        val amount = FinanceDesignSystem.text(
            context, value, FinanceDesignTokens.TEXT_VALUE_SP,
            if (emphasized) palette.accent else palette.text,
            bold = true
        )
        row.addView(amount, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            FinanceDesignSystem.dp(context, FinanceDesignTokens.BUTTON_HEIGHT)
        ))
        return row
    }

    fun recordRow(
        context: Context,
        isDark: Boolean,
        iconResource: Int,
        title: String,
        subtitle: String,
        amount: String,
        onClick: ((View) -> Unit)? = null,
        onLongClick: ((View) -> Unit)? = null
    ): LinearLayout {
        val palette = FinanceDesignSystem.colors(isDark)
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = FinanceDesignSystem.dp(
                context, FinanceDesignTokens.RECORD_ROW_HEIGHT
            )
            setPadding(
                FinanceDesignSystem.dp(context, FinanceDesignTokens.COMPONENT_HORIZONTAL_PADDING),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.SPACE_8),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.COMPONENT_HORIZONTAL_PADDING),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.SPACE_8)
            )
        }

        val icon = FinanceDesignSystem.iconBox(context, isDark, iconResource)
        row.addView(icon, LinearLayout.LayoutParams(
            FinanceDesignSystem.dp(context, FinanceDesignTokens.RECORD_ICON_BOX_SIZE),
            FinanceDesignSystem.dp(context, FinanceDesignTokens.RECORD_ICON_BOX_SIZE)
        ))

        val detail = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(FinanceDesignSystem.dp(context, FinanceDesignTokens.COMPONENT_HORIZONTAL_PADDING), 0,
                FinanceDesignSystem.dp(context, FinanceDesignTokens.SPACE_8), 0)
        }
        detail.addView(FinanceDesignSystem.text(
            context, title, FinanceDesignTokens.TEXT_BODY_SP, palette.text, bold = true
        ))
        if (subtitle.isNotBlank()) {
            detail.addView(FinanceDesignSystem.text(
                context, subtitle, FinanceDesignTokens.TEXT_CAPTION_SP, palette.secondaryText
            ).apply { maxLines = 1 })
        }
        row.addView(detail, LinearLayout.LayoutParams(0,
            FinanceDesignSystem.dp(context, FinanceDesignTokens.BUTTON_HEIGHT), 1f))

        row.addView(FinanceDesignSystem.text(
            context, amount, FinanceDesignTokens.TEXT_AMOUNT_SP, palette.text, bold = true
        ))
        if (onClick != null || onLongClick != null) {
            row.isClickable = true
            row.isFocusable = true
            row.foreground = FinanceDesignSystem.ripple(context, isDark)
        }
        if (onClick != null) {
            row.setOnClickListener { onClick(row) }
        }
        if (onLongClick != null) {
            row.isLongClickable = true
            row.setOnLongClickListener {
                onLongClick(row)
                true
            }
        }
        return row
    }

    /** A grouped Telegram-style list surface: the rows provide their own
     * horizontal insets, so the container itself must not add another 16dp. */
    fun groupedList(context: Context, isDark: Boolean): LinearLayout =
        FinanceDesignSystem.surface(
            context,
            isDark,
            padding = 0,
            radius = FinanceDesignTokens.CARD_RADIUS
        )

    /** A content card for charts or panels that need their own inner padding. */
    fun card(context: Context, isDark: Boolean): LinearLayout =
        FinanceDesignSystem.surface(
            context,
            isDark,
            FinanceDesignTokens.CARD_PADDING,
            FinanceDesignTokens.CARD_RADIUS
        )

    fun bottomDivider(context: Context, isDark: Boolean): View =
        FinanceDesignSystem.divider(context, isDark)
}
