package uk.gov.hmcts.reform.fpl.service.docmosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.fpl.config.DocmosisConfiguration;
import uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisRequest;
import uk.gov.hmcts.reform.fpl.model.docmosis.DocmosisData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DocmosisDocumentGeneratorService {
    private final RestTemplate restTemplate;
    private final DocmosisConfiguration configuration;
    private final ObjectMapper mapper;

    public DocmosisDocument generateDocmosisDocument(DocmosisData templateData, DocmosisTemplates template) {
        return generateDocmosisDocument(templateData.toMap(mapper), template);
    }

    public List<DocmosisDocument> generateDocmosisDocuments(DocmosisData templateData, DocmosisTemplates template) {
        return generateDocmosisDocuments(templateData.toMap(mapper), template);
    }

    public DocmosisDocument generateDocmosisDocument(Map<String, Object> templateData, DocmosisTemplates template) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        DocmosisRequest requestBody = DocmosisRequest.builder()
            .templateName(template.getTemplate())
            .data(templateData)
            .outputFormat("pdf")
            .outputName("IGNORED")
            .accessKey(configuration.getAccessKey())
            .build();

        HttpEntity<DocmosisRequest> request = new HttpEntity<>(requestBody, headers);

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            byte[] response = restTemplate.exchange(configuration.getUrl() + "/rs/render",
                HttpMethod.POST, request, byte[].class).getBody();
            stopWatch.stop();

            log.debug("Time taken to generate pdf format document: {} ms", stopWatch.getTotalTimeMillis());
            return new DocmosisDocument(template.getDocumentTitle(), response);
        } catch (HttpClientErrorException.BadRequest ex) {
            log.error("Docmosis document generation failed" + ex.getResponseBodyAsString());
            throw ex;
        }

    }

    public List<DocmosisDocument> generateDocmosisDocuments(Map<String, Object> templateData, DocmosisTemplates template) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        DocmosisRequest requestBody = DocmosisRequest.builder()
            .templateName(template.getTemplate())
            .data(templateData)
            .outputFormat("pdf;doc")
            .outputName("IGNORED") //TODO: change file name
            .accessKey(configuration.getAccessKey())
            .build();

        HttpEntity<DocmosisRequest> request = new HttpEntity<>(requestBody, headers);

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            byte[] response = restTemplate.exchange(configuration.getUrl() + "/rs/render",
                HttpMethod.POST, request, byte[].class).getBody();
            stopWatch.stop();

            log.debug("Time taken to generate pdf and doc format documents: {} ms", stopWatch.getTotalTimeMillis());
            return unzipToDocmosisDocuments(response);
        } catch (HttpClientErrorException.BadRequest ex) {
            log.error("Docmosis document generation failed" + ex.getResponseBodyAsString());
            throw ex;
        }
    }

    private List<DocmosisDocument> unzipToDocmosisDocuments(byte[] response) {
        List<DocmosisDocument> documents = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(response))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                DocmosisDocument document = new DocmosisDocument(zipEntry.getName(), readFile(zipInputStream));
                documents.add(document);
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
        return documents;
    }

    private byte[] readFile(ZipInputStream zipInputStream) {
        final byte[] buffer = new byte[1024];
        int bytesRead;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            while ((bytesRead = zipInputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }

}
