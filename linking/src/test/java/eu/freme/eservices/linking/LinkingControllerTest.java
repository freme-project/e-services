package eu.freme.eservices.linking;

import com.mashape.unirest.http.exceptions.UnirestException;
import eu.freme.bservices.testhelper.AuthenticatedTestHelper;
import eu.freme.bservices.testhelper.OwnedResourceManagingHelper;
import eu.freme.bservices.testhelper.api.IntegrationTestSetup;
import eu.freme.common.persistence.model.Template;
import org.apache.log4j.Logger;

import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 * Created by Arne Binder (arne.b.binder@gmail.com) on 27.01.2016.
 */
public class LinkingControllerTest {
    private Logger logger = Logger.getLogger(LinkingControllerTest.class);
    private AuthenticatedTestHelper ath;
    private OwnedResourceManagingHelper<Template> ormh;
    final static String serviceUrl = "/e-link";

    public LinkingControllerTest() throws UnirestException {
        ApplicationContext context = IntegrationTestSetup.getContext("linking-controller-test-package.xml");
        ath = context.getBean(AuthenticatedTestHelper.class);
        ormh = new OwnedResourceManagingHelper<>(serviceUrl,Template.class, ath, null);
        ath.authenticateUsers();
    }

    @Test
    public void test(){
        logger.info("start test");
    }

}
