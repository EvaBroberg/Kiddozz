import java.io.File

fun main() {
    val manifest = File("app/build/intermediates/merged_manifests/localDebugAndroidTest/AndroidManifest.xml")
    if (!manifest.exists()) {
        println("❌ Manifest not found. Run: ./gradlew :app:processLocalDebugAndroidTestManifest")
        return
    }
    
    val text = manifest.readText()
    println("📄 Manifest file found and read successfully")
    
    // Check for targetPackage
    val targetPackageRegex = Regex("""android:targetPackage="([^"]+)"""")
    val targetPackageMatch = targetPackageRegex.find(text)
    
    if (targetPackageMatch == null) {
        println("❌ No targetPackage found. You need to add it manually in androidTest manifest.")
    } else {
        val pkg = targetPackageMatch.groupValues[1]
        if (pkg != "fi.kidozz.app") {
            println("⚠️ targetPackage mismatch detected: $pkg → should be fi.kidozz.app")
        } else {
            println("✅ targetPackage is correct: fi.kidozz.app")
        }
    }
    
    // Check for instrumentation name
    val nameRegex = Regex("""android:name="([^"]+)"""")
    val nameMatch = nameRegex.find(text)
    
    if (nameMatch == null) {
        println("❌ No android:name found in instrumentation tag")
    } else {
        val name = nameMatch.groupValues[1]
        if (name != "androidx.test.runner.AndroidJUnitRunner") {
            println("⚠️ android:name mismatch detected: $name → should be androidx.test.runner.AndroidJUnitRunner")
        } else {
            println("✅ android:name is correct: androidx.test.runner.AndroidJUnitRunner")
        }
    }
    
    // Check if instrumentation tag exists
    if (text.contains("<instrumentation")) {
        println("✅ <instrumentation> tag found")
    } else {
        println("❌ <instrumentation> tag not found")
    }
    
    // Show the current instrumentation block
    val instrumentationRegex = Regex("""<instrumentation[^>]*>""")
    val instrumentationMatch = instrumentationRegex.find(text)
    if (instrumentationMatch != null) {
        println("📋 Current instrumentation tag: ${instrumentationMatch.value}")
    }
}
