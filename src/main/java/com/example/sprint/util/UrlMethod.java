package com.example.sprint.util;

public class UrlMethod {
    private String url;
    private String httpMethod;

    public UrlMethod(String url, String httpMethod) {
        this.url = url;
        this.httpMethod = httpMethod;
    }

    public String getUrl() {
        return url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        UrlMethod that = (UrlMethod) obj;

        if (!url.equals(that.url)) return false;
        return httpMethod.equals(that.httpMethod);
    }

    @Override
    public int hashCode() {
        int result = url.hashCode();
        result = 31 * result + httpMethod.hashCode();
        return result;
    }
}
