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
package eu.freme.eservices.tilde.translation;

import com.google.common.base.Strings;
import com.hp.hpl.jena.rdf.model.Model;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import eu.freme.common.conversion.etranslate.TranslationConversionService;
import eu.freme.common.conversion.rdf.RDFConstants;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.ExternalServiceFailedException;
import eu.freme.common.exception.NIFVersionNotSupportedException;
import eu.freme.common.rest.BaseRestController;
import eu.freme.common.rest.NIFParameterSet;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
public class TildeETranslation extends BaseRestController {

	Logger logger = Logger.getLogger(TildeETranslation.class);

	@Autowired
	TranslationConversionService translationConversionService;

	//live version: https://services.tilde.com/translation/
	//dev version: https://services.tilde.com/dev/translation
	@Value("${freme.broker.tildeETranslationUrl:https://services.tilde.com/translation}")
	private String endpoint;

	@Value("${tilde.translation.authentication:}")
	private String authCode;

	@RequestMapping(value = "/e-translation/tilde", method = RequestMethod.POST)
	public ResponseEntity<String> tildeTranslate(
			@RequestHeader(value = "Accept", required = false) String acceptHeader,
			@RequestHeader(value = "Content-Type", required = false) String contentTypeHeader,
			@RequestBody(required = false) String postBody,
			@RequestParam(value = "source-lang", required = false) String sourceLang,
			@RequestParam(value = "target-lang", required = false) String targetLang,
			@RequestParam(value = "domain", defaultValue = "") String domain,
			@RequestParam(value = "system", defaultValue = "full") String system,
			@RequestParam(value = "key", required = false) String key,
			//// moved to eu.freme.common.rest.RestHelper.normalizeNif(String postBody, String acceptHeader,
			//// String contentTypeHeader, Map<String, String> parameters, boolean allowEmptyInput) and
			//// eu.freme.common.rest.NIFParameterFactory.constructFromHttp(...)
			//@RequestParam(value = "nif-version", defaultValue = nifVersion2_0) String nifVersion,
			@RequestParam Map<String, String> allParams)
			{

		NIFParameterSet parameters = normalizeNif(postBody,acceptHeader,contentTypeHeader, allParams, false);
		Model inputModel = getRestHelper().convertInputToRDFModel(parameters);

		// send request to tilde mt
		Model responseModel=null;

		//check if input parameters are provided correctly 
		if(Strings.isNullOrEmpty(sourceLang) || Strings.isNullOrEmpty(targetLang)){
			if(Strings.isNullOrEmpty(system))
				throw new BadRequestException("Please specify either the two parameters 'source-lang' and 'target-lang' or the parameter 'system' to define the languages which shall be used.");
		}


		HttpResponse<String> response;
		try{

			if(Strings.isNullOrEmpty(sourceLang)){

				response = Unirest
						.post(endpoint)
						.queryString("system", system)
						.header("Accept", "application/x-turtle")
						.header("Content-Type", "application/x-turtle")
						.header("Authentication", authCode)
						.queryString("domain", domain)
						.queryString("key", key)
						.queryString("nif-version", parameters.getNifVersion())
						.body(serializeRDF(inputModel, TURTLE)).asString();

			}else{

				//source-lang and target-lang parameters are used
				response = Unirest
						.post(endpoint)
						.queryString("sourcelang", sourceLang)
						.queryString("targetlang", targetLang)
						.header("Accept", "application/x-turtle")
						.header("Content-Type", "application/x-turtle")
						.queryString("system", system)
						.header("Authentication", authCode)
						.queryString("domain", domain)
						.queryString("key", key)
						.queryString("nif-version", parameters.getNifVersion())
						.body(serializeRDF(inputModel, TURTLE)).asString();

			}



			if (response.getStatus() != HttpStatus.OK.value()) {
				throw new ExternalServiceFailedException(
						"External service failed: " + response.getBody(),
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

		return  createSuccessResponse(responseModel, parameters.getOutformatString());
			}
}
