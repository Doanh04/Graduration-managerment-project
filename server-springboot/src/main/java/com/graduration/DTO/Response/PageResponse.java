package com.graduration.DTO.Response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {
    List<T> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean first;
    boolean last;

    public static <S, T> PageResponse<T> from(Page<S> source, Function<S, T> mapper) {
        return PageResponse.<T>builder()
                .content(source.getContent().stream().map(mapper).toList())
                .page(source.getNumber())
                .size(source.getSize())
                .totalElements(source.getTotalElements())
                .totalPages(source.getTotalPages())
                .first(source.isFirst())
                .last(source.isLast())
                .build();
    }

    public static <T> PageResponse<T> of(List<T> content) {
        return PageResponse.<T>builder()
                .content(content)
                .page(0)
                .size(content.size())
                .totalElements(content.size())
                .totalPages(content.isEmpty() ? 0 : 1)
                .first(true)
                .last(true)
                .build();
    }
}
