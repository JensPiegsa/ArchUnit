package com.tngtech.archunit.junit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.tngtech.archunit.Slow;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.ClassCache.CacheClassFileImporter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@Category(Slow.class)
public class ClassCacheConcurrencyTest {
    private static final int NUM_THREADS = 20;
    private static final List<Class<?>> TEST_CLASSES = Arrays.asList(
            TestClass1.class, TestClass2.class, TestClass3.class, TestClass4.class, TestClass5.class, TestClass6.class
    );

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Spy
    private CacheClassFileImporter classFileImporter;

    @InjectMocks
    private ClassCache cache = new ClassCache();

    private final ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);

    @Test
    @SuppressWarnings("unchecked")
    public void concurrent_access() throws Exception {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < NUM_THREADS; i++) {
            futures.add(executorService.submit(repeatGetClassesToAnalyze(1000)));
        }
        for (Future<?> future : futures) {
            future.get(1, MINUTES);
        }
        verify(classFileImporter, atMost(TEST_CLASSES.size())).importClasses(any(ImportOption.class), anyCollection());
        verifyNoMoreInteractions(classFileImporter);
    }

    private Runnable repeatGetClassesToAnalyze(final int times) {
        return new Runnable() {
            @Override
            public void run() {
                for (int j = 0; j < times; j++) {
                    cache.getClassesToAnalyzeFor(TEST_CLASSES.get(j % TEST_CLASSES.size()));
                }
            }
        };
    }

    @AnalyzeClasses(packages = "com.tngtech.archunit.junit")
    public static class TestClass1 {
    }

    @AnalyzeClasses(packages = "com.tngtech.archunit.example")
    public static class TestClass2 {
    }

    @AnalyzeClasses(packages = "com.tngtech.archunit.integration")
    public static class TestClass3 {
    }

    @AnalyzeClasses(packages = "com.tngtech.archunit.core")
    public static class TestClass4 {
    }

    @AnalyzeClasses(packages = "com.tngtech.archunit")
    public static class TestClass5 {
    }

    @AnalyzeClasses(packages = "com.tngtech")
    public static class TestClass6 {
    }
}
