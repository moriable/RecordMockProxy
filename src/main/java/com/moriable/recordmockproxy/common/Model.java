package com.moriable.recordmockproxy.common;

import java.io.Serializable;
import java.util.Map;

@Exclude
public abstract class Model implements Serializable {

    @Exclude
    private Map storage;

    @Exclude
    private Object key;

    public final void setStorage(Map storage, Object key) {
        this.storage = storage;
        this.key = key;
    }

    public final boolean hasStorage() {
        return storage != null;
    }

    public final void commit() {
        synchronized (storage) {
            storage.put(key, this);
        }
    }
}
