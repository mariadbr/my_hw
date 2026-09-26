package api.generators;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

public class RandomData {
    private RandomData() {}

//    public static String getUsername() {
//        return RandomStringUtils.randomAlphabetic(10);
//    }
//
//    public static String getPassword() {
//        return RandomStringUtils.randomAlphabetic(3).toUpperCase() +
//                RandomStringUtils.randomAlphabetic(5).toLowerCase() +
//                RandomStringUtils.randomNumeric(3) + "$@#";
//    }

    public static float getRandomPositiveFloat() {
        float min = 1.0f;
        float max = 5000.0f;
        return min + new Random().nextFloat() * (max - min);
    }

    public static float getRandomNegativeFloat() {
        return -(1.0f + new Random().nextFloat() * 4999.0f);
    }

    public static long getRandomAccountId() {
        return Long.parseLong(RandomStringUtils.randomNumeric(6));
    }

    public static String getRandomValidName() {
        return RandomStringUtils.randomAlphabetic(1).toUpperCase() +
                RandomStringUtils.randomAlphabetic(3).toLowerCase() +
                " " + RandomStringUtils.randomAlphabetic(1).toUpperCase() +
                RandomStringUtils.randomAlphabetic(4).toLowerCase();
    }
}