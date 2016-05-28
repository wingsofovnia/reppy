package com.github.wingsofovnia.reppy;

import com.github.wingsofovnia.reppy.api.Specification;

import java.util.Collection;

public class Specifications {
    private Specifications() {
        throw new AssertionError("No instance for you, man!");
    }

    public static <T, Y> Specification<T> eq(String key, Y value) {
        return (root, query, cb) -> cb.equal(root.get(key), value);
    }

    public static <T, Y> Specification<T> ne(String key, Y value) {
        return (root, query, cb) -> cb.notEqual(root.get(key), value);
    }

    public static <T, Y extends Comparable<? super Y>> Specification<T> le(String key, Y value) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(key), value);
    }

    public static <T, Y extends Comparable<? super Y>> Specification<T> lt(String key, Y value) {
        return (root, query, cb) -> cb.lessThan(root.get(key), value);
    }

    public static <T, Y extends Comparable<? super Y>> Specification<T> ge(String key, Y value) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(key), value);
    }

    public static <T, Y extends Comparable<? super Y>> Specification<T> gt(String key, Y value) {
        return (root, query, cb) -> cb.greaterThan(root.get(key), value);
    }

    public static <T, Y extends Comparable<? super Y>> Specification<T> between(String key, Y v1, Y v2) {
        return (root, query, cb) -> cb.between(root.get(key), v1, v2);
    }

    public static <T, Y> Specification<T> in(String key, Collection<Y> values) {
        return (root, query, cb) -> root.get(key).in(values);
    }

    public static <T> Specification<T> like(String key, String pattern) {
        return (root, query, cb) -> cb.like(root.get(key), pattern);
    }
}
