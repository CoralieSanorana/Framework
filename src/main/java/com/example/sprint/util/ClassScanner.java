package com.example.sprint.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;
import com.example.sprint.util.Mapping;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import com.example.sprint.util.UrlMethod;


public class ClassScanner {

    /**
     * Charge les classes depuis les packages spécifiés qui ont l'annotation donnée
     * 
     * @param packages Liste des noms de packages à scanner
     * @param annotationName Nom complet de l'annotation à rechercher
     * @return Liste des noms complets des classes qui ont l'annotation
     */
    public static Map<UrlMethod,Mapping> chargement_classe(List<String> packages, String annotationName) {
        Map<UrlMethod, Mapping> controllerClasses = new HashMap<>();
        
        try {
            // 1. On charge la classe de l'annotation pour les classes (ex: @Controller)
            Class<? extends Annotation> classAnnotation = Class.forName(annotationName).asSubclass(Annotation.class);
            
            // 2. On charge la classe de l'annotation pour les méthodes (ex: @Url)
            Class<? extends Annotation> methodAnnotation = Class.forName("mg.itu.annotation.Url").asSubclass(Annotation.class);
            
            for (String packageName : packages) {
                try {
                    List<String> classes = getClasses(packageName);
                    for (String className : classes) {
                        try {
                            Class<?> clazz = Class.forName(className);
                            
                            // Vérifier si la classe a l'annotation de classe (ex: @Controller)
                            if (clazz.isAnnotationPresent(classAnnotation)) {
                                Method[] methodes = clazz.getDeclaredMethods();
                                for(Method m: methodes){
                                    // Chercher les méthodes avec l'annotation de méthode (ex: @Url)
                                    Annotation annot = m.getAnnotation(methodAnnotation);
                                    if (annot != null) {
                                        Method valueMethod = annot.annotationType().getMethod("value");
                                        String url_mapping = (String) valueMethod.invoke(annot);
                                        Method methodAttr = annot.annotationType().getMethod("method");
                                        String httpMethod = (String) methodAttr.invoke(annot);

                                        UrlMethod key = new UrlMethod(url_mapping, httpMethod.toUpperCase());
    
                                        if (controllerClasses.containsKey(key)) {
                                            Mapping existingMapping = controllerClasses.get(key);
                                            throw new Exception("Doublon de mapping détecté ! L'URL '" + url_mapping + "' " +
                                                                "avec la méthode '" + httpMethod + "' est déjà associée à la méthode : " +
                                                                existingMapping.getClazz().getName() + "." + existingMapping.getMethode().getName() + "()");
                                        }
                                        
                                        Mapping mapping = new Mapping(clazz, m, url_mapping, httpMethod);
                                        controllerClasses.put(new UrlMethod(url_mapping, httpMethod), mapping);
                                    }
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            System.err.println("Classe non trouvée: " + className);
                        } catch (Exception e) {
                            System.err.println("Erreur lors de la lecture des méthodes de " + className + ": " + e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Erreur lors du scan du package " + packageName + ": " + e.getMessage());
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Annotation introuvable : " + e.getMessage());
        } catch (ClassCastException e) {
            System.err.println("Le nom fourni n'est pas une annotation");
        }
        
        return controllerClasses;
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
