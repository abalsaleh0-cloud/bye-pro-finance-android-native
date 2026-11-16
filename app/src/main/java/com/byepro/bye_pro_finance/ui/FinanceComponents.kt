package com.byepro.bye_pro_finance.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.byepro.bye_pro_finance.R

data class FinanceCategoryTabItem(val title:String,val iconResource:Int,val selected:Boolean,val onClick:()->Unit)
data class FinanceIconOptionItem(val title:String,val iconResource:Int,val selected:Boolean=false,val onClick:()->Unit)

data class FinanceEditorialField(val container:LinearLayout,val input:EditText)

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
                FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_VERTICAL_PADDING),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_HORIZONTAL_PADDING),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.NAVIGATION_VERTICAL_PADDING)
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
                setBackgroundColor(Color.TRANSPARENT)
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

    /** Native horizontal tabs: every route has its own icon and label. */
    fun categoryTabs(context:Context,isDark:Boolean,items:List<FinanceCategoryTabItem>):android.widget.HorizontalScrollView {
        val palette=FinanceDesignSystem.colors(isDark)
        val tabs=LinearLayout(context).apply { orientation=LinearLayout.HORIZONTAL; layoutDirection=View.LAYOUT_DIRECTION_RTL; gravity=Gravity.CENTER_VERTICAL }
        items.forEachIndexed { index,item ->
            val tab=LinearLayout(context).apply {
                orientation=LinearLayout.VERTICAL; layoutDirection=View.LAYOUT_DIRECTION_RTL; gravity=Gravity.CENTER
                setPadding(FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_4),FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_4),FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_4),FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_4))
                background=FinanceDesignSystem.rounded(context,if(item.selected) palette.accentSoft else palette.surface,FinanceDesignTokens.SECTION_TAB_RADIUS)
                isClickable=true; isFocusable=true; foreground=FinanceDesignSystem.ripple(context,isDark); contentDescription=item.title
                setOnClickListener { item.onClick() }
            }
            val icon=ImageView(context).apply { setImageResource(item.iconResource); setColorFilter(if(item.selected) palette.accent else palette.secondaryText); scaleType=ImageView.ScaleType.CENTER_INSIDE; contentDescription=item.title }
            tab.addView(icon,LinearLayout.LayoutParams(FinanceDesignSystem.dp(context,FinanceDesignTokens.SECTION_TAB_ICON_SIZE),FinanceDesignSystem.dp(context,FinanceDesignTokens.SECTION_TAB_ICON_SIZE)))
            tab.addView(FinanceDesignSystem.text(context,item.title,FinanceDesignTokens.TEXT_CAPTION_SP,if(item.selected) palette.accent else palette.text,bold=item.selected).apply {
                gravity=Gravity.CENTER; maxLines=1; ellipsize=android.text.TextUtils.TruncateAt.END
            },LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin=FinanceDesignSystem.dp(context,FinanceDesignTokens.SECTION_TAB_GAP) })
            tabs.addView(tab,LinearLayout.LayoutParams(FinanceDesignSystem.dp(context,FinanceDesignTokens.SECTION_TAB_WIDTH),FinanceDesignSystem.dp(context,FinanceDesignTokens.SECTION_TAB_HEIGHT)).apply {
                if(index>0) marginStart=FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_4)
            })
        }
        return android.widget.HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled=false; overScrollMode=View.OVER_SCROLL_NEVER; layoutDirection=View.LAYOUT_DIRECTION_RTL
            addView(tabs,ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    /** Icon rows shared by expense-category selection and section Add menus. */
    fun iconChoiceList(context:Context,isDark:Boolean,items:List<FinanceIconOptionItem>):LinearLayout {
        val palette=FinanceDesignSystem.colors(isDark)
        return LinearLayout(context).apply {
            orientation=LinearLayout.VERTICAL; layoutDirection=View.LAYOUT_DIRECTION_RTL
            setPadding(FinanceDesignSystem.dp(context,FinanceDesignTokens.CARD_PADDING),FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_8),FinanceDesignSystem.dp(context,FinanceDesignTokens.CARD_PADDING),FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_8))
            items.forEachIndexed { index,item ->
                val row=LinearLayout(context).apply {
                    orientation=LinearLayout.HORIZONTAL; layoutDirection=View.LAYOUT_DIRECTION_RTL; gravity=Gravity.CENTER_VERTICAL
                    minimumHeight=FinanceDesignSystem.dp(context,FinanceDesignTokens.TOUCH_TARGET_SIZE)
                    background=if(item.selected) FinanceDesignSystem.rounded(context,palette.accentSoft,FinanceDesignTokens.CONTROL_RADIUS) else ColorDrawable(Color.TRANSPARENT)
                    isClickable=true; isFocusable=true; foreground=FinanceDesignSystem.ripple(context,isDark); contentDescription=item.title
                    setOnClickListener { item.onClick() }
                }
                val icon=ImageView(context).apply { setImageResource(item.iconResource); setColorFilter(if(item.selected) palette.accent else palette.secondaryText); scaleType=ImageView.ScaleType.CENTER_INSIDE }
                row.addView(icon,LinearLayout.LayoutParams(FinanceDesignSystem.dp(context,FinanceDesignTokens.STANDARD_ICON_SIZE),FinanceDesignSystem.dp(context,FinanceDesignTokens.STANDARD_ICON_SIZE)))
                row.addView(FinanceDesignSystem.text(context,item.title,FinanceDesignTokens.TEXT_BODY_SP,palette.text,bold=item.selected),LinearLayout.LayoutParams(0,FinanceDesignSystem.dp(context,FinanceDesignTokens.TOUCH_TARGET_SIZE),1f).apply {
                    marginStart=FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_12)
                })
                addView(row,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,FinanceDesignSystem.dp(context,FinanceDesignTokens.TOUCH_TARGET_SIZE)))
                if(index<items.lastIndex) addView(bottomDivider(context,isDark))
            }
        }
    }

    fun formIconHeader(context:Context,isDark:Boolean,iconResource:Int,title:String,description:String):LinearLayout {
        val palette=FinanceDesignSystem.colors(isDark)
        return LinearLayout(context).apply {
            orientation=LinearLayout.HORIZONTAL; layoutDirection=View.LAYOUT_DIRECTION_RTL; gravity=Gravity.CENTER_VERTICAL
            setPadding(0,0,0,FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_12))
            val icon=ImageView(context).apply { setImageResource(iconResource); setColorFilter(palette.accent); scaleType=ImageView.ScaleType.CENTER_INSIDE }
            addView(icon,LinearLayout.LayoutParams(FinanceDesignSystem.dp(context,FinanceDesignTokens.RECORD_ICON_BOX_SIZE),FinanceDesignSystem.dp(context,FinanceDesignTokens.RECORD_ICON_BOX_SIZE)))
            val labels=LinearLayout(context).apply {
                orientation=LinearLayout.VERTICAL; layoutDirection=View.LAYOUT_DIRECTION_RTL; gravity=Gravity.CENTER_VERTICAL
                setPadding(FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_12),0,FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_4),0)
                addView(FinanceDesignSystem.text(context,title,FinanceDesignTokens.TEXT_BODY_SP,palette.text,bold=true))
                addView(FinanceDesignSystem.text(context,description,FinanceDesignTokens.TEXT_CAPTION_SP,palette.secondaryText))
            }
            addView(labels,LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f))
        }
    }

    fun balanceStatement(context:Context,isDark:Boolean,title:String,amount:String,caption:String):LinearLayout {
        val palette=FinanceDesignSystem.colors(isDark)
        val panel=FinanceDesignSystem.surface(context,isDark)
        panel.addView(FinanceDesignSystem.text(context,title,
            FinanceDesignTokens.TEXT_LABEL_SP,palette.secondaryText,bold=true))
        panel.addView(FinanceDesignSystem.text(context,amount,
            FinanceDesignTokens.TEXT_BALANCE_SP,palette.text,bold=true).apply {
            setPadding(0,FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_8),0,
                FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_8))
        })
        panel.addView(View(context).apply { setBackgroundColor(palette.divider) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                FinanceDesignSystem.dp(context,FinanceDesignTokens.DIVIDER_HEIGHT)))
        panel.addView(FinanceDesignSystem.text(context,caption,
            FinanceDesignTokens.TEXT_CAPTION_SP,palette.secondaryText).apply {
            setPadding(0,FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_8),0,0)
        })
        return panel
    }

    fun brandSignature(
        context:Context,isDark:Boolean,markResource:Int,descriptor:String,sequence:String
    ):LinearLayout {
        val palette=FinanceDesignSystem.colors(isDark)
        return LinearLayout(context).apply {
            orientation=LinearLayout.HORIZONTAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
            gravity=Gravity.CENTER_VERTICAL
            val mark=ImageView(context).apply {
                setImageResource(markResource)
                scaleType=ImageView.ScaleType.CENTER_INSIDE
                background=FinanceDesignSystem.rounded(context,palette.accentSoft,FinanceDesignTokens.CONTROL_RADIUS)
                val pad=FinanceDesignSystem.dp(context,FinanceDesignTokens.ENTRY_MARK_PADDING)
                setPadding(pad,pad,pad,pad)
                contentDescription="BYE PRO"
            }
            addView(mark,LinearLayout.LayoutParams(
                FinanceDesignSystem.dp(context,FinanceDesignTokens.ENTRY_BRAND_ICON_SIZE),
                FinanceDesignSystem.dp(context,FinanceDesignTokens.ENTRY_BRAND_ICON_SIZE)))
            val words=LinearLayout(context).apply {
                orientation=LinearLayout.VERTICAL
                layoutDirection=View.LAYOUT_DIRECTION_RTL
                setPadding(FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_8),0,0,0)
                addView(FinanceDesignSystem.text(context,"BYE PRO",
                    FinanceDesignTokens.TEXT_SECTION_TITLE_SP,palette.text,bold=true))
                addView(FinanceDesignSystem.text(context,descriptor,
                    FinanceDesignTokens.TEXT_CAPTION_SP,palette.secondaryText))
            }
            addView(words,LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f))
            addView(FinanceDesignSystem.text(context,sequence,
                FinanceDesignTokens.TEXT_CAPTION_SP,palette.secondaryText,bold=true))
        }
    }

    /** Original finance-orbit artwork drawn natively from semantic palette colors. */
    fun entryArtwork(context:Context,isDark:Boolean,markResource:Int):FrameLayout {
        val palette=FinanceDesignSystem.colors(isDark)
        return FrameLayout(context).apply {
            background=ColorDrawable(Color.TRANSPARENT)
            val orbit=object:View(context) {
                private val stroke=Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style=Paint.Style.STROKE
                    strokeWidth=FinanceDesignSystem.dp(context,FinanceDesignTokens.ENTRY_ARTWORK_STROKE).toFloat()
                }
                private val fill=Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.FILL }
                override fun onDraw(canvas:Canvas) {
                    super.onDraw(canvas)
                    val cx=width*.54f; val cy=height*.48f
                    val r=minOf(width*.30f,height*.43f)
                    val outer=RectF(cx-r,cy-r,cx+r,cy+r)
                    val inner=RectF(cx-r*.68f,cy-r*.68f,cx+r*.68f,cy+r*.68f)
                    stroke.color=palette.divider
                    canvas.drawCircle(cx,cy,r*1.28f,stroke)
                    canvas.drawCircle(cx,cy,r,stroke)
                    stroke.color=palette.accentSoft
                    canvas.drawArc(outer,24f,112f,false,stroke)
                    canvas.drawArc(inner,198f,126f,false,stroke)
                    val lx=cx-r*.92f; val ly=cy+r*.44f
                    val tx=cx+r*.48f; val ty=cy-r*.92f
                    val rx=cx+r*1.12f; val ry=cy+r*.18f
                    stroke.color=palette.divider
                    canvas.drawLine(lx,ly,cx,cy,stroke)
                    canvas.drawLine(cx,cy,tx,ty,stroke)
                    canvas.drawLine(cx,cy,rx,ry,stroke)
                    fill.color=palette.accent; canvas.drawCircle(lx,ly,r*.075f,fill)
                    fill.color=palette.positive; canvas.drawCircle(tx,ty,r*.065f,fill)
                    fill.color=palette.accent; canvas.drawCircle(rx,ry,r*.055f,fill)
                    val base=height*.88f; val bw=width*.025f
                    val xs=floatArrayOf(width*.12f,width*.19f,width*.26f,width*.33f)
                    val hs=floatArrayOf(.18f,.28f,.42f,.58f)
                    fill.color=palette.accentSoft
                    for(i in xs.indices) {
                        val top=base-height*hs[i]*.36f
                        canvas.drawRoundRect(RectF(xs[i],top,xs[i]+bw,base),bw,bw,fill)
                    }
                    stroke.color=palette.divider
                    canvas.drawLine(width*.08f,base,width*.40f,base,stroke)
                }
            }
            addView(orbit,FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT))
            val mark=ImageView(context).apply {
                setImageResource(markResource)
                scaleType=ImageView.ScaleType.CENTER_INSIDE
                background=FinanceDesignSystem.rounded(context,palette.surface,FinanceDesignTokens.ENTRY_ORBIT_CENTER_RADIUS)
                val pad=FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_12)
                setPadding(pad,pad,pad,pad)
                contentDescription="BYE PRO Finance"
            }
            addView(mark,FrameLayout.LayoutParams(
                FinanceDesignSystem.dp(context,FinanceDesignTokens.ENTRY_ORBIT_MARK_SIZE),
                FinanceDesignSystem.dp(context,FinanceDesignTokens.ENTRY_ORBIT_MARK_SIZE),Gravity.CENTER))
        }
    }

    fun editorialInput(
        context:Context,isDark:Boolean,label:String,hint:String,initialValue:String=""
    ):FinanceEditorialField {
        val palette=FinanceDesignSystem.colors(isDark)
        val column=LinearLayout(context).apply {
            orientation=LinearLayout.VERTICAL
            layoutDirection=View.LAYOUT_DIRECTION_RTL
        }
        column.addView(FinanceDesignSystem.text(context,label,
            FinanceDesignTokens.TEXT_CAPTION_SP,palette.secondaryText,bold=true).apply {
            setPadding(0,0,0,FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_4))
        })
        val input=FinanceDesignSystem.input(context,isDark,hint,initialValue=initialValue).apply {
            background=ColorDrawable(Color.TRANSPARENT)
            setPadding(FinanceDesignSystem.dp(context,FinanceDesignTokens.INPUT_HORIZONTAL_PADDING),0,
                FinanceDesignSystem.dp(context,FinanceDesignTokens.INPUT_HORIZONTAL_PADDING),0)
            minHeight=FinanceDesignSystem.dp(context,FinanceDesignTokens.ENTRY_FIELD_HEIGHT)
            isSingleLine=true
        }
        column.addView(input,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            FinanceDesignSystem.dp(context,FinanceDesignTokens.ENTRY_FIELD_HEIGHT)))
        column.addView(View(context).apply { setBackgroundColor(palette.divider) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                FinanceDesignSystem.dp(context,FinanceDesignTokens.DIVIDER_HEIGHT)))
        return FinanceEditorialField(column,input)
    }

    fun selectField(context:Context,isDark:Boolean,label:String,iconResource:Int,onClick:(TextView)->Unit):TextView {
        val palette=FinanceDesignSystem.colors(isDark)
        return FinanceDesignSystem.text(context,label,FinanceDesignTokens.TEXT_BODY_SP,palette.text).apply {
            gravity=Gravity.CENTER_VERTICAL
            setPadding(FinanceDesignSystem.dp(context,FinanceDesignTokens.INPUT_HORIZONTAL_PADDING),0,FinanceDesignSystem.dp(context,FinanceDesignTokens.INPUT_HORIZONTAL_PADDING),0)
            minHeight=FinanceDesignSystem.dp(context,FinanceDesignTokens.BUTTON_HEIGHT)
            background=FinanceDesignSystem.rounded(context,palette.raised,FinanceDesignTokens.CONTROL_RADIUS)
            setCompoundDrawablesRelativeWithIntrinsicBounds(iconResource,0,0,0)
            compoundDrawablePadding=FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_12)
            compoundDrawableTintList=android.content.res.ColorStateList.valueOf(palette.accent)
            isClickable=true; isFocusable=true; foreground=FinanceDesignSystem.ripple(context,isDark)
            setOnClickListener { onClick(this) }
        }
    }

    fun categoryDescription(
        context: Context,
        isDark: Boolean,
        description: String
    ): TextView = FinanceDesignSystem.text(
        context,
        description,
        FinanceDesignTokens.TEXT_CAPTION_SP,
        FinanceDesignSystem.colors(isDark).secondaryText
    ).apply {
        setPadding(
            FinanceDesignSystem.dp(context, FinanceDesignTokens.SPACE_4),
            FinanceDesignSystem.dp(context, FinanceDesignTokens.SPACE_8),
            FinanceDesignSystem.dp(context, FinanceDesignTokens.SPACE_4),
            FinanceDesignSystem.dp(context, FinanceDesignTokens.SPACE_4)
        )
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

    /** Shared selected-month control with available months and a calendar action. */
    fun monthSelector(context:Context,isDark:Boolean,caption:String,selectedMonth:String,contentDescription:String,onClick:(View)->Unit,onCalendarClick:(View)->Unit):LinearLayout {
        val palette=FinanceDesignSystem.colors(isDark)
        return LinearLayout(context).apply {
            orientation=LinearLayout.HORIZONTAL; layoutDirection=View.LAYOUT_DIRECTION_RTL; gravity=Gravity.CENTER_VERTICAL
            setPadding(0,FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_2),0,FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_2))
            addView(FinanceDesignSystem.text(context,caption,FinanceDesignTokens.TEXT_LABEL_SP,palette.secondaryText,bold=true),LinearLayout.LayoutParams(0,FinanceDesignSystem.dp(context,FinanceDesignTokens.BUTTON_HEIGHT),1f))
            val month=FinanceDesignSystem.text(context,"$selectedMonth  ▾",FinanceDesignTokens.TEXT_BODY_SP,palette.accent,bold=true).apply {
                gravity=Gravity.CENTER; minHeight=FinanceDesignSystem.dp(context,FinanceDesignTokens.TOUCH_TARGET_SIZE)
                setPadding(FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_8),0,FinanceDesignSystem.dp(context,FinanceDesignTokens.SPACE_8),0)
                this.contentDescription=contentDescription; isClickable=true; isFocusable=true; foreground=FinanceDesignSystem.ripple(context,isDark)
                setOnClickListener { onClick(this) }
            }
            addView(month,LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,FinanceDesignSystem.dp(context,FinanceDesignTokens.TOUCH_TARGET_SIZE)))
            addView(FinanceDesignSystem.iconAction(context,isDark,R.drawable.ic_calendar,context.getString(com.byepro.bye_pro_finance.R.string.calendar)) { onCalendarClick(this) },
                LinearLayout.LayoutParams(FinanceDesignSystem.dp(context,FinanceDesignTokens.TOUCH_TARGET_SIZE),FinanceDesignSystem.dp(context,FinanceDesignTokens.TOUCH_TARGET_SIZE)))
        }
    }

    /** Shared empty state keeps its typography and spacing consistent. */
    fun emptyState(context: Context, isDark: Boolean, message: String): TextView =
        FinanceDesignSystem.text(
            context,
            message,
            FinanceDesignTokens.TEXT_BODY_SP,
            FinanceDesignSystem.colors(isDark).secondaryText
        ).apply {
            gravity = Gravity.CENTER
            setPadding(
                FinanceDesignSystem.dp(context, FinanceDesignTokens.COMPONENT_HORIZONTAL_PADDING),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.EMPTY_STATE_VERTICAL_PADDING),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.COMPONENT_HORIZONTAL_PADDING),
                FinanceDesignSystem.dp(context, FinanceDesignTokens.EMPTY_STATE_VERTICAL_PADDING)
            )
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
