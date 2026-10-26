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
        assertEquals("1200 W durante 1 minuto",es["setup_heating_example"])
        assertEquals("1200 W for 1 minute",en["setup_heating_example"])
        assertTrue(es.getValue("setup_heating_reference").contains("usar 0"))
        assertTrue(en.getValue("setup_heating_reference").contains("use 0"))
        assertTrue(es.getValue("setup_heating_caution").contains("depende de cada impresora"))
        assertTrue(en.getValue("setup_heating_caution").contains("depends on each printer"))
        assertEquals("((Precio final ÷ Precio base) − 1) × 100",es["setup_tax_formula"])
        assertEquals("((Final price ÷ Base price) − 1) × 100",en["setup_tax_formula"])
        assertTrue(listOf(es,en).all{it.getValue("setup_tax_included").contains("0 %")})
        assertTrue(listOf(es,en).all{!it.getValue("setup_printer_intro").contains("\\n")})
        assertEquals("PLA: 120 W · PETG: 140 W · ASA/ABS: 180 W",es["print_power_reference_values"])
        assertEquals(es["print_power_reference_values"],en["print_power_reference_values"])
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
