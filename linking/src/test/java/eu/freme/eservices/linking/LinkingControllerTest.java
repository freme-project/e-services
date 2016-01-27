package eu.freme.eservices.linking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import eu.freme.bservices.testhelper.AuthenticatedTestHelper;
import eu.freme.bservices.testhelper.OwnedResourceManagingHelper;
import eu.freme.bservices.testhelper.SimpleEntityRequest;
import eu.freme.bservices.testhelper.ValidationHelper;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.persistence.model.OwnedResource;
import eu.freme.common.persistence.model.Template;
import org.apache.log4j.Logger;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
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
    final static String mockupEntityUrl = "/linking-ELINK.ttl";

    public LinkingControllerTest() throws UnirestException {
        ApplicationContext context = IntegrationTestSetup.getContext("linking-controller-test-package.xml");
        ath = context.getBean(AuthenticatedTestHelper.class);
        vh = context.getBean(ValidationHelper.class);
        ormh = new OwnedResourceManagingHelper<>(serviceUrl,Template.class, ath, null);
        ath.authenticateUsers();
    }

    @Test
    public void testTemplateManaging() throws IOException, UnirestException {
        logger.info("start test");
        String body1 = constructTemplate(
                "template1",
                "PREFIX dbpedia: <http://dbpedia.org/resource/>\nPREFIX dbpedia-owl: <http://dbpedia.org/ontology/>\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\nPREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\nCONSTRUCT {\n  ?museum <http://xmlns.com/foaf/0.1/based_near> <@@@entity_uri@@@> .\n}\nWHERE {\n  <@@@entity_uri@@@> geo:geometry ?citygeo .\n  ?museum rdf:type <http://schema.org/Museum> .\n  ?museum geo:geometry ?museumgeo .\n  FILTER (<bif:st_intersects>(?museumgeo, ?citygeo, 10))\n} LIMIT 10",
                null,
                "description1",
                "sparql",
                "PUBLIC");
        String body2 = constructTemplate(
                "template2",
                "PREFIX dbpedia: <http://dbpedia.org/resource/> PREFIX dbpedia-owl: <http://dbpedia.org/ontology/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> CONSTRUCT { ?event <http://dbpedia.org/ontology/place> <@@@entity_uri@@@> . } WHERE { ?event rdf:type <http://dbpedia.org/ontology/Event> .  ?event <http://dbpedia.org/ontology/place> <@@@entity_uri@@@> .  } LIMIT 10",
                null,
                "description2",
                "sparql",
                "PUBLIC");

        logger.info("start CRUD check");
        ormh.checkCRUDOperations(new SimpleEntityRequest(body1), new SimpleEntityRequest(body2));
    }

    @Test
    public void testLinking(){
        // TODO: implement!
        // also check private template access
    }

    @Test
    public void testELinkExploreSparqlMockup() throws UnirestException, IOException {
        /*HttpResponse<String> response;

        String rdf_resource = "http://dbpedia.org/resource/Berlin";
        String endpoint = ath.getAPIBaseUrl()+ mockupUrl +"/linking-EXPLORE-Berlin.ttl";

        rdf_resource = "http://dbpedia.org/resource/Berlin";

        response= Unirest.post(ath.getAPIBaseUrl()+serviceUrl+"/explore")
                .queryString("informat","turtle")
                .queryString("outformat","turtle")
                .queryString("endpoint-type","sparql")
                .queryString("resource", rdf_resource)
                .queryString("endpoint", endpoint)
                .asString();

        vh.validateNIFResponse(response, RDFConstants.RDFSerialization.TURTLE);
*/
    }

    /*@Test
    public void testDummy() throws UnirestException {
        HttpResponse<String> response =  Unirest.get(ath.getAPIBaseUrl()+mockupUrl+"/linking-EXPLORE-ldf-resource-Berlin.ttl").asString();
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }*/

    @Test
    public void testELinkExploreLdfMockup() throws UnirestException, IOException {
/*
        String rdf_resource = "http://dbpedia.org/resource/Berlin";
        String endpoint = ath.getAPIBaseUrl()+ mockupUrl + "/linking-EXPLORE-ldf-resource-Berlin.ttl";
        endpoint = endpoint.replace("localhost", "127.0.0.1");

        HttpResponse<String> response =  Unirest.post(ath.getAPIBaseUrl()+serviceUrl+"/explore")
                .queryString("informat","turtle")
                .queryString("outformat","turtle")
                .queryString("endpoint-type","ldf")
                .queryString("resource", rdf_resource)
                .queryString("endpoint", endpoint)
                .asString();


        vh.validateNIFResponse(response, RDFConstants.RDFSerialization.TURTLE);
*/
    }

    //Used for constructiong Templates with sparql queries in E-link and E-Link Security Test
    public String constructTemplate(String label, String query, String endpoint, String description, String endpointType, String visibility) throws JsonProcessingException {
        if(endpoint==null)
            endpoint = ath.getAPIBaseUrl()+ mockupUrl + mockupEntityUrl;
        Template template = new Template(null, OwnedResource.Visibility.getByString(visibility), Template.Type.getByString(endpointType), endpoint, query, label, description);
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String serialization = ow.writeValueAsString(template);
        return serialization;
    }


}
