package Utility;

import Exceptions.IllegalRangeException;

import java.util.Date;
import java.util.Random;

public class RandomInRange {

    private static Random random = new Random(new Date().getTime() % 100);

    public static double random(int begin, int end) throws IllegalRangeException {
        if (begin > end) {
            throw new IllegalRangeException();
        }
        return random.nextInt(end) + begin;
    }

}
