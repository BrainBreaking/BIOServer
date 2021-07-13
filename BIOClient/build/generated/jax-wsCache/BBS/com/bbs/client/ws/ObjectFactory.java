
package com.bbs.client.ws;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.bbs.client.ws package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _CreatePerson_QNAME = new QName("http://ws.bbs.com/", "createPerson");
    private final static QName _Login_QNAME = new QName("http://ws.bbs.com/", "login");
    private final static QName _FindPersonBy_QNAME = new QName("http://ws.bbs.com/", "findPersonBy");
    private final static QName _GetMatch_QNAME = new QName("http://ws.bbs.com/", "getMatch");
    private final static QName _Authenticate_QNAME = new QName("http://ws.bbs.com/", "authenticate");
    private final static QName _DeleteAll_QNAME = new QName("http://ws.bbs.com/", "deleteAll");
    private final static QName _GetPerson_QNAME = new QName("http://ws.bbs.com/", "getPerson");
    private final static QName _FindPersonByResponse_QNAME = new QName("http://ws.bbs.com/", "findPersonByResponse");
    private final static QName _FindPerson_QNAME = new QName("http://ws.bbs.com/", "findPerson");
    private final static QName _CountFingerprint_QNAME = new QName("http://ws.bbs.com/", "countFingerprint");
    private final static QName _GetTemplatesResponse_QNAME = new QName("http://ws.bbs.com/", "getTemplatesResponse");
    private final static QName _DeleteAllResponse_QNAME = new QName("http://ws.bbs.com/", "deleteAllResponse");
    private final static QName _GetTemplates_QNAME = new QName("http://ws.bbs.com/", "getTemplates");
    private final static QName _GetPersonResponse_QNAME = new QName("http://ws.bbs.com/", "getPersonResponse");
    private final static QName _GetTemplateResponse_QNAME = new QName("http://ws.bbs.com/", "getTemplateResponse");
    private final static QName _AuthenticateResponse_QNAME = new QName("http://ws.bbs.com/", "authenticateResponse");
    private final static QName _GetTime_QNAME = new QName("http://ws.bbs.com/", "getTime");
    private final static QName _GetMatchResponse_QNAME = new QName("http://ws.bbs.com/", "getMatchResponse");
    private final static QName _GetTimeResponse_QNAME = new QName("http://ws.bbs.com/", "getTimeResponse");
    private final static QName _CountPersonResponse_QNAME = new QName("http://ws.bbs.com/", "countPersonResponse");
    private final static QName _DeletePersonResponse_QNAME = new QName("http://ws.bbs.com/", "deletePersonResponse");
    private final static QName _CountFingerprintResponse_QNAME = new QName("http://ws.bbs.com/", "countFingerprintResponse");
    private final static QName _AuthenticateOneOne_QNAME = new QName("http://ws.bbs.com/", "authenticateOneOne");
    private final static QName _PutFingerprintSet_QNAME = new QName("http://ws.bbs.com/", "putFingerprintSet");
    private final static QName _CreatePersonFP_QNAME = new QName("http://ws.bbs.com/", "createPersonFP");
    private final static QName _GetTemplate_QNAME = new QName("http://ws.bbs.com/", "getTemplate");
    private final static QName _DeletePerson_QNAME = new QName("http://ws.bbs.com/", "deletePerson");
    private final static QName _PutFingerprint_QNAME = new QName("http://ws.bbs.com/", "putFingerprint");
    private final static QName _LoginResponse_QNAME = new QName("http://ws.bbs.com/", "loginResponse");
    private final static QName _CountPerson_QNAME = new QName("http://ws.bbs.com/", "countPerson");
    private final static QName _AuthenticateOneOneResponse_QNAME = new QName("http://ws.bbs.com/", "authenticateOneOneResponse");
    private final static QName _FindPersonResponse_QNAME = new QName("http://ws.bbs.com/", "findPersonResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.bbs.client.ws
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link CountFingerprint }
     * 
     */
    public CountFingerprint createCountFingerprint() {
        return new CountFingerprint();
    }

    /**
     * Create an instance of {@link FindPerson }
     * 
     */
    public FindPerson createFindPerson() {
        return new FindPerson();
    }

    /**
     * Create an instance of {@link FindPersonByResponse }
     * 
     */
    public FindPersonByResponse createFindPersonByResponse() {
        return new FindPersonByResponse();
    }

    /**
     * Create an instance of {@link GetPerson }
     * 
     */
    public GetPerson createGetPerson() {
        return new GetPerson();
    }

    /**
     * Create an instance of {@link Authenticate }
     * 
     */
    public Authenticate createAuthenticate() {
        return new Authenticate();
    }

    /**
     * Create an instance of {@link GetMatch }
     * 
     */
    public GetMatch createGetMatch() {
        return new GetMatch();
    }

    /**
     * Create an instance of {@link DeleteAll }
     * 
     */
    public DeleteAll createDeleteAll() {
        return new DeleteAll();
    }

    /**
     * Create an instance of {@link Login }
     * 
     */
    public Login createLogin() {
        return new Login();
    }

    /**
     * Create an instance of {@link CreatePerson }
     * 
     */
    public CreatePerson createCreatePerson() {
        return new CreatePerson();
    }

    /**
     * Create an instance of {@link FindPersonBy }
     * 
     */
    public FindPersonBy createFindPersonBy() {
        return new FindPersonBy();
    }

    /**
     * Create an instance of {@link PutFingerprint }
     * 
     */
    public PutFingerprint createPutFingerprint() {
        return new PutFingerprint();
    }

    /**
     * Create an instance of {@link DeletePerson }
     * 
     */
    public DeletePerson createDeletePerson() {
        return new DeletePerson();
    }

    /**
     * Create an instance of {@link GetTemplate }
     * 
     */
    public GetTemplate createGetTemplate() {
        return new GetTemplate();
    }

    /**
     * Create an instance of {@link CreatePersonFP }
     * 
     */
    public CreatePersonFP createCreatePersonFP() {
        return new CreatePersonFP();
    }

    /**
     * Create an instance of {@link LoginResponse }
     * 
     */
    public LoginResponse createLoginResponse() {
        return new LoginResponse();
    }

    /**
     * Create an instance of {@link CountPerson }
     * 
     */
    public CountPerson createCountPerson() {
        return new CountPerson();
    }

    /**
     * Create an instance of {@link AuthenticateOneOneResponse }
     * 
     */
    public AuthenticateOneOneResponse createAuthenticateOneOneResponse() {
        return new AuthenticateOneOneResponse();
    }

    /**
     * Create an instance of {@link FindPersonResponse }
     * 
     */
    public FindPersonResponse createFindPersonResponse() {
        return new FindPersonResponse();
    }

    /**
     * Create an instance of {@link CountFingerprintResponse }
     * 
     */
    public CountFingerprintResponse createCountFingerprintResponse() {
        return new CountFingerprintResponse();
    }

    /**
     * Create an instance of {@link PutFingerprintSet }
     * 
     */
    public PutFingerprintSet createPutFingerprintSet() {
        return new PutFingerprintSet();
    }

    /**
     * Create an instance of {@link AuthenticateOneOne }
     * 
     */
    public AuthenticateOneOne createAuthenticateOneOne() {
        return new AuthenticateOneOne();
    }

    /**
     * Create an instance of {@link GetTimeResponse }
     * 
     */
    public GetTimeResponse createGetTimeResponse() {
        return new GetTimeResponse();
    }

    /**
     * Create an instance of {@link CountPersonResponse }
     * 
     */
    public CountPersonResponse createCountPersonResponse() {
        return new CountPersonResponse();
    }

    /**
     * Create an instance of {@link GetMatchResponse }
     * 
     */
    public GetMatchResponse createGetMatchResponse() {
        return new GetMatchResponse();
    }

    /**
     * Create an instance of {@link GetTime }
     * 
     */
    public GetTime createGetTime() {
        return new GetTime();
    }

    /**
     * Create an instance of {@link DeletePersonResponse }
     * 
     */
    public DeletePersonResponse createDeletePersonResponse() {
        return new DeletePersonResponse();
    }

    /**
     * Create an instance of {@link DeleteAllResponse }
     * 
     */
    public DeleteAllResponse createDeleteAllResponse() {
        return new DeleteAllResponse();
    }

    /**
     * Create an instance of {@link GetTemplatesResponse }
     * 
     */
    public GetTemplatesResponse createGetTemplatesResponse() {
        return new GetTemplatesResponse();
    }

    /**
     * Create an instance of {@link AuthenticateResponse }
     * 
     */
    public AuthenticateResponse createAuthenticateResponse() {
        return new AuthenticateResponse();
    }

    /**
     * Create an instance of {@link GetTemplateResponse }
     * 
     */
    public GetTemplateResponse createGetTemplateResponse() {
        return new GetTemplateResponse();
    }

    /**
     * Create an instance of {@link GetPersonResponse }
     * 
     */
    public GetPersonResponse createGetPersonResponse() {
        return new GetPersonResponse();
    }

    /**
     * Create an instance of {@link GetTemplates }
     * 
     */
    public GetTemplates createGetTemplates() {
        return new GetTemplates();
    }

    /**
     * Create an instance of {@link TFingerprint }
     * 
     */
    public TFingerprint createTFingerprint() {
        return new TFingerprint();
    }

    /**
     * Create an instance of {@link TPerson }
     * 
     */
    public TPerson createTPerson() {
        return new TPerson();
    }

    /**
     * Create an instance of {@link Match }
     * 
     */
    public Match createMatch() {
        return new Match();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreatePerson }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "createPerson")
    public JAXBElement<CreatePerson> createCreatePerson(CreatePerson value) {
        return new JAXBElement<CreatePerson>(_CreatePerson_QNAME, CreatePerson.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Login }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "login")
    public JAXBElement<Login> createLogin(Login value) {
        return new JAXBElement<Login>(_Login_QNAME, Login.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FindPersonBy }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "findPersonBy")
    public JAXBElement<FindPersonBy> createFindPersonBy(FindPersonBy value) {
        return new JAXBElement<FindPersonBy>(_FindPersonBy_QNAME, FindPersonBy.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetMatch }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "getMatch")
    public JAXBElement<GetMatch> createGetMatch(GetMatch value) {
        return new JAXBElement<GetMatch>(_GetMatch_QNAME, GetMatch.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Authenticate }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "authenticate")
    public JAXBElement<Authenticate> createAuthenticate(Authenticate value) {
        return new JAXBElement<Authenticate>(_Authenticate_QNAME, Authenticate.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeleteAll }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "deleteAll")
    public JAXBElement<DeleteAll> createDeleteAll(DeleteAll value) {
        return new JAXBElement<DeleteAll>(_DeleteAll_QNAME, DeleteAll.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPerson }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "getPerson")
    public JAXBElement<GetPerson> createGetPerson(GetPerson value) {
        return new JAXBElement<GetPerson>(_GetPerson_QNAME, GetPerson.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FindPersonByResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "findPersonByResponse")
    public JAXBElement<FindPersonByResponse> createFindPersonByResponse(FindPersonByResponse value) {
        return new JAXBElement<FindPersonByResponse>(_FindPersonByResponse_QNAME, FindPersonByResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FindPerson }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "findPerson")
    public JAXBElement<FindPerson> createFindPerson(FindPerson value) {
        return new JAXBElement<FindPerson>(_FindPerson_QNAME, FindPerson.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CountFingerprint }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "countFingerprint")
    public JAXBElement<CountFingerprint> createCountFingerprint(CountFingerprint value) {
        return new JAXBElement<CountFingerprint>(_CountFingerprint_QNAME, CountFingerprint.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTemplatesResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "getTemplatesResponse")
    public JAXBElement<GetTemplatesResponse> createGetTemplatesResponse(GetTemplatesResponse value) {
        return new JAXBElement<GetTemplatesResponse>(_GetTemplatesResponse_QNAME, GetTemplatesResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeleteAllResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "deleteAllResponse")
    public JAXBElement<DeleteAllResponse> createDeleteAllResponse(DeleteAllResponse value) {
        return new JAXBElement<DeleteAllResponse>(_DeleteAllResponse_QNAME, DeleteAllResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTemplates }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "getTemplates")
    public JAXBElement<GetTemplates> createGetTemplates(GetTemplates value) {
        return new JAXBElement<GetTemplates>(_GetTemplates_QNAME, GetTemplates.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetPersonResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "getPersonResponse")
    public JAXBElement<GetPersonResponse> createGetPersonResponse(GetPersonResponse value) {
        return new JAXBElement<GetPersonResponse>(_GetPersonResponse_QNAME, GetPersonResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTemplateResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "getTemplateResponse")
    public JAXBElement<GetTemplateResponse> createGetTemplateResponse(GetTemplateResponse value) {
        return new JAXBElement<GetTemplateResponse>(_GetTemplateResponse_QNAME, GetTemplateResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AuthenticateResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "authenticateResponse")
    public JAXBElement<AuthenticateResponse> createAuthenticateResponse(AuthenticateResponse value) {
        return new JAXBElement<AuthenticateResponse>(_AuthenticateResponse_QNAME, AuthenticateResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTime }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "getTime")
    public JAXBElement<GetTime> createGetTime(GetTime value) {
        return new JAXBElement<GetTime>(_GetTime_QNAME, GetTime.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetMatchResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "getMatchResponse")
    public JAXBElement<GetMatchResponse> createGetMatchResponse(GetMatchResponse value) {
        return new JAXBElement<GetMatchResponse>(_GetMatchResponse_QNAME, GetMatchResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTimeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "getTimeResponse")
    public JAXBElement<GetTimeResponse> createGetTimeResponse(GetTimeResponse value) {
        return new JAXBElement<GetTimeResponse>(_GetTimeResponse_QNAME, GetTimeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CountPersonResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "countPersonResponse")
    public JAXBElement<CountPersonResponse> createCountPersonResponse(CountPersonResponse value) {
        return new JAXBElement<CountPersonResponse>(_CountPersonResponse_QNAME, CountPersonResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeletePersonResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "deletePersonResponse")
    public JAXBElement<DeletePersonResponse> createDeletePersonResponse(DeletePersonResponse value) {
        return new JAXBElement<DeletePersonResponse>(_DeletePersonResponse_QNAME, DeletePersonResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CountFingerprintResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "countFingerprintResponse")
    public JAXBElement<CountFingerprintResponse> createCountFingerprintResponse(CountFingerprintResponse value) {
        return new JAXBElement<CountFingerprintResponse>(_CountFingerprintResponse_QNAME, CountFingerprintResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AuthenticateOneOne }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "authenticateOneOne")
    public JAXBElement<AuthenticateOneOne> createAuthenticateOneOne(AuthenticateOneOne value) {
        return new JAXBElement<AuthenticateOneOne>(_AuthenticateOneOne_QNAME, AuthenticateOneOne.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PutFingerprintSet }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "putFingerprintSet")
    public JAXBElement<PutFingerprintSet> createPutFingerprintSet(PutFingerprintSet value) {
        return new JAXBElement<PutFingerprintSet>(_PutFingerprintSet_QNAME, PutFingerprintSet.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreatePersonFP }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "createPersonFP")
    public JAXBElement<CreatePersonFP> createCreatePersonFP(CreatePersonFP value) {
        return new JAXBElement<CreatePersonFP>(_CreatePersonFP_QNAME, CreatePersonFP.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTemplate }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "getTemplate")
    public JAXBElement<GetTemplate> createGetTemplate(GetTemplate value) {
        return new JAXBElement<GetTemplate>(_GetTemplate_QNAME, GetTemplate.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeletePerson }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "deletePerson")
    public JAXBElement<DeletePerson> createDeletePerson(DeletePerson value) {
        return new JAXBElement<DeletePerson>(_DeletePerson_QNAME, DeletePerson.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PutFingerprint }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "putFingerprint")
    public JAXBElement<PutFingerprint> createPutFingerprint(PutFingerprint value) {
        return new JAXBElement<PutFingerprint>(_PutFingerprint_QNAME, PutFingerprint.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LoginResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "loginResponse")
    public JAXBElement<LoginResponse> createLoginResponse(LoginResponse value) {
        return new JAXBElement<LoginResponse>(_LoginResponse_QNAME, LoginResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CountPerson }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "countPerson")
    public JAXBElement<CountPerson> createCountPerson(CountPerson value) {
        return new JAXBElement<CountPerson>(_CountPerson_QNAME, CountPerson.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AuthenticateOneOneResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "authenticateOneOneResponse")
    public JAXBElement<AuthenticateOneOneResponse> createAuthenticateOneOneResponse(AuthenticateOneOneResponse value) {
        return new JAXBElement<AuthenticateOneOneResponse>(_AuthenticateOneOneResponse_QNAME, AuthenticateOneOneResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FindPersonResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://ws.bbs.com/", name = "findPersonResponse")
    public JAXBElement<FindPersonResponse> createFindPersonResponse(FindPersonResponse value) {
        return new JAXBElement<FindPersonResponse>(_FindPersonResponse_QNAME, FindPersonResponse.class, null, value);
    }

}
