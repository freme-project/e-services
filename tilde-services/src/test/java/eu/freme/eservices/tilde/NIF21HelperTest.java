package eu.freme.eservices.tilde;

import static eu.freme.common.conversion.rdf.RDFConstants.TURTLE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;

import eu.freme.bservices.testhelper.TestHelper;
import eu.freme.bservices.testhelper.ValidationHelper;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import eu.freme.eservices.tilde.translation.TildeETranslation;

/**
 * 
 * @author britta (britta.grusdt@dfki.de)
 *
 */
public class NIF21HelperTest {

	String sourceLang = "en";
	String targetLang = "de";
	TestHelper testHelper;
	ValidationHelper validationHelper;
	TildeETranslation tildeTranslation;
	
	@Before
	public void setup() {
		ApplicationContext context = IntegrationTestSetup
				.getContext("NIF21Helper-test-package.xml");
		testHelper = context.getBean(TestHelper.class);
		validationHelper = context.getBean(ValidationHelper.class);
		tildeTranslation = context.getBean(TildeETranslation.class);
	}
	
	private HttpRequestWithBody baseRequest() {
		String url = testHelper.getAPIBaseUrl() + "/e-translation/tilde";
		return Unirest.post(url).queryString("source-lang", sourceLang)
				.queryString("target-lang", targetLang);
	}
	
	@Test
	public void testAddConformsTo() throws UnirestException, IOException{
//		<http://freme-project.eu/#collection>
//	        a               nif:ContextCollection ;
//	        nif:hasContext  <http://freme-project.eu/#offset_0_13> ;
//	        <http://purl.org/dc/terms/conformsTo>
		String endpoint = "http://localhost:8080/mockups/file/TildeETranslation_helloWorld_nif21.ttl";
		
		HttpResponse<String> response = baseRequest()
				.queryString("informat", "text")
				.queryString("input", "Hello, world!")
				.queryString("outformat", "turtle")
				.queryString("nif-version", "2.1").asString();
		
		
		validationHelper.validateNIFResponse(response, TURTLE);
		
		assertTrue(response.getStatus() == 200);
		assertTrue(response.getBody().contains("http://purl.org/dc/terms/conformsTo"));
		
		endpoint = "http://localhost:8080/mockups/file/TildeETranslation_helloWorld_nif20.ttl";
		tildeTranslation.setEndpoint(endpoint);
		
		response = baseRequest()
				.queryString("informat", "text")
				.queryString("input", "Hello, world!")
				.queryString("outformat", "turtle").asString();

	
		assertTrue(response.getStatus() == 200);
		assertFalse(response.getBody().contains("http://purl.org/dc/terms/conformsTo"));
	}
}