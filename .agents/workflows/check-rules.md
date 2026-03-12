---
description: 检查项目是否符合命名规范、资源规则和架构约束
---

使用此 workflow 对项目进行全面规范审查，生成合规报告。

**使用方式**：直接触发此 workflow，我会扫描整个项目并生成报告。
> "/check-rules"

### Step 1: 检查 Layout 文件命名
扫描 `res/layout/` 目录，检查所有文件是否以正确前缀开头：
- `activity_*`、`fragment_*`、`view_*`、`item_*`、`dialog_*`

// turbo
```bash
dir /b app\src\main\res\layout\
```

### Step 2: 检查字符串资源是否硬编码
// turbo
```bash
./gradlew lint --check HardcodedText
```

### Step 3: 检查依赖版本管理
扫描 `build.gradle` 文件，确认没有硬编码版本号（所有库应通过 `libs.*` 引用）。

// turbo
```bash
grep -rn "implementation \"" app\build.gradle lib_base\build.gradle
```

### Step 4: 运行 Lint 静态分析
// turbo
```bash
./gradlew lint
```

### Step 5: 运行 Detekt 代码检查
// turbo
```bash
./gradlew detekt
```

### Step 6: 生成报告
我会汇总所有发现的问题并生成一份合规报告，包括：
- ❌ 不符合规范的文件列表
- ⚠️ 潜在风险
- ✅ 修复建议
