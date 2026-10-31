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
    private Map<String, Object> classMap;
    private Binder binder;
    private String prefixe;
    private String suffixe;
    private String authorisationKey;

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
        this.authorisationKey = config.getInitParameter("authorisation");
        this.classMap = new HashMap<>();

        System.out.println("[Framework] prefixe = " + prefixe);
        System.out.println("[Framework] suffixe = " + suffixe);
        System.out.println("[Framework] authorisation key = " + authorisationKey);
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
                Object   instance  = construireController(mapping.getClassName());
                Method   laMethode = trouverMethode(laClasse, mapping.getMethodName());

                // Gestion des authorisations
                if (laMethode.isAnnotationPresent(Autorisation.class)) {
                    Autorisation authAnnotation = laMethode.getAnnotation(Autorisation.class);
                    String[] rolesAutorises = authAnnotation.value(); 

                    MySession sessionCourante = new MySession(request.getSession());

                    // Verifier si un utilisateur est connecte
                    if (this.authorisationKey == null || this.authorisationKey.trim().isEmpty()) {
                        throw new ServletException("Parametre d'init 'authorisation' manquant dans web.xml");
                    }

                    Object roleUserObj = sessionCourante.get(this.authorisationKey);

                    // Si la cle est absente de la session
                    if (roleUserObj == null) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                        out.println("Erreur : Acces refuse. Veuillez vous connecter.");
                        return;
                    }

                    String roleUser = roleUserObj.toString();

                    // Si des roles specifiques sont demandes, on verifie si le role de l'utilisateur y figure
                    if (rolesAutorises != null && rolesAutorises.length > 0) {
                        boolean estAutorise = false;
                        for (String role : rolesAutorises) {
                            if (role.equalsIgnoreCase(roleUser)) {
                                estAutorise = true;
                                break;
                            }
                        }

                        if (!estAutorise) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
                            out.println("Erreur : Acces interdit. Votre rele (" + roleUser + ") n'est pas autorise pour cette ressource.");
                            return;
                        }
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

    private Object construireController(String className) throws Exception {

        if (classMap.containsKey(className)) {
            return classMap.get(className);
        } else {
            Class<?> laClasse = Class.forName(className);
            Object instance = laClasse.getDeclaredConstructor().newInstance();
            classMap.put(className, instance);
            return instance;
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