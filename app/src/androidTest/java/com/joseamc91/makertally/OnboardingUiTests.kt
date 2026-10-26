package com.joseamc91.makertally

import android.content.Intent
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.appcompat.app.AppCompatDelegate
import com.joseamc91.makertally.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Real Compose accessibility/actions and Activity recreation, using only the existing runner. */
class OnboardingUiTests {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private fun node(tag:String): AccessibilityNodeInfo? {
        fun find(n:AccessibilityNodeInfo?):AccessibilityNodeInfo? {
            if(n==null)return null
            if(n.viewIdResourceName==tag)return n
            for(i in 0 until n.childCount) find(n.getChild(i))?.let{return it}
            return null
        }
        return find(instrumentation.uiAutomation.rootInActiveWindow)
    }
    private fun waitFor(tag:String,enabled:Boolean?=null,text:String?=null):AccessibilityNodeInfo {
        val deadline=SystemClock.uptimeMillis()+15000
        while(SystemClock.uptimeMillis()<deadline) {
            val n=node(tag)
            if(n!=null && (enabled==null || n.isEnabled==enabled) && (text==null || n.text?.toString()==text))return n
            SystemClock.sleep(100)
        }
        error("Missing UI: $tag, enabled=$enabled")
    }
    private fun click(tag:String) { assertTrue(waitFor(tag,true).performAction(AccessibilityNodeInfo.ACTION_CLICK)) }
    private fun text(tag:String,value:String) {
        val n=waitFor(tag)
        assertTrue(n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value)
        }))
        // Accessibility actions are asynchronous; wait for Compose to apply the edit.
        waitFor(tag,text=value)
    }
    private fun scrollToEnd(tag:String) {
        repeat(12) {
            if(!waitFor(tag).performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))return
            instrumentation.waitForIdleSync();SystemClock.sleep(150)
        }
        error("Scroll did not reach end: $tag")
    }
    private fun bounds(tag:String)=Rect().also { waitFor(tag).getBoundsInScreen(it) }

    @Test fun onboardingKeepsInputsAndValidationAcrossPortraitLandscapeAndLocales() = runBlocking {
        val context=instrumentation.targetContext
        val repo=(context.applicationContext as MakerTallyApplication).repository
        val original=repo.load()
        val originalLocales=AppCompatDelegate.getApplicationLocales()
        val monitor=instrumentation.addMonitor(MainActivity::class.java.name,null,false)
        fun launch() { instrumentation.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) }
        fun finish() { monitor.lastActivity?.let { activity -> instrumentation.runOnMainSync { activity.finish() };instrumentation.waitForIdleSync() } }
        try {
            for(language in listOf("es-ES","en-US")) {
                repo.saveOnboarding(OnboardingState(language=language))
                launch();waitFor("onboarding_step_welcome");waitFor("onboarding_reassurance")
                click("onboarding_next");waitFor("onboarding_step_electricity")
                waitFor("onboarding_next",false)
                text("onboarding_electricity",",");text("onboarding_tax","0")
                waitFor("onboarding_next",false)
                val activity=monitor.lastActivity ?: error("Activity not created")
                instrumentation.runOnMainSync { activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                val deadline=SystemClock.uptimeMillis()+15000
                while(context.resources.configuration.orientation!=Configuration.ORIENTATION_LANDSCAPE && SystemClock.uptimeMillis()<deadline) SystemClock.sleep(100)
                assertEquals(Configuration.ORIENTATION_LANDSCAPE,context.resources.configuration.orientation)
                waitFor("onboarding_step_electricity")
                assertEquals(",",waitFor("onboarding_electricity").text?.toString())
                text("onboarding_electricity","0")
                click("onboarding_next");waitFor("onboarding_step_printer")
                click("onboarding_back");waitFor("onboarding_step_electricity")
                assertEquals("0",waitFor("onboarding_electricity").text?.toString())
                finish();launch();waitFor("onboarding_step_electricity")
                assertEquals("0",waitFor("onboarding_tax").text?.toString())
                finish()
            }
        } finally {
            finish();repo.saveOnboarding(original.onboarding)
            instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(originalLocales) }
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test fun referencesScrollAboveReassuranceAndFreshCalculatorErrorsWaitForInteraction() = runBlocking {
        val context=instrumentation.targetContext
        val repo=(context.applicationContext as MakerTallyApplication).repository
        val original=repo.load()
        val originalLocales=AppCompatDelegate.getApplicationLocales()
        val monitor=instrumentation.addMonitor(MainActivity::class.java.name,null,false)
        fun launch() { instrumentation.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) }
        fun finish() { monitor.lastActivity?.let { activity -> instrumentation.runOnMainSync { activity.finish() };instrumentation.waitForIdleSync() } }
        fun rotate(orientation:Int) {
            instrumentation.runOnMainSync { monitor.lastActivity!!.requestedOrientation=orientation }
            val expected=if(orientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
            val deadline=SystemClock.uptimeMillis()+15000
            while(context.resources.configuration.orientation!=expected && SystemClock.uptimeMillis()<deadline) SystemClock.sleep(100)
            assertEquals(expected,context.resources.configuration.orientation)
            instrumentation.waitForIdleSync()
        }
        fun errorState(tag:String,invalid:Boolean) {
            val deadline=SystemClock.uptimeMillis()+15000
            while(SystemClock.uptimeMillis()<deadline) {
                if(node(tag)?.isContentInvalid==invalid)return
                SystemClock.sleep(100)
            }
            error("Unexpected error state for $tag: $invalid")
        }
        try {
            for(language in listOf("es-ES","en-US")) {
                for(step in listOf(SetupStep.Printer,SetupStep.Costs)) {
                    repo.saveOnboarding(OnboardingState(step=step,language=language,electricity="0",tax="0"))
                    launch();waitFor("onboarding_step_${step.name.lowercase()}")
                    rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                    scrollToEnd("onboarding_content")
                    val reassurance=bounds("onboarding_reassurance")
                    val tags=if(step==SetupStep.Printer)listOf("onboarding_heating_example","onboarding_heating_caution")
                        else listOf("onboarding_machine_reference","onboarding_multiplier_reference")
                    tags.forEach { tag ->
                        assertTrue(waitFor(tag).isVisibleToUser)
                        assertTrue("$tag is covered by reassurance",bounds(tag).bottom<=reassurance.top)
                    }
                    assertTrue(bounds("onboarding_next").top>=reassurance.bottom)
                    if(step==SetupStep.Printer) {
                        assertEquals(if(language=="es-ES")"1200 W durante 1 minuto" else "1200 W for 1 minute",waitFor("onboarding_heating_example").text?.toString())
                        assertEquals("",repo.load().onboarding.heatingPower);assertEquals("",repo.load().onboarding.heatingMinutes)
                    }
                    finish()
                }
                repo.saveOnboarding(OnboardingState(language=language,completed=true,filamentIntroSeen=true))
                launch();waitFor("piece_weight");rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                assertEquals("",waitFor("piece_weight").text?.toString());assertEquals("",waitFor("hours").text?.toString())
                assertEquals("0",waitFor("minutes").text?.toString());assertEquals("—",waitFor("suggested_price").text?.toString())
                errorState("piece_weight",false);errorState("hours",false)
                rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                errorState("piece_weight",false);errorState("hours",false)
                text("piece_weight",".");errorState("piece_weight",true);errorState("hours",false)
                text("piece_weight","0");errorState("piece_weight",false)
                text("hours","1.5");errorState("hours",true)
                text("hours","0");errorState("hours",false)
                text("piece_weight","");errorState("piece_weight",true)
                finish()
            }
        } finally {
            finish();repo.saveOnboarding(original.onboarding)
            instrumentation.runOnMainSync { AppCompatDelegate.setApplicationLocales(originalLocales) }
            instrumentation.removeMonitor(monitor)
        }
    }
}
