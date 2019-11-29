import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.moriable.recordmockproxy.admin.model.MockModel;
import com.moriable.recordmockproxy.common.Exclude;
import com.moriable.recordmockproxy.common.Model;
import com.moriable.recordmockproxy.common.Storage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.codec.net.URLCodec;

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.*;

public class Test {
    public static void main(String[] args) throws CloneNotSupportedException {
        Storage<String, TestModel> storage = new Storage<>(new File("/Users/hiroki.nakamori/tmp/test.json"));

        TestModel model1 = new TestModel();
        model1.setName("tanaka");
        model1.setAge(29);
        model1.setChild(new TestModel.Child("xxx"));
        storage.put("1", model1);

        TestModel model2 = new TestModel();
        model2.setName("suzuki");
        model2.setAge(38);
        model2.setChild(new TestModel.Child("yyy"));
        storage.put("2", model2);

        storage.save();

        model2.setAge(40);
        model2.getChild().setScool("yyy2");
        storage.save();

        model2.commit();
        storage.save();

        Type t = new TypeToken<Map<String,TestModel>>(){}.getType();
        Map<String,TestModel> m = new Gson().fromJson("{\"1\":{\"name\":\"tanaka\",\"age\":29,\"child\":{\"scool\":\"xxx\"}},\"2\":{\"name\":\"suzuki\",\"age\":40,\"child\":{\"scool\":\"yyy2\"}}}"
                ,t
        );
        System.out.println(m.get("1").getClass().getName());
        System.out.println(m.getClass().getName());

        m = Collections.synchronizedMap(m);
        System.out.println(m.getClass().getName());

        List<String> list = Collections.synchronizedList(new ArrayList<>());
        list.add("A");
        list.add("B");
        list.add("C");
        list.add("D");
        list.add("E");

        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            list.add("F");

            list.remove(2);
        }).start();

        list.stream().forEach(s -> {
            System.out.println(s);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @Data
    @EqualsAndHashCode(callSuper=true)
    public static class TestModel extends Model {
        private String name;
        private int age;
        private Child child;

        @Data
        @AllArgsConstructor
        public static class Child implements Serializable {
            private String scool;
        }
    }
}
