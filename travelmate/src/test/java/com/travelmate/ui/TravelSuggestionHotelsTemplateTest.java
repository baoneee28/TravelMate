package com.travelmate.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TravelSuggestionHotelsTemplateTest {

    private static final Path HOTELS_TEMPLATE =
            Path.of("src/main/resources/templates/user/hotels.html");
    private static final Path TRAVEL_TEMPLATE =
            Path.of("src/main/resources/templates/user/travel.html");
    private static final Path USER_HEADER_FRAGMENT =
            Path.of("src/main/resources/templates/fragments/user-header.html");

    @Test
    void searchResultTabUsesTravelSuggestionLabelAndStaysOnAccommodationsPage() throws IOException {
        String html = Files.readString(HOTELS_TEMPLATE);

        assertThat(html).contains("Gợi ý du lịch tại điểm đến");
        assertThat(html).doesNotContain("Địa điểm gần đây");
        assertThat(html).contains("href=\"#travelSuggestSection\"");
        assertThat(html).contains("th:if=\"${showTravelSuggestSection}\"");
        assertThat(html).contains("th:unless=\"${showTravelSuggestSection}\"");
        assertThat(html).contains("th:href=\"@{/travel}\"");
        assertThat(html).contains("fragments/user-header :: header(true, 'hotels')");
        assertThat(Files.readString(USER_HEADER_FRAGMENT)).contains("GỢI Ý DU LỊCH");
        assertThat(html).doesNotContain("<li><a href=\"/accommodations\">GỢI Ý DU LỊCH</a></li>");
        assertThat(html).doesNotContain("bấm card");
    }

    @Test
    void travelSuggestionSectionIsRenderedAfterAccommodationCards() throws IOException {
        String html = Files.readString(HOTELS_TEMPLATE);

        int resultsHeader = html.indexOf("class=\"results-header\"");
        int hotelListArea = html.indexOf("id=\"hotelListArea\"");
        int travelSection = html.indexOf("id=\"travelSuggestSection\"");
        int hotelCards = html.indexOf("Hotel Cards");

        assertThat(resultsHeader).isGreaterThanOrEqualTo(0);
        assertThat(hotelListArea).isGreaterThan(resultsHeader);
        assertThat(hotelCards).isGreaterThan(hotelListArea);
        assertThat(travelSection).isGreaterThan(resultsHeader);
        assertThat(travelSection).isGreaterThan(hotelCards);
        assertThat(html).contains("document.getElementById('hotelListArea')");
    }

    @Test
    void travelPostCardsOpenRealSourceInNewTabWithoutModal() throws IOException {
        String html = Files.readString(HOTELS_TEMPLATE);

        assertThat(html).contains("onclick=\"openTravelPostSource(this)\"");
        assertThat(html).contains("window.open(url, '_blank', 'noopener,noreferrer')");
        assertThat(html).contains("target=\"_blank\"");
        assertThat(html).contains("rel=\"noopener noreferrer\"");
        assertThat(html).doesNotContain("openTsModal");
        assertThat(html).doesNotContain("tsdm-overlay");
    }

    @Test
    void travelSuggestionCardsShowSourceAndReadMoreButton() throws IOException {
        String html = Files.readString(HOTELS_TEMPLATE);

        assertThat(html).contains("ts-card-img");
        assertThat(html).contains("ts-card-title");
        assertThat(html).contains("ts-card-summary");
        assertThat(html).contains("ts-meta-src");
        assertThat(html).contains("Đọc thêm");
    }

    @Test
    void travelSuggestionCardsKeepAccommodationSearchVisibleAndDoNotReplaceResults() throws IOException {
        String html = Files.readString(HOTELS_TEMPLATE);

        assertThat(html).contains("id=\"hotelListArea\"", "id=\"travelSuggestSection\"");
        assertThat(html.indexOf("id=\"hotelListArea\""))
                .isLessThan(html.indexOf("id=\"travelSuggestSection\""));
        assertThat(html).contains("Danh sách nơi lưu trú vẫn được giữ nguyên theo bộ lọc hiện tại.");
    }

    @Test
    void travelSuggestionSectionKeepsReturnPathToAccommodationResults() throws IOException {
        String html = Files.readString(HOTELS_TEMPLATE);

        assertThat(html).contains("ts-context");
        assertThat(html).contains("showStayResultsFromSuggestion()");
        assertThat(html).contains("Danh sách khách sạn");
        assertThat(html).contains("Danh sách nơi lưu trú vẫn được giữ nguyên theo bộ lọc hiện tại.");
    }

    @Test
    void travelPageRemainsStandaloneOverview() throws IOException {
        String html = Files.readString(TRAVEL_TEMPLATE);

        assertThat(html).contains("Cẩm nang du lịch");
        assertThat(html).contains("Địa điểm tham quan");
        assertThat(html).contains("Cần thiết cho du lịch");
        assertThat(html).contains("destinationMode");
    }
}
