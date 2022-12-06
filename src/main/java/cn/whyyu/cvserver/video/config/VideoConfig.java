package cn.whyyu.cvserver.video.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@EnableConfigurationProperties(VideoConfig.class)
@PropertySource(value = {"classpath:videoConfig.properties"})
@ConfigurationProperties(prefix = "video")
@Configuration
@Data
public class VideoConfig {
    private String targetPath;
    private String linuxPath;
    private String url;
    private String pushUrlPrefix;
    private String nginxUrl;
    private Long interval;
    private String downloadPath;
}
