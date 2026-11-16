#!/usr/bin/env python3
"""Static guardrails for the BYE PRO Finance Native Design System."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "app/src/main/java/com/byepro/bye_pro_finance/ui"
SCREEN = ROOT / "app/src/main/java/com/byepro/bye_pro_finance/MainActivity.kt"
COMPONENTS = UI / "FinanceComponents.kt"
SYSTEM = UI / "FinanceDesignSystem.kt"
HOME_DOMAIN = ROOT / "app/src/main/java/com/byepro/bye_pro_finance/domain/HomeCategory.kt"
STRINGS_AR = ROOT / "app/src/main/res/values/strings.xml"
STRINGS_EN = ROOT / "app/src/main/res/values-en/strings.xml"
COLORS = ROOT / "app/src/main/res/values/colors.xml"
DIMENS = ROOT / "app/src/main/res/values/dimens.xml"
STYLE_FILES = [
    ROOT / "app/src/main/res/values/styles.xml",
    ROOT / "app/src/main/res/values-night/styles.xml",
    ROOT / "app/src/main/res/drawable/finance_dialog_background_dark.xml",
    ROOT / "app/src/main/res/drawable/finance_dialog_background_light.xml",
]

errors: list[str] = []


def read(path: Path) -> str:
    if not path.is_file():
        errors.append(f"Missing required file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


def kotlin_palette(source: str, mode: str) -> dict[str, str]:
    match = re.search(
        rf"val {mode} = FinanceDesignColors\((.*?)\n    \)",
        source,
        flags=re.DOTALL,
    )
    if not match:
        errors.append(f"Could not find FinanceDesignSystem.{mode} palette")
        return {}
    values = dict(
        re.findall(
            r"(\w+)\s*=\s*0x([0-9A-Fa-f]{8})\.toInt\(\)",
            match.group(1),
        )
    )
    result: dict[str, str] = {}
    for key, argb in values.items():
        if not argb.upper().startswith("FF"):
            errors.append(f"{mode}.{key} must be an opaque ARGB color")
            continue
        snake = re.sub(r"(?<!^)(?=[A-Z])", "_", key).lower()
        result[f"finance_{mode}_{snake}"] = "#" + argb[2:].upper()
    return result


system = read(SYSTEM)
colors_xml = read(COLORS)
dimens_xml = read(DIMENS)
screen = read(SCREEN)
components = read(COMPONENTS)
styles = "\n".join(read(path) for path in STYLE_FILES)
home_domain = read(HOME_DOMAIN)
strings_ar = read(STRINGS_AR)
strings_en = read(STRINGS_EN)

# Android resource theme colors must mirror the semantic Kotlin palettes.
xml_colors = {
    name: value.upper()
    for name, value in re.findall(
        r'<color\s+name="([^"]+)"\s*>(#[0-9A-Fa-f]{6,8})</color>',
        colors_xml,
    )
}
for mode in ("dark", "light"):
    for name, expected in kotlin_palette(system, mode).items():
        actual = xml_colors.get(name)
        if actual != expected:
            errors.append(
                f"Palette mismatch for {name}: Kotlin={expected}, XML={actual or 'missing'}"
            )

# Platform-owned widgets must use the same core geometry tokens.
token_block_match = re.search(
    r"object FinanceDesignTokens \{(.*?)\n\}",
    system,
    flags=re.DOTALL,
)
token_block = token_block_match.group(1) if token_block_match else ""
tokens = {
    name: int(value)
    for name, value in re.findall(
        r"const val (BUTTON_HEIGHT|TOUCH_TARGET_SIZE|DIALOG_RADIUS)\s*=\s*(\d+)",
        token_block,
    )
}
xml_dimens = {
    name: value
    for name, value in re.findall(
        r'<dimen\s+name="([^"]+)"\s*>(\d+)dp</dimen>',
        dimens_xml,
    )
}
dimension_map = {
    "BUTTON_HEIGHT": "finance_button_height",
    "TOUCH_TARGET_SIZE": "finance_touch_target_size",
    "DIALOG_RADIUS": "finance_dialog_radius",
}
for token, resource in dimension_map.items():
    expected = tokens.get(token)
    actual = xml_dimens.get(resource)
    if expected is None or actual is None or int(actual) != expected:
        errors.append(
            f"Dimension mismatch for {resource}: token={expected}, XML={actual or 'missing'}"
        )

# Screen code must consume tokens/components, not invent visual rules locally.
screen_rules = {
    "raw dp literal": r"\bdp\s*\(\s*\d+",
    "direct text size": r"\btextSize\s*=",
    "literal text-size value": r"\b(?:11|12|13|14|15|16|20)f\b",
    "literal text color": r"setTextColor\s*\(\s*0x",
    "literal background color": r"setBackgroundColor\s*\(\s*0x",
    "ad-hoc parsed color": r"Color\.parseColor",
}
for label, pattern in screen_rules.items():
    if re.search(pattern, screen):
        errors.append(f"MainActivity contains {label}; use FinanceDesignTokens/FinanceComponents")

component_rules = {
    "raw dp literal": r"\bdp\s*\(\s*context\s*,\s*\d+",
    "literal text-size value": r"\b(?:11|12|13|14|15|16|20)f\b",
}
for label, pattern in component_rules.items():
    if re.search(pattern, components):
        errors.append(f"FinanceComponents contains {label}; move it into FinanceDesignTokens")

# This repository is a standalone Native Android project, not a Flutter package.
for forbidden_name in ("pubspec.yaml", "pubspec.lock"):
    if (ROOT / forbidden_name).exists():
        errors.append(f"Flutter package file must not exist in the Native repository root: {forbidden_name}")
for path in ROOT.rglob("*"):
    if any(part in {".git", ".gradle", "build"} for part in path.parts):
        continue
    if path.is_file() and path.suffix == ".dart":
        errors.append(f"Flutter/Dart source file found in Native repository: {path.relative_to(ROOT)}")
    if path.is_dir() and path.name in {"ios", "web"}:
        errors.append(f"Non-Native application directory found in Native repository: {path.relative_to(ROOT)}")
settings_file = read(ROOT / "settings.gradle.kts")
if 'include(":app")' not in settings_file or "includeBuild(" in settings_file:
    errors.append("Standalone Native Gradle settings must include only the app module and no external included build")

# The Home contract must define exactly the three approved categories and their data sources.
home_category_markers = {
    "MONTHLY_BILLS": 'HomeRecordSource.BILLS, billKind = "monthly"',
    "RENT": 'HomeRecordSource.BILLS, billKind = "rent"',
    "EXPENSES": "HomeRecordSource.HOME_EXPENSES",
}
for category, mapping in home_category_markers.items():
    if category not in home_domain or mapping not in home_domain:
        errors.append(f"Home category mapping is missing or changed: {category}")
if len(re.findall(r"^    (?:MONTHLY_BILLS|RENT|EXPENSES)\(", home_domain, flags=re.MULTILINE)) != 3:
    errors.append("Home must contain exactly three primary categories.")

# The screen must consume the shared Home contract and shared entry components.
for marker in (
    "HomeCategory.ordered.map",
    "FinanceComponents.categoryTabs(",
    "FinanceComponents.categoryDescription(",
    "private fun showWelcomeScreen()",
    "private fun showLoginScreen()",
    "FinanceDesignSystem.button(",
    "FinanceDesignSystem.input(",
):
    if marker not in screen:
        errors.append(f"MainActivity is missing required shared UI integration: {marker}")

# Launch and Home copy must exist in both supported languages.
required_strings = (
    "home_monthly_bills",
    "home_rent",
    "home_expenses",
    "home_monthly_bills_description",
    "home_rent_description",
    "home_expenses_description",
    "welcome_title",
    "welcome_description",
    "continue_to_login",
    "swipe_to_continue",
    "login_title",
    "login_description",
    "login_invalid",
)
for string_name in required_strings:
    pattern = rf'<string\s+name="{re.escape(string_name)}">'
    if not re.search(pattern, strings_ar):
        errors.append(f"Arabic string resource is missing: {string_name}")
    if not re.search(pattern, strings_en):
        errors.append(f"English string resource is missing: {string_name}")

# Hex values belong only in the palette resource, not individual framework styles.
if re.search(r"#[0-9A-Fa-f]{6,8}", styles):
    errors.append("A framework style/drawable contains a literal color; use @color resources")

if errors:
    print("Design System verification FAILED:")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Design System verification PASSED: palette parity, shared dimensions and screen guardrails are valid.")
