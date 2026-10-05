package main.com.util;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Binder {

    // Registre des convertisseurs pour éviter la longue chaîne de if/else
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

            // 1. Cas où la méthode demande directement le HttpServletRequest
            if (HttpServletRequest.class.isAssignableFrom(typeParam)) {
                arguments[i] = request;
                continue;
            }

            // 2. Cas des types simples (Primitifs, Wrappers, String, Enum, Char)
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
            // 3. Cas d'un objet complexe (Model / DTO)
            else {
                arguments[i] = instancierEtRemplirObjet(typeParam, request);
            }
        }
        return arguments;
    }

    // Crée une instance de la classe cible et remplit ses champs à partir du HttpServletRequest.
    private Object instancierEtRemplirObjet(Class clazz, HttpServletRequest request) {
        try {
            // Instanciation de l'objet via le constructeur sans argument
            Object instance = clazz.getDeclaredConstructor().newInstance();

            // Parcours de tous les champs déclarés de la classe
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                String fieldName = field.getName();
                String valeurReq = request.getParameter(fieldName);

                // Si la requête contient une valeur correspondant au nom du champ
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

    // Vérifie si le type est un type simple supporté directement.
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

    // Fournit une valeur par défaut pour les types primitifs si le paramètre est absent.
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
}