package com.example.sprint.util;
import java.lang.reflect.Method;

public class Mapping {
    private Class<?> clazz;
    private Method methode;
    private String url;
    private String httpMethode;  

    public Mapping(Class<?> clazz, Method methode, String url, String httpMethode) {
        this.clazz = clazz;
        this.methode = methode;
        this.url = url;
        this.httpMethode = httpMethode;
    }

    @Override
    public boolean equals(Object obj) {
        /*
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        */
       Mapping mapping = (Mapping) obj;
        return url.equals(mapping.url) &&
                httpMethode.equals(mapping.httpMethode);
    }

    @Override
    public int hashCode() {
        int result = url.hashCode();
        result = 31 * result + httpMethode.hashCode();
        return result;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public Method getMethode() {
        return methode;
    }

    public String getUrl() {
        return url;
    }

    public void setClazz(Class<?> clazz){
        this.clazz = clazz;
    }

    public void setMethode(Method methode){
        this.methode = methode;
    }

    public void setUrl(String url){
        this.url = url;
    }

    public String getHttpMethode() {
        return httpMethode;
    }

    public void setHttpMethode(String httpMethode) {
        this.httpMethode = httpMethode;
    }
}