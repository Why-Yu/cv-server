package cn.whyyu.cvserver.ftp.config;

import lombok.Data;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@EnableConfigurationProperties
@PropertySource(value = {"classpath:ftpConfig.properties"})
@ConfigurationProperties(prefix = "ftp.client")
@Configuration
@Data
public class FTPConfig extends GenericObjectPoolConfig {
    String workingDirectory;
    String host;
    int port;
    String username;
    String password;
    String encoding;
    boolean passiveMode;
    int clientTimeout;
    int threadNum;
    int transferFileType;
    boolean renameUploaded;
    int retryTimes;
    int bufferSize;

    int maxTotal;
    int minldle;
    int maxldle;
    int maxWait;
    boolean blockWhenExhausted;
    boolean testOnBorrow;
    boolean testOnReturn;
    boolean testOnCreate;
    boolean testWhileldle;
    boolean lifo;
}
