import com.moriable.recordmockproxy.model.AbstractModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonSchemaResolver;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.ProblemHandler;

import javax.json.JsonReader;
import javax.json.JsonStructure;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Test {

    static Resolver resolver;
    static class Resolver implements JsonSchemaResolver {

        private JsonValidationService service;

        public Resolver(JsonValidationService service) {
            this.service = service;
        }

        @Override
        public JsonSchema resolveSchema(URI id) {
            System.out.println(id);
            return service.createSchemaReaderFactoryBuilder()
                    .withSchemaResolver(resolver)
                    .build()
                    .createSchemaReader(Test.class.getResourceAsStream(id.getPath()))
                    .read();
        }
    }

    public static void validateJson(String schemaPath, InputStream input) {
        JsonValidationService service = JsonValidationService.newInstance();
        resolver = new Resolver(service);

        JsonSchema schema = service.createSchemaReaderFactoryBuilder()
                .withSchemaResolver(resolver)
                .build()
                .createSchemaReader(Test.class.getResourceAsStream(schemaPath))
                .read();

        List<String> errors = new ArrayList<>();
        ProblemHandler handler = service.createProblemPrinter(s -> {
            errors.add(s);
        });

        try (JsonReader r = service.createReader(input, schema, handler)) {
            JsonStructure value = r.read();
            System.out.println(errors.size());
            System.out.println("xxx:" + value);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(2%2);

        JsonValidationService service = JsonValidationService.newInstance();
        resolver = new Resolver(service);

        JsonSchema schema = service.createSchemaReaderFactoryBuilder()
                .withSchemaResolver(resolver)
                .build()
                .createSchemaReader(Test.class.getResourceAsStream("mockForm.schema.json"))
                .read();

        //        JsonSchema schema = service.readSchema(Test.class.getResourceAsStream("mockForm.schema.json"));
        List<String> errors = new ArrayList<>();
        ProblemHandler handler = service.createProblemPrinter(s -> {
            errors.add(s);
        });
        Path path = Paths.get("mock/mock.json");
        try (JsonReader r = service.createReader(path, schema, handler)) {
            JsonStructure value = r.read();
            System.out.println(errors.size());
            System.out.println("xxx:" + value);
        }

//        ModelStorage<String, TestModel> storage = new ModelStorage<>(new File("/Users/hiroki.nakamori/tmp/test.json"));
//
//        TestModel model1 = new TestModel();
//        model1.setName("tanaka");
//        model1.setAge(29);
//        model1.setChild(new TestModel.Child("xxx"));
//        storage.put("1", model1);
//
//        TestModel model2 = new TestModel();
//        model2.setName("suzuki");
//        model2.setAge(38);
//        model2.setChild(new TestModel.Child("yyy"));
//        storage.put("2", model2);
//
//        storage.save();
//
//        model2.setAge(40);
//        model2.getChild().setScool("yyy2");
//        storage.save();
//
//        model2.commit();
//        storage.save();
//
//        Type t = new TypeToken<Map<String,TestModel>>(){}.getType();
//        Map<String,TestModel> m = new Gson().fromJson("{\"1\":{\"name\":\"tanaka\",\"age\":29,\"child\":{\"scool\":\"xxx\"}},\"2\":{\"name\":\"suzuki\",\"age\":40,\"child\":{\"scool\":\"yyy2\"}}}"
//                ,t
//        );
//        System.out.println(m.get("1").getClass().getName());
//        System.out.println(m.getClass().getName());
//
//        m = Collections.synchronizedMap(m);
//        System.out.println(m.getClass().getName());
//
//        List<String> list = Collections.synchronizedList(new ArrayList<>());
//        list.add("A");
//        list.add("B");
//        list.add("C");
//        list.add("D");
//        list.add("E");
//
//        new Thread(() -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            list.add("F");
//
//            list.remove(2);
//        }).start();
//
//        list.stream().forEach(s -> {
//            System.out.println(s);
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        });
    }

    @Data
    @EqualsAndHashCode(callSuper=true)
    public static class TestModel extends AbstractModel {
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
