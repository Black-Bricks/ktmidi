import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath(libs.metalava.gradle)
    }
}

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
    id("maven-publish")
    id("signing")
    id("me.tylerbwong.gradle.metalava") version "0.3.3"
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    android {
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("debug", "release")
    }
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport {}
                }
            }
        }
        nodejs {
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries {
            framework {
                baseName = "ktmidi"
            }
        }

        it.compilations.getByName("main") {
            @Suppress("UnusedPrivateMember")
            val rtmidi by cinterops.creating {
                defFile("src/iosMain/rtmidi/rtmidi.def")
                includeDirs("src/iosMain/rtmidi/include")
            }
        }

        it.binaries.all {
            linkerOpts(
                "-L$projectDir/src/iosMain/rtmidi/lib",
                "-lrtmidi",
            )
        }
    }

    macosArm64()
    macosX64()
    // kotlinx-datetime is not built enough to make it possible...
    //linuxArm64()
    linuxX64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.io)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val jvmMain by getting
        val androidMain by getting {
            dependencies {
                implementation(libs.core.ktx)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(npm("jzz", "1.4.7"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
        val nativeMain by creating
        val nativeTest by creating
        val macosArm64Main by getting
        val macosX64Main by getting
        // kotlinx-datetime is not built enough to make it possible...
        //val linuxArm64Main by getting
        val linuxX64Main by getting
        val mingwX64Main by getting
        val iosArm64Main by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Main by getting
        val iosSimulatorArm64Test by getting
        val iosX64Main by getting
        val iosX64Test by getting

        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

metalava {
    filename = "api/$name-api.txt"
    outputKotlinNulls = false
    includeSignatureVersion = false
}

android {
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["test"].assets.srcDir("src/commonTest/resources") // kind of hack...
    defaultConfig {
        compileSdk = 33
        minSdk = 23
    }
    buildTypes {
        val debug by getting
        val release by getting
    }
}

afterEvaluate {
    val javadocJar by tasks.registering(Jar::class) {
        archiveClassifier.set("javadoc")
    }

    publishing {
        publications.withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set("ktmidi")
                description.set("Kotlin Multiplatform library for MIDI 1.0 and MIDI 2.0")
                url.set("https://github.com/atsushieno/ktmidi")
                scm {
                    url.set("https://github.com/atsushieno/ktmidi")
                }
                licenses {
                    license {
                        name.set("the MIT License")
                        url.set("https://github.com/atsushieno/ktmidi/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("atsushieno")
                        name.set("Atsushi Eno")
                        email.set("atsushieno@gmail.com")
                    }
                }
            }
        }

        repositories {
            /*
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/atsushieno/ktmidi")
                    credentials {
                        username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
                        password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
                    }
                }
                */
            maven {
                name = "OSSRH"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("OSSRH_USERNAME")
                    password = System.getenv("OSSRH_PASSWORD")
                }
            }
        }
    }

    // keep it as is. It is replaced by CI release builds
    signing {}
}
