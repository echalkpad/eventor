package com.eventor.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.eventor.internal.EventorCollections.newMap;

public class EventorReflections {
    public static Map<Class<? extends Annotation>, Iterable<Class<?>>> getClassesAnnotated(
            Iterable<Class<?>> classes,
            Class<? extends Annotation>... annotations) {
        Map<Class<? extends Annotation>, Set<Class<?>>> result = newMap();
        for (Class<? extends Annotation> annotation : annotations) {
            result.put(annotation, EventorCollections.<Class<?>>newSet());
        }
        for (Class<?> clazz : classes) {
            for (Class<? extends Annotation> annotation : annotations) {
                if (classAnnotated(clazz, annotation)) {
                    result.get(annotation).add(clazz);
                }
            }
        }
        return (Map) result;
    }

    public static Map<Class<? extends Annotation>, Iterable<Method>> getMethodsAnnotated(
            Class<?> clazz, Class<? extends Annotation>... annotations) {
        Map<Class<? extends Annotation>, Set<Method>> result = newMap();
        for (Class<? extends Annotation> annotation : annotations) {
            result.put(annotation, EventorCollections.<Method>newSet());
        }
        for (Method method : clazz.getDeclaredMethods()) {
            for (Class<? extends Annotation> annotation : annotations) {
                if (method.getAnnotation(annotation) != null) {
                    result.get(annotation).add(method);
                }
            }
        }
        return (Map) result;
    }

    public static Class<?> getSingleParamType(Method each) {
        Class<?>[] parameterTypes = each.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new RuntimeException(
                    String.format("Single argument method expected in place of %s", stringify(each)));
        }
        return parameterTypes[0];
    }

    private static String stringify(Method method) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(method.getDeclaringClass().getSimpleName())
                .append(".")
                .append(method.getName())
                .append("(");
        for (Class<?> each : method.getParameterTypes()) {
            stringBuilder.append(each.getSimpleName()).append(", ");
        }
        return stringBuilder.append(")").toString();
    }

    public static boolean classAnnotated(Class<?> candidate, Class<? extends Annotation> annotation) {
        return candidate.getAnnotation(annotation) != null;
    }

    public static Map<Class<? extends Annotation>, Annotation> paramAnnotations(Method method, int index) {
        Map<Class<? extends Annotation>, Annotation> result = EventorCollections.newMap();
        for (Annotation each : method.getParameterAnnotations()[index]) {
            if (result.containsKey(each.getClass())) {
                throw new RuntimeException("Repeatable annotations are not supported in " + stringify(method));
            }
            result.put(each.annotationType(), each);
        }
        return result;
    }

    public static Object retrieveAnnotatedValue(Object obj, Class<? extends Annotation> mark) {
        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                if (f.isAnnotationPresent(mark)) {
                    f.setAccessible(true);
                    return f.get(obj);
                }
            }
            throw new RuntimeException(String.format("Fail while getting %s from %s", mark, obj));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object retrieveNamedValue(Object obj, String mark) {
        try {
            String expectedGetterName = "get" + mark.substring(0, 1).toUpperCase() + mark.substring(1);
            for (Method m : obj.getClass().getDeclaredMethods()) {
                if (m.getName().equals(expectedGetterName) &&
                        m.getParameterTypes().length == 0) {
                    m.setAccessible(true);
                    return m.invoke(obj);
                }
            }
            for (Field f : obj.getClass().getDeclaredFields()) {
                if (f.getName().equals(mark)) {
                    f.setAccessible(true);
                    return f.get(obj);
                }
            }
            int dotIndex = mark.indexOf('.');
            if (dotIndex != -1) {
                String firstPart = mark.substring(0, dotIndex);
                String secondPart = mark.substring(dotIndex + 1);
                return retrieveNamedValue(retrieveNamedValue(obj, firstPart), secondPart);
            }
            throw new RuntimeException(String.format("Fail while getting %s from %s", mark, obj));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void validateMark(Class<?> classAnnotated, Class<?> classExpected, String mark) {
        // verify that last char does not have ".",
        // because "abc.".split("\\.") returns array that has only one "abc" item.
        if (mark.contains(" ") || mark.charAt(mark.length() - 1) == '.') {
            throw new IllegalArgumentException(String.format("Illegal IdIn specification [%s] in %s for %s",
                    mark, classAnnotated.getName(), classExpected.getName()));
        }
        for (String each : mark.split("\\.")) {
            if (each.isEmpty()) {
                throw new IllegalArgumentException(String.format("Illegal IdIn specification [%s] in %s for %s",
                        mark, classAnnotated.getName(), classExpected.getName()));
            }
        }
    }

    public static Collection<?> invoke(Object obj, Method m, Object arg) {
        try {
            if (!m.isAccessible()) m.setAccessible(true);
            return EventorCollections.toCollection(m.invoke(obj, arg));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
