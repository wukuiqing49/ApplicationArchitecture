package com.wkq.buildlogic;

import org.gradle.api.Project;

public class AndroidFeatureConventionPlugin extends AndroidLibraryConventionPlugin {
    @Override
    public void apply(Project project) {
        super.apply(project);

        DependencyUtils.addLibrary(project, "implementation", "androidx-core-ktx");
        DependencyUtils.addLibrary(project, "implementation", "androidx-appcompat");
        DependencyUtils.addLibrary(project, "implementation", "androidx-lifecycle-viewmodel-ktx");
        DependencyUtils.addLibrary(project, "implementation", "material");
        DependencyUtils.addLibrary(project, "implementation", "androidx-lifecycle-runtime-ktx");
        DependencyUtils.addLibrary(project, "implementation", "androidx-activity-ktx");
        DependencyUtils.addLibrary(project, "implementation", "androidx-fragment-ktx");
        DependencyUtils.addLibrary(project, "implementation", "androidx-constraintlayout");
        DependencyUtils.addLibrary(project, "implementation", "androidx-recyclerview");
        DependencyUtils.addLibrary(project, "androidTestImplementation", "androidx-espresso-core");

        DependencyUtils.addProject(project, "implementation", ":core:core_ui");
        DependencyUtils.addProject(project, "implementation", ":core:core_base");
        DependencyUtils.addProject(project, "implementation", ":core:core_util");
        DependencyUtils.addProject(project, "implementation", ":core:core_user");
    }
}
