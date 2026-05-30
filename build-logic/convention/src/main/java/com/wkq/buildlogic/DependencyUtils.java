package com.wkq.buildlogic;

import org.gradle.api.Project;
import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionCatalogsExtension;

final class DependencyUtils {
    private DependencyUtils() {
    }

    static void addLibrary(Project project, String configurationName, String alias) {
        VersionCatalog libs = project.getExtensions()
                .getByType(VersionCatalogsExtension.class)
                .named("libs");
        Object dependency = libs.findLibrary(alias)
                .orElseThrow(() -> new IllegalStateException("Missing version catalog library: " + alias));
        project.getDependencies().add(configurationName, dependency);
    }

    static void addProject(Project project, String configurationName, String path) {
        project.getDependencies().add(configurationName, project.project(path));
    }
}
