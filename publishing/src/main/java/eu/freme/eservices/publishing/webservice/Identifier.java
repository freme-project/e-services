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
package eu.freme.eservices.publishing.webservice;

/**
 *
 * @author Pieter Heyvaert <pheyvaer.heyvaert@ugent.be>
 */
public class Identifier {
    
    private String scheme, value;

    public Identifier() {}
    
    public Identifier(String scheme, String value) {
        this.scheme = scheme;
        this.value = value;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}