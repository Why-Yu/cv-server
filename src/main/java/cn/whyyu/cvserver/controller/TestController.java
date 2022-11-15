package cn.whyyu.cvserver.controller;

import cn.whyyu.cvserver.ftp.FTPUtil;
import cn.whyyu.cvserver.redis.RedisUtil;
import cn.whyyu.cvserver.video.mission.TestImageToVideo;
import cn.whyyu.cvserver.video.threadpool.VideoThreadPool;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {
    @Autowired
    VideoThreadPool videoThreadPool;
    @Autowired
    FTPUtil ftpUtil;
    @Autowired
    RedisUtil redisUtil;


    @RequestMapping("/config")
    public void getConfig() throws IOException {
//        videoThreadPool.startVideoPull();
//        videoThreadPool.startVideoPush();
//        ftpUtil.download("image/rawImage/0.jpg", "F:/Image/newImage/2000000.jpg");
        TestImageToVideo test = new TestImageToVideo();
        test.test();
    }

    @RequestMapping("/videoUpload")
    public void videoUpload() {
        videoThreadPool.startVideoPull();
        redisUtil.createGroup("newImage", "newImageGroup");
        redisUtil.registerDownloadListener();
        videoThreadPool.startVideoPush();
    }
}
