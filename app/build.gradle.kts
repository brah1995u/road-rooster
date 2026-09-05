plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
}

abstract class OptimizeReleaseArtTask : Exec() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
}

val optimizeReleaseArt by tasks.registering(OptimizeReleaseArtTask::class) {
    outputDirectory.set(layout.buildDirectory.dir("generated/optimizedReleaseRes"))
    val source = layout.projectDirectory.dir("src/main/res/drawable-nodpi")
    val script = rootProject.layout.projectDirectory.file("tools/optimize_release_art.py")
    val report = layout.buildDirectory.file("reports/release-art.json")
    inputs.files(fileTree(source) { include("*.png") })
    inputs.file(script)
    outputs.file(report)
    val python = providers.gradleProperty("assetPython").getOrElse("python")
    doFirst {
        // AGP assigns the generated-source output directory after task registration.
        commandLine(
            python, script.asFile,
            "--source", source.asFile,
            "--output", outputDirectory.get().asFile,
            "--report", report.get().asFile,
        )
    }
}

android {
    namespace = "com.chickenroadrunner.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chickenroadrunner.game"
        minSdk = 24
        targetSdk = 35
        versionCode = 11
        versionName = "0.4.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }


    testOptions {
        unitTests.all {
            it.maxHeapSize = "512m"
            it.maxParallelForks = 1
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "SHOW_DEBUG_TOOLS", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "SHOW_DEBUG_TOOLS", "false")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        // Track generated resources for every AGP consumer, including lint/deep links.
        variant.sources.res?.addGeneratedSourceDirectory(
            optimizeReleaseArt, OptimizeReleaseArtTask::outputDirectory,
        )
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}
