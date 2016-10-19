/**
 * Copyright (C) 2015 Agro-Know, Deutsches Forschungszentrum für Künstliche Intelligenz, iMinds,
 * Institut für Angewandte Informatik e. V. an der Universität Leipzig,
 * Istituto Superiore Mario Boella, Tilde, Vistatec, WRIPL (http://freme-project.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.freme.eservices.tilde;

import static eu.freme.common.conversion.rdf.RDFConstants.*;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import org.apache.commons.io.FileUtils;
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
import eu.freme.common.conversion.rdf.RDFConstants;

/**
 * @author Jan Nehring - jan.nehring@dfki.de
 */

public class TildeETranslationTest {

	String sourceLang = "en";
	String targetLang = "de";
	String system = "smt-c71f6f22-5e2a-4682-a4cc-2d97d38fac5e";
	TestHelper testHelper;
	ValidationHelper validationHelper;

	@Before
	public void setup() {
		ApplicationContext context = IntegrationTestSetup
				.getContext(TestConstants.pathToPackage);
		testHelper = context.getBean(TestHelper.class);
		validationHelper = context.getBean(ValidationHelper.class);
	}

	private HttpRequestWithBody baseRequest(String param) {
		String url = testHelper.getAPIBaseUrl() + "/e-translation/tilde";
		if(param.equals("system")){
			return Unirest.post(url).queryString("system",system);
		}else{
			return Unirest.post(url).queryString("source-lang", sourceLang)
					.queryString("target-lang", targetLang);
		}
	}


	@Test
	public void testEtranslateWithLangParam() throws UnirestException, IOException,
	Exception {
		doTest("");
	}

	@Test
	public void testEtranslateWithSystemParam() throws UnirestException, IOException,
	Exception {
		doTest("system");
	}



	private void doTest(String method) throws UnirestException, IOException,
	Exception {

		HttpResponse<String> response = baseRequest(method)
				.queryString("informat", "text")
				.queryString("input", "Show me the source of the light.")
				.queryString("outformat", "rdf-xml").asString();

		validationHelper.validateNIFResponse(response, RDF_XML);

		String data = FileUtils.readFileToString(new File("src/test/resources/rdftest/e-translate/showmethesourceofthelight.ttl"));
		response = baseRequest(method).header("Content-Type", "text/turtle")
				.body(data).asString();
		validationHelper.validateNIFResponse(response, TURTLE);

		assertTrue(response.getStatus() == 200);
		assertTrue(response.getBody().length() > 0);

		data = FileUtils.readFileToString(new File("src/test/resources/rdftest/e-translate/showmethesourceofthelight.json"));
		response = baseRequest(method).header("Content-Type", "application/json+ld")
				.queryString("outformat", "json-ld").body(data).asString();
		assertTrue(response.getStatus() == 200);
		assertTrue(response.getBody().length() > 0);
		validationHelper.validateNIFResponse(response, JSON_LD);

		data = FileUtils.readFileToString(new File("src/test/resources/rdftest/e-translate/showmethesourceofthelight.txt"));
		response = baseRequest(method)
				.queryString("input", URLEncoder.encode(data, "UTF-8"))
				.queryString("informat", "text").queryString("outformat", "n3")
				.asString();
		validationHelper.validateNIFResponse(response, N3);

		response = baseRequest(method)
				.queryString("input", URLEncoder.encode(data, "UTF-8"))
				.queryString("informat", "text")
				.queryString("outformat", "n-triples").asString();
		validationHelper.validateNIFResponse(response, N_TRIPLES);
	}
}
