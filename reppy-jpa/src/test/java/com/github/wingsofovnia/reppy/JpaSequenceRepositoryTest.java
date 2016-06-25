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

package com.github.wingsofovnia.reppy;

import com.github.wingsofovnia.reppy.jpa.*;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.persistence.EntityManagerFactory;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class JpaSequenceRepositoryTest extends JpaTest {
    private static EntityManagerFactory entityManagerFactory;
    private static JpaSequenceRepository<Entity, Integer> repository;

    @BeforeClass
    public static void initEntityManagerFactory() {
        entityManagerFactory = buildEntityManagerFactory();
    }

    @Before
    public void initJpaRepository() {
        repository = new JpaSequenceRepository<>(entityManagerFactory.createEntityManager(), Entity.class);
    }

    @After
    public void destroyJpaRepository() {
        repository.clear();
        repository.getEntityManager().close();
    }

    @Test
    public void jpaSRepositoryGetSpecTest() {
        List<Entity> entityCollection = new ArrayList<Entity>() {{
            add(new Entity(1, "test"));
            add(new Entity(2));
            add(new Entity(3));
            add(new Entity(4));
        }};
        repository.addAll(entityCollection);

        Optional<Entity> entityEq1 = repository.get(JpaSpecifications.of(Entity.class).eq("x", 1)).findFirst();
        assertTrue(entityEq1.isPresent());
        assertEquals(entityCollection.get(0), entityEq1.get());

        List<Entity> entitiesBtw1n3 = repository.get(JpaSpecifications.of(Entity.class).between("x", 1, 3))
                .collect(Collectors.toList());
        assertEquals(entityCollection.subList(0, 3), entitiesBtw1n3);

        Optional<Entity> entityLike1 = repository.get(JpaSpecifications.of(Entity.class).like("s", "te%")).findFirst();
        assertTrue(entityLike1.isPresent());
        assertEquals(entityCollection.get(0), entityLike1.get());
    }

    @Test
    public void jpaSRepositoryGetTransformedSpecTest() {
        List<Entity> entityCollection = new ArrayList<Entity>() {{
            add(new Entity(1, "test"));
            add(new Entity(2));
            add(new Entity(3));
            add(new Entity(4));
        }};
        repository.addAll(entityCollection);

        JpaSpecification<Entity> eq1 = JpaSpecifications.of(Entity.class).eq("x", 1);
        JpaSpecification<Entity> notEq1 = eq1.not();
        assertEquals(entityCollection.subList(1, 4), repository.get(notEq1).collect(Collectors.toList()));

        JpaSpecification<Entity> eq2 = JpaSpecifications.of(Entity.class).eq("x", 2);
        assertEquals(entityCollection.get(1), repository.get(eq2).findFirst().get());

        JpaSpecification<Entity> eq1or2 = eq1.or(eq2);
        assertEquals(entityCollection.subList(0, 2), repository.get(eq1or2).collect(Collectors.toList()));

        JpaSpecification<Entity> eqTest = JpaSpecifications.of(Entity.class).eq("s", "test");
        JpaSpecification<Entity> eq1nTest = eq1.and(eqTest);
        assertEquals(entityCollection.get(0), repository.get(eq1nTest).findFirst().get());
    }
}
