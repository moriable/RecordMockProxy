package com.moriable.recordmockproxy.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.moriable.recordmockproxy.common.ExcludeWithAnotateStrategy;
import org.apache.commons.lang.SerializationUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MockStorage {

    private File storageFile;
    private Map<String, MockModel> storage = null;
    private Map<String, Integer> mockCount = Collections.synchronizedMap(new HashMap<>());
    private Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new ExcludeWithAnotateStrategy()).create();

    public MockStorage(File storageFile) {
        if (storageFile == null) {
            throw new IllegalArgumentException();
        }

        this.storageFile = storageFile;
        if (storageFile.exists()) {
            if (!storageFile.isFile()) {
                throw new IllegalArgumentException("storageFile is not file.");
            }

            Type t = new TypeToken<Map<String, MockModel>>(){}.getType();
            Map<String ,MockModel> map = null;
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

    public MockModel get(String key) {
        if (!storage.containsKey(key)) {
            return null;
        }

        MockModel value = (MockModel) SerializationUtils.clone(storage.get(key));
        value.setStorage(storage, key);
        return value;
    }

    public void put(String key, MockModel value) {
        if (value.registeredStorage()) {
            throw new IllegalStateException("This value is already put. Use commit method to reflect changes.");
        }

        value.setStorage(null, null);
        MockModel clone = (MockModel) SerializationUtils.clone(value);
        storage.put(key, clone);

        value.setStorage(storage, key);
    }

    public int call(String key) {
        synchronized (mockCount) {
            int count = 0;
            if (mockCount.containsKey(key)) {
                count = mockCount.get(key);
            }
            mockCount.put(key, count + 1);

            return count;
        }
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

    public synchronized String dump() {
        return gson.toJson(storage);
    }
}
