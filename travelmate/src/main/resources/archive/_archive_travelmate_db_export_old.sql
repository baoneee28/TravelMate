-- ⚠️  FILE NÀY LÀ ARCHIVE CŨ — KHÔNG DÙNG ĐỂ DEMO
-- File demo chính: src/main/resources/travelmate_db.sql
-- Archived: 2026-05-20
CREATE DATABASE IF NOT EXISTS travelmate_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE travelmate_db;

-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: travelmate_db
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `accommodations`
--

DROP TABLE IF EXISTS `accommodations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `accommodations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `thumbnail_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `star_rating` int DEFAULT NULL,
  `rating` double DEFAULT NULL,
  `review_count` int DEFAULT NULL,
  `property_type` enum('HOMESTAY','HOTEL','RESORT','VILLA') COLLATE utf8mb4_unicode_ci NOT NULL,
  `approval_status` enum('APPROVED','PENDING','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_accommodations_owner` (`owner_id`),
  CONSTRAINT `fk_accommodations_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `accommodations`
--

LOCK TABLES `accommodations` WRITE;
/*!40000 ALTER TABLE `accommodations` DISABLE KEYS */;
INSERT INTO `accommodations` VALUES (1,'LATA Hotel & Apartments','Kh├ích sß║ín hiß╗çn ─æß║íi nß║▒m ngay trung t├óm th├ánh phß╗æ ─É├á Lß║ít, c├ích chß╗ú ─æ├¬m ─É├á Lß║ít 500m. Ph├▓ng rß╗Öng r├úi, view ─æß║╣p, tiß╗çn nghi ─æß║ºy ─æß╗º.','15 Phan Bß╗Öi Ch├óu, Ph╞░ß╗¥ng 1','─É├á Lß║ít','https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&q=80',4,9.3,3,'HOTEL','APPROVED',3,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(2,'Tulip Hotel 2 Dalat','Kh├ích sß║ín 3 sao thiß║┐t kß║┐ phong c├ích Ch├óu ├éu, gß║ºn hß╗ô Xu├ón H╞░╞íng v├á V╞░ß╗¥n hoa th├ánh phß╗æ. Dß╗ïch vß╗Ñ tß║¡n t├¼nh, gi├í cß║ú phß║úi ch─âng.','56 B├╣i Thß╗ï Xu├ón, Ph╞░ß╗¥ng 2','─É├á Lß║ít','https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800&q=80',3,9,2,'HOTEL','APPROVED',3,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(3,'TravelMate Grand Hotel','Kh├ích sß║ín 5 sao sang trß╗ìng bß║¡c nhß║Ñt ─É├á Lß║ít, tß╗ìa lß║íc tr├¬n ─æß╗ôi th├┤ng vß╗¢i tß║ºm nh├¼n to├án cß║únh thung l┼⌐ng. Spa cao cß║Ñp, nh├á h├áng fine dining, hß╗ô b╞íi v├┤ cß╗▒c.','88 Nguyß╗àn Ch├¡ Thanh, Ph╞░ß╗¥ng 6','─É├á Lß║ít','https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800&q=80',5,10,1,'HOTEL','APPROVED',3,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(4,'The Anam Villa Nha Trang','Biß╗çt thß╗▒ nghß╗ë d╞░ß╗íng phong c├ích ─É├┤ng D╞░╞íng sang trß╗ìng tß╗ìa lß║íc ngay tr├¬n b├úi biß╗ân ri├¬ng Cam Ranh. Hß╗ô b╞íi private, butler ri├¬ng, view biß╗ân v├┤ cß╗▒c.','Nguyß╗àn Tß║Ñt Th├ánh, Cam L├óm','Nha Trang','https://images.unsplash.com/photo-1582268611958-ebfd161ef9cf?w=800&q=80',5,10,2,'VILLA','APPROVED',7,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(5,'Ba Na Hills Forest Villa','Villa bungalow giß╗»a rß╗½ng nguy├¬n sinh n├║i B├á N├á, thiß║┐t kß║┐ gß╗ù tß╗▒ nhi├¬n ß║Ñm ├íp. Gß║ºn c├íp treo d├ái nhß║Ñt thß║┐ giß╗¢i, kh├¡ hß║¡u m├ít mß║╗ quanh n─âm.','Km 20 Huyß╗çn H├▓a Vang','─É├á Nß║╡ng','https://images.unsplash.com/photo-1510798831971-661eb04b3739?w=800&q=80',4,8.7,3,'VILLA','APPROVED',7,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(6,'Hoa L╞░ Riverside Homestay','Nh├á d├ón truyß╗ün thß╗æng 3 gian m├íi ng├│i ven s├┤ng Thu Bß╗ôn, phß╗æ cß╗ò Hß╗Öi An chß╗ë 5 ph├║t ─æi bß╗Ö. Bß╗»a s├íng b├ính m├¼ Hß╗Öi An tß╗▒ l├ám, xe ─æß║íp miß╗àn ph├¡.','42 Nguyß╗àn Trung Trß╗▒c, Cß║⌐m Ch├óu','Hß╗Öi An','https://images.unsplash.com/photo-1586375300773-8384e3e4916f?w=800&q=80',3,10,3,'HOMESTAY','APPROVED',8,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(7,'Mß╗Öc Nhi├¬n Garden Homestay ─É├á Lß║ít','C─ân nh├á gß╗ù th├┤ng ─É├á Lß║ít phong c├ích Ph├íp cß╗ò giß╗»a v╞░ß╗¥n hoa d├ú quß╗│. L├▓ s╞░ß╗ƒi cß╗ºi, bß║┐p nß║Ñu chung, view ─æß╗ôi th├┤ng y├¬n t─⌐nh tuyß╗çt ─æß╗æi.','18 ─É╞░ß╗¥ng Vß║ín Kiß║┐p, Ph╞░ß╗¥ng 5','─É├á Lß║ít','https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&q=80',3,8.7,3,'HOMESTAY','APPROVED',8,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(8,'Vinpearl Resort & Spa Nha Trang','Khu nghß╗ë d╞░ß╗íng 5 sao tr├¬n ─æß║úo H├▓n Tre huyß╗ün thoß║íi, kß║┐t nß╗æi bß║▒ng c├íp treo v╞░ß╗út biß╗ân d├ái nhß║Ñt thß║┐ giß╗¢i. C├┤ng vi├¬n n╞░ß╗¢c, s├ón golf, casino, spa ─æß║│ng cß║Ñp quß╗æc tß║┐.','─Éß║úo H├▓n Tre, V─⌐nh Nguy├¬n','Nha Trang','https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800&q=80',5,9,2,'RESORT','APPROVED',4,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(9,'Furama Resort ─É├á Nß║╡ng','Khu nghß╗ë d╞░ß╗íng 5 sao h╞░ß╗¢ng biß╗ân Mß╗╣ Kh├¬ nß╗òi tiß║┐ng. Hß╗ô b╞íi n╞░ß╗¢c ngß╗ìt + muß╗æi, nh├á h├áng fine dining, spa phong c├ích ├ü ─É├┤ng, b├úi biß╗ân ri├¬ng 200m.','68 Hß╗ô Xu├ón H╞░╞íng, Mß╗╣ An','─É├á Nß║╡ng','https://images.unsplash.com/photo-1596436869741-a96b7c7d0a5b?w=800&q=80',5,10,1,'RESORT','APPROVED',4,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(10,'[PENDING DEMO] Da Lat Mountain Boutique Hotel','Kh├ích sß║ín boutique phong c├ích n├║i rß╗½ng, view thung l┼⌐ng ─É├á Lß║ít. Vß╗½a ─æ╞░ß╗úc partner ─æ─âng k├╜ ΓÇö chß╗¥ Admin duyß╗çt.','101 Triß╗çu Viß╗çt V╞░╞íng, Ph╞░ß╗¥ng 4','─É├á Lß║ít','https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=800&q=80',4,0,0,'HOTEL','PENDING',3,'2026-05-04 06:35:46.000000','2026-05-04 06:35:46.000000'),(11,'[REJECTED DEMO] Da Lat Fake Hotel','Listing bß╗ï Admin tß╗½ chß╗æi do th├┤ng tin kh├┤ng hß╗úp lß╗ç.','─Éß╗ïa chß╗ë kh├┤ng r├╡ r├áng','─É├á Lß║ít','',2,0,0,'HOTEL','REJECTED',3,'2026-05-02 07:35:46.000000','2026-05-02 07:35:46.000000');
/*!40000 ALTER TABLE `accommodations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bookings`
--

DROP TABLE IF EXISTS `bookings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bookings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_code` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `accommodation_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `check_in` date NOT NULL,
  `check_out` date NOT NULL,
  `adults` int DEFAULT '1',
  `children` int DEFAULT '0',
  `room_quantity` int DEFAULT '1',
  `customer_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `customer_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customer_email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_amount` decimal(15,0) NOT NULL,
  `paid_amount` decimal(15,0) NOT NULL,
  `remaining_amount` decimal(15,0) NOT NULL,
  `booking_status` enum('CANCELLED','CHECKED_IN','COMPLETED','CONFIRMED','NO_SHOW','PENDING_ADMIN_APPROVAL') COLLATE utf8mb4_unicode_ci NOT NULL,
  `payment_option` enum('DEPOSIT_30','FULL_PAYMENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `payment_status` enum('APPROVED','CANCELLED','DEPOSIT_FORFEITED','PENDING_ADMIN_APPROVAL','REJECTED','SUBMITTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `partner_status` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `voucher_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `discount_amount` decimal(15,0) DEFAULT NULL,
  `voucher_cost_bearer` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_before_discount` decimal(15,0) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `booking_code` (`booking_code`),
  KEY `fk_bookings_user` (`user_id`),
  KEY `fk_bookings_accommodation` (`accommodation_id`),
  KEY `fk_bookings_room` (`room_id`),
  CONSTRAINT `fk_bookings_accommodation` FOREIGN KEY (`accommodation_id`) REFERENCES `accommodations` (`id`),
  CONSTRAINT `fk_bookings_room` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_bookings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bookings`
--

LOCK TABLES `bookings` WRITE;
/*!40000 ALTER TABLE `bookings` DISABLE KEYS */;
INSERT INTO `bookings` VALUES (1,'BK-LATA-STD-0001',2,1,1,'2026-05-07','2026-05-09',2,0,1,'Nguyß╗àn V─ân An','0912 345 678','user@travelmate.vn',1300000,390000,910000,'PENDING_ADMIN_APPROVAL','DEPOSIT_30','PENDING_ADMIN_APPROVAL',NULL,NULL,NULL,0,NULL,0,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(2,'BK-TLP-SUP-0001',2,2,6,'2026-05-11','2026-05-13',2,0,1,'Nguyß╗àn V─ân An','0912 345 678','user@travelmate.vn',1240000,1240000,0,'PENDING_ADMIN_APPROVAL','FULL_PAYMENT','PENDING_ADMIN_APPROVAL',NULL,NULL,NULL,0,NULL,0,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(3,'BK-TMG-DLX-0001',2,3,9,'2026-05-05','2026-05-07',2,0,1,'Nguyß╗àn V─ân An','0912 345 678','user@travelmate.vn',2400000,720000,1680000,'CONFIRMED','DEPOSIT_30','APPROVED','PENDING_PARTNER_CONFIRMATION',NULL,NULL,0,NULL,0,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(4,'BK-LATA-DLX-0001',2,1,2,'2026-05-06','2026-05-08',2,1,1,'Nguyß╗àn V─ân An','0912 345 678','user@travelmate.vn',1700000,1700000,0,'CONFIRMED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Partner ─æ├ú x├íc nhß║¡n giß╗» ph├▓ng cho kh├ích.',NULL,0,NULL,0,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(5,'BK-TLP-STD-0001',2,2,5,'2026-05-02','2026-05-04',2,0,1,'Nguyß╗àn V─ân An','0912 345 678','user@travelmate.vn',960000,288000,672000,'NO_SHOW','DEPOSIT_30','DEPOSIT_FORFEITED',NULL,'Kh├ích kh├┤ng ─æß║┐n check-in. Cß╗ìc 30% bß╗ï giß╗» lß║íi theo ch├¡nh s├ích.',NULL,0,NULL,0,'2026-04-29 07:35:46.000000','2026-04-29 07:35:46.000000'),(6,'BK-TMG-PRE-0001',2,3,10,'2026-05-03','2026-05-06',2,0,1,'Nguyß╗àn V─ân An','0912 345 678','user@travelmate.vn',4950000,4950000,0,'CHECKED_IN','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Kh├ích ─æ├ú check-in. Partner ─æ├ú x├íc nhß║¡n giß╗» ph├▓ng.',NULL,0,NULL,0,'2026-04-27 07:35:46.000000','2026-04-27 07:35:46.000000'),(7,'BK-LATA-FAM-0001',2,1,3,'2026-04-24','2026-04-27',3,1,1,'Nguyß╗àn V─ân An','0912 345 678','user@travelmate.vn',3600000,3600000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Kh├ích ─æ├ú checkout. ─É╞ín ─æß║╖t ph├▓ng ho├án tß║Ñt. Ph├▓ng ─æ├ú ─æ╞░ß╗úc mß╗ƒ lß║íi.',NULL,0,NULL,0,'2026-04-22 07:35:46.000000','2026-04-22 07:35:46.000000'),(8,'BK-VNT-DLX-0001',2,8,25,'2026-05-09','2026-05-12',2,1,1,'Trß║ºn Thß╗ï B├¡ch','0988 111 222','user@travelmate.vn',8400000,2520000,5880000,'CONFIRMED','DEPOSIT_30','APPROVED','PENDING_PARTNER_CONFIRMATION',NULL,NULL,0,NULL,0,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(9,'BK-VNT-SUI-0001',2,8,26,'2026-05-07','2026-05-10',2,0,1,'L├¬ Minh ─Éß╗⌐c','0977 333 444','user@travelmate.vn',13500000,13500000,0,'CONFIRMED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Partner2 ─æ├ú x├íc nhß║¡n giß╗» ph├▓ng Junior Suite.',NULL,0,NULL,0,'2026-05-02 07:35:46.000000','2026-05-02 07:35:46.000000'),(10,'BK-MND-ATT-0001',2,7,23,'2026-04-26','2026-04-29',2,0,1,'Phß║ím Quß╗│nh Anh','0966 555 666','user@travelmate.vn',1740000,1740000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Kh├ích ─æ├ú checkout. ─É╞ín Homestay Mß╗Öc Nhi├¬n ho├án tß║Ñt.',NULL,0,NULL,0,'2026-04-24 07:35:46.000000','2026-04-24 07:35:46.000000'),(11,'BK-ANM-GDN-0001',2,4,13,'2026-05-14','2026-05-17',2,0,1,'Ho├áng V─ân H├╣ng','0911 777 888','user@travelmate.vn',10500000,3150000,7350000,'PENDING_ADMIN_APPROVAL','DEPOSIT_30','PENDING_ADMIN_APPROVAL',NULL,NULL,NULL,0,NULL,0,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(12,'BK-FDN-DLX-0001',2,9,28,'2026-05-01','2026-05-03',2,0,1,'Ng├┤ Thß╗ï Lan','0922 999 000','user@travelmate.vn',4800000,1440000,3360000,'NO_SHOW','DEPOSIT_30','DEPOSIT_FORFEITED','PARTNER_CONFIRMED','Kh├ích kh├┤ng ─æß║┐n check-in. Cß╗ìc 30% bß╗ï giß╗» lß║íi.',NULL,0,NULL,0,'2026-04-28 07:35:46.000000','2026-04-28 07:35:46.000000'),(13,'BK-HLR-DLX-0001',2,6,20,'2026-05-08','2026-05-10',2,0,1,'V┼⌐ Thß╗ï Mai','0955 123 456','user@travelmate.vn',960000,960000,0,'CONFIRMED','FULL_PAYMENT','APPROVED','PENDING_PARTNER_CONFIRMATION',NULL,NULL,0,NULL,0,'2026-05-04 07:35:46.000000','2026-05-04 07:35:46.000000'),(14,'BK-BNH-BNG-0001',2,5,16,'2026-05-03','2026-05-06',2,0,1,'─Éinh V─ân T├╣ng','0944 234 567','user@travelmate.vn',6600000,6600000,0,'CHECKED_IN','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Kh├ích ─æ├ú check-in tß║íi Ba Na Villa.',NULL,0,NULL,0,'2026-05-01 07:35:46.000000','2026-05-01 07:35:46.000000'),(15,'BK-LATA-STD-0002',2,1,1,'2026-04-19','2026-04-21',2,0,1,'Nguyß╗àn V─ân An','0912 345 678','user@travelmate.vn',1170000,1170000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','├üp dß╗Ñng voucher SUMMER10 giß║úm 10% (admin chß╗ïu).','SUMMER10',130000,'ADMIN',1300000,'2026-04-17 07:35:46.000000','2026-04-17 07:35:46.000000'),(16,'BK-VNT-DLX-0002',2,8,25,'2026-04-20','2026-04-22',2,0,1,'Trß║ºn Thß╗ï B├¡ch','0988 111 222','user@travelmate.vn',5600000,5600000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Vinpearl Deluxe 2 ─æ├¬m ΓÇö ho├án tß║Ñt.',NULL,0,NULL,0,'2026-04-18 07:35:46.000000','2026-04-18 07:35:46.000000'),(17,'BK-LATA-DLX-0002',2,1,2,'2026-04-12','2026-04-15',2,1,1,'L├¬ Thß╗ï Hoa','0901 888 999','user@travelmate.vn',2040000,2040000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','├üp dß╗Ñng LATA20 giß║úm 20% ΓÇö partner chß╗ïu chi ph├¡ voucher.','LATA20',510000,'PARTNER',2550000,'2026-04-10 07:35:46.000000','2026-04-10 07:35:46.000000'),(18,'BK-MND-STD-0001',2,7,22,'2026-04-12','2026-04-14',2,0,1,'Phß║ím V─ân B├¼nh','0933 222 333','user@travelmate.vn',780000,780000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Homestay Mß╗Öc Nhi├¬n 2 ─æ├¬m ΓÇö ho├án tß║Ñt.',NULL,0,NULL,0,'2026-04-10 07:35:46.000000','2026-04-10 07:35:46.000000'),(19,'BK-FDN-DLX-0002',2,9,28,'2026-04-05','2026-04-07',2,0,1,'Ho├áng Thß╗ï Lan','0966 444 555','user@travelmate.vn',4800000,4800000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Furama Resort 2 ─æ├¬m ΓÇö ho├án tß║Ñt.',NULL,0,NULL,0,'2026-04-03 07:35:46.000000','2026-04-03 07:35:46.000000'),(20,'BK-VNT-DLX-0003',2,8,25,'2026-04-06','2026-04-08',2,0,1,'V┼⌐ Minh Tuß║Ñn','0977 666 777','user@travelmate.vn',5500000,5500000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','├üp dß╗Ñng VNT100K giß║úm 100K ΓÇö partner chß╗ïu chi ph├¡ voucher.','VNT100K',100000,'PARTNER',5600000,'2026-04-04 07:35:46.000000','2026-04-04 07:35:46.000000'),(21,'BK-TMG-DLX-0002',2,3,9,'2026-03-29','2026-03-31',2,0,1,'─Éß╗ù Quang Hß║úi','0944 888 999','user@travelmate.vn',2400000,2400000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','TM Grand 2 ─æ├¬m ΓÇö ho├án tß║Ñt.',NULL,0,NULL,0,'2026-03-27 07:35:46.000000','2026-03-27 07:35:46.000000'),(22,'BK-ANM-GDN-0002',2,4,13,'2026-03-30','2026-04-01',2,0,1,'Ng├┤ Thß╗ï Ph╞░╞íng','0911 000 111','user@travelmate.vn',7000000,7000000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Anam Villa 2 ─æ├¬m ΓÇö ho├án tß║Ñt.',NULL,0,NULL,0,'2026-03-28 07:35:46.000000','2026-03-28 07:35:46.000000'),(23,'BK-TLP-STD-0002',5,2,5,'2026-03-22','2026-03-24',2,0,1,'Trß║ºn Thß╗ï Mai','0923 456 789','user2@travelmate.vn',960000,960000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Tulip Hotel Standard 2 ─æ├¬m ΓÇö ho├án tß║Ñt.',NULL,0,NULL,0,'2026-03-20 07:35:46.000000','2026-03-20 07:35:46.000000'),(24,'BK-TLP-SUP-0002',2,2,6,'2026-03-15','2026-03-17',2,0,1,'Nguyß╗àn V─ân An','0912 345 678','user@travelmate.vn',1240000,1240000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Tulip Hotel Superior 2 ─æ├¬m ΓÇö ho├án tß║Ñt.',NULL,0,NULL,0,'2026-03-13 07:35:46.000000','2026-03-13 07:35:46.000000'),(25,'BK-BNH-BNG-0002',6,5,16,'2026-03-08','2026-03-10',2,0,1,'L├¬ V─ân ─Éß╗⌐c','0934 567 890','user3@travelmate.vn',4400000,4400000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Ba Na Hills Forest Bungalow 2 ─æ├¬m ΓÇö ho├án tß║Ñt.',NULL,0,NULL,0,'2026-03-06 07:35:46.000000','2026-03-06 07:35:46.000000'),(26,'BK-BNH-TWN-0001',5,5,17,'2026-03-01','2026-03-03',3,0,1,'Trß║ºn Thß╗ï Mai','0923 456 789','user2@travelmate.vn',3200000,3200000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Ba Na Hills Twin Cabin 2 ─æ├¬m ΓÇö ho├án tß║Ñt.',NULL,0,NULL,0,'2026-02-27 07:35:46.000000','2026-02-27 07:35:46.000000'),(27,'BK-HLR-STD-0001',2,6,19,'2026-02-22','2026-02-24',2,0,1,'Nguyß╗àn V─ân An','0912 345 678','user@travelmate.vn',640000,640000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Hoa L╞░ Riverside Standard 2 ─æ├¬m ΓÇö ho├án tß║Ñt.',NULL,0,NULL,0,'2026-02-20 07:35:46.000000','2026-02-20 07:35:46.000000'),(28,'BK-HLR-DLX-0002',6,6,20,'2026-02-15','2026-02-17',2,0,1,'L├¬ V─ân ─Éß╗⌐c','0934 567 890','user3@travelmate.vn',960000,960000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Hoa L╞░ Riverside Deluxe 2 ─æ├¬m ΓÇö ho├án tß║Ñt.',NULL,0,NULL,0,'2026-02-13 07:35:46.000000','2026-02-13 07:35:46.000000'),(29,'BK-ANM-BCH-0001',5,4,14,'2026-04-14','2026-04-17',2,0,1,'Tr?n Th? Mai','0923 456 789','user2@travelmate.vn',17400000,17400000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Anam Beachfront Villa 3 ?├¬m ? kh├ích ?├ú checkout.',NULL,NULL,NULL,NULL,'2026-04-12 08:08:48.000000','2026-04-12 08:08:48.000000'),(30,'BK-BNH-SUI-0001',6,5,18,'2026-04-02','2026-04-04',2,0,1,'L├¬ V?n ??c','0934 567 890','user3@travelmate.vn',6650000,6650000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','BNH-SUI v?i voucher ANAM15.','ANAM15',1050000,'PARTNER',7700000,'2026-03-31 08:08:48.000000','2026-03-31 08:08:48.000000'),(31,'BK-HLR-FAM-0001',5,6,21,'2026-04-09','2026-04-11',3,1,1,'Tr?n Th? Mai','0923 456 789','user2@travelmate.vn',1500000,1500000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Hoa L? Family 2 ?├¬m.',NULL,NULL,NULL,NULL,'2026-04-07 08:09:14.000000','2026-04-07 08:09:14.000000'),(32,'BK-MND-FAM-0001',2,7,24,'2026-03-25','2026-03-27',2,1,1,'Nguy?n V?n An','0912 345 678','user@travelmate.vn',1710000,1710000,0,'COMPLETED','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','MND-FAM v?i HOALUU50K.','HOALUU50K',50000,'PARTNER',1760000,'2026-03-23 08:09:14.000000','2026-03-23 08:09:14.000000'),(33,'BK-ANM-GDN-0003',6,4,13,'2026-05-09','2026-05-12',2,0,1,'L├¬ V?n ??c','0934 567 890','user3@travelmate.vn',10500000,10500000,0,'CONFIRMED','FULL_PAYMENT','APPROVED','PENDING_PARTNER_CONFIRMATION',NULL,NULL,NULL,NULL,NULL,'2026-05-04 08:09:14.000000','2026-05-04 08:09:14.000000'),(34,'BK-MND-ATT-0002',5,7,23,'2026-05-07','2026-05-09',2,0,1,'Tr?n Th? Mai','0923 456 789','user2@travelmate.vn',1160000,1160000,0,'CONFIRMED','FULL_PAYMENT','APPROVED','PENDING_PARTNER_CONFIRMATION',NULL,NULL,NULL,NULL,NULL,'2026-05-04 08:09:14.000000','2026-05-04 08:09:14.000000'),(35,'BK-FDN-BCH-0001',6,9,29,'2026-05-03','2026-05-06',2,0,1,'L├¬ V?n ??c','0934 567 890','user3@travelmate.vn',10800000,10800000,0,'CHECKED_IN','FULL_PAYMENT','APPROVED','PARTNER_CONFIRMED','Kh├ích ?├ú check-in Furama Beachfront.',NULL,NULL,NULL,NULL,'2026-04-30 08:09:14.000000','2026-04-30 08:09:14.000000');
/*!40000 ALTER TABLE `bookings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `partner_settlements`
--

DROP TABLE IF EXISTS `partner_settlements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `partner_settlements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `partner_id` bigint NOT NULL,
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `gross_amount` decimal(15,0) DEFAULT '0',
  `commission_amount` decimal(15,0) DEFAULT '0',
  `voucher_deduction_amount` decimal(15,0) DEFAULT '0',
  `payout_amount` decimal(15,0) DEFAULT '0',
  `settlement_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING',
  `settlement_date` datetime(6) DEFAULT NULL,
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_partner_period` (`partner_id`,`period_start`,`period_end`),
  CONSTRAINT `fk_settlements_partner` FOREIGN KEY (`partner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `partner_settlements`
--

LOCK TABLES `partner_settlements` WRITE;
/*!40000 ALTER TABLE `partner_settlements` DISABLE KEYS */;
INSERT INTO `partner_settlements` VALUES (1,3,'2026-04-27','2026-05-03',3600000,540000,0,3060000,'PAID','2026-05-02 07:35:46.000000','Admin ─æ├ú thanh to├ín cho partner Sunrise Sapa Lodge (HOTEL). Booking LATA-FAM ho├án tß║Ñt.','2026-05-02 07:35:46.000000'),(2,4,'2026-04-27','2026-05-03',13500000,2430000,0,11070000,'PAID','2026-05-02 07:35:46.000000','Admin ─æ├ú thanh to├ín cho partner Blue Ocean Resort (RESORT). Booking VNT-SUI ho├án tß║Ñt.','2026-05-02 07:35:46.000000'),(3,7,'2026-04-27','2026-05-03',7000000,840000,0,6160000,'PAID','2026-05-02 07:35:46.000000','Admin ─æ├ú thanh to├ín cho partner Green Hills Villa (VILLA). Booking ANM-GDN ho├án tß║Ñt.','2026-05-02 07:35:46.000000'),(4,8,'2026-04-27','2026-05-03',1740000,174000,0,1566000,'PAID','2026-05-02 07:35:46.000000','Admin ─æ├ú thanh to├ín cho partner Mekong Homestay (HOMESTAY). Booking MND-ATT ho├án tß║Ñt.','2026-05-02 07:35:46.000000'),(5,3,'2026-04-20','2026-04-26',1170000,175500,0,994500,'PAID','2026-05-01 07:35:46.000000','Tuß║ºn 2: LATA Hotel 1 booking ΓÇö voucher SUMMER10 do Admin chß╗ïu, kh├┤ng trß╗½ partner.','2026-05-01 07:35:46.000000'),(6,4,'2026-04-20','2026-04-26',5600000,1008000,0,4592000,'PAID','2026-05-01 07:35:46.000000','Tuß║ºn 2: Vinpearl Resort 1 booking ΓÇö kh├┤ng c├│ voucher.','2026-05-01 07:35:46.000000'),(7,3,'2026-04-13','2026-04-19',2040000,306000,510000,1224000,'PAID','2026-04-24 07:35:46.000000','Tuß║ºn 3: LATA Hotel 1 booking ΓÇö voucher LATA20 do Partner chß╗ïu, trß╗½ 510.000─æ.','2026-04-24 07:35:46.000000'),(8,8,'2026-04-13','2026-04-19',780000,78000,0,702000,'PAID','2026-04-24 07:35:46.000000','Tuß║ºn 3: Mß╗Öc Nhi├¬n Garden Homestay 1 booking ΓÇö kh├┤ng c├│ voucher.','2026-04-24 07:35:46.000000'),(9,4,'2026-04-06','2026-04-12',10300000,1854000,100000,8346000,'PAID','2026-04-17 07:35:46.000000','Tuß║ºn 4: Furama Resort + Vinpearl Resort 2 bookings ΓÇö voucher VNT100K do Partner chß╗ïu, trß╗½ 100.000─æ.','2026-04-17 07:35:46.000000'),(10,3,'2026-03-30','2026-04-05',2400000,360000,0,2040000,'PAID','2026-04-10 07:35:46.000000','Tuß║ºn 5: TM Grand Hotel 1 booking ΓÇö kh├┤ng c├│ voucher.','2026-04-10 07:35:46.000000'),(11,7,'2026-03-30','2026-04-05',7000000,840000,0,6160000,'PAID','2026-04-10 07:35:46.000000','Tuß║ºn 5: The Anam Villa 1 booking ΓÇö kh├┤ng c├│ voucher.','2026-04-10 07:35:46.000000'),(12,3,'2026-03-23','2026-03-29',2200000,330000,0,1870000,'PAID','2026-04-03 07:35:46.000000','Tuß║ºn 6: Tulip Hotel 2 bookings (TLP-STD + TLP-SUP) ΓÇö kh├┤ng c├│ voucher.','2026-04-03 07:35:46.000000'),(13,7,'2026-03-16','2026-03-22',7600000,912000,0,6688000,'PAID','2026-03-27 07:35:46.000000','Tuß║ºn 7: Ba Na Hills Forest Villa 2 bookings ΓÇö kh├┤ng c├│ voucher.','2026-03-27 07:35:46.000000'),(14,8,'2026-03-09','2026-03-15',1600000,160000,0,1440000,'PAID','2026-03-20 07:35:46.000000','Tuß║ºn 8: Hoa L╞░ Riverside Homestay 2 bookings (HLR-STD + HLR-DLX) ΓÇö kh├┤ng c├│ voucher.','2026-03-20 07:35:46.000000'),(15,3,'2026-04-27','2026-05-04',6650000,997500,0,5652500,'PENDING',NULL,'Tuß║ºn hiß╗çn tß║íi: LATA-DLX + TMG-PRE ΓÇö Admin sß║╜ chuyß╗ân khoß║ún v├áo Thß╗⌐ Ba tß╗¢i.','2026-05-04 07:35:46.000000'),(16,4,'2026-04-27','2026-05-04',8400000,1512000,0,6888000,'PENDING',NULL,'Tuß║ºn hiß╗çn tß║íi: VNT-DLX ΓÇö Admin sß║╜ chuyß╗ân khoß║ún sau khi booking ho├án tß║Ñt.','2026-05-04 07:35:46.000000'),(17,7,'2026-04-27','2026-05-04',10500000,1260000,0,9240000,'PENDING',NULL,'Tu?n hi?n t?i: Anam Garden Pool Villa 3 ?├¬m ? Admin s? thanh to├ín Th? Ba t?i.','2026-05-04 07:35:46.000000'),(18,8,'2026-04-27','2026-05-04',1160000,139200,0,1020800,'PENDING',NULL,'Tu?n hi?n t?i: M?c Nhi├¬n ├üp M├íi View 2 ?├¬m ? Admin s? thanh to├ín Th? Ba t?i.','2026-05-04 07:35:46.000000'),(25,7,'2026-04-13','2026-04-19',17400000,2610000,0,14790000,'PAID','2026-04-29 08:10:41.000000','Tu?n Apr 13-19: Anam Beachfront Pool Villa 3 ?├¬m ? comm 15% override.','2026-04-29 08:10:41.000000'),(26,7,'2026-04-06','2026-04-12',7700000,1232000,1050000,5418000,'PAID','2026-04-22 08:10:41.000000','Tu?n Apr 6-12: Ba Na Hills Treetop Suite + voucher ANAM15 partner ch?u.','2026-04-22 08:10:41.000000'),(27,8,'2026-03-30','2026-04-05',1760000,176000,50000,1534000,'PAID','2026-04-15 08:10:41.000000','Tu?n Mar 30-Apr 5: M?c Nhi├¬n Family + voucher HOALUU50K partner ch?u.','2026-04-15 08:10:41.000000');
/*!40000 ALTER TABLE `partner_settlements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_id` bigint NOT NULL,
  `payment_method` enum('MOMO_DEMO','VNPAY_DEMO') COLLATE utf8mb4_unicode_ci NOT NULL,
  `payment_option` enum('DEPOSIT_30','FULL_PAYMENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(15,0) NOT NULL,
  `transaction_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_status` enum('APPROVED','CANCELLED','DEPOSIT_FORFEITED','PENDING_ADMIN_APPROVAL','REJECTED','SUBMITTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_payments_booking` (`booking_id`),
  CONSTRAINT `fk_payments_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
INSERT INTO `payments` VALUES (1,1,'VNPAY_DEMO','DEPOSIT_30',390000,'TXN-1777880146000','PENDING_ADMIN_APPROVAL','2026-05-04 07:35:46.000000','Thanh to├ín demo VNPay - Cß╗ìc 30%'),(2,2,'VNPAY_DEMO','FULL_PAYMENT',1240000,'TXN-1779658026146','PENDING_ADMIN_APPROVAL','2026-05-04 07:35:46.000000','Thanh to├ín demo VNPay - Thanh to├ín 100%'),(3,3,'VNPAY_DEMO','DEPOSIT_30',720000,'TXN-1781435906292','APPROVED','2026-05-04 07:35:46.000000','Thanh to├ín demo VNPay - Cß╗ìc 30% ΓÇö admin ─æ├ú duyß╗çt'),(4,4,'VNPAY_DEMO','FULL_PAYMENT',1700000,'TXN-1783213786438','APPROVED','2026-05-04 07:35:46.000000','Thanh to├ín demo VNPay - 100% ΓÇö admin ─æ├ú duyß╗çt'),(5,5,'VNPAY_DEMO','DEPOSIT_30',288000,'TXN-1784991666584','DEPOSIT_FORFEITED','2026-04-29 07:35:46.000000','Cß╗ìc 30% bß╗ï giß╗» lß║íi do kh├ích kh├┤ng ─æß║┐n check-in.'),(6,6,'VNPAY_DEMO','FULL_PAYMENT',4950000,'TXN-1786769546730','APPROVED','2026-04-27 07:35:46.000000','Thanh to├ín 100% ΓÇö ─æ├ú check-in'),(7,7,'VNPAY_DEMO','FULL_PAYMENT',3600000,'TXN-1788547426876','APPROVED','2026-04-22 07:35:46.000000','Thanh to├ín 100% ΓÇö ─æ├ú ho├án tß║Ñt'),(8,8,'VNPAY_DEMO','DEPOSIT_30',2520000,'TXN-1790325307022','APPROVED','2026-05-04 07:35:46.000000','Cß╗ìc 30% Vinpearl DLX ΓÇö partner2 chß╗¥ x├íc nhß║¡n'),(9,9,'VNPAY_DEMO','FULL_PAYMENT',13500000,'TXN-1792103187168','APPROVED','2026-05-02 07:35:46.000000','100% Vinpearl SUI ΓÇö partner2 ─æ├ú x├íc nhß║¡n'),(10,10,'VNPAY_DEMO','FULL_PAYMENT',1740000,'TXN-1793881067314','APPROVED','2026-04-24 07:35:46.000000','Homestay Mß╗Öc Nhi├¬n ΓÇö ho├án tß║Ñt'),(11,11,'VNPAY_DEMO','DEPOSIT_30',3150000,'TXN-1795658947460','PENDING_ADMIN_APPROVAL','2026-05-04 07:35:46.000000','Cß╗ìc 30% Anam Garden Villa ΓÇö chß╗¥ admin duyß╗çt'),(12,12,'VNPAY_DEMO','DEPOSIT_30',1440000,'TXN-1797436827606','DEPOSIT_FORFEITED','2026-04-28 07:35:46.000000','Cß╗ìc 30% Furama DLX ΓÇö no-show mß║Ñt cß╗ìc'),(13,13,'VNPAY_DEMO','FULL_PAYMENT',960000,'TXN-1799214707752','APPROVED','2026-05-04 07:35:46.000000','100% Homestay Hß╗Öi An ΓÇö partner1 chß╗¥ x├íc nhß║¡n'),(14,14,'VNPAY_DEMO','FULL_PAYMENT',6600000,'TXN-1800992587898','APPROVED','2026-05-01 07:35:46.000000','100% Ba Na Bungalow ΓÇö ─æ├ú check-in'),(15,15,'VNPAY_DEMO','FULL_PAYMENT',1170000,'TXN-LATA-STD2-1777880146','APPROVED','2026-04-17 07:35:46.000000','LATA-STD 2 ─æ├¬m vß╗¢i SUMMER10 ΓÇö ho├án tß║Ñt'),(16,16,'VNPAY_DEMO','FULL_PAYMENT',5600000,'TXN-VNT-DLX2-1777880146','APPROVED','2026-04-18 07:35:46.000000','VNT-DLX 2 ─æ├¬m ΓÇö ho├án tß║Ñt'),(17,17,'VNPAY_DEMO','FULL_PAYMENT',2040000,'TXN-LATA-DLX2-1777880146','APPROVED','2026-04-10 07:35:46.000000','LATA-DLX 3 ─æ├¬m vß╗¢i LATA20 ΓÇö ho├án tß║Ñt'),(18,18,'VNPAY_DEMO','FULL_PAYMENT',780000,'TXN-MND-STD1-1777880146','APPROVED','2026-04-10 07:35:46.000000','MND-STD 2 ─æ├¬m ΓÇö ho├án tß║Ñt'),(19,19,'VNPAY_DEMO','FULL_PAYMENT',4800000,'TXN-FDN-DLX2-1777880146','APPROVED','2026-04-03 07:35:46.000000','FDN-DLX 2 ─æ├¬m ΓÇö ho├án tß║Ñt'),(20,20,'VNPAY_DEMO','FULL_PAYMENT',5500000,'TXN-VNT-DLX3-1777880146','APPROVED','2026-04-04 07:35:46.000000','VNT-DLX 2 ─æ├¬m vß╗¢i VNT100K ΓÇö ho├án tß║Ñt'),(21,21,'VNPAY_DEMO','FULL_PAYMENT',2400000,'TXN-TMG-DLX2-1777880146','APPROVED','2026-03-27 07:35:46.000000','TMG-DLX 2 ─æ├¬m ΓÇö ho├án tß║Ñt'),(22,22,'VNPAY_DEMO','FULL_PAYMENT',7000000,'TXN-ANM-GDN2-1777880146','APPROVED','2026-03-28 07:35:46.000000','ANM-GDN 2 ─æ├¬m ΓÇö ho├án tß║Ñt'),(23,23,'VNPAY_DEMO','FULL_PAYMENT',960000,'TXN-TLP-STD1-1777880146','APPROVED','2026-03-20 07:35:46.000000','TLP-STD 2 ─æ├¬m ΓÇö ho├án tß║Ñt'),(24,24,'VNPAY_DEMO','FULL_PAYMENT',1240000,'TXN-TLP-SUP1-1777880146','APPROVED','2026-03-13 07:35:46.000000','TLP-SUP 2 ─æ├¬m ΓÇö ho├án tß║Ñt'),(25,25,'VNPAY_DEMO','FULL_PAYMENT',4400000,'TXN-BNH-BNG2-1777880146','APPROVED','2026-03-06 07:35:46.000000','BNH-BNG 2 ─æ├¬m ΓÇö ho├án tß║Ñt'),(26,26,'VNPAY_DEMO','FULL_PAYMENT',3200000,'TXN-BNH-TWN1-1777880146','APPROVED','2026-02-27 07:35:46.000000','BNH-TWN 2 ─æ├¬m ΓÇö ho├án tß║Ñt'),(27,27,'VNPAY_DEMO','FULL_PAYMENT',640000,'TXN-HLR-STD1-1777880146','APPROVED','2026-02-20 07:35:46.000000','HLR-STD 2 ─æ├¬m ΓÇö ho├án tß║Ñt'),(28,28,'VNPAY_DEMO','FULL_PAYMENT',960000,'TXN-HLR-DLX2-1777880146','APPROVED','2026-02-13 07:35:46.000000','HLR-DLX 2 ─æ├¬m ΓÇö ho├án tß║Ñt'),(29,29,'VNPAY_DEMO','FULL_PAYMENT',17400000,'TXN-ANM-BCH1-1777882128','APPROVED','2026-04-12 08:08:48.000000','ANM-BCH 3 ?├¬m'),(30,30,'VNPAY_DEMO','FULL_PAYMENT',6650000,'TXN-BNH-SUI1-1777882128','APPROVED','2026-03-31 08:08:48.000000','BNH-SUI v?i ANAM15'),(31,31,'VNPAY_DEMO','FULL_PAYMENT',1500000,'TXN-HLR-FAM1-1777882154','APPROVED','2026-04-07 08:09:14.000000','HLR-FAM 2 ?├¬m'),(32,32,'VNPAY_DEMO','FULL_PAYMENT',1710000,'TXN-MND-FAM1-1777882154','APPROVED','2026-03-23 08:09:14.000000','MND-FAM v?i HOALUU50K'),(33,33,'VNPAY_DEMO','FULL_PAYMENT',10500000,'TXN-ANM-GDN3-1777882154','APPROVED','2026-05-04 08:09:14.000000','ANM-GDN ch? partner3'),(34,34,'VNPAY_DEMO','FULL_PAYMENT',1160000,'TXN-MND-ATT2-1777882154','APPROVED','2026-05-04 08:09:14.000000','MND-ATT ch? partner4'),(35,35,'VNPAY_DEMO','FULL_PAYMENT',10800000,'TXN-FDN-BCH1-1777882154','APPROVED','2026-04-30 08:09:14.000000','FDN-BCH ?├ú check-in');
/*!40000 ALTER TABLE `payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `accommodation_id` bigint NOT NULL,
  `booking_id` bigint NOT NULL,
  `rating` int NOT NULL,
  `comment` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `booking_id` (`booking_id`),
  KEY `fk_reviews_user` (`user_id`),
  KEY `fk_reviews_accommodation` (`accommodation_id`),
  CONSTRAINT `fk_reviews_accommodation` FOREIGN KEY (`accommodation_id`) REFERENCES `accommodations` (`id`),
  CONSTRAINT `fk_reviews_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
  CONSTRAINT `fk_reviews_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reviews`
--

LOCK TABLES `reviews` WRITE;
/*!40000 ALTER TABLE `reviews` DISABLE KEYS */;
INSERT INTO `reviews` VALUES (1,2,1,7,5,'Ph├▓ng rß║Ñt sß║ích sß║╜, nh├ón vi├¬n nhiß╗çt t├¼nh. View ─æß║╣p, gß║ºn trung t├óm chß╗ú ─æ├¬m ─É├á Lß║ít. Ph├▓ng gia ─æ├¼nh rß╗Öng r├úi, rß║Ñt ph├╣ hß╗úp cho gia ─æ├¼nh c├│ trß║╗ nhß╗Å. Lß║ºn sau sß║╜ quay lß║íi!','2026-04-27 07:35:46.000000'),(2,2,7,10,4,'Homestay rß║Ñt y├¬n t─⌐nh, kh├┤ng gian th╞í mß╗Öng giß╗»a v╞░ß╗¥n hoa d├ú quß╗│. Ph├▓ng ├íp m├íi c├│ cß╗¡a sß╗ò k├¡nh nh├¼n ─æß╗ôi th├┤ng rß║Ñt l├úng mß║ín. WiFi buß╗òi tß╗æi h╞íi yß║┐u nh╞░ng nh├¼n chung rß║Ñt ─æ├íng tiß╗ün.','2026-04-29 07:35:46.000000'),(3,2,1,15,5,'D├╣ng voucher SUMMER10 rß║Ñt hß╗¥i! Ph├▓ng sß║ích sß║╜, check-in nhanh, vß╗ï tr├¡ trung t├óm ─É├á Lß║ít tiß╗çn lß╗úi. Nh├ón vi├¬n lß╗à t├ón th├ón thiß╗çn v├á nhiß╗çt t├¼nh hß╗ù trß╗ú h├ánh l├╜.','2026-04-21 07:35:46.000000'),(4,2,1,17,4,'Voucher LATA20 giß║úm ─æ╞░ß╗úc nhiß╗üu, ph├▓ng Deluxe rß╗Öng c├│ ban c├┤ng nh├¼n v╞░ß╗¥n ─æß║╣p. Bß╗»a s├íng ß╗òn nh╞░ng ch╞░a ─æa dß║íng lß║»m. Nh├¼n chung rß║Ñt ─æ├íng tiß╗ün, sß║╜ quay lß║íi.','2026-04-15 07:35:46.000000'),(5,2,3,21,5,'TravelMate Grand Hotel thß║¡t sß╗▒ xß╗⌐ng ─æ├íng 5 sao! Spa tuyß╗çt vß╗¥i, nh├á h├áng fine dining ngon, ph├▓ng view thung l┼⌐ng cß╗▒c ─æß║╣p. Dß╗ïch vß╗Ñ butler tß║¡n t├óm, chß║»c chß║»n sß║╜ quay lß║íi.','2026-03-31 07:35:46.000000'),(6,2,4,22,5,'The Anam Villa l├á thi├¬n ─æ╞░ß╗¥ng nghß╗ë d╞░ß╗íng! Butler phß╗Ñc vß╗Ñ tß║¡n t├¼nh, hß╗ô b╞íi private s├ít biß╗ân, bß╗»a s├íng ─æß║╖t tß║íi ph├▓ng tuyß╗çt hß║úo. Gi├í xß╗⌐ng ─æ├íng vß╗¢i ─æß║│ng cß║Ñp nhß║¡n ─æ╞░ß╗úc.','2026-04-01 07:35:46.000000'),(7,2,8,16,5,'Vinpearl Resort ─æß║│ng cß║Ñp! B├úi biß╗ân ri├¬ng tuyß╗çt ─æß║╣p, hß╗ô b╞íi lß╗¢n, ph├▓ng view biß╗ân tho├íng m├ít. Dß╗ïch vß╗Ñ chuy├¬n nghiß╗çp, bß╗»a s├íng buffet phong ph├║. Sß║╜ quay lß║íi v├áo dß╗ïp kh├íc!','2026-04-22 07:35:46.000000'),(8,2,7,18,4,'Homestay Mß╗Öc Nhi├¬n y├¬n t─⌐nh v├á th╞í mß╗Öng, kh├┤ng gian xanh m╞░ß╗¢t giß╗»a n├║i ─æß╗ôi ─É├á Lß║ít. Chß╗º nh├á rß║Ñt th├ón thiß╗çn, gß╗úi ├╜ nhiß╗üu ─æß╗ïa ─æiß╗âm hay. WiFi ß╗òn ─æß╗ïnh h╞ín lß║ºn tr╞░ß╗¢c!','2026-04-14 07:35:46.000000'),(9,2,9,19,5,'Furama Resort ─É├á Nß║╡ng l├á kß╗│ nghß╗ë tuyß╗çt vß╗¥i nhß║Ñt! Ph├▓ng rß╗Öng c├│ ban c├┤ng nh├¼n thß║│ng ra biß╗ân Mß╗╣ Kh├¬. Spa tuyß╗çt vß╗¥i, nh├á h├áng phß╗Ñc vß╗Ñ tß║¡n t├¼nh. Chß║»c chß║»n sß║╜ quay lß║íi!','2026-04-07 07:35:46.000000'),(10,2,8,20,4,'Kß╗│ nghß╗ë thß╗⌐ hai tß║íi Vinpearl, lß║ºn n├áy d├╣ng voucher VNT100K. Ph├▓ng Deluxe tiß╗çn nghi ─æß║ºy ─æß╗º. B├úi biß╗ân ─æß║╣p nh╞░ng kh├í ─æ├┤ng v├áo cuß╗æi tuß║ºn. Nh├¼n chung rß║Ñt ─æ├íng tiß╗ün.','2026-04-08 07:35:46.000000'),(11,5,2,23,5,'Tulip Hotel 2 Dalat tuy 3 sao nh╞░ng chß║Ñt l╞░ß╗úng v╞░ß╗út mong ─æß╗úi! Ph├▓ng Standard sß║ích sß║╜ thoß║úi m├íi, view ─æß╗ôi th├┤ng ─É├á Lß║ít buß╗òi s├íng rß║Ñt ─æß║╣p. Nh├ón vi├¬n th├ón thiß╗çn, check-in nhanh ch├│ng. Gi├í rß║Ñt phß║úi ch─âng cho vß╗ï tr├¡ trung t├óm gß║ºn hß╗ô Xu├ón H╞░╞íng. Chß║»c chß║»n sß║╜ quay lß║íi!','2026-03-26 07:35:46.000000'),(12,2,2,24,4,'Kh├ích sß║ín phong c├ích Ch├óu ├éu cß╗ò ─æiß╗ân rß║Ñt duy├¬n d├íng. Ph├▓ng Superior c├│ ban c├┤ng nh├¼n hß╗ô Xu├ón H╞░╞íng tuyß╗çt ─æß║╣p v├áo buß╗òi s├íng. Bß╗»a s├íng buffet ß╗òn, WiFi ß╗òn ─æß╗ïnh. Gi├í xß╗⌐ng ─æ├íng vß╗¢i chß║Ñt l╞░ß╗úng, ph├╣ hß╗úp cho cß║╖p ─æ├┤i.','2026-03-19 07:35:46.000000'),(13,6,5,25,5,'Trß║úi nghiß╗çm ─æß╗ënh cao giß╗»a rß╗½ng B├á N├á! Bungalow gß╗ù tß╗▒ nhi├¬n ß║Ñm ├íp, s├án k├¡nh ngß║»m rß╗½ng vß╗ü ─æ├¬m cß╗▒c kß╗│ ß║úo diß╗çu. Kh├┤ng kh├¡ trong l├ánh, y├¬n t─⌐nh tuyß╗çt ─æß╗æi. Gß║ºn c├íp treo v├á c├íc ─æiß╗âm tham quan nß╗òi tiß║┐ng. ─É├íng tß╗½ng ─æß╗ông tiß╗ün bß╗Å ra!','2026-03-12 07:35:46.000000'),(14,5,5,26,4,'Villa giß╗»a rß╗½ng B├á N├á rß║Ñt th╞í mß╗Öng v├á ─æß╗Öc ─æ├ío. S├ón hi├¬n c├│ bß║┐p BBQ c├╣ng nh├│m bß║ín rß║Ñt vui vß║╗. Ph├▓ng h╞íi nhß╗Å h╞ín ß║únh nh╞░ng trang thiß║┐t bß╗ï ─æß║ºy ─æß╗º v├á sß║ích sß║╜. Nh├ón vi├¬n nhiß╗çt t├¼nh, ─æß╗ô ─ân ngon. Sß║╜ giß╗¢i thiß╗çu cho bß║ín b├¿!','2026-03-05 07:35:46.000000'),(15,2,6,27,5,'Hoa L╞░ Riverside Homestay l├á vi├¬n ngß╗ìc ß║⌐n cß╗ºa Hß╗Öi An! Nh├á cß╗ò 3 gian m├íi ng├│i b├¬n s├┤ng Thu Bß╗ôn, buß╗òi s├íng ─ân b├ính m├¼ do chß╗º nh├á tß╗▒ l├ám ngon tuyß╗çt. ─É╞░ß╗úc m╞░ß╗ún xe ─æß║íp miß╗àn ph├¡ ─æi phß╗æ cß╗ò chß╗ë 5 ph├║t. Chß╗º nh├á hiß║┐u kh├ích v├á nhiß╗çt t├¼nh t╞░ vß║Ñn ─æß╗ïa ─æiß╗âm!','2026-02-26 07:35:46.000000'),(16,6,6,28,5,'Homestay truyß╗ün thß╗æng ─æß║¡m chß║Ñt Hß╗Öi An! Ph├▓ng Deluxe nh├á tß║»m ri├¬ng sß║ích sß║╜, cß╗¡a sß╗ò nh├¼n v╞░ß╗¥n xanh m├ít. Bß╗»a s├íng phß╗ƒ v├á b├ính m├¼ tß╗▒ l├ám si├¬u ngon. Vß╗ï tr├¡ ─æi bß╗Ö ra phß╗æ cß╗ò 5 ph├║t. Mß╗Öt trß║úi nghiß╗çm ─æ├íng nhß╗¢ kh├íc biß╗çt ho├án to├án vß╗¢i kh├ích sß║ín th├┤ng th╞░ß╗¥ng!','2026-02-19 07:35:46.000000'),(17,5,4,29,5,'Ph├▓ng Beachfront Pool Villa t?i Anam l├á tr?i nghi?m kh├┤ng th? qu├¬n! H? b?i tr├án ra bi?n, butler ph?c v? 24/7, b?a s├íng t?i ph├▓ng ho├án h?o. Kh├┤ng gian ri├¬ng t? tuy?t ??i, th├¡ch h?p cho tu?n tr?ng m?t.','2026-04-19 08:09:46.000000'),(18,6,5,30,4,'Treetop Suite B├á N├á th?c s? ??c ?├ío! Ban c├┤ng 360┬░ nh├¼n to├án r?ng th├┤ng. D├╣ng voucher ANAM15 ???c gi?m t?t. B?n t?m jacuzzi ngo├ái tr?i v? ?├¬m r?t tuy?t. D?ch v? ?n u?ng t?i ch? c├▓n ├¡t l?a ch?n.','2026-04-06 08:09:46.000000'),(19,5,6,31,5,'Ph├▓ng gia ?├¼nh Hoa L? c?c k? tho?i m├íi! Ban c├┤ng nh├¼n s├┤ng Thu B?n th? m?ng. Ch? nh├á nhi?t t├¼nh d?n ?i ph? c?. B├ính m├¼ t? l├ám bu?i s├íng ngon nh?t H?i An!','2026-04-13 08:09:46.000000'),(20,2,7,32,5,'Ph├▓ng Gia ?├¼nh M?c Nhi├¬n c├│ s├ón th??ng nh├¼n v??n d├ú qu? c?c l├úng m?n! D├╣ng voucher HOALUU50K ti?t ki?m 50K. L├▓ s??i c?i bu?i t?i ?m ├íp, b?a s├íng th?m ngon.','2026-03-29 08:09:46.000000');
/*!40000 ALTER TABLE `reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rooms`
--

DROP TABLE IF EXISTS `rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `accommodation_id` bigint NOT NULL,
  `room_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `room_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `bed_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `capacity` int DEFAULT '2',
  `price_per_night` decimal(15,0) NOT NULL,
  `available_quantity` int DEFAULT '1',
  `description` text COLLATE utf8mb4_unicode_ci,
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approval_status` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT 'APPROVED',
  `room_category` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT 'STANDARD',
  `commission_rate_override` decimal(5,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_rooms_accommodation` (`accommodation_id`),
  CONSTRAINT `fk_rooms_accommodation` FOREIGN KEY (`accommodation_id`) REFERENCES `accommodations` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rooms`
--

LOCK TABLES `rooms` WRITE;
/*!40000 ALTER TABLE `rooms` DISABLE KEYS */;
INSERT INTO `rooms` VALUES (1,1,'LATA-STD','Ph├▓ng Ti├¬u Chuß║⌐n Gi╞░ß╗¥ng King','1 gi╞░ß╗¥ng cß╗í King',2,650000,5,'Ph├▓ng ti├¬u chuß║⌐n 25m┬▓, tß║ºm nh├¼n th├ánh phß╗æ, WiFi miß╗àn ph├¡, ─æiß╗üu ho├á, minibar.','https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=400&q=70','APPROVED','STANDARD',NULL),(2,1,'LATA-DLX','Ph├▓ng Deluxe Gi╞░ß╗¥ng ─É├┤i','2 gi╞░ß╗¥ng ─æ╞ín',3,850000,3,'Ph├▓ng Deluxe 30m┬▓ vß╗¢i ban c├┤ng ri├¬ng, view v╞░ß╗¥n hoa. Bao gß╗ôm bß╗»a s├íng.','https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400&q=70','APPROVED','DELUXE',NULL),(3,1,'LATA-FAM','Ph├▓ng Gia ─É├¼nh','1 gi╞░ß╗¥ng King + 1 gi╞░ß╗¥ng ─æ╞ín',4,1200000,3,'Ph├▓ng gia ─æ├¼nh 40m┬▓, ph├╣ hß╗úp gia ─æ├¼nh c├│ trß║╗ nhß╗Å. C├│ bß╗ôn tß║»m lß╗¢n.','https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=400&q=70','APPROVED','FAMILY',12.00),(4,1,'LATA-SUI','Ph├▓ng Suite Cao Cß║Ñp','1 gi╞░ß╗¥ng King size',2,1800000,1,'Suite 55m┬▓ sang trß╗ìng vß╗¢i ph├▓ng kh├ích ri├¬ng, view hß╗ô Xu├ón H╞░╞íng, bß╗ôn tß║»m jacuzzi.','https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400&q=70','APPROVED','SUITE',20.00),(5,2,'TLP-STD','Ph├▓ng Standard Twin','2 gi╞░ß╗¥ng ─æ╞ín',2,480000,6,'Ph├▓ng standard 22m┬▓, nß╗Öi thß║Ñt ─æ╞ín giß║ún tiß╗çn nghi, WiFi miß╗àn ph├¡.','https://images.unsplash.com/photo-1540518614846-7eded433c457?w=400&q=70','APPROVED','STANDARD',NULL),(6,2,'TLP-SUP','Ph├▓ng Superior Double','1 gi╞░ß╗¥ng ─æ├┤i',2,620000,5,'Ph├▓ng Superior 28m┬▓ vß╗¢i view ─æß╗ôi th├┤ng, bao gß╗ôm bß╗»a s├íng buffet.','https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=400&q=70','APPROVED','DELUXE',NULL),(7,2,'TLP-FAM','Ph├▓ng Gia ─É├¼nh Rß╗Öng','1 gi╞░ß╗¥ng ─æ├┤i + 2 gi╞░ß╗¥ng ─æ╞ín',5,1050000,2,'Ph├▓ng gia ─æ├¼nh rß╗Öng 45m┬▓, l├╜ t╞░ß╗ƒng cho nh├│m bß║ín hoß║╖c gia ─æ├¼nh lß╗¢n.','https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=400&q=70','APPROVED','FAMILY',NULL),(8,2,'TLP-VIP','Ph├▓ng VIP Panorama','1 gi╞░ß╗¥ng King size',2,1500000,2,'Ph├▓ng VIP 50m┬▓ vß╗¢i ban c├┤ng rß╗Öng, view 360┬░ to├án cß║únh ─É├á Lß║ít.','https://images.unsplash.com/photo-1591088398332-8a7791972843?w=400&q=70','APPROVED','VIP',18.00),(9,3,'TMG-DLX','Deluxe Garden View','1 gi╞░ß╗¥ng King size',2,1200000,7,'Ph├▓ng Deluxe 35m┬▓, view v╞░ß╗¥n th├┤ng t─⌐nh lß║╖ng, bß╗ôn tß║»m ─æß╗⌐ng + bß╗ôn ng├óm ri├¬ng.','https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=400&q=70','APPROVED','DELUXE',NULL),(10,3,'TMG-PRE','Premium Valley View','1 gi╞░ß╗¥ng King hoß║╖c 2 gi╞░ß╗¥ng ─æ╞ín',3,1650000,4,'Ph├▓ng Premium 42m┬▓, tß║ºm nh├¼n thung l┼⌐ng ngoß║ín mß╗Ñc, minibar complimentary.','https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400&q=70','APPROVED','VIP',18.00),(11,3,'TMG-FAM','Family Grand Suite','2 gi╞░ß╗¥ng King size',5,2800000,3,'Suite gia ─æ├¼nh 65m┬▓, 2 ph├▓ng ngß╗º, ph├▓ng kh├ích, bß║┐p nhß╗Å, ph├╣ hß╗úp nghß╗ë d╞░ß╗íng d├ái ng├áy.','https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=400&q=70','APPROVED','FAMILY',12.00),(12,3,'TMG-PRE2','Presidential Suite','1 gi╞░ß╗¥ng King cß╗í lß╗¢n',2,5500000,1,'Suite Tß╗òng Thß╗æng 100m┬▓, sang trß╗ìng nhß║Ñt kh├ích sß║ín. Ph├▓ng kh├ích ri├¬ng, b├án l├ám viß╗çc, spa tß║íi ph├▓ng, butler ri├¬ng.','https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400&q=70','APPROVED','SUITE',20.00),(13,4,'ANM-GDN','Garden Pool Villa','1 gi╞░ß╗¥ng King size',2,3500000,4,'Biß╗çt thß╗▒ 80m┬▓ c├│ hß╗ô b╞íi ri├¬ng, v╞░ß╗¥n nhiß╗çt ─æß╗¢i, view n├║i. Bao gß╗ôm bß╗»a s├íng ─æß║╖t tß║íi ph├▓ng.','https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400&q=70','APPROVED','STANDARD',NULL),(14,4,'ANM-BCH','Beachfront Pool Villa','1 gi╞░ß╗¥ng King cß╗í lß╗¢n',2,5800000,2,'Biß╗çt thß╗▒ 120m┬▓ s├ít biß╗ân, hß╗ô b╞íi private infinity tr├án ra biß╗ân. Butler phß╗Ñc vß╗Ñ 24/7.','https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=400&q=70','APPROVED','VIP',15.00),(15,4,'ANM-FAM','Family Grand Villa','3 gi╞░ß╗¥ng King',6,8500000,1,'Biß╗çt thß╗▒ 200m┬▓ hai tß║ºng, 3 ph├▓ng ngß╗º, ph├▓ng kh├ích rß╗Öng, bß║┐p ─ân full, hß╗ô b╞íi private.','https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=400&q=70','APPROVED','FAMILY',10.00),(16,5,'BNH-BNG','Forest Bungalow','1 gi╞░ß╗¥ng King size',2,2200000,4,'Bungalow 60m┬▓ gß╗ù tß╗▒ nhi├¬n, s├án k├¡nh ngß║»m rß╗½ng, bß╗ôn tß║»m thß║úo mß╗Öc, h╞íi s╞░╞íng s├íng sß╗¢m.','https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=400&q=70','APPROVED','STANDARD',NULL),(17,5,'BNH-TWN','Twin Cabin','2 gi╞░ß╗¥ng ─æ╞ín',3,1600000,4,'Cabin 45m┬▓ d├ánh cho nh├│m bß║ín, view ─æß╗ôi th├┤ng, s├ón hi├¬n ngo├ái trß╗¥i c├│ bß║┐p n╞░ß╗¢ng BBQ.','https://images.unsplash.com/photo-1444201983204-c43cbd584d93?w=400&q=70','APPROVED','FAMILY',NULL),(18,5,'BNH-SUI','Treetop Suite','1 gi╞░ß╗¥ng King size',2,3800000,2,'Suite tr├¬n c├óy 70m┬▓, ban c├┤ng 360┬░ nh├¼n to├án rß╗½ng, bß╗ôn tß║»m jacuzzi ngo├ái trß╗¥i.','https://images.unsplash.com/photo-1537640538966-79f369143f8f?w=400&q=70','APPROVED','SUITE',16.00),(19,6,'HLR-STD','Ph├▓ng Truyß╗ün Thß╗æng','1 gi╞░ß╗¥ng ─æ├┤i',2,320000,4,'Ph├▓ng 20m┬▓ trang tr├¡ bß║▒ng ─æß╗ô gß╗æm Chu ─Éß║¡u v├á tranh lß╗Ña Hß╗Öi An. Nh├á tß║»m chung sß║ích sß║╜.','https://images.unsplash.com/photo-1540518614846-7eded433c457?w=400&q=70','APPROVED','STANDARD',NULL),(20,6,'HLR-DLX','Ph├▓ng Deluxe Ri├¬ng','1 gi╞░ß╗¥ng ─æ├┤i',2,480000,2,'Ph├▓ng 25m┬▓ nh├á tß║»m ri├¬ng, cß╗¡a sß╗ò nh├¼n v╞░ß╗¥n, bao gß╗ôm bß╗»a s├íng phß╗ƒ v├á b├ính m├¼.','https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=400&q=70','APPROVED','DELUXE',NULL),(21,6,'HLR-FAM','Ph├▓ng Gia ─É├¼nh Ven S├┤ng','2 gi╞░ß╗¥ng ─æ├┤i',4,750000,2,'Ph├▓ng 35m┬▓ ban c├┤ng nh├¼n s├┤ng Thu Bß╗ôn, l├╜ t╞░ß╗ƒng cho gia ─æ├¼nh nhß╗Å c├│ trß║╗ em.','https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=400&q=70','APPROVED','FAMILY',8.00),(22,7,'MND-STD','Ph├▓ng Nh├á Gß╗ù Ti├¬u Chuß║⌐n','1 gi╞░ß╗¥ng ─æ├┤i',2,390000,5,'Ph├▓ng gß╗ù th├┤ng 22m┬▓, l├▓ s╞░ß╗ƒi mini, nß╗Öi thß║Ñt vintage, bao gß╗ôm bß╗»a s├íng b├ính m├¼ thß╗ït n╞░ß╗¢ng.','https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=400&q=70','APPROVED','STANDARD',NULL),(23,7,'MND-ATT','Ph├▓ng ├üp M├íi View ─Éß╗ôi','1 gi╞░ß╗¥ng King size',2,580000,3,'Ph├▓ng ├íp m├íi 28m┬▓ cß╗¡a sß╗ò m├íi k├¡nh, view ─æß╗ôi th├┤ng xanh, ─æß║╖c biß╗çt y├¬n t─⌐nh.','https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400&q=70','APPROVED','DELUXE',12.00),(24,7,'MND-FAM','Ph├▓ng Gia ─É├¼nh V╞░ß╗¥n Hoa','1 gi╞░ß╗¥ng King + 1 ─æ╞ín',4,880000,2,'Ph├▓ng 38m┬▓ s├ón th╞░ß╗úng ri├¬ng nh├¼n ra v╞░ß╗¥n hoa d├ú quß╗│, th├¡ch hß╗úp gia ─æ├¼nh hoß║╖c nh├│m nhß╗Å.','https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=400&q=70','APPROVED','FAMILY',NULL),(25,8,'VNT-DLX','Deluxe Ocean View','1 gi╞░ß╗¥ng King size',2,2800000,9,'Ph├▓ng Deluxe 42m┬▓ view biß╗ân, bao gß╗ôm v├⌐ c├íp treo v├á c├┤ng vi├¬n giß║úi tr├¡ Vinpearl Land.','https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=400&q=70','APPROVED','DELUXE',NULL),(26,8,'VNT-SUI','Junior Suite Beachfront','1 gi╞░ß╗¥ng King cß╗í lß╗¢n',2,4500000,4,'Suite 65m┬▓ ban c├┤ng h╞░ß╗¢ng biß╗ân, bß╗ôn tß║»m jacuzzi trong ph├▓ng, dß╗ïch vß╗Ñ butler cao cß║Ñp.','https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=400&q=70','APPROVED','SUITE',22.00),(27,8,'VNT-VIL','Pool Villa','2 gi╞░ß╗¥ng King',4,9800000,2,'Villa ri├¬ng 150m┬▓ vß╗¢i hß╗ô b╞íi private, 2 ph├▓ng ngß╗º, bß║┐p ─ân, ph├╣ hß╗úp gia ─æ├¼nh hoß║╖c tuß║ºn tr─âng mß║¡t.','https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=400&q=70','APPROVED','FAMILY',15.00),(28,9,'FDN-DLX','Deluxe Garden View','1 gi╞░ß╗¥ng King size',2,2400000,8,'Ph├▓ng 40m┬▓ view v╞░ß╗¥n nhiß╗çt ─æß╗¢i, bao gß╗ôm bß╗»a s├íng buffet tß║íi nh├á h├áng La Maison 1888.','https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400&q=70','APPROVED','DELUXE',NULL),(29,9,'FDN-BCH','Beachfront Superior','1 gi╞░ß╗¥ng King size',2,3600000,6,'Ph├▓ng 48m┬▓ view biß╗ân Mß╗╣ Kh├¬, b├úi tß║»m ri├¬ng 200m, ghß║┐ nß║▒m v├á ├┤ d├╣ phß╗Ñc vß╗Ñ tß║¡n n╞íi.','https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=400&q=70','APPROVED','VIP',20.00),(30,9,'FDN-FAM','Family Suite','2 gi╞░ß╗¥ng ─æ├┤i',5,5200000,3,'Suite gia ─æ├¼nh 80m┬▓, 2 ph├▓ng ngß╗º, ph├▓ng kh├ích ri├¬ng, view biß╗ân panorama, bß║┐p nhß╗Å.','https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=400&q=70','APPROVED','FAMILY',15.00);
/*!40000 ALTER TABLE `rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `support_tickets`
--

DROP TABLE IF EXISTS `support_tickets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `support_tickets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `partner_id` bigint NOT NULL,
  `category` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `priority` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Trung b├¼nh',
  `description` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN',
  `admin_response` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_tickets_partner` (`partner_id`),
  CONSTRAINT `fk_tickets_partner` FOREIGN KEY (`partner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `support_tickets`
--

LOCK TABLES `support_tickets` WRITE;
/*!40000 ALTER TABLE `support_tickets` DISABLE KEYS */;
INSERT INTO `support_tickets` VALUES (1,3,'Thanh to├ín & Quyß║┐t to├ín','Quyß║┐t to├ín tuß║ºn 3 bß╗ï sai sß╗æ tiß╗ün','Cao','Ch├áo Admin, t├┤i kiß╗âm tra lß║íi quyß║┐t to├ín tuß║ºn 3 th├¼ thß║Ñy sß╗æ tiß╗ün payout l├á 1.224.000─æ nh╞░ng theo t├¡nh to├ín cß╗ºa t├┤i th├¼ phß║úi cao h╞ín. Booking BK-LATA-DLX-0002 c├│ total 2.040.000─æ, commission 15% = 306.000─æ, voucher deduction 510.000─æ, payout ─æ├║ng = 1.224.000─æ. Thß╗▒c ra ─æ├║ng rß╗ôi, t├┤i nhß║ºm. Xin lß╗ùi v├á cß║úm ╞ín ─æ├ú hß╗ù trß╗ú!','RESPONDED','Ch├áo bß║ín, t├┤i ─æ├ú kiß╗âm tra lß║íi v├á x├íc nhß║¡n con sß╗æ quyß║┐t to├ín tuß║ºn 3 l├á ch├¡nh x├íc: gross 2.040.000─æ - commission 15% (306.000─æ) - voucher LATA20 do partner chß╗ïu (510.000─æ) = payout 1.224.000─æ. Rß║Ñt vui v├¼ bß║ín ─æ├ú tß╗▒ kiß╗âm tra ─æ╞░ß╗úc! Nß║┐u c├│ thß║»c mß║»c g├¼ th├¬m h├úy li├¬n hß╗ç.','2026-04-26 07:35:46.000000','2026-04-27 07:35:46.000000'),(2,3,'─É╞ín ─æß║╖t ph├▓ng','Kh├ích kh├┤ng ─æß║┐n nh╞░ng kh├┤ng thß╗â ─æ├ính dß║Ñu No-Show','Trung b├¼nh','Booking BK-TLP-STD-0001, kh├ích Nguyß╗àn V─ân An ─æ├ú kh├┤ng ─æß║┐n check-in ng├áy h├┤m qua. T├┤i muß╗æn ─æ├ính dß║Ñu No-Show ─æß╗â hß╗ç thß╗æng tß╗▒ ─æß╗Öng giß╗» cß╗ìc 30% nh╞░ng kh├┤ng thß║Ñy n├║t n├áy ß╗ƒ giao diß╗çn. Mong Admin xß╗¡ l├╜ gi├║p.','RESPONDED','Ch├áo bß║ín, chß╗⌐c n─âng ─æ├ính dß║Ñu No-Show hiß╗çn chß╗ë Admin mß╗¢i thß╗▒c hiß╗çn ─æ╞░ß╗úc ─æß╗â ─æß║úm bß║úo kiß╗âm so├ít chß║╖t chß║╜. T├┤i ─æ├ú cß║¡p nhß║¡t booking BK-TLP-STD-0001 th├ánh NO_SHOW v├á cß╗ìc 30% (288.000─æ) ─æ├ú ─æ╞░ß╗úc giß╗» lß║íi. Trong phi├¬n bß║ún tß╗¢i ch├║ng t├┤i sß║╜ cho ph├⌐p Partner tß╗▒ ─æ├ính dß║Ñu sau 4h kß╗â tß╗½ giß╗¥ check-in.','2026-04-29 07:35:46.000000','2026-04-30 07:35:46.000000'),(3,3,'Kß╗╣ thuß║¡t','Trang doanh thu kh├┤ng hiß╗ân thß╗ï biß╗âu ─æß╗ô','Thß║Ñp','Khi t├┤i v├áo /partner/revenue, trang tß║úi b├¼nh th╞░ß╗¥ng nh╞░ng phß║ºn biß╗âu ─æß╗ô doanh thu theo tuß║ºn bß╗ï trß╗æng. Tr├¼nh duyß╗çt Chrome v124. Xin hß╗ù trß╗ú.','OPEN',NULL,'2026-05-03 07:35:46.000000','2026-05-03 07:35:46.000000'),(4,4,'C╞í sß╗ƒ l╞░u tr├║','Muß╗æn th├¬m ß║únh thumbnail cho Vinpearl Resort','Thß║Ñp','ß║ónh thumbnail hiß╗çn tß║íi cß╗ºa Vinpearl Resort & Spa Nha Trang (acc id=8) tr├┤ng h╞íi tß╗æi. T├┤i muß╗æn cß║¡p nhß║¡t ß║únh mß╗¢i ─æß║╣p h╞ín nh╞░ng kh├┤ng thß║Ñy chß╗ù chß╗ënh sß╗¡a trong giao diß╗çn Partner. Xin h╞░ß╗¢ng dß║½n.','CLOSED','Ch├áo bß║ín, hiß╗çn tß║íi chß╗⌐c n─âng thay ─æß╗òi thumbnail cß║ºn Admin hß╗ù trß╗ú. T├┤i ─æ├ú cß║¡p nhß║¡t ß║únh thumbnail mß╗¢i cho Vinpearl Resort cß╗ºa bß║ín. Trong phi├¬n bß║ún tß╗¢i, Partner sß║╜ tß╗▒ chß╗ënh sß╗¡a ─æ╞░ß╗úc trß╗▒c tiß║┐p tß╗½ trang N╞íi l╞░u tr├║ cß╗ºa t├┤i.','2026-04-19 07:35:46.000000','2026-04-22 07:35:46.000000'),(5,4,'Thanh to├ín & Quyß║┐t to├ín','Ch╞░a nhß║¡n ─æ╞░ß╗úc thanh to├ín tuß║ºn 2','Cao','Quyß║┐t to├ín tuß║ºn 2 (Apr 13-19) c├│ payout 4.592.000─æ, trß║íng th├íi ─æ├ú PAID nh╞░ng t├ái khoß║ún Vietcombank cß╗ºa t├┤i ch╞░a nhß║¡n ─æ╞░ß╗úc tiß╗ün. Sß╗æ TK: 9876543210, chß╗º TK: TRAN THI B. ─É├ú chß╗¥ 3 ng├áy rß╗ôi.','RESPONDED','Ch├áo bß║ín, t├┤i ─æ├ú kiß╗âm tra lß║íi. Giao dß╗ïch chuyß╗ân khoß║ún 4.592.000─æ ─æ├ú ─æ╞░ß╗úc xß╗¡ l├╜ ng├áy h├┤m qua, th╞░ß╗¥ng mß║Ñt 1-2 ng├áy l├ám viß╗çc ─æß╗â tiß╗ün vß╗ü t├ái khoß║ún. Nß║┐u sau 48h nß╗»a vß║½n ch╞░a nhß║¡n ─æ╞░ß╗úc, vui l├▓ng li├¬n hß╗ç lß║íi vß╗¢i m├ú giao dß╗ïch ─æß╗â t├┤i x├íc nhß║¡n vß╗¢i bß╗Ö phß║¡n t├ái ch├¡nh.','2026-04-30 07:35:46.000000','2026-05-01 07:35:46.000000'),(6,7,'─É╞ín ─æß║╖t ph├▓ng','Booking BK-ANM-GDN-0001 chß╗¥ x├íc nhß║¡n qu├í l├óu','Cao','Booking BK-ANM-GDN-0001 (Anam Villa, kh├ích Ho├áng V─ân H├╣ng, check-in +10 ng├áy) ─æang ß╗ƒ trß║íng th├íi PENDING_ADMIN_APPROVAL ─æ├ú h╞ín 2 ng├áy. T├┤i muß╗æn hß╗Åi Admin khi n├áo sß║╜ duyß╗çt ─æß╗â t├┤i chuß║⌐n bß╗ï ph├▓ng cho kh├ích.','OPEN',NULL,'2026-05-02 07:35:46.000000','2026-05-02 07:35:46.000000'),(7,7,'Kß╗╣ thuß║¡t','Kh├┤ng th├¬m ─æ╞░ß╗úc ph├▓ng mß╗¢i cho Ba Na Hills Villa','Trung b├¼nh','T├┤i v├áo acc5 Ba Na Hills Forest Villa, bß║Ñm n├║t Th├¬m ph├▓ng nh╞░ng trang b├ío lß╗ùi \"Chß╗ë c├│ thß╗â th├¬m ph├▓ng cho c╞í sß╗ƒ ─æ├ú ─æ╞░ß╗úc Admin duyß╗çt\". Trong khi acc5 ─æang c├│ trß║íng th├íi APPROVED. Xin kiß╗âm tra gi├║p.','RESPONDED','Ch├áo bß║ín, t├┤i ─æ├ú kiß╗âm tra v├á x├íc nhß║¡n acc5 Ba Na Hills Forest Villa ─æang ß╗ƒ trß║íng th├íi APPROVED. Lß╗ùi c├│ thß╗â do cache tr├¼nh duyß╗çt. H├úy thß╗¡ Ctrl+Shift+R ─æß╗â hard refresh. Nß║┐u vß║½n lß╗ùi, h├úy chß╗Ñp m├án h├¼nh v├á gß╗¡i lß║íi cho t├┤i.','2026-04-28 07:35:46.000000','2026-04-29 07:35:46.000000'),(8,8,'Voucher','Muß╗æn tß║ío voucher nh╞░ng kh├┤ng thß║Ñy C╞í sß╗ƒ l╞░u tr├║ cß╗ºa t├┤i','Trung b├¼nh','T├┤i v├áo /partner/vouchers, tab \"Voucher theo c╞í sß╗ƒ\" kh├┤ng hiß╗çn dropdown ─æß╗â chß╗ìn c╞í sß╗ƒ. T├┤i c├│ 2 homestay l├á Hoa L╞░ (id=6) v├á Mß╗Öc Nhi├¬n (id=7), cß║ú hai ─æß╗üu ─æang APPROVED nh╞░ng kh├┤ng hiß╗çn trong danh s├ích.','OPEN',NULL,'2026-05-01 07:35:46.000000','2026-05-01 07:35:46.000000'),(9,8,'Thanh to├ín & Quyß║┐t to├ín','Hß╗Åi vß╗ü lß╗ïch quyß║┐t to├ín h├áng tuß║ºn','Thß║Ñp','T├┤i muß╗æn hß╗Åi TravelMate thanh to├ín quyß║┐t to├ín cho partner v├áo ng├áy n├áo trong tuß║ºn? V├á sß╗æ tiß╗ün tß╗æi thiß╗âu ─æß╗â ─æ╞░ß╗úc thanh to├ín l├á bao nhi├¬u?','CLOSED','Ch├áo bß║ín! TravelMate thß╗▒c hiß╗çn quyß║┐t to├ín v├áo mß╗ùi Thß╗⌐ Ba h├áng tuß║ºn cho kß╗│ tuß║ºn tr╞░ß╗¢c (T2-CN). Kh├┤ng c├│ sß╗æ tiß╗ün tß╗æi thiß╗âu ΓÇö d├╣ chß╗ë 1 booking ─æ├ú ho├án tß║Ñt c┼⌐ng sß║╜ ─æ╞░ß╗úc thanh to├ín. Tiß╗ün chuyß╗ân vß╗ü t├ái khoß║ún ─æ─âng k├╜ trong mß╗Ñc Hß╗ô s╞í trong v├▓ng 1-2 ng├áy l├ám viß╗çc.','2026-04-14 07:35:46.000000','2026-04-16 07:35:46.000000');
/*!40000 ALTER TABLE `support_tickets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `partner_property_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_account_number` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_account_holder` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_branch` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin@travelmate.vn','$2a$10$o2YdU5h/0dkNDo2ZUijtee2Sxds517yp0Nsn5qsREyQxKQHLjcf3i','Admin TravelMate','Admin TravelMate','0901 234 567','ADMIN','ACTIVE',NULL,NULL,NULL,NULL,NULL,'2026-05-04 07:35:46.000000','2026-05-04 15:07:56.632140'),(2,'user@travelmate.vn','$2a$10$nBLe0AvDztis2xWon7ag/uYE812bTx9..8vCpXSUHmMaUnzhWnqci','Nguyß╗àn V─ân An','Nguyß╗àn V─ân An','0912 345 678','USER','ACTIVE',NULL,NULL,NULL,NULL,NULL,'2026-05-04 07:35:46.000000','2026-05-04 15:07:56.713869'),(3,'partner@travelmate.vn','$2a$10$ZeIdXbT3uugaRCrOFt5bTOc3SCPODhNcrnlf8fAkCchGYJdGslvtq','Sunrise Sapa Lodge','Sunrise Sapa Lodge','0933 456 789','PARTNER','ACTIVE','HOTEL','0123456789','MB Bank','NGUYEN VAN A','TP.HCM','2026-05-04 07:35:46.000000','2026-05-04 15:07:56.791627'),(4,'partner2@travelmate.vn','$2a$10$/9kEw3uwZlNwaLOiZULC..lvQyH.pe0OTUCfBZtfmVQLFS9DMFSuO','Blue Ocean Resort','Blue Ocean Resort','0944 567 890','PARTNER','ACTIVE','RESORT','9876543210','Vietcombank','TRAN THI B','─É├á Nß║╡ng','2026-05-04 07:35:46.000000','2026-05-04 15:07:56.869945'),(5,'user2@travelmate.vn','$2a$10$FsFOdcKQPqCnlIPA3j82ZeY7R6tMpdhavFO2bfqWUfJqF1Z4nloO2','Trß║ºn Thß╗ï Mai','Trß║ºn Thß╗ï Mai','0923 456 789','USER','ACTIVE',NULL,NULL,NULL,NULL,NULL,'2026-02-03 07:35:46.000000','2026-02-03 07:35:46.000000'),(6,'user3@travelmate.vn','$2a$10$FsFOdcKQPqCnlIPA3j82ZeY7R6tMpdhavFO2bfqWUfJqF1Z4nloO2','L├¬ V─ân ─Éß╗⌐c','L├¬ V─ân ─Éß╗⌐c','0934 567 890','USER','ACTIVE',NULL,NULL,NULL,NULL,NULL,'2026-02-18 07:35:46.000000','2026-02-18 07:35:46.000000'),(7,'partner3@travelmate.vn','$2a$10$NXcTZUcfkDtVU2QzcnU8ouQO9I7SC.JZzTnljpqyMwVRThR.uMBtu','Green Hills Villa','Green Hills Villa','0955 678 901','PARTNER','ACTIVE','VILLA','1122334455','Techcombank','LE VAN C','H├á Nß╗Öi','2026-05-04 07:35:46.000000','2026-05-04 15:07:56.947458'),(8,'partner4@travelmate.vn','$2a$10$Sdlws/gB5mQ/giQ1m6YE4elD.8kmeI7.Gan1bbkILirDvx3r.yqva','Mekong Homestay','Mekong Homestay','0966 789 012','PARTNER','ACTIVE','HOMESTAY','5544332211','Agribank','PHAM THI D','Cß║ºn Th╞í','2026-05-04 07:35:46.000000','2026-05-04 15:07:57.025109');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vouchers`
--

DROP TABLE IF EXISTS `vouchers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vouchers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `discount_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PERCENT',
  `discount_value` decimal(15,2) NOT NULL DEFAULT '0.00',
  `max_discount_amount` decimal(15,0) DEFAULT NULL,
  `min_order_amount` decimal(15,0) DEFAULT '0',
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `active` tinyint(1) DEFAULT '1',
  `voucher_scope` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER_GLOBAL',
  `cost_bearer` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ADMIN',
  `owner_id` bigint DEFAULT NULL,
  `accommodation_id` bigint DEFAULT NULL,
  `room_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `fk_vouchers_owner` (`owner_id`),
  KEY `FKcopiy2m64jtkdkhu516s3y64m` (`accommodation_id`),
  KEY `FKl0frfppemmjn91a5jyli9ape3` (`room_id`),
  CONSTRAINT `fk_vouchers_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKcopiy2m64jtkdkhu516s3y64m` FOREIGN KEY (`accommodation_id`) REFERENCES `accommodations` (`id`),
  CONSTRAINT `FKl0frfppemmjn91a5jyli9ape3` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vouchers`
--

LOCK TABLES `vouchers` WRITE;
/*!40000 ALTER TABLE `vouchers` DISABLE KEYS */;
INSERT INTO `vouchers` VALUES (1,'SUMMER10','╞»u ─æ├úi H├¿ 10%','Giß║úm 10% tß╗æi ─æa 500.000─æ cho mß╗ìi ─æß║╖t ph├▓ng m├╣a h├¿.','PERCENT',10.00,500000,500000,'2026-05-04','2026-08-02',1,'USER_GLOBAL','ADMIN',NULL,NULL,NULL,'2026-05-04 07:35:46.000000'),(2,'WELCOME50','Ch├áo mß╗½ng 50K','Giß║úm 50.000─æ cho ─æß║╖t ph├▓ng ─æß║ºu ti├¬n ΓÇö ├íp dß╗Ñng ─æ╞ín tß╗æi thiß╗âu 200.000─æ.','FIXED_AMOUNT',50000.00,NULL,200000,'2026-05-04','2026-06-03',1,'USER_GLOBAL','ADMIN',NULL,NULL,NULL,'2026-05-04 07:35:46.000000'),(3,'TRAVEL15','╞»u ─æ├úi TravelMate','Giß║úm 15% tß╗æi ─æa 1.000.000─æ cho ─æß║╖t ph├▓ng Resort hoß║╖c Villa cao cß║Ñp.','PERCENT',15.00,1000000,2000000,'2026-05-04','2026-07-03',1,'USER_GLOBAL','ADMIN',NULL,NULL,NULL,'2026-05-04 07:35:46.000000'),(4,'LATA20','LATA Hotel ╞»u ─É├úi 20%','Giß║úm 20% cho ─æß║╖t ph├▓ng tß║íi LATA Hotel ΓÇö partner chß╗ïu chi ph├¡.','PERCENT',20.00,800000,650000,'2026-05-04','2026-06-18',1,'PARTNER_ACCOMMODATION','PARTNER',3,1,NULL,'2026-05-04 07:35:46.000000'),(5,'VNT100K','Vinpearl Giß║úm 100K','Giß║úm 100.000─æ khi ─æß║╖t ph├▓ng Deluxe Ocean View tß║íi Vinpearl.','FIXED_AMOUNT',100000.00,NULL,2800000,'2026-05-04','2026-06-03',1,'PARTNER_ROOM','PARTNER',4,NULL,25,'2026-05-04 07:35:46.000000'),(6,'ANAM15','Anam Villa ╞»u ─É├úi 15%','Giß║úm 15% tß╗æi ─æa 1.200.000─æ cho ─æß║╖t ph├▓ng tß║íi The Anam Villa Nha Trang.','PERCENT',15.00,1200000,3000000,'2026-05-04','2026-07-03',1,'PARTNER_ACCOMMODATION','PARTNER',7,4,NULL,'2026-05-04 07:35:46.000000'),(7,'HOALUU50K','Hoa L╞░ Giß║úm 50K','Giß║úm 50.000─æ khi ─æß║╖t ph├▓ng Deluxe River View tß║íi Hoa L╞░ Riverside Homestay.','FIXED_AMOUNT',50000.00,NULL,600000,'2026-05-04','2026-06-18',1,'PARTNER_ROOM','PARTNER',8,NULL,20,'2026-05-04 07:35:46.000000');
/*!40000 ALTER TABLE `vouchers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'travelmate_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-04 15:11:02

