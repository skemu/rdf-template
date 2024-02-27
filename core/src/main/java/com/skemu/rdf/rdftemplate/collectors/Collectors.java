package com.skemu.rdf.rdftemplate.collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Collectors {

    public static <T, K, U> Collector<T, ?, Map<K, U>> toUnmodifiableLinkedHashMap(
            Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
        Objects.requireNonNull(keyMapper, "keyMapper");
        Objects.requireNonNull(valueMapper, "valueMapper");
        return collectingAndThen(
                toMap(
                        keyMapper,
                        valueMapper,
                        (v1, v2) -> {
                            throw new AssertionError();
                        },
                        LinkedHashMap::new),
                Collections::unmodifiableMap);
    }
}
