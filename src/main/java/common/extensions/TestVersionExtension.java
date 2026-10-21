package common.extensions;

import api.configs.Config;
import common.annotations.TestVersion;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Arrays;

public class TestVersionExtension implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        // Если JUnit проверяет класс, разрешаем перейти к проверке методов.
        if (extensionContext.getTestMethod().isEmpty()) {
            return ConditionEvaluationResult.enabled(
                    "Version will be checked for each test method");
        }

        TestVersion annotation = extensionContext.getTestMethod()
                .get()
                .getAnnotation(TestVersion.class);

        if (annotation == null) {
            return ConditionEvaluationResult.disabled(
                    "Test is missing @TestVersion annotation");
        }

        String currentVersion = Config.getProperty("testVersion");

//        if (currentVersion == null || currentVersion.isBlank()) {
//            throw new IllegalStateException(
//                    "Property 'testVersion' is missing or empty");
//        }

//        currentVersion = currentVersion.trim();

        String[] allowedVersions = annotation.value();

        boolean matches = Arrays.stream(allowedVersions)
                .anyMatch(version -> version.equals(currentVersion));

        if (matches) {
            return ConditionEvaluationResult.enabled("Test version matches: " + currentVersion);
        }

        return ConditionEvaluationResult.disabled(
                "Test requires version from the list: " + Arrays.toString(allowedVersions) + ", current version is " + currentVersion);
    }
}


