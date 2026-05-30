package com.wkq.buildlogic;

import org.gradle.api.Project;

public class AndroidComponentConventionPlugin extends AndroidLibraryConventionPlugin {
    @Override
    public void apply(Project project) {
        super.apply(project);

        DependencyUtils.addLibrary(project, "implementation", "androidx-core-ktx");
    }
}
