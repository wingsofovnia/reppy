package com.github.wingsofovnia.reppy.api;

import java.io.Serializable;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;

public interface SequenceRepository<T, ID extends Serializable> extends Repository<T> {
    Optional<T> get(ID index);

    Stream<T> getAll();

    void remove(ID index);

    @Override
    default Spliterator<T> spliterator() {
        return Spliterators.spliterator(iterator(), size(), Spliterator.ORDERED);
    }
}
