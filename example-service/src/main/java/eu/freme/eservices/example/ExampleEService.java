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
package eu.freme.eservices.example;

import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import eu.freme.common.conversion.etranslate.TranslationConversionService;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.conversion.rdf.RDFConstants.RDFSerialization;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.ExternalServiceFailedException;
import eu.freme.common.exception.InternalServerErrorException;
import eu.freme.common.rest.BaseRestController;
import eu.freme.common.rest.NIFParameterSet;

/**
 * Example e-Service
 * 
 * @author Jan Nehring - jan.nehring@dfki.de
 */
@RestController
public class ExampleEService extends BaseRestController {

	Logger logger = Logger.getLogger(ExampleEService.class);

	@RequestMapping(value = "/e-xample", method = RequestMethod.POST)
	public ResponseEntity<String> example(
			@RequestHeader(value = "Accept", required = false) String acceptHeader,
			@RequestHeader(value = "Content-Type", required = false) String contentTypeHeader,
			@RequestParam Map<String, String> allParams,
			@RequestBody(required = false) String postBody) {
		
		NIFParameterSet parameters = this.normalizeNif(postBody,acceptHeader,contentTypeHeader,allParams,false);

		// create rdf model
		Model model = ModelFactory.createDefaultModel();

		if (!parameters.getInformat().equals(
				RDFConstants.RDFSerialization.PLAINTEXT)) {
			// input is nif
			try {
				model = this.unserializeNif(parameters.getInput(),
						parameters.getInformat());
			} catch (Exception e) {
				logger.error("failed", e);
				throw new BadRequestException("Error parsing NIF input");
			}
		} else {
			// input is plaintext
			getRdfConversionService().plaintextToRDF(model, parameters.getInput(),
					null, parameters.getPrefix());
		}
		
		Statement textForEnrichment;
		try {
			textForEnrichment = getRdfConversionService().extractFirstPlaintext(model);
		} catch (Exception e) {
			throw new InternalServerErrorException();
		}
		if( textForEnrichment == null ){
			throw new BadRequestException("Could not find input for enrichment");
		}
		
		String plaintext = textForEnrichment.getObject().asLiteral().getString();
		String enrichment = plaintext.toUpperCase();
		Property property = model.createProperty("http://freme-project.eu/example-enrichment");
		textForEnrichment.getSubject().addLiteral(property, enrichment);
		
		return createSuccessResponse(model, parameters.getOutformat());
	}
}
