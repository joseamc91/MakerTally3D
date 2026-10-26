package com.joseamc91.makertally

import android.content.Intent
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.appcompat.app.AppCompatDelegate
import com.joseamc91.makertally.data.OnboardingState
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
}
