kotlinJvmApplication {
    javaVersion = 17
    mainClass = "com.example.AppKt"

    dependencies {
        implementation(project(":kotlin-jvm-util"))
        implementation("com.google.guava:guava:32.1.3-jre")
    }
}
