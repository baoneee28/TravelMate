package com.travelmate.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminTravelPostSidebarTest {

    private static final Path ADMIN_TEMPLATE_DIR = Path.of("src/main/resources/templates/admin");

    @Test
    void everyAdminPageLinksToTravelPostCms() throws IOException {
        List<Path> templates;
        try (var stream = Files.list(ADMIN_TEMPLATE_DIR)) {
            templates = stream
                    .filter(path -> path.getFileName().toString().endsWith(".html"))
                    .toList();
        }

        assertThat(templates).isNotEmpty();
        for (Path template : templates) {
            assertThat(Files.readString(template))
                    .as(template.getFileName().toString())
                    .contains("/admin/travel-posts");
        }
    }
}
