# Reppy <img src="https://travis-ci.org/wingsofovnia/reppy.svg?branch=develop"> <img src="https://codecov.io/gh/wingsofovnia/reppy/branch/develop/graph/badge.svg" alt="Codecov" />
Reppy aims to reach simple and pure implementation of [Repository pattern](https://thinkinginobjects.com/2012/08/26/dont-use-dao-use-repository/). It based on 2 interfaces `Repository` and `SequenceRepository`, which mimic `java.util.Collection` and `java.util.List`respectively. As of today, the only implementation of Reppy's API is `reppy-jpa` with JPA. `Reppy-jpa` also implements Specifications pattern for building queries in more convenient way.

## Usage

```java
SequenceRepository<Entity, Long> srep = new JpaSequenceRepository<>(entityManager, Entity.class);
srep.addAll(new ArrayList<Entity>() {{
                add(new Entity(1));
                add(new Entity(2));
                add(new Entity(3));}});

Optional<Entity> e = srep.get(1L);
srep.remove(e.orElseThrow(() -> new EntityNotFoundException()));
if (!srep.isEmpty())
  System.out.println("Repository.size() = " + srep.size()); // 2
srep.stream().forEach(System.out::println); // Entity{x=2}, Entity{x=3}
```

```java
JpaSequenceRepository<Entity, Long> srep = new JpaSequenceRepository<>(entityManager, Entity.class);
srep.addAll(new ArrayList<Entity>() {{
                add(new Entity(1));
                add(new Entity(2));
                add(new Entity(3));}});

JpaSpecification<Entity> eq1 = JpaSpecifications.of(Entity.class).eq("x", 1)
JpaSpecification<Entity> eq1 = JpaSpecifications.of(Entity.class).eq("x", 2)
JpaSpecification<Entity> eq1or3 = eq1.or(eq2);
assertEquals(entityCollection.subList(0, 2), srep.get(eq1or3).collet(Collectors.toList())); // true
```

## Bugs and Feedback
For bugs, questions and discussions please use the [Github Issues](https://github.com/wingsofovnia/reppy/issues).

## License
Except as otherwise noted this software is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
