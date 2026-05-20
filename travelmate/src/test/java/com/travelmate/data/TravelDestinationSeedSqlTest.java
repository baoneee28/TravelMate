package com.travelmate.data;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TravelDestinationSeedSqlTest {

    private static final Path DB_SEED = Path.of("src/main/resources/travelmate_db.sql");
    private static final List<String> REQUIRED_SLUGS = List.of(
            "da-lat",
            "nha-trang",
            "da-nang",
            "hoi-an",
            "sa-pa",
            "ha-giang",
            "ninh-binh",
            "ha-noi",
            "phu-quoc",
            "ho-chi-minh",
            "can-tho",
            "quang-ninh",
            "yen-bai",
            "son-la",
            "lao-cai"
    );

    @Test
    void sqlCreatesAndSeedsHomepageDestinations() throws IOException {
        String sql = Files.readString(DB_SEED);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS travel_destinations");
        assertThat(sql).contains("slug VARCHAR(120) NOT NULL UNIQUE");
        assertThat(sql).contains("ON DUPLICATE KEY UPDATE");

        for (String slug : REQUIRED_SLUGS) {
            assertThat(sql)
                    .as("destination slug %s should be seeded for homepage demo", slug)
                    .contains("'" + slug + "'");
        }
    }
}
