/**
 * Copyright © 2016 Agro-Know, Deutsches Forschungszentrum für Künstliche Intelligenz, iMinds,
 * Institut für Angewandte Informatik e. V. an der Universität Leipzig,
 * Istituto Superiore Mario Boella, Tilde, Vistatec, WRIPL (http://freme-project.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.freme.eservices.dbpediaspotlight;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.freme.bservices.testhelper.*;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import eu.freme.common.conversion.rdf.RDFConstants;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static eu.freme.common.conversion.rdf.RDFConstants.TURTLE;
import static org.junit.Assert.assertEquals;
/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 27.01.2016.
 */
public class DBpediaSpotlightTest {
    private Logger logger = Logger.getLogger(DBpediaSpotlightTest.class);
    private TestHelper th;
    private ValidationHelper vh;

    final static String serviceUrl = "/e-entity/dbpedia-spotlight/";

    public DBpediaSpotlightTest() throws UnirestException, IOException {
        ApplicationContext context = IntegrationTestSetup.getContext("dbpedia-spotlight-test-package.xml");
        th = context.getBean(TestHelper.class);
        vh = context.getBean(ValidationHelper.class);
    }

    @Value("${freme.eentity.dbpediaSpotlightEndpointUrl:http://www.dbpedia-spotlight.com/en/annotate}")
    String dbpediaSpotlightUrl;

    @Test
    public void TestDBpediaSpotlightNER() throws UnirestException, IOException, UnsupportedEncodingException {
        logger.info("Starting DBpediaSpotlightTest against endpoint: " + dbpediaSpotlightUrl);
        HttpResponse<String> response;
        String[] availableLanguages = {"en"};
        //String[] availableLanguages = {"en","de","it","nl","fr","es"};
        String testinput= "This is Germany";

        String testinputEncoded= URLEncoder.encode(testinput, "UTF-8");
        String data = readFile("rdftest/e-entity/data.ttl");
        //Tests every language
        for (String lang : availableLanguages) {

            //Tests POST
            //Plaintext Input in Query String
            logger.info("Testing Language: "+lang);
            logger.info("Testing Plaintext Input in Query String");
            response = Unirest.post(th.getAPIBaseUrl() + serviceUrl+  "documents")
                    .queryString("input", testinput)
                    .queryString("language", lang)
                    .queryString("informat", "text")
                    .asString();
            vh.validateNIFResponse(response, TURTLE);

            //Tests POST
            //Plaintext Input in Body
            logger.info("Testing Plaintext Input in Body");
            response = Unirest.post(th.getAPIBaseUrl() + serviceUrl+  "documents")
                    .queryString("language", lang)
                    .header("Content-Type", "text/plain")
                    .body(testinput)
                    .asString();
            vh.validateNIFResponse(response, TURTLE);
            //Tests POST
            //NIF Input in Body (Turtle)
            logger.info("Testing Turtle Input in Body");
            response = Unirest.post(th.getAPIBaseUrl() + serviceUrl+  "documents").header("Content-Type", "text/turtle")
                    .queryString("language", lang)
                    .body(data).asString();
            vh.validateNIFResponse(response, TURTLE);


            //Tests POST
            //Test Prefix
            logger.info("Testing prefix & confidence parameters");
            response = Unirest.post(th.getAPIBaseUrl() + serviceUrl+  "documents")
                    .queryString("input", testinput)
                    .queryString("language", lang)
                    .queryString("informat", "text")
                    .queryString("confidence","0.2")
                    .queryString("prefix", "http://test-prefix.com/")
                    .asString();
            vh.validateNIFResponse(response, TURTLE);

            //assertTrue(response.getString() contains prefix)

            //Tests GET
            //response = Unirest.get(url+"documents?informat=text&input="+testinputEncoded+"&language="+lang+"&dataset="+dataset).asString();
            response = Unirest.get(th.getAPIBaseUrl() + serviceUrl+  "documents")
                    .queryString("informat", "text")
                    .queryString("input", testinputEncoded)
                    .queryString("language", lang)
                    .asString();
            vh.validateNIFResponse(response, TURTLE);
        }
    }

    private String readFile(String filename) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(filename).getFile());
        //File file = new File("src/main/resources/mockup-endpoint-mockup-endpoint-data/"+filename);
        return FileUtils.readFileToString(file);
    }
}
