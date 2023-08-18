package bureauchain;

import java.util.Date;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.json.JSONObject;
import com.owlike.genson.annotation.JsonProperty;

@DataType()
public class Diploma {

	@Property()
	private final String diplomaID;

	@Property()
	private String OIB;

	@Property()
	private String name;
	@Property()
	private String surname;
	@Property()
	private String dateOfBirth;
	@Property()
	private String placeOfBirth;
	@Property()
	private String dateOfIssue;

	@Property()
	private String university;
	@Property()
	private String faculty;
	@Property()
	private String department;
	@Property()
	private String study;
	@Property()
	private String level; // prijedipl, dipl, poslijedipl, doktorski
	@Property()
	private String academicDegree; // titula
	@Property()
	private String studyType; // znanstveni, drustveni, umjetnicki

	public Diploma(
			@JsonProperty("diplomaID") final String diplomaID

			, @JsonProperty("OIB") final String OIB, @JsonProperty("name") final String name,
			@JsonProperty("surname") final String surname, @JsonProperty("dateOfBirth") final String dateOfBirth,
			@JsonProperty("placeOfBirth") final String placeOfBirth,
			@JsonProperty("dateOfIssue") final String dateOfIssue

			, @JsonProperty("university") final String university, @JsonProperty("faculty") final String faculty,
			@JsonProperty("department") final String department, @JsonProperty("study") final String study,
			@JsonProperty("level") final String level, @JsonProperty("academicDegree") final String academicDegree,
			@JsonProperty("studyType") final String studyType) {

		this.diplomaID = diplomaID;

		this.OIB = OIB;
		this.name = name;
		this.surname = surname;
		this.dateOfBirth = dateOfBirth;
		this.placeOfBirth = placeOfBirth;
		this.dateOfIssue = dateOfIssue;

		this.university = university;
		this.faculty = faculty;
		this.department = department;
		this.study = study;
		this.level = level;
		this.academicDegree = academicDegree;
		this.studyType = studyType;
	}

	public String getDiplomaID() {
		return diplomaID;
	}

	public String getOIB() {
		return OIB;
	}

	public void setOIB(String oIB) {
		OIB = oIB;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getPlaceOfBirth() {
		return placeOfBirth;
	}

	public void setPlaceOfBirth(String placeOfBirth) {
		this.placeOfBirth = placeOfBirth;
	}

	public String getDateOfIssue() {
		return dateOfIssue;
	}

	public void setDateOfIssue(String dateOfIssue) {
		this.dateOfIssue = dateOfIssue;
	}

	public String getUniversity() {
		return university;
	}

	public void setUniversity(String university) {
		this.university = university;
	}

	public String getFaculty() {
		return faculty;
	}

	public void setFaculty(String faculty) {
		this.faculty = faculty;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getStudy() {
		return study;
	}

	public void setStudy(String study) {
		this.study = study;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getAcademicDegree() {
		return academicDegree;
	}

	public void setAcademicDegree(String academicDegree) {
		this.academicDegree = academicDegree;
	}

	public String getStudyType() {
		return studyType;
	}

	public void setStudyType(String studyType) {
		this.studyType = studyType;
	}

	public String toJSONString() {
		return new JSONObject(this).toString();
	}

	public static Diploma fromJSONString(String json) {

		JSONObject jsonObj = new JSONObject(json);

		String diplomaID = jsonObj.getString("diplomaID");

		String OIB = jsonObj.getString("OIB");
		String name = jsonObj.getString("name");
		String surname = jsonObj.getString("surname");
		String dateOfBirth = jsonObj.getString("dateOfBirth");
		String placeOfBirth = jsonObj.getString("placeOfBirth");
		String dateOfIssue = jsonObj.getString("dateOfIssue");

		String university = jsonObj.getString("university");
		String faculty = jsonObj.getString("faculty");
		String department = jsonObj.getString("department");
		String study = jsonObj.getString("study");
		String level = jsonObj.getString("level");
		String academicDegree = jsonObj.getString("academicDegree");
		String studyType = jsonObj.getString("studyType");

		Diploma asset = new Diploma(
				diplomaID, OIB, name, surname, dateOfBirth, placeOfBirth, dateOfIssue, university, faculty, department,
				study, level, academicDegree, studyType);
		return asset;
	}
}
