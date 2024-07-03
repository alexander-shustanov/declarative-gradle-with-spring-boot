package org.gradle.api.experimental.android;

import androidx.baselineprofile.gradle.consumer.BaselineProfileConsumerExtension;
import androidx.room.gradle.RoomExtension;
import com.android.build.api.dsl.BuildType;
import com.android.build.api.dsl.CommonExtension;
import com.android.build.api.dsl.UnitTestOptions;
import com.google.devtools.ksp.gradle.KspExtension;
import org.gradle.api.JavaVersion;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.experimental.android.extensions.BaselineProfile;
import org.gradle.api.experimental.android.extensions.testing.AndroidTestDependencies;
import org.gradle.api.experimental.android.extensions.testing.TestOptions;
import org.gradle.api.experimental.android.extensions.testing.Testing;
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension;

import java.io.File;

import static org.gradle.api.experimental.android.AndroidSupport.ifPresent;
import static org.gradle.api.experimental.android.extensions.ComposeSupport.configureCompose;

public abstract class AbstractAndroidSoftwarePlugin implements Plugin<Project> {
    protected static final int DEFAULT_MIN_ANDROID_SDK = 21;

    protected abstract AndroidSoftware getAndroidSoftware();

    @Override
    public void apply(Project project) {
        AndroidSoftware dslModel = getAndroidSoftware();

        // Setup Android software conventions
        dslModel.getMinSdk().convention(DEFAULT_MIN_ANDROID_SDK); // https://developer.android.com/build/multidex#mdex-gradle
        dslModel.getVectorDrawablesUseSupportLibrary().convention(false);

        // Setup minify conventions
        dslModel.getBuildTypes().getDebug().getMinify().getEnabled().convention(false);
        dslModel.getBuildTypes().getDebug().getBaselineProfile().getEnabled().convention(false);
        dslModel.getBuildTypes().getRelease().getMinify().getEnabled().convention(false);
        dslModel.getBuildTypes().getRelease().getBaselineProfile().getEnabled().convention(false);

        // Setup desugaring conventions and desugar automatically when JDK > 8 is targeted
        dslModel.getCoreLibraryDesugaring().getEnabled().convention(project.provider(() -> dslModel.getJdkVersion().get() > 8));
        dslModel.getCoreLibraryDesugaring().getLibVersion().convention("2.0.4");

        // Setup Serialization conventions
        dslModel.getKotlinSerialization().getEnabled().convention(false);
        dslModel.getKotlinSerialization().getVersion().convention("1.6.3");
        dslModel.getKotlinSerialization().getJsonEnabled().convention(false);

        // Setup Linting conventions
        dslModel.getLint().getEnabled().convention(false);
        dslModel.getLint().getXmlReport().convention(false);
        dslModel.getLint().getCheckDependencies().convention(false);

        // Setup other feature extension conventions
        dslModel.getFeature().getEnabled().convention(false);
        dslModel.getCompose().getEnabled().convention(false);
        dslModel.getHilt().getEnabled().convention(false);
        dslModel.getRoom().getEnabled().convention(false);
        dslModel.getRoom().getVersion().convention("2.6.1");
        dslModel.getLicenses().getEnabled().convention(false);
        dslModel.getBaselineProfile().getEnabled().convention(false);

        // Setup Test Options conventions
        dslModel.getTesting().getTestOptions().getIncludeAndroidResources().convention(false);
        dslModel.getTesting().getTestOptions().getReturnDefaultValues().convention(false);
        dslModel.getTesting().getJacoco().getEnabled().convention(false);
        dslModel.getTesting().getJacoco().getVersion().convention("0.8.7");
        dslModel.getTesting().getRoborazzi().getEnabled().convention(false);
    }

    /**
     * Performs common dependency linking actions that do not need to occur within an afterEvaluate block.
     */
    @SuppressWarnings("UnstableApiUsage")
    protected void linkCommonDependencies(AndroidSoftwareDependencies dependencies, ConfigurationContainer configurations) {
        configurations.getByName("implementation").fromDependencyCollector(dependencies.getImplementation());
        configurations.getByName("compileOnly").fromDependencyCollector(dependencies.getCompileOnly());
        configurations.getByName("runtimeOnly").fromDependencyCollector(dependencies.getRuntimeOnly());
    }

    /**
     * Performs linking actions that must occur within an afterEvaluate block.
     */
    protected void linkDslModelToPlugin(Project project, AndroidSoftware dslModel, CommonExtension<?, ?, ?, ?, ?, ?> android) {
        KotlinAndroidProjectExtension kotlin = project.getExtensions().getByType(KotlinAndroidProjectExtension.class);

        // Link common properties
        ifPresent(dslModel.getNamespace(), android::setNamespace);
        ifPresent(dslModel.getCompileSdk(), android::setCompileSdk);
        android.defaultConfig(defaultConfig -> {
            ifPresent(dslModel.getMinSdk(), defaultConfig::setMinSdk);
            ifPresent(dslModel.getVectorDrawablesUseSupportLibrary(), defaultConfig.getVectorDrawables()::setUseSupportLibrary);
            return null;
        });
        android.compileOptions(compileOptions -> {
            // Up to Java 11 APIs are available through desugaring
            // https://developer.android.com/studio/write/java11-minimal-support-table
            compileOptions.setSourceCompatibility(JavaVersion.toVersion(dslModel.getJdkVersion().get()));
            compileOptions.setTargetCompatibility(JavaVersion.toVersion(dslModel.getJdkVersion().get()));
            return null;
        });
        ifPresent(dslModel.getJdkVersion(), jdkVersion -> {
            kotlin.jvmToolchain(jdkVersion);
            android.getCompileOptions().setSourceCompatibility(JavaVersion.toVersion(jdkVersion));
            android.getCompileOptions().setTargetCompatibility(JavaVersion.toVersion(jdkVersion));
        });

        // Link build types
        AndroidSoftwareBuildTypes modelBuildType = dslModel.getBuildTypes();
        NamedDomainObjectContainer<? extends BuildType> androidBuildTypes = android.getBuildTypes();
        linkBuildType(project, androidBuildTypes.getByName("debug"), modelBuildType.getDebug(), android);
        linkBuildType(project, androidBuildTypes.getByName("release"), modelBuildType.getRelease(), android);

        configureTesting(project, dslModel, android);

        configureKotlinSerialization(project, dslModel);
        configureDesugaring(project, dslModel, android);
        configureHilt(project, dslModel);
        configureCompose(project, dslModel, android);
        configureRoom(project, dslModel);
        configureLicenses(project, dslModel);

        if (project.getExtensions().findByName("baselineProfile") != null) {
            BaselineProfileConsumerExtension baselineProfileExtension = project.getExtensions().getByType(BaselineProfileConsumerExtension.class);
            configureBaselineProfile(project, dslModel.getBaselineProfile(), baselineProfileExtension);
        }
    }

    private static void configureLicenses(Project project, AndroidSoftware dslModel) {
        if (dslModel.getLicenses().getEnabled().get()) {
            project.getPlugins().apply("com.google.android.gms.oss-licenses-plugin");
        }
    }

    protected void configureHilt(Project project, AndroidSoftware dslModel) {
        if (dslModel.getHilt().getEnabled().get()) {
            project.getLogger().info("Hilt is enabled in: " + project.getPath());

            // Add support for KSP
            project.getPlugins().apply("com.google.devtools.ksp");
            project.getDependencies().add("ksp", "com.google.dagger:hilt-android-compiler:2.50");

            // Add support for Hilt
            project.getPlugins().apply("dagger.hilt.android.plugin");
            project.getDependencies().add("implementation", "com.google.dagger:hilt-android:2.50");

            project.getDependencies().add("kspTest", "com.google.dagger:hilt-android-compiler:2.50");
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    protected void configureRoom(Project project, AndroidSoftware dslModel) {
        if (dslModel.getRoom().getEnabled().get()) {
            project.getLogger().info("Room is enabled in: " + project.getPath());

            project.getPlugins().apply("androidx.room");
            project.getPlugins().apply("com.google.devtools.ksp");

            KspExtension kspExtension = project.getExtensions().getByType(KspExtension.class);
            kspExtension.arg("room.generateKotlin", "true");

            RoomExtension room = project.getExtensions().getByType(RoomExtension.class);
            // The schemas directory contains a schema file for each version of the Room database.
            // This is required to enable Room auto migrations.
            // See https://developer.android.com/reference/kotlin/androidx/room/AutoMigration.
            ifPresent(dslModel.getRoom().getSchemaDirectory(), room::schemaDirectory);

            String roomVersion = dslModel.getRoom().getVersion().get();
            dslModel.getDependencies().getImplementation().add("androidx.room:room-runtime:" + roomVersion);
            dslModel.getDependencies().getImplementation().add("androidx.room:room-ktx:" + roomVersion);
            project.getDependencies().add("ksp", "androidx.room:room-compiler:" + roomVersion);
        }
    }

    protected void configureDesugaring(Project project, AndroidSoftware dslModel, CommonExtension<?, ?, ?, ?, ?, ?> android) {
        if (dslModel.getCoreLibraryDesugaring().getEnabled().get()) {
            project.getLogger().info("Core library desugaring is enabled in: " + project.getPath());

            android.compileOptions(compileOptions -> {
                compileOptions.setCoreLibraryDesugaringEnabled(dslModel.getCoreLibraryDesugaring().getEnabled().get());
                return null;
            });

            project.getDependencies().addProvider("coreLibraryDesugaring", dslModel.getCoreLibraryDesugaring().getLibVersion().map(version -> "com.android.tools:desugar_jdk_libs:" + version));
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    protected void configureTesting(Project project, AndroidSoftware dslModel, CommonExtension<?, ?, ?, ?, ?, ?> android) {
        Testing testing = dslModel.getTesting();
        AndroidTestDependencies testDependencies = testing.getDependencies();

        TestOptions testOptions = testing.getTestOptions();
        ifPresent(testOptions.getTestInstrumentationRunner(), android.getDefaultConfig()::setTestInstrumentationRunner);

        UnitTestOptions unitTestOptions = android.getTestOptions().getUnitTests();
        unitTestOptions.setIncludeAndroidResources(testOptions.getIncludeAndroidResources().get());
        unitTestOptions.setReturnDefaultValues(testOptions.getReturnDefaultValues().get());

        ConfigurationContainer configurations = project.getConfigurations();
        configurations.getByName("testImplementation").fromDependencyCollector(testDependencies.getImplementation());
        configurations.getByName("testCompileOnly").fromDependencyCollector(testDependencies.getCompileOnly());
        configurations.getByName("testRuntimeOnly").fromDependencyCollector(testDependencies.getRuntimeOnly());
        configurations.getByName("androidTestImplementation").fromDependencyCollector(testDependencies.getAndroidImplementation());

        configureRoborazzi(project, dslModel);
    }

    protected void configureRoborazzi(Project project, AndroidSoftware dslModel) {
        if (dslModel.getTesting().getRoborazzi().getEnabled().get()) {
            project.getPlugins().apply("io.github.takahirom.roborazzi");
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    protected void configureKotlinSerialization(Project project, AndroidSoftware dslModel) {
        if (dslModel.getKotlinSerialization().getEnabled().get()) {
            project.getLogger().info("Kotlin Serialization is enabled in: " + project.getPath());

            project.getPlugins().apply("org.jetbrains.kotlin.plugin.serialization");
            project.getConfigurations().getByName("testImplementation").fromDependencyCollector(dslModel.getKotlinSerialization().getDependencies().getImplementation());

            if (dslModel.getKotlinSerialization().getJsonEnabled().get()) {
                project.getDependencies().addProvider("implementation", dslModel.getKotlinSerialization().getVersion().map(version -> "org.jetbrains.kotlinx:kotlinx-serialization-json:" + version));
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void configureBaselineProfile(Project project, BaselineProfile baselineProfile, BaselineProfileConsumerExtension baselineProfileExtension) {
        if (baselineProfile.getEnabled().get()) {
            project.getPlugins().apply("androidx.baselineprofile");

            baselineProfileExtension.setAutomaticGenerationDuringBuild(baselineProfile.getAutomaticGenerationDuringBuild().get());

            project.getConfigurations().getByName("baselineProfile").fromDependencyCollector(baselineProfile.getDependencies().getProfile());
        }
    }

    /**
     * Links build types from the model to the android extension.
     */
    protected void linkBuildType(Project project, BuildType buildType, AndroidSoftwareBuildType model, CommonExtension<?, ?, ?, ?, ?, ?> android) {
        buildType.setMinifyEnabled(model.getMinify().getEnabled().get());
        linkBuildTypeDependencies(buildType, model.getDependencies(), project.getConfigurations());

        model.getDefaultProguardFiles().get().forEach(proguardFile -> {
            File defaultProguardFile = android.getDefaultProguardFile(proguardFile.getName().get());
            buildType.proguardFile(defaultProguardFile);
        });
        model.getProguardFiles().get().forEach(proguardFile -> {
            buildType.proguardFile(proguardFile.getName().get());
        });

        if (buildType.getExtensions().findByName("baselineProfile") != null) {
            BaselineProfileConsumerExtension baselineProfileExtension = buildType.getExtensions().getByType(BaselineProfileConsumerExtension.class);
            configureBaselineProfile(project, model.getBaselineProfile(), baselineProfileExtension);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    protected void linkBuildTypeDependencies(BuildType buildType, AndroidSoftwareDependencies dependencies, ConfigurationContainer configurations) {
        String name = buildType.getName();
        configurations.getByName(name + "Implementation").fromDependencyCollector(dependencies.getImplementation());
        configurations.getByName(name + "CompileOnly").fromDependencyCollector(dependencies.getCompileOnly());
        configurations.getByName(name + "RuntimeOnly").fromDependencyCollector(dependencies.getRuntimeOnly());
    }
}
