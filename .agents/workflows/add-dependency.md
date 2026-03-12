---
description: 添加新的第三方依赖（自动写入 libs.versions.toml）
---

在本项目中，**所有依赖版本必须通过 `gradle/libs.versions.toml` 统一管理**，不允许在 `build.gradle` 中直接写版本号。

**使用方式**：告诉我要添加的库名称，我会帮你完成所有步骤。例如：
> "按照 add-dependency workflow 添加 Retrofit 依赖"

### Step 1: 确认依赖信息
查找该库的以下信息（我会帮你搜索）：
- `group`（如 `com.squareup.retrofit2`）
- `name`（如 `retrofit`）
- 最新稳定版本号

### Step 2: 修改 libs.versions.toml

在 `[versions]` 区块添加版本：
```toml
retrofit = "2.9.0"
```

在 `[libraries]` 区块添加依赖条目：
```toml
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
```

### Step 3: 在 build.gradle 中引用

```groovy
dependencies {
    implementation(libs.retrofit)
}
```

### Step 4: 同步项目
// turbo
```bash
./gradlew --write-verification-metadata sha256
```

> ⚠️ **注意**：敏感信息（如 API Key）必须放在 `local.properties`，不能硬编码在代码或资源文件中。
