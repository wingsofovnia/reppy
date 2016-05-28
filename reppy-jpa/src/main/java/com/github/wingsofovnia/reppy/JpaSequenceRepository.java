package com.github.wingsofovnia.reppy;

import com.github.wingsofovnia.reppy.api.RepositoryException;
import com.github.wingsofovnia.reppy.api.SequenceRepository;
import com.github.wingsofovnia.reppy.api.Specification;

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

    @Override
    public Optional<T> get(Specification<T> specification) {
        return getAll(specification).findFirst();
    }

    @Override
    public Stream<T> getAll() {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
        Root<T> root = criteriaQuery.from(entityClass);

        return entityManager.createQuery(criteriaQuery.select(root)).getResultList().stream();
    }

    @Override
    public Stream<T> getAll(Specification<T> specification) {
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
