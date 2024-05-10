package org.gradle.api.experimental.android.library;

import org.gradle.api.Action;
import org.gradle.api.artifacts.dsl.Dependencies;
import org.gradle.api.artifacts.dsl.DependencyCollector;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.declarative.dsl.model.annotations.Adding;
import org.gradle.declarative.dsl.model.annotations.Configuring;
import org.gradle.declarative.dsl.model.annotations.Restricted;

/**
 * Extension to manage setting up Kotlin serialization for a Kotlin Android library.
 */
@SuppressWarnings("UnstableApiUsage")
@Restricted
public interface KotlinSerialization {
    /**
     * Kotlin serialization library version to use.
     */
    @Restricted
    Property<String> getVersion();

    @Nested
    SerializationDependencies getDependencies();

    @Configuring
    default void dependencies(Action<? super SerializationDependencies> action) {
        action.execute(getDependencies());
    }

    @Adding
    default void json() {
        getDependencies().getImplementation().add("org.jetbrains.kotlinx:kotlinx-serialization-json:" + getVersion().get());
    }

    @Restricted
    interface SerializationDependencies extends Dependencies {
        DependencyCollector getImplementation();
    }
}


