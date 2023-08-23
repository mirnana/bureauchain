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
	private static final String MSP_ID 			= System.getenv().getOrDefault("MSP_ID", "Org2MSP");
	private static final String CHANNEL_NAME 	= System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
	private static final String CHAINCODE_NAME 	= System.getenv().getOrDefault("CHAINCODE_NAME", "diploma");

	// Path to crypto materials.
	private static final Path CRYPTO_PATH = Paths
			.get("../fabric-samples/test-network/organizations/peerOrganizations/org2.example.com");
	// Path to user certificate.
	private static final Path CERT_PATH = CRYPTO_PATH
			.resolve(Paths.get("users/User1@org2.example.com/msp/signcerts/cert.pem"));
	// Path to user private key directory.
	private static final Path KEY_DIR_PATH = CRYPTO_PATH
			.resolve(Paths.get("users/User1@org2.example.com/msp/keystore"));
	// Path to peer tls certificate.
	private static final Path TLS_CERT_PATH = CRYPTO_PATH.resolve(Paths.get("peers/peer0.org2.example.com/tls/ca.crt"));

	// Gateway peer end point.
	private static final String PEER_ENDPOINT = "localhost:9051";
	private static final String OVERRIDE_AUTH = "peer0.org2.example.com";

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
			System.out.println("Enter: a to read all diplomas");
			System.out.println("       n to read diploma by the owner's name");
			System.out.println("       i to read diploma by the owner's national ID");
			System.out.println("       x to exit");

			String str = sc.nextLine();

			if (str.equals("a")) {
				System.out.println(getAllDiplomas());
			} else if (str.equals("n")) {
				System.out.println("Insert first name:");
				String firstName = sc.nextLine();
				System.out.println("Insert last name:");
				String lastName = sc.nextLine();
				System.out.println(readDiplomaByName(firstName, lastName));
			} else if (str.equals("i")) {
				System.out.println("Insert national ID:");
				String nationalID = sc.nextLine();
				System.out.println(readDiplomaByNationalID(nationalID));
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

	private String getAllDiplomas() throws GatewayException {

		var result = contract.evaluateTransaction("getAllDiplomas");
		return prettyJson(result);
	}

	private String readDiplomaByName(String firstName, String lastName) throws GatewayException {

		var result = contract.evaluateTransaction("queryDiplomasByName"
												, firstName
												, lastName);
		return prettyJson(result);
	}

	private String readDiplomaByNationalID(String nationalID) throws GatewayException {

		var result = contract.evaluateTransaction("queryDiplomasByNationalID"
												, nationalID);
		return prettyJson(result);
	}	
}
