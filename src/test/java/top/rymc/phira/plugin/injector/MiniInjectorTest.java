package top.rymc.phira.plugin.injector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MiniInjector")
class MiniInjectorTest {

    private MiniInjector injector;

    @BeforeEach
    void setUp() {
        injector = new MiniInjector();
    }

    @Test
    @DisplayName("should create new instance for concrete class")
    void shouldCreateNewInstanceForConcreteClass() {
        var service = injector.getInstance(TestService.class);

        assertThat(service).isNotNull();
    }

    @Test
    @DisplayName("should inject constructor dependencies")
    void shouldInjectConstructorDependencies() {
        var dependency = new TestDependency();
        injector.bind(TestDependency.class, dependency);

        var service = injector.getInstance(ServiceWithDependency.class);

        assertThat(service.getDependency()).isSameAs(dependency);
    }

    @Test
    @DisplayName("should inject field dependencies")
    void shouldInjectFieldDependencies() {
        var dependency = new TestDependency();
        injector.bind(TestDependency.class, dependency);

        var service = new ServiceWithFieldInjection();
        injector.injectFields(service);

        assertThat(service.getDependency()).isSameAs(dependency);
    }

    @Test
    @DisplayName("should create instance without bindings for concrete class")
    void shouldCreateInstanceWithoutBindingsForConcreteClass() {
        var service = injector.getInstance(TestService.class);

        assertThat(service).isNotNull();
    }

    @Test
    @DisplayName("should throw when no binding for interface")
    void shouldThrowWhenNoBindingForInterface() {
        assertThatThrownBy(() -> injector.getInstance(TestInterface.class))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("No injectable constructor");
    }

    @Test
    @DisplayName("should auto create dependency when field injection missing binding")
    void shouldAutoCreateDependencyWhenFieldInjectionMissingBinding() {
        var service = new ServiceWithFieldInjection();

        injector.injectFields(service);

        assertThat(service.getDependency()).isNotNull();
    }

    @Test
    @DisplayName("should use no-arg constructor when no inject annotation")
    void shouldUseNoArgConstructorWhenNoInjectAnnotation() {
        var service = injector.getInstance(ServiceWithNoArgConstructor.class);

        assertThat(service).isNotNull();
        assertThat(service.isInitialized()).isTrue();
    }

    @Test
    @DisplayName("should use annotated constructor when present")
    void shouldUseAnnotatedConstructorWhenPresent() {
        var dependency = new TestDependency();
        injector.bind(TestDependency.class, dependency);

        var service = injector.getInstance(ServiceWithAnnotatedConstructor.class);

        assertThat(service.getDependency()).isSameAs(dependency);
    }

    @Test
    @DisplayName("should cache field reflection")
    void shouldCacheFieldReflection() {
        var dependency = new TestDependency();
        injector.bind(TestDependency.class, dependency);

        var service1 = new ServiceWithFieldInjection();
        var service2 = new ServiceWithFieldInjection();

        injector.injectFields(service1);
        injector.injectFields(service2);

        assertThat(service1.getDependency()).isSameAs(dependency);
        assertThat(service2.getDependency()).isSameAs(dependency);
    }

    static class TestService {}

    static class TestDependency {}

    interface TestInterface {}

    static class ServiceWithDependency {
        private final TestDependency dependency;

        @Inject
        ServiceWithDependency(TestDependency dependency) {
            this.dependency = dependency;
        }

        TestDependency getDependency() {
            return dependency;
        }
    }

    static class ServiceWithFieldInjection {
        @Inject
        private TestDependency dependency;

        TestDependency getDependency() {
            return dependency;
        }
    }

    static class ServiceWithNoArgConstructor {
        private boolean initialized = false;

        ServiceWithNoArgConstructor() {
            this.initialized = true;
        }

        boolean isInitialized() {
            return initialized;
        }
    }

    static class ServiceWithAnnotatedConstructor {
        private final TestDependency dependency;

        ServiceWithAnnotatedConstructor() {
            this.dependency = null;
        }

        @Inject
        ServiceWithAnnotatedConstructor(TestDependency dependency) {
            this.dependency = dependency;
        }

        TestDependency getDependency() {
            return dependency;
        }
    }
}
