/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

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
import java.util.Scanner;
import java.sql.*;

public final class App {
	private static final String MSP_ID = System.getenv().getOrDefault("MSP_ID", "Org1MSP");
	private static final String CHANNEL_NAME = System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
	private static final String CHAINCODE_NAME = System.getenv().getOrDefault("CHAINCODE_NAME", "diploma");

	// Path to crypto materials.
	private static final Path CRYPTO_PATH = Paths
			.get("../fabric-samples/test-network/organizations/peerOrganizations/org1.example.com");
	// Path to user certificate.
	private static final Path CERT_PATH = CRYPTO_PATH
			.resolve(Paths.get("users/User1@org1.example.com/msp/signcerts/cert.pem"));
	// Path to user private key directory.
	private static final Path KEY_DIR_PATH = CRYPTO_PATH
			.resolve(Paths.get("users/User1@org1.example.com/msp/keystore"));
	// Path to peer tls certificate.
	private static final Path TLS_CERT_PATH = CRYPTO_PATH.resolve(Paths.get("peers/peer0.org1.example.com/tls/ca.crt"));

	// Gateway peer end point.
	private static final String PEER_ENDPOINT = "localhost:7051";
	private static final String OVERRIDE_AUTH = "peer0.org1.example.com";

	// Database connection details
	private static final String DB_URL = "jdbc:mysql://localhost:3306/bureau?useSSL=false&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Europe/Zagreb";
	// 3306 is the default port for MySQL
	private static final String DB_USERNAME = "user";
	private static final String DB_PASSWORD = "password";

	private final Contract contract;
	private final String assetId = "asset" + Instant.now().toEpochMilli();
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
			System.out.println("Enter: s to create a single diploma by student ID");
			System.out.println("       d to create multiple diplomas by date of defence of thesis");
			System.out.println("       x to exit");
			String str = sc.nextLine();
			if (str.equals("s")) {
				System.out.println("Insert student ID:");
				String studentID = sc.nextLine();
				createDiplomaByStudentID(studentID);
			} else if (str.equals("d")) {
				System.out.println("Insert date in format YYYY-MM-dd:");
				String dateStr = sc.nextLine();
				try {
					LocalDate date = LocalDate.parse(dateStr);
					createDiplomasByDateOfDefence(dateStr);
				} catch (DateTimeParseException dtpe) {
					System.out.println("Unable to parse date");
				}
			} else if (str.equals("x"))
				break;
		}
		sc.close();
	}

	/**
	 * Evaluate a transaction to query ledger state.
	 */
	private void getAllAssets() throws GatewayException {
		System.out.println(
				"\n--> Evaluate Transaction: GetAllAssets, function returns all the current assets on the ledger");

		var result = contract.evaluateTransaction("GetAllAssets");

		System.out.println("*** Result: " + prettyJson(result));
	}

	private String prettyJson(final byte[] json) {
		return prettyJson(new String(json, StandardCharsets.UTF_8));
	}

	private String prettyJson(final String json) {
		var parsedJson = JsonParser.parseString(json);
		return gson.toJson(parsedJson);
	}

	/**
	 * Submit a transaction synchronously, blocking until it has been committed to
	 * the ledger.
	 */
	private void createAsset() throws EndorseException, SubmitException, CommitStatusException, CommitException {
		System.out.println(
				"\n--> Submit Transaction: CreateAsset, creates new asset with ID, Color, Size, Owner and AppraisedValue arguments");

		contract.submitTransaction("createDiploma", assetId, "55499041797", "mirna", "imrovic", "1999-02-01", "zagreb",
				"2023-08-15", "unizg", "pmf", "mo", "racunarstvo", "dipl", "mag. math. et inf.", "znanstveni");

		System.out.println("*** Transaction committed successfully");
	}

	/**
	 * Submit transaction asynchronously, allowing the application to process the
	 * smart contract response (e.g. update a UI) while waiting for the commit
	 * notification.
	 */
	private void transferAssetAsync() throws EndorseException, SubmitException, CommitStatusException {
		System.out.println("\n--> Async Submit Transaction: TransferAsset, updates existing asset owner");

		var commit = contract.newProposal("TransferAsset")
				.addArguments(assetId, "Saptha")
				.build()
				.endorse()
				.submitAsync();

		var result = commit.getResult();
		var oldOwner = new String(result, StandardCharsets.UTF_8);

		System.out.println(
				"*** Successfully submitted transaction to transfer ownership from " + oldOwner + " to Saptha");
		System.out.println("*** Waiting for transaction commit");

		var status = commit.getStatus();
		if (!status.isSuccessful()) {
			throw new RuntimeException("Transaction " + status.getTransactionId() +
					" failed to commit with status code " + status.getCode());
		}

		System.out.println("*** Transaction committed successfully");
	}

	private void readAssetById() throws GatewayException {
		System.out.println("\n--> Evaluate Transaction: ReadAsset, function returns asset attributes");

		var evaluateResult = contract.evaluateTransaction("readDiploma", assetId);

		System.out.println("*** Result:" + prettyJson(evaluateResult));
	}

	/**
	 * submitTransaction() will throw an error containing details of any error
	 * responses from the smart contract.
	 */
	private void updateNonExistentAsset() {
		try {
			System.out.println(
					"\n--> Submit Transaction: UpdateAsset asset70, asset70 does not exist and should return an error");

			contract.submitTransaction("UpdateAsset", "diploma1", "55499041797", "mirna", "imrovic", "1999-02-01",
					"zagreb", "2023-08-15", "unizg", "pmf", "mo", "racunarstvo", "preddipl", "mag. math. et inf.",
					"znanstveni");

			System.out.println("******** FAILED to return an error");
		} catch (EndorseException | SubmitException | CommitStatusException e) {
			System.out.println("*** Successfully caught the error: ");
			e.printStackTrace(System.out);
			System.out.println("Transaction ID: " + e.getTransactionId());

			var details = e.getDetails();
			if (!details.isEmpty()) {
				System.out.println("Error Details:");
				for (var detail : details) {
					System.out.println("- address: " + detail.getAddress() + ", mspId: " + detail.getMspId()
							+ ", message: " + detail.getMessage());
				}
			}
		} catch (CommitException e) {
			System.out.println("*** Successfully caught the error: " + e);
			e.printStackTrace(System.out);
			System.out.println("Transaction ID: " + e.getTransactionId());
			System.out.println("Status code: " + e.getCode());
		}
	}

	public void createDiplomaByStudentID(String studentID) throws SQLException, Exception {
		try {
			Connection c = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
			Statement stmt = c.createStatement();

			ResultSet rs = stmt.executeQuery(
					"SELECT nationalID, firstName, lastName, dateOfBirth, placeOfBirth, institutionID " +
							"  FROM student " +
							" WHERE studentID = '" + studentID + "' ;");
			int rowCount = 0;
			if (rs.last()) {
				rowCount = rs.getRow();
			}
			if (rowCount == 0) {
				throw new Exception("Student with studentID " + studentID + " does not exist");
			}
			rs.first(); // only one row expected as 'studentID' is the primary key
			String nationalID = rs.getString("nationalID");
			String firstName = rs.getString("firstName");
			String lastName = rs.getString("lastName");
			Date dateOfBirth = rs.getDate("dateOfBirth");
			String placeOfBirth = rs.getString("placeOfBirth");
			String institutionID = rs.getString("institutionID");
			String institutionID2 = institutionID;

			String institution = "";
			String parentInstitutionID = "";
			boolean firstIteration = true;
			do {
				rs = stmt.executeQuery(
						"SELECT institutionName, parentInstitutionID " +
								"  FROM institution " +
								" WHERE institutionID = '" + institutionID + "' ;");
				rs.first();
				if (firstIteration) {
					firstIteration = false;
				} else {
					institution += ", ";
				}
				institution += rs.getString("institutionName");
				institutionID = parentInstitutionID = rs.getString("parentInstitutionID");
			} while (parentInstitutionID != null);

			rs = stmt.executeQuery(
					"SELECT courseID, degree " +
							"  FROM defenceOfThesis " +
							" WHERE institutionID = '" + institutionID2 + "' " +
							"   AND studentID = '" + studentID + "' " +
							"	AND grade IS NOT NULL " +
							"ORDER BY dueDate DESC, dateOfDefence DESC, seq DESC " +
							"LIMIT 1;");
			rowCount = 0;
			if (rs.last()) {
				rowCount = rs.getRow();
			}
			if (rowCount == 0) {
				throw new Exception("Student with studentID " + studentID + " has not defended any theses");
			}
			rs.first();
			String courseID = rs.getString("courseID");
			String degree = rs.getString("degree");

			rs = stmt.executeQuery(
					"SELECT courseName, levelOfStudy " +
							"  FROM course " +
							" WHERE courseID = '" + courseID + "' " +
							"   AND institutionID = '" + institutionID2 + "' ;");
			rs.first();
			String courseName = rs.getString("courseName");
			String levelOfStudy = rs.getString("levelOfStudy");

			c.close();

			String diplomaID = "diploma" + Instant.now().toEpochMilli();
			contract.submitTransaction("createDiploma", diplomaID, nationalID, firstName, lastName,
					dateOfBirth.toString(), placeOfBirth,
					LocalDate.now().toString(), institution, courseName, levelOfStudy, degree);
			System.out.println("Successfully created new diploma " + diplomaID);

		} catch (Exception e) {
			System.out.println("ERROR creating diploma by ID: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void createDiplomasByDateOfDefence(String dateOfDefence) {
		try {
			Connection c = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
			Statement stmt = c.createStatement();

			ResultSet rs = stmt.executeQuery(
					"SELECT institutionID, courseID, studentID, degree " +
							"  FROM defenceOfThesis " +
							" WHERE dateOfDefence = '" + dateOfDefence + "' " +
							"   AND grade IS NOT NULL;");
			int rowCount = 0;
			if (rs.last()) {
				rowCount = rs.getRow();
				rs.beforeFirst();
			}
			if (rowCount == 0) {
				throw new Exception("No defences found on date " + dateOfDefence);
			}
			while (rs.next()) {
				String institutionID = rs.getString("institutionID");
				String institutionID2 = institutionID;
				String courseID = rs.getString("courseID");
				String studentID = rs.getString("studentID");
				String degree = rs.getString("degree");

				Statement stmt2 = c.createStatement();

				String institution = "";
				String parentInstitutionID = "";
				boolean firstIteration = true;
				do {
					ResultSet rs2 = stmt2.executeQuery(
							"SELECT institutionName, parentInstitutionID " +
									"  FROM institution " +
									" WHERE institutionID = '" + institutionID + "' ;");
					rs2.first();
					if (firstIteration) {
						firstIteration = false;
					} else {
						institution += ", ";
					}
					institution += rs2.getString("institutionName");
					institutionID = parentInstitutionID = rs2.getString("parentInstitutionID");
				} while (parentInstitutionID != null);

				ResultSet rs2 = stmt2.executeQuery(
						"SELECT courseName, levelOfStudy " +
								"  FROM course " +
								" WHERE courseID = " + courseID +
								"   AND institutionID = " + institutionID2 + ";");
				rs2.first();
				String courseName = rs2.getString("courseName");
				String levelOfStudy = rs2.getString("levelOfStudy");

				rs2 = stmt2.executeQuery(
						"SELECT nationalID, firstName, lastName, dateOfBirth, placeOfBirth " +
								"  FROM student " +
								" WHERE studentID = '" + studentID + "' ;");
				rs2.first();
				String nationalID = rs2.getString("nationalID");
				String firstName = rs2.getString("firstName");
				String lastName = rs2.getString("lastName");
				Date dateOfBirth = rs2.getDate("dateOfBirth");
				String placeOfBirth = rs2.getString("placeOfBirth");

				String diplomaID = "diploma" + Instant.now().toEpochMilli();
				contract.submitTransaction("createDiploma", diplomaID, nationalID, firstName, lastName,
						dateOfBirth.toString(), placeOfBirth,
						LocalDate.now().toString(), institution, courseName, levelOfStudy, degree);
				System.out.println("Successfully created new diploma " + diplomaID);
			}

			c.close();

		} catch (Exception e) {
			System.out.println("ERROR creating diploma by date of defence: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
