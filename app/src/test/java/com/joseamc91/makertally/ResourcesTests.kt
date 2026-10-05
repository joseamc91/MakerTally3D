package com.joseamc91.makertally
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ResourcesTests {
    @Test fun bothLocalesHaveTheSameCompleteNonEmptyResourceKeys() {
        val parser=DocumentBuilderFactory.newInstance().newDocumentBuilder()
        fun strings(path:String):Map<String,String> {
            val nodes=parser.parse(File(path)).getElementsByTagName("string")
            return (0 until nodes.length).associate { index->val node=nodes.item(index);node.attributes.getNamedItem("name").nodeValue to node.textContent }
        }
        val en=strings("src/main/res/values/strings.xml");val es=strings("src/main/res/values-es/strings.xml")
        assertEquals(en.keys,es.keys);assertTrue(en.values.all{it.isNotBlank()});assertTrue(es.values.all{it.isNotBlank()})
        assertEquals("Machine (additional)",en["machine_additional"]);assertEquals("Máquina (adicional)",es["machine_additional"])
        assertFalse(en.keys.any{it.contains("roundup")})
    }
    @Test fun nativeManifestHasNoInternetOrGoogleServicesAndDeclaresPerAppLocales() {
        val manifest=File("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("android.permission.INTERNET"));assertFalse(manifest.contains("com.google.android.gms"))
        assertTrue(manifest.contains("@xml/locales_config"));assertTrue(manifest.contains("autoStoreLocales"))
        val build=File("build.gradle.kts").readText()
        assertTrue(build.contains("minSdk = 24"));assertTrue(build.contains("targetSdk = 36"));assertTrue(build.contains("versionName = \"0.1-alpha4\""))
    }
}
