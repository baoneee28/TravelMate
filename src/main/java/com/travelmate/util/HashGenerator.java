package com.travelmate.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("Hash for Travelmate@123: " + encoder.encode("Travelmate@123"));
        System.out.println("Match Travelmate@123: " + encoder.matches("Travelmate@123", "$2a$10$slYQmyNdgzFoDeloNFfkA.62Yq3G8lGiYpDPH5vOMEZj8bm3MRJIG"));
    }
}
