package top.rymc.phira.plugin.injector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MiniInjector {
    private final Map<Class<?>, Object> bindings = new ConcurrentHashMap<>();
    private final Map<Class<?>, Field[]> fieldsCache = new ConcurrentHashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        bindings.put(type, instance);
    }

    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<T> type) {
        Constructor<?> ctor = findConstructor(type);
        Object[] args = Arrays.stream(ctor.getParameterTypes())
                .map(this::resolve)
                .toArray();

        try {
            T instance = (T) ctor.newInstance(args);
            injectFields(instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create " + type.getName(), e);
        }
    }

    public void injectFields(Object instance) {
        Class<?> clazz = instance.getClass();
        Field[] fields = fieldsCache.computeIfAbsent(clazz, c ->
                Arrays.stream(c.getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(Inject.class))
                        .peek(f -> f.setAccessible(true))
                        .toArray(Field[]::new)
        );

        for (Field field : fields) {
            Object value = resolve(field.getType());
            if (value == null) {
                throw new RuntimeException("No binding for " + field.getType() + " in " + clazz.getName());
            }
            try {
                field.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Object resolve(Class<?> type) {
        Object bound = bindings.get(type);
        if (bound != null) return bound;

        if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
            return getInstance(type);
        }
        return null;
    }

    private Constructor<?> findConstructor(Class<?> type) {
        Constructor<?> injected = Arrays.stream(type.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .findFirst()
                .orElse(null);

        if (injected != null) return injected;

        try {
            return type.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No injectable constructor in " + type);
        }
    }
}