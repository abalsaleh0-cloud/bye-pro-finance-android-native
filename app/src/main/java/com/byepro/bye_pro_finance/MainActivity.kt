package com.byepro.bye_pro_finance

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.widget.EditText
import android.widget.LinearLayout
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
import com.byepro.bye_pro_finance.domain.HomeCategory
import com.byepro.bye_pro_finance.domain.HomeRecordSource
import com.byepro.bye_pro_finance.ui.FinanceAppearance
import com.byepro.bye_pro_finance.ui.FinanceAppearanceMode
import com.byepro.bye_pro_finance.ui.FinanceCategoryTab
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
    private var selectedHomeCategory = HomeCategory.MONTHLY_BILLS
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
            ?: YearMonth.now()
        selectedHomeCategory = savedInstanceState?.getString("home_category")
            ?.let { runCatching { HomeCategory.valueOf(it) }.getOrNull() }
            ?: HomeCategory.MONTHLY_BILLS

        darkTheme = resolveDarkTheme()
        applySystemBars()
        showWelcomeScreen()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("active_section", activeSection.name)
        outState.putString("selected_month", selectedMonth.toString())
        outState.putString("home_category", selectedHomeCategory.name)
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
            gravity = Gravity.CENTER
            setPadding(
                dp(FinanceDesignTokens.PAGE_HORIZONTAL_PADDING),
                dp(FinanceDesignTokens.PAGE_VERTICAL_PADDING),
                dp(FinanceDesignTokens.PAGE_HORIZONTAL_PADDING),
                dp(FinanceDesignTokens.PAGE_VERTICAL_PADDING)
            )
            setBackgroundColor(palette.background)
        }

        val panel = FinanceComponents.card(this, darkTheme)
        val mark = FinanceDesignSystem.iconBox(
            this,
            darkTheme,
            R.drawable.ic_bye_pro_finance,
            selected = true
        )
        panel.addView(mark, LinearLayout.LayoutParams(
            dp(FinanceDesignTokens.WELCOME_ICON_SIZE),
            dp(FinanceDesignTokens.WELCOME_ICON_SIZE)
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(FinanceDesignTokens.SECTION_SPACING)
        })
        panel.addView(FinanceDesignSystem.text(
            this,
            s(R.string.welcome_title),
            FinanceDesignTokens.TEXT_PAGE_TITLE_SP,
            palette.text,
            bold = true
        ).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        })
        panel.addView(FinanceDesignSystem.text(
            this,
            s(R.string.welcome_description),
            FinanceDesignTokens.TEXT_BODY_SP,
            palette.secondaryText
        ).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(
                0,
                dp(FinanceDesignTokens.SECTION_SPACING),
                0,
                dp(FinanceDesignTokens.SECTION_SPACING)
            )
        })
        panel.addView(FinanceDesignSystem.button(
            this,
            darkTheme,
            s(R.string.continue_to_login),
            primary = true
        ) {
            continueFromWelcome()
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(FinanceDesignTokens.BUTTON_HEIGHT)
        ))
        panel.addView(FinanceDesignSystem.text(
            this,
            s(R.string.swipe_to_continue),
            FinanceDesignTokens.TEXT_CAPTION_SP,
            palette.secondaryText
        ).apply {
            gravity = Gravity.CENTER
            setPadding(
                0,
                dp(FinanceDesignTokens.SPACE_12),
                0,
                0
            )
        })

        screen.addView(panel, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        var startX = 0f
        var startY = 0f
        screen.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    false
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = event.x - startX
                    val deltaY = event.y - startY
                    if (kotlin.math.abs(deltaX) >= dp(FinanceDesignTokens.SWIPE_THRESHOLD) &&
                        kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY)
                    ) {
                        continueFromWelcome()
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
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
        val palette = FinanceDesignSystem.colors(darkTheme)
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER
            setPadding(
                dp(FinanceDesignTokens.PAGE_HORIZONTAL_PADDING),
                dp(FinanceDesignTokens.PAGE_VERTICAL_PADDING),
                dp(FinanceDesignTokens.PAGE_HORIZONTAL_PADDING),
                dp(FinanceDesignTokens.PAGE_VERTICAL_PADDING)
            )
            setBackgroundColor(palette.background)
        }
        val panel = FinanceComponents.card(this, darkTheme)
        panel.addView(FinanceDesignSystem.text(
            this,
            s(R.string.login_title),
            FinanceDesignTokens.TEXT_PAGE_TITLE_SP,
            palette.text,
            bold = true
        ).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        })
        panel.addView(FinanceDesignSystem.text(
            this,
            s(R.string.login_description),
            FinanceDesignTokens.TEXT_BODY_SP,
            palette.secondaryText
        ).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, dp(FinanceDesignTokens.SPACE_8), 0,
                dp(FinanceDesignTokens.SECTION_SPACING))
        })

        val username = FinanceDesignSystem.input(
            this,
            darkTheme,
            s(R.string.username),
            initialValue = localAuthentication.currentUsername()
        )
        panel.addView(username, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(FinanceDesignTokens.BUTTON_HEIGHT)
        ).apply {
            bottomMargin = dp(FinanceDesignTokens.SECTION_SPACING / 2)
        })

        val password = FinanceDesignSystem.input(
            this,
            darkTheme,
            s(R.string.password)
        ).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        panel.addView(password, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(FinanceDesignTokens.BUTTON_HEIGHT)
        ).apply {
            bottomMargin = dp(FinanceDesignTokens.SECTION_SPACING / 2)
        })

        val errorMessage = FinanceDesignSystem.text(
            this,
            "",
            FinanceDesignTokens.TEXT_CAPTION_SP,
            palette.danger
        ).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            minHeight = dp(FinanceDesignTokens.SECTION_HEADER_HEIGHT)
        }
        panel.addView(errorMessage)
        panel.addView(FinanceDesignSystem.button(
            this,
            darkTheme,
            s(R.string.login),
            primary = true
        ) {
            if (localAuthentication.authenticate(
                    username.text.toString(),
                    password.text.toString()
                )
            ) {
                enterApplication()
            } else {
                password.text?.clear()
                errorMessage.text = s(R.string.login_invalid)
            }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(FinanceDesignTokens.BUTTON_HEIGHT)
        ))
        screen.addView(panel, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        setContentView(screen)
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
                selectedMonth = selectedMonth.toString(),
                contentDescription = s(R.string.month_shown) + ": " + selectedMonth
            ) { anchor ->
                showMonthPicker(anchor, summary.availableMonths)
            },
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
        addSectionHeading(s(com.byepro.bye_pro_finance.R.string.work_records))
        addMonthQuickPicker()
        val records = (
            repository.loadWorkRecords(selectedMonth) +
                repository.loadTransfers(selectedMonth)
            ).sortedByDescending { it.timestamp }
        if (records.isEmpty()) {
            addEmptyState(s(com.byepro.bye_pro_finance.R.string.no_records))
            return
        }
        addRecordGroup(records)
    }

    private fun renderHome() {
        addSectionHeading(s(com.byepro.bye_pro_finance.R.string.home_records))
        addMonthQuickPicker()

        val tabs = HomeCategory.ordered.map { category ->
            val titleResource = when (category) {
                HomeCategory.MONTHLY_BILLS -> R.string.home_monthly_bills
                HomeCategory.RENT -> R.string.home_rent
                HomeCategory.EXPENSES -> R.string.home_expenses
            }
            FinanceCategoryTab(
                title = s(titleResource),
                selected = selectedHomeCategory == category
            ) {
                selectedHomeCategory = category
                renderCurrentSection()
            }
        }
        pageContent.addView(
            FinanceComponents.categoryTabs(this, darkTheme, tabs),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(FinanceDesignTokens.SECTION_TAB_HEIGHT)
            ).apply {
                bottomMargin = dp(FinanceDesignTokens.SPACE_4)
            }
        )

        val categoryDescription = when (selectedHomeCategory) {
            HomeCategory.MONTHLY_BILLS -> s(R.string.home_monthly_bills_description)
            HomeCategory.RENT -> s(R.string.home_rent_description)
            HomeCategory.EXPENSES -> s(R.string.home_expenses_description)
        }
        pageContent.addView(
            FinanceComponents.categoryDescription(this, darkTheme, categoryDescription),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        addGap(FinanceDesignTokens.SECTION_SPACING)

        val records = when (selectedHomeCategory.source) {
            HomeRecordSource.BILLS -> repository.loadBills(
                selectedMonth,
                requireNotNull(selectedHomeCategory.billKind)
            )
            HomeRecordSource.HOME_EXPENSES -> repository.loadHomeExpenses(selectedMonth)
        }.sortedByDescending {
            if (it.kind == "bill") Long.MAX_VALUE else it.timestamp
        }

        if (records.isEmpty()) {
            addEmptyState(s(R.string.no_records))
            return
        }
        addRecordGroup(records)
    }

    private fun renderDebts() {
        addSectionHeading(s(com.byepro.bye_pro_finance.R.string.debt_records))
        val records = repository.loadDebts()
        if (records.isEmpty()) {
            addEmptyState(s(com.byepro.bye_pro_finance.R.string.no_records))
            return
        }
        addRecordGroup(records)
    }

    private fun renderAssistant() {
        addSectionHeading(s(R.string.ai))
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
                    iconResource = recordIcon(record.kind),
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
        val menu = PopupMenu(popupContext(), anchor)
        when (activeSection) {
            Section.WORK -> {
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.new_salary)) {
                    showWorkEntryForm("salary")
                }
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.new_advance)) {
                    showWorkEntryForm("advance")
                }
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.new_work_payment)) {
                    showWorkEntryForm("payment")
                }
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.new_transfer)) {
                    showTransferForm()
                }
            }
            Section.HOME -> {
                when (selectedHomeCategory) {
                    HomeCategory.MONTHLY_BILLS -> {
                        addMenuAction(menu, s(R.string.new_bill)) {
                            showBillForm(billKind = "monthly")
                        }
                    }
                    HomeCategory.RENT -> {
                        addMenuAction(menu, s(R.string.new_rent)) {
                            showBillForm(billKind = "rent")
                        }
                    }
                    HomeCategory.EXPENSES -> {
                        addMenuAction(menu, s(R.string.new_expense)) {
                            showHomeExpenseForm()
                        }
                    }
                }
            }
            Section.DEBTS -> {
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.new_creditor)) {
                    showDebtForm("creditor")
                }
                addMenuAction(menu, s(com.byepro.bye_pro_finance.R.string.new_debtor)) {
                    showDebtForm("debtor")
                }
            }
            else -> Unit
        }
        menu.show()
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
        form: LinearLayout,
        hint: String,
        initial: String = "",
        numeric: Boolean = false
    ): EditText {
        val multiline = hint == s(R.string.note_optional)
        val field = FinanceDesignSystem.input(
            context = this,
            isDark = darkTheme,
            hint = hint,
            initialValue = initial,
            numeric = numeric,
            multiline = multiline
        )
        form.addView(field, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            if (multiline) dp(FinanceDesignTokens.MULTILINE_INPUT_HEIGHT) else dp(FinanceDesignTokens.BUTTON_HEIGHT)
        ).apply {
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
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = selectedMillis
            }
            DatePickerDialog(
                ContextThemeWrapper(
                    this,
                    if (darkTheme) R.style.NativeFinanceDialogDarkTheme
                    else R.style.NativeFinanceDialogLightTheme
                ),
                { _, year, month, day ->
                    val date = java.util.Calendar.getInstance().apply {
                        set(year, month, day, calendar.get(java.util.Calendar.HOUR_OF_DAY),
                            calendar.get(java.util.Calendar.MINUTE), 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    selectedMillis = date.timeInMillis
                    refresh()
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
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
            context = this,
            isDark = darkTheme,
            label = categoryTitle(selectedCategoryId)
        ) { field ->
            val labels = categories.map(::categoryTitle).toTypedArray()
            val selectedIndex = categories.indexOf(selectedCategoryId).coerceAtLeast(0)
            dialogBuilder()
                .setTitle(s(R.string.select_expense_category))
                .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                    selectedCategoryId = categories[which]
                    field.text = categoryTitle(selectedCategoryId) + "  ▾"
                    dialog.dismiss()
                }
                .setNegativeButton(s(R.string.cancel), null)
                .show()
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

    private fun showMonthPicker(anchor: View, months: List<YearMonth>? = null) {
        val available = months ?: repository.loadDashboard(selectedMonth).availableMonths
        val popup = PopupMenu(popupContext(), anchor)
        available.forEach { month ->
            popup.menu.add(month.toString()).setOnMenuItemClickListener {
                selectedMonth = month
                renderCurrentSection()
                true
            }
        }
        popup.show()
    }

    private fun showCalculator() {
        val form = createForm()
        val first = formInput(form, s(R.string.enter_amount), numeric = true)
        val second = formInput(form, s(R.string.enter_amount), numeric = true)
        dialogBuilder()
            .setTitle(s(com.byepro.bye_pro_finance.R.string.calculator))
            .setView(form)
            .setPositiveButton(s(com.byepro.bye_pro_finance.R.string.result)) { _, _ ->
                val a = first.text.toString().toLongOrNull() ?: 0L
                val b = second.text.toString().toLongOrNull() ?: 0L
                dialogBuilder()
                    .setMessage(formatAmount(a + b))
                    .setPositiveButton(s(com.byepro.bye_pro_finance.R.string.cancel), null)
                    .show()
            }
            .setNegativeButton(s(com.byepro.bye_pro_finance.R.string.cancel), null)
            .show()
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

    private fun recordIcon(kind: String): Int = when (kind) {
        "salary" -> R.drawable.ic_payments
        "advance" -> R.drawable.ic_advance
        "payment", "salaryPayment" -> R.drawable.ic_payments
        "transfer" -> R.drawable.ic_transfer
        "expense" -> R.drawable.ic_expense
        "bill", "billPayment" -> R.drawable.ic_bill
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
