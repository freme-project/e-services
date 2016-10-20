/**
 * Copyright © 2016 Agro-Know, Deutsches Forschungszentrum für Künstliche
 * 							Intelligenz, iMinds,
 * 							Institut für Angewandte Informatik e. V. an
 * 							der Universität Leipzig,
 * 							Istituto Superiore Mario Boella, Tilde,
 * 							Vistatec, WRIPL (http://freme-project.eu)
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
package eu.freme.eservices.publishing;

import com.google.gson.Gson;
import eu.freme.eservices.publishing.exception.EPubCreationException;
import eu.freme.eservices.publishing.exception.InvalidZipException;
import eu.freme.eservices.publishing.exception.MissingMetadataException;
import eu.freme.eservices.publishing.webservice.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Pieter Heyvaert <pheyvaer.heyvaert@ugent.be>
 */
@RestController
@SuppressWarnings("unused")
public class ServiceRestController {

    @Autowired
    EPublishingService epubAPI;

    @RequestMapping(value = "/e-publishing/html", method = RequestMethod.POST)
    public ResponseEntity<byte[]> htmlToEPub(
            @RequestParam("htmlZip") MultipartFile file,
            @RequestParam("metadata") String jMetadata
    ) throws IOException, InvalidZipException, EPubCreationException, MissingMetadataException {
        Gson gson = new Gson();
        Metadata metadata = gson.fromJson(jMetadata, Metadata.class);
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "Application/epub+zip");
        String filename = file.getName();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "Attachment; filename="+ filename.split("\\.")[0]+".epub");
        try (InputStream in = file.getInputStream()) {
            return new ResponseEntity<>(epubAPI.createEPUB(metadata, in), headers, HttpStatus.OK);
        }
    }
}