package com.rajlaxmi.jewellers.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {

    @Test
    void createsDirectoryResourceLocationForRelativeUploadPath() {
        assertThat(WebConfig.toResourceLocation("uploads"))
                .startsWith("file:")
                .endsWith("/uploads/");
    }

    @Test
    void normalizesParentSegmentsAndKeepsDirectorySuffix() {
        assertThat(WebConfig.toResourceLocation("storage/../uploads"))
                .startsWith("file:")
                .doesNotContain("/storage/")
                .endsWith("/uploads/");
    }
}
