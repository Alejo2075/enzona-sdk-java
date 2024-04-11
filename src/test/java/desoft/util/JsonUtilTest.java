package desoft.util;

import com.desoft.enzonasdk.util.JsonUtil;
import org.junit.Test;
import static org.junit.Assert.*;

public class JsonUtilTest {

    @Test
    public void testFromJsonValid() {
        String json = "{\"name\":\"John\", \"age\":30}";
        Person person = JsonUtil.fromJson(json, Person.class);
        assertNotNull("The result should not be null", person);
        assertEquals("The name should be John", "John", person.getName());
        assertEquals("The age should be 30", 30, person.getAge());
    }

    @Test(expected = RuntimeException.class)
    public void testFromJsonInvalid() {
        String json = "{name:\"John\", age:30}"; // Missing quotes around field names
        JsonUtil.fromJson(json, Person.class);
    }

    @Test
    public void testToJsonValid() {
        Person person = new Person("Jane", 25);
        String json = JsonUtil.toJson(person);
        assertNotNull("The result should not be null", json);
        assertTrue("The JSON should contain the name Jane", json.contains("\"name\":\"Jane\""));
        assertTrue("The JSON should contain the age 25", json.contains("\"age\":25"));
    }

    @Test(expected = RuntimeException.class)
    public void testToJsonInvalid() {
        Person person = new Person(null, 25); // Assuming the serializer is set to fail on null fields
        JsonUtil.toJson(person);
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

