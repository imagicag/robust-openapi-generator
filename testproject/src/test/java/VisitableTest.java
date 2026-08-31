import org.junit.Assert;
import org.junit.Test;
import undertest.api.model.Error;
import undertest.api.model.FakeItem;
import undertest.common.api.Visitable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VisitableTest {

    @Test
    public void exposesPropertiesUsingOpenApiNames() {
        Visitable value = new Error();

        List<String> properties = new ArrayList<>();
        value.properties().forEach(properties::add);

        Assert.assertEquals(Arrays.asList("code", "class", "int", "float", "double", "message"), properties);
        Assert.assertFalse(properties.contains("class$"));

        Iterator<String> iterator = value.properties().iterator();
        iterator.next();
        Assert.assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    @Test
    public void getsSetsAndDescribesProperties() {
        Visitable value = new Error();
        FakeItem item = new FakeItem().withFakeId(42L).withFakeName("item");
        Set<FakeItem> items = new LinkedHashSet<>(List.of(item));
        List<FakeItem> itemList = new ArrayList<>(List.of(item));
        Map<String, FakeItem> itemMap = new LinkedHashMap<>();
        itemMap.put("item", item);

        value.set("code", 7);
        value.set("class", "failure");
        value.set("int", items);
        value.set("float", itemList);
        value.set("double", itemMap);

        Assert.assertEquals(7, value.get("code"));
        Assert.assertEquals("failure", value.get("class"));
        Assert.assertSame(items, value.get("int"));
        Assert.assertSame(itemList, value.get("float"));
        Assert.assertSame(itemMap, value.get("double"));
        Assert.assertEquals(Integer.class, value.type("code"));
        Assert.assertEquals(String.class, value.type("class"));
        Assert.assertEquals("java.util.Set<undertest.api.model.FakeItem>", value.type("int").getTypeName());
        Assert.assertEquals("java.util.List<undertest.api.model.FakeItem>", value.type("float").getTypeName());
        Assert.assertEquals("java.util.Map<java.lang.String, undertest.api.model.FakeItem>", value.type("double").getTypeName());

        value.set("class", null);
        Assert.assertNull(value.get("class"));
    }

    @Test
    public void ignoresUnknownPropertiesAndRejectsWrongTypes() {
        Visitable value = new Error().withCode(7);

        value.set("unknown", "ignored");
        value.set(null, "ignored");

        Assert.assertEquals(7, value.get("code"));
        Assert.assertNull(value.get("unknown"));
        Assert.assertNull(value.get(null));
        Assert.assertNull(value.type("unknown"));
        Assert.assertNull(value.type(null));
        Assert.assertThrows(ClassCastException.class, () -> value.set("code", "not an integer"));
        Assert.assertThrows(ClassCastException.class, () -> value.set("int", new ArrayList<>()));
    }
}
