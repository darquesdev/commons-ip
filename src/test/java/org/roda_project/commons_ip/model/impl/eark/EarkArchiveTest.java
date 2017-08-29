/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/commons-ip
 */
package org.roda_project.commons_ip.model.impl.eark;

import org.hamcrest.core.Is;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.roda_project.commons_ip.model.*;
import org.roda_project.commons_ip.model.MetadataType.MetadataTypeEnum;
import org.roda_project.commons_ip.model.ValidationEntry.LEVEL;
import org.roda_project.commons_ip.utils.IPException;
import org.roda_project.commons_ip.utils.METSEnums.CreatorType;
import org.roda_project.commons_ip.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for EARK Information Packages (SIP, AIP and DIP)
 */
public class EarkArchiveTest {

  private static final String REPRESENTATION_STATUS_NORMALIZED = "NORMALIZED";

  private static final Logger LOGGER = LoggerFactory.getLogger(EarkArchiveTest.class);

  private static Path tempFolder;

  @BeforeClass
  public static void setup() throws IOException {
    tempFolder = Files.createTempDirectory("temp");
  }

  @AfterClass
  public static void cleanup() throws Exception {
    Utils.deletePath(tempFolder);
  }

  @Test
  public void buildAndParseEARKSIP() throws IPException, ParseException, InterruptedException, JAXBException, IOException {
    LOGGER.info("Creating full E-ARK SIP");
    Path zipSIP = createFullEARKSIP();
    LOGGER.info("Done creating full E-ARK SIP");

    LOGGER.info("Parsing (and validating) full E-ARK SIP");
    parseAndValidateFullEARKSIP(zipSIP);
    LOGGER.info("Done parsing (and validating) full E-ARK SIP");

  }

  private Path createFullEARKSIP() throws IPException, InterruptedException, JAXBException, IOException {

    // 1) instantiate E-ARK SIP object
    SIP sip = new EARKSIP("SIP_1", IPContentType.getMIXED());
    sip.addCreatorSoftwareAgent("RODA Commons IP");

    // 1.1) set optional human-readable description
    sip.setDescription("A full E-ARK SIP");

    // 1.2) add descriptive metadata (SIP level)
    IPDescriptiveMetadata metadataDescriptiveDCArchive = new IPDescriptiveMetadata(
      new IPFile(generateArchiveDescriptiveMetadata()),
      new MetadataType(MetadataTypeEnum.DC), null);
    sip.addDescriptiveMetadata(metadataDescriptiveDCArchive);

    // 1.3) add preservation metadata (SIP level)
    IPMetadata metadataPreservation = new IPMetadata(
      new IPFile(generateArchivePreservationMetadata()));
    sip.addPreservationMetadata(metadataPreservation);


    // 1.3) add preservation metadata (SIP level)
    IPMetadata otherMetadata = new IPMetadata(
      new IPFile(generateArchivePreservationMetadata()));
    sip.addOtherMetadata(otherMetadata);

    // 1.5) add xml schema (SIP level)
    sip.addSchema(new IPFile(Paths.get("src/test/resources/eark/schema.xsd")));
    sip.addSchema(new IPFile(Paths.get("src/test/resources/data/Sip.xsd")));

    // 1.6) add documentation (SIP level)
    sip.addDocumentation(new IPFile(Paths.get("src/test/resources/eark/documentation.pdf")));

    // 1.9) add a representation (status will be set to the default value, i.e.,
    // ORIGINAL)
    IPRepresentation representation1 = new IPRepresentation("representation 1");
    sip.addRepresentation(representation1);

    // 1.9.1) add a file to the representation
    IPFile representationFile = new IPFile(Paths.get("src/test/resources/eark/documentation.pdf"));
    representationFile.setRenameTo("data_.pdf");
    representation1.addFile(representationFile);

    // 2) build SIP, providing an output directory
    Path zipSIP = sip.build(tempFolder);

    return zipSIP;
  }

    private Path generateArchivePreservationMetadata() {
        //return Paths.get("src/test/resources/data/Sip.xsd");
        return Paths.get("src/test/resources/data/ES_E03101201_2017_EXP_ELOY_4.xml");
    }

    private Path generateArchiveDescriptiveMetadata() throws JAXBException, IOException {

        SimpleDublinCoreMetadata simpleDc = new SimpleDublinCoreMetadata();
        simpleDc.setContributor("contributor archive test");
        simpleDc.setIdentifier("identifier archive test");
        simpleDc.setTitle("title archive test");
        simpleDc.setDescription("description archive test");

        JAXBContext jc = JAXBContext.newInstance(SimpleDublinCoreMetadata.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "../../dc.xsd");

        Path path = Files.createFile(Paths.get("archive-metadata-dc.xml"));
        marshaller.marshal(simpleDc, path.toFile());
        return path;
  }

  private void parseAndValidateFullEARKSIP(Path zipSIP) throws ParseException {

    // 1) invoke static method parse and that's it
    SIP earkSIP = EARKSIP.parse(zipSIP, tempFolder);

    // general assessment
    earkSIP.getValidationReport().getValidationEntries().stream().filter(e -> e.getLevel() == LEVEL.ERROR)
      .forEach(e -> LOGGER.error("Validation report entry: {}", e));
    Assert.assertTrue(earkSIP.getValidationReport().isValid());

    // assess # of representations
    List<IPRepresentation> representations = earkSIP.getRepresentations();
    Assert.assertThat(representations.size(), Is.is(1));

    // assess representations status
    Assert.assertThat(representations.get(0).getStatus().asString(),
      Is.is(RepresentationStatus.getORIGINAL().asString()));

    LOGGER.info("SIP with id '{}' parsed with success (valid? {})!", earkSIP.getId(),
      earkSIP.getValidationReport().isValid());
  }

}
