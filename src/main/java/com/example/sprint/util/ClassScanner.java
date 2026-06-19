package com.example.sprint.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClassScanner {

    /**
     * Charge les classes depuis les packages spécifiés qui ont l'annotation donnée
     * 
     * @param packages Liste des noms de packages à scanner
     * @param annotationName Nom complet de l'annotation à rechercher
     * @return Liste des noms complets des classes qui ont l'annotation
     */
    public static List<String> chargement_classe(List<String> packages, String annotationName) {
        List<String> classesWithAnnotation = new ArrayList<>();
        
        for (String packageName : packages) {
            try {
                List<String> classes = getClasses(packageName);
                for (String className : classes) {
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (hasAnnotation(clazz, annotationName)) {
                            classesWithAnnotation.add(className);
                        }
                    } catch (ClassNotFoundException e) {
                        System.err.println("Classe non trouvée: " + className);
                    }
                }
            } catch (IOException e) {
                System.err.println("Erreur lors du scan du package " + packageName + ": " + e.getMessage());
            }
        }
        
        return classesWithAnnotation;
    }
    
    /**
     * Récupère toutes les classes d'un package
     */
    private static List<String> getClasses(String packageName) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        
        List<String> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        
        return classes;
    }
    
    /**
     * Trouve récursivement toutes les classes dans un répertoire
     */
    private static List<String> findClasses(File directory, String packageName) {
        List<String> classes = new ArrayList<>();
        
        if (!directory.exists()) {
            return classes;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                classes.add(className);
            }
        }
        
        return classes;
    }
    
    /**
     * Vérifie si une classe a l'annotation spécifiée
     */
    private static boolean hasAnnotation(Class<?> clazz, String annotationName) {
        try {
            Class<? extends Annotation> annotationClass = Class.forName(annotationName).asSubclass(Annotation.class);
            return clazz.isAnnotationPresent(annotationClass);
        } catch (ClassNotFoundException e) {
            System.err.println("Annotation non trouvée: " + annotationName);
            return false;
        } catch (ClassCastException e) {
            System.err.println("Le type n'est pas une annotation: " + annotationName);
            return false;
        }
    }
}
