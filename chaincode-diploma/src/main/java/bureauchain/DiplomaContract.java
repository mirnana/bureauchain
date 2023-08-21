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
	public void createDiploma(Context ctx, String diplomaID, String nationalID, String firstName, String lastName,
			String dateOfBirth, String placeOfBirth, String dateOfIssue, String institution,
			String course, String level, String degree) {

		if (diplomaExists(ctx, diplomaID)) {
			throw new ChaincodeException("The diploma " + diplomaID + " already exists");
		}

		Diploma diploma = new Diploma(
				diplomaID, nationalID, firstName, lastName, dateOfBirth, placeOfBirth, dateOfIssue, institution,
				course, level, degree);

		String sortedJSON = genson.serialize(diploma);
		ctx.getStub().putStringState(diplomaID, sortedJSON);
	}

	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void updateDiploma(Context ctx, String diplomaID, String newNationalID, String newFirstName,
			String newLastName,
			String newDateOfBirth, String newPlaceOfBirth, String newDateOfIssue, String newInstitution,
			String newCourse, String newLevel, String newDegree) {

		if (!diplomaExists(ctx, diplomaID)) {
			throw new ChaincodeException("The diploma " + diplomaID + " does not exist");
		}

		Diploma diploma = new Diploma(
				diplomaID, newNationalID, newFirstName, newLastName, newDateOfBirth, newPlaceOfBirth, newDateOfIssue,
				newInstitution,
				newCourse, newLevel, newDegree);

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
