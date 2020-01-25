package startup;

import com.google.inject.AbstractModule;

public class AppInitialiser extends AbstractModule {
    @Override
    protected void configure() {
        bind(InMemoryDbInitialiser.class).asEagerSingleton();
    }
}
