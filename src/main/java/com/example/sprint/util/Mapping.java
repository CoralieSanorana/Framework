package com.example.sprint.util;
import java.lang.reflect.Method;

public class Mapping {
    private Class<?> className;
    private Method methodName;
    private String url;
    private String httpMethod;  

    public Mapping(Class<?> className, Method methodName, String url, String httpMethod) {
        this.className = className;
        this.methodName = methodName;
        this.url = url;
        this.httpMethod = httpMethod;
    }

    public Class<?> getClassName() {
        return className;
    }

    public Method getMethodName() {
        return methodName;
    }

    public String getUrl() {
        return url;
    }

    public void setClassName(Class<?> className){
        this.className = className;
    }

    public void setMethodName(Method methodName){
        this.methodName = methodName;
    }

    public void setUrl(String url){
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
}