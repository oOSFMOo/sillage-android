plugins { alias(libs.plugins.android.library) }
android {
    namespace = "eu.davidea.flexibleadapter"
    compileSdk = 37
    defaultConfig {
        minSdk = 26
        buildConfigField("String", "VERSION_NAME", "\"5.1.0-c8013533\"")
    }
    buildFeatures { buildConfig = true }
}
dependencies { implementation(libs.androidx.recyclerView) }
