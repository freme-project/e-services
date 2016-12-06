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

import com.hp.hpl.jena.rdf.model.Model;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.exception.*;
import eu.freme.common.persistence.dao.OwnedResourceDAO;
import eu.freme.common.persistence.model.Template;
import eu.freme.common.rest.BaseRestController;
import eu.freme.common.rest.NIFParameterSet;
import eu.freme.eservices.elink.api.DataEnricher;
import eu.freme.eservices.elink.exceptions.InvalidNIFException;
import eu.freme.eservices.elink.exceptions.InvalidTemplateEndpointException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 26.01.2016.
 */
@RestController
@RequestMapping("/e-link")
public class LinkingController extends BaseRestController{

    Logger logger = Logger.getLogger(LinkingController.class);

    public final static String templateIdentiferName = "templateid";

    @Autowired
    DataEnricher dataEnricher;

    @Autowired
    TemplateValidator templateValidator;

    @Autowired
    OwnedResourceDAO<Template> entityDAO;

    // Enriching using a template.
    // POST /e-link/enrich/
    // Example: curl -X POST -d @mockup-endpoint-data.ttl
    // "http://localhost:8080/e-link/enrich/documents/?outformat=turtle&templateid=3&limit-val=4"
    // -H "Content-Type: text/turtle"
    @RequestMapping(value = "/documents", method = RequestMethod.POST)
    @Secured({ "ROLE_USER", "ROLE_ADMIN" })
    public ResponseEntity<String> enrich(
            @RequestParam(value = templateIdentiferName) String templateIdStr,
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            @RequestHeader(value = "Content-Type", required = false) String contentTypeHeader,
            @RequestBody (required = false) String postBody,
            @RequestParam Map<String, String> allParams) {
        try {
        	
            try {
                new Long(templateIdStr);
            } catch (NumberFormatException e) {
                logger.error(e);
                String msg = "Parameter templateid is required to be a numeric value.";
                throw new BadRequestException(msg);
            }

            // int templateId = validateTemplateID(templateIdStr);
            NIFParameterSet nifParameters = normalizeNif(postBody,
                    acceptHeader, contentTypeHeader, allParams, false);

            // templateDAO.findOneById(templateIdStr);
            // Check read access and retrieve the template
            Template template = entityDAO.findOneByIdentifier(templateIdStr);

            HashMap<String, String> templateParams = new HashMap<>();

            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                if (!getNifParameterFactory().isNIFParameter(entry.getKey())) {
                    templateParams.put(entry.getKey(), entry.getValue());
                }
            }

            Model inModel = unserializeRDF(
                    nifParameters.getInput(), nifParameters.getInformatString());
            inModel = dataEnricher.enrichWithTemplate(inModel, template,
                    templateParams);

            return createSuccessResponse(inModel,nifParameters.getOutformatString());
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
            //@RequestHeader(value = "Content-Type", required = false) String contentTypeHeader,
            @RequestParam Map<String, String> allParams,
            @RequestParam(value = "resource") String resource,
            @RequestParam(value = "endpoint") String endpoint,
            @RequestParam(value = "endpoint-type", required = false) String endpointType) {
        try {

            templateValidator.validateTemplateEndpoint(endpoint);
            NIFParameterSet nifParameters = normalizeNif(null, acceptHeader,
                    null, allParams, true);

            Model outModel = dataEnricher.exploreResource(resource, endpoint,
                    endpointType);

            return createSuccessResponse(outModel,nifParameters.getOutformatString());
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


}
