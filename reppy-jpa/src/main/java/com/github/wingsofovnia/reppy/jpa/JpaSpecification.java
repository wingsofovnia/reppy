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

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Objects;

public interface JpaSpecification<T> {
    Predicate toPredicate(Root<T> root, CriteriaQuery query, CriteriaBuilder cb);

    default JpaSpecification<T> and(JpaSpecification<T> other) {
        Objects.requireNonNull(other, "Other specification must be not null");

        return (root, query, builder) -> {
            Predicate otherPredicate = other.toPredicate(root, query, builder);
            Predicate thisPredicate = JpaSpecification.this.toPredicate(root, query, builder);
            return builder.and(thisPredicate, otherPredicate);
        };
    }

    default JpaSpecification<T> or(JpaSpecification<T> other) {
        Objects.requireNonNull(other, "Other specification must be not null");

        return new JpaSpecification<T>() {
            public Predicate toPredicate(Root<T> root, CriteriaQuery query, CriteriaBuilder builder) {
                Predicate otherPredicate = other.toPredicate(root, query, builder);
                Predicate thisPredicate = JpaSpecification.this.toPredicate(root, query, builder);
                return builder.or(thisPredicate, otherPredicate);
            }
        };
    }

    default JpaSpecification<T> not() {
        return new JpaSpecification<T>() {
            public Predicate toPredicate(Root<T> root, CriteriaQuery query, CriteriaBuilder builder) {
                Predicate thisPredicate = JpaSpecification.this.toPredicate(root, query, builder);
                return builder.not(thisPredicate);
            }
        };
    }
}
