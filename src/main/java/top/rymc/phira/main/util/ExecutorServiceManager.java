package top.rymc.phira.main.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ExecutorServiceManager {

    private static final ReferenceQueue<ExecutorService> gcNotifications = new ReferenceQueue<>();
    private static final Set<WeakReference<ExecutorService>> weakRegistry = ConcurrentHashMap.newKeySet();

    private static final ScheduledExecutorService internalExecutor = initInternalExecutor();

    private static ScheduledExecutorService initInternalExecutor() {
        ScheduledExecutorService internalExecutor = Executors.newSingleThreadScheduledExecutor(
                ThreadFactoryCompat.THREAD_FACTORY_CREATOR.apply("executor-manager")
        );
        internalExecutor.scheduleWithFixedDelay(
                ExecutorServiceManager::sweep, 30, 30, TimeUnit.SECONDS
        );

        return internalExecutor;
    }

    public static ExecutorService registerService(ExecutorService executor) {
        if (executor == null) {
            return null;
        }

        WeakReference<ExecutorService> weakRef = new WeakReference<>(executor, gcNotifications);
        weakRegistry.add(weakRef);

        return executor;
    }

    public static void shutdown() {
        sweep();

        List<ExecutorService> living = new ArrayList<>();
        for (WeakReference<ExecutorService> ref : weakRegistry) {
            ExecutorService es = ref.get();
            if (es != null && !es.isShutdown()) {
                es.shutdown();
                living.add(es);
            }
        }
        weakRegistry.clear();

        if (!internalExecutor.isShutdown()) {
            internalExecutor.shutdownNow();
        }

        for (ExecutorService es : living) {
            try {
                if (!es.awaitTermination(5, TimeUnit.SECONDS)) {
                    es.shutdownNow();
                }
            } catch (InterruptedException e) {
                es.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void sweep() {
        WeakReference<? extends ExecutorService> dead;
        while ((dead = (WeakReference<? extends ExecutorService>) gcNotifications.poll()) != null) {
            weakRegistry.remove(dead);
        }
    }
}