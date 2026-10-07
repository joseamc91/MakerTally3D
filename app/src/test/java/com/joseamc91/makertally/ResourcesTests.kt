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
        assertFalse(en.containsKey("alpha_version"));assertFalse(es.containsKey("alpha_version"))
        assertEquals("Enter a valid number greater than zero",en["positive_number"])
        assertEquals("Introduce un número válido mayor que cero",es["positive_number"])
        assertEquals("Privacy policy",en["privacy_policy"]);assertEquals("Política de privacidad",es["privacy_policy"])
        // The complete offline policies must match the public documents byte-for-byte.
        assertEquals(File("../PRIVACY.md").readText(),File("src/main/res/raw/privacy_policy.md").readText())
        assertEquals(File("../PRIVACY.es.md").readText(),File("src/main/res/raw-es/privacy_policy.md").readText())
    }
    @Test fun nativeManifestHasNoInternetOrGoogleServicesAndDeclaresPerAppLocales() {
        val manifest=File("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("android.permission.INTERNET"));assertFalse(manifest.contains("com.google.android.gms"))
        assertTrue(manifest.contains("@xml/locales_config"));assertTrue(manifest.contains("autoStoreLocales"))
        val build=File("build.gradle.kts").readText()
        assertTrue(build.contains("minSdk = 24"));assertTrue(build.contains("targetSdk = 36"))
        assertTrue(build.contains("versionName = \"1.0.0\""));assertTrue(build.contains("versionCode = 5"))
    }
}
