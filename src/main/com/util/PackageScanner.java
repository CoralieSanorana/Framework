package main.com.util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PackageScanner {

    // entree
    public static List<Class<?>> findByPackage(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();

        String path = packageName.replace('.', '/');

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);

        if (resource == null) {
            return classes;
        }

        File directory = new File(resource.toURI());

        // parcourir
        for (File file : findAll(directory)) {
            String absolutePath = file.getAbsolutePath().replace('\\', '/');
            int idx = absolutePath.indexOf(path);
            if (idx != -1) {
                String classSubPath = absolutePath.substring(idx);
                String className = classSubPath.substring(0, classSubPath.length() - 6).replace('/', '.');
                classes.add(Class.forName(className));
            }
        }

        return classes;
    }

    private static List<File> findAll(File directory) {
        List<File> result = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files == null)
            return result;

        for (File file : files) {
            if (file.isDirectory()) {
                result.addAll(findAll(file));
            } else if (file.getName().endsWith(".class")) {
                result.add(file);
            }
        }
        return result;
    }
}