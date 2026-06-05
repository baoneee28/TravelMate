package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Room;
import com.travelmate.entity.enums.PropertyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CommissionService - Ty le hoa hong theo loai luu tru va phong")
class CommissionServiceTest {

    private final CommissionService commissionService = new CommissionService();

    @Test
    @DisplayName("Ty le mac dinh dung cho Hotel, Resort, Villa va Homestay")
    void propertyTypeDefaultsMatchPublishedPolicy() {
        assertThat(commissionService.getCommissionRate(PropertyType.HOTEL)).isEqualByComparingTo("0.15");
        assertThat(commissionService.getCommissionRate(PropertyType.RESORT)).isEqualByComparingTo("0.18");
        assertThat(commissionService.getCommissionRate(PropertyType.VILLA)).isEqualByComparingTo("0.12");
        assertThat(commissionService.getCommissionRate(PropertyType.HOMESTAY)).isEqualByComparingTo("0.10");
    }

    @Test
    @DisplayName("Phong Hotel co override su dung dung ty le rieng thay vi mac dinh Hotel")
    void hotelRoomOverrideWinsOverPropertyTypeDefault() {
        Accommodation hotel = new Accommodation();
        hotel.setPropertyType(PropertyType.HOTEL);
        Room suite = new Room();
        suite.setAccommodation(hotel);
        suite.setCommissionRateOverride(new BigDecimal("18.00"));

        assertThat(commissionService.isRoomOverride(suite)).isTrue();
        assertThat(commissionService.getEffectiveCommissionRate(suite)).isEqualByComparingTo("0.1800");
        assertThat(commissionService.calculateCommission(new BigDecimal("1000000"), suite))
                .isEqualByComparingTo("180000");
    }

    @Test
    @DisplayName("Phong VIP Hotel override 20% tinh dung tren base duoc truyen vao")
    void hotelVipOverride_calculatesCommissionFromProvidedBase() {
        Accommodation hotel = new Accommodation();
        hotel.setPropertyType(PropertyType.HOTEL);
        Room vipRoom = new Room();
        vipRoom.setAccommodation(hotel);
        vipRoom.setCommissionRateOverride(new BigDecimal("20.00"));

        assertThat(commissionService.calculateCommission(new BigDecimal("600000"), vipRoom))
                .isEqualByComparingTo("120000");
    }
}
