package com.trsang.doan2.dtos.facebook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PictureData {
    private String url;
    private int width;
    private int height;
    private boolean is_silhouette;
}
