package cn.whyyu.cvserver;

import cn.whyyu.cvserver.ftp.FTPUtil;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CvServerApplicationTests {
    @Autowired
    FTPUtil ftpUtil;

    @Test
    void getMetaData() {
        String url = "D:/ftpResource/image";
        ftpUtil.createDirectory(url);
    }

}
