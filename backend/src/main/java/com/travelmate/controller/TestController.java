package com.travelmate.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * TestController - Controller dung de test xem backend co chay duoc khong.
 *
 * Day la buoc kiem tra co ban nhat:
 * - Ung dung Spring Boot khoi dong thanh cong
 * - Ket noi MySQL thanh cong
 * - API co the goi duoc tu browser hoac Postman
 *
 * Sau khi he thong on dinh, co the xoa controller nay di.
 *
 * Annotation giai thich:
 * - @RestController: danh dau day la REST controller, moi method tra ve JSON
 * - @RequestMapping("/api/test"): tat ca API trong controller nay bat dau bang /api/test
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * API test don gian nhat.
     * Goi: GET http://localhost:8080/api/test/hello
     * Ket qua mong doi: {"message": "TravelMate backend dang chay thanh cong!"}
     *
     * - @GetMapping: chi chap nhan request HTTP GET
     * - ResponseEntity: cho phep tuy chinh HTTP status code va body response
     */
    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        return ResponseEntity.ok(Map.of(
                "message", "TravelMate backend dang chay thanh cong!",
                "status", "OK"
        ));
    }

    /**
     * API test ket noi database.
     * Goi: GET http://localhost:8080/api/test/db
     *
     * Neu app khoi dong duoc thi co nghia la MySQL da ket noi thanh cong.
     * Vi Spring Boot se KHONG khoi dong duoc neu ket noi DB that bai.
     */
    @GetMapping("/db")
    public ResponseEntity<Map<String, String>> testDb() {
        return ResponseEntity.ok(Map.of(
                "message", "Ket noi MySQL thanh cong!",
                "database", "travelmate_db"
        ));
    }
}
