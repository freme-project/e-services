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
package eu.freme.eservices.tilde.terminology;

import com.hp.hpl.jena.rdf.model.Model;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.exception.ExternalServiceFailedException;
import eu.freme.common.exception.NIFVersionNotSupportedException;
import eu.freme.common.rest.BaseRestController;
import eu.freme.common.rest.NIFParameterSet;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static eu.freme.common.conversion.rdf.RDFConstants.TURTLE;

/**
 * REST controller for Tilde e-Translation service
 * 
 * @author Jan Nehring - jan.nehring@dfki.de
 */
@RestController
public class TildeETerminology extends BaseRestController {

	Logger logger = Logger.getLogger(TildeETerminology.class);

//	@Value("${freme.broker.tildeETerminologyUrl:https://services.tilde.com/Terminology/?sourceLang={source-lang}&targetLang={target-lang}}")
	@Value("${freme.broker.tildeETerminologyUrl:https://services.tilde.com/Terminology/}")
//	private String endpoint;// = "https://services.tilde.com/translation/?sourceLang={source-lang}&targetLang={target-lang}";
	private String endpoint = "https://services.tilde.com/Terminology/";

	@RequestMapping(value = "/e-terminology/tilde", method = RequestMethod.POST)
	public ResponseEntity<String> tildeTranslate(
			@RequestHeader(value = "Accept", required = false) String acceptHeader,
			@RequestHeader(value = "Content-Type", required = false) String contentTypeHeader,
			@RequestBody(required = false) String postBody,
			@RequestParam(value = "source-lang") String sourceLang,
			@RequestParam(value = "target-lang") String targetLang,
			@RequestParam(value = "domain", defaultValue = "") String domain,
			@RequestParam(value = "mode", defaultValue = "full") String mode,
			@RequestParam(value = "collection", required = false) String collection,
			@RequestParam(value = "key", required= false) String key,
			@RequestParam(value = "nif-version", required = false) String nifVersion,
			@RequestParam Map<String, String> allParams
	) {

		NIFParameterSet parameters = normalizeNif(postBody,acceptHeader,contentTypeHeader, allParams, false);
		parameters.setNifVersion(nifVersion);
		Model inputModel = getRestHelper().convertInputToRDFModel(parameters);

		// send request to tilde mt
		Model responseModel = null;
		try {
			HttpResponse<String> response = Unirest.post(endpoint)
					.queryString("sourceLang", sourceLang)
					.queryString("targetLang", targetLang)
					.queryString("domain", domain)
					.header("Accept", "application/turtle")
					.header("Content-Type", "application/turtle")
					.queryString("mode", mode)
					.queryString("collection", collection)
					.header("Authentication", "Basic RlJFTUU6dXxGcjNtM19zJGN1ciQ=")
					.queryString("key", key)
					.queryString("nif-version", nifVersion)
					.body(serializeRDF(inputModel, TURTLE)).asString();


			if (response.getStatus() != HttpStatus.OK.value()) {
				throw new ExternalServiceFailedException(
						"External service failed: "+response.getBody(),
						HttpStatus.valueOf(response.getStatus()));
			}


			String translation = response.getBody();

			responseModel = unserializeRDF(translation, TURTLE);
			
		} catch (Exception e) {
			if (e instanceof ExternalServiceFailedException) {
				throw new ExternalServiceFailedException(e.getMessage(),
						((ExternalServiceFailedException) e)
								.getHttpStatusCode());
			} else {
				throw new ExternalServiceFailedException(e.getMessage());
			}
		}
		
		return createSuccessResponse(responseModel, parameters.getOutformatString());


	}
}
