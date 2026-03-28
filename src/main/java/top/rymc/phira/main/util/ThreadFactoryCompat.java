package top.rymc.phira.main.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

public final class ThreadFactoryCompat {

    private ThreadFactoryCompat() {
    }

    public static final boolean VIRTUAL_THREAD_AVAILABLE;
    public static final Function<String, ThreadFactory> THREAD_FACTORY_CREATOR;

    static {
        Function<String, ThreadFactory> creator;
        boolean available = false;

        try {
            Method ofVirtualMethod = Thread.class.getMethod("ofVirtual");
            Class<?> builderType = ofVirtualMethod.getReturnType();

            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            MethodHandle ofVirtual = lookup.unreflect(ofVirtualMethod);
            MethodHandle name = lookup.findVirtual(
                    builderType,
                    "name",
                    MethodType.methodType(builderType, String.class)
            );
            MethodHandle factory = lookup.findVirtual(
                    builderType,
                    "factory",
                    MethodType.methodType(ThreadFactory.class)
            );

            MethodHandle namedBuilderByString = MethodHandles.collectArguments(name, 0, ofVirtual);
            MethodHandle virtualFactoryByName = MethodHandles.filterReturnValue(namedBuilderByString, factory);

            creator = threadName -> {
                try {
                    return (ThreadFactory) virtualFactoryByName.invokeExact(threadName);
                } catch (Throwable throwable) {
                    if (throwable instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    }

                    if (throwable instanceof Error error) {
                        throw error;
                    }

                    throw new IllegalStateException("Failed to create virtual thread factory", throwable);
                }
            };
            available = true;
        } catch (Exception | LinkageError e) {
            ThreadFactory delegate = Executors.defaultThreadFactory();
            creator = threadName -> runnable -> {
                Thread thread = delegate.newThread(runnable);
                thread.setName(threadName);
                return thread;
            };
        }

        VIRTUAL_THREAD_AVAILABLE = available;
        THREAD_FACTORY_CREATOR = creator;
    }

}