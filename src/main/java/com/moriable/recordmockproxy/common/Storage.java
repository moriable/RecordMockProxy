package com.moriable.recordmockproxy.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.SerializationUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Storage<K,V extends Model> {

    private File storageFile;
    private Map<K,V> storage = null;
    private Gson gson = new GsonBuilder().addSerializationExclusionStrategy( new ExcludeWithAnotateStrategy() ).create();

    public Storage(File storageFile) {
        this.storageFile = storageFile;
        if (storageFile.exists()) {
            if (!storageFile.isFile()) {
                throw new IllegalArgumentException("storageFile is not file.");
            }

            Type t = new TypeToken<Map<K,V>>(){}.getType();
            Map<K,V> map = null;
            try {
                map = gson.fromJson(new FileReader(storageFile), t);
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
            if (map != null) {
                storage = Collections.synchronizedMap(map);
            }
        }

        if (storage == null) {
            storage = Collections.synchronizedMap(new LinkedHashMap<>());
        }
    }

    public V get(K key) {
        V value = (V) SerializationUtils.clone(storage.get(key));
        value.setStorage(storage, key);
        return value;
    }

    public void put(K key, V value) {
        if (value.hasStorage()) {
            throw new IllegalStateException("This value is already put. Use commit method to reflect changes.");
        }

        value.setStorage(null, null);
        V clone = (V) SerializationUtils.clone(value);
        storage.put(key, clone);

        value.setStorage(storage, key);
    }

    public synchronized void save() {
        try {
            if (!storageFile.exists()) {
                storageFile.createNewFile();
            }
            try (Writer writer = new FileWriter(storageFile)) {
                gson.toJson(storage, writer);
            }
        } catch (IOException e) {
            new IllegalStateException(e);
        }
    }
}
