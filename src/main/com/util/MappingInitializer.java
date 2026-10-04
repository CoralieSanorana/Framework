package main.com.util;

import main.com.annotation.*;

import jakarta.servlet.ServletException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class MappingInitializer {

    public void initializeMappings(String packageToScan, Map<VerbUrl, Mapping> mappings)
            throws ServletException {

        if (mappings == null) {
            throw new IllegalArgumentException("La map de mappings ne peut pas etre null.");
        }

        if (packageToScan == null || packageToScan.trim().isEmpty()) {
            throw new ServletException("Parametre 'package_controllers' manquant dans web.xml");
        }

        try {
            List<Class<?>> allClasses = PackageScanner.findByPackage(packageToScan);

            for (Class<?> cls : allClasses) {
                if (!cls.isAnnotationPresent(Controller.class)) {
                    continue;
                }

                for (Method method : cls.getDeclaredMethods()) {
                    if (!method.isAnnotationPresent(UrlMapping.class)) {
                        continue;
                    }

                    UrlMapping annotation = method.getAnnotation(UrlMapping.class);
                    String url = annotation.value();
                    String httpMethod = annotation.method();

                    VerbUrl key = new VerbUrl(url, httpMethod);

                    if (mappings.containsKey(key)) {
                        throw new ServletException("Conflit : " + key + " est declaree deux fois.");
                    }

                    mappings.put(key, new Mapping(cls.getName(), method.getName()));

                    System.out.println("[framework] enregistre : "
                            + key + " -> "
                            + cls.getSimpleName() + "." + method.getName() + "()");
                }
            }

            System.out.println("[framework] " + mappings.size() + " mapping(s) charge(s).");

        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des controllers", e);
        }
    }
}