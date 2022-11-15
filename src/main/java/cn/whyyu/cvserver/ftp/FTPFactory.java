package cn.whyyu.cvserver.ftp;

import cn.whyyu.cvserver.ftp.config.FTPConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FTPFactory implements PooledObjectFactory<FTPClient> {
    @Autowired
    FTPConfig config;


    @Override
    public void activateObject(PooledObject<FTPClient> pooledObject) throws Exception {
        FTPClient ftpClient = pooledObject.getObject();
        ftpClient.connect(config.getHost(),config.getPort());
        ftpClient.login(config.getUsername(), config.getPassword());
        ftpClient.setControlEncoding(config.getEncoding());
        ftpClient.changeWorkingDirectory(config.getWorkingDirectory());
        //设置上传文件类型为二进制，否则将无法打开文件
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    }

    @Override
    public void destroyObject(PooledObject<FTPClient> pooledObject) throws Exception {
        FTPClient ftpClient = pooledObject.getObject();
        if (ftpClient != null) {
            try {
                ftpClient.disconnect();
                log.debug("销毁ftp连接");
            } catch (Exception e) {
                log.error("销毁ftpClient异常，error：");
            }
        }
    }

    @Override
    public PooledObject<FTPClient> makeObject() throws Exception {
        FTPClient ftpClient = new FTPClient();
        ftpClient.setConnectTimeout(config.getClientTimeout());
        ftpClient.connect(config.getHost(), config.getPort());
        int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            return null;
        }
        boolean success;
        success = ftpClient.login(config.getUsername(), config.getPassword());
        if (!success) {
            return null;
        }
        ftpClient.setFileType(config.getTransferFileType());
        ftpClient.setBufferSize(1024);
        ftpClient.setControlEncoding(config.getEncoding());
        if (config.isPassiveMode()) {
            ftpClient.enterLocalPassiveMode();
        }
        log.debug("创建ftp连接");
        return new DefaultPooledObject<>(ftpClient);
    }

    @Override
    public void passivateObject(PooledObject<FTPClient> pooledObject) throws Exception {
        FTPClient ftpClient = pooledObject.getObject();
        try {
            ftpClient.changeWorkingDirectory(config.getWorkingDirectory());
            ftpClient.logout();
            if (ftpClient.isConnected()) {
                ftpClient.disconnect();
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not disconnect from server.", e);
        }
    }

    @Override
    public boolean validateObject(PooledObject<FTPClient> pooledObject) {
        FTPClient ftpClient = pooledObject.getObject();
        try {
            return ftpClient != null && ftpClient.sendNoOp();
        } catch (Exception e) {
            return false;
        }
    }

    public FTPConfig getConfig() {
        return config;
    }
}
