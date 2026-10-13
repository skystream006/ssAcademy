package com.ssmath.app

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.Locale
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h800dp")
class SkinsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val application get() = ApplicationProvider.getApplicationContext<Application>()
    private val settings get() = application.getSharedPreferences("settings", 0)

    @Before fun setup() {
        settings.edit().clear().commit()
        File(application.filesDir, "practice_history.json").delete()
        DebugLog.initialize(application)
    }

    @Test fun registryPreservesOriginalSkinsAndAppendsExactUpstreamChoices() {
        val original = listOf("CHERRY_BLOSSOM", "STARRY_CITY", "OCEAN_WAVE", "OCEAN_MOONLIGHT",
            "GALAXY", "TROPICAL", "MECHANICS")
        val additions = listOf(
            Triple("AURORA_BOREALIS", "Aurora Borealis", "Luminous northern lights above snowy pines"),
            Triple("DESERT_DUNES", "Desert Dunes", "Golden sand ridges beneath a hazy desert sun"),
            Triple("BAMBOO_GROVE", "Bamboo Grove", "Misty green bamboo surrounding a quiet forest path"),
            Triple("AUTUMN_RIVER", "Autumn River", "Amber foliage along a winding reflective river"),
            Triple("ALPINE_DAWN", "Alpine Dawn", "Snow-capped peaks reflected in a still mountain lake"),
            Triple("RAINY_WINDOW", "Rainy Window", "Rain-speckled glass with softly glowing city lights"),
            Triple("VOLCANIC_EMBER", "Volcanic Ember", "Glowing lava flowing through dark volcanic rock"),
            Triple("CRYSTAL_CAVERN", "Crystal Cavern", "Luminous crystal facets in a violet underground cave"),
            Triple("PAPER_LANTERNS", "Paper Lanterns", "Warm hanging lanterns above twilight rooftops"),
            Triple("SYNTHWAVE_GRID", "Synthwave Grid", "A striped neon sun over a retro perspective grid")
        )
        assertEquals(original + additions.map { it.first }, AppSkin.entries.map { it.name })
        additions.forEach { (name, label, description) ->
            val skin = AppSkin.valueOf(name)
            assertEquals(label, skin.label)
            assertEquals(description, skin.description)
        }
        assertEquals(17, AppSkin.entries.map { it.label }.toSet().size)
        assertEquals(17, AppSkin.entries.map { it.drawable }.toSet().size)
        AppSkin.entries.forEach { skin ->
            assertEquals("skin_${skin.name.lowercase(Locale.ROOT)}",
                application.resources.getResourceEntryName(skin.drawable))
        }
    }

    @Test fun everySkinPreferenceRoundTripsWithoutChangingTheColorTheme() {
        val model = MathViewModel(application)
        model.chooseWaveAppearance(false)
        model.chooseTheme("royal-purple")
        model.chooseDark(true)
        model.chooseSkins(true)
        val theme = model.theme
        val mode = model.mode
        AppSkin.entries.forEach { skin ->
            assertEquals(skin, AppSkin.fromPreference(skin.name))
            model.chooseSkin(skin)
            assertEquals(skin.name, settings.getString("skin", null))
            val restored = MathViewModel(application)
            assertEquals(skin, restored.skin)
            assertTrue(restored.skinsEnabled)
            assertEquals(theme, restored.theme)
            assertEquals(mode, restored.mode)
            assertFalse(restored.waveAppearance)
        }
        model.chooseSkins(false)
        val restored = MathViewModel(application)
        assertFalse(restored.skinsEnabled)
        assertEquals(AppSkin.SYNTHWAVE_GRID, restored.skin)
    }

    @Test fun missingOrUnknownPreferencesDefaultToOceanWave() {
        assertTrue(MathViewModel(application).skinsEnabled)
        listOf(null, "", "unknown", "aurora_borealis", "Aurora Borealis").forEach { value ->
            settings.edit().putString("skin", value).commit()
            assertEquals(AppSkin.OCEAN_WAVE, AppSkin.fromPreference(value))
            assertEquals(AppSkin.OCEAN_WAVE, MathViewModel(application).skin)
        }
    }

    @Test fun savedSkinAndDisabledPreferenceOverrideDefaults() {
        settings.edit().putString("skin", AppSkin.CHERRY_BLOSSOM.name).putBoolean("skins_enabled", false).commit()
        val restored = MathViewModel(application)
        assertEquals(AppSkin.CHERRY_BLOSSOM, restored.skin)
        assertFalse(restored.skinsEnabled)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun everyBundledVectorRendersDistinctDetailedArtwork() {
        val signatures = mutableSetOf<Int>()
        AppSkin.entries.forEach { skin ->
            val drawable = requireNotNull(application.getDrawable(skin.drawable))
            val bitmap = Bitmap.createBitmap(180, 320, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, bitmap.width, bitmap.height)
            drawable.draw(Canvas(bitmap))
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            assertTrue("${skin.name} fills the background", pixels.count { it ushr 24 > 128 } > pixels.size * 0.95)
            assertTrue("${skin.name} has varied illustration colors", pixels.toSet().size > 20)
            assertTrue("${skin.name} has distinct artwork", signatures.add(pixels.contentHashCode()))
            bitmap.recycle()
        }
    }

    @Test fun pickerOffersEverySkinAndAppliesSelectionsWithoutChangingTheTheme() {
        val model = MathViewModel(application)
        model.chooseSkins(true)
        val theme = model.theme
        val mode = model.mode
        compose.setContent {
            MathTheme {
                SkinSetting(model.skinsEnabled, model.skin, model::chooseSkin, model::chooseSkins)
            }
        }
        AppSkin.entries.forEach { skin ->
            compose.onNodeWithText(model.skin.label).performClick()
            compose.onNode(isDialog()).assertIsDisplayed()
            compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
                .assertCountEquals(17)
            compose.onNode(hasText(skin.label) and hasAnyAncestor(isDialog()))
                .performScrollTo().assertIsDisplayed()
                .assertTextContains(skin.description).assertHasClickAction().performClick()
            compose.onNode(isDialog()).assertDoesNotExist()
            assertEquals(skin, model.skin)
            assertEquals(skin, MathViewModel(application).skin)
            compose.onNodeWithText(skin.label).assertIsDisplayed()
        }
        assertEquals(theme, model.theme)
        assertEquals(mode, model.mode)
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp")
    fun pickerDismissalAndDisablingSkinsPreserveSelection() {
        val model = MathViewModel(application)
        model.chooseSkin(AppSkin.SYNTHWAVE_GRID)
        model.chooseSkins(false)
        compose.setContent {
            MathTheme {
                SkinSetting(model.skinsEnabled, model.skin, model::chooseSkin, model::chooseSkins)
            }
        }
        compose.onNodeWithText(model.skin.label).assertIsNotEnabled().performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithContentDescription("Skins").assertIsOff().performClick()
        compose.onNodeWithText(model.skin.label).assertIsEnabled().performClick()
        compose.onNode(hasText(model.skin.label) and hasAnyAncestor(isDialog()))
            .performScrollTo().assertIsSelected()
        compose.onNodeWithText("Close").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithText(model.skin.label).performClick()
        compose.runOnIdle {
            (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithContentDescription("Skins").performClick().assertIsOff()
        assertEquals(AppSkin.SYNTHWAVE_GRID, model.skin)
        assertEquals(AppSkin.SYNTHWAVE_GRID, MathViewModel(application).skin)
        assertFalse(MathViewModel(application).skinsEnabled)
    }
}
