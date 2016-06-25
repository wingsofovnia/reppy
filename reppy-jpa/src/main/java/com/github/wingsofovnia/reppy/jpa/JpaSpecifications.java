/*
 * (C) Copyright 2016 Reppy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.wingsofovnia.reppy.jpa;

import java.util.Collection;

public class JpaSpecifications<T> {
    private JpaSpecifications() {}

    public static <E> JpaSpecifications<E> of(Class<E> etity) {
        return new JpaSpecifications<>();
    }


    public <Y> JpaSpecification<T> eq(String key, Y value) {
        return (root, query, cb) -> cb.equal(root.get(key), value);
    }

    public <Y> JpaSpecification<T> ne(String key, Y value) {
        return (root, query, cb) -> cb.notEqual(root.get(key), value);
    }

    public <Y extends Comparable<? super Y>> JpaSpecification<T> le(String key, Y value) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(key), value);
    }

    public <Y extends Comparable<? super Y>> JpaSpecification<T> lt(String key, Y value) {
        return (root, query, cb) -> cb.lessThan(root.get(key), value);
    }

    public <Y extends Comparable<? super Y>> JpaSpecification<T> ge(String key, Y value) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(key), value);
    }

    public <Y extends Comparable<? super Y>> JpaSpecification<T> gt(String key, Y value) {
        return (root, query, cb) -> cb.greaterThan(root.get(key), value);
    }

    public <Y extends Comparable<? super Y>> JpaSpecification<T> between(String key, Y v1, Y v2) {
        return (root, query, cb) -> cb.between(root.get(key), v1, v2);
    }

    public <Y> JpaSpecification<T> in(String key, Collection<Y> values) {
        return (root, query, cb) -> root.get(key).in(values);
    }

    public JpaSpecification<T> like(String key, String pattern) {
        return (root, query, cb) -> cb.like(root.get(key), pattern);
    }
}
