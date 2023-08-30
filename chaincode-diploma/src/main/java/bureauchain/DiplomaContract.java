package bureauchain;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.owlike.genson.Genson;

@Contract(
	name = "DiplomaContract", 
	info = @Info(
		title = "Diploma Contract", 
		description = "A smart contract that represents a college diploma being issued"
	)
)
@Default
public class DiplomaContract implements ContractInterface {

	private final Genson genson = new Genson();

	public DiplomaContract() {
	}

	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public boolean diplomaExists( final Context ctx
								, final String diplomaID) {
		String diplomaJSON = ctx.getStub().getStringState(diplomaID);
		return (diplomaJSON != null && !diplomaJSON.isEmpty());
	}

	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public Diploma readDiploma(   final Context ctx
								, final String diplomaID) {

        if (!diplomaExists(ctx, diplomaID)) {
            throw new ChaincodeException("The diploma " + diplomaID + " does not exist");
        }

        String diplomaJSON = ctx.getStub().getStringState(diplomaID);
        Diploma diploma = genson.deserialize(diplomaJSON, Diploma.class);
        return diploma;
	}

	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void createDiploma(Context ctx
							, String diplomaID
							, String nationalID
							, String firstName
							, String lastName
							, String dateOfBirth
							, String placeOfBirth
							, String dateOfIssue
							, String institution
							, String course
							, String level
							, String degree) {

		if (diplomaExists(ctx, diplomaID)) {
			throw new ChaincodeException("The diploma " + diplomaID + " already exists");
		}

		Diploma diploma = new Diploma(diplomaID
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

		String sortedJSON = genson.serialize(diploma);
		ctx.getStub().putStringState(diplomaID, sortedJSON);
	}

	@Transaction(intent = Transaction.TYPE.SUBMIT)
	public void updateDiploma(Context ctx
							, String diplomaID
							, String newNationalID
							, String newFirstName
							, String newLastName
							, String newDateOfBirth
							, String newPlaceOfBirth
							, String newDateOfIssue
							, String newInstitution
							, String newCourse
							, String newLevel
							, String newDegree) {

		if (!diplomaExists(ctx, diplomaID)) {
            throw new ChaincodeException("The diploma " + diplomaID + " does not exist");
		}

		Diploma diploma = new Diploma(diplomaID
									, newNationalID
									, newFirstName
									, newLastName
									, newDateOfBirth
									, newPlaceOfBirth
									, newDateOfIssue
									, newInstitution
									, newCourse
									, newLevel
									, newDegree);

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

	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public String getAllDiplomas(final Context ctx) {

		List<Diploma> queryResults = new ArrayList<Diploma>();

		QueryResultsIterator<KeyValue> results = ctx.getStub().getStateByRange("", "");

		for (KeyValue result : results) {
			Diploma diploma = genson.deserialize(result.getStringValue(), Diploma.class);
			System.out.println(diploma);
			queryResults.add(diploma);
		}

		final String response = genson.serialize(queryResults);

		return response;
	}

	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public Diploma[] queryDiplomasByPrimKey(  final Context ctx
											, final String nationalID
											, final String institution
											, final String course
											, final String level) throws Exception, UnsupportedOperationException {

		String selector = String.format(
				"{\"selector\":" + 
					"{\"nationalID\":\"%s\"," + 
					"\"institution\":\"%s\"," +
					"\"course\":\"%s\"," + 
					"\"level\":\"%s\"}, " + 
				"\"use_index\":" + 
					"[\"/indexPrimKeyDoc\", " + 
					"\"indexPrimKey\"]}"
				
				, nationalID
				, institution
				, course
				, level);
		return getQueryResult(ctx, selector);
	}

	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public Diploma[] queryDiplomasByName( final Context ctx
										, final String firstName
										, final String lastName) throws Exception, UnsupportedOperationException {
		
		String selector = String.format(
				"{\"selector\":" + 
					"{\"firstName\":\"%s\"," + 
					"\"lastName\":\"%s\"}, " + 
				"\"use_index\":" + 
					"[\"/indexNameDoc\", " + 
					"\"indexName\"]}"
					
				, firstName
				, lastName);
		return getQueryResult(ctx, selector);
	}

	@Transaction(intent = Transaction.TYPE.EVALUATE)
	public Diploma[] queryDiplomasByNationalID(final Context ctx
											 , final String nationalID) throws Exception, UnsupportedOperationException {
		String selector = String.format(
				"{\"selector\":" + 
					"{\"nationalID\":\"%s\"}, " + 
				"\"use_index\":" + 
					"[\"/indexNationalIDDoc\", " + 
					"\"indexNationalID\"]}"
					
				, nationalID);
		return getQueryResult(ctx, selector);
	}

	private Diploma[] getQueryResult( final Context ctx
									, final String selector) throws Exception, UnsupportedOperationException {

		List<Diploma> queryResults = new ArrayList<Diploma>();

		try (QueryResultsIterator<KeyValue> results = ctx.getStub().getQueryResult(selector)) {

			for (KeyValue result : results) {
				if (result.getStringValue() == null || result.getStringValue().length() == 0) {
					continue;
				}
				Diploma diploma = genson.deserialize(result.getStringValue(), Diploma.class);
				queryResults.add(diploma);
			}
		}

		return queryResults.toArray(new Diploma[0]);
	}
}
