package main.java.com.example.sprint.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.example.sprint.util.ClassScanner;

public class URLServlet extends HttpServlet {
    
    private List<String> controllerClasses = new ArrayList<>();
    private String annotationName;
    private List<String> url_valide = new ArrayList<>();
    private List<String> annotation_valide = new ArrayList<>();
    private Map<String, List<String>> classesByAnnotation = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        
        String packagesParam = getInitParameter("packages");
        String annotationParam = getInitParameter("annotation");
        String annotation_valideParam = getInitParameter("annotation_valide");
        String url_valideParam = getInitParameter("url_valide");
        
        if (packagesParam == null || annotationParam == null) {
            throw new ServletException("<p>Les paramètres 'packages' et 'annotation' doivent être configurés dans web.xml</p>");
        }
        
        List<String> packages = new ArrayList<>();
        String[] packageArray = packagesParam.split(",");
        for (String pkg : packageArray) {
            packages.add(pkg.trim());
        }

        String[] annotation_valideArray = annotation_valideParam.split(",");
        for (String ann : annotation_valideArray) {
            annotation_valide.add(ann.trim());
        }
        
        String[] url_valideArray = url_valideParam.split(",");
        for (String url : url_valideArray) {
            url_valide.add(url.trim());
        }
        
        annotationName = annotationParam.trim();
        
        controllerClasses = ClassScanner.chargement_classe(packages, annotationName);
        
        // Scanner les classes pour chaque annotation valide
        for (String ann : annotation_valide) {
            List<String> classes = ClassScanner.chargement_classe(packages, ann);
            classesByAnnotation.put(ann, classes);
        }
        
        System.out.println("<p>Classes avec l'annotation @" + annotationName + ":</p>");
        for (String className : controllerClasses) {
            System.out.println("<p>  - " + className + "</p>");
        }
    }
    
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        String url = request.getRequestURL().toString();
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        
        try {
            out.println("<html><body>");
            out.println("<h1>Request URL: " + url + "</h1>");
            out.println("<p>Methode: " + request.getMethod() + "</p>");
            
            // Vérifier si le chemin est dans la liste des URLs valides
            boolean urlValideTrouvee = false;
            String annotationCorrespondante = null;
            
            for (int i = 0; i < url_valide.size(); i++) {
                if (path.endsWith(url_valide.get(i))) {
                    urlValideTrouvee = true;
                    if (i < annotation_valide.size()) {
                        annotationCorrespondante = annotation_valide.get(i);
                    }
                    break;
                }
            }
            
            if (urlValideTrouvee && annotationCorrespondante != null) {
                // URL valide : afficher les classes pour l'annotation correspondante
                out.println("<h2>Classes avec l'annotation @" + annotationCorrespondante + ":</h2>");
                out.println("<ul>");
                List<String> classes = classesByAnnotation.get(annotationCorrespondante);
                if (classes != null && !classes.isEmpty()) {
                    for (String className : classes) {
                        out.println("<li>" + className + "</li>");
                    }
                } else {
                    out.println("<p>Aucune classe trouvée avec cette annotation.</p>");
                }
                out.println("</ul>");
            } else {
                // URL invalide : afficher la liste des URLs valides et toutes les annotations
                out.println("<h2>URL invalide. Voici les URLs valides:</h2>");
                out.println("<ul>");
                for (String validUrl : url_valide) {
                    out.println("<li><a href=\"" + validUrl + "\">" + validUrl + "</a></li>");
                }
                out.println("</ul>");
                
                out.println("<h2>Toutes les annotations valides et leurs classes:</h2>");
                for (String ann : annotation_valide) {
                    out.println("<h3>Annotation @" + ann + ":</h3>");
                    out.println("<ul>");
                    List<String> classes = classesByAnnotation.get(ann);
                    if (classes != null && !classes.isEmpty()) {
                        for (String className : classes) {
                            out.println("<li>" + className + "</li>");
                        }
                    } else {
                        out.println("<p>Aucune classe trouvée.</p>");
                    }
                    out.println("</ul>");
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