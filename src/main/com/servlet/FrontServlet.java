package main.com.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.HashMap;
import com.google.gson.Gson;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.com.annotation.*;
import main.com.util.*;

public class FrontServlet extends HttpServlet {

    private Map<VerbUrl, Mapping> urlMappingMap;
    private Binder binder;
    private String prefixe;
    private String suffixe;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.binder = new Binder();
       
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
                Method   laMethode = trouverMethode(laClasse, mapping.getMethodName());

                // Gestion des authorisations
                if (laMethode.isAnnotationPresent(Autorisation.class)) {
                    Autorisation authAnnotation = laMethode.getAnnotation(Autorisation.class);
                    String accesRequis = authAnnotation.value(); 

                    // Recuperation de la session de l'utilisateur via MySession
                    MySession sessionCourante = new MySession(request.getSession());
                    Object accesUserObj = sessionCourante.get("acces");

                    // Verifier si l'utilisateur est connecte
                    if (accesUserObj == null) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); 
                        out.println("Erreur : Acces refuse. Veuillez vous connecter.");
                        return;
                    }

                    String accesUser = accesUserObj.toString();

                    // Verifier si l'utilisateur a le niveau d'acces requis
                    if (!accesRequis.isEmpty() && !accesUser.equalsIgnoreCase(accesRequis)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        out.println("Erreur : Acces interdit. Role requis : " + accesRequis + " (Votre role : " + accesUser + ")");
                        return;
                    }
                }

                // Verifier si la methode a l'annotation @RestAPI
                boolean isRestAPI = laMethode.isAnnotationPresent(RestAPI.class);

                Object resultat = laMethode.invoke(instance, this.binder.construireArguments(laMethode, request));

                if (isRestAPI) {
                    // Mode API REST - retourner du JSON
                    response.setContentType("application/json;charset=UTF-8");
                    
                    if (resultat instanceof String) {
                        out.print(resultat);
                    } else {
                        Gson gson = new Gson();
                        String json = gson.toJson(resultat);
                        out.print(json);
                    }
                } else if (resultat instanceof ModelAndView) {
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
                    out.println("Methode executee(Pas ModelAndView/RestApi) = "+resultat);
                }

            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Erreur : " + e.getMessage());
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

    private Method trouverMethode(Class<?> classe, String nomMethode)
            throws NoSuchMethodException {
        for (Method methode : classe.getDeclaredMethods()) {
            if (methode.getName().equals(nomMethode)) {
                return methode;
            }
        }
        throw new NoSuchMethodException("Methode introuvable : " + nomMethode);
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