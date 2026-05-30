package com.wkq.buildlogic;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.tasks.TaskAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CheckArchitectureTask extends DefaultTask {
    private static final List<String> CHECKED_CONFIGURATIONS = Arrays.asList(
            "api",
            "implementation",
            "compileOnly",
            "runtimeOnly"
    );

    public CheckArchitectureTask() {
        setGroup("verification");
        setDescription("Check module dependency direction to keep modular boundaries clean.");
        getOutputs().upToDateWhen(task -> false);
        notCompatibleWithConfigurationCache("Architecture check reads the current project dependency model.");
    }

    @TaskAction
    public void check() {
        List<String> violations = new ArrayList<>();
        for (Project project : getProject().getRootProject().getSubprojects()) {
            String from = project.getPath();
            project.getConfigurations().matching(configuration ->
                    CHECKED_CONFIGURATIONS.contains(configuration.getName())
            ).forEach(configuration ->
                    configuration.getDependencies().withType(ProjectDependency.class).forEach(dependency -> {
                        String to = dependency.getDependencyProject().getPath();
                        if (isIllegalModuleDependency(from, to)) {
                            violations.add(from + " -> " + to + " (" + configuration.getName() + ")");
                        }
                    })
            );
        }

        if (!violations.isEmpty()) {
            throw new GradleException("Illegal module dependencies found:\n - " + String.join("\n - ", violations));
        }

        getLogger().lifecycle("Module dependency direction check passed.");
    }

    private boolean isIllegalModuleDependency(String from, String to) {
        if (from.startsWith(":core:") && (to.startsWith(":feature:") || to.startsWith(":component:"))) {
            return true;
        }
        if (from.startsWith(":feature:") && to.startsWith(":feature:")) {
            return true;
        }
        return from.startsWith(":component:") && to.startsWith(":feature:");
    }
}
