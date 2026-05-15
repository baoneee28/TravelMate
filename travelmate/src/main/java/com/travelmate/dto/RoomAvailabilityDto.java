package com.travelmate.dto;

import java.math.BigDecimal;

/**
 * RoomAvailabilityDto — Kết quả kiểm tra tình trạng phòng theo khoảng ngày.
 *
 * Công thức:
 *   total        = availableQuantity (DB) + sumAllActiveBookings
 *   occupied     = sumOverlappingBookings (trong khoảng ngày yêu cầu)
 *   available    = total - occupied
 *   status       = AVAILABLE | LIMITED | FULL
 */
public class RoomAvailabilityDto {

    private Long roomId;
    private String roomCode;
    private String roomName;
    private String roomCategory;       // STANDARD, DELUXE, FAMILY, VIP, SUITE, OTHER
    private Long accommodationId;
    private String accommodationName;
    private String city;
    private String propertyType;       // HOTEL, VILLA, HOMESTAY, RESORT
    private String ownerName;          // tên partner
    private BigDecimal pricePerNight;

    private int totalQuantity;
    private int occupiedQuantity;
    private int availableQuantity;

    /** AVAILABLE | LIMITED | FULL */
    private String status;

    public RoomAvailabilityDto() {}

    public RoomAvailabilityDto(Long roomId, String roomCode, String roomName, String roomCategory,
                               Long accommodationId, String accommodationName, String city,
                               String propertyType, String ownerName, BigDecimal pricePerNight,
                               int totalQuantity, int occupiedQuantity, int availableQuantity) {
        this.roomId = roomId;
        this.roomCode = roomCode;
        this.roomName = roomName;
        this.roomCategory = roomCategory;
        this.accommodationId = accommodationId;
        this.accommodationName = accommodationName;
        this.city = city;
        this.propertyType = propertyType;
        this.ownerName = ownerName;
        this.pricePerNight = pricePerNight;
        this.totalQuantity = totalQuantity;
        this.occupiedQuantity = occupiedQuantity;
        this.availableQuantity = availableQuantity;
        this.status = computeStatus(totalQuantity, availableQuantity);
    }

    private static String computeStatus(int total, int available) {
        if (available <= 0) return "FULL";
        if (total > 0 && available <= (int) Math.ceil(total * 0.3)) return "LIMITED";
        return "AVAILABLE";
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getRoomId()              { return roomId; }
    public String getRoomCode()          { return roomCode; }
    public String getRoomName()          { return roomName; }
    public String getRoomCategory()      { return roomCategory; }
    public Long getAccommodationId()     { return accommodationId; }
    public String getAccommodationName() { return accommodationName; }
    public String getCity()              { return city; }
    public String getPropertyType()      { return propertyType; }
    public String getOwnerName()         { return ownerName; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public int getTotalQuantity()        { return totalQuantity; }
    public int getOccupiedQuantity()     { return occupiedQuantity; }
    public int getAvailableQuantity()    { return availableQuantity; }
    public String getStatus()            { return status; }

    /** % còn trống (0–100) — dùng cho progress bar */
    public int getAvailablePercent() {
        if (totalQuantity == 0) return 0;
        return (int) Math.round(availableQuantity * 100.0 / totalQuantity);
    }
}
