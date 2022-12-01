package cn.whyyu.cvserver.video.mission;

import cn.whyyu.cvserver.redis.RedisUtil;
import cn.whyyu.cvserver.video.VideoInfo;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
@Slf4j
@Scope(value = "prototype")
public class VideoPush implements Runnable{
    private VideoInfo videoInfo;
    private FFmpegFrameRecorder recorder;

    public void registerRecorder(String rtmpURL) {
        recorder = new FFmpegFrameRecorder(
                rtmpURL, videoInfo.getWidth(), videoInfo.getHeight());
        recorder.setFormat("flv");
        recorder.setInterleaved(true);
        // h264编/解码器
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setGopSize((int) 4 * 2);
        // 视频帧率
        recorder.setFrameRate(4);

        Map<String, String> videoOption = new HashMap<>();
        // 该参数用于降低延迟
        videoOption.put("tune", "zerolatency");
        videoOption.put("preset", "ultrafast");
        // 画面质量参数，0~51；18~28是一个合理范围
        videoOption.put("crf", "28");
        recorder.setOptions(videoOption);
        try {
            recorder.start();
        } catch (FFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            Java2DFrameConverter converter = new Java2DFrameConverter();
            ConcurrentLinkedDeque<String> imageDeque = RedisUtil.imageDeque;

            int count = 0;
            while(!imageDeque.isEmpty() || count < 1000) {
                while(!imageDeque.isEmpty()) {
                    BufferedImage bufferedImage = ImageIO.read(new File(imageDeque.removeFirst()));
                    recorder.record(converter.getFrame(bufferedImage));
                }
                // 因为队列可能被消费空，原因可以是图片传的没那么快，也可以是正在切换摄像头
                // 所以自旋等待
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
