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
    public static VideoInfo videoInfo;
    @Autowired
    private VideoConfig videoConfig;
    private static ApplicationContext applicationContext;
    private static final ExecutorService threadPool = new ThreadPoolExecutor(
            3, 6,
            60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    /**
     * 生成两个VideoPull任务，扔进线程池中处理
     */
    public void startVideoPull() {
        VideoPull videoPull = applicationContext.getBean(VideoPull.class);
        //属性信息单线程拿取，保证后续的push子线程一定能够获取videoInfo
        videoInfo = videoPull.getInformation();
        threadPool.execute(videoPull);
    }

    /**
     *  生成VideoPush任务，扔进线程池中处理
     */
    public void startVideoPush() {
        VideoPush videoPush = applicationContext.getBean(VideoPush.class);
        videoPush.setVideoInfo(videoInfo);
        threadPool.execute(videoPush);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        VideoThreadPool.applicationContext = applicationContext;
    }
}
