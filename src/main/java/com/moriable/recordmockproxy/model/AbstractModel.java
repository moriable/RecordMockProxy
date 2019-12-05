package com.moriable.recordmockproxy.model;

import com.moriable.recordmockproxy.common.Exclude;

import java.io.Serializable;
import java.util.Map;

@Exclude
public abstract class AbstractModel implements Serializable {

    @Exclude
    private Map storage;
    @Exclude
    private Object key;

    protected final void setStorage(Map storage, Object key) {
        this.storage = storage;
        this.key = key;
    }

    protected final boolean registeredStorage() {
        return storage != null;
    }

    public final void commit() {
        if (storage == null) {
            return;
        }

        synchronized (storage) {
            storage.put(key, this);
        }
    }
}
