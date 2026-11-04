package common.extensions;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TimingExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final String START_TIME = "startTime";
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(TimingExtension.class);

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        String testName = extensionContext.getRequiredTestClass().getPackageName() + "." + extensionContext.getDisplayName();

        extensionContext.getStore(NAMESPACE).put(START_TIME, System.nanoTime());

        System.out.println("Thread " + Thread.currentThread().getName() + " : Test started " + testName);
    }


    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        String testName = extensionContext.getRequiredTestClass().getPackageName() + "." + extensionContext.getDisplayName();

        Long startTime = extensionContext.getStore(NAMESPACE).remove(START_TIME, Long.class);

        long testDuration = System.nanoTime() - startTime;
        long durationInMillis = testDuration / 1_000_000;

        System.out.println("Thread " + Thread.currentThread().getName() + " : Test finished " + testName + ", test duration "
                + durationInMillis + " ms");
    }
}