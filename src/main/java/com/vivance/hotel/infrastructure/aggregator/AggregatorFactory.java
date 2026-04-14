package com.vivance.hotel.infrastructure.aggregator;

import com.vivance.hotel.domain.enums.AggregatorType;
import com.vivance.hotel.exception.AggregatorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory that resolves the correct {@link HotelAggregatorService} by {@link AggregatorType}.
 *
 * <p>All beans implementing {@link HotelAggregatorService} are auto-discovered via Spring
 * constructor injection. Adding a new aggregator requires only:
 * <ol>
 *   <li>Implementing {@link HotelAggregatorService}</li>
 *   <li>Annotating the new class with {@code @Service}</li>
 * </ol>
 * No changes to this factory are needed.
 */
@Slf4j
@Component
public class AggregatorFactory {

    private final Map<AggregatorType, HotelAggregatorService> registry;

    public AggregatorFactory(List<HotelAggregatorService> aggregators) {
        this.registry = aggregators.stream()
                .collect(Collectors.toMap(
                        HotelAggregatorService::getAggregatorType,
                        Function.identity()
                ));
        log.info("Registered aggregators: {}", registry.keySet());
    }

    /**
     * Retrieves the aggregator service for the given type.
     *
     * @param type aggregator type
     * @return the matching service
     * @throws AggregatorException if no service is registered for the given type
     */
    public HotelAggregatorService getAggregator(AggregatorType type) {
        HotelAggregatorService service = registry.get(type);
        if (service == null) {
            throw new AggregatorException("No aggregator registered for type: " + type);
        }
        return service;
    }

    public List<HotelAggregatorService> getAggregators(List<AggregatorType> types) {
        return types.stream()
                .map(this::getAggregator)
                .toList();
    }

    public boolean isRegistered(AggregatorType type) {
        return registry.containsKey(type);
    }
}
