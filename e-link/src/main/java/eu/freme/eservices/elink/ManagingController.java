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

import com.google.common.base.Strings;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.persistence.model.Template;
import eu.freme.common.rest.OwnedResourceManagingController;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 04.04.2016.
 */

@RestController
@RequestMapping("/e-link/templates")
public class ManagingController extends OwnedResourceManagingController<Template> {

    Logger logger = Logger.getLogger(LinkingController.class);

    @Autowired
    TemplateValidator templateValidator;

    @Override
    protected Template createEntity(String body, Map<String, String> parameters, Map<String, String> headers) throws BadRequestException {
        JSONObject jsonObj = new JSONObject(body);
        templateValidator.validateTemplateEndpoint(jsonObj.getString("endpoint"));

        // AccessDeniedException can be thrown, if current
        // authentication is the anonymousUser
        return new Template(jsonObj);
    }

    @Override
    protected void updateEntity(Template template, String body, Map<String, String> parameters, Map<String, String> headers) throws BadRequestException {
        if(!Strings.isNullOrEmpty(body) && !body.trim().isEmpty() && !body.trim().toLowerCase().equals("null") && !body.trim().toLowerCase().equals("empty")) {
            JSONObject jsonObj = new JSONObject(body);
            template.update(jsonObj);
        }
    }
}
