/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.ws;

import com.bbs.bean.FingerprintFacadeLocal;
import com.bbs.bean.PersonFacadeLocal;
import com.bbs.bean.UserFacadeLocal;
import com.bbs.entity.Fingerprint;
import com.bbs.entity.FingerprintPK;
import com.bbs.entity.Person;
import com.bbs.entity.User;
import com.bbs.model.Match;
import com.bbs.model.TFingerprint;
import com.bbs.model.TPerson;
import com.bbs.neuro.NeuroManager;
import com.neurotec.biometrics.NTemplate;
import com.neurotec.biometrics.standards.BDIFStandard;
import com.neurotec.biometrics.standards.FMRecord;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

/**
 *
 * @author Usuario
 */
@WebService(serviceName = "BBS")
@Stateless()
public class BBS {

    @EJB
    private UserFacadeLocal ejbRefUser;
    @EJB
    private PersonFacadeLocal ejbRefPerson;
    @EJB
    private FingerprintFacadeLocal ejbRefFingerprint;// Add business logic below. (Right-click in editor and choose
    
    private static NeuroManager test = NeuroManager.getInstance();

    @WebMethod(operationName = "createPerson")
    @Oneway
    public void createPerson(@WebParam(name = "person") TPerson person) {
        ejbRefPerson.create(person.convert());
    }

    @WebMethod(operationName = "createPersonFP")
    @Oneway
    public void createPersonFP(@WebParam(name = "person") TPerson person, @WebParam(name = "fingerprintset") TFingerprint[] fingerprintset) {
        this.createPerson(person);
        try {
            this.putFingerprintSet(fingerprintset);
        } catch (Exception ex) {
            Logger.getLogger(BBS.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }

    @WebMethod(operationName = "findPerson")
    public TPerson findPerson(@WebParam(name = "id") Object id) {
        TPerson person = null;
        Person find = ejbRefPerson.find(id);
        if (find != null) {
            person = new TPerson(find);
        }
        return person;
    }
    @WebMethod(operationName = "findPersonBy")
    public List<TPerson> findPersonBy(@WebParam(name = "pin") String pin,@WebParam(name = "nombre1") String nombre1,@WebParam(name = "nombre2") String nombre2,@WebParam(name = "apellido1") String apellido1,@WebParam(name = "apellido2") String apellido2) {
        List<TPerson> result=new ArrayList<TPerson>();
        List<Person> find = ejbRefPerson.find(pin, nombre1, nombre2, apellido1, apellido2);
        for(Person p:find){
            result.add(new TPerson(p));
        }
        return result;
    }
    @WebMethod(operationName = "getTime")
    public Date getTime() {
        return new Date();
    }

    @WebMethod(operationName = "login")
    public boolean login(@WebParam(name = "user") String user,@WebParam(name = "password") String password) {
        List<User> find = ejbRefUser.find(user);
        if(find!=null && find.size()==1){
            return find.get(0).getPassword().equals(password);
        }else{
            Logger.getAnonymousLogger().info("User Not Found");
        }
        return false;
    }
    
    private static List<Person> allPerson=null;
    @WebMethod(operationName = "getPerson")
    public TPerson getPerson(){
        if(allPerson==null){
            allPerson = ejbRefPerson.findAll();
        }
        Random a=new Random();
        int nextInt = a.nextInt(allPerson.size());
        return new TPerson(allPerson.get(nextInt));
    }
    @WebMethod(operationName = "getMatch")
    public Match getMatch(){
        Match match=new Match();
        if(allPerson==null){
            allPerson = ejbRefPerson.findAll();
        }
        Random a=new Random();
        int nextInt = a.nextInt(allPerson.size());
        TPerson applicant=new TPerson(allPerson.get(nextInt));
         nextInt = a.nextInt(allPerson.size());
        TPerson candidate=new TPerson(allPerson.get(nextInt));
        TFingerprint template = this.getTemplate(candidate.getPin(),1);
        if(template!=null){
            Match[] authenticate = authenticate(applicant.getPin(), new TFingerprint[]{template});
            match=authenticate[0];
        }else{
            match.setPin(applicant.getPin());
            match.setResult(Match.MATCH_RESULT.NO_HIT);
            match.setScore(0);
        }
        
        return match;
    }

    @WebMethod(operationName = "deletePerson")
    public boolean deletePerson(@WebParam(name = "id") Object id) {
        
        Person person = ejbRefPerson.find(id);
        if (person != null) {
            ejbRefPerson.remove(person);
            TFingerprint[] templates = getTemplates(String.valueOf(id));
            for(TFingerprint tfp:templates){
                ejbRefFingerprint.remove(tfp.convert());
            }
            return true;
        }
        return false;

    }

    @WebMethod(operationName = "deleteAll")
    public boolean deleteAll() {
        List<Fingerprint> allFingerprints = ejbRefFingerprint.findAll();
        for (Fingerprint fp : allFingerprints) {
            ejbRefFingerprint.remove(fp);
        }
        List<Person> allPersons = ejbRefPerson.findAll();
        for (Person p : allPersons) {
            ejbRefPerson.remove(p);
        }
        return true;

    }

    @WebMethod(operationName = "countPerson")
    public int countPerson() {
        return ejbRefPerson.count();
    }

    @WebMethod(operationName = "countFingerprint")
    public int countFingerprint() {
        return ejbRefFingerprint.count();
    }

    @WebMethod(operationName = "putFingerprint")
    @Oneway
    public void putFingerprint(@WebParam(name = "fingerprint") TFingerprint fingerprint) {
        //Logger.getAnonymousLogger().info("Fingerprint:"+fingerprint.getPin()+" - "+fingerprint.getType()+" ["+fingerprint.getBinary().length()+"]");
        Fingerprint convert = fingerprint.convert();
        ejbRefFingerprint.create(convert);
    }

    @WebMethod(operationName = "putFingerprintSet")
    @Oneway
    public void putFingerprintSet(@WebParam(name = "fingerprintset") TFingerprint[] fingerprintset) {
        //Logger.getAnonymousLogger().info("Fingerprint:"+fingerprint.getPin()+" - "+fingerprint.getType()+" ["+fingerprint.getBinary().length()+"]");
        for (TFingerprint tfp : fingerprintset) {
            Fingerprint convert = tfp.convert();
            ejbRefFingerprint.create(convert);
        }
    }

    @WebMethod(operationName = "getTemplates")
    public TFingerprint[] getTemplates(@WebParam(name = "pin") String pin) {

        TFingerprint[] fingerprintSet = null;
        ArrayList<TFingerprint> list = new ArrayList<TFingerprint>();
        for (int i = 1; i <= 10; i++) {
            TFingerprint template = this.getTemplate(pin, i);
            if (template != null) {
                list.add(template);
            }
        }
        fingerprintSet = new TFingerprint[list.size()];
        fingerprintSet = list.toArray(fingerprintSet);


        return fingerprintSet;
    }

    @WebMethod(operationName = "getTemplate")
    public TFingerprint getTemplate(@WebParam(name = "pin") String pin, int id) {
        TFingerprint fingerprint = null;
        FingerprintPK fpk = new FingerprintPK();
        fpk.setPin(pin);
        fpk.setId(id);
        Fingerprint find = ejbRefFingerprint.find(fpk);
        if (find != null) {
            fingerprint = new TFingerprint(find);
        }
        return fingerprint;
    }

    @WebMethod(operationName = "authenticateOneOne")
    public synchronized Match authenticateOneOne(@WebParam(name = "candidate") TFingerprint candidate, @WebParam(name = "applicant") TFingerprint applicant) {
        Match match = new Match();
        NeuroManager manager = NeuroManager.getInstance();

        System.out.println("Applicant Size:" + applicant.getTemplate().length);
        System.out.println("Candidate Size:" + candidate.getTemplate().length);

        FMRecord applicantRecord = new FMRecord(ByteBuffer.wrap(applicant.getTemplate()), BDIFStandard.ISO);
        NTemplate tApplicant = applicantRecord.toNTemplate();


        /*NFRecord record=new NFRecord(ByteBuffer.wrap(candidate.getTemplate()));
         FingerRecord fingerRecord=new FingerRecord(record);
         NTemplate tCandidate=new NTemplate();
         fingerRecord.addToTemplate(tCandidate);
         */

        int verify = manager.verify(tApplicant, tApplicant);
        match.setScore(verify);
        match.setResult(verify >= manager.getSettings().getMatchingThreshold() ? Match.MATCH_RESULT.HIT : Match.MATCH_RESULT.NO_HIT);
        return match;
    }

    @WebMethod(operationName = "authenticate")
    public synchronized Match[] authenticate(@WebParam(name = "pin") String pin, @WebParam(name = "fingerprintset") TFingerprint[] fingerprintset) {
        Match[] result = new Match[fingerprintset.length];
        NeuroManager manager = NeuroManager.getInstance();
        ArrayList<Match> list = new ArrayList<Match>();
        TFingerprint[] templates = getTemplates(pin);
        HashMap<Integer, TFingerprint> mapCandidate = new HashMap<Integer, TFingerprint>();
        for (TFingerprint tfp : templates) {
            mapCandidate.put(tfp.getType(), tfp);
        }
        for (TFingerprint applicant : fingerprintset) {
            Match match = new Match();
            match.setApplicant(applicant);
            match.setPin(pin);
            TFingerprint candidate = mapCandidate.get(applicant.getType());
            if (candidate != null) {
                try {
                    match.setCandidate(candidate);
                    int verify = manager.verify(applicant.getTemplate(), candidate.getTemplate(), applicant.getFormat().equalsIgnoreCase("ISO_19794") ? BDIFStandard.ISO : BDIFStandard.ANSI);
                    match.setScore(verify);
                    match.setResult(verify >= manager.getSettings().getMatchingThreshold() ? Match.MATCH_RESULT.HIT : Match.MATCH_RESULT.NO_HIT);
                } catch (Exception e) {
                    Logger.getAnonymousLogger().info(e.getMessage());
                    match.setResult(Match.MATCH_RESULT.UNKNOW);
                }
            } else {
                match.setResult(Match.MATCH_RESULT.NOT_FOUND);
            }
            list.add(match);
        }
        result = list.toArray(result);
        return result;
    }
}
