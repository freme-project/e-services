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
package eu.freme.eservices.publishing;

import eu.freme.eservices.publishing.exception.EPubCreationException;
import eu.freme.eservices.publishing.exception.InvalidZipException;
import eu.freme.eservices.publishing.exception.MissingMetadataException;
import eu.freme.eservices.publishing.webservice.Metadata;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

/**
 *
 * @author Pieter Heyvaert <pheyvaer.heyvaert@ugent.be>
 */
public class EPublishingService {

    private static final File tempFolderPath = getTempFolder();
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(EPublishingService.class);

	/**
     * Creates an EPUB3 file from the input stream.
     * @param metadata  Some extra meta mockup-endpoint-data
     * @param in        The input. It is supposed to be a zip file. The caller of this method is responsible for closing the stream!
     * @return          The EPUB. It is the binary contents of a zipped EPUB-3 file.
     * @throws InvalidZipException
     * @throws EPubCreationException
     * @throws IOException
     * @throws MissingMetadataException
	 */
    public byte[] createEPUB(Metadata metadata, InputStream in) throws InvalidZipException, EPubCreationException, IOException, MissingMetadataException {
        // initialize the class that parses the input, and passes mockup-endpoint-data to the EPUB creator
        File unzippedPath = new File(tempFolderPath, "freme_epublishing_" + System.currentTimeMillis());
        FileUtils.forceDeleteOnExit(unzippedPath);

        try (ZipInputStream zin = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            Unzipper.unzip(zin, unzippedPath);

            EPubCreator creator = new EPubCreatorImpl(metadata, unzippedPath);

            // write the EPUB "file", in this case to bytes
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                creator.onEnd(bos);
                return bos.toByteArray();
            }
        } catch (IOException ex) {
            Logger.getLogger(EPublishingService.class.getName()).log(Level.SEVERE, null, ex);
            throw new InvalidZipException("Something went wrong with the provided Zip file. Make sure you are providing a valid Zip file.");
        } finally {
            FileUtils.deleteDirectory(unzippedPath);
        }
    }

    private static File getTempFolder() {
        File tmpFolder = new File(System.getProperty("java.io.tmpdir", "/tmp"));
        if (!testTmp(tmpFolder)) {
            logger.warn("Not enough permissions on default temporary folder " + tmpFolder + "!! Trying current working dir...");    // replace with logger
            // try the current working dir
            File workingDir = new File(System.getProperty("user.dir", "/tmp"));
            if (!testTmp(workingDir)) {
                logger.warn("Not enough permissions current working directory " + workingDir + "!! Temporary files will be written in " + tmpFolder + " and will exist until manually deleted !!");    // replace with logger
            } else {    // OK, make 'workingDir' the temporary folder
                tmpFolder = workingDir;
            }
        }
        return tmpFolder;
    }

    private static boolean testTmp(final File tmpFolder) {
        return (tmpFolder.exists() && tmpFolder.isDirectory() && tmpFolder.canWrite() && tmpFolder.canExecute());
    }
}
