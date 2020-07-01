package uk.gov.hmcts.reform.fpl.config;

import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import uk.gov.hmcts.reform.fpl.request.RequestData;
import uk.gov.hmcts.reform.fpl.request.RequestDataCache;
import uk.gov.hmcts.reform.fpl.request.SimpleRequestData;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

@Slf4j
@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AsyncConfiguration implements AsyncConfigurer {

    private final ApplicationContext context;

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }

    public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(Throwable throwable, Method method, Object... obj) {
            log.error("Unexpected error occurred during async execution", throwable);
        }
    }

    @Override
    @Bean
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new MyThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("GithubLookup-");
        executor.initialize();
        return executor;
    }

    static class AsyncTaskDecorator implements TaskDecorator {

        final ApplicationContext context;

        AsyncTaskDecorator(ApplicationContext context) {
            this.context = context;
        }

        @Override
        public Runnable decorate(@Nonnull Runnable task) {
            SimpleRequestData requestData = new SimpleRequestData(context.getBean(RequestData.class));

            RequestTelemetryContext requestTelemetryContext = ThreadContext.getRequestTelemetryContext();
            return () -> {
//
                RequestDataCache.add(requestData);
                ThreadContext.setRequestTelemetryContext(requestTelemetryContext);
                try {
                    task.run();
                } finally {
                    ThreadContext.remove();
                    RequestDataCache.remove();
                }
            };
        }
    }

    private class MyThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

        @Override
        public <T> Future<T>  submit(Callable<T> task) {
            return super.submit(new Wrapped(task, ThreadContext.getRequestTelemetryContext()));
        }
    }

    /**
     * A wrapper class that holds the instance of runnable and the associated context
     */
    protected class Wrapped<T> implements Callable<T> {
        private final Callable<T> task;
        private final RequestTelemetryContext rtc;

        Wrapped(Callable<T> task, RequestTelemetryContext rtc) {
            this.task = task;
            this.rtc = rtc;
        }

        @Override
        public T call() throws Exception {
            if (ThreadContext.getRequestTelemetryContext() != null) {
                ThreadContext.remove();
            }

            // Set the context explicitly
            ThreadContext.setRequestTelemetryContext(rtc);
            return task.call();

        }
    }
}
