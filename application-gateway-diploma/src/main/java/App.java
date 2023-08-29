
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.SubmitException;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;

import javax.naming.spi.DirStateFactory.Result;

import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.sql.*;

public final class App {
	private static final String MSP_ID 			= System.getenv().getOrDefault("MSP_ID", "Org1MSP");
	private static final String CHANNEL_NAME 	= System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
	private static final String CHAINCODE_NAME	= System.getenv().getOrDefault("CHAINCODE_NAME", "diploma");

	// Path to crypto materials.
	private static final Path CRYPTO_PATH = Paths.get(
		"../fabric-samples/test-network/organizations/peerOrganizations/org1.example.com"
	);
	// Path to user certificate.
	private static final Path CERT_PATH = CRYPTO_PATH.resolve(
		Paths.get("users/User1@org1.example.com/msp/signcerts/cert.pem")
	);
	// Path to user private key directory.
	private static final Path KEY_DIR_PATH = CRYPTO_PATH.resolve(
		Paths.get("users/User1@org1.example.com/msp/keystore")
	);
	// Path to peer tls certificate.
	private static final Path TLS_CERT_PATH = CRYPTO_PATH.resolve(
		Paths.get("peers/peer0.org1.example.com/tls/ca.crt")
	);

	// Gateway peer end point.
	private static final String PEER_ENDPOINT = "localhost:7051";
	private static final String OVERRIDE_AUTH = "peer0.org1.example.com";

	// Database connection details
	private static final String DB_URL = "jdbc:mysql://localhost:3306/bureau" +
											"?useSSL=false" + 
											"&useJDBCCompliantTimezoneShift=true" + 
											"&useLegacyDatetimeCode=false" + 
											"&serverTimezone=Europe/Zagreb";
	// 3306 is the default port for MySQL
	private static final String DB_USERNAME = "user";
	private static final String DB_PASSWORD = "password";

	private final Contract contract;
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public static void main(final String[] args) throws Exception {
		// The gRPC client connection should be shared by all Gateway connections to
		// this endpoint.
		var channel = newGrpcConnection();

		var builder = Gateway.newInstance().identity(newIdentity()).signer(newSigner()).connection(channel)
				// Default timeouts for different gRPC calls
				.evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
				.endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
				.submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
				.commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

		try (var gateway = builder.connect()) {
			new App(gateway).run();
		} finally {
			channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
		}
	}

	private static ManagedChannel newGrpcConnection() throws IOException {
		var credentials = TlsChannelCredentials.newBuilder()
				.trustManager(TLS_CERT_PATH.toFile())
				.build();
		return Grpc.newChannelBuilder(PEER_ENDPOINT, credentials)
				.overrideAuthority(OVERRIDE_AUTH)
				.build();
	}

	private static Identity newIdentity() throws IOException, CertificateException {
		var certReader = Files.newBufferedReader(CERT_PATH);
		var certificate = Identities.readX509Certificate(certReader);

		return new X509Identity(MSP_ID, certificate);
	}

	private static Signer newSigner() throws IOException, InvalidKeyException {
		var keyReader = Files.newBufferedReader(getPrivateKeyPath());
		var privateKey = Identities.readPrivateKey(keyReader);

		return Signers.newPrivateKeySigner(privateKey);
	}

	private static Path getPrivateKeyPath() throws IOException {
		try (var keyFiles = Files.list(KEY_DIR_PATH)) {
			return keyFiles.findFirst().orElseThrow();
		}
	}

	public App(final Gateway gateway) {
		// Get a network instance representing the channel where the smart contract is
		// deployed.
		var network = gateway.getNetwork(CHANNEL_NAME);

		// Get the smart contract from the network.
		contract = network.getContract(CHAINCODE_NAME);
	}

	public void run() throws GatewayException, CommitException, SQLException, Exception, DateTimeParseException {

		Scanner sc = new Scanner(System.in);
		while (true) {
			System.out.println("Enter: r to read diploma by ID");
			System.out.println("       a to read all diplomas");
			System.out.println("       p to read diploma by nationalID, institution, course, level (i.e. the primary key)");
			System.out.println("       n to read diploma by the owner's name");
			System.out.println("       i to read diploma by the owner's national ID");
			System.out.println("       s to create a single diploma by student ID");
			System.out.println("       t to create multiple diplomas by date of defence of thesis");
			System.out.println("       u to update a diploma");
			System.out.println("       d to delete a diploma");
			System.out.println("       x to exit");

			String str = sc.nextLine();

			if (str.equals("r")) {
				System.out.println("Insert diploma ID:");
				String diplomaID = sc.nextLine();
				try {
					System.out.println(readDiploma(diplomaID));
				} catch(Exception e) {
					System.out.println("ERROR reading diploma: " + e.getMessage());
				}
			} else if (str.equals("a")) {
				System.out.println(getAllDiplomas());
			} else if (str.equals("p")) {
				System.out.println("Insert national ID:");
				String nationalID = sc.nextLine();
				System.out.println("Insert institution:");
				String institution = sc.nextLine();
				System.out.println("Insert course:");
				String course = sc.nextLine();
				System.out.println("Insert level:");
				String level = sc.nextLine();
				System.out.println(readDiplomaByPrimKey(  nationalID
														, institution
														, course
														, level));
			} else if (str.equals("n")) {
				System.out.println("Insert first name:");
				String firstName = sc.nextLine();
				System.out.println("Insert last name:");
				String lastName = sc.nextLine();
				System.out.println(readDiplomaByName( firstName
													, lastName));
			} else if (str.equals("i")) {
				System.out.println("Insert national ID:");
				String nationalID = sc.nextLine();
				System.out.println(readDiplomaByNationalID(nationalID));
			} else if (str.equals("s")) {
				System.out.println("Insert student ID:");
				String studentID = sc.nextLine();
				createDiplomaByStudentID(studentID);
			} else if (str.equals("t")) {
				System.out.println("Insert date in format YYYY-MM-dd:");
				String dateStr = sc.nextLine();
				try {
					LocalDate date = LocalDate.parse(dateStr);
					createDiplomasByDateOfDefence(dateStr);
				} catch (DateTimeParseException dtpe) {
					System.out.println("Unable to parse date");
				}
			} else if (str.equals("u")) {
				System.out.println("Insert diplomaID:");
				String diplomaID = sc.nextLine();
				System.out.println("Insert nationalID:");
				String nationalID = sc.nextLine();
				System.out.println("Insert firstName:");
				String firstName = sc.nextLine();
				System.out.println("Insert lastName:");
				String lastName = sc.nextLine();
				System.out.println("Insert dateOfBirth:");
				String dateOfBirth = sc.nextLine();
				System.out.println("Insert placeOfBirth:");
				String placeOfBirth = sc.nextLine();
				System.out.println("Insert dateOfIssue:");
				String dateOfIssue = sc.nextLine();
				System.out.println("Insert institution:");
				String institution = sc.nextLine();
				System.out.println("Insert course:");
				String course = sc.nextLine();
				System.out.println("Insert level:");
				String level = sc.nextLine();
				System.out.println("Insert degree:");
				String degree = sc.nextLine();
				updateDiploma(diplomaID
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
			} else if (str.equals("d")) {
				System.out.println("Insert diploma ID:");
				String diplomaID = sc.nextLine();
				deleteDiploma(diplomaID);
			} else if (str.equals("x")) {
				System.out.println("Bye");
				break;
			}
		}
		sc.close();
	}

	private String prettyJson(final byte[] json) {
		return prettyJson(new String(json, StandardCharsets.UTF_8));
	}

	private String prettyJson(final String json) {
		var parsedJson = JsonParser.parseString(json);
		return gson.toJson(parsedJson);
	}

	private String readDiploma(String diplomaID) throws GatewayException {

		var result = contract.evaluateTransaction("readDiploma", diplomaID);
		return prettyJson(result);
	}

	private String getAllDiplomas() throws GatewayException {

		var result = contract.evaluateTransaction("getAllDiplomas");
		return prettyJson(result);
	}

	private String readDiplomaByPrimKey(  String nationalID
										, String institution
										, String course
										, String level) throws GatewayException {

		var result = contract.evaluateTransaction("queryDiplomasByPrimKey"
												, nationalID
												, institution
												, course
												, level);
		return prettyJson(result);
	}

	private String readDiplomaByName( String firstName
									, String lastName) throws GatewayException {

		var result = contract.evaluateTransaction("queryDiplomasByName"
												, firstName
												, lastName);
		return prettyJson(result);
	}

	private String readDiplomaByNationalID(String nationalID) throws GatewayException {

		var result = contract.evaluateTransaction("queryDiplomasByNationalID", nationalID);
		return prettyJson(result);
	}

	private Map<String, String> fromStudentPrepStmtResults(String studentID) throws SQLException, Exception {
		Connection c = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
		PreparedStatement stmt = c.prepareStatement(
			"SELECT nationalID, firstName, lastName, dateOfBirth, placeOfBirth, institutionID " +
			"  FROM student " +
			" WHERE studentID = ?;"
		);
		stmt.setString(1, studentID);
		ResultSet rs = stmt.executeQuery();

		int rowCount = 0;
		if (rs.last()) {
			rowCount = rs.getRow();
		}
		if (rowCount == 0) {
			throw new Exception("Student with studentID " + studentID + " does not exist");
		}
		
		rs.first(); // only one row expected as 'studentID' is the primary key
		Map<String, String> attributes = new HashMap<>();
		attributes.put("nationalID", rs.getString("nationalID"));
		attributes.put("firstName", rs.getString("firstName"));
		attributes.put("lastName", rs.getString("lastName"));
		attributes.put("dateOfBirth", rs.getString("dateOfBirth"));
		attributes.put("placeOfBirth", rs.getString("placeOfBirth"));
		attributes.put("institutionID", rs.getString("institutionID"));

		c.close();
		return attributes;
	}

	private Map<String, String> fromCoursePrepStmtResults(Integer courseID, Integer institutuionID) throws SQLException {
		Connection c = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
		PreparedStatement stmt = c.prepareStatement(
			"SELECT courseName, levelOfStudy " +
			"  FROM course " +
			" WHERE courseID = ?" +
			"   AND institutionID = ?;"
		);
		stmt.setInt(1, courseID);
		stmt.setInt(2, institutuionID);
		ResultSet rs = stmt.executeQuery();
		
		rs.first();
		Map<String, String> attributes = new HashMap<>();
		attributes.put("courseName", rs.getString("courseName"));
		attributes.put("levelOfStudy", rs.getString("levelOfStudy"));

		c.close();
		return attributes;
	}

	private Map<String, String> fromInstitutionPrepStmtResults(Integer institutionID) throws SQLException {
		Connection c = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
		PreparedStatement stmt = c.prepareStatement(
			"SELECT institutionName, parentInstitutionID " +
			"  FROM institution " +
			" WHERE institutionID = ?;"
		);
		stmt.setInt(1, institutionID);
		ResultSet rs = stmt.executeQuery();

		rs.first();
		Map<String, String> attributes = new HashMap<>();
		attributes.put("institutionName", rs.getString("institutionName"));
		attributes.put("parentInstitutionID", rs.getString("parentInstitutionID"));

		c.close();
		return attributes;
	}

	private Map<String, String> fromDefenceOfThesisPrepStmtResult(Integer institutionID
																, String studentID) throws SQLException, Exception {
		Connection c = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
		PreparedStatement stmt = c.prepareStatement(
			"SELECT courseID, degree " +
			"  FROM defenceOfThesis " +
			" WHERE institutionID = ? " +
			"   AND studentID = ? " +
			"	AND grade IS NOT NULL " +
			"ORDER BY dueDate DESC, dateOfDefence DESC, seq DESC " +
			"LIMIT 1;"
		);
		stmt.setInt(1, institutionID);
		stmt.setString(2, studentID);
		ResultSet rs = stmt.executeQuery();
		
		int rowCount = 0;
		if (rs.last()) {
			rowCount = rs.getRow();
		}
		if (rowCount == 0) {
			throw new Exception("Student with studentID " + studentID + " has not defended any theses");
		}
		
		rs.first(); // only one row expected because of LIMIT 1 in the query
		Map<String, String> attributes = new HashMap<>();
		attributes.put("courseID", rs.getString("courseID"));
		attributes.put("degree", rs.getString("degree"));

		c.close();
		return attributes;
	}

	private void createDiplomaByStudentID(String studentID) throws SQLException, Exception {
		try {
			Connection c = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
			Statement stmt = c.createStatement();

			System.out.println("... querying the local relational database ...");
			ResultSet rs;
			
			Map<String, String> studentAttributes = new HashMap(fromStudentPrepStmtResults(studentID));
			String nationalID 		= studentAttributes.get("nationalID");
			String firstName 		= studentAttributes.get("firstName");
			String lastName 		= studentAttributes.get("lastName");
			String dateOfBirth 		= studentAttributes.get("dateOfBirth");
			String placeOfBirth 	= studentAttributes.get("placeOfBirth");
			String institutionID 	= studentAttributes.get("institutionID");
			String institutionID2 	= institutionID;

			String institution 			= "";
			String parentInstitutionID 	= "";
			boolean firstIteration 		= true;
			do {
				Map<String, String> institutionAttributes = new HashMap<>(
					fromInstitutionPrepStmtResults(Integer.parseInt(institutionID)));
				
					if (firstIteration) {
					firstIteration = false;
				} else {
					institution += ", ";
				}
				
				institution += institutionAttributes.get("institutionName");
				institutionID 	= parentInstitutionID 
								= institutionAttributes.get("parentInstitutionID");
			} while (parentInstitutionID != null);

			Map<String, String> defenceAttributes = new HashMap<>(
				fromDefenceOfThesisPrepStmtResult(Integer.parseInt(institutionID2), studentID));
			String courseID = defenceAttributes.get("courseID");
			String degree = defenceAttributes.get("degree");
			

			Map<String, String> courseAttributes = new HashMap<>(
				fromCoursePrepStmtResults(Integer.parseInt(courseID), Integer.parseInt(institutionID2)));
			String courseName 	= courseAttributes.get("courseName");
			String levelOfStudy = courseAttributes.get("levelOfStudy");

			c.close();
			System.out.println("... data from relational database retreived ...");

			System.out.println("... checking if diploma already exists ...");
			String fromLedger = readDiplomaByPrimKey( nationalID
													, institution
													, courseName
													, levelOfStudy);
			if (!fromLedger.equals("[]")) {
				throw new Exception("Diploma with the given parameters already exists: " + fromLedger);
			}

			String diplomaID = "diploma" + Instant.now().toEpochMilli();
			contract.submitTransaction("createDiploma"
									, diplomaID
									, nationalID
									, firstName
									, lastName
									, dateOfBirth.toString()
									, placeOfBirth
									, LocalDate.now().toString()
									, institution
									, courseName
									, levelOfStudy
									, degree);
			System.out.println("Successfully created new diploma " + diplomaID);

		} catch (Exception e) {
			System.out.println("ERROR while creating diploma by ID: " + e.getMessage());
		}
	}

	private void createDiplomasByDateOfDefence(String dateOfDefence) {
		try {
			Connection c = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
			System.out.println("... querying the local relational database ...");
			PreparedStatement stmt = c.prepareStatement(	
				"SELECT institutionID, courseID, studentID, degree " +
				"  FROM defenceOfThesis " +
				" WHERE dateOfDefence = ? " +
				"   AND grade IS NOT NULL;"
			);
			stmt.setString(1, dateOfDefence);
			ResultSet rs = stmt.executeQuery();

			int rowCount = 0;
			if (rs.last()) {
				rowCount = rs.getRow();
				rs.beforeFirst();
			}
			if (rowCount == 0) {
				throw new Exception("No defences found on date " + dateOfDefence);
			}
			while (rs.next()) {
				String institutionID 	= rs.getString("institutionID");
				String institutionID2	= institutionID;
				String courseID 		= rs.getString("courseID");
				String studentID 		= rs.getString("studentID");
				String degree 			= rs.getString("degree");

				Statement stmt2 = c.createStatement();

				String institution 			= "";
				String parentInstitutionID 	= "";
				boolean firstIteration 		= true;
				do {
					Map<String, String> institutionAttributes = new HashMap<>(
						fromInstitutionPrepStmtResults(Integer.parseInt(institutionID)));

					if (firstIteration) {
						firstIteration = false;
					} else {
						institution += ", ";
					}
					
					institution += institutionAttributes.get("institutionName");
					institutionID = parentInstitutionID = institutionAttributes.get("parentInstitutionID");
				} while (parentInstitutionID != null);

				Map<String, String> courseAttributes = new HashMap<>(fromCoursePrepStmtResults(Integer.parseInt(courseID), Integer.parseInt(institutionID2)));
				String courseName 	= courseAttributes.get("courseName");
				String levelOfStudy = courseAttributes.get("levelOfStudy");

				Map<String, String> studentAttributes = new HashMap(fromStudentPrepStmtResults(studentID));
				String nationalID 		= studentAttributes.get("nationalID");
				String firstName 		= studentAttributes.get("firstName");
				String lastName 		= studentAttributes.get("lastName");
				String dateOfBirth 		= studentAttributes.get("dateOfBirth");
				String placeOfBirth 	= studentAttributes.get("placeOfBirth");

				System.out.println("... creating diploma for " + nationalID + " ...");
				String fromLedger = readDiplomaByPrimKey( nationalID
														, institution
														, courseName
														, levelOfStudy);
				if (!fromLedger.equals("[]")) {
					throw new Exception("Diploma with the given parameters already exists: " + fromLedger);
				}

				String diplomaID = "diploma" + Instant.now().toEpochMilli();
				contract.submitTransaction("createDiploma"
										, diplomaID
										, nationalID
										, firstName
										, lastName
										, dateOfBirth.toString()
										, placeOfBirth
										, LocalDate.now().toString()
										, institution
										, courseName
										, levelOfStudy
										, degree);
				System.out.println("Successfully created new diploma " + diplomaID);
			}

			c.close();

		} catch (Exception e) {
			System.out.println(
					"ERROR while creating diploma by date of defence: " + e.getMessage());
		}
	}

	private void updateDiploma(	  String diplomaID
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

		try {
			contract.submitTransaction("updateDiploma"
									, diplomaID
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
			System.out.println("Update successful");
		} catch (Exception e) {
			System.out.println("ERROR while updating diploma: " + e.getMessage());
		}
	}

	private void deleteDiploma(String diplomaID) {

		try {
			contract.submitTransaction("deleteDiploma", diplomaID);
			System.out.println("Delete successful");
		} catch (Exception e) {
			System.out.println("ERROR while deleting diploma: " + e.getMessage());
		}
	}
}
