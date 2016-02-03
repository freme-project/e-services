package eu.freme.eservices.example;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import org.springframework.context.ApplicationContext;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import eu.freme.bservices.testhelper.TestHelper;
import eu.freme.common.starter.FREMEStarter;

public class ExampleEServiceTest {

	@Test
	public void test() throws UnirestException {
		ApplicationContext context = FREMEStarter
				.startPackageFromClasspath("example-test-package.xml");
		TestHelper testHelper = context.getBean(TestHelper.class);

		String response = Unirest
				.post(testHelper.getAPIBaseUrl() + "/e-xample")
				.queryString("input", "hello world")
				.queryString("informat", "text").asString().getBody();

		assertTrue(response.contains("HELLO WORLD"));
	}
}
