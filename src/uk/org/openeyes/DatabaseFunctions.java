/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.org.openeyes;

import java.io.File;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import uk.org.openeyes.models.EtOphinbiometryCalculation;
import uk.org.openeyes.models.EtOphinbiometryIolRefValues;
import uk.org.openeyes.models.EtOphinbiometryMeasurement;
import uk.org.openeyes.models.EtOphinbiometrySelection;
import uk.org.openeyes.models.Event;
import uk.org.openeyes.models.EventType;
import uk.org.openeyes.models.Eye;
import uk.org.openeyes.models.OphinbiometryCalculationFormula;
import uk.org.openeyes.models.OphinbiometryImportedEvents;
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
    private Session session;
    private Transaction transaction;
    private User selectedUser;
    private StudyData eventStudy;
    private BiometryData eventBiometry;
    private OphinbiometryImportedEvents importedBiometryEvent;
    private boolean isNewEvent = true;
    
    private User searchStudyUser(String userName){

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
        return returnUser;
    }
    
    private OphinbiometryLenstypeLens searchForLensData(String lensName, Double aConst){
        OphinbiometryLenstypeLens lensType = null;
        Criteria crit = session.createCriteria(OphinbiometryLenstypeLens.class);
        
        //we search for the full name first
        crit.add(Restrictions.eq("name",lensName));
        if(!crit.list().isEmpty()){
            if(crit.list().get(0) != null){
                lensType = (OphinbiometryLenstypeLens) crit.list().get(0);
            }
        }
        /*  we don't need this part now, but keep it here if someone ask for it later
        else{
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
        */
        //if nothing found we create a new one
        if(lensType == null){
            lensType = new OphinbiometryLenstypeLens();
            lensType.setName(lensName);
            // TODO: we should extract A constant value for single lense format somehow!!!!
            lensType.setAcon(BigDecimal.valueOf(aConst));
            // TODO: we may need to add user name here!!
            User selectedUser = searchStudyUser("");
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
    
    private OphinbiometryCalculationFormula searchForFormulaData(String formulaName){
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
            User selectedUser = searchStudyUser("");
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
        String defaultConfig = "resources/hibernate.cfg.xml";
        File inputFile = null;
        final StandardServiceRegistry registry;
                
        if( ! configFile.equals("")){
           inputFile = new File(configFile);
        }
        
        if( inputFile != null){
            registry = new StandardServiceRegistryBuilder()
                        .configure(inputFile) // configures settings from hibernate.cfg.xml
                        .build();
        }else{
            registry = new StandardServiceRegistryBuilder()
                        .configure(defaultConfig) // configures settings from hibernate.cfg.xml
                        .build();
        }
        
        try {
            sessionFactory = new MetadataSources( registry ).buildMetadata().buildSessionFactory();
        }
        catch (Exception e) {
            // The registry would be destroyed by the SessionFactory, but we had trouble building the SessionFactory
            // so destroy it manually.
            System.out.println("Failed to connect to the database, please check your hibernate configuration file!");
            
            // TODO: need to add debug config here!
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
                System.out.println("Selected episode: "+selectedEpisode.toString());
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
    
    /**
    *
    * 
    **/
    public String getStudyYMD(Calendar studyDate) {
        String formattedStudyDate = String.format("%04d-%02d-%02d %02d:%02d:%02d",
                studyDate.get(Calendar.YEAR),
                studyDate.get(Calendar.MONTH),
                studyDate.get(Calendar.DAY_OF_MONTH),
                studyDate.get(Calendar.HOUR_OF_DAY),
                studyDate.get(Calendar.MINUTE),
                studyDate.get(Calendar.SECOND)
        );
        return formattedStudyDate;
    }
    
    private Event createNewEvent(){
        Event newBiometryEvent = new Event();
        
        System.out.println("Starting event...");
        if(this.selectedEpisode != null){
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
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
        try {
            newBiometryEvent.setEventDate(df.parse(getStudyYMD(eventStudy.getStudyDateTime())));
        } catch (ParseException ex) {
            Logger.getLogger(DatabaseFunctions.class.getName()).log(Level.SEVERE, null, ex);
        }

        // let's save it!
        // 1. create new event
        session.save(newBiometryEvent);
        System.out.println("Event saved...");
        
        return newBiometryEvent;
    }
    
    /**
     * 
     *
     * @return OphinbiometryImportedEvents
     **/
    private OphinbiometryImportedEvents processImportedEvent(){
        
        OphinbiometryImportedEvents importedEvent;
                
        Criteria currentEvent = session.createCriteria(OphinbiometryImportedEvents.class);
        
        currentEvent.add(Restrictions.eq("studyId", eventStudy.getStudyInstanceID()));

        // if an event already exists we pick up that 
        if(!currentEvent.list().isEmpty()){
            importedEvent = (OphinbiometryImportedEvents) currentEvent.list().get(0);
            //importedEvent = new OphinbiometryImportedEvents(currentData.getEventId().getId());
            isNewEvent = false;
        }
        // or else we should create a new event
        else{
            Event newEvent = createNewEvent();
            importedEvent = new OphinbiometryImportedEvents();
            importedEvent.setDeviceName(eventStudy.getInstituionName());
            importedEvent.setDeviceId(eventStudy.getStationName());
            importedEvent.setDeviceManufacturer(eventStudy.getDeviceManufacturer());
            importedEvent.setDeviceModel(eventStudy.getDeviceModel());
            importedEvent.setDeviceSoftwareVersion(eventStudy.getDeviceSoftwareVersion());
            importedEvent.setStudyId(eventStudy.getStudyInstanceID());
            importedEvent.setPatientId(getSelectedPatient());
            importedEvent.setEventId(newEvent);
            importedEvent.setCreatedDate(new Date());
            importedEvent.setLastModifiedDate(new Date());
            importedEvent.setCreatedUserId(selectedUser);
            importedEvent.setLastModifiedUserId(selectedUser);
            
            boolean isLinked = false;
            if(getSelectedEpisode() != null){
                isLinked = true;
            }
            importedEvent.setIsLinked(isLinked);
            session.save(importedEvent);
        }
        return importedEvent;
    }

    // for unit testing it need to be public
    public void setSession(){
        this.session = sessionFactory.openSession();
    }
    
    private Session getSession(){
        return this.session;
    }
    
    // for unit testing it need to be public
    public void setTransaction(){
        if(session == null){
            this.setSession();
        }
        this.transaction = session.beginTransaction();
    }
    
    private Transaction getTransaction(){
        return this.transaction;
    }
    
    // for unit testing it need to be public
    public void setEventStudy(StudyData inputStudy){
        this.eventStudy = inputStudy;
    }
    
    private StudyData getEventStudy(){
        return this.eventStudy;
    }
    
    // for unit testing it need to be public
    public void setEventBiometry(BiometryData inputBiometry){
        this.eventBiometry = inputBiometry;
    }
    
    private BiometryData getEventBiometry(){
        return this.eventBiometry;
    }
    
    // for unit testing it need to be public
    public void setSelectedUser(){
        if(eventStudy != null){
            String SurgeonName="";
            if(eventStudy.getSurgeonName() != null){
                SurgeonName = eventStudy.getSurgeonName();
            }
            this.selectedUser = searchStudyUser(SurgeonName);
        }
    }
    
    private void setMeasurementData(EtOphinbiometryMeasurement basicMeasurementData){
        
        BiometrySide sideData;
        Double SNR;
                
        if(basicMeasurementData.getEventId() == null){
            basicMeasurementData.setEventId(importedBiometryEvent.getEventId());
            basicMeasurementData.setEyeId(new Eye(eventBiometry.getEyeId()));
            basicMeasurementData.setCreatedDate(new Date());
            basicMeasurementData.setCreatedUserId(selectedUser);
            basicMeasurementData.setLastModifiedDate(new Date());
            basicMeasurementData.setLastModifiedUserId(selectedUser);
        }
        
        sideData = eventBiometry.getBiometryValue("L");
        basicMeasurementData.setK1Left(BigDecimal.valueOf(sideData.getK1()));
        basicMeasurementData.setK2Left(BigDecimal.valueOf(sideData.getK2()));
        basicMeasurementData.setAxisK1Left(BigDecimal.valueOf(sideData.getAxisK1()));
        basicMeasurementData.setAxialLengthLeft(BigDecimal.valueOf(sideData.getAL()));
        SNR = (Double) sideData.getSNR();
        basicMeasurementData.setSnrLeft(SNR.intValue());
        
        sideData = eventBiometry.getBiometryValue("R");
        basicMeasurementData.setK1Right(BigDecimal.valueOf(sideData.getK1()));
        basicMeasurementData.setK2Right(BigDecimal.valueOf(sideData.getK2()));
        basicMeasurementData.setAxisK1Right(BigDecimal.valueOf(sideData.getAxisK1()));
        basicMeasurementData.setAxialLengthRight(BigDecimal.valueOf(sideData.getAL()));
        SNR = (Double) sideData.getSNR();
        basicMeasurementData.setSnrRight(SNR.intValue());        
    }
    
    private void createSelectionData(){
        // we save 0 values here because Biometry event require those values to display the element
        EtOphinbiometrySelection newBasicSelectionData = new EtOphinbiometrySelection();
        newBasicSelectionData.setCreatedDate(new Date());
        newBasicSelectionData.setLastModifiedDate(new Date());
        newBasicSelectionData.setCreatedUserId(selectedUser);
        newBasicSelectionData.setLastModifiedUserId(selectedUser);
        newBasicSelectionData.setEventId(importedBiometryEvent.getEventId());
        newBasicSelectionData.setEyeId(new Eye(eventBiometry.getEyeId()));
        newBasicSelectionData.setIolPowerLeft(BigDecimal.ZERO);
        newBasicSelectionData.setIolPowerRight(BigDecimal.ZERO);
        newBasicSelectionData.setPredictedRefractionLeft(BigDecimal.ZERO);
        newBasicSelectionData.setPredictedRefractionRight(BigDecimal.ZERO);
        session.save(newBasicSelectionData);
    }
    
    private void createCalculationData(){
        EtOphinbiometryCalculation newBasicCalculationData = new EtOphinbiometryCalculation();
        newBasicCalculationData.setCreatedDate(new Date());
        newBasicCalculationData.setLastModifiedDate(new Date());
        newBasicCalculationData.setCreatedUserId(selectedUser);
        newBasicCalculationData.setLastModifiedUserId(selectedUser);
        newBasicCalculationData.setEventId(importedBiometryEvent.getEventId());
        newBasicCalculationData.setEyeId(new Eye(eventBiometry.getEyeId()));
        newBasicCalculationData.setFormulaIdLeft(new OphinbiometryCalculationFormula(1));
        newBasicCalculationData.setFormulaIdRight(new OphinbiometryCalculationFormula(1));
        newBasicCalculationData.setTargetRefractionLeft(BigDecimal.ZERO);
        newBasicCalculationData.setTargetRefractionRight(BigDecimal.ZERO);
        session.save(newBasicCalculationData);

    }
    
    private void saveIolRefValues(){
        // 3. save lens and formula specific data
        ArrayList<BiometryMeasurementData> storedBiometryMeasurementDataLeft = eventBiometry.getBiometryValue("L").getMeasurements();
        ArrayList<BiometryMeasurementData> storedBiometryMeasurementDataRight = eventBiometry.getBiometryValue("R").getMeasurements();

        Integer ArrayListSize;
        String ReferenceSide;
        
        if(storedBiometryMeasurementDataLeft.size() > storedBiometryMeasurementDataRight.size()){
            ArrayListSize = storedBiometryMeasurementDataLeft.size();
            ReferenceSide = "L";
        }else{
            ArrayListSize = storedBiometryMeasurementDataRight.size();
            ReferenceSide = "R";
        }
        
        OphinbiometryLenstypeLens lensType = null;
        OphinbiometryCalculationFormula formulaType = null;

        for(Integer i = 0; i < ArrayListSize; i++){
            // TODO: we need to handle multi formula - multi formula here!!!
            // there are some files where the lenses stored as FORMULA IOLType, but in that case the A constant is displayed as A0, A1, A2 and pACD const
            // formulas used in this cases: HofferQ, Haigis L
            // head data should be the same for both sides
            BiometryMeasurementData rowData;
            if(ReferenceSide.equals("L")){
                rowData = storedBiometryMeasurementDataLeft.get(i);
            }else{
                rowData = storedBiometryMeasurementDataRight.get(i);
            }
            
            if(rowData.getLenseName() != null && !rowData.getLenseName().equals("")){
                System.out.println("Multi lense - single formula format...");
                lensType = searchForLensData(rowData.getLenseName(), rowData.getAConst());
                //System.out.println(lensType);
                formulaType = searchForFormulaData(eventStudy.getFormulaName());
            }else if(rowData.getFormulaName() != null && !rowData.getFormulaName().equals("")){
                System.out.println("Multi formula - singe lense format...");
                formulaType = searchForFormulaData(rowData.getFormulaName());
                // TODO: need to handle A const here for single lense format!!!
                lensType = searchForLensData(eventStudy.getLenseName(), 0.0);
            }
            
            EtOphinbiometryIolRefValues iolRefValues = new EtOphinbiometryIolRefValues();
            iolRefValues.setCreatedUserId(selectedUser);
            iolRefValues.setLastModifiedUserId(selectedUser);
            iolRefValues.setCreatedDate(new Date());
            iolRefValues.setLastModifiedDate(new Date());
            iolRefValues.setEventId(importedBiometryEvent.getEventId());
            iolRefValues.setEyeId(new Eye(eventBiometry.getEyeId()));
            iolRefValues.setFormulaId(formulaType);
            iolRefValues.setLensId(lensType);
            if(ReferenceSide.equals("L")){
                iolRefValues.setIolRefValuesLeft(rowData.getIOLREFJSON());
                iolRefValues.setEmmetropiaLeft(BigDecimal.valueOf(rowData.getEmmetropia()));
                if(storedBiometryMeasurementDataLeft.size() == storedBiometryMeasurementDataRight.size()){
                    iolRefValues.setIolRefValuesRight(storedBiometryMeasurementDataRight.get(i).getIOLREFJSON());
                    iolRefValues.setEmmetropiaRight(BigDecimal.valueOf(storedBiometryMeasurementDataRight.get(i).getEmmetropia()));
                }
            }else{
                iolRefValues.setIolRefValuesRight(rowData.getIOLREFJSON());
                iolRefValues.setEmmetropiaRight(BigDecimal.valueOf(rowData.getEmmetropia()));
                if(storedBiometryMeasurementDataLeft.size() == storedBiometryMeasurementDataRight.size()){
                    iolRefValues.setIolRefValuesLeft(storedBiometryMeasurementDataLeft.get(i).getIOLREFJSON());
                    iolRefValues.setEmmetropiaLeft(BigDecimal.valueOf(storedBiometryMeasurementDataLeft.get(i).getEmmetropia()));
                }
            }
            
            session.save(iolRefValues);
            formulaType = null;
            lensType = null;

        }

    }
    
    public void processBiometryEvent(StudyData IOLStudy, BiometryData IOLBiometry){
        this.setSession();
        this.setTransaction();
        
        this.setEventStudy(IOLStudy);
        System.out.println("Study data set successfully");
        this.setEventBiometry(IOLBiometry);
        System.out.println("Biometry data set successfully");
        
        this.setSelectedUser();
        System.out.println("User selected successfully");

        this.selectActiveEpisode();
        
        // Event ID will be: importedBiometryEvent.getEventId
        importedBiometryEvent = processImportedEvent();

        // we want to save measurement field only if the event is new, for existing events we need to merge the
        // iol_ref_values table only
        if(isNewEvent){
            EtOphinbiometryMeasurement newBasicMeasurementData = new EtOphinbiometryMeasurement();
        
            setMeasurementData(newBasicMeasurementData);
        
            session.save(newBasicMeasurementData);
            
            // we save selection and calculation data as empty
            this.createSelectionData();
            this.createCalculationData();
            
        }
        
        this.saveIolRefValues();
        
        transaction.commit();
        // while we are testing it is better to rollback
        //transaction.rollback();
        session.close();
    }
    
   
    public void logAuditData(){
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        
    }
}
