package org.umlg.sqlg.predicate;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.PBiPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Postgres specific array data type operator to check if two arrays overlap.
 * https://www.postgresql.org/docs/9.6/functions-array.html
 */
public class ArrayOverlaps<T> implements PBiPredicate<T[], T[]> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArrayOverlaps.class);

    private final T[] values;

    public ArrayOverlaps(T[] values) {
        Set<T> uniqueValues = Arrays.stream(values).collect(Collectors.toSet());
        this.values = uniqueValues.toArray(values);
    }

    public P<T[]> getPredicate() {
        return new P<>(this, values);
    }

    public T[] getValues() {
        return values;
    }

    @Override
    public boolean test(T[] lhs, T[] rhs) {
        LOGGER.warn("Using Java implementation of && (array overlaps) instead of database");
        if (lhs.length == 0 && rhs.length == 0) {
            return true;
        }

        Set<T> lhsSet = new HashSet<>();
        for (T item : lhs) {
            lhsSet.add(item);
        }

        for (T item : rhs) {
            if (lhsSet.contains(item)) {
                return true;
            }
        }

        return false;
    }
}
