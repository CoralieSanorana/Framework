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

    public String getId() {
        return session.getId();
    }

    // Chaque écriture modifie instantanément le HttpSession sous-jacent
    public void set(String key, Object value) {
        session.setAttribute(key, value);
    }

    public Object get(String key) {
        return session.getAttribute(key);
    }

    public void remove(String key) {
        session.removeAttribute(key);
    }

    public void invalidate() {
        session.invalidate();
    }

    public Map<String, Object> getAllAttributes() {
        Map<String, Object> map = new HashMap<>();
        Enumeration<String> keys = session.getAttributeNames();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            map.put(key, session.getAttribute(key));
        }
        return map;
    }
}