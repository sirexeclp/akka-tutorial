package com.lightbend.akka.sample;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

//#printer-messages
public class PasswortCrackerManager extends AbstractActor {
	// #printer-messages
	public PasswortCrackerManager() {
	}

	static public Props props() {
		return Props.create(PasswortCracker.class, () -> new PasswortCracker());
	}

	// #printer-messages
	static public class CrackingManagerMessage {
		public final List<String> secrets;
		public final int numWorkerActors;
		public final int maxPassword;

		public CrackingTaskMessage(List<String> secrets, int numWorkerActors,int maxPassword) {
			this.secrets = secrets;
			this.numWorkerActors = numWorkerActors;
			this.maxPassword = maxPassword;
		}
	}
	private List<Integer> decrypt(CrackingManagerMessage message) {
		int numThreads = 4;
		//might not cover the whole range!!
		int range = SerialAnalyzer.maxPassword/numThreads;
		if (message.maxPassword % message.numWorkerActors != 0)
		{
			
		}
		return IntStream.range(0, numThreads)
			.parallel()
			.mapToObj(index -> {

				List<Integer> tmp = this.unhash(secrets,index*range,range);
				//System.out.println(tmp);
				return tmp;
			})
			.flatMap(lst -> lst.stream())
			.collect(Collectors.toList());
		//return secrets.stream().map(this::unhash).collect(Collectors.toList());
	}
		
	private String hash(int number) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashedBytes = digest.digest(String.valueOf(number).getBytes("UTF-8"));
			
			StringBuffer stringBuffer = new StringBuffer();
			for (int i = 0; i < hashedBytes.length; i++) {
				stringBuffer.append(Integer.toString((hashedBytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			return stringBuffer.toString();
		}
		catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	// #printer-messages

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(CrackingTaskMessage.class
				,this::unhash)
			.build();
	}
	// #printer-messages
}
// #printer-messages
