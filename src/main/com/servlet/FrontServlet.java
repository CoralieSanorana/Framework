package main.com.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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
                Method   laMethode = trouverMethode(laClasse, mapping.getMethodName());

                // Vérifier si la méthode a l'annotation @RestAPI
                boolean isRestAPI = laMethode.isAnnotationPresent(RestAPI.class);

                Object resultat = laMethode.invoke(instance, construireArguments(laMethode, request));

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

    private Object[] construireArguments(Method methode, HttpServletRequest request) {
        Parameter[] parametres = methode.getParameters();
        Object[] arguments = new Object[parametres.length];

        for (int i = 0; i < parametres.length; i++) {
            Parameter parametre = parametres[i];
            if (!parametre.isNamePresent()) {
                throw new IllegalArgumentException(
                        "Les noms des parametres de " + methode.getName()
                                + " ne sont pas disponibles. Compilez avec -parameters.");
            }

            String valeur = request.getParameter(parametre.getName());
            if (valeur == null) {
                throw new IllegalArgumentException(
                        "Parametre requis manquant : " + parametre.getName());
            }
            arguments[i] = convertirArgument(valeur, parametre.getType(), parametre.getName());
        }
        return arguments;
    }

    private Object convertirArgument(String valeur, Class<?> type, String nomParametre) {
        try {
            if (type == String.class) return valeur;
            if (type == int.class || type == Integer.class) return Integer.valueOf(valeur);
            if (type == long.class || type == Long.class) return Long.valueOf(valeur);
            if (type == double.class || type == Double.class) return Double.valueOf(valeur);
            if (type == float.class || type == Float.class) return Float.valueOf(valeur);
            if (type == short.class || type == Short.class) return Short.valueOf(valeur);
            if (type == byte.class || type == Byte.class) return Byte.valueOf(valeur);
            if (type == boolean.class || type == Boolean.class) return Boolean.valueOf(valeur);
            if (type == char.class || type == Character.class) {
                if (valeur.length() != 1) {
                    throw new IllegalArgumentException(
                            "Le parametre " + nomParametre + " doit contenir un seul caractere.");
                }
                return valeur.charAt(0);
            }
            if (type.isEnum()) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                Object valeurEnum = Enum.valueOf((Class<? extends Enum>) type, valeur);
                return valeurEnum;
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Valeur invalide pour le parametre " + nomParametre + " : " + valeur, e);
        }

        throw new IllegalArgumentException(
                "Type de parametre non supporte pour " + nomParametre + " : " + type.getName());
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