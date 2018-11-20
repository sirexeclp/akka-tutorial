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
public class PasswortCracker extends AbstractActor {
	// #printer-messages
	public PasswortCracker() {
	}

	static public Props props() {
		return Props.create(PasswortCrackerManager.class, () -> new PasswortCrackerManager());
	}

	// #printer-messages
	static public class CrackingTaskMessage {
		public final List<String> secrets;
		public final int start;
		public final int range;
		public final int end;

		public CrackingTaskMessage(List<String> secrets, int index, int range) {
			this.secrets = secrets;
			this.start = index * range;
			this.range = range;
			this.end = start+range;
		}
	}
	private List<Integer> unhash(CrackingTaskMessage message) {
		// int result = IntStream.range(0, SerialAnalyzer.maxPassword)
		// 	.parallel()
		// 	.filter(i-> hash(i).equals(hexHash))
		// 	.findFirst()
		// 	.orElseThrow(()-> new RuntimeException("Cracking failed for " + hexHash));
		// 	System.out.println("Cracked PW: "+ result);
		// return result;
		HashMap<String,Integer> rainbowTable = new HashMap<>();
		for (int i = message.start; i < message.end; i++)
		{
			String tmp = this.hash(i);
			rainbowTable.put(tmp, i);
			// if (tmp.equals(hexHash))
			// 	{
			// 		System.out.println("Cracked PW: "+i );
			// 		//return i;
			// 	}
		}
		return message.secrets
			.stream()
			.filter(x->rainbowTable.containsKey(x))
			.map(x->{
				int tmp =rainbowTable.get(x);
				log.info(String.valueOf(tmp));
				return tmp;
			})
			.collect(Collectors.toList());
		// if(rainbowTable.containsKey(hexHash))
		// 	return rainbowTable.get(hexHash);
		//throw new RuntimeException("Cracking failed for " + hexHash);
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
