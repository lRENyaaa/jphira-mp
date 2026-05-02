package top.rymc.phira.main.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

public class ExecutorServiceManager {

    private static final List<ExecutorService> EXECUTOR_SERVICES = new CopyOnWriteArrayList<>();

    public static ExecutorService registerService(ExecutorService executor) {
        EXECUTOR_SERVICES.add(executor);
        return executor;
    }

    public static void shutdown() {
        EXECUTOR_SERVICES.stream()
                .filter((executorService) -> !executorService.isShutdown())
                .forEach(ExecutorService::shutdownNow);
    }
}
