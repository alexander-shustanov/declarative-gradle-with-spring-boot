/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    id("com.example.java-library-conventions")
}

javaLibrary {
    dependencies {
        api(project(":list"))
    }
}