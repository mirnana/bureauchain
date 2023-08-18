package bureauchain;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;

import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.time.LocalDate;
import java.util.Date;

import com.owlike.genson.Genson;

@Contract(name = "DiplomaContract", info = @Info(title = "Diploma Contract", description = "A smart contract that represents a college diploma being issued"))
@Default
public class DiplomaContract implements ContractInterface {

	private final Genson genson = new Genson();

	public DiplomaContract() {
	}

	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public boolean diplomaExists(final Context ctx, final String diplomaID) {
		String diplomaJSON = ctx.getStub().getStringState(diplomaID);
		return (diplomaJSON != null && !diplomaJSON.isEmpty());
	}

	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public Diploma readDiploma(final Context ctx, final String diplomaID) {
		Diploma diploma = null;
		try {
			if (!diplomaExists(ctx, diplomaID)) {
				String err = "The diploma " + diplomaID + " does not exist";
				System.out.println(err);
				throw new ChaincodeException(err, err);
			}

			String diplomaJSON = ctx.getStub().getStringState(diplomaID);
			System.out.println(diplomaJSON);
			diploma = genson.deserialize(diplomaJSON, Diploma.class);
			return diploma;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ChaincodeException(e.toString());
		}
	}

	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void createDiploma(Context ctx, String diplomaID, String OIB, String name, String surname,
			String dateOfBirth, String placeOfBirth, String dateOfIssue, String university, String faculty,
			String department, String study, String level, String academicDegree, String studyType) {

		if (diplomaExists(ctx, diplomaID)) {
			throw new ChaincodeException("The diploma " + diplomaID + " already exists");
		}

		Diploma diploma = new Diploma(
				diplomaID, OIB, name, surname, dateOfBirth, placeOfBirth, dateOfIssue, university, faculty, department,
				study, level, academicDegree, studyType);

		String sortedJSON = genson.serialize(diploma);
		ctx.getStub().putStringState(diplomaID, sortedJSON);
	}

	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void updateDiploma(Context ctx, String diplomaID, String newOIB, String newName, String newSurname,
			String newDateOfBirth, String newPlaceOfBirth, String newDateOfIssue, String newUniversity,
			String newFaculty, String newDepartment, String newStudy, String newLevel, String newAcademicDegree,
			String newStudyType) {

		if (!diplomaExists(ctx, diplomaID)) {
			throw new ChaincodeException("The diploma " + diplomaID + " does not exist");
		}

		Diploma diploma = new Diploma(
				diplomaID, newOIB, newName, newSurname, newDateOfBirth, newPlaceOfBirth, newDateOfIssue, newUniversity,
				newFaculty, newDepartment, newStudy, newLevel, newAcademicDegree, newStudyType);

		String sortedJSON = genson.serialize(diploma);
		ctx.getStub().putStringState(diplomaID, sortedJSON);
	}

	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void deleteDiploma(Context ctx, String diplomaID) {

		if (!diplomaExists(ctx, diplomaID)) {
			throw new ChaincodeException("The diploma " + diplomaID + " does not exist");
		}

		ctx.getStub().delState(diplomaID);
	}
}
