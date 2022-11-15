package cn.whyyu.cvserver.redis;

import cn.whyyu.cvserver.ftp.FTPUtil;
import cn.whyyu.cvserver.video.config.VideoConfig;
import cn.whyyu.cvserver.video.threadpool.VideoThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class ImageStreamListener implements StreamListener<String, MapRecord<String, String, String>> {
    @Autowired
    FTPUtil ftpUtil;
    @Autowired
    public VideoConfig videoConfig;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
       Map<String, String> parameter = message.getValue();
        System.out.println(parameter);
       String ftpURL = parameter.get("ftpURL");
       String fileName = ftpURL.substring(ftpURL.lastIndexOf("/") + 1);
//       try {
//           ftpUtil.download(ftpURL, videoConfig.getDownloadPath() + "/" + fileName);
           RedisUtil.imageDeque.add(videoConfig.getDownloadPath() + "/" + fileName);
//       } catch (IOException e) {
//           e.printStackTrace();
//       }

    }
}
