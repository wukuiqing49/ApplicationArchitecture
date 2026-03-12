---
description: Run Code Analysis (Lint & Detekt)
---

This workflow helps you find unused resources, layouts, strings, and bad Kotlin code practices in your project.

### Step 1: Run Android Lint (Detect Unused Resources)
This will use Android's built-in Lint tool to scan for unused XML files, string resources, and potential bugs.

// turbo
```bash
./gradlew lint
```

When it finishes, you can find the detailed HTML reports in your module's build directory (e.g., `app/build/reports/lint-results-debug.html`).

### Step 2: (Optional) Setup & Run Detekt (Kotlin Code Smells)
If you want to check your Kotlin code for unused variables, functions, and style issues, you can run this script to inject Detekt into your build and run it.

```bash
# Apply Detekt to app module temporarily to check code
echo "plugins { id 'io.gitlab.arturbosch.detekt' version '1.23.6' }" >> app/build.gradle
./gradlew :app:detekt
```
The Detekt report will be available at `app/build/reports/detekt/detekt.html`.