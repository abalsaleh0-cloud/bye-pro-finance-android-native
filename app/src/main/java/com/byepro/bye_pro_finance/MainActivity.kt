package com.byepro.bye_pro_finance

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.LinearLayout
import android.text.method.DigitsKeyListener
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.byepro.bye_pro_finance.data.DashboardRepository
import com.byepro.bye_pro_finance.data.FinanceWriteRepository
import com.byepro.bye_pro_finance.data.FinanceRuleException
import com.byepro.bye_pro_finance.data.LegacyFinanceDatabase
import com.byepro.bye_pro_finance.data.LocalAuthentication
import com.byepro.bye_pro_finance.domain.ActivityItem
import com.byepro.bye_pro_finance.domain.DashboardSummary
import com.byepro.bye_pro_finance.domain.FinanceRecord
import com.byepro.bye_pro_finance.domain.FinanceCalculatorEngine
import com.byepro.bye_pro_finance.domain.HomeCategory
import com.byepro.bye_pro_finance.domain.WorkCategory
import com.byepro.bye_pro_finance.domain.HomeRecordSource
import com.byepro.bye_pro_finance.ui.FinanceAppearance
import com.byepro.bye_pro_finance.ui.FinanceAppearanceMode
import com.byepro.bye_pro_finance.ui.FinanceCategoryTabItem
import com.byepro.bye_pro_finance.ui.FinanceIconOptionItem
import com.byepro.bye_pro_finance.ui.FinanceComponents
import com.byepro.bye_pro_finance.ui.FinanceDesignSystem
import com.byepro.bye_pro_finance.ui.FinanceDesignTokens
import com.byepro.bye_pro_finance.ui.FinanceNavigationItem
import com.byepro.bye_pro_finance.ui.FinancePalette
import com.byepro.bye_pro_finance.ui.FinancePalette.setRoundedBackground
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.text.DateFormatSymbols
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : Activity() {

    private enum class Section {
        DASHBOARD, WORK, HOME, DEBTS, AI
    }

    private lateinit var database: LegacyFinanceDatabase
    private lateinit var localAuthentication: LocalAuthentication
    private lateinit var repository: DashboardRepository
    private lateinit var writeRepository: FinanceWriteRepository
    private lateinit var root: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var pageContent: LinearLayout
    private lateinit var navigation: LinearLayout

    private enum class EntryStage { WELCOME, LOGIN, APP }

    private var entryStage = EntryStage.WELCOME
    private var activeSection = Section.DASHBOARD
    private var selectedHomeCategory = HomeCategory.RECEIVED_FROM_WORK
    private var selectedWorkCategory = WorkCategory.SALARY
    private var selectedMonth = YearMonth.now()
    private var localeCode = "ar"
    private var themeMode = "system"
    private var darkTheme = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = LegacyFinanceDatabase(this)
        // Delete only obsolete theme catalogue/background preferences. Financial
        // tables and every work, home, bill, debt and payment row are untouched.
        database.deleteSettings(FinanceAppearance.LEGACY_SETTING_KEYS)
        localAuthentication = LocalAuthentication(
            readSetting = database::readSetting,
            writeSetting = database::writeSetting
        ).apply {
            initializeIfNeeded()
        }
        repository = DashboardRepository(database)
        writeRepository = FinanceWriteRepository(database)
        localeCode = database.readSetting("locale")?.let {
            if (it == "en") "en" else "ar"
        } ?: "ar"
        themeMode = database.readSetting("theme_mode") ?: "system"
        activeSection = savedInstanceState?.getString("active_section")
            ?.let { runCatching { Section.valueOf(it) }.getOrNull() }
            ?: Section.DASHBOARD
        selectedMonth = savedInstanceState?.getString("selected_month")
            ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
            ?: database.readSetting("selected_month")
                ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
            ?: YearMonth.now()
        selectedHomeCategory = savedInstanceState?.getString("home_category")
            ?.let { runCatching { HomeCategory.valueOf(it) }.getOrNull() }
            ?: HomeCategory.RECEIVED_FROM_WORK
        selectedWorkCategory = savedInstanceState?.getString("work_category")
            ?.let { runCatching { WorkCategory.valueOf(it) }.getOrNull() }
            ?: WorkCategory.SALARY

        darkTheme = resolveDarkTheme()
        applySystemBars()
        showWelcomeScreen()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("active_section", activeSection.name)
        outState.putString("selected_month", selectedMonth.toString())
        outState.putString("home_category", selectedHomeCategory.name)
        outState.putString("work_category", selectedWorkCategory.name)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (::database.isInitialized) database.close()
        super.onDestroy()
    }

    private fun showWelcomeScreen() {
        entryStage = EntryStage.WELCOME
        val palette = FinanceDesignSystem.colors(darkTheme)
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(FinanceDesignTokens.PAGE_HORIZONTAL_PADDING),
                dp(FinanceDesignTokens.PAGE_VERTICAL_PADDING),
                dp(FinanceDesignTokens.PAGE_HORIZONTAL_PADDING),
                dp(FinanceDesignTokens.PAGE_VERTICAL_PADDING)
            )
            background = ColorDrawable(palette.background)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        content.addView(FinanceComponents.brandSignature(
            this, darkTheme, R.drawable.ic_bye_pro_finance,
            s(R.string.entry_brand_meta), s(R.string.welcome_step)
        ), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(FinanceDesignTokens.ENTRY_BRAND_ROW_HEIGHT)
        ))
        val artwork = FinanceComponents.entryArtwork(this, darkTheme, R.drawable.ic_bye_pro_finance)
        content.addView(artwork, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(FinanceDesignTokens.ENTRY_ARTWORK_HEIGHT)
        ).apply {
            topMargin = dp(FinanceDesignTokens.SPACE_8)
            bottomMargin = dp(FinanceDesignTokens.SPACE_8)
        })
        content.addView(FinanceDesignSystem.text(
            this, s(R.string.welcome_kicker),
            FinanceDesignTokens.TEXT_ENTRY_KICKER_SP, palette.accent, bold = true
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(FinanceDesignTokens.SPACE_4), 0, dp(FinanceDesignTokens.SPACE_4))
        })
        content.addView(FinanceDesignSystem.text(
            this, s(R.string.welcome_title),
            FinanceDesignTokens.TEXT_ENTRY_TITLE_SP, palette.text, bold = true
        ).apply {
            setLineSpacing(dp(FinanceDesignTokens.SPACE_4).toFloat(), 1f)
            setPadding(0, dp(FinanceDesignTokens.SPACE_4), 0, dp(FinanceDesignTokens.SPACE_8))
        })
        content.addView(FinanceDesignSystem.text(
            this, s(R.string.welcome_description),
            FinanceDesignTokens.TEXT_BODY_SP, palette.secondaryText
        ).apply {
            setLineSpacing(dp(FinanceDesignTokens.SPACE_4).toFloat(), 1f)
            setPadding(0, 0, 0, dp(FinanceDesignTokens.SPACE_16))
        })
        val progress = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        progress.addView(FinanceDesignSystem.text(
            this, s(R.string.welcome_step),
            FinanceDesignTokens.TEXT_CAPTION_SP, palette.secondaryText, bold = true
        ))
        progress.addView(View(this).apply { setBackgroundColor(palette.divider) },
            LinearLayout.LayoutParams(0, dp(FinanceDesignTokens.DIVIDER_HEIGHT), 1f).apply {
                marginStart = dp(FinanceDesignTokens.SPACE_12)
                marginEnd = dp(FinanceDesignTokens.SPACE_12)
            })
        progress.addView(FinanceDesignSystem.text(
            this, s(R.string.swipe_to_continue),
            FinanceDesignTokens.TEXT_CAPTION_SP, palette.secondaryText
        ))
        screen.addView(content, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        screen.addView(progress, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(FinanceDesignTokens.ENTRY_FOOTER_HEIGHT)
        ).apply {
            topMargin = dp(FinanceDesignTokens.SPACE_8)
            bottomMargin = dp(FinanceDesignTokens.SPACE_8)
        })
        screen.addView(FinanceDesignSystem.button(
            this, darkTheme, s(R.string.continue_to_login), primary = true
        ) { continueFromWelcome() }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(FinanceDesignTokens.BUTTON_HEIGHT)
        ))
        var startX = 0f
        var startY = 0f
        val swipe = View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { startX = event.x; startY = event.y; false }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - startX
                    val dy = event.y - startY
                    if (kotlin.math.abs(dx) >= dp(FinanceDesignTokens.SWIPE_THRESHOLD) &&
                        kotlin.math.abs(dx) > kotlin.math.abs(dy)
                    ) {
                        continueFromWelcome()
                        true
                    } else false
                }
                else -> false
            }
        }
        screen.setOnTouchListener(swipe)
        content.setOnTouchListener(swipe)
        artwork.setOnTouchListener(swipe)
        setContentView(screen)
    }

    private fun continueFromWelcome() {
        if (entryStage != EntryStage.WELCOME) return
        if (localAuthentication.isEnabled()) {
            showLoginScreen()
        } else {
            enterApplication()
        }
    }

    private fun showLoginScreen() {
        entryStage = EntryStage.LOGIN
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val palette = FinanceDesignSystem.colors(darkTheme)
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(palette.background)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(
                dp(FinanceDesignTokens.PAGE_HORIZONTAL_PADDING),
                dp(FinanceDesignTokens.PAGE_VERTICAL_PADDING),
                dp(FinanceDesignTokens.PAGE_HORIZONTAL_PADDING),
                dp(FinanceDesignTokens.PAGE_VERTICAL_PADDING)
            )
        }
        content.addView(FinanceComponents.brandSignature(
            this, darkTheme, R.drawable.ic_bye_pro_finance,
            s(R.string.entry_brand_meta), s(R.string.login_kicker)
        ), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(FinanceDesignTokens.ENTRY_BRAND_ROW_HEIGHT)
        ))
        content.addView(FinanceComponents.entryArtwork(
            this, darkTheme, R.drawable.ic_bye_pro_finance
        ), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(FinanceDesignTokens.ENTRY_LOGIN_ARTWORK_HEIGHT)
        ).apply {
            topMargin = dp(FinanceDesignTokens.SPACE_4)
            bottomMargin = dp(FinanceDesignTokens.SPACE_4)
        })
        content.addView(FinanceDesignSystem.text(
            this, s(R.string.login_kicker),
            FinanceDesignTokens.TEXT_ENTRY_KICKER_SP, palette.accent, bold = true
        ).apply { setPadding(0, dp(FinanceDesignTokens.SPACE_4), 0, dp(FinanceDesignTokens.SPACE_4)) })
        content.addView(FinanceDesignSystem.text(
            this, s(R.string.login_title),
            FinanceDesignTokens.TEXT_ENTRY_TITLE_SP, palette.text, bold = true
        ).apply {
            setLineSpacing(dp(FinanceDesignTokens.SPACE_4).toFloat(), 1f)
            setPadding(0, dp(FinanceDesignTokens.SPACE_4), 0, dp(FinanceDesignTokens.SPACE_4))
        })
        content.addView(FinanceDesignSystem.text(
            this, s(R.string.login_description),
            FinanceDesignTokens.TEXT_BODY_SP, palette.secondaryText
        ).apply { setPadding(0, 0, 0, dp(FinanceDesignTokens.SPACE_12)) })

        val username = FinanceComponents.editorialInput(
            this, darkTheme, s(R.string.username), s(R.string.username),
            localAuthentication.currentUsername()
        )
        content.addView(username.container, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(FinanceDesignTokens.ENTRY_FIELD_BOTTOM_SPACE) })

        val passwordField = FinanceComponents.editorialInput(
            this, darkTheme, s(R.string.password), s(R.string.password)
        )
        val password = passwordField.input.apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        content.addView(passwordField.container, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(FinanceDesignTokens.SPACE_4) })

        val error = FinanceDesignSystem.text(
            this, "", FinanceDesignTokens.TEXT_CAPTION_SP, palette.danger
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
            minHeight = dp(FinanceDesignTokens.ENTRY_ERROR_HEIGHT)
        }
        content.addView(error)
        content.addView(FinanceDesignSystem.button(
            this, darkTheme, s(R.string.login), primary = true
        ) {
            if (localAuthentication.authenticate(username.input.text.toString(), password.text.toString())) {
                enterApplication()
            } else {
                password.text?.clear()
                error.text = s(R.string.login_invalid)
                password.requestFocus()
            }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(FinanceDesignTokens.BUTTON_HEIGHT)
        ).apply { topMargin = dp(FinanceDesignTokens.SPACE_4) })
        content.addView(FinanceDesignSystem.text(
            this, s(R.string.login_security_note),
            FinanceDesignTokens.TEXT_CAPTION_SP, palette.secondaryText
        ).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, dp(FinanceDesignTokens.SPACE_12), 0, dp(FinanceDesignTokens.SPACE_8))
        })
        scroll.addView(content, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        setContentView(scroll)
    }

    private fun enterApplication() {
        entryStage = EntryStage.APP
        applySystemBars()
        buildApplicationShell()
        renderCurrentSection()
    }

    private fun localizedContext(): Context {
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(localeCode))
        return createConfigurationContext(configuration)
    }

    private fun s(resourceId: Int): String = localizedContext().getString(resourceId)

    private fun resolveDarkTheme(): Boolean {
        val systemIsDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
                Configuration.UI_MODE_NIGHT_NO
        return FinanceAppearance.isDark(
            FinanceAppearanceMode.fromStorage(themeMode),
            systemIsDark
        )
    }

    private fun applySystemBars() {
        val background = FinancePalette.background(darkTheme)
        window.setBackgroundDrawable(ColorDrawable(background))
        window.statusBarColor = background
        window.navigationBarColor = background
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (darkTheme) 0 else
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }

    private fun buildApplicationShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
            setBackgroundColor(FinancePalette.background(darkTheme))
        }

        root.addView(buildToolbar(), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(FinanceDesignTokens.APP_BAR_HEIGHT)
        ))

        scrollView = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        pageContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(
                dp(FinanceDesignTokens.PAGE_HORIZONTAL_PADDING),
                dp(FinanceDesignTokens.PAGE_VERTICAL_PADDING),
                dp(FinanceDesignTokens.PAGE_HORIZONTAL_PADDING),
                dp(FinanceDesignTokens.PAGE_VERTICAL_PADDING)
            )
        }
        scrollView.addView(pageContent)
        root.addView(scrollView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        navigation = buildNavigation()
        root.addView(navigation, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(FinanceDesignTokens.NAVIGATION_BAR_HEIGHT)
        ))
        setContentView(root)
    }

    private fun buildToolbar(): LinearLayout {
        val actions = mutableListOf<Pair<Int, Pair<String, () -> Unit>>>()
        if (activeSection == Section.WORK ||
            activeSection == Section.HOME ||
            activeSection == Section.DEBTS
        ) {
            actions += R.drawable.ic_add to (
                s(R.string.add_record) to {
                    showCreateMenu(root.getChildAt(0))
                }
            )
        }
        actions += R.drawable.ic_calendar to (
            s(R.string.calendar) to {
                showMonthPicker(root.getChildAt(0))
            }
        )
        actions += R.drawable.ic_calculator to (
            s(R.string.calculator) to { showCalculator() }
        )
        actions += R.drawable.ic_settings to (
            s(R.string.settings) to { showSettings() }
        )
        return FinanceComponents.topBar(
            context = this,
            isDark = darkTheme,
            title = sectionTitle(activeSection),
            actions = actions
        )
    }

    private fun buildBottomNavigationBar(): LinearLayout =
        FinanceComponents.bottomNavigation(
            this,
            darkTheme,
            listOf(
                FinanceNavigationItem(
                    s(R.string.dashboard), R.drawable.ic_dashboard,
                    activeSection == Section.DASHBOARD
                ) { activeSection = Section.DASHBOARD; renderCurrentSection() },
                FinanceNavigationItem(
                    s(R.string.work), R.drawable.ic_work,
                    activeSection == Section.WORK
                ) { activeSection = Section.WORK; renderCurrentSection() },
                FinanceNavigationItem(
                    s(R.string.home), R.drawable.ic_home,
                    activeSection == Section.HOME
                ) { activeSection = Section.HOME; renderCurrentSection() },
                FinanceNavigationItem(
                    s(R.string.debts), R.drawable.ic_debts,
                    activeSection == Section.DEBTS
                ) { activeSection = Section.DEBTS; renderCurrentSection() },
                FinanceNavigationItem(
                    s(R.string.ai), R.drawable.ic_ai,
                    activeSection == Section.AI
                ) { activeSection = Section.AI; renderCurrentSection() }
            )
        )

    private fun buildNavigation(): LinearLayout {
        navigation = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(FinancePalette.surface(darkTheme))
        }
        updateNavigationItems()
        return navigation
    }

    private fun updateNavigationItems() {
        navigation.removeAllViews()
        navigation.addView(
            FinanceComponents.bottomDivider(this, darkTheme),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(FinanceDesignTokens.DIVIDER_HEIGHT)
            )
        )
        navigation.addView(
            buildBottomNavigationBar(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
    }

    private fun renderCurrentSection() {
        pageContent.removeAllViews()
        when (activeSection) {
            Section.DASHBOARD -> renderDashboard()
            Section.WORK -> renderWork()
            Section.HOME -> renderHome()
            Section.DEBTS -> renderDebts()
            Section.AI -> renderAssistant()
        }
        scrollView.scrollTo(0, 0)
        refreshToolbar()
        updateNavigationItems()
    }

    private fun refreshToolbar() {
        if (root.childCount == 0) return
        root.removeViewAt(0)
        root.addView(
            buildToolbar(),
            0,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(FinanceDesignTokens.APP_BAR_HEIGHT)
            )
        )
    }

    private fun renderDashboard() {
        val summary = repository.loadDashboard(selectedMonth)
        addMonthSelector(summary)
        addGap(FinanceDesignTokens.SPACE_16)
        addSectionHeading(s(com.byepro.bye_pro_finance.R.string.financial_snapshot))
        addMetricGroup(
            MetricDisplay(s(com.byepro.bye_pro_finance.R.string.home_available_balance), summary.homeAvailableBalance, R.drawable.ic_payments, true),
            MetricDisplay(s(com.byepro.bye_pro_finance.R.string.work_remaining), summary.workRemaining, R.drawable.ic_work),
            MetricDisplay(s(com.byepro.bye_pro_finance.R.string.total_work_outstanding), summary.totalWorkOutstanding, R.drawable.ic_calendar)
        )

        addSectionHeading(s(com.byepro.bye_pro_finance.R.string.work))
        addMetricGroup(
            MetricDisplay(s(com.byepro.bye_pro_finance.R.string.salary_total), summary.salaryTotal, R.drawable.ic_payments),
            MetricDisplay(s(com.byepro.bye_pro_finance.R.string.advances_total), summary.advancesTotal, R.drawable.ic_advance),
            MetricDisplay(s(com.byepro.bye_pro_finance.R.string.payments_total), summary.paymentsTotal, R.drawable.ic_payments),
            MetricDisplay(s(com.byepro.bye_pro_finance.R.string.current_salary_outstanding), summary.currentSalaryOutstanding, R.drawable.ic_calendar)
        )

        addSectionHeading(s(com.byepro.bye_pro_finance.R.string.home))
        addMetricGroup(
            MetricDisplay(s(com.byepro.bye_pro_finance.R.string.home_expenses_total), summary.homeExpensesTotal, R.drawable.ic_work),
            MetricDisplay(s(com.byepro.bye_pro_finance.R.string.transfers_total), summary.transfersTotal, R.drawable.ic_transfer),
            MetricDisplay(s(com.byepro.bye_pro_finance.R.string.bills_outstanding), summary.billsOutstanding, R.drawable.ic_payments),
            MetricDisplay(s(com.byepro.bye_pro_finance.R.string.bills_paid), summary.billsPaid, R.drawable.ic_bill)
        )

        addSectionHeading(s(com.byepro.bye_pro_finance.R.string.cash_flow))
        addCashFlowComparison(
            summary.transfersTotal,
            summary.homeExpensesTotal
        )

        addSectionHeading(s(com.byepro.bye_pro_finance.R.string.debts))
        addMetricGroup(
            MetricDisplay(s(com.byepro.bye_pro_finance.R.string.creditor_balance), summary.creditorBalance, R.drawable.ic_payments),
            MetricDisplay(s(com.byepro.bye_pro_finance.R.string.debtor_balance), summary.debtorBalance, R.drawable.ic_advance)
        )

        addSectionHeading(s(com.byepro.bye_pro_finance.R.string.recent_activity))
        addActivityGroup(summary.recentActivity)
        addGap(FinanceDesignTokens.SPACE_8)
    }

    private fun addMonthSelector(summary: DashboardSummary) {
        pageContent.addView(
            FinanceComponents.monthSelector(
                context = this,
                isDark = darkTheme,
                caption = s(R.string.month_shown),
                selectedMonth = monthDisplayLabel(selectedMonth),
                contentDescription = s(R.string.month_shown) + ": " + monthDisplayLabel(selectedMonth),
                onClick = { anchor -> showMonthPicker(anchor, summary.availableMonths) },
                onCalendarClick = { showMonthCalendarPicker() }
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private data class MetricDisplay(
        val label: String,
        val amount: Long,
        val icon: Int,
        val emphasized: Boolean = false
    )

    private fun addMetricGroup(vararg metrics: MetricDisplay) {
        val group = FinanceComponents.groupedList(this, darkTheme)
        metrics.forEachIndexed { index, metric ->
            if (index > 0) {
                group.addView(FinanceComponents.bottomDivider(this, darkTheme))
            }
            group.addView(
                FinanceComponents.metricRow(
                    context = this,
                    isDark = darkTheme,
                    iconResource = metric.icon,
                    title = metric.label,
                    value = formatAmount(metric.amount),
                    emphasized = metric.emphasized
                ),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        pageContent.addView(group, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(FinanceDesignTokens.SECTION_SPACING)
        })
    }

    private fun addSectionHeading(title: String) {
        pageContent.addView(
            FinanceComponents.sectionHeader(this, darkTheme, title),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(FinanceDesignTokens.SECTION_HEADER_HEIGHT)
            )
        )
    }

    private fun addCashFlowComparison(transfers: Long, expenses: Long) {
        val total = maxOf(transfers, expenses, 1L).toFloat()
        val container = FinanceComponents.card(this, darkTheme)
        addComparisonBar(
            container,
            s(R.string.transfers_total),
            transfers,
            transfers / total
        )
        addGapTo(container, FinanceDesignTokens.SECTION_SPACING)
        addComparisonBar(
            container,
            s(R.string.home_expenses_total),
            expenses,
            expenses / total
        )
        pageContent.addView(container, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(FinanceDesignTokens.SECTION_SPACING)
        })
    }

    private fun addComparisonBar(
        parent: LinearLayout,
        label: String,
        value: Long,
        ratio: Float
    ) {
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        head.addView(makeText(label, FinanceDesignTokens.TEXT_LABEL_SP, false), LinearLayout.LayoutParams(0, dp(FinanceDesignTokens.CHART_LABEL_ROW_HEIGHT), 1f))
        head.addView(makeText(formatAmount(value), FinanceDesignTokens.TEXT_BODY_SP, true), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(FinanceDesignTokens.CHART_LABEL_ROW_HEIGHT)
        ))
        parent.addView(head)
        val track = View(this).apply {
            setRoundedBackground(FinancePalette.raised(darkTheme), FinanceDesignTokens.CHART_BAR_RADIUS_DP)
        }
        parent.addView(track, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(FinanceDesignTokens.CHART_BAR_HEIGHT)
        ).apply { topMargin = dp(FinanceDesignTokens.CHART_BAR_TOP_GAP) })
        val fill = View(this).apply {
            setRoundedBackground(FinancePalette.accent(darkTheme), FinanceDesignTokens.CHART_BAR_RADIUS_DP)
        }
        // Keep the indicator RTL and proportional without creating a second
        // transaction or changing any stored monetary value.
        val measuredWidth = (resources.displayMetrics.widthPixels - dp(FinanceDesignTokens.CHART_HORIZONTAL_INSET)).coerceAtLeast(1)
        val width = (measuredWidth * ratio).toInt().coerceAtLeast(if (value == 0L) 0 else dp(FinanceDesignTokens.CHART_MIN_VISIBLE_FILL))
        parent.addView(fill, LinearLayout.LayoutParams(width, dp(FinanceDesignTokens.CHART_BAR_HEIGHT)).apply {
            topMargin = -dp(FinanceDesignTokens.CHART_BAR_HEIGHT)
            gravity = Gravity.END
        })
    }

    private fun addActivityGroup(items: List<ActivityItem>) {
        if (items.isEmpty()) {
            addEmptyState(s(R.string.no_recent_activity))
            return
        }
        val group = FinanceComponents.groupedList(this, darkTheme)
        items.forEachIndexed { index, item ->
            if (index > 0) {
                group.addView(FinanceComponents.bottomDivider(this, darkTheme))
            }
            group.addView(
                FinanceComponents.recordRow(
                    context = this,
                    isDark = darkTheme,
                    iconResource = activityIcon(item.icon),
                    title = activityTitle(item.title),
                    subtitle = item.subtitle.ifBlank { formatDate(item.timestamp) },
                    amount = formatAmount(item.amountMinorUnits)
                )
            )
        }
        pageContent.addView(group)
    }

    private fun renderWork() {
        addSectionHeading(s(R.string.work_records))
        addMonthQuickPicker()
        val tabs = WorkCategory.ordered.map { category ->
            val titleResource = when (category) {
                WorkCategory.SALARY -> R.string.work_salary
                WorkCategory.PAYMENT -> R.string.work_payment
                WorkCategory.ADVANCE -> R.string.work_advance
                WorkCategory.REMAINING -> R.string.work_remaining
                WorkCategory.HOME_TRANSFERS -> R.string.work_home_transfers
            }
            FinanceCategoryTabItem(
                title = s(titleResource),
                iconResource = workCategoryIcon(category),
                selected = selectedWorkCategory == category
            ) {
                selectedWorkCategory = category
                renderCurrentSection()
            }
        }
        pageContent.addView(FinanceComponents.categoryTabs(this, darkTheme, tabs),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addGap(FinanceDesignTokens.SPACE_8)

        val workRecords = repository.loadWorkRecords(selectedMonth)
        val records = when (selectedWorkCategory) {
            WorkCategory.SALARY -> workRecords.filter { it.kind == "salary" }
            WorkCategory.PAYMENT -> workRecords.filter { it.kind == "payment" || it.kind == "salaryPayment" }
            WorkCategory.ADVANCE -> workRecords.filter { it.kind == "advance" }
            WorkCategory.REMAINING -> emptyList()
            WorkCategory.HOME_TRANSFERS -> repository.loadTransfers(selectedMonth)
        }.sortedByDescending { it.timestamp }

        if (selectedWorkCategory == WorkCategory.REMAINING) {
            val summary = repository.loadDashboard(selectedMonth)
            pageContent.addView(FinanceComponents.balanceStatement(
                this, darkTheme, s(R.string.work_remaining),
                formatAmount(summary.workRemaining), s(R.string.work_remaining_formula)
            ), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            pageContent.addView(FinanceComponents.metricRow(
                this, darkTheme, R.drawable.ic_work_salary,
                s(R.string.current_salary_outstanding),
                formatAmount(summary.currentSalaryOutstanding), emphasized = false
            ), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            return
        }
        if (records.isEmpty()) {
            addEmptyState(s(R.string.no_records))
            return
        }
        addRecordGroup(records)
    }

    private fun renderHome() {
        addSectionHeading(s(R.string.home_records))
        addMonthQuickPicker()
        val tabs = HomeCategory.ordered.map { category ->
            val titleResource = when (category) {
                HomeCategory.MONTHLY_BILLS -> R.string.home_monthly_bills
                HomeCategory.RENT -> R.string.home_rent
                HomeCategory.EXPENSES -> R.string.home_expenses
                HomeCategory.RECEIVED_FROM_WORK -> R.string.home_received_from_work
            }
            FinanceCategoryTabItem(
                title = s(titleResource),
                iconResource = homeCategoryIcon(category),
                selected = selectedHomeCategory == category
            ) {
                selectedHomeCategory = category
                renderCurrentSection()
            }
        }
        pageContent.addView(FinanceComponents.categoryTabs(this, darkTheme, tabs),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addGap(FinanceDesignTokens.SECTION_SPACING)

        val records = when (selectedHomeCategory.source) {
            HomeRecordSource.BILLS -> repository.loadBills(selectedMonth, requireNotNull(selectedHomeCategory.billKind))
            HomeRecordSource.HOME_EXPENSES -> repository.loadHomeExpenses(selectedMonth)
            HomeRecordSource.WORK_TRANSFERS -> repository.loadTransfers(selectedMonth)
        }.sortedByDescending { if (it.kind == "bill") Long.MAX_VALUE else it.timestamp }

        if (selectedHomeCategory == HomeCategory.RECEIVED_FROM_WORK) {
            val total = repository.loadDashboard(selectedMonth).transfersTotal
            pageContent.addView(FinanceComponents.balanceStatement(
                this, darkTheme, s(R.string.home_received_from_work_total),
                formatAmount(total), s(R.string.home_received_from_work_total_caption)
            ), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addGap(FinanceDesignTokens.SPACE_8)
        }
        if (records.isEmpty()) {
            addEmptyState(s(R.string.no_records))
            return
        }
        addRecordGroup(records)
    }

    private fun renderDebts() {
        addSectionHeading(s(com.byepro.bye_pro_finance.R.string.debt_records))
        addMonthQuickPicker()
        val records = repository.loadDebts()
        if (records.isEmpty()) {
            addEmptyState(s(com.byepro.bye_pro_finance.R.string.no_records))
            return
        }
        addRecordGroup(records)
    }

    private fun renderAssistant() {
        addSectionHeading(s(R.string.ai))
        addMonthQuickPicker()
        val panel = FinanceComponents.card(this, darkTheme)
        val icon = FinanceDesignSystem.iconBox(
            this,
            darkTheme,
            R.drawable.ic_ai,
            selected = true
        )
        panel.addView(icon, LinearLayout.LayoutParams(dp(FinanceDesignTokens.RECORD_ICON_BOX_SIZE), dp(FinanceDesignTokens.RECORD_ICON_BOX_SIZE)))
        panel.addView(makeText(
            s(R.string.ai_migration_notice),
            FinanceDesignTokens.TEXT_BODY_SP,
            false
        ).apply {
            setPadding(0, dp(FinanceDesignTokens.SECTION_SPACING), 0, 0)
        })
        pageContent.addView(panel, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    private fun addMonthQuickPicker() {
        val summary = repository.loadDashboard(selectedMonth)
        addMonthSelector(summary)
        addGap(FinanceDesignTokens.SECTION_SPACING)
    }

    private fun addRecordGroup(records: List<FinanceRecord>) {
        val group = FinanceComponents.groupedList(this, darkTheme)
        records.forEachIndexed { index, record ->
            if (index > 0) {
                group.addView(FinanceComponents.bottomDivider(this, darkTheme))
            }
            group.addView(
                FinanceComponents.recordRow(
                    context = this,
                    isDark = darkTheme,
                    iconResource = recordIcon(record),
                    title = recordTitle(record),
                    subtitle = recordDetail(record),
                    amount = formatAmount(record.amountMinorUnits),
                    onClick = { clickedRow ->
                        if (record.kind in setOf(
                            "bill", "creditor", "debtor", "debtPayment", "billPayment"
                        )) showRecordActions(clickedRow, record)
                    },
                    onLongClick = { clickedRow ->
                        showRecordActions(clickedRow, record)
                    }
                )
            )
        }
        pageContent.addView(group)
    }

    private fun addEmptyState(message: String) {
        pageContent.addView(
            FinanceComponents.emptyState(this, darkTheme, message),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun showCreateMenu(anchor: View) {
        val actions = mutableListOf<FinanceIconOptionItem>()
        when (activeSection) {
            Section.WORK -> {
                actions += FinanceIconOptionItem(s(R.string.new_salary), R.drawable.ic_work_salary) {
                    selectedWorkCategory = WorkCategory.SALARY; showWorkEntryForm("salary")
                }
                actions += FinanceIconOptionItem(s(R.string.new_work_payment), R.drawable.ic_work_payment) {
                    selectedWorkCategory = WorkCategory.PAYMENT; showWorkEntryForm("payment")
                }
                actions += FinanceIconOptionItem(s(R.string.new_advance), R.drawable.ic_work_advance) {
                    selectedWorkCategory = WorkCategory.ADVANCE; showWorkEntryForm("advance")
                }
                actions += FinanceIconOptionItem(s(R.string.new_transfer), R.drawable.ic_work_transfers) {
                    selectedWorkCategory = WorkCategory.HOME_TRANSFERS; showTransferForm()
                }
            }
            Section.HOME -> {
                actions += FinanceIconOptionItem(s(R.string.new_bill), R.drawable.ic_home_bills) {
                    selectedHomeCategory = HomeCategory.MONTHLY_BILLS; showBillForm(billKind = "monthly")
                }
                actions += FinanceIconOptionItem(s(R.string.new_rent), R.drawable.ic_home_rent) {
                    selectedHomeCategory = HomeCategory.RENT; showBillForm(billKind = "rent")
                }
                actions += FinanceIconOptionItem(s(R.string.new_expense), R.drawable.ic_home_expenses) {
                    selectedHomeCategory = HomeCategory.EXPENSES; showHomeExpenseForm()
                }
            }
            Section.DEBTS -> {
                actions += FinanceIconOptionItem(s(R.string.new_creditor), R.drawable.ic_debts) { showDebtForm("creditor") }
                actions += FinanceIconOptionItem(s(R.string.new_debtor), R.drawable.ic_debts) { showDebtForm("debtor") }
            }
            else -> Unit
        }
        if (actions.isEmpty()) return
        lateinit var dialog: AlertDialog
        val choices = actions.map { item -> item.copy(onClick = { dialog.dismiss(); item.onClick() }) }
        dialog = dialogBuilder().setTitle(s(R.string.add_record))
            .setView(FinanceComponents.iconChoiceList(this, darkTheme, choices))
            .setNegativeButton(s(R.string.cancel), null).create()
        dialog.show()
    }

    private fun addMenuAction(menu: PopupMenu, title: String, action: () -> Unit) {
        menu.menu.add(title).setOnMenuItemClickListener {
            action()
            true
        }
    }

    private fun createForm(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(
            dp(FinanceDesignTokens.CARD_PADDING),
            dp(FinanceDesignTokens.SECTION_SPACING),
            dp(FinanceDesignTokens.CARD_PADDING),
            dp(FinanceDesignTokens.SECTION_SPACING / 2)
        )
    }

    private fun formInput(
        form: LinearLayout, hint: String, initial: String = "", numeric: Boolean = false
    ): EditText {
        val multiline = hint == s(R.string.note_optional)
        form.addView(FinanceDesignSystem.text(
            this, hint, FinanceDesignTokens.TEXT_CAPTION_SP, FinancePalette.text(darkTheme), bold = true
        ).apply { setPadding(dp(FinanceDesignTokens.SPACE_2), 0, dp(FinanceDesignTokens.SPACE_2), dp(FinanceDesignTokens.SPACE_4)) })
        val field = FinanceDesignSystem.input(this, darkTheme, hint, initial, numeric, multiline)
        if (numeric) {
            field.keyListener = DigitsKeyListener.getInstance("0123456789")
            field.inputType = InputType.TYPE_CLASS_NUMBER
            field.textDirection = View.TEXT_DIRECTION_LTR
            field.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            field.setTextLocale(Locale.US)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(field, LinearLayout.LayoutParams(0,
            if (multiline) dp(FinanceDesignTokens.MULTILINE_INPUT_HEIGHT) else dp(FinanceDesignTokens.BUTTON_HEIGHT), 1f))
        if (numeric) {
            row.addView(FinanceDesignSystem.iconAction(this, darkTheme, R.drawable.ic_calculator, s(R.string.calculator)) {
                showCalculator { result -> field.setText(result) }
            }, LinearLayout.LayoutParams(dp(FinanceDesignTokens.TOUCH_TARGET_SIZE), dp(FinanceDesignTokens.BUTTON_HEIGHT)).apply {
                marginStart = dp(FinanceDesignTokens.SPACE_4)
            })
        }
        form.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(FinanceDesignTokens.SECTION_SPACING / 2)
        })
        return field
    }

    private data class DateField(
        val view: TextView,
        val value: () -> Long
    )

    private fun addDateField(
        form: LinearLayout,
        label: String,
        initialEpochMillis: Long
    ): DateField {
        var selectedMillis = initialEpochMillis
        val view = FinanceDesignSystem.text(
            this, "", FinanceDesignTokens.TEXT_BODY_SP, FinancePalette.accent(darkTheme), bold = true
        ).apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_calendar, 0, 0, 0)
            compoundDrawableTintList = android.content.res.ColorStateList.valueOf(FinancePalette.accent(darkTheme))
            compoundDrawablePadding = dp(FinanceDesignTokens.SPACE_8)
            setPadding(dp(FinanceDesignTokens.INPUT_HORIZONTAL_PADDING), 0, dp(FinanceDesignTokens.INPUT_HORIZONTAL_PADDING), 0)
            background = FinanceDesignSystem.rounded(
                this@MainActivity,
                FinancePalette.raised(darkTheme),
                FinanceDesignTokens.CONTROL_RADIUS
            )
            minHeight = dp(FinanceDesignTokens.BUTTON_HEIGHT)
            isClickable = true
            isFocusable = true
        }
        fun refresh() {
            view.text = "$label: ${formatDate(selectedMillis)}  ▾"
        }
        refresh()
        view.setOnClickListener {
            val initial = java.util.Calendar.getInstance().apply { timeInMillis = selectedMillis }
            showFlutterStyleDatePicker(initial) { picked ->
                selectedMillis = picked.timeInMillis
                refresh()
            }
        }
        form.addView(view, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(FinanceDesignTokens.BUTTON_HEIGHT)
        ).apply {
            topMargin = dp(FinanceDesignTokens.SECTION_SPACING / 2)
            bottomMargin = dp(FinanceDesignTokens.SECTION_SPACING / 2)
        })
        return DateField(view) { selectedMillis }
    }

    private fun showWorkEntryForm(kind: String, existing: FinanceRecord? = null) {
        val form = createForm()
        val amount = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.amount),
            existing?.amountMinorUnits?.toString().orEmpty(),
            numeric = true
        )
        val note = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.note_optional),
            existing?.note.orEmpty()
        )
        val dueYear = existing?.dueYear ?: selectedMonth.year
        val dueMonth = existing?.dueMonth ?: selectedMonth.monthValue

        dialogBuilder()
            .setTitle(
                when (kind) {
                    "salary" -> s(com.byepro.bye_pro_finance.R.string.new_salary)
                    "advance" -> s(com.byepro.bye_pro_finance.R.string.new_advance)
                    else -> s(com.byepro.bye_pro_finance.R.string.new_work_payment)
                }
            )
            .setView(form)
            .setPositiveButton(s(com.byepro.bye_pro_finance.R.string.save)) { _, _ ->
                val value = amount.text.toString().trim().toLongOrNull()
                if (value == null || value <= 0L) {
                    showRuleMessage(FinanceRuleException(FinanceRuleException.Rule.INVALID_AMOUNT))
                    return@setPositiveButton
                }
                try {
                    writeRepository.saveWorkEntry(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        kind = existing?.kind ?: kind,
                        amountMinorUnits = value,
                        dueYear = dueYear,
                        dueMonth = dueMonth,
                        recordedAtEpochMillis = existing?.timestamp ?: System.currentTimeMillis(),
                        note = note.text.toString().trim().ifEmpty { null }
                    )
                    showToast(s(com.byepro.bye_pro_finance.R.string.saved_successfully))
                    renderCurrentSection()
                } catch (error: Throwable) {
                    showRuleMessage(error)
                }
            }
            .setNegativeButton(s(com.byepro.bye_pro_finance.R.string.cancel), null)
            .show()
    }

    private fun showTransferForm(existing: FinanceRecord? = null) {
        val form = createForm()
        val amount = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.amount),
            existing?.amountMinorUnits?.toString().orEmpty(),
            numeric = true
        )
        val note = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.note_optional),
            existing?.note.orEmpty()
        )
        val date = addDateField(
            form,
            s(com.byepro.bye_pro_finance.R.string.payment_date),
            existing?.timestamp ?: System.currentTimeMillis()
        )

        dialogBuilder()
            .setTitle(s(com.byepro.bye_pro_finance.R.string.new_transfer))
            .setView(form)
            .setPositiveButton(s(com.byepro.bye_pro_finance.R.string.save)) { _, _ ->
                val value = amount.text.toString().trim().toLongOrNull()
                if (value == null || value <= 0L) {
                    showRuleMessage(FinanceRuleException(FinanceRuleException.Rule.INVALID_AMOUNT))
                    return@setPositiveButton
                }
                try {
                    writeRepository.saveTransfer(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        amountMinorUnits = value,
                        transferredAtEpochMillis = date.value(),
                        note = note.text.toString().trim().ifEmpty { null }
                    )
                    showToast(s(com.byepro.bye_pro_finance.R.string.saved_successfully))
                    renderCurrentSection()
                } catch (error: Throwable) {
                    showRuleMessage(error)
                }
            }
            .setNegativeButton(s(com.byepro.bye_pro_finance.R.string.cancel), null)
            .show()
    }

    private fun showHomeExpenseForm(existing: FinanceRecord? = null) {
        val form = createForm()
        val amount = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.amount),
            existing?.amountMinorUnits?.toString().orEmpty(),
            numeric = true
        )
        val categories = listOf(
            "groceries", "utilities", "housing", "cleaning", "maintenance",
            "transport", "personal", "health", "other"
        )
        var selectedCategoryId = existing?.categoryId
            ?.takeIf { it in categories } ?: "other"
        val category = FinanceComponents.selectField(
            context = this, isDark = darkTheme,
            label = categoryTitle(selectedCategoryId),
            iconResource = expenseCategoryIcon(selectedCategoryId)
        ) { field ->
            lateinit var categoryDialog: AlertDialog
            val options = categories.map { id ->
                FinanceIconOptionItem(
                    title = categoryTitle(id),
                    iconResource = expenseCategoryIcon(id),
                    selected = id == selectedCategoryId
                ) {
                    selectedCategoryId = id
                    field.text = categoryTitle(id)
                    field.setCompoundDrawablesRelativeWithIntrinsicBounds(expenseCategoryIcon(id), 0, 0, 0)
                    categoryDialog.dismiss()
                }
            }
            categoryDialog = dialogBuilder()
                .setTitle(s(R.string.select_expense_category))
                .setView(FinanceComponents.iconChoiceList(this, darkTheme, options))
                .setNegativeButton(s(R.string.cancel), null)
                .create()
            categoryDialog.show()
        }
        form.addView(category, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(FinanceDesignTokens.BUTTON_HEIGHT)
        ).apply {
            bottomMargin = dp(FinanceDesignTokens.SECTION_SPACING / 2)
        })

        val note = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.note_optional),
            existing?.note.orEmpty()
        )
        val date = addDateField(
            form,
            s(com.byepro.bye_pro_finance.R.string.payment_date),
            existing?.timestamp ?: System.currentTimeMillis()
        )
        val dueYear = existing?.dueYear ?: selectedMonth.year
        val dueMonth = existing?.dueMonth ?: selectedMonth.monthValue

        dialogBuilder()
            .setTitle(s(com.byepro.bye_pro_finance.R.string.new_expense))
            .setView(form)
            .setPositiveButton(s(com.byepro.bye_pro_finance.R.string.save)) { _, _ ->
                val value = amount.text.toString().trim().toLongOrNull()
                if (value == null || value <= 0L || selectedCategoryId.isBlank()) {
                    showRuleMessage(FinanceRuleException(FinanceRuleException.Rule.INVALID_AMOUNT))
                    return@setPositiveButton
                }
                try {
                    writeRepository.saveHomeExpense(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        amountMinorUnits = value,
                        categoryId = selectedCategoryId,
                        expenseDateEpochMillis = date.value(),
                        dueYear = dueYear,
                        dueMonth = dueMonth,
                        note = note.text.toString().trim().ifEmpty { null }
                    )
                    showToast(s(com.byepro.bye_pro_finance.R.string.saved_successfully))
                    renderCurrentSection()
                } catch (error: Throwable) {
                    showRuleMessage(error)
                }
            }
            .setNegativeButton(s(com.byepro.bye_pro_finance.R.string.cancel), null)
            .show()
    }

    private fun showDebtForm(direction: String, existing: FinanceRecord? = null) {
        val form = createForm()
        val counterparty = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.counterparty),
            existing?.title.orEmpty()
        )
        val originalAmount = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.amount),
            (existing?.originalAmountMinorUnits ?: existing?.amountMinorUnits)
                ?.toString().orEmpty(),
            numeric = true
        )
        val note = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.note_optional),
            existing?.note.orEmpty()
        )
        val dueYear = existing?.dueYear ?: selectedMonth.year
        val dueMonth = existing?.dueMonth ?: selectedMonth.monthValue

        dialogBuilder()
            .setTitle(
                if (direction == "creditor")
                    s(com.byepro.bye_pro_finance.R.string.new_creditor)
                else s(com.byepro.bye_pro_finance.R.string.new_debtor)
            )
            .setView(form)
            .setPositiveButton(s(com.byepro.bye_pro_finance.R.string.save)) { _, _ ->
                val party = counterparty.text.toString().trim()
                val value = originalAmount.text.toString().trim().toLongOrNull()
                if (party.isBlank() || value == null || value <= 0L) {
                    showRuleMessage(FinanceRuleException(FinanceRuleException.Rule.INVALID_AMOUNT))
                    return@setPositiveButton
                }
                try {
                    writeRepository.saveDebt(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        direction = existing?.kind ?: direction,
                        originalAmountMinorUnits = value,
                        outstandingAmountMinorUnits = existing?.amountMinorUnits ?: value,
                        dueYear = dueYear,
                        dueMonth = dueMonth,
                        createdAtEpochMillis = existing?.timestamp ?: System.currentTimeMillis(),
                        counterparty = party,
                        note = note.text.toString().trim().ifEmpty { null }
                    )
                    showToast(s(com.byepro.bye_pro_finance.R.string.saved_successfully))
                    renderCurrentSection()
                } catch (error: Throwable) {
                    showRuleMessage(error)
                }
            }
            .setNegativeButton(s(com.byepro.bye_pro_finance.R.string.cancel), null)
            .show()
    }

    private fun showBillForm(existing: FinanceRecord? = null, billKind: String? = null) {
        val form = createForm()
        val title = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.bill_title),
            existing?.title.orEmpty()
        )
        val amount = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.amount),
            (existing?.originalAmountMinorUnits ?: existing?.amountMinorUnits)
                ?.toString().orEmpty(),
            numeric = true
        )
        val note = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.note_optional),
            existing?.note.orEmpty()
        )
        val dueYear = existing?.dueYear ?: selectedMonth.year
        val dueMonth = existing?.dueMonth ?: selectedMonth.monthValue
        val kind = billKind ?: if (existing?.detail?.startsWith("rent") == true) "rent" else "monthly"
        val formTitle = if (kind == "rent") R.string.new_rent else R.string.new_bill
        form.addView(FinanceComponents.formIconHeader(
            this, darkTheme,
            if (kind == "rent") R.drawable.ic_home_rent else R.drawable.ic_home_bills,
            s(formTitle),
            if (kind == "rent") s(R.string.home_rent_description) else s(R.string.home_monthly_bills_description)
        ))

        dialogBuilder()
            .setTitle(s(formTitle))
            .setView(form)
            .setPositiveButton(s(com.byepro.bye_pro_finance.R.string.save)) { _, _ ->
                val name = title.text.toString().trim()
                val value = amount.text.toString().trim().toLongOrNull()
                if (name.isBlank() || value == null || value <= 0L) {
                    showRuleMessage(FinanceRuleException(FinanceRuleException.Rule.INVALID_AMOUNT))
                    return@setPositiveButton
                }
                try {
                    writeRepository.saveBill(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        title = name,
                        kind = kind,
                        amountMinorUnits = value,
                        dueYear = dueYear,
                        dueMonth = dueMonth,
                        note = note.text.toString().trim().ifEmpty { null }
                    )
                    showToast(s(com.byepro.bye_pro_finance.R.string.saved_successfully))
                    renderCurrentSection()
                } catch (error: Throwable) {
                    showRuleMessage(error)
                }
            }
            .setNegativeButton(s(com.byepro.bye_pro_finance.R.string.cancel), null)
            .show()
    }

    private fun showDebtPaymentForm(debt: FinanceRecord) {
        val form = createForm()
        form.addView(makeText(
            s(com.byepro.bye_pro_finance.R.string.remaining_amount) + ": " +
                formatAmount(debt.amountMinorUnits),
            FinanceDesignTokens.TEXT_BODY_SP,
            true
        ))
        val amount = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.amount),
            numeric = true
        )
        val note = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.note_optional)
        )
        dialogBuilder()
            .setTitle(s(com.byepro.bye_pro_finance.R.string.record_payment))
            .setView(form)
            .setPositiveButton(s(com.byepro.bye_pro_finance.R.string.save)) { _, _ ->
                val value = amount.text.toString().trim().toLongOrNull()
                if (value == null || value <= 0L) {
                    showRuleMessage(FinanceRuleException(FinanceRuleException.Rule.INVALID_AMOUNT))
                    return@setPositiveButton
                }
                try {
                    writeRepository.recordDebtPayment(
                        id = UUID.randomUUID().toString(),
                        debtId = debt.id,
                        amountMinorUnits = value,
                        paidAtEpochMillis = System.currentTimeMillis(),
                        note = note.text.toString().trim().ifEmpty { null }
                    )
                    showToast(s(com.byepro.bye_pro_finance.R.string.payment_saved))
                    renderCurrentSection()
                } catch (error: Throwable) {
                    showRuleMessage(error)
                }
            }
            .setNegativeButton(s(com.byepro.bye_pro_finance.R.string.cancel), null)
            .show()
    }

    private fun showBillPaymentForm(bill: FinanceRecord) {
        val form = createForm()
        form.addView(makeText(
            s(com.byepro.bye_pro_finance.R.string.remaining_amount) + ": " +
                formatAmount(bill.amountMinorUnits),
            FinanceDesignTokens.TEXT_BODY_SP,
            true
        ))
        val amount = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.amount),
            numeric = true
        )
        val note = formInput(
            form,
            s(com.byepro.bye_pro_finance.R.string.note_optional)
        )
        dialogBuilder()
            .setTitle(s(com.byepro.bye_pro_finance.R.string.pay_bill))
            .setView(form)
            .setPositiveButton(s(com.byepro.bye_pro_finance.R.string.save)) { _, _ ->
                val value = amount.text.toString().trim().toLongOrNull()
                if (value == null || value <= 0L) {
                    showRuleMessage(FinanceRuleException(FinanceRuleException.Rule.INVALID_AMOUNT))
                    return@setPositiveButton
                }
                try {
                    writeRepository.recordBillPayment(
                        id = UUID.randomUUID().toString(),
                        billId = bill.id,
                        amountMinorUnits = value,
                        paidAtEpochMillis = System.currentTimeMillis(),
                        note = note.text.toString().trim().ifEmpty { null }
                    )
                    showToast(s(com.byepro.bye_pro_finance.R.string.payment_saved))
                    renderCurrentSection()
                } catch (error: Throwable) {
                    showRuleMessage(error)
                }
            }
            .setNegativeButton(s(com.byepro.bye_pro_finance.R.string.cancel), null)
            .show()
    }

    private fun showRecordActions(anchor: View, record: FinanceRecord) {
        val menu = PopupMenu(popupContext(), anchor)
        when (record.kind) {
            "creditor", "debtor" -> {
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.record_payment)) {
                    showDebtPaymentForm(record)
                }
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.payment_history)) {
                    showPaymentHistory(record, debtPayments = true)
                }
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.edit)) {
                    showDebtForm(record.kind, record)
                }
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.delete)) {
                    confirmDeleteRecord(record)
                }
            }
            "bill" -> {
                if (record.amountMinorUnits > 0L) {
                    addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.pay_bill)) {
                        showBillPaymentForm(record)
                    }
                }
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.payment_history)) {
                    showPaymentHistory(record, debtPayments = false)
                }
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.edit)) {
                    showBillForm(record)
                }
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.delete)) {
                    confirmDeleteRecord(record)
                }
            }
            "debtPayment", "billPayment" -> {
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.undo_payment)) {
                    confirmUndoPayment(record)
                }
            }
            else -> {
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.edit)) {
                    when (record.kind) {
                        "transfer" -> showTransferForm(record)
                        "expense" -> showHomeExpenseForm(record)
                        else -> showWorkEntryForm(record.kind, record)
                    }
                }
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.delete)) {
                    confirmDeleteRecord(record)
                }
            }
        }
        menu.show()
    }

    private fun showPaymentHistory(record: FinanceRecord, debtPayments: Boolean) {
        val payments = if (debtPayments) {
            repository.loadDebtPayments(record.id)
        } else {
            repository.loadBillPayments(record.id)
        }
        if (payments.isEmpty()) {
            dialogBuilder()
                .setTitle(s(com.byepro.bye_pro_finance.R.string.payment_history))
                .setMessage(s(com.byepro.bye_pro_finance.R.string.no_payment_history))
                .setPositiveButton(s(com.byepro.bye_pro_finance.R.string.cancel), null)
                .show()
            return
        }
        val items = payments.map {
            formatDate(it.timestamp) + " • " + formatAmount(it.amountMinorUnits)
        }.toTypedArray()
        dialogBuilder()
            .setTitle(s(com.byepro.bye_pro_finance.R.string.payment_history))
            .setItems(items) { _, index ->
                confirmUndoPayment(payments[index])
            }
            .setNegativeButton(s(com.byepro.bye_pro_finance.R.string.cancel), null)
            .show()
    }

    private fun confirmUndoPayment(payment: FinanceRecord) {
        dialogBuilder()
            .setTitle(s(com.byepro.bye_pro_finance.R.string.undo_payment))
            .setMessage(formatDate(payment.timestamp) + " • " +
                formatAmount(payment.amountMinorUnits))
            .setPositiveButton(s(com.byepro.bye_pro_finance.R.string.undo_payment)) { _, _ ->
                try {
                    if (payment.kind == "debtPayment") {
                        writeRepository.undoDebtPayment(payment.id)
                    } else {
                        writeRepository.undoBillPayment(payment.id)
                    }
                    showToast(s(com.byepro.bye_pro_finance.R.string.deleted_successfully))
                    renderCurrentSection()
                } catch (error: Throwable) {
                    showRuleMessage(error)
                }
            }
            .setNegativeButton(s(com.byepro.bye_pro_finance.R.string.cancel), null)
            .show()
    }

    private fun confirmDeleteRecord(record: FinanceRecord) {
        dialogBuilder()
            .setTitle(s(com.byepro.bye_pro_finance.R.string.delete))
            .setMessage(s(com.byepro.bye_pro_finance.R.string.confirm_delete))
            .setPositiveButton(s(com.byepro.bye_pro_finance.R.string.delete)) { _, _ ->
                try {
                    when (record.kind) {
                        "transfer" -> writeRepository.deleteTransfer(record.id)
                        "expense" -> writeRepository.deleteHomeExpense(record.id)
                        "creditor", "debtor" -> writeRepository.deleteDebt(record.id)
                        "bill" -> writeRepository.deleteBill(record.id)
                        else -> writeRepository.deleteWorkEntry(record.id)
                    }
                    showToast(s(com.byepro.bye_pro_finance.R.string.deleted_successfully))
                    renderCurrentSection()
                } catch (error: Throwable) {
                    showRuleMessage(error)
                }
            }
            .setNegativeButton(s(com.byepro.bye_pro_finance.R.string.cancel), null)
            .show()
    }

    private fun dialogBuilder(): AlertDialog.Builder =
        AlertDialog.Builder(
            this,
            if (darkTheme) R.style.NativeFinanceDialogDarkTheme
            else R.style.NativeFinanceDialogLightTheme
        )

    private fun popupContext(): Context = ContextThemeWrapper(
        this,
        if (darkTheme) R.style.NativeFinancePopupDarkTheme
        else R.style.NativeFinancePopupLightTheme
    )

    private fun showRuleMessage(error: Throwable) {
        val resource = when ((error as? FinanceRuleException)?.rule) {
            FinanceRuleException.Rule.INVALID_AMOUNT,
            FinanceRuleException.Rule.INVALID_MONTH ->
                com.byepro.bye_pro_finance.R.string.invalid_amount
            FinanceRuleException.Rule.INSUFFICIENT_WORK_BALANCE ->
                com.byepro.bye_pro_finance.R.string.insufficient_work_balance
            FinanceRuleException.Rule.TRANSFER_WOULD_OVERDRAW_HOME ->
                com.byepro.bye_pro_finance.R.string.transfer_overdraw_warning
            FinanceRuleException.Rule.INSUFFICIENT_HOME_BALANCE ->
                com.byepro.bye_pro_finance.R.string.insufficient_home_balance
            FinanceRuleException.Rule.DEBT_PAYMENT_EXCEEDS_BALANCE ->
                com.byepro.bye_pro_finance.R.string.debt_payment_exceeds
            FinanceRuleException.Rule.BILL_PAYMENT_EXCEEDS_BALANCE ->
                com.byepro.bye_pro_finance.R.string.bill_payment_exceeds
            FinanceRuleException.Rule.DEBT_HAS_PAYMENT_HISTORY,
            FinanceRuleException.Rule.BILL_HAS_PAYMENT_HISTORY ->
                com.byepro.bye_pro_finance.R.string.record_has_payment_history
            FinanceRuleException.Rule.BILL_AMOUNT_BELOW_PAYMENTS ->
                com.byepro.bye_pro_finance.R.string.bill_payment_exceeds
            FinanceRuleException.Rule.DEBT_BALANCE_DOES_NOT_MATCH_HISTORY ->
                com.byepro.bye_pro_finance.R.string.debt_payment_exceeds
            FinanceRuleException.Rule.INSUFFICIENT_BILL_PAYMENT_BALANCE ->
                com.byepro.bye_pro_finance.R.string.insufficient_home_balance
            FinanceRuleException.Rule.RECORD_NOT_FOUND -> 
                com.byepro.bye_pro_finance.R.string.data_error
            null -> com.byepro.bye_pro_finance.R.string.data_error
        }
        dialogBuilder()
            .setMessage(s(resource))
            .setPositiveButton(s(com.byepro.bye_pro_finance.R.string.cancel), null)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun monthDisplayLabel(month: YearMonth): String {
        val displayLocale = if (localeCode == "ar") Locale("ar") else Locale.US
        return DateTimeFormatter.ofPattern("MMMM yyyy", displayLocale)
            .withDecimalStyle(DecimalStyle.STANDARD)
            .format(month.atDay(1))
    }

    private fun monthStorageLabel(month: YearMonth): String =
        String.format(Locale.US, "%04d-%02d", month.year, month.monthValue)

    private fun showMonthPicker(anchor: View, months: List<YearMonth>? = null) {
        val available = (months ?: repository.loadDashboard(selectedMonth).availableMonths)
            .distinct().sortedDescending()
            .ifEmpty { listOf(selectedMonth) }
        val palette = FinanceDesignSystem.colors(darkTheme)
        val dialog = Dialog(ContextThemeWrapper(
            this,
            if (darkTheme) R.style.NativeFinanceDialogDarkTheme else R.style.NativeFinanceDialogLightTheme
        ))
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(FinanceDesignTokens.CARD_PADDING), dp(FinanceDesignTokens.SPACE_12),
                dp(FinanceDesignTokens.CARD_PADDING), dp(FinanceDesignTokens.SPACE_16))
            background = GradientDrawable().apply {
                val radius = dp(24).toFloat()
                cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
                setColor(palette.surface)
            }
        }
        val handle = View(this).apply {
            background = FinanceDesignSystem.rounded(this@MainActivity, palette.divider, 8)
        }
        root.addView(handle, LinearLayout.LayoutParams(dp(40), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(FinanceDesignTokens.SPACE_12)
        })
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(FinanceDesignSystem.text(
            this, s(R.string.month_shown), FinanceDesignTokens.TEXT_SECTION_TITLE_SP,
            palette.text, bold = true
        ), LinearLayout.LayoutParams(0, dp(FinanceDesignTokens.BUTTON_HEIGHT), 1f))
        heading.addView(FinanceDesignSystem.iconAction(
            this, darkTheme, R.drawable.ic_close, s(R.string.cancel)
        ) { dialog.dismiss() }, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(heading)
        root.addView(FinanceDesignSystem.text(
            this, monthDisplayLabel(selectedMonth), FinanceDesignTokens.TEXT_BALANCE_SP,
            palette.accent, bold = true
        ).apply {
            setPadding(0, dp(FinanceDesignTokens.SPACE_4), 0, dp(FinanceDesignTokens.SPACE_12))
        })
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        available.forEach { month ->
            val selected = month == selectedMonth
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(56)
                setPadding(dp(FinanceDesignTokens.SPACE_12), 0,
                    dp(FinanceDesignTokens.SPACE_12), 0)
                background = if (selected)
                    FinanceDesignSystem.rounded(this@MainActivity, palette.accentSoft, FinanceDesignTokens.CONTROL_RADIUS)
                    else ColorDrawable(android.graphics.Color.TRANSPARENT)
                isClickable = true
                isFocusable = true
                foreground = FinanceDesignSystem.ripple(this@MainActivity, darkTheme)
                setOnClickListener {
                    dialog.dismiss()
                    selectMonth(month)
                }
            }
            row.addView(FinanceDesignSystem.iconBox(this, darkTheme, R.drawable.ic_calendar, selected),
                LinearLayout.LayoutParams(dp(FinanceDesignTokens.RECORD_ICON_BOX_SIZE),
                    dp(FinanceDesignTokens.RECORD_ICON_BOX_SIZE)))
            row.addView(FinanceDesignSystem.text(
                this, monthDisplayLabel(month), FinanceDesignTokens.TEXT_BODY_SP,
                if (selected) palette.accent else palette.text, bold = selected
            ), LinearLayout.LayoutParams(0, dp(FinanceDesignTokens.TOUCH_TARGET_SIZE), 1f).apply {
                marginStart = dp(FinanceDesignTokens.SPACE_12)
            })
            if (selected) {
                row.addView(FinanceDesignSystem.text(
                    this, "✓", FinanceDesignTokens.TEXT_VALUE_SP, palette.accent, bold = true
                ))
            }
            list.addView(row, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(FinanceDesignTokens.SPACE_4) })
        }
        val listScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(list)
        }
        root.addView(listScroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(300)
        ))
        root.addView(FinanceDesignSystem.button(
            this, darkTheme, s(R.string.calendar), primary = false
        ) {
            dialog.dismiss()
            showMonthCalendarPicker()
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(FinanceDesignTokens.BUTTON_HEIGHT)).apply {
            topMargin = dp(FinanceDesignTokens.SPACE_8)
        })
        dialog.setContentView(root)
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.attributes = dialog.window?.attributes?.apply { dimAmount = 0.46f }
    }

    /** Persist the shared selected month using the Flutter app's setting key. */
    private fun selectMonth(month: YearMonth) {
        selectedMonth = month
        database.writeSetting("selected_month", monthStorageLabel(month))
        renderCurrentSection()
    }

    private fun showMonthCalendarPicker() {
        val initial = java.util.Calendar.getInstance().apply {
            set(selectedMonth.year, selectedMonth.monthValue - 1, 1, 12, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        showFlutterStyleDatePicker(initial) { picked ->
            selectMonth(YearMonth.of(
                picked.get(java.util.Calendar.YEAR),
                picked.get(java.util.Calendar.MONTH) + 1
            ))
        }
    }

    /**
     * Material-style date dialog matching Flutter showDatePicker:
     * selected-date header, month navigation, localized weekday row,
     * round day cells, year picker, and Cancel/Confirm actions.
     */
    private fun showFlutterStyleDatePicker(
        initialDate: java.util.Calendar,
        onDateSelected: (java.util.Calendar) -> Unit
    ) {
        val locale = if (localeCode == "ar") Locale("ar") else Locale.US
        val palette = FinanceDesignSystem.colors(darkTheme)
        val selected = java.util.Calendar.getInstance().apply {
            timeInMillis = initialDate.timeInMillis
            set(java.util.Calendar.HOUR_OF_DAY, 12)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val visibleMonth = java.util.Calendar.getInstance().apply {
            timeInMillis = selected.timeInMillis
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        val dialog = Dialog(ContextThemeWrapper(
            this,
            if (darkTheme) R.style.NativeFinanceDialogDarkTheme else R.style.NativeFinanceDialogLightTheme
        ))
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(FinanceDesignTokens.SPACE_16), dp(FinanceDesignTokens.SPACE_16),
                dp(FinanceDesignTokens.SPACE_16), dp(FinanceDesignTokens.SPACE_12))
            background = FinanceDesignSystem.rounded(this@MainActivity, palette.surface, 24)
        }
        val dateHeader = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(FinanceDesignTokens.SPACE_16), dp(FinanceDesignTokens.SPACE_12),
                dp(FinanceDesignTokens.SPACE_16), dp(FinanceDesignTokens.SPACE_12))
            background = GradientDrawable().apply {
                val radius = dp(20).toFloat()
                cornerRadii = floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)
                setColor(palette.accent)
            }
        }
        dateHeader.addView(FinanceDesignSystem.text(
            this, s(R.string.calendar), FinanceDesignTokens.TEXT_CAPTION_SP,
            palette.onAccent, bold = true
        ))
        val selectedDateText = FinanceDesignSystem.text(
            this, "", FinanceDesignTokens.TEXT_BALANCE_SP, palette.onAccent, bold = true
        ).apply { setPadding(0, dp(FinanceDesignTokens.SPACE_4), 0, 0) }
        dateHeader.addView(selectedDateText)
        root.addView(dateHeader, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        val monthRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(FinanceDesignTokens.SPACE_8), 0, dp(FinanceDesignTokens.SPACE_4))
        }
        fun calendarArrow(glyph: String, action: () -> Unit): TextView =
            FinanceDesignSystem.text(this, glyph, FinanceDesignTokens.TEXT_VALUE_SP, palette.text, bold = true).apply {
                gravity = Gravity.CENTER
                minWidth = dp(44)
                minHeight = dp(44)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(palette.raised)
                }
                isClickable = true
                isFocusable = true
                foreground = FinanceDesignSystem.ripple(this@MainActivity, darkTheme)
                setOnClickListener { action() }
            }
        var refreshCalendarAction: (() -> Unit)? = null
        val previousMonth = calendarArrow("‹") {
            visibleMonth.add(java.util.Calendar.MONTH, -1)
            refreshCalendarAction?.invoke()
        }
        val nextMonth = calendarArrow("›") {
            visibleMonth.add(java.util.Calendar.MONTH, 1)
            refreshCalendarAction?.invoke()
        }
        val monthTitle = FinanceDesignSystem.text(
            this, "", FinanceDesignTokens.TEXT_BODY_SP, palette.text, bold = true
        ).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            foreground = FinanceDesignSystem.ripple(this@MainActivity, darkTheme)
            setOnClickListener {
                val yearPicker = NumberPicker(this@MainActivity).apply {
                    minValue = 1900
                    maxValue = 2100
                    value = visibleMonth.get(java.util.Calendar.YEAR).coerceIn(1900, 2100)
                    wrapSelectorWheel = false
                }
                dialogBuilder()
                    .setTitle(s(R.string.calendar))
                    .setView(yearPicker)
                    .setPositiveButton(s(R.string.confirm)) { _, _ ->
                        visibleMonth.set(java.util.Calendar.YEAR, yearPicker.value)
                        refreshCalendarAction?.invoke()
                    }
                    .setNegativeButton(s(R.string.cancel), null)
                    .show()
            }
        }
        monthRow.addView(previousMonth, LinearLayout.LayoutParams(dp(44), dp(44)))
        monthRow.addView(monthTitle, LinearLayout.LayoutParams(0, dp(44), 1f))
        monthRow.addView(nextMonth, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(monthRow)
        val weekdayRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(weekdayRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(34)
        ))
        val calendarGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        root.addView(calendarGrid)
        refreshCalendarAction = {
            val monthKey = YearMonth.of(
                visibleMonth.get(java.util.Calendar.YEAR),
                visibleMonth.get(java.util.Calendar.MONTH) + 1
            )
            monthTitle.text = monthDisplayLabel(monthKey)
            val datePattern = if (localeCode == "ar") "EEEE، d MMMM yyyy" else "EEEE, d MMMM yyyy"
            selectedDateText.text = SimpleDateFormat(datePattern, locale).format(selected.time)
            weekdayRow.removeAllViews()
            calendarGrid.removeAllViews()
            val dayNames = java.text.DateFormatSymbols(locale).shortWeekdays
            val firstDay = java.util.Calendar.getInstance(locale).firstDayOfWeek
            for (offset in 0 until 7) {
                val dayOfWeek = ((firstDay - 1 + offset) % 7) + 1
                val dayLabel = FinanceDesignSystem.text(
                    this, dayNames[dayOfWeek].take(2), FinanceDesignTokens.TEXT_CAPTION_SP,
                    palette.secondaryText, bold = true
                ).apply {
                    gravity = Gravity.CENTER
                    textDirection = View.TEXT_DIRECTION_RTL
                    setTextLocale(locale)
                }
                weekdayRow.addView(dayLabel, LinearLayout.LayoutParams(0, dp(34), 1f))
            }
            val first = visibleMonth.clone() as java.util.Calendar
            first.set(java.util.Calendar.DAY_OF_MONTH, 1)
            val leading = (first.get(java.util.Calendar.DAY_OF_WEEK) - firstDay + 7) % 7
            val daysInMonth = visibleMonth.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            for (week in 0 until 6) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                }
                for (column in 0 until 7) {
                    val day = week * 7 + column - leading + 1
                    val cell = FinanceDesignSystem.text(
                        this, if (day in 1..daysInMonth) String.format(Locale.US, "%d", day) else "",
                        FinanceDesignTokens.TEXT_BODY_SP,
                        palette.text
                    ).apply {
                        gravity = Gravity.CENTER
                        textDirection = View.TEXT_DIRECTION_LTR
                        setTextLocale(Locale.US)
                        isClickable = day in 1..daysInMonth
                        isFocusable = isClickable
                    }
                    if (day in 1..daysInMonth) {
                        val isSelected = selected.get(java.util.Calendar.YEAR) == visibleMonth.get(java.util.Calendar.YEAR) &&
                            selected.get(java.util.Calendar.MONTH) == visibleMonth.get(java.util.Calendar.MONTH) &&
                            selected.get(java.util.Calendar.DAY_OF_MONTH) == day
                        if (isSelected) {
                            cell.background = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(palette.accent)
                            }
                            cell.setTextColor(palette.onAccent)
                            cell.typeface = Typeface.DEFAULT_BOLD
                        } else {
                            cell.background = null
                        }
                        cell.setOnClickListener {
                            selected.set(
                                visibleMonth.get(java.util.Calendar.YEAR),
                                visibleMonth.get(java.util.Calendar.MONTH),
                                day
                            )
                            refreshCalendarAction?.invoke()
                        }
                    }
                    row.addView(cell, LinearLayout.LayoutParams(0, dp(42), 1f))
                }
                calendarGrid.addView(row, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(42)
                ))
            }
        }
        refreshCalendarAction?.invoke()
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(FinanceDesignTokens.SPACE_8), 0, 0)
        }
        actions.addView(FinanceDesignSystem.button(
            this, darkTheme, s(R.string.cancel), primary = false
        ) { dialog.dismiss() }, LinearLayout.LayoutParams(0, dp(FinanceDesignTokens.BUTTON_HEIGHT), 1f))
        actions.addView(FinanceDesignSystem.button(
            this, darkTheme, s(R.string.confirm), primary = true
        ) {
            onDateSelected(selected.clone() as java.util.Calendar)
            dialog.dismiss()
        }, LinearLayout.LayoutParams(0, dp(FinanceDesignTokens.BUTTON_HEIGHT), 1f).apply {
            marginStart = dp(FinanceDesignTokens.SPACE_8)
        })
        root.addView(actions)
        dialog.setContentView(root)
        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        val width = (resources.displayMetrics.widthPixels - dp(32)).coerceAtMost(dp(420))
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showCalculator(onUseResult: ((String) -> Unit)? = null) {
        val palette = FinanceDesignSystem.colors(darkTheme)
        var mode = 0
        var financialMode = 0
        var resultForInsertion: String? = null
        var resultLabel = ""
        val history = mutableListOf<Pair<String, String>>()
        var activeError: String? = null
        var dialog: AlertDialog? = null

        val expression = FinanceDesignSystem.input(
            this, darkTheme, s(R.string.calculator_expression)
        ).apply {
            textDirection = View.TEXT_DIRECTION_LTR
            textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            isFocusable = false
            isFocusableInTouchMode = false
        }
        fun numericField(hint: String, decimal: Boolean = false) =
            FinanceDesignSystem.input(this, darkTheme, hint, numeric = true).apply {
                keyListener = DigitsKeyListener.getInstance(
                    if (decimal) "0123456789.," else "0123456789"
                )
                inputType = InputType.TYPE_CLASS_NUMBER or
                    if (decimal) InputType.TYPE_NUMBER_FLAG_DECIMAL else 0
                textDirection = View.TEXT_DIRECTION_LTR
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                setTextLocale(Locale.US)
            }
        val amount = numericField(s(R.string.calculator_amount))
        val percent = numericField(s(R.string.calculator_percent), decimal = true)
        val people = numericField(s(R.string.calculator_people))
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(FinanceDesignTokens.SPACE_4), dp(FinanceDesignTokens.SPACE_4),
                dp(FinanceDesignTokens.SPACE_4), dp(FinanceDesignTokens.SPACE_4))
        }
        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val errorView = FinanceDesignSystem.text(
            this, "", FinanceDesignTokens.TEXT_CAPTION_SP, palette.danger
        ).apply { visibility = View.GONE }
        val resultCard = FinanceComponents.card(this, darkTheme).apply {
            visibility = View.GONE
        }
        val resultView = FinanceDesignSystem.text(
            this, "", FinanceDesignTokens.TEXT_VALUE_SP, palette.text, bold = true
        ).apply {
            textDirection = View.TEXT_DIRECTION_LTR
            textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            isTextSelectable = true
        }
        val historyPanel = FinanceComponents.card(this, darkTheme).apply {
            visibility = View.GONE
        }
        resultCard.addView(resultView)
        val resultCopy = FinanceDesignSystem.button(
            this, darkTheme, s(R.string.calculator_copy), primary = false
        ) {
            if (resultLabel.isNotBlank()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("calculator", resultLabel))
                showToast(s(R.string.calculator_copied))
            }
        }
        resultCard.addView(resultCopy, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(FinanceDesignTokens.BUTTON_HEIGHT)
        ).apply { topMargin = dp(FinanceDesignTokens.SPACE_8) })

        var rebuildPanelAction: (() -> Unit)? = null
        var clearResultAction: (() -> Unit)? = null
        fun refreshHistory() {
            historyPanel.removeAllViews()
            if (history.isEmpty()) {
                historyPanel.visibility = View.GONE
                return
            }
            historyPanel.visibility = View.VISIBLE
            val heading = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                gravity = Gravity.CENTER_VERTICAL
            }
            heading.addView(FinanceDesignSystem.text(
                this, s(R.string.calculator_history), FinanceDesignTokens.TEXT_BODY_SP,
                palette.text, bold = true
            ), LinearLayout.LayoutParams(0, dp(FinanceDesignTokens.BUTTON_HEIGHT), 1f))
            heading.addView(FinanceDesignSystem.text(
                this, s(R.string.cancel), FinanceDesignTokens.TEXT_CAPTION_SP,
                palette.accent, bold = true
            ).apply {
                gravity = Gravity.CENTER
                isClickable = true
                setOnClickListener {
                    history.clear()
                    refreshHistory()
                }
            })
            historyPanel.addView(heading)
            history.takeLast(5).asReversed().forEach { entry ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val labels = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutDirection = View.LAYOUT_DIRECTION_LTR
                }
                labels.addView(FinanceDesignSystem.text(
                    this, entry.first, FinanceDesignTokens.TEXT_CAPTION_SP,
                    palette.secondaryText
                ).apply { maxLines = 1 })
                labels.addView(FinanceDesignSystem.text(
                    this, entry.second, FinanceDesignTokens.TEXT_BODY_SP,
                    palette.text, bold = true
                ).apply { maxLines = 1 })
                row.addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                val reuse = FinanceDesignSystem.iconAction(
                    this, darkTheme, R.drawable.ic_edit, s(R.string.calculator_expression)
                ) {
                    expression.setText(entry.second)
                    expression.setSelection(expression.text.length)
                    mode = 0
                    clearResultAction?.invoke()
                    rebuildPanelAction?.invoke()
                }
                row.addView(reuse, LinearLayout.LayoutParams(dp(44), dp(44)))
                val copy = FinanceDesignSystem.iconAction(
                    this, darkTheme, R.drawable.ic_calculator, s(R.string.calculator_copy)
                ) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("calculator", entry.second))
                    showToast(s(R.string.calculator_copied))
                }
                row.addView(copy, LinearLayout.LayoutParams(dp(44), dp(44)))
                historyPanel.addView(row)
                historyPanel.addView(FinanceComponents.bottomDivider(this, darkTheme))
            }
        }

        fun refreshResultButton() {
            dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = resultForInsertion != null
        }
        fun clearResult() {
            resultForInsertion = null
            resultLabel = ""
            activeError = null
            errorView.visibility = View.GONE
            resultCard.visibility = View.GONE
            refreshResultButton()
        }
        clearResultAction = { clearResult() }
        fun showError(message: String) {
            resultForInsertion = null
            resultLabel = ""
            activeError = message
            errorView.text = message
            errorView.visibility = View.VISIBLE
            resultCard.visibility = View.GONE
            refreshResultButton()
        }
        fun showResult(label: String, insertable: String?) {
            resultForInsertion = insertable
            resultLabel = label
            activeError = null
            errorView.visibility = View.GONE
            resultView.text = label
            resultCard.visibility = View.VISIBLE
            refreshResultButton()
        }
        fun addHistory(source: String, result: String) {
            history.add(source to result)
            if (history.size > 20) history.removeAt(0)
            refreshHistory()
        }
        fun appendToken(token: String) {
            val startsNew = resultLabel.isNotBlank() && Regex("^[0-9.]$").matches(token)
            val next = (if (startsNew) "" else expression.text.toString()) + token
            expression.setText(next)
            expression.setSelection(expression.text.length)
            clearResult()
        }
        fun calculateExpression() {
            val sourceText = expression.text.toString().trim()
            try {
                val value = FinanceCalculatorEngine.evaluate(sourceText)
                val formatted = FinanceCalculatorEngine.format(value)
                showResult(formatted, FinanceCalculatorEngine.asInsertableAmount(value))
                addHistory(sourceText, formatted)
            } catch (_: IllegalArgumentException) {
                showError(s(R.string.calculator_invalid_expression))
            } catch (_: ArithmeticException) {
                showError(s(R.string.calculator_invalid_expression))
            }
        }
        fun calculateFinancial() {
            val entered = amount.text.toString().trim().toBigIntegerOrNull()
            if (entered == null || entered.signum() < 0) {
                showError(s(R.string.calculator_invalid_financial))
                return
            }
            when (financialMode) {
                0 -> {
                    val value = FinanceCalculatorEngine.percentage(entered, percent.text.toString())
                    if (value == null) {
                        showError(s(R.string.calculator_invalid_financial))
                    } else {
                        val label = s(R.string.result) + ": " + value
                        showResult(label, value.toString())
                        addHistory(s(R.string.calculator_percentage) + ": " + amount.text + " · " + percent.text + "%", label)
                    }
                }
                1 -> {
                    val value = FinanceCalculatorEngine.discount(entered, percent.text.toString())
                    if (value == null) {
                        showError(s(R.string.calculator_invalid_financial))
                    } else {
                        val label = s(R.string.result) + ": " + value
                        showResult(label, value.toString())
                        addHistory(s(R.string.calculator_discount) + ": " + amount.text + " · " + percent.text + "%", label)
                    }
                }
                else -> {
                    val split = FinanceCalculatorEngine.split(
                        entered, people.text.toString().toBigIntegerOrNull()
                    )
                    if (split == null) {
                        showError(s(R.string.calculator_invalid_financial))
                    } else {
                        val label = s(R.string.calculator_per_person) + ": " + split.first +
                            " • " + s(R.string.calculator_remainder) + ": " + split.second
                        showResult(label, split.first.toString())
                        addHistory(s(R.string.calculator_split) + ": " + amount.text + " / " + people.text, label)
                    }
                }
            }
        }
        fun choiceChip(title: String, selected: Boolean, onClick: () -> Unit): TextView =
            FinanceDesignSystem.text(
                this, title, FinanceDesignTokens.TEXT_CAPTION_SP,
                if (selected) palette.onAccent else palette.text, bold = selected
            ).apply {
                gravity = Gravity.CENTER
                minHeight = dp(38)
                setPadding(dp(FinanceDesignTokens.SPACE_12), 0, dp(FinanceDesignTokens.SPACE_12), 0)
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(if (selected) palette.accent else palette.raised)
                    setStroke(dp(1), if (selected) palette.accent else palette.divider)
                }
                isClickable = true
                isFocusable = true
                foreground = FinanceDesignSystem.ripple(this@MainActivity, darkTheme)
                setOnClickListener { onClick() }
            }
        fun keyButton(key: String): TextView =
            FinanceDesignSystem.text(
                this, key,
                if (key == "=") FinanceDesignTokens.TEXT_VALUE_SP else FinanceDesignTokens.TEXT_BODY_SP,
                if (key == "=") palette.onAccent else palette.text,
                bold = key == "="
            ).apply {
                gravity = Gravity.CENTER
                minHeight = dp(44)
                background = GradientDrawable().apply {
                    cornerRadius = dp(FinanceDesignTokens.CONTROL_RADIUS).toFloat()
                    setColor(if (key == "=") palette.accent else palette.raised)
                    setStroke(dp(1), if (key == "=") palette.accent else palette.divider)
                }
                isClickable = true
                isFocusable = true
                foreground = FinanceDesignSystem.ripple(this@MainActivity, darkTheme)
                setOnClickListener {
                    when (key) {
                        "AC" -> {
                            expression.setText("")
                            clearResult()
                        }
                        "⌫" -> {
                            expression.setText(expression.text.toString().dropLast(1))
                            expression.setSelection(expression.text.length)
                            clearResult()
                        }
                        "√" -> appendToken("√(")
                        "x²" -> appendToken("^2")
                        "±" -> {
                            val current = expression.text.toString().trim()
                            expression.setText(if (current.isNotEmpty()) "-(" + current + ")" else "−")
                            expression.setSelection(expression.text.length)
                            clearResult()
                        }
                        "=" -> calculateExpression()
                        else -> appendToken(key)
                    }
                }
            }
        fun labelledField(title: String, field: EditText): LinearLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                addView(FinanceDesignSystem.text(
                    this@MainActivity, title, FinanceDesignTokens.TEXT_CAPTION_SP,
                    palette.secondaryText, bold = true
                ).apply {
                    setPadding(dp(FinanceDesignTokens.SPACE_2), 0, dp(FinanceDesignTokens.SPACE_2), dp(FinanceDesignTokens.SPACE_4))
                })
                addView(field, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(FinanceDesignTokens.BUTTON_HEIGHT)
                ))
            }
        rebuildPanelAction = {
            panel.removeAllViews()
            modeRow.removeAllViews()
            modeRow.addView(choiceChip(s(R.string.calculator_scientific), mode == 0) {
                mode = 0
                clearResult()
                rebuildPanelAction?.invoke()
            }, LinearLayout.LayoutParams(0, dp(38), 1f))
            modeRow.addView(choiceChip(s(R.string.calculator_financial), mode == 1) {
                mode = 1
                clearResult()
                rebuildPanelAction?.invoke()
            }, LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                marginStart = dp(FinanceDesignTokens.SPACE_8)
            })
            if (mode == 0) {
                panel.addView(expression, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(FinanceDesignTokens.BUTTON_HEIGHT)
                ))
                val rows = listOf(
                    listOf("AC", "(", ")", "%", "⌫"),
                    listOf("7", "8", "9", "÷", "√"),
                    listOf("4", "5", "6", "×", "x²"),
                    listOf("1", "2", "3", "−", "^"),
                    listOf("0", ".", "±", "+", "=")
                )
                rows.forEach { keys ->
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutDirection = View.LAYOUT_DIRECTION_LTR
                    }
                    keys.forEach { key ->
                        row.addView(keyButton(key), LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                            if (key != keys.first()) marginStart = dp(FinanceDesignTokens.SPACE_4)
                        })
                    }
                    panel.addView(row, LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(44)
                    ).apply { topMargin = dp(FinanceDesignTokens.SPACE_6) })
                }
            } else {
                val financialModes = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                }
                listOf(
                    s(R.string.calculator_percentage),
                    s(R.string.calculator_discount),
                    s(R.string.calculator_split)
                ).forEachIndexed { index, title ->
                    financialModes.addView(choiceChip(title, financialMode == index) {
                        financialMode = index
                        clearResult()
                        rebuildPanelAction?.invoke()
                    }, LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                        if (index > 0) marginStart = dp(FinanceDesignTokens.SPACE_4)
                    })
                }
                panel.addView(financialModes)
                panel.addView(labelledField(s(R.string.calculator_amount), amount),
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = dp(FinanceDesignTokens.SPACE_8)
                    })
                if (financialMode == 2) {
                    panel.addView(labelledField(s(R.string.calculator_people), people),
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            topMargin = dp(FinanceDesignTokens.SPACE_8)
                        })
                } else {
                    panel.addView(labelledField(s(R.string.calculator_percent), percent),
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            topMargin = dp(FinanceDesignTokens.SPACE_8)
                        })
                }
                panel.addView(FinanceDesignSystem.button(
                    this, darkTheme, s(R.string.calculator_calculate), primary = true
                ) { calculateFinancial() }, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(FinanceDesignTokens.BUTTON_HEIGHT)
                ).apply { topMargin = dp(FinanceDesignTokens.SPACE_12) })
            }
            content.removeAllViews()
            content.addView(modeRow)
            content.addView(panel, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(FinanceDesignTokens.SPACE_12) })
            content.addView(errorView, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(FinanceDesignTokens.SPACE_8) })
            content.addView(resultCard, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(FinanceDesignTokens.SPACE_8) })
            content.addView(historyPanel, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(FinanceDesignTokens.SPACE_8) })
        }
        rebuildPanelAction?.invoke()
        refreshHistory()
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(content)
        }
        val maxDialogHeight = (resources.displayMetrics.heightPixels * 0.68f).toInt()
        scroll.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxDialogHeight)
        dialog = dialogBuilder()
            .setTitle(s(R.string.calculator))
            .setView(scroll)
            .setNegativeButton(s(R.string.cancel), null)
            .setPositiveButton(s(R.string.use_result)) { _, _ ->
                val value = resultForInsertion
                if (value != null) {
                    if (onUseResult != null) onUseResult.invoke(value) else showToast(resultLabel)
                }
            }
            .create()
        dialog?.setOnShowListener {
            dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        }
        dialog?.show()
        refreshResultButton()
    }
    private fun showSettings() {
        val options = arrayOf(
            s(R.string.appearance),
            s(R.string.language),
            s(R.string.login_security)
        )
        dialogBuilder()
            .setTitle(s(R.string.settings))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAppearanceModePicker()
                    1 -> showLanguagePicker()
                    2 -> showSecuritySettings()
                }
            }
            .setNegativeButton(s(R.string.cancel), null)
            .show()
    }

    private fun showAppearanceModePicker() {
        val modes = arrayOf(
            FinanceAppearanceMode.SYSTEM,
            FinanceAppearanceMode.LIGHT,
            FinanceAppearanceMode.DARK
        )
        val labels = arrayOf(
            s(R.string.system),
            s(R.string.light),
            s(R.string.dark)
        )
        val selected = modes.indexOf(FinanceAppearanceMode.fromStorage(themeMode))
            .coerceAtLeast(0)

        dialogBuilder()
            .setTitle(s(R.string.appearance))
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                themeMode = modes[which].storageValue
                database.writeSetting(FinanceAppearance.SETTINGS_KEY, themeMode)
                dialog.dismiss()
                darkTheme = resolveDarkTheme()
                applySystemBars()
                buildApplicationShell()
                renderCurrentSection()
            }
            .setNegativeButton(s(R.string.cancel), null)
            .show()
    }

    private fun showLanguagePicker() {
        val languages = arrayOf(
            s(R.string.arabic),
            s(R.string.english)
        )
        val selected = if (localeCode == "en") 1 else 0
        dialogBuilder()
            .setTitle(s(R.string.language))
            .setSingleChoiceItems(languages, selected) { dialog, which ->
                localeCode = if (which == 1) "en" else "ar"
                database.writeSetting("locale", localeCode)
                dialog.dismiss()
                buildApplicationShell()
                renderCurrentSection()
            }
            .setNegativeButton(s(R.string.cancel), null)
            .show()
    }

    private fun showSecuritySettings() {
        val enabled = localAuthentication.isEnabled()
        val options = arrayOf(
            s(R.string.change_login_credentials),
            if (enabled) s(R.string.disable_login) else s(R.string.enable_login)
        )
        dialogBuilder()
            .setTitle(s(R.string.login_security))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showChangeCredentialsDialog()
                    1 -> confirmLoginToggle(!enabled)
                }
            }
            .setNegativeButton(s(R.string.cancel), null)
            .show()
    }

    private fun confirmLoginToggle(enable: Boolean) {
        val message = if (enable) s(R.string.enable_login_confirm)
            else s(R.string.disable_login_confirm)
        dialogBuilder()
            .setTitle(s(R.string.login_security))
            .setMessage(message)
            .setPositiveButton(s(R.string.confirm)) { _, _ ->
                localAuthentication.setEnabled(enable)
                showToast(
                    s(if (enable) R.string.login_enabled else R.string.login_disabled)
                )
            }
            .setNegativeButton(s(R.string.cancel), null)
            .show()
    }

    private fun showChangeCredentialsDialog() {
        val form = createForm()
        val currentPassword = addPasswordField(form, s(R.string.current_password))
        val newUsername = formInput(
            form,
            s(R.string.new_username),
            localAuthentication.currentUsername()
        )
        val newPassword = addPasswordField(form, s(R.string.new_password))
        val confirmPassword = addPasswordField(form, s(R.string.confirm_new_password))

        dialogBuilder()
            .setTitle(s(R.string.change_login_credentials))
            .setView(form)
            .setPositiveButton(s(R.string.save)) { _, _ ->
                val passwordValue = newPassword.text.toString()
                if (passwordValue != confirmPassword.text.toString()) {
                    showToast(s(R.string.passwords_do_not_match))
                    return@setPositiveButton
                }
                when (localAuthentication.changeCredentials(
                    currentPassword = currentPassword.text.toString(),
                    newUsername = newUsername.text.toString(),
                    newPassword = passwordValue
                )) {
                    LocalAuthentication.CredentialChangeResult.SUCCESS ->
                        showToast(s(R.string.credentials_updated))
                    LocalAuthentication.CredentialChangeResult.CURRENT_PASSWORD_INVALID ->
                        showToast(s(R.string.current_password_invalid))
                    LocalAuthentication.CredentialChangeResult.USERNAME_TOO_SHORT ->
                        showToast(s(R.string.username_too_short))
                    LocalAuthentication.CredentialChangeResult.PASSWORD_TOO_SHORT ->
                        showToast(s(R.string.password_too_short))
                }
            }
            .setNegativeButton(s(R.string.cancel), null)
            .show()
    }

    private fun addPasswordField(form: LinearLayout, hint: String): EditText =
        formInput(form, hint).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

    private fun sectionTitle(section: Section): String = when (section) {
        Section.DASHBOARD -> s(com.byepro.bye_pro_finance.R.string.dashboard)
        Section.WORK -> s(com.byepro.bye_pro_finance.R.string.work)
        Section.HOME -> s(com.byepro.bye_pro_finance.R.string.home)
        Section.DEBTS -> s(com.byepro.bye_pro_finance.R.string.debts)
        Section.AI -> s(com.byepro.bye_pro_finance.R.string.ai)
    }

    private fun recordTitle(record: FinanceRecord): String = when (record.kind) {
        "salary" -> s(R.string.salary)
        "advance" -> s(R.string.advance)
        "payment", "salaryPayment" -> s(R.string.payment)
        "transfer" -> s(R.string.transfer_to_home)
        "expense" -> categoryTitle(record.title)
        "creditor" -> record.title
        "debtor" -> record.title
        "bill" -> record.title
        else -> record.title
    }

    private fun categoryTitle(category: String): String {
        val ar = localeCode == "ar"
        return when (category) {
            "groceries" -> if (ar) "مواد غذائية" else "Groceries"
            "utilities" -> if (ar) "فواتير وخدمات" else "Utilities"
            "housing" -> if (ar) "السكن" else "Housing"
            "cleaning" -> if (ar) "تنظيف" else "Cleaning"
            "maintenance" -> if (ar) "صيانة" else "Maintenance"
            "transport" -> if (ar) "مواصلات" else "Transport"
            "personal" -> if (ar) "شخصي" else "Personal"
            "health" -> if (ar) "صحة" else "Health"
            "other" -> if (ar) "أخرى" else "Other"
            else -> category
        }
    }

    private fun activityTitle(title: String): String = when (title) {
        "salary" -> s(com.byepro.bye_pro_finance.R.string.salary)
        "advance" -> s(com.byepro.bye_pro_finance.R.string.advance)
        "payment" -> s(com.byepro.bye_pro_finance.R.string.payment)
        "transfer_to_home" -> s(com.byepro.bye_pro_finance.R.string.transfer_to_home)
        "expense" -> s(com.byepro.bye_pro_finance.R.string.expense)
        "bill_payment" -> s(com.byepro.bye_pro_finance.R.string.bill_payment)
        "debt_created" -> s(com.byepro.bye_pro_finance.R.string.debt_created)
        "debt_payment" -> s(com.byepro.bye_pro_finance.R.string.debt_payment)
        else -> title
    }

    private fun activityIcon(icon: String): Int = when (icon) {
        "work" -> R.drawable.ic_work
        "home" -> R.drawable.ic_expense
        "transfer" -> R.drawable.ic_transfer
        "bill" -> R.drawable.ic_bill
        else -> R.drawable.ic_debts
    }

    private fun workCategoryIcon(category: WorkCategory): Int = when (category) {
        WorkCategory.SALARY -> R.drawable.ic_work_salary
        WorkCategory.PAYMENT -> R.drawable.ic_work_payment
        WorkCategory.ADVANCE -> R.drawable.ic_work_advance
        WorkCategory.REMAINING -> R.drawable.ic_work_remaining
        WorkCategory.HOME_TRANSFERS -> R.drawable.ic_work_transfers
    }

    private fun homeCategoryIcon(category: HomeCategory): Int = when (category) {
        HomeCategory.MONTHLY_BILLS -> R.drawable.ic_home_bills
        HomeCategory.RENT -> R.drawable.ic_home_rent
        HomeCategory.EXPENSES -> R.drawable.ic_home_expenses
        HomeCategory.RECEIVED_FROM_WORK -> R.drawable.ic_home_received
    }

    private fun expenseCategoryIcon(category: String): Int = when (category) {
        "groceries" -> R.drawable.ic_expense_groceries
        "utilities" -> R.drawable.ic_expense_utilities
        "housing" -> R.drawable.ic_expense_housing
        "cleaning" -> R.drawable.ic_expense_cleaning
        "maintenance" -> R.drawable.ic_expense_maintenance
        "transport" -> R.drawable.ic_expense_transport
        "personal" -> R.drawable.ic_expense_personal
        "health" -> R.drawable.ic_expense_health
        else -> R.drawable.ic_expense_other
    }

    private fun recordIcon(record: FinanceRecord): Int = when (record.kind) {
        "salary" -> R.drawable.ic_work_salary
        "advance" -> R.drawable.ic_work_advance
        "payment", "salaryPayment" -> R.drawable.ic_work_payment
        "transfer" -> R.drawable.ic_work_transfers
        "expense" -> expenseCategoryIcon(record.categoryId ?: "other")
        "bill", "billPayment" -> if (record.detail.startsWith("rent")) R.drawable.ic_home_rent else R.drawable.ic_home_bills
        "creditor", "debtor", "debtPayment" -> R.drawable.ic_debts
        else -> R.drawable.ic_work
    }

    private fun recordDetail(record: FinanceRecord): String {
        val parts = mutableListOf<String>()
        when (record.kind) {
            "salary", "advance", "payment", "salaryPayment", "bill" -> {
                if (record.dueYear != null && record.dueMonth != null) {
                    parts.add("%04d-%02d".format(record.dueYear, record.dueMonth))
                }
            }
            else -> if (record.timestamp > 0L) parts.add(formatDate(record.timestamp))
        }
        record.note?.takeIf { it.isNotBlank() }?.let(parts::add)
        if (record.note.isNullOrBlank() && record.detail.isNotBlank()) {
            if (record.kind == "bill") {
                val type = if (record.detail.startsWith("rent")) {
                    s(R.string.rent_bill)
                } else {
                    s(R.string.monthly_bill)
                }
                parts.add(type)
            } else if (record.kind == "creditor") {
                parts.add(s(R.string.creditor_balance))
            } else if (record.kind == "debtor") {
                parts.add(s(R.string.debtor_balance))
            }
        }
        return parts.joinToString(" • ")
    }

    private fun makeText(value: String, sizeSp: Float, bold: Boolean): TextView =
        FinanceDesignSystem.text(
            context = this,
            value = value,
            sizeSp = sizeSp,
            color = FinancePalette.text(darkTheme),
            bold = bold
        )

    private fun addGap(heightDp: Int) {
        pageContent.addView(View(this), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp)
        ))
    }

    private fun addGapTo(parent: LinearLayout, heightDp: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp)
        ))
    }

    private fun formatAmount(amount: Long): String =
        DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US)).format(amount)

    private fun formatDate(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMillis))

    private fun dp(value: Int): Int = FinanceDesignSystem.dp(this, value)
}
