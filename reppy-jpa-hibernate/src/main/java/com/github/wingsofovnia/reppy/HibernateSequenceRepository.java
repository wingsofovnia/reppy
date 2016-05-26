package com.github.wingsofovnia.reppy;

import com.github.wingsofovnia.reppy.api.RepositoryException;
import com.github.wingsofovnia.reppy.api.SequenceRepository;
import com.github.wingsofovnia.reppy.api.Specification;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import java.io.Closeable;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;


public class HibernateSequenceRepository<T, ID extends Serializable> implements SequenceRepository<T, ID> {
    private static final int ITERATOR_FETCH_SIZE = 256;

    private final EntityManager entityManager;
    private final Class<T> entityClass;

    public HibernateSequenceRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager);
        this.entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @Override
    public Optional<T> get(ID index) {
        return Optional.ofNullable(entityManager.find(entityClass, Objects.requireNonNull(index)));
    }

    @Override
    public Optional<T> get(Specification specification) {
        return getAll(specification).findFirst();
    }

    @Override
    public Stream<T> getAll(Specification specification) {
        Objects.requireNonNull(specification);

        Session hibSession = entityManager.unwrap(Session.class);

        Criteria criteria = hibSession.createCriteria(entityClass);
        specification.criteria().stream().forEach(criterion -> {
            String attribute = String.valueOf(criterion.getKey());
            Specification.Operator operator = criterion.getRelation();
            Object value = criterion.getValue();

            switch (operator) {
                case eq:
                    criteria.add(Restrictions.eq(attribute, value));
                    break;
                case ne:
                    criteria.add(Restrictions.ne(attribute, value));
                    break;
                case le:
                    criteria.add(Restrictions.le(attribute, value));
                    break;
                case lt:
                    criteria.add(Restrictions.lt(attribute, value));
                    break;
                case ge:
                    criteria.add(Restrictions.ge(attribute, value));
                    break;
                case gt:
                    criteria.add(Restrictions.gt(attribute, value));
                    break;
                case between:
                    Object[] values = (Object[]) value;
                    if (values.length < 2)
                        throw new IllegalArgumentException("Not enough params for between query");

                    criteria.add(Restrictions.between(attribute, values[0], values[1]));
                    break;
                case in:
                    criteria.add(Restrictions.in(attribute, value));
                    break;
                case like:
                    criteria.add(Restrictions.like(attribute, value));
                    break;
                default:
                    throw new RepositoryException("Failed to detect spec operator");
            }
        });

        return ((List<T>) criteria.list()).stream();
    }

    @Override
    public void add(T subject) {
        Objects.requireNonNull(subject);

        try {
            entityManager.persist(subject);
        } catch (Exception e) {
            throw new RepositoryException("Failed to add " + subject.toString() + " object.", e);
        }
    }

    @Override
    public void addAll(Stream<T> subjects) {
        Objects.requireNonNull(subjects);

        try {
            subjects.forEach(entityManager::persist);
        } catch (Exception e) {
            throw new RepositoryException("Failed to persist objects: " + subjects.toString(), e);
        }
    }

    @Override
    public boolean remove(T subject) {
        Objects.requireNonNull(subject);

        try {
            entityManager.remove(entityManager.contains(subject) ? subject : entityManager.merge(subject));
            return true;
        } catch (Exception e) {
            throw new RepositoryException("Failed to remove object: " + subject.toString(), e);
        }
    }

    @Override
    public boolean remove(ID index) {
        Objects.requireNonNull(index);

        Optional<T> objOpt = get(index);
        if (!objOpt.isPresent())
            return false;
        T obj = objOpt.get();

        return remove(obj);
    }

    @Override
    public int removeAll(Stream<T> subjects) {
        Objects.requireNonNull(subjects);

        final AtomicInteger counter = new AtomicInteger(0);
        try {
            subjects.forEach(s -> {
                if (remove(s))
                    counter.incrementAndGet();
            });
        } catch (Exception e) {
            throw new RepositoryException("Failed to remove objects: " + subjects.toString(), e);
        }
        return counter.get();
    }

    @Override
    public boolean contains(T subject) {
        final EntityType<T> entityType = entityManager.getMetamodel().entity(entityClass);
        final Set<SingularAttribute<? super T, ?>> singularAttributes = entityType.getSingularAttributes();

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
        Root<T> selectTable = criteriaQuery.from(entityClass);

        singularAttributes.forEach(attr -> {
            try {
                if (attr.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC) {
                    final Field field = (Field) attr.getJavaMember();
                    field.setAccessible(true);
                    criteriaQuery.where(criteriaBuilder.equal(selectTable.get(attr), field.get(subject)));
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
        return Objects.requireNonNull(subjects).allMatch(this::contains);
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
        } catch (Exception e) {
            throw new RepositoryException("Failed to clear repository of " + entityClass.getCanonicalName() + " objects.", e);
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new HibernateIterator<>(entityManager, entityClass, ITERATOR_FETCH_SIZE);
    }

    @Override
    public Stream<T> stream() {
        final HibernateIterator<T> iterator = (HibernateIterator<T>) iterator();
        return StreamSupport.stream(spliterator(iterator), false).onClose(iterator::close);
    }

    private static final class HibernateIterator<T> implements Iterator<T>, Closeable {
        private final Class<T> entityClass;
        private final ScrollableResults results;

        private HibernateIterator(EntityManager entityManager, Class<T> entityClass, int fetchSize) {
            this.entityClass = entityClass;

            final Session session = entityManager.unwrap(Session.class);
            this.results = session.createCriteria(entityClass)
                                  .setFetchSize(fetchSize)
                                  .setReadOnly(true)
                                  .scroll(ScrollMode.FORWARD_ONLY);
        }

        @Override
        public boolean hasNext() {
            return results.next();
        }

        @Override
        public T next() {
            return entityClass.cast(results.get(0));
        }

        @Override
        public void close() {
            results.close();
        }
    }

    @Override
    public Spliterator<T> spliterator() {
        return spliterator(iterator());
    }

    private Spliterator<T> spliterator(Iterator<T> iterator) {
        return Spliterators.spliterator(iterator, size(),
                Spliterator.ORDERED | Spliterator.DISTINCT |
                        Spliterator.NONNULL | Spliterator.CONCURRENT | Spliterator.IMMUTABLE);
    }
}