package com.eventor;

import com.eventor.api.*;
import com.eventor.internal.Akka;
import com.eventor.internal.ClassProcessor;
import com.eventor.internal.EventorCollections;
import com.eventor.internal.EventorPreconditions;
import com.eventor.internal.meta.Info;
import com.eventor.internal.meta.MetaAggregate;
import com.eventor.internal.meta.MetaHandler;
import com.eventor.internal.meta.MetaSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.eventor.internal.EventorCollections.newList;

public class Eventor implements CommandBus {
    private final Info info;
    private final InstanceCreator instanceCreator;
    private final Akka akka = new Akka();
    private final Map<Object, Object> aggregates = new HashMap<Object, Object>();
    private final CommandBus commandBus;
    private final EventBus eventBus;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public Eventor(final Iterable<Class<?>> aggregates, final InstanceCreator instanceCreator) {
        this.instanceCreator = instanceCreator;
        info = new ClassProcessor().apply(aggregates);
        this.eventBus = createEventBus();
        this.commandBus = createCommandBus();
    }

    public Eventor(final Iterable<Class<?>> aggregates,
                   InstanceCreator instanceCreator,
                   CommandBus commandBus,
                   EventBus eventBus) {
        this.instanceCreator = instanceCreator;
        info = new ClassProcessor().apply(aggregates);
        this.eventBus = eventBus;
        this.commandBus = commandBus;
    }

    private EventBus createEventBus() {
        ArrayList<Listener> eventListeners = new ArrayList<Listener>();
        for (final MetaAggregate eachMetaAggregate : info.aggregates) {
            log.info("Register aggregate {} as event listener", eachMetaAggregate.origClass.getSimpleName());
            eventListeners.add(aggregateAsListener(eachMetaAggregate));
        }
        for (final MetaSubscriber each : info.subscribers) {
            log.info("Register event listener {}", each.origClass.getSimpleName());
            eventListeners.add(eventListenerAsListener(each));
        }
        final Invokable router = akka.createBroadcaster(eventListeners);
        return new EventBus() {
            @Override
            public void publish(Object event) {
                log.info("Published to bus {}", event);
                router.invoke(event, null);
            }
        };
    }

    private CommandBus createCommandBus() {
        List<Listener> commandHandlers = newList();
        for (final MetaAggregate eachMetaAggregate : info.aggregates) {
            log.info("Register aggregate {} as command handler", eachMetaAggregate.origClass.getSimpleName());
            commandHandlers.add(aggregateAsCommandHandlerAsListener(eachMetaAggregate));
        }
        final Invokable router = akka.createBroadcaster(commandHandlers);
        return new CommandBus() {
            @Override
            public void submit(Object cmd) {
                log.info("Command {} received", cmd);
                router.invoke(cmd, null);
            }
        };
    }

    private void handleCmdByAggregate(MetaAggregate eachMetaAggregate, Object cmd) {
        for (MetaHandler eachMetaHandler : eachMetaAggregate.commandHandlers) {
            if (eachMetaHandler.expected.equals(cmd.getClass())) {
                Object aggregateId = eachMetaHandler.extractId(cmd);
                if (aggregates.containsKey(aggregateId)) {
                    Object aggregate = aggregates.get(aggregateId);
                    Collection<?> events = EventorCollections.toCollection(eachMetaHandler.execute(aggregate, cmd));
                    for (Object eachEvent : events) {
                        handleEventByAggregate(aggregate, eachEvent);
                    }
                    for (Object eachEvent : events) {
                        eventBus.publish(eachEvent);
                    }
                }
            }
        }
    }

    private void handleEventByEvenHandler(MetaSubscriber eventListener, Object event) {
        for (MetaHandler eachMetaHandler : eventListener.eventHandlers) {
            if (eachMetaHandler.expected.equals(event.getClass())) {
                eachMetaHandler.execute(instanceCreator.getInstanceOf(eventListener.origClass), event);
            }
        }
    }

    private void handleEventByAggregate(Object aggregate, Object event) {
        Class<?> actualClass = aggregate.getClass();
        for (MetaAggregate metaAggregate : info.aggregates) {
            if (metaAggregate.origClass.equals(actualClass)) {
                handleEventByAggregate(metaAggregate, event);
            }
        }
    }

    private void handleEventByAggregate(final MetaAggregate eachMetaAggregate, final Object event) {
        for (final MetaHandler eachMetaHandler : eachMetaAggregate.eventHandlers) {
            if (eachMetaHandler.expected.equals(event.getClass())) {
                if (eachMetaHandler.alwaysStart) {
                    Object aggregate = instanceCreator.newInstanceOf(eachMetaAggregate.origClass);
                    handleEventByAggregate(aggregate, eachMetaHandler, event);
                    Object id = eachMetaAggregate.retrieveId(aggregate);
                    EventorPreconditions.assume(!aggregates.containsKey(id),
                            "Could not create aggregate with duplicate id [%s] on event [%s]",
                            id, event);
                    aggregates.put(id, aggregate);
                    log.info("Aggregate with id {} registered", id);
                } else {
                    for (Object aggregate : aggregates.values()) {
                        handleEventByAggregate(aggregate, eachMetaHandler, event);
                    }
                }
            }
        }
    }

    private void handleEventByAggregate(Object aggregate, MetaHandler eachMetaHandler, Object event) {
        Collection<?> res = eachMetaHandler.execute(aggregate, event);
        if (!res.isEmpty()) throw new RuntimeException("Void expected");
    }

    private Listener aggregateAsCommandHandlerAsListener(final MetaAggregate eachMetaAggregate) {
        return new Listener() {
            @Override
            public void apply(Object cmd) {
                log.info("Command {} will be handled by aggregates", cmd);
                handleCmdByAggregate(eachMetaAggregate, cmd);
            }
        };
    }

    private Listener eventListenerAsListener(final MetaSubscriber each) {
        return new Listener() {
            @Override
            public void apply(Object event) {
                log.info("Event {} will be handled by event listener {}", event, each.origClass.getSimpleName());
                handleEventByEvenHandler(each, event);
            }
        };
    }

    private Listener aggregateAsListener(final MetaAggregate eachMetaAggregate) {
        return new Listener() {
            @Override
            public void apply(Object event) {
                log.info("Event {} will be handled by aggregates", event);
                handleEventByAggregate(eachMetaAggregate, event);
            }
        };
    }

    @Override
    public void submit(Object cmd) {
        commandBus.submit(cmd);
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
