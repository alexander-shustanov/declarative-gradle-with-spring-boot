import org.gradle.api.experimental.kmp.KmpJsTarget.JsEnvironment
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.gradle.experimental.kmp-library")
}

kmpLibrary {
    dependencies {
        api("io.ktor:ktor-client-core:2.3.7")
    }

    targets {
        jvm {
            jvmTarget = JvmTarget.JVM_14

            dependencies {
                api("org.apache.commons:commons-lang3:3.14.0")
            }
        }
        js {
            environment = JsEnvironment.NODE

            dependencies {
                implementation("com.squareup.sqldelight:runtime:1.5.5")
            }
        }
    }
}