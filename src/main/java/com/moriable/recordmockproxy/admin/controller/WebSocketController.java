package com.moriable.recordmockproxy.admin.controller;

import com.google.gson.Gson;
import com.moriable.recordmockproxy.model.RecordModel;
import com.moriable.recordmockproxy.model.RecordStorage;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@WebSocket
public class WebSocketController implements RecordStorage.RequestResponseListener {

    private final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

    private Gson gson = new Gson();

    private ExecutorService execService = Executors.newCachedThreadPool();

    @OnWebSocketConnect
    public void connected(Session session) {
        sessions.add(session);
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        sessions.remove(session);
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
    }

    public void bloadcast(String message) {
        sessions.forEach(session -> {
            try {
                session.getRemote().sendString(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onRequest(RecordModel recordModel) {
        execService.submit(() -> {
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("type", "REQUEST");
            messageMap.put("data", recordModel);

            bloadcast(gson.toJson(messageMap));
        });
    }

    @Override
    public void onResponse(RecordModel recordModel) {
        execService.submit(() -> {
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("type", "RESPONSE");
            messageMap.put("data", recordModel);

            bloadcast(new Gson().toJson(messageMap));
        });
    }
}
