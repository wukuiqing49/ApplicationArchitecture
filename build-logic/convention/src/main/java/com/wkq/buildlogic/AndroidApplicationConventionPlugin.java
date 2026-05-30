package com.wkq.buildlogic;

import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.lang.reflect.Method;
import java.util.Map;

public class AndroidApplicationConventionPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("com.android.application");
        project.getPluginManager().apply("org.jetbrains.kotlin.android");
        project.getPluginManager().apply("com.google.devtools.ksp");

        project.getExtensions().configure("android", android -> {
            Object appConfig = project.getRootProject().getExtensions().getExtraProperties().get("appConfig");
            String appNamespace = (String) getConfigValue(appConfig, "appNamespace");

            invoke(android, "setNamespace", appNamespace);
            invoke(android, "setCompileSdk", getConfigValue(appConfig, "compileSdk"));

            Object defaultConfig = invoke(android, "getDefaultConfig");
            invoke(defaultConfig, "setApplicationId", appNamespace);
            invoke(defaultConfig, "setMinSdk", getConfigValue(appConfig, "minSdk"));
            invoke(defaultConfig, "setTargetSdk", getConfigValue(appConfig, "targetSdk"));
            invoke(defaultConfig, "setVersionCode", getConfigValue(appConfig, "versionCode"));
            invoke(defaultConfig, "setVersionName", getConfigValue(appConfig, "versionName"));
            invoke(defaultConfig, "setTestInstrumentationRunner", "androidx.test.runner.AndroidJUnitRunner");

            Object buildTypes = invoke(android, "getBuildTypes");
            invoke(buildTypes, "getByName", "release", (Action<Object>) release -> {
                invoke(release, "setMinifyEnabled", true);
                invoke(release, "setShrinkResources", true);
                Object defaultProguard = invoke(android, "getDefaultProguardFile", "proguard-android-optimize.txt");
                invoke(release, "proguardFiles", (Object) new Object[]{defaultProguard, "proguard-rules.pro"});
            });

            Object compileOptions = invoke(android, "getCompileOptions");
            invoke(compileOptions, "setSourceCompatibility", JavaVersion.VERSION_17);
            invoke(compileOptions, "setTargetCompatibility", JavaVersion.VERSION_17);

            Object buildFeatures = invoke(android, "getBuildFeatures");
            invoke(buildFeatures, "setViewBinding", true);
            invoke(buildFeatures, "setBuildConfig", true);

            Object sourceSets = invoke(android, "getSourceSets");
            Object mainSourceSet = invoke(sourceSets, "getByName", "main");
            Object javaSourceDirectorySet = invoke(mainSourceSet, "getJava");
            Object routerSourceDir = project.getLayout()
                    .getBuildDirectory()
                    .dir("generated/source/router")
                    .get()
                    .getAsFile();
            invoke(javaSourceDirectorySet, "srcDir", routerSourceDir);
        });

        project.getExtensions().configure("kotlin", kotlin -> {
            Object compilerOptions = invoke(kotlin, "getCompilerOptions");
            Object jvmTarget = invoke(compilerOptions, "getJvmTarget");
            Object appConfig = project.getRootProject().getExtensions().getExtraProperties().get("appConfig");
            invoke(jvmTarget, "set", getConfigValue(appConfig, "jvmTarget"));
        });

        addDefaultDependencies(project);
    }

    private void addDefaultDependencies(Project project) {
        DependencyUtils.addLibrary(project, "implementation", "androidx-core-ktx");
        DependencyUtils.addLibrary(project, "implementation", "androidx-appcompat");
        DependencyUtils.addLibrary(project, "implementation", "material");
        DependencyUtils.addLibrary(project, "implementation", "androidx-constraintlayout");
        DependencyUtils.addLibrary(project, "testImplementation", "junit");
        DependencyUtils.addLibrary(project, "androidTestImplementation", "androidx-junit");
        DependencyUtils.addLibrary(project, "androidTestImplementation", "androidx-espresso-core");

        DependencyUtils.addProject(project, "implementation", ":core:core_base");
        DependencyUtils.addProject(project, "implementation", ":core:core_util");
        DependencyUtils.addProject(project, "implementation", ":core:core_storage");
        DependencyUtils.addProject(project, "implementation", ":core:core_media");
        DependencyUtils.addProject(project, "implementation", ":core:core_ui");
        DependencyUtils.addProject(project, "implementation", ":core:core_network");
        DependencyUtils.addProject(project, "implementation", ":core:core_router_api");
        DependencyUtils.addProject(project, "implementation", ":core:core_user");
    }

    @SuppressWarnings("unchecked")
    private static Object getConfigValue(Object appConfig, String key) {
        return ((Map<String, Object>) appConfig).get(key);
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
                Class<?> parameterType = wrap(parameterTypes[i]);
                if (!parameterType.isAssignableFrom(args[i].getClass())) {
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

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == boolean.class) return Boolean.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
