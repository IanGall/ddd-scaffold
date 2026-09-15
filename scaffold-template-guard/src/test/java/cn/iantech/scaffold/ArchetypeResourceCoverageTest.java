package cn.iantech.scaffold;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 脚手架模板覆盖率护栏：archetype-resources 下的每个模板文件都必须被 archetype-metadata.xml 中某个 fileSet 命中。
 *
 * <p>历史缺陷：未被任何 fileSet 匹配的模板文件不会进入生成工程，而且**没有任何提示**。
 * {@code shardingsphere} 的分片配置就曾因为 include 只写了 {@code **&#47;*.yml}（实际是 .yaml）而从未生成，
 * 直到生成工程连库失败才被发现。</p>
 *
 * <p>本类的第二条断言针对现有护栏的盲区：{@link ArchetypeResourcePlaceholderTest} 只检测
 * {@code ${symbol_dollar}}，因此「忘了给新的 YAML fileSet 打开 filtered」会让模板变量以字面量形式残留在生成物里，
 * 而既有护栏完全看不见。这里直接断言包含 {@code *.yaml} 的 fileSet 必须开启过滤。</p>
 */
class ArchetypeResourceCoverageTest {

    /** 需要护栏的脚手架工程目录名（相对 ddd-scaffold 根）。 */
    private static final List<String> ARCHETYPE_MODULES = List.of("scaffold-std", "scaffold-gateway");

    /**
     * 模块 pom.xml 由 maven-archetype 插件隐式处理（不需要也不应该出现在 fileSet 的 includes 里），
     * 因此不在覆盖率检查范围内。
     */
    private static final String IMPLICIT_POM = "pom.xml";

    @Test
    void everyTemplateFileMustBeMatchedBySomeFileSet() throws Exception {
        Path workspace = Path.of("..").toAbsolutePath().normalize();
        List<String> violations = new ArrayList<>();
        int checkedModules = 0;

        for (String module : ARCHETYPE_MODULES) {
            Path archetypeRoot = workspace.resolve(module).resolve("src/main/resources/archetype-resources");
            Path metadataFile = workspace.resolve(module).resolve("src/main/resources/META-INF/maven/archetype-metadata.xml");
            if (!Files.isDirectory(archetypeRoot) || !Files.isRegularFile(metadataFile)) {
                continue;
            }
            checkedModules++;
            violations.addAll(findUnmatchedFiles(module, archetypeRoot, metadataFile));
        }

        assertTrue(checkedModules > 0,
                "未在 " + workspace + " 下找到任何 archetype-resources，模板护栏未生效");
        assertTrue(violations.isEmpty(), "以下模板文件没有被 archetype-metadata.xml 的任何 fileSet 命中，"
                + "生成工程不会包含它们（新增文件类型时必须同步补 include）："
                + System.lineSeparator() + String.join(System.lineSeparator(), violations));
    }

    @Test
    void yamlFileSetsMustEnableVelocityFiltering() throws Exception {
        Path workspace = Path.of("..").toAbsolutePath().normalize();
        List<String> violations = new ArrayList<>();
        int checkedModules = 0;

        for (String module : ARCHETYPE_MODULES) {
            Path metadataFile = workspace.resolve(module).resolve("src/main/resources/META-INF/maven/archetype-metadata.xml");
            if (!Files.isRegularFile(metadataFile)) {
                continue;
            }
            checkedModules++;
            for (ArchetypeFileSets.FileSet fileSet : ArchetypeFileSets.readAll(metadataFile)) {
                boolean matchesYaml = fileSet.includes().stream().anyMatch(include -> include.endsWith(".yaml"));
                if (matchesYaml && !fileSet.filtered()) {
                    violations.add(module + " 的 fileSet（目录 " + fileSet.baseDir() + "，include "
                            + fileSet.includes() + "）未开启 filtered");
                }
            }
        }

        assertTrue(checkedModules > 0,
                "未在 " + workspace + " 下找到任何 archetype-metadata.xml，模板护栏未生效");
        assertTrue(violations.isEmpty(), "以下 fileSet 会生成 YAML 但没有开启 Velocity 过滤，"
                + "生成物里会残留 ${...} 字面量、kubectl apply 直接失败（既有占位符护栏抓不到这种残留）："
                + System.lineSeparator() + String.join(System.lineSeparator(), violations));
    }

    private static List<String> findUnmatchedFiles(String module, Path archetypeRoot, Path metadataFile) throws Exception {
        List<ArchetypeFileSets.FileSet> fileSets = ArchetypeFileSets.readAll(metadataFile);
        List<String> violations = new ArrayList<>();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(archetypeRoot)) {
            files = walk.filter(Files::isRegularFile).toList();
        }
        for (Path file : files) {
            String relative = archetypeRoot.relativize(file).toString().replace('\\', '/');
            if (relative.endsWith(IMPLICIT_POM)) {
                continue;
            }
            boolean matched = fileSets.stream().anyMatch(fileSet -> fileSet.matches(relative));
            if (!matched) {
                violations.add(module + "/archetype-resources/" + relative);
            }
        }
        return violations;
    }
}
