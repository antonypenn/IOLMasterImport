/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.org.openeyes;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 *
 * @author VEDELEKT
 */
public class StudyData {
    private Calendar StudyDateTime = new GregorianCalendar();
    private String PhysicianName;
    private String SurgeonName;
    private String FormulaName;
    private String LenseName;
    private String InstitutionName;
    private String StationName;
    private String StudyID;
    private String StudyInstanceID;
    
    public void setStudyDateTime(String SDateTime){
        System.out.println(SDateTime);
        this.StudyDateTime.set(Integer.parseInt(SDateTime.substring(0,4)), Integer.parseInt(SDateTime.substring(4,6)), Integer.parseInt(SDateTime.substring(6,8)), Integer.parseInt(SDateTime.substring(8,10)), Integer.parseInt(SDateTime.substring(10,12)));
    }
    
    public Calendar getStudyDateTime(){
        return this.StudyDateTime;
    }
    
    public void setPhysicianName(String SPhysicianName){
        this.PhysicianName = SPhysicianName;
    }
    
    public void setSurgeonName(String SSurgeonName){
        this.SurgeonName = SSurgeonName;
    }
    
    public String getSurgeonName(){
        return this.SurgeonName;
    }

    public void setFormulaName(String SFormulaName){
        this.FormulaName = SFormulaName;
    }
    
    public String getFormulaName(){
        return this.FormulaName;
    }
    
    public void setLenseName(String SLenseName){
        this.LenseName = SLenseName;
    }
    
    public String getLenseName(){
        return this.LenseName;
    }
    
    public void setInstituionName(String SInstitutionName){
        this.InstitutionName = SInstitutionName;
    }
    
    public void setStationName(String SStationName){
        this.StationName = SStationName;
    }
    
    public String getStationName(){
        return this.StationName;
    }
    
    public void setStudyID(String SStudyID){
        this.StudyID = SStudyID;
    }
    
    public String getStudyID(){
        return this.StudyID;
    }
    
    public void setStudyInstanceID(String SStudyInstanceID){
        this.StudyInstanceID = SStudyInstanceID;
    }
    
    public String getStudyInstanceID(){
        return this.StudyInstanceID;
    }
    
    public void printStudyData(){
        System.out.println("--== Study data ==--");
        System.out.println("Study date and time: "+this.StudyDateTime.get(Calendar.DAY_OF_MONTH)+"/"+this.StudyDateTime.get(Calendar.MONTH)+"/"+this.StudyDateTime.get(Calendar.YEAR)+" "+this.StudyDateTime.get(Calendar.HOUR_OF_DAY)+":"+this.StudyDateTime.get(Calendar.MINUTE));
        System.out.println("Study location: "+this.InstitutionName);
        System.out.println("Study station: "+this.StationName);
        System.out.println("Study physician: "+this.PhysicianName);
        System.out.println("Study surgeon: "+this.SurgeonName);
        System.out.println("Study formula: "+this.FormulaName);
        System.out.println("Study lense: "+this.LenseName);
        System.out.println("Study instance ID: "+this.StudyInstanceID);
        System.out.println("Study ID: "+this.StudyID);
        System.out.println("");
    }
    
}