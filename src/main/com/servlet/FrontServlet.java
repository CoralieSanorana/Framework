package main.com.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;
import com.google.gson.Gson;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import main.com.annotation.*;
import main.com.util.*;

public class FrontServlet extends HttpServlet {

    private Map<VerbUrl, Mapping> urlMappingMap;

    
    private String prefixe;
    private String suffixe;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

       
        ServletContext servletContext = config.getServletContext();
        Object attribute = servletContext.getAttribute(
            FrameworkContextListener.MAPPING_MAP_ATTRIBUTE
        );

        if (attribute instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<VerbUrl, Mapping> sharedMap = (Map<VerbUrl, Mapping>) attribute;
            this.urlMappingMap = sharedMap;
        } else {
            this.urlMappingMap = new HashMap<>();
            String packageToScan = config.getInitParameter("package_controllers");
            MappingInitializer initializer = new MappingInitializer();
            initializer.initializeMappings(packageToScan, this.urlMappingMap);
        }

        this.prefixe = config.getInitParameter("prefixe");
        this.suffixe = config.getInitParameter("suffixe");

        System.out.println("[Framework] prefixe = " + prefixe);
        System.out.println("[Framework] suffixe = " + suffixe);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String httpMethod   = request.getMethod().toUpperCase();
        String contextPath  = request.getContextPath();
        String requestedUrl = request.getRequestURI().substring(contextPath.length());

        if (requestedUrl.equals("/") || requestedUrl.isEmpty()) {
            response.setContentType("application/json;charset=UTF-8");
            Map<String, String> mappings = new HashMap<>();
            for (VerbUrl cle : urlMappingMap.keySet()) {
                mappings.put(cle.toString(), urlMappingMap.get(cle).toString());
            }
            Gson gson = new Gson();
            out.print(gson.toJson(mappings));
            return;
        }

        VerbUrl cle = new VerbUrl(requestedUrl, httpMethod);

        if (urlMappingMap.containsKey(cle)) {
            Mapping mapping = urlMappingMap.get(cle);

            try {
                Class<?> laClasse  = Class.forName(mapping.getClassName());
                Object   instance  = laClasse.getDeclaredConstructor().newInstance();
                Method   laMethode = laClasse.getDeclaredMethod(mapping.getMethodName());

                // Vérifier si la méthode a l'annotation @RestAPI
                boolean isRestAPI = laMethode.isAnnotationPresent(RestAPI.class);

                Object resultat = laMethode.invoke(instance);

                if (isRestAPI) {
                    // Mode API REST - retourner du JSON
                    response.setContentType("application/json;charset=UTF-8");
                    
                    if (resultat instanceof String) {
                        // Si c'est une String, écrire directement
                        out.print(resultat);
                    } else {
                        // Sinon, convertir en JSON avec Gson
                        Gson gson = new Gson();
                        String json = gson.toJson(resultat);
                        out.print(json);
                    }
                } else if (resultat instanceof ModelAndView) {
                    // Mode classique - forward vers JSP
                    ModelAndView mv = (ModelAndView) resultat;
                    String cheminJsp = prefixe + mv.getUrl() + suffixe;
                    if (!cheminJsp.startsWith("/")) {
                        cheminJsp = "/" + cheminJsp;
                    }

                    for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }

                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(cheminJsp);
                    dispatcher.forward(request, response);

                } else {
                    out.println("Methode executee. (pas de ModelAndView retourne)");
                }

            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Erreur : " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        out.println("=== URL non supportee ===");
        out.println("Demandee : " + cle);
        out.println("");
        out.println("URLs disponibles :");
        for (VerbUrl k : urlMappingMap.keySet()) {
            out.println("  " + k);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}