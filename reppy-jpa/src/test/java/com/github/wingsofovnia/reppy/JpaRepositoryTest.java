package com.github.wingsofovnia.reppy;

import com.github.wingsofovnia.reppy.jpa.JpaRepository;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.service.ServiceRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import static org.junit.Assert.*;

public class JpaRepositoryTest extends JpaTest {
    private static EntityManagerFactory entityManagerFactory;
    private static JpaRepository<Entity> repository;

    @BeforeClass
    public static void initEntityManagerFactory() {
        entityManagerFactory = buildEntityManagerFactory();
    }

    @Before
    public void initJpaRepository() {
        repository = new JpaRepository<>(entityManagerFactory.createEntityManager(), Entity.class);
    }

    @After
    public void destroyJpaRepository() {
        repository.clear();
        repository.getEntityManager().close();
    }

    @Test
    public void jpaRepositoryClearTest() {
        assertEquals(0, repository.size());

        final Entity entity = new Entity(1);

        repository.add(entity);
        assertEquals(1, repository.size());

        repository.clear();
        assertEquals(0, repository.size());

        repository.add(entity);
        assertEquals(1, repository.size());

        repository.clear();
        assertEquals(0, repository.size());
    }

    @Test
    public void jpaRepositorySizeTest() {
        repository.add(new Entity(1));
        repository.add(new Entity(2));
        assertEquals(2, repository.size());
    }


    @Test
    public void jpaRepositoryIsEmptyTest() {
        assertTrue(repository.isEmpty());
        repository.add(new Entity(1));
        repository.add(new Entity(2));
        assertFalse(repository.isEmpty());
    }

    @Test
    public void jpaRepositoryAddTest() {
        repository.add(new Entity(1));
        repository.add(new Entity(2));
        assertEquals(2, repository.size());
        repository.clear();

        repository.addAll(new ArrayList<Entity>() {{
            add(new Entity(1));
            add(new Entity(2));
            add(new Entity(3));
        }});
        assertEquals(3, repository.size());
    }

    @Test
    public void jpaRepositoryRemoveTest() {
        repository.add(new Entity(1));

        Entity entityToRemove = new Entity(2);
        repository.add(entityToRemove);
        assertEquals(2, repository.size());

        repository.remove(entityToRemove);
        assertEquals(1, repository.size());
    }

    @Test
    public void jpaRepositoryRemoveAllTest() {
        List<Entity> removable = new ArrayList<Entity>() {{
            add(new Entity(1));
            add(new Entity(2));
            add(new Entity(3));
        }};
        repository.addAll(removable);
        assertEquals(3, repository.size());

        repository.removeAll(removable);
        assertEquals(0, repository.size());
    }

    @Test
    public void jpaRepositoryContainsTest() {
        Entity entityToCheck = new Entity(2, "12");
        Entity entityToCheckWithNull = new Entity(3);
        repository.add(entityToCheck);
        repository.add(entityToCheckWithNull);
        assertEquals(2, repository.size());

        assertTrue(repository.contains(entityToCheck));
        assertTrue(repository.contains(entityToCheckWithNull));

        repository.clear();
        assertFalse(repository.contains(entityToCheck));
        assertFalse(repository.contains(entityToCheckWithNull));
    }

    @Test
    public void jpaRepositoryContainsAllTest() {
        Collection<Entity> toCheckOnAvailability = new ArrayList<Entity>() {{
            add(new Entity(1));
            add(new Entity(2));
            add(new Entity(3));
        }};
        repository.addAll(toCheckOnAvailability);
        assertEquals(3, repository.size());

        assertTrue(repository.containsAll(toCheckOnAvailability));
    }

    @Test(expected = java.util.ConcurrentModificationException.class)
    public void jpnRepositoryIteratorConcurrentModificationTest() {
        Collection<Entity> entityCollection = new ArrayList<Entity>() {{
            add(new Entity(1));
            add(new Entity(2));
            add(new Entity(3));
        }};
        repository.addAll(entityCollection);

        try {
            Iterator<Entity> iterator = repository.iterator();
            repository.add(new Entity(12));
            iterator.next();
        } finally {
            repository.clear(); // junit dont run @After in exception was thrown. why?
        }
    }

    @Test
    public void jpnRepositoryIteratorTest() {
        List<Entity> entityCollection = new ArrayList<Entity>() {{
            add(new Entity(1));
            add(new Entity(2));
            add(new Entity(3));
            add(new Entity(4));
            add(new Entity(5));
            add(new Entity(6));
            add(new Entity(7));
            add(new Entity(8));
            add(new Entity(9));
            add(new Entity(10));
            add(new Entity(11));
        }};
        repository.addAll(entityCollection);

        int i = 0;
        for (Entity entity : repository) {
            Entity entityFromJC = entityCollection.get(i++);
            assertEquals("Obj from Repository is not equal to obj from Collection", entityFromJC, entity);
        }
        assertEquals(entityCollection.size(), i);
    }
}
