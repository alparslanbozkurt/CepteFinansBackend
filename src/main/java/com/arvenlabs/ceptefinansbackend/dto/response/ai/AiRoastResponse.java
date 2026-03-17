package com.arvenlabs.ceptefinansbackend.dto.response.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiRoastResponse {
    private String roast;
    private int score;
}
