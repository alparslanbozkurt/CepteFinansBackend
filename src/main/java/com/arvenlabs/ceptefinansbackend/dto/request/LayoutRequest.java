package com.arvenlabs.ceptefinansbackend.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class LayoutRequest {
    private List<String> widgets;
}
