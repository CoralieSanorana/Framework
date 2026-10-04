package main.com.util;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {
    private String url;
    private Map<String, Object> data;

    public ModelAndView() {
        this.data = new HashMap<>();
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setAttribute(String nom, Object valeur) {
        this.data.put(nom, valeur);
    }

    public Map<String, Object> getData() {
        return data;
    }
}