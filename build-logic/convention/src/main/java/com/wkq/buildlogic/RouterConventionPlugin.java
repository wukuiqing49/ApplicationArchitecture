package com.wkq.buildlogic;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.lang.reflect.Method;

public class RouterConventionPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("com.google.devtools.ksp");

        project.getExtensions().configure("ksp", ksp ->
                invoke(ksp, "arg", "moduleName", inferModuleName(project.getName()))
        );

        project.getDependencies().add("implementation", project.project(":core:core_router_api"));
        project.getDependencies().add("ksp", project.project(":core:core_router_processor"));
    }

    private static String inferModuleName(String projectName) {
        String[] parts = projectName.split("[_-]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            builder.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return builder.toString();
    }

    private static Object invoke(Object target, String methodName, Object... args) {
        try {
            Method method = findMethod(target.getClass(), methodName, args);
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to call Gradle API: " + target.getClass().getName() + "#" + methodName, e);
        }
    }

    private static Method findMethod(Class<?> clazz, String methodName, Object[] args) throws NoSuchMethodException {
        for (Method method : clazz.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean matched = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (args[i] == null) {
                    continue;
                }
                if (!parameterTypes[i].isAssignableFrom(args[i].getClass())) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return method;
            }
        }
        throw new NoSuchMethodException(clazz.getName() + "." + methodName);
    }
}
