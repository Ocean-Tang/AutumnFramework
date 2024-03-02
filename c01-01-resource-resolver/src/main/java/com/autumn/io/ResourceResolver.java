package com.autumn.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

/**
 * @author huangcanjie
 */
public class ResourceResolver {

    Logger log = LoggerFactory.getLogger(getClass());

    String basePackage;

    public static void main(String[] args) {
        new ResourceResolver("lombok").scan(r -> (Object)r );
    }

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    public <R> List<R> scan(Function<Resource, R> mapper) {
        // 将包路径转化为文件路径
        String basePackagePath = this.basePackage.replace('.', '/');
        String path = basePackagePath;

        try {
            // 扫描路径，并将资源放置到集合中，mapper的作用为将对应的 Resource，转换为指定的参数R 类型
            List<R> collector = new ArrayList<>();
            scan0(basePackagePath, path, collector, mapper);
            return collector;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    <R> void scan0(String basePackagePath, String path, List<R> collector, Function<Resource, R> mapper) throws IOException, URISyntaxException {
        log.debug("scan path:{}", path);

        // 获取类加载器，扫描 path 路径下的资源
        Enumeration<URL> enumeration = getContextClassLoader().getResources(path);
        while (enumeration.hasMoreElements()) {
            URL url = enumeration.nextElement();
            URI uri = url.toURI();
            // 删除尾部斜杆
            String uriStr = removeTrailingSlash(uriToString(uri));
            // 去掉资源名，得到资源所在的路径
            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());

            // 如果在 jar 包下，得到的路径为 jar:file:...
            if (uriBaseStr.startsWith("file:")) {
                uriBaseStr = uriBaseStr.substring(5);
            }
            if (uriStr.startsWith("jar:")) {
                scanFile(true, uriBaseStr, jarUriToPath(basePackagePath, uri), collector, mapper);
            } else {
                scanFile(false, uriBaseStr, Paths.get(uri), collector, mapper);
            }
        }
    }

    <R> void scanFile(boolean isJar, String uriBaseStr, Path root, List<R> collector, Function<Resource,R> mapper) throws IOException {
        String baseDir = removeTrailingSlash(uriBaseStr);
        Files.walk(root).filter(Files::isRegularFile).forEach(file -> {
            Resource resource = null;
            if (isJar) {
                // 如果是 jar 包中的资源，直接新建 Resource 类
                resource = new Resource(baseDir, removeLeadingSlash(file.toString()));
            } else {
                // 普通路径下的资源， 给路径前缀加上 file:
                String path = file.toString();
                String name = removeLeadingSlash(path.substring(baseDir.length()));
                resource = new Resource("file:" + path, name);
            }
            log.debug("find resource: {}", resource);
            // 将resource 转换为 R 参数类型
            R r = mapper.apply(resource);
            if (r != null) {
                collector.add(r);
            }
        });
    }

    String removeLeadingSlash(String s) {
        if (s.startsWith("/") || s.startsWith("\\")) {
            s = s.substring(1);
        }
        return s;
    }

    Path jarUriToPath(String basePackagePath, URI uri) throws IOException {
        return FileSystems.newFileSystem(uri, new HashMap<>()).getPath(basePackagePath);
    }

    String removeTrailingSlash(String s) {
        if (s.endsWith("/") || s.endsWith("\\")) {
            s = s.substring(0, s.length()-1);
        }
        return s;
    }

    String uriToString(URI uri) {
        try {
            return URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    ClassLoader getContextClassLoader() {
        // 从当前上下文中获取类加载器，因为类资源可能在web容器的 /WEB-INF/ 下，获取不到，再从当前Class获取
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        return classLoader;
    }
}
