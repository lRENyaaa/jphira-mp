package top.rymc.phira.main.game;

import java.lang.reflect.Field;

public final class ReflectionUtil {

    private ReflectionUtil() {}

    @SuppressWarnings("unchecked")
    public static <T> T getField(Object target, String fieldName) {
        try {
            Class<?> clazz = target instanceof Class ? (Class<?>) target : target.getClass();
            Field field = findField(clazz, fieldName);
            field.setAccessible(true);
            return (T) field.get(target instanceof Class ? null : target);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get field " + fieldName + " from " + target.getClass(), e);
        }
    }

    public static void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target instanceof Class ? (Class<?>) target : target.getClass();
            Field field = findField(clazz, fieldName);
            field.setAccessible(true);
            field.set(target instanceof Class ? null : target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName + " on " + target.getClass(), e);
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), fieldName);
            }
            throw e;
        }
    }
}
