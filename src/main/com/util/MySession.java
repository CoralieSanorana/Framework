package main.com.util;

import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class MySession {
    private HttpSession session;

    public MySession(HttpSession session) {
        this.session = session;
    }

    public Object get(String key) {
        return session.getAttribute(key);
    }

    public void set(String key, Object value) {
        session.setAttribute(key, value);
    }

    public void remove(String key) {
        session.removeAttribute(key);
    }

    public void invalidate() {
        session.invalidate();
    }

    // Optionnel : récupérer toute la session sous forme de Map pour affichage
    public Map<String, Object> getAll() {
        Map<String, Object> map = new HashMap<>();
        Enumeration<String> keys = session.getAttributeNames();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            map.put(key, session.getAttribute(key));
        }
        return map;
    }
}