package com.moriable.recordmockproxy.admin.validator;

import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonSchemaResolver;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.ProblemHandler;

import javax.json.JsonReader;
import javax.json.JsonStructure;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class JsonValidator {

    private Resolver resolver;

    private JsonValidationService service = JsonValidationService.newInstance();

    private class Resolver implements JsonSchemaResolver {

        @Override
        public JsonSchema resolveSchema(URI id) {
            System.out.println(id.getPath());
            return service.createSchemaReaderFactoryBuilder()
                    .withSchemaResolver(resolver)
                    .build()
                    .createSchemaReader(JsonValidator.class.getResourceAsStream("/" + id.getPath()))
                    .read();
        }
    }

    public String validate(String schemaResourcePath, String input) throws JsonValidatorException {
        resolver = new Resolver();
        JsonSchema schema = service.createSchemaReaderFactoryBuilder()
                .withSchemaResolver(resolver)
                .build()
                .createSchemaReader(this.getClass().getResourceAsStream(schemaResourcePath))
                .read();
        List<String> errors = new ArrayList<>();
        ProblemHandler handler = service.createProblemPrinter(s -> {
            errors.add(s);
        });

        try (JsonReader r = service.createReader(new StringReader(input), schema, handler)) {
            JsonStructure value = r.read();
            if (errors.size() != 0) {
                throw new JsonValidatorException(errors);
            }

            return value.toString();
        }
    }

    public class JsonValidatorException extends Exception {

        private List<String> errors;

        public JsonValidatorException(List<String> errors) {
            this.errors = errors;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
