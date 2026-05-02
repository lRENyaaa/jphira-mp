package top.rymc.phira.main.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ThreadFactoryCompat {

    private ThreadFactoryCompat() {
    }


    public static final boolean VIRTUAL_THREAD_AVAILABLE;
    public static final Function<String, ThreadFactory> THREAD_FACTORY_CREATOR;
    public static final Supplier<ExecutorService> BATCH_EXECUTOR_CREATOR;

    static {
        Function<String, ThreadFactory> factoryCreator;
        Supplier<ExecutorService> executorCreator;
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

            factoryCreator = threadName -> {
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

            MethodHandle newVirtual = MethodHandles.publicLookup().findStatic(
                    Executors.class,
                    "newVirtualThreadPerTaskExecutor",
                    MethodType.methodType(ExecutorService.class)
            );

            executorCreator = () -> {
                try {
                    return ExecutorServiceManager.registerService((ExecutorService) newVirtual.invokeExact());
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            };

            available = true;
        } catch (Exception | LinkageError e) {
            ThreadFactory delegate = Executors.defaultThreadFactory();
            factoryCreator = threadName -> runnable -> {
                Thread thread = delegate.newThread(runnable);
                thread.setName(threadName);
                return thread;
            };

            executorCreator = () -> ExecutorServiceManager.registerService(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        }

        VIRTUAL_THREAD_AVAILABLE = available;
        THREAD_FACTORY_CREATOR = factoryCreator;
        BATCH_EXECUTOR_CREATOR = executorCreator;

    }



}