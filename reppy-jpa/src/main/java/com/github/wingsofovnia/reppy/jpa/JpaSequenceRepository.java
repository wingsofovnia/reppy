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

import com.github.wingsofovnia.reppy.api.RepositoryException;
import com.github.wingsofovnia.reppy.api.SequenceRepository;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class JpaSequenceRepository<T, ID extends Serializable> extends JpaRepository<T>
        implements SequenceRepository<T, ID> {

    public JpaSequenceRepository(EntityManager entityManager, Class<T> entityClass) {
        super(entityManager, entityClass);
    }

    @Override
    public Optional<T> get(ID index) {
        Objects.requireNonNull(index, "Cannot retrieve object by null identifier");
        return Optional.ofNullable(entityManager.find(entityClass, index));
    }

    public Optional<T> get(JpaSpecification<T> specification) {
        return getAll(specification).findFirst();
    }

    @Override
    public Stream<T> getAll() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
        Root<T> root = criteriaQuery.from(entityClass);

        return entityManager.createQuery(criteriaQuery.select(root)).getResultList().stream();
    }

    public Stream<T> getAll(JpaSpecification<T> specification) {
        Objects.requireNonNull(specification, "Cannot retrieve object by null specification");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);

        Root<T> rawRoot = criteriaQuery.from(entityClass);
        Predicate predicate = specification.toPredicate(rawRoot, criteriaQuery, criteriaBuilder);
        CriteriaQuery<T> root = criteriaQuery.where(predicate);

        return entityManager.createQuery(root).getResultList().stream();
    }

    @Override
    public void remove(ID index) {
        Objects.requireNonNull(index, "Cannot delete object by null index");

        Optional<T> objOpt = get(index);
        if (!objOpt.isPresent())
            throw new RepositoryException("Failed to find object by id #" + index.toString() + " while deletion");
        T obj = objOpt.get();

        remove(obj);
    }
}
