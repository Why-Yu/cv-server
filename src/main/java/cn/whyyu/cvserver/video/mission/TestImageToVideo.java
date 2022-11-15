package cn.whyyu.cvserver.video.mission;

import cn.whyyu.cvserver.redis.RedisUtil;
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
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
public class TestImageToVideo {
    public void test() {
        File file = new File("F:/Image/newImage");
        File[] files = file.listFiles();
        Map<Integer, File> imageMap = new HashMap<Integer, File>();
        int num = 0;
        for (File imgFile : files) {
            imageMap.put(num, imgFile);
            num++;
        }

        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                "F:/image/newImage/new.mp4", 1920, 1088);
        recorder.setFormat("mp4");
        // h264编/解码器
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setGopSize((int) 30 * 2);
        // 视频帧率
        recorder.setFrameRate(4);
        try {
            recorder.start();
            Java2DFrameConverter converter = new Java2DFrameConverter();
            ConcurrentLinkedDeque<String> imageDeque = RedisUtil.imageDeque;

            for (int i = 0; i < imageMap.size() ; i++) {
                BufferedImage read = ImageIO.read(imageMap.get(i));
                recorder.record(converter.getFrame(read));
            }
        } catch (IOException e) {
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

}
