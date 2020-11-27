package uk.gov.hmcts.reform.fpl.service.validators;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fpl.model.ApplicationDocument;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.common.Element;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.springframework.util.ObjectUtils.isEmpty;

@Service
public class ApplicationDocumentChecker implements EventChecker {

    @Override
    public List<String> validate(CaseData caseData) {
        return emptyList();
    }

    @Override
    public boolean isStarted(CaseData caseData) {
        final List<Element<ApplicationDocument>> applicationDocuments = caseData.getDocuments();
        final String documentsToFollow = caseData.getDocumentsToFollow();

        if (!isEmpty(documentsToFollow) || !isEmpty(applicationDocuments)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isCompleted(CaseData caseData) {
        return false;
    }
}
