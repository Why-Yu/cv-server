package cn.whyyu.cvserver.video.threadpool;

import cn.whyyu.cvserver.video.VideoInfo;
import cn.whyyu.cvserver.video.config.VideoConfig;
import cn.whyyu.cvserver.video.mission.VideoPull;
import cn.whyyu.cvserver.video.mission.VideoPush;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * 将视频流截取为图片的线程池，这里设置为两个线程
 * 一个负责截取奇数秒的图片，一个负责偶数秒的图片
 */
@Component
public class VideoThreadPool implements ApplicationContextAware {
    //存储视频的相关元数据，key为url
    //！！！该map目前没有清除元素的代码，长期使用会导致内存溢出，修改比较麻烦，后续再说
    public static ConcurrentHashMap<String, VideoInfo> videoInfoMap;
    @Autowired
    private VideoConfig videoConfig;
    private static ApplicationContext applicationContext;
    private static final ExecutorService threadPool = new ThreadPoolExecutor(
            3, 6,
            60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    /**
     * 生成VideoPull任务，扔进线程池中处理
     * videoURL为视频文件全路径名
     */
    public void startVideoPull(String videoURL) {
        VideoPull videoPull = applicationContext.getBean(VideoPull.class);
        //属性信息单线程拿取，保证后续的push子线程一定能够获取videoInfo
        VideoInfo videoInfo = videoPull.registerGrabber(videoURL);
        videoInfoMap.put(videoURL, videoInfo);
        threadPool.execute(videoPull);
    }

    /**
     *  生成VideoPush任务，扔进线程池中处理
     *  videoURL为视频文件全路径名，负责到map中获取videoInfo
     *  也依据这个url生成推送视频流的url
     */
    public String startVideoPush(String videoURL) {
        VideoPush videoPush = applicationContext.getBean(VideoPush.class);
        videoPush.setVideoInfo(videoInfoMap.get(videoURL));
        int beginIndex = videoURL.lastIndexOf("/") + 1;
        int endIndex = videoURL.indexOf(".");
        String pushUrlSuffix = videoURL.substring(beginIndex, endIndex);
        // 注册recorder，以便复用该对象
        videoPush.registerRecorder(videoConfig.getPushUrlPrefix() + pushUrlSuffix);
        threadPool.execute(videoPush);
        return videoConfig.getPushUrlPrefix() + pushUrlSuffix;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        VideoThreadPool.applicationContext = applicationContext;
    }
}
