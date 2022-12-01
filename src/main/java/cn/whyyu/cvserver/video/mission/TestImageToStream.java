package cn.whyyu.cvserver.video.mission;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 将图片按照一定帧率发布为http-flv视频流
 * 需要配合嵌入http-flv插件的nginx发布
 * nginx已配置好，启动nginx.exe即可
 * 当图片队列消费为空时，显示视频流的窗口会卡住在最后一帧，不会报错
 */
@Slf4j
public class TestImageToStream {
    public void test() {
        File file = new File("D:/ftpResource/image/result");
        File[] files = file.listFiles();
        TreeMap<Long, File> imageMap = new TreeMap<>();
        for (File imageFile : files) {
            long timestamp = getTimestamp(imageFile);
            imageMap.put(timestamp, imageFile);
        }
        String rtmpURL = "rtmp://127.0.0.1:1935/live/camera1";
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                rtmpURL, 1920, 1088);
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
            Java2DFrameConverter converter = new Java2DFrameConverter();

            while(!imageMap.isEmpty()) {
                BufferedImage read = ImageIO.read(imageMap.pollFirstEntry().getValue());
                recorder.record(converter.getFrame(read));
                log.info("放入一帧");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                recorder.close();
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
        }
    }

    public long getTimestamp(File file) {
        String fileName = file.getName();
        return Long.parseLong(fileName.substring(0, fileName.lastIndexOf(".")));
    }

    public static void main(String[] args) {
        TestImageToStream test = new TestImageToStream();
        test.test();
    }
}
