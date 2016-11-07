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

import eu.freme.eservices.publishing.exception.MissingMetadataException;
import eu.freme.eservices.publishing.webservice.Person;
import eu.freme.eservices.publishing.webservice.Section;
import nl.siegmann.epublib.bookprocessor.Epub2HtmlCleanerBookProcessor;
import nl.siegmann.epublib.bookprocessor.Epub3HtmlCleanerBookProcessor;
import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * A class that generates an EPUB file from the contents of a number of
 * files.</p>
 *
 * <p>
 * Use this class only once per EPUB to create (not thread-safe)!</p>
 *
 * <p>
 * Copyright 2015 MMLab, UGent</p>
 *
 * @author Gerald Haesendonck
 */
public class EPubCreatorImpl implements EPubCreator {

    private final Book book;
    private final Metadata metadata;
    private final eu.freme.eservices.publishing.webservice.Metadata ourMetadata;
    private final File unzippedPath;
    private final EpubWriter epubWriter;

    public EPubCreatorImpl(final eu.freme.eservices.publishing.webservice.Metadata ourMetadata, final File unzippedPath) throws IOException, MissingMetadataException {
        book = new Book();
        this.metadata = new Metadata();
        this.unzippedPath = unzippedPath;
        this.ourMetadata = ourMetadata;

        addCreators(ourMetadata.getCreators());
        addContributors(ourMetadata.getContributors());

        if (ourMetadata.getLanguage() != null) {
            this.metadata.setLanguage(ourMetadata.getLanguage());
        }

        if (ourMetadata.getIdentifier() != null) {
            String scheme = ourMetadata.getIdentifier().getScheme();
            
            if (scheme == null) {
                scheme = "";
            }
            
            this.metadata.addIdentifier(new Identifier(scheme, ourMetadata.getIdentifier().getValue()));
        } else {
            throw new MissingMetadataException("Identifier is missing.");
        }

        if (ourMetadata.getTitles() != null && !ourMetadata.getTitles().isEmpty()) {
            this.metadata.setTitles(ourMetadata.getTitles());
        } else {
            throw new MissingMetadataException("At least one title is required.");
        }

        if (ourMetadata.getSubjects() != null) {
            this.metadata.setSubjects(ourMetadata.getSubjects());
        }

        if (ourMetadata.getPublicationDate() != null) {
            this.metadata.addDate(new Date(ourMetadata.getPublicationDate().getTime()));
        }
  
        if (ourMetadata.getSources() != null) {
            this.metadata.setSources(ourMetadata.getSources());
        }

        if (ourMetadata.getTypes() != null) {
            this.metadata.setTypes(ourMetadata.getTypes());
        }

        if (ourMetadata.getDescriptions() != null) {
            this.metadata.setDescriptions(ourMetadata.getDescriptions());
        }
        
        if (ourMetadata.getRelations() != null) {
            this.metadata.setRelations(ourMetadata.getRelations());
        }

        if (ourMetadata.getRights() != null) {
            this.metadata.setRights(ourMetadata.getRights());
        }

        if (ourMetadata.getTableOfContents() == null) {
            ourMetadata.setTableOfContents(createBestEffortTableOfContents(null));
        }
        
        createSections(ourMetadata.getTableOfContents(), null);

        if (ourMetadata.getCoverImage() != null) {
            addCoverImage(ourMetadata.getCoverImage());
        }

        copyUnaddedFilesFromZipToEpub(null);
        
        if (ourMetadata.getEPUBVersion() != null && ourMetadata.getEPUBVersion().equals("2")) {
            BookProcessor[] bookProcessors = {new Epub2HtmlCleanerBookProcessor()};
            BookProcessor bookProcessorPipeline = new BookProcessorPipeline(Arrays.asList(bookProcessors));
            epubWriter = new Epub2Writer(bookProcessorPipeline);
        } else {
            BookProcessor[] bookProcessors = {new Epub3HtmlCleanerBookProcessor()};
            BookProcessor bookProcessorPipeline = new BookProcessorPipeline(Arrays.asList(bookProcessors));
            epubWriter = new Epub3Writer(bookProcessorPipeline);
        }
    }

    private void addCoverImage(String coverImage) throws IOException {
        try (FileInputStream fis = new FileInputStream(new File(unzippedPath, coverImage))) {
            book.setCoverImage(new Resource(fis, coverImage));
        }
    }

    private void addCreators(List<Person> creators) {
        if (creators != null) {
            for (Person creator : creators) {
                metadata.addCreator(new CreatorContributor(creator.getFirstName(), creator.getLastName(), creator.getRoles()));
            }
        }
    }
    
     private void addContributors(List<Person> contributors) {
        if (contributors != null) {
            for (Person contributor : contributors) {
                metadata.addContributor(new CreatorContributor(contributor.getFirstName(), contributor.getLastName(), contributor.getRoles()));
            }
        }
    }

    private void createSections(List<Section> toc, TOCReference parentSection) throws IOException {
        for (Section section : toc) {
            Resource resource;
             
            String sectionResource = section.getResource();
            
            if (sectionResource.contains("#")) {
                sectionResource = sectionResource.substring(0, sectionResource.indexOf("#"));
            }
            
            
            try (FileInputStream fis = new FileInputStream(new File(unzippedPath, sectionResource))) {
                //resource = new Resource(fis, section.getResource());
                resource = new Resource(fis, sectionResource, section.getResource());
            }

            TOCReference bookSection;
            if (parentSection == null) {
                bookSection = book.addSection(section.getTitle(), resource);
                //System.out.println(section.getTitle());
            } else {
                bookSection = book.addSection(parentSection, section.getTitle(), resource);
            }

            if (section.getSubsections() != null) {
                createSections(section.getSubsections(), bookSection);
            }
        }
    }

    private List<Section> createBestEffortTableOfContents(String parent) throws IOException {
        List<Section> sections = new ArrayList<>();
        File folder;

        if (parent == null || parent.equals("")) {
            folder = unzippedPath;
        } else {
            folder = new File(unzippedPath, parent);
        }

        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            Arrays.sort(listOfFiles, (file1, file2) -> file1.getName().compareTo(file2.getName()));

            for (File listOfFile : listOfFiles) {
                if (listOfFile.isFile() && (listOfFile.getName().endsWith(".html") || listOfFile.getName().endsWith(".xhtml"))) {
                    Section s;
                    if (parent == null || parent.equals("")) {
                        s = new Section(listOfFile.getName().substring(0, listOfFile.getName().lastIndexOf(".")), listOfFile.getName());
                    } else {
                        s = new Section(listOfFile.getName().substring(0, listOfFile.getName().lastIndexOf(".")), parent + File.separator + listOfFile.getName());
                    }

                    sections.add(s);
                } else if (listOfFile.isDirectory()) {
                    if (parent == null || parent.equals("")) {
                        sections.addAll(createBestEffortTableOfContents(listOfFile.getName()));
                    } else {
                        sections.addAll(createBestEffortTableOfContents(parent + File.separator + listOfFile.getName()));
                    }
                }
            }
        }
        
        return sections;
    }

    private void copyUnaddedFilesFromZipToEpub(String parent) throws IOException {
        File folder;

        if (parent == null || parent.equals("")) {
            folder = unzippedPath;
        } else {
            folder = new File(unzippedPath, parent);
        }

        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile() && !isFileAlreadyAdded(file)) {
                    if (parent == null || parent.equals("")) {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            book.addResource(new Resource(fis, file.getName()));
                        }
                    } else {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            book.addResource(new Resource(fis, parent + File.separator + file.getName()));
                        }
                    }
                } else if (file.isDirectory()) {
                    if (parent == null || parent.equals("")) {
                        copyUnaddedFilesFromZipToEpub(file.getName());
                    } else {
                        copyUnaddedFilesFromZipToEpub(parent + File.separator + file.getName());
                    }
                }
            }
        }
    }
    
    private boolean isFileAlreadyAdded(File file) {
        String name = file.getAbsolutePath().replace(unzippedPath + File.separator, "");

        return Section.hasSectionWithResource(ourMetadata.getTableOfContents(), name) || name.equals(ourMetadata.getCoverImage());
    }

    @Override
    public void onEnd(OutputStream out) throws IOException {
        book.setMetadata(metadata);
        epubWriter.write(book, out);
    }
}