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

public class URLServlet extends HttpServlet {
    
    private Map<String,Mapping> controllerClasses = new HashMap<>();
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
        
        controllerClasses = ClassScanner.chargement_classe(packages, annotationName);

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

            if(controllerClasses.containsKey(mappingPath) && controllerClasses.get(mappingPath).getHttpMethod().equalsIgnoreCase(request.getMethod())) {
                Mapping mapping = controllerClasses.get(mappingPath);
                out.println("<p>URL: " + mapping.getUrl() + "</p>");
                out.println("<p>Classe: " + mapping.getClassName().getName() + "</p>");
                out.println("<p>Méthode: " + mapping.getMethodName().getName() + "</p>");
                out.println("<p>HTTP Method: " + mapping.getHttpMethod() + "</p>");
            } else {
                out.println("<p>Aucune classe trouvée pour URL: " + mappingPath + "</p>");
                out.println("<p>Voici toutes les URL valides:</p>");
                for (String validUrl : controllerClasses.keySet()) {
                    out.println("<p><a href=\"" + contextPath + validUrl + "\">" + validUrl + "</a></p>");
                }
            }
            
            out.println("</body></html>");
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