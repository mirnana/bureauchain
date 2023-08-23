package bureauchain;

import java.util.Date;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.json.JSONObject;
import com.owlike.genson.annotation.JsonProperty;

@DataType()
public class Diploma {

	@Property() private final String diplomaID;

	@Property() private String nationalID;

	@Property() private String firstName;
	@Property() private String lastName;
	@Property() private String dateOfBirth;
	@Property() private String placeOfBirth;
	@Property() private String dateOfIssue;

	@Property() private String institution;
	@Property() private String course;
	@Property() private String level;
	@Property() private String degree;

	public Diploma(
			  @JsonProperty("diplomaID") 	final String diplomaID

			, @JsonProperty("nationalID") 	final String nationalID
			, @JsonProperty("firstName") 	final String firstName
			, @JsonProperty("lastName") 	final String lastName
			, @JsonProperty("dateOfBirth") 	final String dateOfBirth
			, @JsonProperty("placeOfBirth") final String placeOfBirth
			, @JsonProperty("dateOfIssue") 	final String dateOfIssue

			, @JsonProperty("institution") 	final String institution
			, @JsonProperty("course") 		final String course
			, @JsonProperty("level") 		final String level
			, @JsonProperty("degree") 		final String degree) {

		this.diplomaID 		= diplomaID;

		this.nationalID 	= nationalID;
		this.firstName 		= firstName;
		this.lastName 		= lastName;
		this.dateOfBirth 	= dateOfBirth;
		this.placeOfBirth	= placeOfBirth;
		this.dateOfIssue 	= dateOfIssue;

		this.institution 	= institution;
		this.course 		= course;
		this.level 			= level;
		this.degree 		= degree;
	}

	public String getDiplomaID() {
		return diplomaID;
	}

	public String getNationalID() {
		return nationalID;
	}
	public void setNationalID(String nationalID) {
		this.nationalID = nationalID;
	}

	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
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

	public String getInstitution() {
		return institution;
	}
	public void setInstitution(String institution) {
		this.institution = institution;
	}

	public String getCourse() {
		return course;
	}
	public void setCourse(String course) {
		this.course = course;
	}

	public String getLevel() {
		return level;
	}
	public void setLevel(String level) {
		this.level = level;
	}

	public String getDegree() {
		return degree;
	}
	public void setDegree(String degree) {
		this.degree = degree;
	}

	public String toJSONString() {
		return new JSONObject(this).toString();
	}

	public static Diploma fromJSONString(String json) {

		JSONObject jsonObj 	= new JSONObject(json);

		String diplomaID 	= jsonObj.getString("diplomaID");

		String nationalID 	= jsonObj.getString("nationalID");
		String firstName 	= jsonObj.getString("firstName");
		String lastName		= jsonObj.getString("lastName");
		String dateOfBirth 	= jsonObj.getString("dateOfBirth");
		String placeOfBirth	= jsonObj.getString("placeOfBirth");
		String dateOfIssue 	= jsonObj.getString("dateOfIssue");

		String institution 	= jsonObj.getString("institution");
		String course 		= jsonObj.getString("course");
		String level 		= jsonObj.getString("level");
		String degree 		= jsonObj.getString("degree");

		Diploma asset = new Diploma(  diplomaID
									, nationalID
									, firstName
									, lastName
									, dateOfBirth
									, placeOfBirth
									, dateOfIssue
									, institution
									, course
									, level
									, degree);
		return asset;
	}
}
