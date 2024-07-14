springApplication {
    // compile for 17
    javaVersion = 17
    mainClass = "com.example.App"

    dependencies {
        implementation(project(":java-util"))
        implementation("org.springframework.boot:spring-boot-starter:3.3.1")

        implementation("com.google.guava:guava:32.1.3-jre")
    }

    testing {
        // test on 21
        javaVersion = 17

        dependencies {
            implementation("org.junit.jupiter:junit-jupiter:5.10.2")
        }
    }
}
