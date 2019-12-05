package com.moriable.recordmockproxy.model;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class RecordStorage {

    private Map<String, RecordModel> recordMap;
    private Set<RequestResponseListener> listenerSet = new CopyOnWriteArraySet<>();

    public RecordStorage(Map<String, RecordModel> recordMap) {
        this.recordMap = recordMap;
    }

    public RecordModel get(String key) {
        return recordMap.get(key);
    }

    public RecordModel put(String key, RecordModel recordModel) {
        return recordMap.put(key, recordModel);
    }

    public Collection<RecordModel> values() {
        return recordMap.values();
    }

    public void clear() {
        recordMap.clear();
    }

    public void addListener(RequestResponseListener listener) {
        listenerSet.add(listener);
    }

    public void notifyRequest(RecordModel.RequestModel requestModel) {
        listenerSet.forEach(listener -> {
            listener.onRequest(requestModel);
        });
    }

    public void notifyResponse(RecordModel.ResponseModel responseModel) {
        listenerSet.forEach(listener -> {
            listener.onResponse(responseModel);
        });
    }

    public static interface RequestResponseListener {
        void onRequest(RecordModel.RequestModel requestModel);
        void onResponse(RecordModel.ResponseModel responseModel);
    }
}
