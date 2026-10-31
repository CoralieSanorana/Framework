package main.com.util;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.io.IOException; 
import java.io.InputStream; 
import java.lang.reflect.ParameterizedType; 
import java.lang.reflect.Type; 
import java.util.*; 
import javax.servlet.ServletException; 
import javax.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import javax.servlet.http.HttpSession;

public class Binder {

    // Registre des convertisseurs pour eviter la longue chaine de if/else
    private static final Map<Class<?>, Function<String, ?>> CONVERTERS = new HashMap<>();
    static {
        CONVERTERS.put(String.class, v -> v);
        CONVERTERS.put(int.class, Integer::valueOf);
        CONVERTERS.put(Integer.class, Integer::valueOf);
        CONVERTERS.put(long.class, Long::valueOf);
        CONVERTERS.put(Long.class, Long::valueOf);
        CONVERTERS.put(double.class, Double::valueOf);
        CONVERTERS.put(Double.class, Double::valueOf);
        CONVERTERS.put(float.class, Float::valueOf);
        CONVERTERS.put(Float.class, Float::valueOf);
        CONVERTERS.put(short.class, Short::valueOf);
        CONVERTERS.put(Short.class, Short::valueOf);
        CONVERTERS.put(byte.class, Byte::valueOf);
        CONVERTERS.put(Byte.class, Byte::valueOf);
        CONVERTERS.put(boolean.class, Boolean::valueOf);
        CONVERTERS.put(Boolean.class, Boolean::valueOf);
    }

    public Object[] construireArguments(Method methode, HttpServletRequest request) {
        Parameter[] parametres = methode.getParameters();
        Object[] arguments = new Object[parametres.length];

        for (int i = 0; i < parametres.length; i++) {
            Parameter parametre = parametres[i];
            Class typeParam = parametre.getType();

            // 1. Cas ou la methode demande directement le HttpServletRequest
            if (HttpServletRequest.class.isAssignableFrom(typeParam)) {
                arguments[i] = request;
                continue;
            }

            // 2. Cas ou la methode demande directement le HttpSession standard de Java
            if (HttpSession.class.isAssignableFrom(typeParam)) {
                arguments[i] = request.getSession();
                continue;
            }

            // 3. Upload : FileUpload, FileUpload[] ou List<FileUpload>
            if (estTypeUpload(parametre)) {
                String nomParam = parametre.getName();   // = attribut name de <input type="file">
                List<FileUpload> fichiers = lireFichiers(request, nomParam);

                if (typeParam == FileUpload.class) {
                    arguments[i] = fichiers.isEmpty() ? null : fichiers.get(0);
                } else if (typeParam.isArray()) {
                    arguments[i] = fichiers.toArray(new FileUpload[0]);
                } else {
                    arguments[i] = fichiers;
                }
                continue;
            }

            // 4. Cas des types simples (Primitifs, Wrappers, String, Enum, Char)
            if (estTypeSimple(typeParam)) {
                if (!parametre.isNamePresent()) {
                    throw new IllegalArgumentException(
                            "Les noms des parametres de " + methode.getName()
                                    + " ne sont pas disponibles. Compilez avec -parameters.");
                }

                String nomParam = parametre.getName();
                String valeur = request.getParameter(nomParam);

                if (valeur == null || valeur.trim().isEmpty()) {
                    if (typeParam.isPrimitive()) {
                        arguments[i] = valeurParDefautPrimitif(typeParam);
                        continue;
                    }
                    throw new IllegalArgumentException("Parametre requis manquant : " + nomParam);
                }

                arguments[i] = convertirArgument(valeur, typeParam, nomParam);
            }
            
            // 5. Cas d'un objet complexe (Model / DTO)
            else {
                arguments[i] = instancierEtRemplirObjet(typeParam, request);
            }
        }
        return arguments;
    }

    // Cree une instance de la classe cible et remplit ses champs a partir du HttpServletRequest.
    private Object instancierEtRemplirObjet(Class clazz, HttpServletRequest request) {
        try {
            // Instanciation de l'objet via le constructeur sans argument
            Object instance = clazz.getDeclaredConstructor().newInstance();

            // Parcours de tous les champs declares de la classe
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                String fieldName = field.getName();

                if (field.getType() == FileUpload.class) {
                    List<FileUpload> f = lireFichiers(request, fieldName);
                    if (!f.isEmpty()) {
                        field.setAccessible(true);
                        field.set(instance, f.get(0));
                    }
                    continue;
                }

                String valeurReq = request.getParameter(fieldName);

                // Si la requete contient une valeur correspondant au nom du champ
                if (valeurReq != null && !valeurReq.trim().isEmpty()) {
                    field.setAccessible(true);
                    Object valeurConvertie = convertirArgument(valeurReq, field.getType(), fieldName);
                    field.set(instance, valeurConvertie);
                }
            }
            return instance;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Impossible d'instancier ou de remplir l'objet de type : " + clazz.getName(), e);
        }
    }

    // Verifie si le type est un type simple supporte directement.
    private boolean estTypeSimple(Class type) {
        return CONVERTERS.containsKey(type) 
                || type.isEnum() 
                || type == char.class 
                || type == Character.class;
    }

    // Convertit une valeur String vers le type cible.
    private Object convertirArgument(String valeur, Class type, String nomParametre) {
        try {
            if (CONVERTERS.containsKey(type)) {
                return CONVERTERS.get(type).apply(valeur);
            }

            if (type == char.class || type == Character.class) {
                if (valeur.length() != 1) {
                    throw new IllegalArgumentException(
                            "Le parametre " + nomParametre + " doit contenir un seul caractere.");
                }
                return valeur.charAt(0);
            }

            if (type.isEnum()) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                Object valeurEnum = Enum.valueOf((Class) type, valeur);
                return valeurEnum;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Valeur invalide pour le parametre " + nomParametre + " : " + valeur, e);
        }

        throw new IllegalArgumentException(
                "Type de parametre non supporte pour " + nomParametre + " : " + type.getName());
    }

    // Fournit une valeur par defaut pour les types primitifs si le parametre est absent.
    private Object valeurParDefautPrimitif(Class type) {
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        return null;
    }

    private boolean estTypeUpload(Parameter p) {
        Class<?> t = p.getType();
        if (t == FileUpload.class) return true;
        if (t.isArray() && t.getComponentType() == FileUpload.class) return true;
        if (List.class.isAssignableFrom(t)) {
            Type g = p.getParameterizedType();
            if (g instanceof ParameterizedType) {
                Type arg = ((ParameterizedType) g).getActualTypeArguments()[0];
                return arg == FileUpload.class;
            }
        }
        return false;
    }

    private List<FileUpload> lireFichiers(HttpServletRequest request, String nom) {
        List<FileUpload> resultat = new ArrayList<>();
        String ct = request.getContentType();
        if (ct == null || !ct.toLowerCase().startsWith("multipart/")) {
            return resultat;
        }
        try {
            for (Part part : request.getParts()) {
                if (!nom.equals(part.getName())) continue;
                String nomFichier = part.getSubmittedFileName();
                if (nomFichier == null || nomFichier.isEmpty()) continue;
                try (InputStream in = part.getInputStream()) {
                    resultat.add(new FileUpload(nomFichier, part.getContentType(), lireOctets(in)));
                }
            }
        } catch (IOException | ServletException e) {
            throw new IllegalArgumentException("Erreur lors de la lecture de l'upload : " + nom, e);
        }
        return resultat;
    }

    private byte[] lireOctets(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int n;
        while ((n = in.read(data)) != -1) {
            buffer.write(data, 0, n);
        }
        return buffer.toByteArray();
    }

}