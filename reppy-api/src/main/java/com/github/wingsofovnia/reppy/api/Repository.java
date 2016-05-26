package com.github.wingsofovnia.reppy.api;

import java.util.Collection;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface Repository<T> extends Iterable<T> {

    void add(T subject);

    void addAll(Stream<T> subjects);

    default void addAll(Collection<T> subjects) {
        addAll(subjects.stream());
    }

    boolean remove(T subject);

    int removeAll(Stream<T> subjects);

    default int removeAll(Collection<T> subjects) {
        return removeAll(subjects.stream());
    }

    boolean contains(T subject);

    boolean containsAll(Stream<T> subjects);

    default boolean containsAll(Collection<T> subjects) {
        return containsAll(subjects.stream());
    }

    long size();

    boolean isEmpty();

    void clear();

    default Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
