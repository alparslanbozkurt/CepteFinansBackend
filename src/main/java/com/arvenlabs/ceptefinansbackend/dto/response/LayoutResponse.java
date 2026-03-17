package com.arvenlabs.ceptefinansbackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class LayoutResponse {
    private UUID id;
    private List<String> widgets;
    private LocalDateTime updatedAt;
}
