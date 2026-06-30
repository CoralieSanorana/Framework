package main.java.com.example.sprint.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.example.sprint.util.ClassScanner;
import com.example.sprint.util.Mapping;
import java.lang.reflect.Method;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Enumeration;
import com.example.sprint.util.UrlMethod;

public class URLServlet extends HttpServlet {
    
    private Map<UrlMethod,Mapping> controllerClasses = new HashMap<>();
    private String annotationName;
    private List<String> url_valide = new ArrayList<>();
    private List<String> annotation_valide = new ArrayList<>();
    private Map<String, List<String>> classesByAnnotation = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        
        String packagesParam = getInitParameter("packages");
        String annotationParam = getInitParameter("annotation");
        
        if (packagesParam == null || annotationParam == null) {
            throw new ServletException("<p>Les paramètres 'packages' et 'annotation' doivent être configurés dans web.xml</p>");
        }
        
        List<String> packages = new ArrayList<>();
        String[] packageArray = packagesParam.split(",");
        for (String pkg : packageArray) {
            packages.add(pkg.trim());
        }
        
        annotationName = annotationParam.trim();
        try {
            controllerClasses = ClassScanner.chargement_classe(packages, annotationName);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        String url = request.getRequestURL().toString();
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        String queryString = request.getQueryString();
        
        // Extraire le chemin sans le contexte (ex: /AppTest/order-details -> /order-details)
        String mappingPath = path.substring(contextPath.length());
        
        try {
            out.println("<html><body>");
            out.println("<h1>Request URL: " + url + "</h1>");
            out.println("<p>Methode: " + request.getMethod() + "</p>");
            out.println("<p>Path: " + path + "</p>");
            out.println("<p>Mapping Path: " + mappingPath + "</p>");
            out.println("<br>");

            UrlMethod urlMethod = new UrlMethod(mappingPath, request.getMethod());
            if(controllerClasses.containsKey(urlMethod)) {
                try{
                    Mapping mapping = controllerClasses.get(urlMethod);

                    // 1. Recuperer la classe du contrôleur 
                    Class<?> controllerClass = mapping.getClazz();
                    
                    // 2. Instancier dynamiquement l'objet controleur
                    Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                    
                    // 3. Recuperer l'objet Method 
                    Method methodToInvoke = mapping.getMethode();
                    
                    // 4. Invoquer la methode sur l'instance creer
                    String resultat = (String) methodToInvoke.invoke(controllerInstance);
                    
                    // 5. Afficher le résultat dans le HTML
                    out.println("<p>URL: " + mapping.getUrl() + "</p>"); 
                    out.println("<p>Classe: " + controllerClass.getName() + "</p>"); 
                    out.println("<p>Méthode Java: " + methodToInvoke.getName() + "</p>"); 
                    out.println("<p><b>Résultat de l'exécution :</b> " + resultat + "</p>");
                    out.println("<p>HTTP Method: " + mapping.getHttpMethode() + "</p>");
                } catch (Exception e) {
                    out.println("<p style='color:red;'>Erreur lors de l'invocation du contrôleur : " + e.getMessage() + "</p>");
                    e.printStackTrace(); 
                }
            } else {
                out.println("<p>Aucune classe trouvée pour URL: " + mappingPath + "</p>");
                out.println("<p>Voici toutes les URL valides:</p>");
                for (UrlMethod validUrl : controllerClasses.keySet()) {
                    out.println("<p><a href=\"" + contextPath + validUrl.getUrl() + "\">" + validUrl.getUrl() + "</a></p>");
                }
            }
            
            out.println("</body></html>");
        } catch(Exception e){
            out.print("Erreur: "+e.getMessage());
        } finally {
            out.close();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
}