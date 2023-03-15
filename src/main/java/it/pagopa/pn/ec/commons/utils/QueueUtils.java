package it.pagopa.pn.ec.commons.utils;

import java.util.Random;

public class QueueUtils {


    public static String generateMessageGroupId() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 64;
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static void main(String[] args) {
        System.out.println(generateMessageGroupId());
        System.out.println(generateMessageGroupId());
    }

}
