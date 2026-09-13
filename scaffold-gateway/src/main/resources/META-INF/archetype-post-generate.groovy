// 生成后置脚本：把模板占位文件名 __gitignore__ 还原为 .gitignore。
//
// 背景：maven-resources-plugin 的 default excludes 含 **/.gitignore，
// 所以模板无法直接以 .gitignore 入库，只能用 __gitignore__ 占位；
// 又没有匹配的属性名可供模板引擎替换（__uAppName__ 那种才会被替换），
// 因此必须在生成结束后手动改名。
//
// 绑定说明：maven-archetype-plugin 的 post-generate Binding 只提供
// artifactId / groupId / version / package / request 等键（不含 outputDirectory 之类的目录键），
// 生成目录需从 request.getOutputDirectory() 取。

import java.nio.charset.StandardCharsets

def warn = { String message -> println "[archetype-post-generate] ${message}" }

/** 递归收集 __gitignore__ 占位文件，最多下钻两层；跳过 target 与隐藏目录。 */
def collectPlaceholders
collectPlaceholders = { File directory, int level, List<File> collected ->
    if (level > 2 || directory == null || !directory.isDirectory()) {
        return
    }
    File[] children = directory.listFiles()
    if (children == null) {
        return
    }
    for (File child : children) {
        if (child.isDirectory()) {
            if (!child.getName().startsWith('target') && !child.getName().startsWith('.')) {
                collectPlaceholders(child, level + 1, collected)
            }
        } else if (child.getName() == '__gitignore__') {
            collected.add(child)
        }
    }
}

try {
    def request = binding.hasVariable('request') ? binding.getVariable('request') : null

    Set<File> roots = new LinkedHashSet<File>()
    if (request != null) {
        // 注意：request.getOutputDirectory() 返回 String（不是 File），需自行转换
        Object outputDirectoryValue = request.getOutputDirectory()
        File outputDirectory = outputDirectoryValue == null
                ? null
                : (outputDirectoryValue instanceof File
                        ? (File) outputDirectoryValue
                        : new File(outputDirectoryValue.toString()))
        Object artifactId = request.getArtifactId()
        if (outputDirectory != null) {
            // 单模块工程直接生成在 outputDirectory；多模块生成在 outputDirectory/<artifactId>
            roots << outputDirectory
            if (artifactId != null && artifactId.toString()) {
                roots << new File(outputDirectory, artifactId.toString())
            }
        }
    }

    File renamedTo = null
    for (File root : roots) {
        if (renamedTo != null || root == null || !root.isDirectory()) {
            continue
        }
        // 深度 2 内查找占位文件；跳过 target 与隐藏目录，避免误伤构建产物
        List<File> candidates = new ArrayList<File>()
        collectPlaceholders(root, 0, candidates)
        for (File placeholder : candidates) {
            if (renamedTo != null) {
                break
            }
            File target = new File(placeholder.getParentFile(), '.gitignore')
            if (target.exists() && !target.delete()) {
                warn "已存在且无法覆盖：${target}"
                continue
            }
            if (placeholder.renameTo(target)) {
                renamedTo = target
            } else {
                // 跨文件系统 rename 可能失败，退回复制后删除
                target.setText(placeholder.getText(StandardCharsets.UTF_8.name()), StandardCharsets.UTF_8.name())
                if (placeholder.delete()) {
                    renamedTo = target
                }
            }
        }
    }

    if (renamedTo != null) {
        warn "__gitignore__ -> .gitignore：${renamedTo}"
    } else {
        warn "未找到 __gitignore__（工程的 .gitignore 可能缺失）"
    }
} catch (Exception exception) {
    // 后置脚本失败不应让生成中断，但必须留下明确痕迹
    warn "执行失败，生成工程可能缺少 .gitignore：${exception}"
}
