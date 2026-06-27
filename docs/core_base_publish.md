# core_base GitHub 网络引用说明

## 结论

- 推荐使用 JitPack：公开 GitHub 仓库打 tag 后，其他项目可以直接通过网络依赖引用，不需要账号 token。
- GitHub Packages 也已支持：适合私有库或公司内部包，但使用方需要配置 GitHub 账号和 token。
- 本地开发仍然使用 `implementation project(':core:core_base')`，不受发布配置影响。

## 方式一：JitPack 引用（推荐）

### 发布方

把项目推到 GitHub 后打 tag：

```bash
git tag v1.0.0
git push origin v1.0.0
```

JitPack 会读取仓库根目录的 `jitpack.yml`，并发布 `core_base` 的 release AAR 到 JitPack Maven。

### 使用方

在使用方项目的 `settings.gradle` 或 `settings.gradle.kts` 加：

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = "https://jitpack.io" }
    }
}
```

多模块项目优先使用这个坐标：

```gradle
dependencies {
    implementation "com.github.GitHub用户名.仓库名:core_base:v1.0.0"
}
```

注意：`v1.0.0` 要和 Git tag 完全一致。最终坐标以 JitPack 页面生成结果为准。

## 方式二：GitHub Packages 引用

### 发布方

已新增 GitHub Actions：

```text
.github/workflows/publish-core-base.yml
```

触发方式：

- 推送 `v*` tag，例如 `v1.0.0`。
- 在 GitHub Actions 页面手动运行 workflow，并输入版本号。

本地手动发布也可以执行：

```bash
./gradlew :core:core_base:publishReleasePublicationToGitHubPackagesRepository \
  "-PPOM_GROUP_ID=com.github.GitHub用户名" \
  "-PPOM_VERSION=1.0.0"
```

Windows PowerShell：

```powershell
.\gradlew.bat :core:core_base:publishReleasePublicationToGitHubPackagesRepository `
  "-PPOM_GROUP_ID=com.github.GitHub用户名" `
  "-PPOM_VERSION=1.0.0"
```

本地发布到 GitHub Packages 需要在本机 `~/.gradle/gradle.properties` 配置：

```properties
gpr.user=GitHub用户名
gpr.key=GitHub Personal Access Token
GITHUB_REPOSITORY=GitHub用户名/仓库名
```

不要把 token 提交到 Git。

### 使用方

使用方需要在 `settings.gradle` 加 GitHub Packages 仓库：

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/GitHub用户名/仓库名")
            credentials {
                username = providers.gradleProperty("gpr.user").get()
                password = providers.gradleProperty("gpr.key").get()
            }
        }
    }
}
```

依赖：

```gradle
dependencies {
    implementation "com.github.GitHub用户名:core_base:1.0.0"
}
```

## 本地发布验证

发布到本地 Maven：

```bash
./gradlew :core:core_base:publishReleasePublicationToMavenLocal "-PPOM_GROUP_ID=com.github.local" "-PPOM_VERSION=1.0.0"
```

使用方可临时加：

```gradle
repositories {
    mavenLocal()
}
```

然后引用：

```gradle
implementation "com.github.local:core_base:1.0.0"
```

## 混淆说明

`core_base` 已配置：

```gradle
consumerProguardFiles "consumer-rules.pro"
```

使用方通过 Maven/JitPack/GitHub Packages 引用时，consumer rules 会随 AAR 传递。正常开启 R8 不需要每个业务项目再单独写一份 `core_base` 专属混淆规则。

## 依赖暴露说明

`core_base` 的 public API 会直接暴露部分 AndroidX 类型，例如：

- `ViewModel`
- `ViewBinding`
- `Fragment`
- `RecyclerView.Adapter`
- `FragmentStateAdapter`
- `SmartRefreshLayout` 相关列表能力

因此发布配置中已把这些对外可见依赖改为 `api`，使用方只引入 `core_base` 就能正常编译。

## 版本建议

- 正式版本：`v1.0.0`、`v1.1.0`
- 修复版本：`v1.0.1`
- 不稳定版本：`v1.1.0-beta01`

公开给别人使用后，尽量不要随意改包名、类名、方法签名和资源名。
