package com.travelmate.data;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class TravelPostSeedSqlTest {

    private static final Path DB_SEED = Path.of("src/main/resources/travelmate_db.sql");
    private static final List<String> DEMO_DESTINATION_SLUGS = List.of(
            "da-lat",
            "nha-trang",
            "hoi-an",
            "da-nang",
            "sa-pa",
            "phu-quoc"
    );
    private static final List<String> REQUIRED_ALIAS_TEST_SLUGS = List.of(
            "da-lat",
            "nha-trang",
            "da-nang",
            "hoi-an",
            "sa-pa",
            "lao-cai",
            "ha-giang",
            "ninh-binh",
            "ha-noi",
            "phu-quoc",
            "ho-chi-minh",
            "can-tho",
            "quang-ninh",
            "yen-bai",
            "son-la"
    );

    @Test
    void travelPostsSeedContainsAtLeastTwoVisiblePostsForEachDemoDestination() throws IOException {
        String travelPostsInsert = travelPostsInsertBlocks();

        for (String slug : DEMO_DESTINATION_SLUGS) {
            assertThat(countLiteral(travelPostsInsert, "'" + slug + "'"))
                    .as("destination_slug %s should have at least 2 seeded posts", slug)
                    .isGreaterThanOrEqualTo(2);
        }

        assertThat(countLiteral(travelPostsInsert, "'VISIBLE'"))
                .as("all demo travel posts should be visible to users")
                .isGreaterThanOrEqualTo(12);
    }

    @Test
    void travelPostsSeedCoversAllDestinationAliasTestCases() throws IOException {
        String travelPostsInsert = travelPostsInsertBlocks();

        for (String slug : REQUIRED_ALIAS_TEST_SLUGS) {
            assertThat(countLiteral(travelPostsInsert, "'" + slug + "'"))
                    .as("destination_slug %s should have seeded travel suggestions", slug)
                    .isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void travelPostSchemaStoresDestinationNameAndSlug() throws IOException {
        String sql = Files.readString(DB_SEED);

        assertThat(sql).contains("destination_name VARCHAR(100)");
        assertThat(sql).contains("destination_slug VARCHAR(120)");
        assertThat(sql).contains("UNIQUE KEY uk_tp_source_url (source_url)");
        assertThat(sql).contains("INSERT INTO travel_posts (title, destination_name, destination_slug");
        assertThat(sql).doesNotContain("INSERT INTO travel_posts (title, destination, destination_slug");
    }

    @Test
    void travelPostSourceUrlsAreRealExternalUrls() throws IOException {
        String travelPostsInsert = travelPostsInsertBlocks();
        Matcher matcher = Pattern.compile("'Vietnam\\.travel',\\s*'(https://[^']+)'").matcher(travelPostsInsert);
        int sourceUrlCount = 0;

        while (matcher.find()) {
            String sourceUrl = matcher.group(1);
            sourceUrlCount++;
            assertThat(sourceUrl).startsWith("https://vietnam.travel/");
            assertThat(sourceUrl)
                    .doesNotContain("localhost")
                    .doesNotContain("javascript:")
                    .doesNotEndWith("#");
        }

        assertThat(sourceUrlCount)
                .as("each seeded travel post should have a real source_url")
                .isGreaterThanOrEqualTo(20);
        assertThat(travelPostsInsert)
                .doesNotContain("'#'")
                .doesNotContain("NULL")
                .doesNotContain("javascript:void(0)")
                .doesNotContain("localhost");
    }

    @Test
    void travelPostSeedCanRunMoreThanOnceWithoutDuplicatingRows() throws IOException {
        String travelPostsInsert = travelPostsInsertBlocks();

        assertThat(travelPostsInsert).contains("ON DUPLICATE KEY UPDATE");
        assertThat(travelPostsInsert).contains("updated_at = VALUES(updated_at)");
    }

    @Test
    void sqlAddsAccommodationDataForTravelPostOnlyDestinations() throws IOException {
        String sql = Files.readString(DB_SEED);

        assertThat(sql).contains("Sapa Cloud Valley Hotel");
        assertThat(sql).contains("Sunset Pearl Resort Phú Quốc");
        assertThat(sql).contains("Azerai Cần Thơ Resort");
        assertThat(sql).contains("'SPC-STD'");
        assertThat(sql).contains("'SPQ-DLX'");
        assertThat(sql).contains("'AZC-DLX'");
        assertThat(sql).contains("--   rooms               : 36");
    }

    private static String travelPostsInsertBlocks() throws IOException {
        String sql = Files.readString(DB_SEED);
        Matcher matcher = Pattern.compile("INSERT INTO travel_posts[\\s\\S]*?;").matcher(sql);
        StringBuilder blocks = new StringBuilder();
        while (matcher.find()) {
            blocks.append(matcher.group()).append('\n');
        }
        assertThat(blocks)
                .as("travel_posts insert blocks should exist")
                .isNotEmpty();
        return blocks.toString();
    }

    private static int countLiteral(String source, String literal) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(literal, index)) >= 0) {
            count++;
            index += literal.length();
        }
        return count;
    }
}
