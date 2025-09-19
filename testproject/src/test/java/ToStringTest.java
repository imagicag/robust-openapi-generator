import org.junit.Assert;
import org.junit.Test;
import undertest.api.request.AddPetJsonRequest;

import java.time.Duration;

public class ToStringTest {

    @Test
    public void testToString() {
        AddPetJsonRequest apjr = new AddPetJsonRequest();
        apjr.withRequestBody(new undertest.api.model.NewPet().withName("testName").withTag("testTag"));
        apjr.withRequestTimeout(Duration.ofMillis(5000));

        String str = apjr.toString();
        String expect = "" +
                "AddPetJsonRequest{\n" +
                "  requestBody=NewPet{\n" +
                "    name=testName\n" +
                "    tag=testTag\n" +
                "  }\n" +
                "  requestTimeout=PT5S\n" +
                "  responseBodyReadTimeout=null\n" +
                "  responseBodyTotalTimeout=null\n" +
                "  additionalHeaderParameter=null\n" +
                "  additionalQueryParameter=null\n" +
                "}";

        Assert.assertEquals(expect, str);
    }
}
