package com.enis.sectio.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void mapsContentAndPreservesPaginationMetadata() {
        PageImpl<Integer> page = new PageImpl<>(
                List.of(10, 20),
                PageRequest.of(1, 2),
                5
        );

        PageResponse<String> response = PageResponse.from(page, value -> "item-" + value);

        assertThat(response.content()).containsExactly("item-10", "item-20");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }
}
