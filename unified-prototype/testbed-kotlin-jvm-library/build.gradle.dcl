kotlinJvmLibrary {
    javaVersion = 17

    dependencies {
        implementation(project(":kotlin-jvm-util"))
        implementation("com.google.guava:guava:32.1.3-jre")
    }
}
