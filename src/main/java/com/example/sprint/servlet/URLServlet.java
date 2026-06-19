package main.java.com.example.sprint.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import com.example.sprint.util.ClassScanner;

public class URLServlet extends HttpServlet {
    
    private List<String> controllerClasses = new ArrayList<>();
    private String annotationName;

    @Override
    public void init() throws ServletException {
        super.init();
        
        String packagesParam = getInitParameter("packages");
        String annotationParam = getInitParameter("annotation");
        
        if (packagesParam == null || annotationParam == null) {
            throw new ServletException("Les paramètres 'packages' et 'annotation' doivent être configurés dans web.xml");
        }
        
        List<String> packages = new ArrayList<>();
        String[] packageArray = packagesParam.split(",");
        for (String pkg : packageArray) {
            packages.add(pkg.trim());
        }
        
        annotationName = annotationParam.trim();
        
        controllerClasses = ClassScanner.chargement_classe(packages, annotationName);
        
        System.out.println("Classes avec l'annotation @" + annotationName + ":");
        for (String className : controllerClasses) {
            System.out.println("  - " + className);
        }
    }
    
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        String url = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        
        try {
            out.println("Request URL: " + url);
            out.println("Classes avec l'annotation @" + annotationName + ":");
            for (String className : controllerClasses) {
                out.println(className);
            }
            if (controllerClasses.isEmpty()) {
                out.println("Aucune classe trouvée avec cette annotation.");
            }
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