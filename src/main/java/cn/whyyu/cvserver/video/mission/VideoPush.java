package cn.whyyu.cvserver.video.mission;

import cn.whyyu.cvserver.redis.RedisUtil;
import cn.whyyu.cvserver.video.VideoInfo;
import cn.whyyu.cvserver.video.threadpool.VideoThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
@Slf4j
@Scope(value = "prototype")
public class VideoPush implements Runnable{
    private VideoInfo videoInfo;

    @Override
    public void run() {
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                "F:/image/newImage/new.flv", videoInfo.getWidth(), videoInfo.getHeight());
        recorder.setFormat("flv");
        // h264编/解码器
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setGopSize((int) videoInfo.getFrameRate() * 2);
        // 视频帧率
        recorder.setFrameRate(4);
        try {
            recorder.start();
            Java2DFrameConverter converter = new Java2DFrameConverter();
            ConcurrentLinkedDeque<String> imageDeque = RedisUtil.imageDeque;

            int count = 0;
            while(!imageDeque.isEmpty() || count < 5) {
                while(!imageDeque.isEmpty()) {
                    BufferedImage bufferedImage = ImageIO.read(new File(imageDeque.removeFirst()));
                    recorder.record(converter.getFrame(bufferedImage));
                }
                // 因为队列可能被消费空，但是其实是图片传的没那么快，所以自旋等待
                Thread.sleep(200);
                count++;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }finally {
            //最后一定要结束并释放资源
            try {
                recorder.close();
            } catch (FrameRecorder.Exception e) {
                log.error("录制流关闭错误");
            }
        }
    }

    public VideoInfo getVideoInfo() {
        return this.videoInfo;
    }

    public void setVideoInfo(VideoInfo videoInfo) {
        this.videoInfo = videoInfo;
    }
}
