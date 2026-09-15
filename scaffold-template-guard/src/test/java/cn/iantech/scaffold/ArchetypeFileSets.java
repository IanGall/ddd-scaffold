package cn.iantech.scaffold;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 读取 archetype-metadata.xml 的 fileSet 定义，并提供与 archetype 一致的路径匹配语义。
 *
 * <p>两个护栏测试都需要「某个模板文件是否被某个 fileSet 命中」这一判断，因此把解析与 glob 匹配集中在这里，
 * 避免两份实现漂移：{@link ArchetypeResourcePlaceholderTest} 用它筛出未开启过滤的 fileSet，
 * {@link ArchetypeResourceCoverageTest} 用它检查是否有模板文件从未被任何 fileSet 命中。</p>
 */
final class ArchetypeFileSets {

    private ArchetypeFileSets() {
    }

    /**
     * 读取全部 fileSet（含各模块内部的），baseDir 统一为相对 {@code archetype-resources} 的路径前缀。
     */
    static List<FileSet> readAll(Path metadataFile) throws Exception {
        Document metadata = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(metadataFile.toFile());
        List<FileSet> fileSets = new ArrayList<>();
        collect(metadata.getDocumentElement(), "", fileSets);
        return fileSets;
    }

    /** 递归收集 fileSet；modules 可以嵌套，因此模块目录逐层拼接。 */
    private static void collect(Element parent, String parentDir, List<FileSet> target) {
        for (Element fileSets : children(parent, "fileSets")) {
            for (Element fileSet : children(fileSets, "fileSet")) {
                String baseDir = join(parentDir, childText(fileSet, "directory"));
                boolean filtered = Boolean.parseBoolean(fileSet.getAttribute("filtered"));
                boolean packaged = Boolean.parseBoolean(fileSet.getAttribute("packaged"));
                target.add(new FileSet(baseDir, includes(fileSet), filtered, packaged));
            }
        }
        for (Element modules : children(parent, "modules")) {
            for (Element module : children(modules, "module")) {
                collect(module, join(parentDir, module.getAttribute("dir")), target);
            }
        }
    }

    private static List<String> includes(Element fileSet) {
        List<String> includes = new ArrayList<>();
        for (Element includesElement : children(fileSet, "includes")) {
            for (Element include : children(includesElement, "include")) {
                includes.add(include.getTextContent().trim());
            }
        }
        return includes;
    }

    private static String childText(Element parent, String tagName) {
        List<Element> elements = children(parent, tagName);
        return elements.isEmpty() ? "" : elements.getFirst().getTextContent().trim();
    }

    private static String join(String left, String right) {
        if (left.isEmpty()) {
            return right;
        }
        return right.isEmpty() ? left : left + "/" + right;
    }

    private static List<Element> children(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && tagName.equals(node.getNodeName())) {
                result.add((Element) node);
            }
        }
        return result;
    }

    /** 单个 fileSet：baseDir 相对 archetype-resources，includes 为 archetype（Ant 风格）glob 表达式。 */
    record FileSet(String baseDir, List<String> includes, boolean filtered, boolean packaged) {

        boolean matches(String relativeToArchetypeRoot) {
            String prefix = baseDir.isEmpty() ? "" : baseDir + "/";
            if (!relativeToArchetypeRoot.startsWith(prefix)) {
                return false;
            }
            String relative = relativeToArchetypeRoot.substring(prefix.length());
            return includes.stream().anyMatch(include -> globToPattern(include).matcher(relative).matches());
        }

        /** 将 archetype glob 转为正则：** 跨目录，* 限单层，? 单字符。 */
        private static Pattern globToPattern(String glob) {
            StringBuilder regex = new StringBuilder();
            for (int i = 0; i < glob.length(); i++) {
                char current = glob.charAt(i);
                if (current == '*') {
                    boolean doubleStar = i + 1 < glob.length() && glob.charAt(i + 1) == '*';
                    if (doubleStar) {
                        i++;
                        if (i + 1 < glob.length() && glob.charAt(i + 1) == '/') {
                            i++;
                            regex.append("(?:.*/)?");
                        } else {
                            regex.append(".*");
                        }
                    } else {
                        regex.append("[^/]*");
                    }
                } else if (current == '?') {
                    regex.append("[^/]");
                } else {
                    if ("\\.[]{}()+-^$|".indexOf(current) >= 0) {
                        regex.append('\\');
                    }
                    regex.append(current);
                }
            }
            return Pattern.compile(regex.toString());
        }
    }
}
