package main.com.util;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.util.HashMap;
import java.util.Map;

public class FrameworkContextListener implements ServletContextListener {

    public static final String MAPPING_MAP_ATTRIBUTE = "framework.urlMappingMap";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();

        String packageToScan = context.getInitParameter("package_controllers");

        Map<VerbUrl, Mapping> mappings = new HashMap<>();
        MappingInitializer initializer = new MappingInitializer();

        try {
            initializer.initializeMappings(packageToScan, mappings);
            context.setAttribute(MAPPING_MAP_ATTRIBUTE, mappings);
            System.out.println("[framework] mappings charges au demarrage de l'application.");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement des mappings au demarrage", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        event.getServletContext().removeAttribute(MAPPING_MAP_ATTRIBUTE);
    }
}