package com.praktikum.database.testing.library.utils;

import com.github.javafaker.Faker;
import java.util.Locale;

public class IndonesianFakerHelper {
    private static final Faker faker = new Faker(new Locale("id-ID"));
    private static final String[] INDONESIAN_EMAIL_DOMAINS = {
            "gmail.com", "yahoo.com", "outlook.com", "hotmail.com",
            "students.university.ac.id", "uin.ac.id", "itb.ac.id", "ui.ac.id",
            "undip.ac.id", "ugm.ac.id", "unair.ac.id", "ipb.ac.id",
            "company.co.id", "startup.id", "telkom.co.id", "bankmandiri.co.id"
    };

    public static String generateIndonesianName() {
        return faker.name().fullName();
    }

    public static String generateIndonesianEmail() {
        String username = faker.name().username()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .replaceAll("\\s+", "");
        String domain = INDONESIAN_EMAIL_DOMAINS[faker.random().nextInt(INDONESIAN_EMAIL_DOMAINS.length)];
        return username + "@" + domain;
    }

    public static String generateIndonesianPhone() {
        String[] prefixes = {"812", "813", "814", "815", "816", "817", "818", "819",
                "821", "822", "823", "851", "852", "853", "855", "856",
                "857", "858", "859", "877", "878", "879", "881", "882",
                "883", "884", "885", "886", "887", "888", "889", "895",
                "896", "897", "898", "899"};
        String prefix = prefixes[faker.random().nextInt(prefixes.length)];
        String firstPart = faker.number().digits(4);
        String secondPart = faker.number().digits(4);
        return "0" + prefix + "-" + firstPart + "-" + secondPart;
    }

    public static String generateIndonesianAddress() {
        return faker.address().fullAddress();
    }

    public static Faker getFaker() {
        return faker;
    }
}