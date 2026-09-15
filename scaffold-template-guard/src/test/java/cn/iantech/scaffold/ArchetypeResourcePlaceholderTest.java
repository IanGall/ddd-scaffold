package cn.iantech.scaffold;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 脚手架模板护栏：archetype-metadata.xml 中未开启 Velocity 过滤（filtered 非 true）的 fileSet
 * 不会被变量替换，其下文件不得出现 ${symbol_dollar} 等占位符，否则生成工程会残留字面量。
 *
 * <p>历史缺陷：{@code sharding/**&#47;*.yaml} 未开启过滤却使用了占位符，生成的
 * {@code sharding-jdbc-dev.yaml} 账号/口令变成字面量，导致生成工程连库失败。</p>
 *
 * <p>放在独立 jar 模块中的原因：{@code maven-archetype} 打包的生命周期不含 compile/test 阶段，
 * 无法直接在 archetype 模块内运行单元测试。</p>
 */
class ArchetypeResourcePlaceholderTest {

    private static final String SYMBOL_DOLLAR = "${symbol_dollar}";

    /** 需要护栏的脚手架工程目录名（相对 ddd-scaffold 根）。 */
    private static final List<String> ARCHETYPE_MODULES = List.of("scaffold-std", "scaffold-gateway");

    @Test
    void unfilteredFileSetsMustNotContainVelocityPlaceholders() throws Exception {
        Path workspace = Path.of("..").toAbsolutePath().normalize();
        List<String> violations = new ArrayList<>();
        int checkedModules = 0;

        for (String module : ARCHETYPE_MODULES) {
            Path resources = workspace.resolve(module).resolve("src/main/resources");
            Path metadataFile = resources.resolve("META-INF/maven/archetype-metadata.xml");
            if (!Files.isRegularFile(metadataFile)) {
                continue;
            }
            checkedModules++;
            violations.addAll(scanUnfilteredFileSets(module, resources, metadataFile));
        }

        assertTrue(checkedModules > 0,
                "未在 " + workspace + " 下找到任何 archetype-metadata.xml，模板护栏未生效");
        assertTrue(violations.isEmpty(), "以下未开启 Velocity 过滤的模板文件包含 " + SYMBOL_DOLLAR
                + "，生成工程会残留字面量；请改为直接书写字面量，或在 archetype-metadata.xml 中开启 filtered："
                + System.lineSeparator() + String.join(System.lineSeparator(), violations));
    }

    private static List<String> scanUnfilteredFileSets(String module, Path resources, Path metadataFile) throws Exception {
        Path archetypeRoot = resources.resolve("archetype-resources");
        if (!Files.isDirectory(archetypeRoot)) {
            return List.of();
        }
        List<ArchetypeFileSets.FileSet> unfilteredFileSets = ArchetypeFileSets.readAll(metadataFile).stream()
                .filter(fileSet -> !fileSet.filtered())
                .toList();

        List<String> violations = new ArrayList<>();
        for (ArchetypeFileSets.FileSet fileSet : unfilteredFileSets) {
            Path base = archetypeRoot.resolve(fileSet.baseDir());
            if (!Files.isDirectory(base)) {
                continue;
            }
            List<Path> files;
            try (Stream<Path> walk = Files.walk(base)) {
                files = walk.filter(Files::isRegularFile).toList();
            }
            for (Path file : files) {
                String relative = archetypeRoot.relativize(file).toString().replace('\\', '/');
                if (fileSet.matches(relative) && Files.readString(file, StandardCharsets.UTF_8).contains(SYMBOL_DOLLAR)) {
                    violations.add(module + "/archetype-resources/" + relative);
                }
            }
        }
        return violations;
    }
}
