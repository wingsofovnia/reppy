package com.github.wingsofovnia.reppy.api;

import java.util.Objects;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public interface Specification<T> {
    Predicate toPredicate(Root<T> root, CriteriaQuery query, CriteriaBuilder cb);

    default Specification<T> and(Specification<T> other) {
        Objects.requireNonNull(other, "Other specification must be not null");

        return new Specification<T>() {
            public Predicate toPredicate(Root<T> root, CriteriaQuery query,
                                         CriteriaBuilder builder) {

                Predicate otherPredicate = other.toPredicate(root, query, builder);
                Predicate thisPredicate = this.toPredicate(root, query, builder);
                return builder.and(thisPredicate, otherPredicate);
            }
        };
    }

    default Specification<T> or(Specification<T> other) {
        Objects.requireNonNull(other, "Other specification must be not null");

        return new Specification<T>() {
            public Predicate toPredicate(Root<T> root, CriteriaQuery query,
                                         CriteriaBuilder builder) {

                Predicate otherPredicate = other.toPredicate(root, query, builder);
                Predicate thisPredicate = this.toPredicate(root, query, builder);
                return builder.or(thisPredicate, otherPredicate);
            }
        };
    }

    default Specification<T> not() {
        return new Specification<T>() {
            public Predicate toPredicate(Root<T> root, CriteriaQuery query,
                                         CriteriaBuilder builder) {
                Predicate thisPredicate = this.toPredicate(root, query, builder);
                return builder.not(thisPredicate);
            }
        };
    }
}