import org.junit.Assert;
import org.junit.Test;
import undertest.api.model.Error;
import undertest.api.model.FakeItem;
import undertest.api.model.NewPet;
import undertest.api.model.StringSetMapContainer;
import undertest.api.request.AddPetJsonRequest;
import undertest.api.request.DeletePetRequest;
import undertest.common.api.PropertyVisitor.PropertyVisitorAction;
import undertest.common.api.RequestParameters;
import undertest.common.api.Visitable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class VisitableTest {

    @Test
    public void visitsRequestBodyAndItsProperties() {
        NewPet body = new NewPet().withName("Fido").withTag("dog");
        RequestParameters request = new AddPetJsonRequest().withRequestBody(body);
        Assert.assertEquals(new TreeSet<>(List.of("requestBody", "requestTimeout", "responseBodyReadTimeout",
                "responseBodyTotalTimeout", "additionalHeaderParameter", "additionalQueryParameter")), request.properties());
        Assert.assertSame(body, request.get("requestBody"));
        Assert.assertEquals(NewPet.class, request.type("requestBody"));

        Map<String, Object> visitedRequest = new LinkedHashMap<>();
        Map<String, Object> visitedBody = new LinkedHashMap<>();
        request.visit((parent, name, value, hasChildren) -> {
            if (parent == request) {
                visitedRequest.put(name, value);
                Assert.assertEquals("requestBody".equals(name), hasChildren);
            } else {
                Assert.assertSame(body, parent);
                Assert.assertFalse(hasChildren);
                visitedBody.put(name, value);
            }
            return PropertyVisitorAction.CONTINUE;
        });

        Assert.assertEquals(request.properties(), visitedRequest.keySet());
        Assert.assertSame(body, visitedRequest.get("requestBody"));
        Assert.assertEquals(Map.of("name", "Fido", "tag", "dog"), visitedBody);

        request.set("requestBody", null);
        Assert.assertNull(request.getRequestBody());
        request.set("requestBody", body);
        Assert.assertSame(body, request.getRequestBody());
    }

    @Test
    public void visitsBodylessRequestParametersAndAdditionalLists() {
        List<String> queryValues = List.of("first", "second");
        List<String> headerValues = List.of("header-value");
        Map<String, List<String>> query = Map.of("extra", queryValues);
        Map<String, List<String>> headers = Map.of("X-Extra", headerValues);
        RequestParameters request = new DeletePetRequest().withId(42L)
                .withAdditionalQueryParameter(query).withAdditionalHeaderParameter(headers);
        Assert.assertEquals(new TreeSet<>(List.of("id", "requestTimeout", "responseBodyReadTimeout",
                "responseBodyTotalTimeout", "additionalHeaderParameter", "additionalQueryParameter")), request.properties());
        Assert.assertNull(request.type("requestBody"));
        Assert.assertEquals(Long.class, request.type("id"));
        Assert.assertEquals("java.util.Map<java.lang.String, java.util.List<java.lang.String>>",
                request.type("additionalQueryParameter").getTypeName());

        request.set("id", 43L);
        request.set("requestTimeout", Duration.ofSeconds(5));
        Assert.assertEquals(43L, request.get("id"));
        Assert.assertEquals(Duration.ofSeconds(5), request.getRequestTimeout());

        Map<String, Object> visitedRequest = new LinkedHashMap<>();
        Map<String, Object> visitedQuery = new LinkedHashMap<>();
        Map<String, Object> visitedHeaders = new LinkedHashMap<>();
        List<Object> visitedQueryValues = new ArrayList<>();
        List<Object> visitedHeaderValues = new ArrayList<>();
        request.visit((parent, name, value, hasChildren) -> {
            if (parent == request) {
                visitedRequest.put(name, value);
                Assert.assertEquals(name.startsWith("additional"), hasChildren);
            } else if (parent == query) {
                Assert.assertTrue(hasChildren);
                visitedQuery.put(name, value);
            } else if (parent == headers) {
                Assert.assertTrue(hasChildren);
                visitedHeaders.put(name, value);
            } else {
                Assert.assertFalse(hasChildren);
                if (parent == queryValues) {
                    Assert.assertEquals(String.valueOf(visitedQueryValues.size()), name);
                    visitedQueryValues.add(value);
                } else {
                    Assert.assertSame(headerValues, parent);
                    Assert.assertEquals(String.valueOf(visitedHeaderValues.size()), name);
                    visitedHeaderValues.add(value);
                }
            }
            return PropertyVisitorAction.CONTINUE;
        });

        Assert.assertEquals(request.properties(), visitedRequest.keySet());
        Assert.assertEquals(43L, visitedRequest.get("id"));
        Assert.assertSame(query, visitedRequest.get("additionalQueryParameter"));
        Assert.assertSame(headers, visitedRequest.get("additionalHeaderParameter"));
        Assert.assertEquals(query, visitedQuery);
        Assert.assertEquals(headers, visitedHeaders);
        Assert.assertEquals(queryValues, visitedQueryValues);
        Assert.assertEquals(headerValues, visitedHeaderValues);
    }

    @Test
    public void exposesPropertiesUsingOpenApiNames() {
        Visitable value = new Error();
        Assert.assertEquals(new TreeSet<>(Arrays.asList("code", "class", "int", "float", "double", "message")), value.properties());

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

    @Test
    public void returnsRawTypeClassForProperties() {
        Visitable value = new Error();

        Assert.assertEquals(Integer.class, value.typeClass("code"));
        Assert.assertEquals(String.class, value.typeClass("class"));
        Assert.assertEquals(Set.class, value.typeClass("int"));
        Assert.assertEquals(List.class, value.typeClass("float"));
        Assert.assertEquals(Map.class, value.typeClass("double"));
        Assert.assertEquals(Map.class, new StringSetMapContainer().typeClass("values"));
        Assert.assertNull(value.typeClass("unknown"));
        Assert.assertNull(value.typeClass(null));
    }
}
