package cn.whyyu.cvserver.video;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoInfo {
    private double frameRate;
    private long timeLength;
    private String videoCodecName;
    private String audioCodecName;
    private int width;
    private int height;
}
