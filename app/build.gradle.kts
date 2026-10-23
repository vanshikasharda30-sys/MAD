plugins {
    alias(libs.plugins.android.application)
}

// Which backend the debug build talks to. Release builds always use production.
//   ./gradlew assembleDebug                      -> production (deployed Vercel API)
//   ./gradlew assembleDebug -Pclinicai.env=dev   -> http://10.0.2.2:3000/ (local backend, emulator only)
val debugEnvironment: String = (project.findProperty("clinicai.env") as String?) ?: "prod"

android {
    namespace = "com.example.clinicai"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.clinicai"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DEV_API_BASE_URL", "\"http://10.0.2.2:3000/\"")
        buildConfigField("String", "PROD_API_BASE_URL", "\"https://clinicai-555888.vercel.app/\"")
        // Clerk publishable keys are public by design (they ship in the website's HTML).
        buildConfigField("String", "CLERK_PUBLISHABLE_KEY", "\"pk_test_cHVtcGVkLWVmdC0xNS5jbGVyay5hY2NvdW50cy5kZXYk\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_ENVIRONMENT", "\"$debugEnvironment\"")
        }
        release {
            buildConfigField("String", "API_ENVIRONMENT", "\"prod\"")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.location)
    implementation(libs.play.services.code.scanner)
    implementation(libs.zxing.core)
    implementation(libs.razorpay.checkout)
    implementation(libs.clerk.android.api)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.androidx.arch.core.testing)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.rules)
}
