package com.github.wingsofovnia.reppy.api;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Specification {
    enum Operator {
        eq("="),
        ne("!="),
        le("<="),
        lt("<"),
        ge(">="),
        gt(">"),
        between("between"),
        in("in"),
        like("like");

        private String value;

        Operator(String operator) {
            this.value = operator;
        }

        public String getValue() {
            return value;
        }
    }

    class Criteria {
        Object key;
        Operator relation;
        Object value;

        public Criteria(Object key, Operator relation, Object value) {
            this.key = key;
            this.value = value;
            this.relation = relation;
        }

        public Object getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public Operator getRelation() {
            return relation;
        }
    }

    Collection<Criteria> criteria();

    default Specification merge(Specification source) {
        Collection<Criteria> sourceCriteria = source.criteria();
        Collection<Criteria> thisCriteria = this.criteria();
        return () -> Stream.concat(sourceCriteria.stream(), thisCriteria.stream())
                           .collect(Collectors.toSet());
    }

    static Specification of(Object key, Operator relation, Object value) {
        return () -> Collections.singletonList(new Criteria(key, relation, value));
    }
}
