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
//        String url = "000001";
//        Long a = Long.parseLong(url);
//        System.out.println(a);
    }

}
