package com.travelmate.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String plain = "Travelmate@123";
        String hashDB = "$2a$10$LWK.MjfPDjseZlKsKCRimewXrQtJTHbYC4v8IwwiS0o27Hbqr1Kxq";
        String myHash = encoder.encode(plain);

        System.out.println("Does DB hash match? " + encoder.matches(plain, hashDB));
        System.out.println("My hash for Travelmate@123: " + myHash);
    }
}
