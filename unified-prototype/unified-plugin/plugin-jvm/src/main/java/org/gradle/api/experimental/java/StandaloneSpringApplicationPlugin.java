package org.gradle.api.experimental.java;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.experimental.common.CliApplicationConventionsPlugin;
import org.gradle.api.experimental.jvm.internal.JvmPluginSupport;
import org.gradle.api.internal.plugins.software.SoftwareType;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.testing.base.TestingExtension;
import org.springframework.boot.gradle.dsl.SpringBootExtension;
import org.springframework.boot.gradle.plugin.SpringBootPlugin;

import javax.inject.Inject;

/**
 * Creates a declarative {@link SpringApplication} DSL model, applies the official Java application plugin,
 * and links the declarative model to the official plugin.
 */
abstract public class StandaloneSpringApplicationPlugin implements Plugin<Project> {

    public static final String SPRING_APPLICATION = "springApplication";

    @SoftwareType(name = SPRING_APPLICATION, modelPublicType = SpringApplication.class)
    abstract public SpringApplication getApplication();

    @Override
    public void apply(Project project) {
        SpringApplication dslModel = getApplication();
        project.getExtensions().add(SPRING_APPLICATION, dslModel);

        project.getPlugins().apply(ApplicationPlugin.class);
        project.getPlugins().apply(SpringBootPlugin.class);

        project.getExtensions().getByType(SpringBootExtension.class).getMainClass().set(dslModel.getMainClass());

        project.getExtensions().getByType(TestingExtension.class).getSuites().withType(JvmTestSuite.class).named("test").configure(testSuite -> {
            testSuite.useJUnitJupiter();
        });

        linkDslModelToPlugin(project, dslModel);
    }

    @Inject
    protected abstract JavaToolchainService getJavaToolchainService();

    private void linkDslModelToPlugin(Project project, SpringApplication dslModel) {
        JvmPluginSupport.linkJavaVersion(project, dslModel);
        JvmPluginSupport.linkApplicationMainClass(project, dslModel);
        JvmPluginSupport.linkMainSourceSourceSetDependencies(project, dslModel.getDependencies());
        JvmPluginSupport.linkTestJavaVersion(project, getJavaToolchainService(), dslModel.getTesting());
        JvmPluginSupport.linkTestSourceSourceSetDependencies(project, dslModel.getTesting().getDependencies());

        dslModel.getRunTasks().add(project.getTasks().named("run"));
    }
}
