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

import com.github.wingsofovnia.reppy.api.Repository;
import com.github.wingsofovnia.reppy.api.RepositoryException;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


public class JpaRepository<T> extends Observable implements Repository<T> {
    final EntityManager entityManager;
    final Class<T> entityClass;

    public JpaRepository(EntityManager entityManager, Class<T> entityClass) {
        this.entityManager = Objects.requireNonNull(entityManager);
        this.entityClass = entityClass;
    }

    @Override
    public void add(T subject) {
        Objects.requireNonNull(subject, "Repository is not suitable for null objects");
        try {
            transaction(() -> entityManager.persist(subject));

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
            transaction(() -> {
                subjects.forEach(entityManager::persist);
            });

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
            transaction(() -> {
                entityManager.remove(entityManager.contains(subject) ? subject : entityManager.merge(subject));
            });

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
        transaction(() -> {
            try {
                subjects.forEach(s -> {
                    entityManager.remove(entityManager.contains(s) ? s : entityManager.merge(s));
                    counter.incrementAndGet();

                    setChanged();
                    notifyObservers();
                });
            } catch (Exception e) {
                throw new RepositoryException("Failed to remove objects: " + subjects.toString(), e);
            }
        });
        return counter.get();
    }

    @Override
    public boolean contains(T subject) {
        Objects.requireNonNull(subject, "Repository is not suitable for null objects");

        final EntityType<T> entityType = entityManager.getMetamodel().entity(entityClass);
        final Set<SingularAttribute<? super T, ?>> singularAttributes = entityType.getSingularAttributes();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = cb.createQuery(entityClass);
        Root<T> selectTable = criteriaQuery.from(entityClass);

        Predicate[] restrictions = singularAttributes.stream()
                .filter(attr -> attr.getPersistentAttributeType() == PersistentAttributeType.BASIC)
                .map(attr -> {
                    try {
                        final Field field = (Field) attr.getJavaMember();
                        field.setAccessible(true);
                        Optional<Object> fieldValue = Optional.ofNullable(field.get(subject));

                        if (fieldValue.isPresent())
                            return cb.equal(selectTable.get(attr), fieldValue.get());

                        return null;
                    } catch (IllegalAccessException e) {
                        throw new RepositoryException(e);
                    }
                }).filter(v -> v != null).toArray(Predicate[]::new);

        criteriaQuery.select(selectTable);
        criteriaQuery.where(cb.and(restrictions));
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
        try {
            transaction(() -> {
                CriteriaBuilder builder = entityManager.getCriteriaBuilder();
                CriteriaDelete<T> query = builder.createCriteriaDelete(entityClass);
                query.from(entityClass);
                entityManager.createQuery(query).executeUpdate();

                entityManager.clear();
            });

            setChanged();
            notifyObservers();
        } catch (Exception e) {
            throw new RepositoryException("Failed to clear repository of " + entityClass.getName() + " objects.", e);
        }
    }

    @Override
    public Iterator<T> iterator() {
        JpaIterator<T> iterator = new JpaIterator<>(entityClass);
        this.addObserver(iterator);
        return iterator;
    }

    private class JpaIterator<E> implements Iterator<E>, Observer {
        private static final int ROWS_PER_PAGE = 32;

        private List<E> page;
        private long pageIndex;
        private long pageRowIndex;
        private final long totalRows;
        private final Class<E> entityClass;

        private boolean isConcurrentModification = false;

        JpaIterator(Class<E> entityClass) {
            this.entityClass = Objects.requireNonNull(entityClass);
            this.totalRows = size();
            this.page = retrievePage(Math.toIntExact(pageIndex * ROWS_PER_PAGE), ROWS_PER_PAGE);
        }

        @Override
        public boolean hasNext() {
            boolean hasNextIndex = pageIndex * ROWS_PER_PAGE + pageRowIndex < totalRows;
            if (!hasNextIndex)
                deleteObserver(this);

            return hasNextIndex;
        }

        @Override
        public E next() {
            if (this.isConcurrentModification)
                throw new ConcurrentModificationException();
            if (!hasNext())
                throw new NoSuchElementException();

            return getCurrentPage().get(Math.toIntExact(pageRowIndex++));
        }

        @Override
        public void update(Observable o, Object arg) {
            this.isConcurrentModification = true;
        }

        private List<E> getCurrentPage() {
            if (page.size() > pageRowIndex)
                return this.page;

            this.pageIndex++;
            this.pageRowIndex = 0;
            this.page = retrievePage(Math.toIntExact(pageIndex * ROWS_PER_PAGE), ROWS_PER_PAGE);

            return this.page;
        }

        private List<E> retrievePage(int offset, int amount) {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(entityClass);
            Root<E> from = criteriaQuery.from(entityClass);

            CriteriaQuery<E> select = criteriaQuery.select(from);
            TypedQuery<E> typedQuery = entityManager.createQuery(select);
            typedQuery.setFirstResult(Math.toIntExact(offset));
            typedQuery.setMaxResults(amount);

            return typedQuery.getResultList();
        }
    }

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliterator(iterator(), size(),
                Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE |
                        Spliterator.NONNULL | Spliterator.SIZED);
    }

    private void transaction(Runnable action) {
        entityManager.getTransaction().begin();
        action.run();
        entityManager.getTransaction().commit();
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }
}
