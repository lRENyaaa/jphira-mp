package top.rymc.phira.plugin.injector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MiniInjectorTest {

    private MiniInjector injector;

    @BeforeEach
    void setUp() {
        injector = new MiniInjector();
    }

    @Test
    @DisplayName("bind and getInstance returns the bound instance")
    void bindAndGetInstanceReturnsTheBoundInstance() {
        ServiceInterface service = new ServiceImpl();
        injector.bind(ServiceInterface.class, service);

        ServiceInterface result = injector.getInstance(ServiceInterface.class);

        assertThat(result).isSameAs(service);
    }

    @Test
    @DisplayName("Constructor injection with Inject annotation")
    void constructorInjectionWithInjectAnnotation() {
        ServiceInterface service = new ServiceImpl();
        injector.bind(ServiceInterface.class, service);

        ConstructorInjectionClient client = injector.getInstance(ConstructorInjectionClient.class);

        assertThat(client.getService()).isSameAs(service);
    }

    @Test
    @DisplayName("Field injection with Inject annotation")
    void fieldInjectionWithInjectAnnotation() {
        ServiceInterface service = new ServiceImpl();
        injector.bind(ServiceInterface.class, service);

        FieldInjectionClient client = injector.getInstance(FieldInjectionClient.class);

        assertThat(client.getService()).isSameAs(service);
    }

    @Test
    @DisplayName("Missing binding throws RuntimeException")
    void missingBindingThrowsRuntimeException() {
        assertThatThrownBy(() -> injector.getInstance(InterfaceDependencyClient.class))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No binding");
    }

    @Test
    @DisplayName("Auto instantiate concrete class without binding")
    void autoInstantiateConcreteClassWithoutBinding() {
        ConcreteService service = injector.getInstance(ConcreteService.class);

        assertThat(service).isNotNull();
    }

    @Test
    @DisplayName("Auto instantiate concrete class with nested dependencies")
    void autoInstantiateConcreteClassWithNestedDependencies() {
        ConcreteService nested = new ConcreteService();
        injector.bind(ConcreteService.class, nested);

        NestedConcreteClient client = injector.getInstance(NestedConcreteClient.class);

        assertThat(client.getConcreteService()).isSameAs(nested);
    }

    @Test
    @DisplayName("Inject fields method injects dependencies into existing instance")
    void injectFieldsMethodInjectsDependenciesIntoExistingInstance() {
        ServiceInterface service = new ServiceImpl();
        injector.bind(ServiceInterface.class, service);

        FieldInjectionClient client = new FieldInjectionClient();
        injector.injectFields(client);

        assertThat(client.getService()).isSameAs(service);
    }

    @Test
    @DisplayName("Multiple bindings work correctly")
    void multipleBindingsWorkCorrectly() {
        ServiceInterface service = new ServiceImpl();
        AnotherService anotherService = new AnotherServiceImpl();
        injector.bind(ServiceInterface.class, service);
        injector.bind(AnotherService.class, anotherService);

        MultipleDependenciesClient client = injector.getInstance(MultipleDependenciesClient.class);

        assertThat(client.getService()).isSameAs(service);
        assertThat(client.getAnotherService()).isSameAs(anotherService);
    }

    interface ServiceInterface {
        String getName();
    }

    static class ServiceImpl implements ServiceInterface {
        @Override
        public String getName() {
            return "ServiceImpl";
        }
    }

    interface AnotherService {
        void doSomething();
    }

    static class AnotherServiceImpl implements AnotherService {
        @Override
        public void doSomething() {
        }
    }

    static class ConstructorInjectionClient {
        private final ServiceInterface service;

        @Inject
        ConstructorInjectionClient(ServiceInterface service) {
            this.service = service;
        }

        ServiceInterface getService() {
            return service;
        }
    }

    static class FieldInjectionClient {
        @Inject
        private ServiceInterface service;

        ServiceInterface getService() {
            return service;
        }
    }

    static class InterfaceDependencyClient {
        @Inject
        private ServiceInterface service;

        ServiceInterface getService() {
            return service;
        }
    }

    static class ConcreteService {
        private final String name = "ConcreteService";

        String getName() {
            return name;
        }
    }

    static class NestedConcreteClient {
        private final ConcreteService concreteService;

        @Inject
        NestedConcreteClient(ConcreteService concreteService) {
            this.concreteService = concreteService;
        }

        ConcreteService getConcreteService() {
            return concreteService;
        }
    }

    static class MultipleDependenciesClient {
        private final ServiceInterface service;
        private final AnotherService anotherService;

        @Inject
        MultipleDependenciesClient(ServiceInterface service, AnotherService anotherService) {
            this.service = service;
            this.anotherService = anotherService;
        }

        ServiceInterface getService() {
            return service;
        }

        AnotherService getAnotherService() {
            return anotherService;
        }
    }
}
