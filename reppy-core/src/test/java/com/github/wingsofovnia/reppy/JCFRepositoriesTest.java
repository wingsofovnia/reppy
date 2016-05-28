package com.github.wingsofovnia.reppy;

import com.github.wingsofovnia.reppy.api.SequenceRepository;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;

public class JCFRepositoriesTest {

    private Integer[] randomArray;

    @Before
    public void init() {
        this.randomArray = new Random().ints(50).boxed().toArray(Integer[]::new);
    }

    @Test
    public void listRepositoryTest() {
        List<Integer> arrayList = new ArrayList<>(Arrays.asList(randomArray));
        SequenceRepository<Integer, Integer> sequenceRepository = JCFRepositories.from(arrayList);

        assertEquals(arrayList.size(), arrayList.size());

        int val = ThreadLocalRandom.current().nextInt();
        arrayList.add(val);
        sequenceRepository.add(val);
        assertEquals(arrayList.contains(val), sequenceRepository.contains(val));

        for (int i = 0; i < arrayList.size(); i++) {
            assertEquals(arrayList.get(i), sequenceRepository.get(i).get());
        }
    }
}
