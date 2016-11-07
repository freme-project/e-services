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

import java.util.List;

/**
 *
 * @author Pieter Heyvaert <pheyvaer.heyvaert@ugent.be>
 */
public class Section {
    
    private List<Section> subsections;
    private String title, resource;
    
    public Section() {}

    public Section(String title, String resource) {
        this.title = title;
        this.resource = resource;
    }
    
    public void addSection(Section section) {
        subsections.add(section);
    }

    public List<Section> getSubsections() {
        return subsections;
    }

    public void setSubsections(List<Section> subsections) {
        this.subsections = subsections;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    } 
    
    public static boolean hasSectionWithResource(List<Section> sections, String resource) {
        if (sections == null) {
            return false;
        }
        
        int i = 0;
        boolean sectionFound = false;
        
        while (i < sections.size() && !sectionFound) {
            Section section = sections.get(i);
            
            String temp = section.getResource();
            
            if (temp.contains("#")) {
                temp = temp.substring(0, temp.indexOf("#"));
            }
            
            sectionFound = temp.equals(resource);
            
            if (!sectionFound && sections.get(i).getSubsections() != null) {
                if (hasSectionWithResource(sections.get(i).getSubsections(), resource)) {
                    sectionFound = true;
                }
            }
            
            i ++;
        }
        
        return sectionFound;
    }
}