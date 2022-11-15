package cn.whyyu.cvserver.ftp;

import cn.whyyu.cvserver.ftp.config.FTPConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class FTPPoolService {
    private GenericObjectPool<FTPClient> pool;
    @Autowired
    private FTPConfig config;
    @Autowired
    private FTPFactory factory;

    @PostConstruct
    private void initPool() {
        this.pool = new GenericObjectPool<>(this.factory, this.config);
    }

    /**
     * 获取ftpClient
     */
    public FTPClient borrowObject() {
        if (this.pool != null) {
            try {
                return this.pool.borrowObject();
            } catch (Exception e) {
                log.error("获取 FTPClient 失败 ", e);
            }
        }
        return null;
    }

    /**
     * 归还 ftpClient
     */
    public void returnObject(FTPClient ftpClient) {
        if (this.pool != null && ftpClient != null) {
            this.pool.returnObject(ftpClient);
        }
    }


    public FTPConfig getFtpConfig() {
        return config;
    }

}
