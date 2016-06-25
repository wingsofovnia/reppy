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

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.service.ServiceRegistry;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.util.logging.Level;

/**
 * Created by superuser on 25-Jun-16.
 */
public abstract class JpaTest {
    @javax.persistence.Entity
    public static class Entity {
        @Id
        private Integer x;
        private String s;

        public Entity() {}

        public Entity(int x, String s) {this.x = x;this.s = s;}
        public Entity(int x) {this.x = x;}
        public Integer getX() {return x;}
        public void setX(Integer x) {this.x = x;}
        public String getS() {return s;}
        public void setS(String s) {this.s = s;}
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entity)) return false;
            Entity entity = (Entity) o;
            if (getX() != null ? !getX().equals(entity.getX()) : entity.getX() != null) return false;
            return getS() != null ? getS().equals(entity.getS()) : entity.getS() == null;
        }
        public String toString() {return "Val{" + "x=" + x + ", s='" + s + '\'' + '}';}
    }

    public static EntityManagerFactory buildEntityManagerFactory() {
        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        configuration.setProperty("hibernate.connection.url", "jdbc:h2:~/test");
        configuration.setProperty("hibernate.connection.pool_size", "1");
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        configuration.setProperty("hibernate.cache.provider_class", "org.hibernate.cache.internal.NoCachingRegionFactory");
        configuration.setProperty("hibernate.hbm2ddl.auto", "create");
        configuration.setProperty("hibernate.connection.autocommit", "false");
        configuration.addAnnotatedClass(Entity.class);

        java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.OFF);

        StandardServiceRegistryBuilder serviceRegistryBuilder = new StandardServiceRegistryBuilder();
        serviceRegistryBuilder.applySettings(configuration.getProperties());
        ServiceRegistry serviceRegistry = serviceRegistryBuilder.build();

        return new EntityManagerFactoryImpl(PersistenceUnitTransactionType.RESOURCE_LOCAL, true, null,
                configuration, serviceRegistry, null);
    }
}
