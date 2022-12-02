package cn.whyyu.cvserver.video.mission;

import cn.whyyu.cvserver.ftp.FTPUtil;
import cn.whyyu.cvserver.redis.RedisUtil;
import cn.whyyu.cvserver.video.VideoInfo;
import cn.whyyu.cvserver.video.config.VideoConfig;
import cn.whyyu.cvserver.video.threadpool.VideoThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * 该类是对视频流截取为图片的任务封装
 * scope设置为原型，因为要从容器中取出多次新对象
 */
@Component
@Slf4j
@Scope(value = "prototype")
public class VideoPull implements Runnable{
    @Autowired
    private VideoConfig videoConfig;
    @Autowired
    RedisUtil redisUtil;
    private VideoInfo videoInfo;
    private FFmpegFrameGrabber grabber;

    public VideoInfo registerGrabber(String videoUrl) {
        grabber = new FFmpegFrameGrabber(videoUrl);
        try {
            grabber.start();
        } catch (FFmpegFrameGrabber.Exception e) {
            e.printStackTrace();
        }
        videoInfo = VideoInfo.builder().frameRate(grabber.getFrameRate())
                .timeLength(grabber.getLengthInTime())
                .width(grabber.getImageWidth())
                .height(grabber.getImageHeight())
                .videoCodecName(grabber.getVideoCodecName())
                .audioCodecName(grabber.getAudioCodecName()).build();
        return videoInfo;
    }

    /**
     * 负责将视频流每一秒截取为一张图片
     * 生成图片后理论上先上传到ftp图片服务器再把图片url消息传递到消息队列
     * 但我把ftp服务布置在本地，所以直接生成到ftp对应物理目录下即可
     */
    @Override
    public void run() {
        Frame frame;
        long interval = videoConfig.getInterval();
        long i  = 0L;
        Java2DFrameConverter converter = new Java2DFrameConverter();
        try {
            createDirectory();
            while(i < videoInfo.getTimeLength()) {
                grabber.setVideoTimestamp(i);
                frame = grabber.grabImage();
                BufferedImage bufferedImage = converter.getBufferedImage(frame);
                File imageFile = getFileName(i);
                ImageIO.write(bufferedImage, "jpg", imageFile);
//                if(ftpUtil.upload(imageFile.getPath(), "image/" + imageFile.getName())
//                        .equals(FTPUtil.UploadStatus.UploadNewFileSuccess)) {
//                    redisUtil.addRawImage(Collections.singletonMap("ftpURL", "image/" + imageFile.getName()));
//                }

                String linuxPath = videoConfig.getLinuxPath();
                redisUtil.addRawImage(Collections.singletonMap("ftpURL", linuxPath + imageFile.getName()));
//                redisUtil.addNewImage(Collections.singletonMap("ftpURL", linuxPath + imageFile.getName()));
                i += interval;
            }
        } catch (FrameGrabber.Exception e) {
            log.error("视频流开启或者抓帧过程中出现错误");
        } catch (IOException e) {
            log.error("图片文件写入错误");
        } finally {
            try {
                grabber.close();
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
                log.error("视频流关闭错误");
            }
        }
    }

    private void createDirectory() {
        String targetPath = videoConfig.getTargetPath();
        File file = new File(targetPath);
        file.mkdirs();
    }

    private File getFileName(long i) {
        String targetPath = videoConfig.getTargetPath();
        return Paths.get(targetPath + "/" + i + ".jpg").toFile();
    }

    public VideoInfo getVideoInfo() {
        return videoInfo;
    }

    public void close() {
        try {
            grabber.close();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }
}

