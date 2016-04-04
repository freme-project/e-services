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
