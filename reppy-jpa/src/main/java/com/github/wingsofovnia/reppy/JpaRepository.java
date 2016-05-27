package com.github.wingsofovnia.reppy;

import com.github.wingsofovnia.reppy.api.Repository;
import com.github.wingsofovnia.reppy.api.RepositoryException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;


public class JpaRepository<T> extends Observable implements Repository<T> {
    protected final EntityManager entityManager;
    protected final Class<T> entityClass;

    public JpaRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager);
        this.entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    @Override
    public void add(T subject) {
        Objects.requireNonNull(subject, "Repository is not suitable for null objects");
        try {
            entityManager.persist(subject);

            setChanged();
            notifyObservers();
        } catch (Exception e) {
            throw new RepositoryException("Failed to add " + subject.toString() + " object.", e);
        }
    }

    @Override
    public void addAll(Stream<T> subjects) {
        Objects.requireNonNull(subjects, "Repository is not suitable for null objects");
        try {
            subjects.forEach(entityManager::persist);

            setChanged();
            notifyObservers();
        } catch (Exception e) {
            throw new RepositoryException("Failed to add objects: " + subjects.toString(), e);
        }
    }

    @Override
    public void remove(T subject) {
        Objects.requireNonNull(subject, "Unable to remove null object");
        try {
            entityManager.remove(entityManager.contains(subject) ? subject :
                    entityManager.merge(subject));

            setChanged();
            notifyObservers();
        } catch (Exception e) {
            throw new RepositoryException("Failed to remove object: " + subject.toString(), e);
        }
    }

    @Override
    public int removeAll(Stream<T> subjects) {
        Objects.requireNonNull(subjects, "Unable to remove null object");

        final AtomicInteger counter = new AtomicInteger(0);
        try {
            subjects.forEach(s -> {
                remove(s);
                counter.incrementAndGet();

                setChanged();
                notifyObservers();
            });
        } catch (Exception e) {
            throw new RepositoryException("Failed to remove objects: " + subjects.toString(), e);
        }
        return counter.get();
    }

    @Override
    public boolean contains(T subject) {
        Objects.requireNonNull(subject, "Repository is not suitable for null objects");

        final EntityType<T> entityType = entityManager.getMetamodel().entity(entityClass);
        final Set<SingularAttribute<? super T, ?>> singularAttributes =
                entityType.getSingularAttributes();

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
        Root<T> selectTable = criteriaQuery.from(entityClass);

        singularAttributes.forEach(attr -> {
            try {
                if (attr.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC) {
                    final Field field = (Field) attr.getJavaMember();
                    field.setAccessible(true);
                    criteriaQuery.where(criteriaBuilder
                            .equal(selectTable.get(attr), field.get(subject)));
                }
            } catch (IllegalAccessException e) {
                throw new RepositoryException(e);
            }
        });

        criteriaQuery.select(selectTable);
        return entityManager.createQuery(criteriaQuery).getResultList().size() > 0;
    }

    @Override
    public boolean containsAll(Stream<T> subjects) {
        Objects.requireNonNull(subjects, "Repository is not suitable for null objects");
        return subjects.allMatch(this::contains);
    }

    @Override
    public long size() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(entityClass);

        query.select(cb.count(root));
        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0L;
    }

    @Override
    public void clear() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaDelete<T> query = builder.createCriteriaDelete(entityClass);
        query.from(entityClass);

        try {
            entityManager.createQuery(query).executeUpdate();

            setChanged();
            notifyObservers();
        } catch (Exception e) {
            throw new RepositoryException(
                    "Failed to clear repository of " + entityClass.getCanonicalName() + " objects.",
                    e);
        }
    }

    @Override
    public Iterator<T> iterator() {
        JpaIterator<T> iterator = new JpaIterator<>(entityClass);
        this.addObserver(iterator);
        return iterator;
    }

    private class JpaIterator<E> implements Iterator<E>, Observer {
        private static final int ROWS_PER_PAGE = 100;

        private List<E> page;
        private long pageIndex;
        private long pageRowIndex;
        private final long totalRows;
        private final Class<E> entityClass;

        JpaIterator(Class<E> entityClass) {
            this.entityClass = Objects.requireNonNull(entityClass);
            this.totalRows = size();
        }

        @Override
        public boolean hasNext() {
            boolean hasNextIndex = pageIndex * ROWS_PER_PAGE + (pageIndex + 1) < totalRows;
            if (!hasNextIndex) deleteObserver(this);

            return hasNextIndex;
        }

        @Override
        public E next() {
            return getCurrentPage().get(Math.toIntExact(pageRowIndex++));
        }

        private List<E> getCurrentPage() {
            if (this.page != null && page.size() < pageRowIndex) return this.page;

            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(entityClass);
            Root<E> from = criteriaQuery.from(entityClass);

            CriteriaQuery<E> select = criteriaQuery.select(from);
            TypedQuery<E> typedQuery = entityManager.createQuery(select);
            typedQuery.setFirstResult(Math.toIntExact(pageIndex * ROWS_PER_PAGE));
            typedQuery.setMaxResults(ROWS_PER_PAGE);

            this.page = typedQuery.getResultList();
            this.pageIndex++;
            this.pageRowIndex = 0;

            return this.page;
        }

        @Override
        public void update(Observable o, Object arg) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliterator(iterator(), size(),
                Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE |
                        Spliterator.NONNULL | Spliterator.SIZED);
    }
}
