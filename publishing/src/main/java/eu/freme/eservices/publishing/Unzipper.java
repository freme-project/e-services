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

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author Pieter Heyvaert <pheyvaer.heyvaert@ugent.be>
 */
public class Unzipper {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(EPublishingService.class);

    public static void unzip(ZipInputStream zis, File outputFolder) throws IOException {
        //create output directory is not exists

        if (!outputFolder.exists() && !outputFolder.mkdirs()) {
            throw new IOException("Cannot create directory " + outputFolder);
        }

        //get the zipped file list entry
        ZipEntry ze = zis.getNextEntry();

        while (ze != null) {
            String fileName = ze.getName();
            File newFile = new File(outputFolder, fileName);

            logger.debug("file unzip : " + newFile.getAbsoluteFile());

            //create all non exists folders
            //else you will hit FileNotFoundException for compressed folder
            File parentDir = newFile.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                throw new IOException("Cannot create directory " + newFile.getParent());
            }

            if (ze.isDirectory()) {
                newFile.mkdirs();
            } else {
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    IOUtils.copyLarge(zis, fos);
                }
            }
            ze = zis.getNextEntry();
        }
        zis.closeEntry();
    }
}
