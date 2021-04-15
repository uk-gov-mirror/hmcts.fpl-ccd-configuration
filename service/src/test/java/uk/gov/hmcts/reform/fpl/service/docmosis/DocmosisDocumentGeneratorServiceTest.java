package uk.gov.hmcts.reform.fpl.service.docmosis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.fpl.config.DocmosisConfiguration;
import uk.gov.hmcts.reform.fpl.enums.docmosis.RenderFormat;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisRequest;
import uk.gov.hmcts.reform.fpl.model.docmosis.DocmosisData;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates.C6;
import static uk.gov.hmcts.reform.fpl.enums.docmosis.RenderFormat.PDF;
import static uk.gov.hmcts.reform.fpl.enums.docmosis.RenderFormat.WORD;

@ExtendWith(MockitoExtension.class)
class DocmosisDocumentGeneratorServiceTest {
    private static final byte[] RESPONSE_BODY = new byte[] {1, 2, 3};

    @Mock
    private DocmosisData data;

    @Mock
    private Map<String, Object> parameters;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ResponseEntity<byte[]> tornadoResponse;

    @Mock
    private DocmosisConfiguration configuration;

    @Mock
    private ObjectMapper mapper;

    @Captor
    private ArgumentCaptor<HttpEntity<DocmosisRequest>> requestCaptor;

    private DocmosisDocumentGeneratorService underTest;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        underTest = new  DocmosisDocumentGeneratorService(restTemplate, configuration, mapper);

        when(restTemplate.exchange(eq(configuration.getUrl() + "/rs/render"),
            eq(HttpMethod.POST), requestCaptor.capture(), eq(byte[].class))).thenReturn(tornadoResponse);

        when(tornadoResponse.getBody()).thenReturn(RESPONSE_BODY);
    }

    @Test
    void shouldInvokeTornadoForPDF() {
        DocmosisDocument docmosisDocument = underTest.generateDocmosisDocument(parameters, C6, PDF);

        HttpEntity<DocmosisRequest> request = requestCaptor.getValue();

        assertThat(docmosisDocument.getBytes()).isEqualTo(RESPONSE_BODY);
        assertThat(request.getBody().getTemplateName()).isEqualTo(C6.getTemplate());
        assertThat(request.getBody().getOutputFormat()).isEqualTo("pdf");
        assertThat(request.getBody().getData()).isEqualTo(parameters);
    }

    @Test
    void shouldInvokeTornadoForWord() {
        DocmosisDocument docmosisDocument = underTest.generateDocmosisDocument(parameters, C6, WORD);

        HttpEntity<DocmosisRequest> request = requestCaptor.getValue();

        assertThat(docmosisDocument.getBytes()).isEqualTo(RESPONSE_BODY);
        assertThat(request.getBody().getTemplateName()).isEqualTo(C6.getTemplate());
        assertThat(request.getBody().getOutputFormat()).isEqualTo("doc");
        assertThat(request.getBody().getData()).isEqualTo(parameters);
    }

    @Test
    void shouldConvertDocmosisDataIntoMap() {
        when(data.toMap(mapper)).thenReturn(parameters);

        underTest.generateDocmosisDocument(data, C6);

        HttpEntity<DocmosisRequest> request = requestCaptor.getValue();

        assertThat(request.getBody().getData()).isEqualTo(parameters);
    }
}

