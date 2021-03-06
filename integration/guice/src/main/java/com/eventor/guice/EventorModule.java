package com.eventor.guice;

import com.eventor.Eventor;
import com.eventor.EventorBuilder;
import com.eventor.api.CommandBus;
import com.eventor.api.EventBus;
import com.eventor.api.InstanceCreator;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public final class EventorModule extends AbstractModule {
    private final Iterable<Class<?>> classes;

    public EventorModule(Iterable<Class<?>> classes) {
        this.classes = classes;
    }

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    public Eventor eventor(Injector injector) {
        return new EventorBuilder()
                .addClasses(classes)
                .withInstanceCreator(new GuiceInstanceCreator(injector))
                .build();
    }

    @Provides
    public EventBus eventBus(Eventor eventor) {
        return eventor.getEventBus();
    }

    @Provides
    public CommandBus commandBus(Eventor eventor) {
        return eventor;
    }

    private static class GuiceInstanceCreator implements InstanceCreator {
        Injector injector;

        public GuiceInstanceCreator(Injector injector) {
            this.injector = injector;
        }

        @Override
        public <T> T findInstanceOf(Class<T> clazz) {
            return injector.getInstance(clazz);
        }

        @Override
        public <T> T findOrCreateInstanceOf(Class<T> clazz, boolean isExpectedToBeSingleton) {
            return injector.getInstance(clazz);
        }
    }
}
