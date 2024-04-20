package io.github.alejo2075.enzonasdk.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonUtilTest {

    @Test
    @DisplayName("Deserialize valid JSON into a Person object")
    void testFromJsonValid() {
        String json = "{\"name\":\"John\", \"age\":30}";
        Person person = JsonUtil.fromJson(json, Person.class);
        assertAll("person",
                () -> assertNotNull(person, "The result should not be null"),
                () -> assertEquals("John", person.getName(), "The name should be John"),
                () -> assertEquals(30, person.getAge(), "The age should be 30")
        );
    }

    @Test
    @DisplayName("Deserialize invalid JSON should throw RuntimeException")
    void testFromJsonInvalid() {
        String json = "{name:\"John\", age:30}"; // Missing quotes around field names
        assertThrows(RuntimeException.class, () -> JsonUtil.fromJson(json, Person.class),
                "Should throw RuntimeException due to invalid JSON format");
    }

    @Test
    @DisplayName("Serialize valid Person object into JSON")
    void testToJsonValid() {
        Person person = new Person("Jane", 25);
        String json = JsonUtil.toJson(person);
        assertAll("json content",
                () -> assertNotNull(json, "The result should not be null"),
                () -> assertTrue(json.contains("\"name\":\"Jane\""), "The JSON should contain the name Jane"),
                () -> assertTrue(json.contains("\"age\":25"), "The JSON should contain the age 25")
        );
    }

    @Test
    @DisplayName("Serialize Person object with null name should throw RuntimeException")
    void testToJsonInvalid() {
        Person person = new Person(null, 25); // Assuming the serializer is set to fail on null fields
        assertThrows(RuntimeException.class, () -> JsonUtil.toJson(person),
                "Should throw RuntimeException because name is null");
    }

    // Helper class for the tests
    static class Person {
        private String name;
        private int age;

        public Person() {
        }

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
