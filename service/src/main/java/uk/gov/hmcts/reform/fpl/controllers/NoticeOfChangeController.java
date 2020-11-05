package uk.gov.hmcts.reform.fpl.controllers;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.CaseAccessDataStoreApi;
import uk.gov.hmcts.reform.ccd.client.CaseUserApi;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.CaseUser;
import uk.gov.hmcts.reform.fpl.config.SystemUpdateUserConfiguration;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.Respondent;
import uk.gov.hmcts.reform.fpl.request.RequestData;
import uk.gov.hmcts.reform.fpl.utils.CaseDetailsMap;
import uk.gov.hmcts.reform.fpl.utils.ElementUtils;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Api
@RestController
@RequestMapping("/callback/notice-of-change")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NoticeOfChangeController extends CallbackController {


    private final IdamClient idamClient;
    private final IdamClient idam;
    private final CaseUserApi caseUser;
    private final CaseAccessDataStoreApi caseAccessDataStoreApi;
    private final AuthTokenGenerator authTokenGenerator;
    private final SystemUpdateUserConfiguration userConfig;
    private final RequestData requestData;
    private final CoreCaseDataApi coreCaseDataApi;


    @PostMapping("/about-to-start")
    public AboutToStartOrSubmitCallbackResponse handleAboutToStart(@RequestBody CallbackRequest callbackrequest) {
        CaseDetails caseDetails = callbackrequest.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);
        UserInfo userDetails = idamClient.getUserInfo(requestData.authorisation());
        caseDetails.getData().put("currentUserText", userDetails.getName());

        caseDetails.getData().remove("firstNameQuestion");
        caseDetails.getData().remove("lastNameQuestion");
        caseDetails.getData().remove("postcodeQuestion");

        return respond(caseDetails);
    }

    @PostMapping("/mid-event")
    public AboutToStartOrSubmitCallbackResponse handleMidEvent(@RequestBody CallbackRequest callbackrequest) {
        CaseDetails caseDetails = callbackrequest.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);

        String caseId = caseDetails.getData().get("caseIdQuestion").toString();
        String firstName = caseDetails.getData().get("firstNameQuestion").toString();
        String lastName = caseDetails.getData().get("lastNameQuestion").toString();
        String postcode = caseDetails.getData().get("postcodeQuestion").toString();

        final String userToken = idam.getAccessToken(userConfig.getUserName(), userConfig.getPassword());
        final String serviceToken = authTokenGenerator.generate();

        CaseDetails xxx = coreCaseDataApi.getCase(userToken, serviceToken, caseId);
        CaseData caseDataXXX = getCaseData(xxx);

        List<String> roles = new ArrayList<>();
        List<Respondent> respondents = new ArrayList<>();

        if (represents(firstName, lastName, postcode, caseDataXXX.getMother())) {
            roles.add("MOTHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getMother());
        }

        if (represents(firstName, lastName, postcode, caseDataXXX.getFather())) {
            roles.add("FATHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getFather());
        }





        if (represents(firstName, lastName, postcode, caseDataXXX.getChild1Father())) {
            roles.add("CHILD_ONE_FATHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getChild1Father());
        }

        if (represents(firstName, lastName, postcode, caseDataXXX.getChild2Father())) {
            roles.add("CHILD_TWO_FATHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getChild2Father());
        }

        if (represents(firstName, lastName, postcode, caseDataXXX.getChild3Father())) {
            roles.add("CHILD_THREE_FATHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getChild3Father());
        }





        if (represents(firstName, lastName, postcode, caseDataXXX.getMaternalGrandfather())) {
            roles.add("MATERNAL_GRANDFATHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getMaternalGrandfather());
        }

        if (represents(firstName, lastName, postcode, caseDataXXX.getMaternalGrandmother())) {
            roles.add("MATERNAL_GRANDMOTHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getMaternalGrandmother());
        }

        if (represents(firstName, lastName, postcode, caseDataXXX.getFraternalGrandfather())) {
            roles.add("FRATERNAL_GRANDFATHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getFraternalGrandfather());
        }

        if (represents(firstName, lastName, postcode, caseDataXXX.getFraternalGrandmother())) {
            roles.add("FRATERNAL_GRANDMOTHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getFraternalGrandmother());
        }

        caseDetails.getData().put("matchedRespondents", ElementUtils.wrapElements(respondents));

        return respond(caseDetails);
    }

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        CaseData caseData = getCaseData(caseDetails);
        CaseDetailsMap caseDetailsMap = CaseDetailsMap.caseDetailsMap(caseDetails);


        final String userToken = idam.getAccessToken(userConfig.getUserName(), userConfig.getPassword());
        final String serviceToken = authTokenGenerator.generate();

        String caseId = caseDetails.getData().get("caseIdQuestion").toString();


        CaseDetails xxx = coreCaseDataApi.getCase(userToken, serviceToken, caseId);
        CaseData caseDataXXX = getCaseData(xxx);

        String firstName = caseDetails.getData().get("firstNameQuestion").toString();
        String lastName = caseDetails.getData().get("lastNameQuestion").toString();
        String postcode = caseDetails.getData().get("postcodeQuestion").toString();

        List<String> roles = new ArrayList<>();
        List<Respondent> respondents = new ArrayList<>();

        if (represents(firstName, lastName, postcode, caseDataXXX.getMother())) {
            roles.add("MOTHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getMother());
        }

        if (represents(firstName, lastName, postcode, caseDataXXX.getFather())) {
            roles.add("FATHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getFather());
        }

        if (represents(firstName, lastName, postcode, caseDataXXX.getChild1Father())) {
            roles.add("CHILD_ONE_FATHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getChild1Father());
        }

        if (represents(firstName, lastName, postcode, caseDataXXX.getChild2Father())) {
            roles.add("CHILD_TWO_FATHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getChild2Father());
        }

        if (represents(firstName, lastName, postcode, caseDataXXX.getChild3Father())) {
            roles.add("CHILD_THREE_FATHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getChild3Father());
        }

        if (represents(firstName, lastName, postcode, caseDataXXX.getMaternalGrandfather())) {
            roles.add("MATERNAL_GRANDFATHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getMaternalGrandfather());
        }

        if (represents(firstName, lastName, postcode, caseDataXXX.getMaternalGrandmother())) {
            roles.add("MATERNAL_GRANDMOTHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getMaternalGrandmother());
        }

        if (represents(firstName, lastName, postcode, caseDataXXX.getFraternalGrandfather())) {
            roles.add("FRATERNAL_GRANDFATHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getFraternalGrandfather());
        }

        if (represents(firstName, lastName, postcode, caseDataXXX.getFraternalGrandmother())) {
            roles.add("FRATERNAL_GRANDMOTHER_REPRESENTATIVE");
            respondents.add(caseDataXXX.getFraternalGrandmother());
        }

        //CHILD1FATHERREPRESENTATIVE

        final Set<String> caseRoles = roles.stream()
            .map(role -> String.format("[%s]", role.replace("_","")))
            .collect(toSet());

        caseUser.updateCaseRolesForUser(userToken, serviceToken, xxx.getId().toString(), requestData.userId(), new CaseUser(requestData.userId(), caseRoles));

        caseDetails.getData().remove("firstNameQuestion");
        caseDetails.getData().remove("lastNameQuestion");
        caseDetails.getData().remove("postcodeQuestion");

        return respond(caseDetailsMap);
    }


    private boolean represents(String firstName, String lastName, String postcode, Respondent respondent) {
        return Optional.ofNullable(respondent)
            .map(mother -> mother.getParty())
            .filter(party -> firstName.equalsIgnoreCase(party.getFirstName())
                && lastName.equalsIgnoreCase(party.getLastName())
                && postcode.equalsIgnoreCase(party.getAddress().getPostcode())
            ).isPresent();
    }
}
