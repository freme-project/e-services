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
package eu.freme.eservices.elink;

import static eu.freme.common.conversion.rdf.RDFConstants.TURTLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import eu.freme.bservices.testhelper.AuthenticatedTestHelper;
import eu.freme.bservices.testhelper.LoggingHelper;
import eu.freme.bservices.testhelper.OwnedResourceManagingHelper;
import eu.freme.bservices.testhelper.SimpleEntityRequest;
import eu.freme.bservices.testhelper.ValidationHelper;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import eu.freme.common.persistence.model.OwnedResource;
import eu.freme.common.persistence.model.Template;
/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 27.01.2016.
 */
public class LinkingControllerTest {
    private Logger logger = Logger.getLogger(LinkingControllerTest.class);
    private AuthenticatedTestHelper ath;
    private ValidationHelper vh;
    private OwnedResourceManagingHelper<Template> ormh;

    final static String serviceUrl = "/e-link";
    final static String mockupUrl = "/mockups/file";
    final static String inputDataFileUrl = "/linking-ELINK.ttl";

    public LinkingControllerTest() throws UnirestException, IOException {
        ApplicationContext context = IntegrationTestSetup.getContext("linking-controller-test-package.xml");
        ath = context.getBean(AuthenticatedTestHelper.class);
        vh = context.getBean(ValidationHelper.class);
        ormh = new OwnedResourceManagingHelper<>("/e-link/templates",Template.class, ath);//, null);
        ath.authenticateUsers();
    }

    /*@Test
    public void templateSerialization() throws Exception {
        User owner = new User("name", "password", User.roleUser);
        Template template = new Template(owner,OwnedResource.Visibility.PUBLIC, Template.Type.SPARQL,"endpoint","query","label","description");

        ObjectMapper om = new ObjectMapper();
        //om.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);;
        ObjectWriter ow = om.writer().withDefaultPrettyPrinter();
        String serialization = ow.writeValueAsString(template);
        logger.info(serialization);
    }*/


    @Test
    public void testTemplateManaging() throws IOException, UnirestException {
        logger.info("start test");

        Template template1 = constructTemplate(
                "template1",
                "PREFIX dbpedia: <http://dbpedia.org/resource/>\nPREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\nPREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\nCONSTRUCT {\n  ?museum <http://xmlns.com/foaf/0.1/based_near> <@@@entity_uri@@@> .\n}\nWHERE {\n  <@@@entity_uri@@@> geo:geometry ?citygeo .\n  ?museum rdf:type <http://schema.org/Museum> .\n  ?museum geo:geometry ?museumgeo .\n  FILTER (<bif:st_intersects>(?museumgeo, ?citygeo, 10))\n} LIMIT 10",
                null,
                "description1",
                "sparql",
                "PUBLIC");

        Template template2 = constructTemplate(
                "template2",
                "PREFIX dbpedia: <http://dbpedia.org/resource/> PREFIX dbpedia-owl: <http://dbpedia.org/ontology/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> CONSTRUCT { ?event <http://dbpedia.org/ontology/place> <@@@entity_uri@@@> . } WHERE { ?event rdf:type <http://dbpedia.org/ontology/Event> .  ?event <http://dbpedia.org/ontology/place> <@@@entity_uri@@@> .  } LIMIT 10",
                null,
                "description2",
                "sparql",
                "PUBLIC");


        logger.info("start CRUD check");
        ormh.checkCRUDOperations(new SimpleEntityRequest(template1.toJson()), new SimpleEntityRequest(template2.toJson()), template1, template2, "9999");
    }

    @Test
    public void testExploreSparql() throws UnirestException, IOException {
        HttpResponse<String> response;

        String rdf_resource = "http://dbpedia.org/resource/Berlin";
        String endpoint = ath.getAPIBaseUrl()+ mockupUrl +"/linking-EXPLORE-Berlin.ttl";

        response= Unirest.post(ath.getAPIBaseUrl()+serviceUrl+"/explore")
                .queryString("informat","turtle")
                .queryString("outformat","turtle")
                .queryString("endpoint-type","sparql")
                .queryString("resource", rdf_resource)
                .queryString("endpoint", endpoint)
                .asString();

        vh.validateNIFResponse(response, TURTLE);

    }

    @Test
    public void testExploreLdf() throws UnirestException, IOException {

        String rdf_resource = "http://dbpedia.org/resource/Berlin";
        String endpoint = ath.getAPIBaseUrl()+ mockupUrl + "/linking-EXPLORE-ldf-resource-Berlin.ttl";

        HttpResponse<String> response =  Unirest.post(ath.getAPIBaseUrl()+serviceUrl+"/explore")
                .queryString("informat","turtle")
                .queryString("outformat","turtle")
                .queryString("endpoint-type","ldf")
                .queryString("resource", rdf_resource)
                .queryString("endpoint", endpoint)
                .asString();


        vh.validateNIFResponse(response, TURTLE);
    }

//    @Test
//    public void testLinkingDocuments() throws Exception {
//
//        logger.info("testLinkingDocuments");
//
//        //String endpoint = "http://dbpedia.org/sparql/";
//        String endpoint = null;
//        logger.info("create private template");
//        Template privateTemplate = ormh.createEntity(
//                new SimpleEntityRequest(constructTemplate("Some label",  readFile("linking-sparql1.ttl"), endpoint, "Some description", "sparql", "private").toJson()),
//                AuthenticatedTestHelper.getTokenWithPermission(),
//                HttpStatus.OK
//        );
//
//        logger.info("create public template");
//        
//        Template publicTemplate = ormh.createEntity(
//                new SimpleEntityRequest(constructTemplate("Some label", readFile("linking-sparql1.ttl"), endpoint , "Some description", "sparql", "public").toJson()),
//                AuthenticatedTestHelper.getTokenWithPermission(),
//                HttpStatus.OK
//        );
//
//        logger.info("read nif to enrich");
//        String nifContent = readFile("data.ttl");
//        try {
//            logger.info("try to enrich via private template as other user... should not work");
//            LoggingHelper.loggerIgnore(LoggingHelper.accessDeniedExceptions);
//            assertEquals(HttpStatus.UNAUTHORIZED.value(), doLinking(nifContent, privateTemplate.getIdentifier(), AuthenticatedTestHelper.getTokenWithoutPermission(), new ArrayList<String>()));
//            LoggingHelper.loggerUnignore(LoggingHelper.accessDeniedExceptions);
//
//            logger.info("try to enrich via private template as template owner... should work");
//            ArrayList<String> response = new ArrayList<String>();
//            assertEquals(HttpStatus.OK.value(), doLinking(nifContent, privateTemplate.getIdentifier(), AuthenticatedTestHelper.getTokenWithPermission(),response));
//            logger.info("check if the response contains the expected enrichment information that is obtained from applying the template");
//            assertTrue(response.get(0).contains("http://dbpedia.org/resource/Museum_of_Asian_Art")&&
//         		   	   response.get(0).contains("http://dbpedia.org/resource/Keramik-Museum_Berlin"));
//            
//            logger.info("try to enrich via public template as other user... should work");
//            assertEquals(HttpStatus.OK.value(), doLinking(nifContent, publicTemplate.getIdentifier(), AuthenticatedTestHelper.getTokenWithoutPermission(), new ArrayList<String>()));
//            
//            logger.info("try to enrich via public template as template owner... should work");
//            assertEquals(HttpStatus.OK.value(), doLinking(nifContent, publicTemplate.getIdentifier(), AuthenticatedTestHelper.getTokenWithPermission(), new ArrayList<String>()));
//        } finally {
//            logger.info("delete private template");
//            ormh.deleteEntity(privateTemplate.getIdentifier(),AuthenticatedTestHelper.getTokenWithPermission(),HttpStatus.OK);
//            logger.info("delete public template");
//            ormh.deleteEntity(publicTemplate.getIdentifier(),AuthenticatedTestHelper.getTokenWithPermission(),HttpStatus.OK);
//        }
//    }

    private Template constructTemplate(String label, String query, String endpoint, String description, String endpointType, String visibility) throws JsonProcessingException {
        if(endpoint==null)
            endpoint = ath.getAPIBaseUrl()+ mockupUrl + inputDataFileUrl;
        Template template = new Template();
        template.setVisibility(OwnedResource.Visibility.getByString(visibility));
        template.setDescription(description);
        template.setEndpointType(Template.Type.getByString(endpointType));
        template.setEndpoint(endpoint);
        template.setQuery(query);
        template.setLabel(label);
        return template;
    }

    private int doLinking(String nifContent, String templateId, String token, ArrayList<String> requestResponse) throws UnirestException, IOException {
        HttpResponse<String> response = ath.addAuthentication(Unirest.post(ath.getAPIBaseUrl()+serviceUrl+"/documents"), token)
                .queryString(LinkingController.templateIdentiferName, templateId)
                .queryString("informat", "turtle")
                .queryString("outformat", "turtle")
                .body(nifContent)
                .asString();
        if(response.getStatus()==HttpStatus.OK.value()) {
            vh.validateNIFResponse(response, TURTLE);
        }
        
        //make body of response accessible
        requestResponse.add(response.getBody());
        return response.getStatus();
    }


    private String readFile(String filename) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(filename).getFile());
        //File file = new File("src/main/resources/mockup-endpoint-mockup-endpoint-data/"+filename);
        return FileUtils.readFileToString(file);
    }
}
