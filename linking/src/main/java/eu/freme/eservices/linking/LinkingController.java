package eu.freme.eservices.linking;

import com.hp.hpl.jena.rdf.model.Model;
import eu.freme.common.conversion.rdf.RDFConversionService;
import eu.freme.common.exception.*;
import eu.freme.common.persistence.model.Template;
import eu.freme.common.rest.NIFParameterFactory;
import eu.freme.common.rest.NIFParameterSet;
import eu.freme.common.rest.OwnedResourceManagingController;
import eu.freme.eservices.elink.api.DataEnricher;
import eu.freme.eservices.linking.exceptions.InvalidNIFException;
import eu.freme.eservices.linking.exceptions.InvalidTemplateEndpointException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 26.01.2016.
 */
@RequestMapping("/e-link")
public class LinkingController extends OwnedResourceManagingController<Template> {

    Logger logger = Logger.getLogger(LinkingController.class);

    @Autowired
    DataEnricher dataEnricher;

    @Autowired
    TemplateValidator templateValidator;

    @Autowired
    RDFConversionService rdfConversionService;

    @Autowired
    NIFParameterFactory nifParameterFactory;

    // Enriching using a template.
    // POST /e-link/enrich/
    // Example: curl -X POST -d @data.ttl
    // "http://localhost:8080/e-link/enrich/documents/?outformat=turtle&templateid=3&limit-val=4"
    // -H "Content-Type: text/turtle"
    @RequestMapping(value = "/documents", method = RequestMethod.POST)
    @Secured({ "ROLE_USER", "ROLE_ADMIN" })
    public ResponseEntity<String> enrich(
            @RequestParam(value = "templateid", required = true) String templateIdStr,
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            @RequestHeader(value = "Content-Type", required = false) String contentTypeHeader,
            @RequestBody String postBody,
            @RequestParam Map<String, String> allParams) {
        try {

            Long templateId;
            try {
                templateId = new Long(templateIdStr);
            } catch (NumberFormatException e) {
                logger.error(e);
                String msg = "Parameter templateid is required to be a numeric value.";
                throw new BadRequestException(msg);
            }

            // int templateId = validateTemplateID(templateIdStr);
            NIFParameterSet nifParameters = this.normalizeNif(postBody,
                    acceptHeader, contentTypeHeader, allParams, false);

            // templateDAO.findOneById(templateIdStr);
            // Check read access and retrieve the template
            Template template = getEntityDAO().findOneByIdentifier(id);

            HashMap<String, String> templateParams = new HashMap<>();

            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                if (!nifParameterFactory.isNIFParameter(entry.getKey())) {
                    templateParams.put(entry.getKey(), entry.getValue());
                }
            }

            Model inModel = rdfConversionService.unserializeRDF(
                    nifParameters.getInput(), nifParameters.getInformat());
            inModel = dataEnricher.enrichWithTemplate(inModel, template,
                    templateParams);

            HttpHeaders responseHeaders = new HttpHeaders();
            String serialization = rdfConversionService.serializeRDF(inModel,
                    nifParameters.getOutformat());
            responseHeaders.add("Content-Type", nifParameters.getOutformat()
                    .contentType());
            return new ResponseEntity<>(serialization, responseHeaders,
                    HttpStatus.OK);
        } catch (AccessDeniedException ex) {
            logger.error(ex.getMessage(), ex);
            throw ex;
        } catch (BadRequestException ex) {
            logger.error(ex.getMessage(), ex);
            throw ex;
        } catch (OwnedResourceNotFoundException ex) {
            logger.error(ex.getMessage());
            throw new TemplateNotFoundException(ex.getMessage());
        } catch (org.apache.jena.riot.RiotException ex) {
            logger.error("Invalid NIF document. " + ex.getMessage(), ex);
            throw new InvalidNIFException(ex.getMessage());
        } catch (Exception ex) {
            logger.error(
                    "Internal service problem. Please contact the service provider.",
                    ex);
            throw new InternalServerErrorException(
                    "Unknown problem. Please contact us.");
        }
    }

    // Enriching using a template.
    // POST /e-link/explore/
    // Example: curl -v -X POST
    // "http://localhost:8080/e-link/explore?resource=http%3A%2F%2Fdbpedia.org%2Fresource%2FBerlin&endpoint=http%3A%2F%2Fdbpedia.org%2Fsparql&outformat=n-triples"
    // -H "Content-Type:"
    @RequestMapping(value = "/explore", method = RequestMethod.POST)
    @Secured({ "ROLE_USER", "ROLE_ADMIN" })
    public ResponseEntity<String> exploreResource(
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            @RequestHeader(value = "Content-Type", required = false) String contentTypeHeader,
            @RequestParam Map<String, String> allParams,
            @RequestParam(value = "resource", required = true) String resource,
            @RequestParam(value = "endpoint", required = true) String endpoint,
            @RequestParam(value = "endpoint-type", required = false) String endpointType) {
        try {

            templateValidator.validateTemplateEndpoint(endpoint);
            NIFParameterSet nifParameters = this.normalizeNif("", acceptHeader,
                    contentTypeHeader, allParams, false);

            Model inModel = dataEnricher.exploreResource(resource, endpoint,
                    endpointType);

            HttpHeaders responseHeaders = new HttpHeaders();
            String serialization = rdfConversionService.serializeRDF(inModel,
                    nifParameters.getOutformat());
            responseHeaders.add("Content-Type", nifParameters.getOutformat()
                    .contentType());
            return new ResponseEntity<>(serialization, responseHeaders,
                    HttpStatus.OK);
        } catch (InvalidTemplateEndpointException ex) {
            logger.error(ex.getMessage(), ex);
            throw new InvalidTemplateEndpointException(ex.getMessage());
        } catch (UnsupportedEndpointType | BadRequestException
                | UnsupportedOperationException ex) {
            logger.error(ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            logger.error(
                    "Internal service problem. Please contact the service provider.",
                    ex);
            throw new InternalServerErrorException(
                    "Unknown problem. Please contact us.");
        }
    }

    @Override
    protected Template createEntity(String body, Map<String, String> parameters, Map<String, String> headers) throws BadRequestException {
        return null;
    }

    @Override
    protected void updateEntity(Template template, String body, Map<String, String> parameters, Map<String, String> headers) throws BadRequestException {

    }
}
