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
package eu.freme.eservices.elink.api;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.*;
import eu.freme.common.exception.BadRequestException;
import eu.freme.common.exception.FREMEHttpException;
import eu.freme.common.exception.InternalServerErrorException;
import eu.freme.common.exception.UnsupportedEndpointType;
import eu.freme.common.persistence.dao.TemplateDAO;
import eu.freme.common.persistence.model.Template;
import eu.freme.common.persistence.model.Template.Type;
import org.apache.log4j.Logger;
import org.linkeddatafragments.model.LinkedDataFragmentGraph;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 *
 * @author Milan Dojchinovski <milan.dojchinovski@fit.cvut.cz>
 * http://dojchinovski.mk
 */
@Component
public class DataEnricher {

    private boolean initialized = false;
    private final String basePath = "http://www.freme-project.eu/mockup-endpoint-data/templates/";
    private final String templatesNs = "http://www.freme-project.eu/ns#";

    static Logger logger = Logger.getLogger(DataEnricher.class);
    private final String exploreQuery = "CONSTRUCT { <@@@entity_uri@@@> ?p ?o . } WHERE { <@@@entity_uri@@@> ?p ?o . }";
    
    public DataEnricher(){
    }
    
    /**
     * Called to enrich NIF document with a template.
     * @param model          The NIF document represented as Jena model.
     * @param template       The template to be used for enrichment.
     * @param templateParams Map of user defined parameters.
     */
    public Model enrichWithTemplate(Model model, Template template, HashMap<String, String> templateParams) {
        try {
            if(template.getEndpointType() == Type.SPARQL) {
                return enrichWithTemplateSPARQL(model, template, templateParams);
            } else if(template.getEndpointType() == Type.LDF) {
                return enrichWithTemplateLDF(model, template, templateParams);                
            }
            return model;
        } catch (FREMEHttpException ex) {
            logger.error(getFullStackTrace(ex));
            throw new BadRequestException(ex.getMessage());
        } catch (Exception ex) {
            logger.error(getFullStackTrace(ex));
            throw new BadRequestException("It seems your SPARQL template is not correctly defined.");
        }
    }
    
    /**
     * Called to enrich NIF document with template using SPARQL endpoint.
     * @param model          The NIF document represented as Jena model.
     * @param template       The template to be used for enrichment.
     * @param templateParams Map of user defined parameters.
     */
    public Model enrichWithTemplateSPARQL(Model model, Template template, HashMap<String, String> templateParams) {
        String endpoint=null;
        String query = null;
        try {
            StmtIterator ex = model.listStatements((Resource) null, model.getProperty("http://www.w3.org/2005/11/its/rdf#taIdentRef"), (RDFNode) null);

            Model enrichment = ModelFactory.createDefaultModel();

            // Iterating through every entity and enriching it.
            while (ex.hasNext()) {

                Statement stm = ex.nextStatement();
                String entityURI = stm.getObject().asResource().getURI();

                // Replacing the entity_uri fields in the template with the entity URI.
                query = template.getQuery().replaceAll("@@@entity_uri@@@", entityURI);

                Map.Entry resModel;
                // Replacing the other custom fields in the template with the corresponding values.
                for (Iterator e = templateParams.entrySet().iterator(); e.hasNext(); query = query.replaceAll("@@@" + (String) resModel.getKey() + "@@@", (String) resModel.getValue())) {
                    resModel = (Map.Entry) e.next();
                }


                endpoint = template.getEndpoint();
//                logger.error(endpoint);
//       ///      logger.error(query);
                // Executing the enrichement.
                QueryExecution e1 = QueryExecutionFactory.sparqlService(endpoint, query);
                Model resModel1 = e1.execConstruct();
                enrichment.add(resModel1);
                e1.close();
                Thread.sleep(400);
            }

            model.add(enrichment);
            return model;

        }catch(org.apache.jena.riot.RiotException ex){
            logger.error(getFullStackTrace(ex));
            throw new InternalServerErrorException("Could not process the enrichment result from the endpoint="+endpoint+" executing the query="+query+". Error message: "+ex.getMessage());
        } catch (com.hp.hpl.jena.query.QueryParseException ex) {
            logger.error(getFullStackTrace(ex));
            throw new BadRequestException("It seems your SPARQL template is not correctly defined.");
        } catch (InterruptedException e) {
            logger.error(getFullStackTrace(e));
            throw new InternalServerErrorException("Failed to interrupt the thread for 400ms.");
        }
    }

     /**
     * Called to enrich NIF document template using LDF endpoint.
     * @param model          The NIF document represented as Jena model.
     * @param template       The template to be used for enrichment.
     * @param templateParams Map of user defined parameters.
     */
    public Model enrichWithTemplateLDF(Model model, Template template, HashMap<String, String> templateParams) {
        try {
            StmtIterator ex = model.listStatements((Resource)null, model.getProperty("http://www.w3.org/2005/11/its/rdf#taIdentRef"), (RDFNode)null);

            Model enrichment = ModelFactory.createDefaultModel();

            // Iterating through every entity and enriching it.
            while(ex.hasNext()) {
                Statement stm = ex.nextStatement();
                String entityURI = stm.getObject().asResource().getURI();

                // Replacing the entity_uri fields in the template with the entity URI.
                String query = template.getQuery().replaceAll("@@@entity_uri@@@", entityURI);

                Map.Entry resModel;
                // Replacing the other custom fields in the template with the corresponding values.
                for(Iterator e = templateParams.entrySet().iterator(); e.hasNext(); query = query.replaceAll("@@@" + (String)resModel.getKey() + "@@@", (String)resModel.getValue())) {
                    resModel = (Map.Entry)e.next();
                }

                // Executing the enrichement.
                Model ldfModel;
                LinkedDataFragmentGraph ldfg = new LinkedDataFragmentGraph(template.getEndpoint());
                ldfModel = ModelFactory.createModelForGraph(ldfg);
                Query qry = QueryFactory.create(query);
                QueryExecution qe = QueryExecutionFactory.create(qry, ldfModel);
                Model enrichedModel = qe.execConstruct();
                enrichment.add(enrichedModel);
                qe.close();                
            }
            model.add(enrichment);
            return model;
        } catch (Exception ex) {
            logger.error(getFullStackTrace(ex));
            throw new BadRequestException("It seems your SPARQL template is not correctly defined.");
        }
    }

    
    /**
     * Called to describe a resource.
     * @param resource     Resource URL.
     * @param endpoint     Endpoint URL.
     * @param endpointType Endpoint type.
     */
    public Model exploreResource(String resource, String endpoint, String endpointType) throws BadRequestException {
        
        if (endpointType == null) {
            return enrichViaSPARQL(resource, endpoint);
        } else if(endpointType.equals("sparql")) {
            return enrichViaSPARQL(resource, endpoint);
        } else if (endpointType.equals("ldf")){
            return enrichViaLDFExplore(resource, endpoint);
        } else{
            throw new UnsupportedEndpointType("Unsupported endpoint type. Only 'sparql' and 'ldf' are supported.");
        }
    }

    private Model enrichViaSPARQL(String resource, String endpoint) throws BadRequestException {
        try {
            Model model = ModelFactory.createDefaultModel();
            String query = exploreQuery.replaceAll("@@@entity_uri@@@", resource);
            QueryExecution e1 = QueryExecutionFactory.sparqlService(endpoint, query);
            Model resModel1 = e1.execConstruct();
            model.add(resModel1);
            e1.close();
            return model;
        } catch (Exception ex) {
            logger.error(getFullStackTrace(ex));
            throw new BadRequestException("Something went wrong when retrieving the content. Please contact the maintainers.");
        }
    }

    private Model enrichViaLDFExplore(String resource, String endpoint) {
        try {
            Model model;
            LinkedDataFragmentGraph ldfg = new LinkedDataFragmentGraph(endpoint);
            model = ModelFactory.createModelForGraph(ldfg);
            String queryString = exploreQuery.replaceAll("@@@entity_uri@@@", resource);
            Query qry = QueryFactory.create(queryString);
            QueryExecution qe = QueryExecutionFactory.create(qry, model);
            Model returnModel = qe.execConstruct();
            qe.close();
            return returnModel;
        } catch (Exception ex) {
            logger.error(getFullStackTrace(ex));
            throw new BadRequestException("Something went wrong when retrieving the content. Please contact the maintainers.");
        }
    }

    private String getFullStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        return exceptionAsString;
    }
}