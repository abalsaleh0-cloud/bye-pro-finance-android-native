import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val rasterFinanceIconNames = listOf(
    "ic_add", "ic_advance", "ic_ai", "ic_arrow_back", "ic_bill", "ic_bye_pro_finance",
    "ic_calculator", "ic_calendar", "ic_close", "ic_dashboard", "ic_debts", "ic_delete",
    "ic_edit", "ic_expense", "ic_expense_cleaning", "ic_expense_groceries",
    "ic_expense_health", "ic_expense_housing", "ic_expense_maintenance", "ic_expense_other",
    "ic_expense_personal", "ic_expense_transport", "ic_expense_utilities", "ic_home",
    "ic_home_bills", "ic_home_expenses", "ic_home_received", "ic_home_rent", "ic_more",
    "ic_payments", "ic_search", "ic_settings", "ic_transfer", "ic_work", "ic_work_advance",
    "ic_work_payment", "ic_work_remaining", "ic_work_salary", "ic_work_transfers"
)
val financeIconSheet = layout.projectDirectory.file("src/main/assets/byepro-icons-sheet.png")
val financeRasterRes = layout.buildDirectory.dir("generated/res/financeRasterIcons")
val generateFinanceRasterIcons = tasks.register("generateFinanceRasterIcons") {
    inputs.file(financeIconSheet)
    outputs.dir(financeRasterRes)
    doLast {
        val source = javax.imageio.ImageIO.read(financeIconSheet.asFile)
            ?: throw GradleException("Unable to read the BYE PRO raster icon sheet.")
        val cellSize = 32
        val columns = 8
        require(source.width == columns * cellSize) { "Unexpected BYE PRO icon sheet width: " + source.width }
        require(source.height >= ((rasterFinanceIconNames.size + columns - 1) / columns) * cellSize) {
            "The BYE PRO icon sheet is missing icon rows."
        }
        val drawableDirectory = financeRasterRes.get().dir("drawable-nodpi").asFile
        drawableDirectory.mkdirs()
        rasterFinanceIconNames.forEachIndexed { index, name ->
            val tile = java.awt.image.BufferedImage(cellSize, cellSize, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            val graphics = tile.createGraphics()
            val sourceX = (index % columns) * cellSize
            val sourceY = (index / columns) * cellSize
            graphics.drawImage(source, 0, 0, cellSize, cellSize, sourceX, sourceY, sourceX + cellSize, sourceY + cellSize, null)
            graphics.dispose()
            val output = java.io.File(drawableDirectory, name + ".png")
            check(javax.imageio.ImageIO.write(tile, "png", output)) { "PNG writer is not available for " + name }
        }
    }
}

// Keep release signing compatible with the Flutter app. The same keystore,
// alias and credentials are used so an eventual Native release can update the
// installed application without changing package identity or signing lineage.
val releaseSigningProperties = Properties()
val nativeAndroidApp = rootProject.projectDir.resolve("app")
val releaseSigningPropertiesFile = rootProject.projectDir.resolve("key.properties")
if (releaseSigningPropertiesFile.isFile) {
    releaseSigningPropertiesFile.inputStream().use(releaseSigningProperties::load)
}

fun releaseSigningValue(environmentName: String, propertiesName: String): String? =
    System.getenv(environmentName)?.takeIf { it.isNotBlank() }
        ?: releaseSigningProperties.getProperty(propertiesName)?.takeIf { it.isNotBlank() }

val releaseStorePath = releaseSigningValue("KEYSTORE_PATH", "storeFile")
val releaseStorePassword = releaseSigningValue("KEYSTORE_PASSWORD", "storePassword")
val releaseKeyPassword = releaseSigningValue("KEY_PASSWORD", "keyPassword")
val releaseKeyAlias = releaseSigningValue("KEY_ALIAS", "keyAlias")
val releaseSigningConfigured = listOf(
    releaseStorePath,
    releaseStorePassword,
    releaseKeyPassword,
    releaseKeyAlias
).all { !it.isNullOrBlank() }

val resolvedReleaseStoreFile = releaseStorePath?.let { configuredPath ->
    val file = File(configuredPath)
    if (file.isAbsolute) file else File(nativeAndroidApp, configuredPath)
}

val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    doLast {
        if (!releaseSigningConfigured) {
            throw GradleException(
                "Release signing is not configured. Set KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_PASSWORD, and KEY_ALIAS, or configure the repository-root key.properties file."
            )
        }
        val store = requireNotNull(resolvedReleaseStoreFile)
        if (!store.isFile) {
            throw GradleException(
                "Release keystore file does not exist at the configured KEYSTORE_PATH/storeFile."
            )
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(generateFinanceRasterIcons)
}

tasks.configureEach {
    if (name == "assembleRelease" || name == "bundleRelease") {
        dependsOn(validateReleaseSigning)
    }
}

android {
    sourceSets.getByName("main").res.srcDir(financeRasterRes)
    namespace = "com.byepro.bye_pro_finance"
    compileSdk = 36

    defaultConfig {
        // Must remain identical to the installed Flutter application's ID.
        applicationId = "com.byepro.bye_pro_finance"
        minSdk = 30
        targetSdk = 36
        versionCode = 159
        versionName = "1.1.58"
        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }

    if (releaseSigningConfigured) {
        signingConfigs {
            create("release") {
                storeFile = requireNotNull(resolvedReleaseStoreFile)
                storePassword = requireNotNull(releaseStorePassword)
                keyPassword = requireNotNull(releaseKeyPassword)
                keyAlias = requireNotNull(releaseKeyAlias)
            }
        }
    }

    buildTypes {
        debug {
            // Preview cannot replace the production app or access its sandbox.
            applicationIdSuffix = ".nativepreview"
            versionNameSuffix = "-native-preview"
        }
        release {
            isMinifyEnabled = false
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
