---
description: 提取最新代码、解决冲突并提交中文说明
---

这个工作流会自动拉取远端代码，检查是否有冲突。如果你需要解决冲突，请在 IDE 里解决完毕后再继续。随后再将你的修改通过标准的中文说明进行提交。

// turbo
1. 第一步：保存当前的未提交修改 (stash) 避免拉取时覆盖代码。
```bash
git stash
```

// turbo
2. 第二步：拉取最新代码。
```bash
git pull origin main
```
> **注意**：如果你们的主分支不是 `main`（比如是 `master` 或 `dev`），请自行修改上述命令。

// turbo
3. 第三步：恢复你刚才的修改 (stash pop)。
如果有文件冲突（Conflict），此时 Git 会提示状态为 `Unmerged`。
```bash
git stash pop
```

> [!IMPORTANT]
> **请在此立刻检查冲突！**
> 如果有冲突（例如 `Auto-merging` 失败，出现 `CONFLICT`），请打开 Android Studio，在含有冲突的文件中手动合并代码。
> 合并并测试无误后，再执行后续步骤。如果没有冲突，请直接继续。

// turbo
4. 第四步：添加所有变更到暂存区。
```bash
git add .
```

5. 第五步：进行提交（使用中文说明）。
请将下方 `[你的提交说明]` 替换成具体的说明，例如 "feat: 新增首页列表功能" 或 "fix: 修复登录崩溃问题" 等。
```bash
git commit -m "[你的提交说明]"
```

6. 第六步：推送到远端仓库。
```bash
git push origin main
```
