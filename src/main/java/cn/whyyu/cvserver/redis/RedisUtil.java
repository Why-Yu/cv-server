package cn.whyyu.cvserver.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Component
public class RedisUtil {
    @Autowired
    RedisTemplate<String,Object> redisTemplate;
    @Autowired
    RedisConnectionFactory redisConnectionFactory;
    @Autowired
    public ImageStreamListener imageStreamListener;
    public static ConcurrentLinkedDeque<String> imageDeque = new ConcurrentLinkedDeque<>();

    public RecordId addRawImage(Map<String, String> parameter) {
        StringRecord record = StreamRecords.string(parameter).withStreamKey("rawImage");
        return redisTemplate.opsForStream().add(record);
    }

    public RecordId addNewImage(Map<String, String> parameter) {
        StringRecord record = StreamRecords.string(parameter).withStreamKey("newImage");
        return redisTemplate.opsForStream().add(record);
    }

    public void createGroup(String key, String groupName) {
        redisTemplate.opsForStream().createGroup(key, groupName);
    }


    /**
     * 注册newImage stream事件监听器
     */
    public void registerDownloadListener() {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> containerOptions = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder().pollTimeout(Duration.ofMillis(1000)).build();
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = StreamMessageListenerContainer.create(redisConnectionFactory,
                containerOptions);
        Subscription subscription = container.receiveAutoAck(Consumer.from("newImageGroup", "consumer1"), StreamOffset.create("newImage", ReadOffset.lastConsumed()), imageStreamListener);
        container.start();
    }

}
