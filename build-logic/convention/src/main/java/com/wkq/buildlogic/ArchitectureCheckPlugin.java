package com.wkq.buildlogic;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ArchitectureCheckPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getTasks().register("checkArchitecture", CheckArchitectureTask.class);
    }
}
