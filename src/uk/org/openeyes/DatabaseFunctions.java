/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.org.openeyes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import uk.org.openeyes.models.Episode;
import uk.org.openeyes.models.EtOphinbiometryIolRefValues;
import uk.org.openeyes.models.EtOphinbiometryMeasurement;
import uk.org.openeyes.models.Event;
import uk.org.openeyes.models.EventType;
import uk.org.openeyes.models.Eye;
import uk.org.openeyes.models.OphinbiometryCalculationFormula;
import uk.org.openeyes.models.OphinbiometryLenstypeLens;
import uk.org.openeyes.models.Patient;
import uk.org.openeyes.models.User;

/**
 *
 * @author VEDELEKT
 */
public class DatabaseFunctions {
    private SessionFactory sessionFactory;
    private Patient selectedPatient;
    private Episode selectedEpisode;
    
    private User searchStudyUser(String userName, Session session){
        Criteria crit = session.createCriteria(User.class);
        Disjunction or = Restrictions.disjunction();
        
        User returnUser = null;
        
        String[] userNameArr = userName.split(" ");
        String lastName = "";
        
        // we cannot rely on the user name format coming from the dicom file!!!
        if(userNameArr.length == 1){
            lastName = "";
        }else{
            for(int i=1; i<userNameArr.length; i++){
                lastName += userNameArr[i]+" "; 
            }
        }
        crit.add(Restrictions.eq("firstName", userNameArr[0]));
        crit.add(Restrictions.eq("lastName", lastName));
        
        if(crit.list().isEmpty()){
            // we search for unknown user
            Criteria crit2 = session.createCriteria(User.class);
            crit2.add(Restrictions.eq("firstName", "Unknown"));
            crit2.add(Restrictions.eq("lastName", "IOLMaster"));
            
            if(crit2.list().isEmpty()){
                // we create the user
                returnUser = new User();
                returnUser.setUsername("UNKNOWN");
                returnUser.setFirstName("Unknown");
                returnUser.setLastName("IOLMaster");
                returnUser.setEmail("");
                returnUser.setActive(true);
                returnUser.setGlobalFirmRights(false);
                returnUser.setTitle("N/A");
                returnUser.setQualifications("Generated by IOLMaster import");
                returnUser.setRole("Import");
                returnUser.setLastModifiedUserId(new User(1));
                returnUser.setLastModifiedDate(new Date());
                returnUser.setCreatedUserId(new User(1));
                returnUser.setCreatedDate(new Date());
                returnUser.setIsClinical(false);
                returnUser.setIsDoctor(false);
                returnUser.setIsConsultant(false);
                returnUser.setIsSurgeon(false);
                returnUser.setHasSelectedFirms(false);
                session.save(returnUser);
            }else{
                returnUser = (User) crit2.list().get(0);
            }
            
        }else{
            returnUser = (User) crit.list().get(0);
        }
        // for now we return static user admin
        return returnUser;
    }
    
    private OphinbiometryLenstypeLens searchForLensData(String lensName, Double aConst, Session session){
        OphinbiometryLenstypeLens lensType = null;
        Criteria crit = session.createCriteria(OphinbiometryLenstypeLens.class);
        
        //we search for the full name first
        crit.add(Restrictions.eq("name",lensName));
        if(!crit.list().isEmpty()){
            if(crit.list().get(0) != null){
                lensType = (OphinbiometryLenstypeLens) crit.list().get(0);
            }
        }else{
            //we also try to search for other possibilities
            Criteria crit2 = session.createCriteria(OphinbiometryLenstypeLens.class);
            List<OphinbiometryLenstypeLens> currentLenses = crit2.list();
            for( OphinbiometryLenstypeLens lensData : currentLenses){
                for (int i = 0; i <= (lensName.length() - lensData.getName().length()); i++) {
                    if (lensName.regionMatches(i, lensData.getName(), 0, lensData.getName().length())) {
                        lensType = lensData;
                        break;
                    }
                }
            }
        }
        //if nothing found we create a new one
        if(lensType == null){
            lensType = new OphinbiometryLenstypeLens();
            lensType.setName(lensName);
            // TODO: we should extract A constant value for single lense format somehow!!!!
            lensType.setAcon(BigDecimal.valueOf(aConst));
            // TODO: we may need to add user name here!!
            User selectedUser = searchStudyUser("", session);
            lensType.setCreatedUserId(selectedUser);
            lensType.setLastModifiedUserId(selectedUser);
            lensType.setCreatedDate(new Date());
            lensType.setLastModifiedDate(new Date());
            lensType.setDescription("Created by IOL Master input!!! "+lensName);
            lensType.setDisplayOrder(0);
            lensType.setDeleted(false);
            lensType.setComments("Imported values, please check! Remove this comment when confirmed!");
            lensType.setPositionId(0);
            session.save(lensType);
        }
        return lensType;
    }
    
    private OphinbiometryCalculationFormula searchForFormulaData(String formulaName, Session session){
        OphinbiometryCalculationFormula formulaType = null;
        Criteria crit = session.createCriteria(OphinbiometryCalculationFormula.class);
        
        //we search for the full name first
        crit.add(Restrictions.eq("name",formulaName));
        if(!crit.list().isEmpty()){
            if(crit.list().get(0) != null){
                formulaType = (OphinbiometryCalculationFormula) crit.list().get(0);
            }
        }else{
            Criteria crit2 = session.createCriteria(OphinbiometryCalculationFormula.class);
            crit2.add(Restrictions.eq("name", formulaName.replace("®", "")));
            if(!crit2.list().isEmpty()){
                if(crit2.list().get(0) != null){
                    formulaType = (OphinbiometryCalculationFormula) crit2.list().get(0);
                }
            }
        }
        if(formulaType == null){
            // if formula not exists we create it
            formulaType = new OphinbiometryCalculationFormula();
            formulaType.setName(formulaName);
            // TODO: add proper user name here!!!
            User selectedUser = searchStudyUser("", session);
            formulaType.setCreatedUserId(selectedUser);
            formulaType.setLastModifiedUserId(selectedUser);
            formulaType.setCreatedDate(new Date());
            formulaType.setLastModifiedDate(new Date());
            formulaType.setDisplayOrder(0);
            formulaType.setDeleted(false);
            session.save(formulaType);
        }

        return formulaType;
    }
    
    public void initSessionFactory(String configFile){
        // A SessionFactory is set up once for an application!
        // if no config specified we should use the default one
        if(configFile.equals("")){
            configFile= "resources/hibernate.cfg.xml";
        }
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                        .configure(configFile) // configures settings from hibernate.cfg.xml
                        .build();
        try {
            sessionFactory = new MetadataSources( registry ).buildMetadata().buildSessionFactory();
        }
        catch (Exception e) {
            // The registry would be destroyed by the SessionFactory, but we had trouble building the SessionFactory
            // so destroy it manually.
            e.printStackTrace();
            StandardServiceRegistryBuilder.destroy( registry );
        }
    }
    
    public boolean checkConnection(){
        Session session = sessionFactory.openSession();
        return session.isConnected();
    }
    
    public void closeSessionFactory(){
        if ( sessionFactory != null ) {
            sessionFactory.close();
	}
    }
    
    public Patient getSelectedPatient(){
        return this.selectedPatient;
    }
    
    public void searchPatient(String hosNum, char gender, Calendar birthDate){
        Session session = sessionFactory.openSession();
        Criteria crit = session.createCriteria(Patient.class);
        crit.add(Restrictions.eq("hosNum",hosNum));
        // we should search for M or F only
        if( Character.toString(gender).equals("F") || Character.toString(gender).equals("M")){
            crit.add(Restrictions.eq("gender", Character.toString(gender)));
        }
        crit.add(Restrictions.sqlRestriction("dob = '"+birthDate.get(Calendar.YEAR)+"-"+birthDate.get(Calendar.MONTH)+"-"+birthDate.get(Calendar.DAY_OF_MONTH)+"'"));
        List patientList = crit.list();
        
        if(patientList.isEmpty()){
            // TODO: How to handle this case??
            System.out.println("ERROR: Patient not found for the data specified (hos_num: "+hosNum+", gender: "+gender+", dob: "+birthDate.get(Calendar.YEAR)+"-"+birthDate.get(Calendar.MONTH)+"-"+birthDate.get(Calendar.DAY_OF_MONTH)+")");
        }else if(patientList.size() > 1){
            // TODO: How to handle this case??
            System.out.println("ERROR: More than 1 record found for patient (hos_num: "+hosNum+", gender: "+gender+", dob: "+birthDate.get(Calendar.YEAR)+"-"+birthDate.get(Calendar.MONTH)+"-"+birthDate.get(Calendar.DAY_OF_MONTH)+")");
        }else{
            // TODO: is everything OK?
            selectedPatient = (Patient) patientList.get(0);
        }
        if(selectedPatient != null){
            System.out.println("Selected patient: "+selectedPatient);
        }
        session.close();
    }
    
    public void selectActiveEpisode(){
        if(this.selectedPatient != null){
            Session session = sessionFactory.openSession();
            Criteria episodeCrit = session.createCriteria(Episode.class);
            Criteria patientJoin = episodeCrit.createCriteria("patientId");

            patientJoin.add(Restrictions.eq("id", selectedPatient.getId()));
            episodeCrit.add(Restrictions.eq("deleted",0));
            Criteria episodeStatusJoin = episodeCrit.createCriteria("episodeStatusId");
            episodeStatusJoin.add(Restrictions.ne("name", "Discharged"));
            List episodesList = episodeCrit.list();

            if(episodesList.isEmpty()){
                System.out.println("ERROR: No open episodes found!");
            }else if(episodesList.size() != 1){
                System.out.println("ERROR: More than 1 open episodes found!");
            }else{
                selectedEpisode = (Episode) episodesList.get(0);
                System.out.println(selectedEpisode.toString());
            }
            
            session.close();
        }
        if(selectedEpisode == null){
            System.out.println("ERROR: No unique open episode found, will create data without episode!");
        }
    }
    
    public Episode getSelectedEpisode(){
        return this.selectedEpisode;        
    }
    
    // TODO: can be moved out to a specific biometry class
    public void createBiometryEvent(Calendar eventDate, StudyData IOLStudy, BiometryData IOLBiometry, Boolean withEpisode){
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        // TODO: need to be able to merge two different files into the same event!!
        // We need to check what is the unique key to reconise the same dataset!!
        // we have 2 fields in et_ophinbiometry_measurement for this - study_id, device_id
        // the studyInstanceId looks the better at this moment...
        // we need to put the logic of this here!!!!
        Criteria currentEvent = session.createCriteria(EtOphinbiometryMeasurement.class);
        
        currentEvent.add(Restrictions.eq("studyId", IOLStudy.getStudyInstanceID()));
        Integer currentEventId = 0;
        if(!currentEvent.list().isEmpty()){
            EtOphinbiometryMeasurement currentData = (EtOphinbiometryMeasurement) currentEvent.list().get(0);
            currentEventId = currentData.getEventId().getId();
        }
        
        Event newBiometryEvent;
        Boolean isEventNew = true;
                
        if(currentEventId > 0){
            newBiometryEvent = new Event(currentEventId);
            isEventNew = false;
        }else{
            newBiometryEvent = new Event();
        }
        
        // This is the input data
        BiometrySide storedBiometryDataLeft = IOLBiometry.getBiometryValue("L");
        BiometrySide storedBiometryDataRight = IOLBiometry.getBiometryValue("R");        
        // TODO: need to add more logic here to find the user!!! - see the searchStudyUser function
        // we also have an other variable called Phisycian name in the Study object! Which one we need to use?
        User selectedUser = searchStudyUser(IOLStudy.getSurgeonName(), session);
        
        // we need to create the new event and the measurement data if the event_id is not set
        if( isEventNew ){
            // just make sure that we have any episode selected
            if(withEpisode && this.selectedEpisode != null){
                newBiometryEvent.setEpisodeId(selectedEpisode);
            }else{
                newBiometryEvent.setEpisodeId(null);
            }
            newBiometryEvent.setCreatedUserId(selectedUser);
            // search for event type name "Biometry"
            Criteria eventTypeCrit = session.createCriteria(EventType.class);
            eventTypeCrit.add(Restrictions.eq("name", "Biometry"));
            newBiometryEvent.setEventTypeId((EventType) eventTypeCrit.list().get(0));
            newBiometryEvent.setCreatedDate(new Date());
            newBiometryEvent.setLastModifiedDate(new Date());
            newBiometryEvent.setLastModifiedUserId(selectedUser);

            // TODO: need to check, because it display one month more!!!!
            newBiometryEvent.setEventDate(eventDate.getTime());

            // let's save it!
            // 1. create new event
            session.save(newBiometryEvent);
     
            // 2. save basic measurement data (et_ophinbiometry_measurement)
            EtOphinbiometryMeasurement newBasicMeasurementData = new EtOphinbiometryMeasurement();
            newBasicMeasurementData.setEventId(newBiometryEvent);
            newBasicMeasurementData.setEyeId(new Eye(IOLBiometry.getEyeId()));

            if(storedBiometryDataLeft != null){
                newBasicMeasurementData.setK1Left(BigDecimal.valueOf(storedBiometryDataLeft.getK1()));
                newBasicMeasurementData.setK2Left(BigDecimal.valueOf(storedBiometryDataLeft.getK2()));
                newBasicMeasurementData.setAxisK1Left(BigDecimal.valueOf(storedBiometryDataLeft.getAxisK1()));
                newBasicMeasurementData.setAxialLengthLeft(BigDecimal.valueOf(storedBiometryDataLeft.getAL()));
                Double SNR = (Double) storedBiometryDataLeft.getSNR();
                newBasicMeasurementData.setSnrLeft(SNR.intValue());
            }

            if(storedBiometryDataRight != null){
                newBasicMeasurementData.setK1Right(BigDecimal.valueOf(storedBiometryDataRight.getK1()));
                newBasicMeasurementData.setK2Right(BigDecimal.valueOf(storedBiometryDataRight.getK2()));
                newBasicMeasurementData.setAxisK1Right(BigDecimal.valueOf(storedBiometryDataRight.getAxisK1()));
                newBasicMeasurementData.setAxialLengthRight(BigDecimal.valueOf(storedBiometryDataRight.getAL()));
                Double SNR = (Double) storedBiometryDataRight.getSNR();
                newBasicMeasurementData.setSnrRight(SNR.intValue());
            }

            newBasicMeasurementData.setCreatedDate(new Date());
            newBasicMeasurementData.setCreatedUserId(selectedUser);
            newBasicMeasurementData.setLastModifiedDate(new Date());
            newBasicMeasurementData.setLastModifiedUserId(selectedUser);

            // TODO: add proper values here!!
            // if we need study instance ID we also need to change the column type of study ID!!!
            newBasicMeasurementData.setStudyId(IOLStudy.getStudyInstanceID());
            newBasicMeasurementData.setDeviceId(IOLStudy.getStationName());

            session.save(newBasicMeasurementData);
        }
        // 3. save lens and formula specific data
        ArrayList<BiometryMeasurementData> storedBiometryMeasurementDataLeft = storedBiometryDataLeft.getMeasurements();
        ArrayList<BiometryMeasurementData> storedBiometryMeasurementDataRight = storedBiometryDataRight.getMeasurements();
        OphinbiometryLenstypeLens lensType = null;
        OphinbiometryCalculationFormula formulaType = null;

        // we need to merge left and right side data here, but we don't know which side contains proper data
        Integer maxRec = 0;

        if(storedBiometryMeasurementDataLeft.size() > storedBiometryMeasurementDataRight.size()){
            maxRec = storedBiometryMeasurementDataLeft.size();
        }else{
            maxRec = storedBiometryMeasurementDataRight.size();
        }

        for(Integer i = 0; i < maxRec; i++){
            BiometryMeasurementData BiometryMDataLeft = null;
            BiometryMeasurementData BiometryMDataRight = null;

            if(storedBiometryMeasurementDataLeft.size()> i){
                BiometryMDataLeft = storedBiometryMeasurementDataLeft.get(i);
                if(BiometryMDataLeft.getLenseName() != null && !BiometryMDataLeft.getLenseName().equals("")){
                    lensType = searchForLensData(BiometryMDataLeft.getLenseName(), BiometryMDataLeft.getAConst(), session);
                    //System.out.println(lensType);
                    formulaType = searchForFormulaData(IOLStudy.getFormulaName(), session);
                }else if(BiometryMDataLeft.getFormulaName() != null && !BiometryMDataLeft.getFormulaName().equals("")){
                    formulaType = searchForFormulaData(BiometryMDataLeft.getFormulaName(), session);
                    //System.out.println(formulaType);
                    // TODO: need to handle A const here for single lense format!!!
                    //lensType = searchForLensData(IOLStudy.getLenseName(), Double.parseDouble(IOLStudy.getLenseName().substring(4, 10)), session);
                    lensType = searchForLensData(IOLStudy.getLenseName(), 0.0, session);
                }
            }

            if(storedBiometryMeasurementDataRight.size()> i){
                BiometryMDataRight = storedBiometryMeasurementDataRight.get(i);
                if(lensType == null && formulaType == null && (BiometryMDataRight.getLenseName() != null && !BiometryMDataRight.getLenseName().equals(""))){
                    lensType = searchForLensData(BiometryMDataRight.getLenseName(), BiometryMDataRight.getAConst(), session);
                    //System.out.println(lensType);
                    formulaType = searchForFormulaData(IOLStudy.getFormulaName(), session);
                }else if(lensType == null && formulaType == null && (BiometryMDataRight.getFormulaName() != null && !BiometryMDataRight.getFormulaName().equals(""))){
                    formulaType = searchForFormulaData(BiometryMDataRight.getFormulaName(), session);
                    //System.out.println(formulaType);
                    lensType = searchForLensData(IOLStudy.getLenseName(), Double.parseDouble(IOLStudy.getLenseName().substring(4, 10)), session);
                }
            }

            EtOphinbiometryIolRefValues iolRefValues = new EtOphinbiometryIolRefValues();
            iolRefValues.setCreatedUserId(selectedUser);
            iolRefValues.setLastModifiedUserId(selectedUser);
            iolRefValues.setCreatedDate(new Date());
            iolRefValues.setLastModifiedDate(new Date());
            iolRefValues.setEventId(newBiometryEvent);
            iolRefValues.setEyeId(new Eye(IOLBiometry.getEyeId()));
            iolRefValues.setFormulaId(formulaType);
            iolRefValues.setLensId(lensType);
            if(BiometryMDataLeft != null){
                iolRefValues.setIolRefValuesLeft(BiometryMDataLeft.getIOLREFJSON());
                iolRefValues.setEmmetropiaLeft(BigDecimal.valueOf(BiometryMDataLeft.getEmmetropia()));
            }
            if(BiometryMDataRight != null){
                iolRefValues.setIolRefValuesRight(BiometryMDataRight.getIOLREFJSON());
                iolRefValues.setEmmetropiaRight(BigDecimal.valueOf(BiometryMDataRight.getEmmetropia()));
            }
            session.save(iolRefValues);
            formulaType = null;
            lensType = null;

        }
        transaction.commit();
        // while we are testing it is better to rollback
        //transaction.rollback();

    }
    
    public void logAuditData(){
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
    }
}
