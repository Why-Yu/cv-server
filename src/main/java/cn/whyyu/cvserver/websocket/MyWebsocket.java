package cn.whyyu.cvserver.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@ServerEndpoint("/websocket/{username}")
@Component
@Slf4j
public class MyWebsocket {
    private Session session;
    private String username;
    private static final Map<String, MyWebsocket> clients = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(@PathParam("username") String username, Session session) throws IOException {
        this.username = username;
        this.session = session;
        clients.put(username, this);
        log.info("有新的连接，总数{},session{}", clients.size(), session);
    }

    @OnClose
    public void onClose() throws IOException {
        clients.remove(username);
        log.info("连接断开，总数{}", clients.size());
    }

    @OnMessage
    public void onMessage(String message) {}

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    public void sendMessageTo(String message, String To) {
        // session.getBasicRemote().sendText(message);
        //session.getAsyncRemote().sendText(message);
        for (MyWebsocket webSocket : clients.values()) {
            if (webSocket.username.equals(To)) {
                webSocket.session.getAsyncRemote().sendText(message);
                log.info("目标：{},发送消息：{}", To, message);
            }
        }
    }

    public void sendMessageAll(String message) {
        for (MyWebsocket webSocket : clients.values()) {
            webSocket.session.getAsyncRemote().sendText(message);
        }
    }
}
