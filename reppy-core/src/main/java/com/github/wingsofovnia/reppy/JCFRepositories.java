package com.github.wingsofovnia.reppy;

import com.github.wingsofovnia.reppy.api.Repository;
import com.github.wingsofovnia.reppy.api.SequenceRepository;
import com.github.wingsofovnia.reppy.api.Specification;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class JCFRepositories {
    private JCFRepositories() {
        throw new AssertionError("No instance for you");
    }

    public static <T> Repository<T> from(Collection<T> collection) {
        return new CollectionRepository<>(collection);
    }

    public static <T> SequenceRepository<T, Integer> from(List<T> list) {
        return new ListSequenceRepository<>(list);
    }

    public static <T> Repository<T> from(Set<T> set) {
        return new CollectionRepository<>(set);
    }

    private static class CollectionRepository<T> implements Repository<T> {
        protected final Collection<T> collection;

        CollectionRepository(Collection<T> collection) {
            this.collection = collection;
        }

        @Override
        public void add(T subject) {
            collection.add(subject);
        }

        @Override
        public void addAll(Stream<T> subjects) {
            subjects.forEach(collection::add);
        }

        @Override
        public void remove(T subject) {
            collection.remove(subject);
        }

        @Override
        public int removeAll(Stream<T> subjects) {
            final AtomicInteger counter = new AtomicInteger(0);
            subjects.parallel().forEach(s -> {
                if (collection.remove(s)) counter.incrementAndGet();
            });

            return counter.get();
        }

        @Override
        public boolean contains(T subject) {
            return collection.contains(subject);
        }

        @Override
        public boolean containsAll(Stream<T> subjects) {
            return subjects.anyMatch(s -> !collection.contains(s));
        }

        @Override
        public long size() {
            return collection.size();
        }

        @Override
        public boolean isEmpty() {
            return collection.isEmpty();
        }

        @Override
        public void clear() {
            collection.clear();
        }

        @Override
        public Iterator<T> iterator() {
            return collection.iterator();
        }
    }

    private static class ListSequenceRepository<T> extends CollectionRepository<T> implements
            SequenceRepository<T, Integer> {
        ListSequenceRepository(List<T> list) {
            super(list);
        }

        @Override
        public Optional<T> get(Integer index) {
            return Optional.of(((List<T>) collection).get(index));
        }

        @Override
        public Optional<T> get(Specification<T> specification) {
            throw new NotImplementedException();
        }

        @Override
        public Stream<T> getAll(Specification<T> specification) {
            throw new NotImplementedException();
        }

        @Override
        public boolean remove(Integer index) {
            ((List<T>) collection).remove((int) index);
            return true;
        }
    }
}
