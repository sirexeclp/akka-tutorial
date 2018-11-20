package de.hpi.octopus.actors;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import de.hpi.octopus.OctopusMaster;
import de.hpi.octopus.actors.Profiler.CompletionMessage;
import de.hpi.octopus.actors.Profiler.RegistrationMessage;
import lombok.AllArgsConstructor;
import lombok.Data;

public class Worker extends AbstractActor {

	////////////////////////
	// Actor Construction //
	////////////////////////

	public static final String DEFAULT_NAME = "worker";

	public static Props props() {
		return Props.create(Worker.class);
	}

	////////////////////
	// Actor Messages //
	////////////////////

	@Data
	@AllArgsConstructor
	@SuppressWarnings("unused")
	public static class PasswordCrackingWorkMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;

		private PasswordCrackingWorkMessage() {
		}

		private int rangeStart;
		private int rangeEnd;
		List<String> secrets;
	}

	@Data
	@AllArgsConstructor
	@SuppressWarnings("unused")
	public static class GeneAnalysisWorkMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;

		private GeneAnalysisWorkMessage() {
		}

		private int rangeStart;
		private int rangeEnd;
		List<String> sequences;
	}

	@Data
	@AllArgsConstructor
	@SuppressWarnings("unused")
	public static class LinearCombinationWorkMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;

		private LinearCombinationWorkMessage() {
		}

		private int rangeStart;
		private int rangeEnd;
		List<Integer> numbers;
	}
	@Data
	@AllArgsConstructor
	@SuppressWarnings("unused")
	public static class HashMiningWorkMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;

		private HashMiningWorkMessage() {
		}

		private int rangeStart;
		private int rangeEnd;
		private List<Integer> partners, prefixes;
		private int prefixLength;
	}

	/////////////////
	// Actor State //
	/////////////////

	private final LoggingAdapter log = Logging.getLogger(this.context().system(), this);
	private final Cluster cluster = Cluster.get(this.context().system());

	/////////////////////
	// Actor Lifecycle //
	/////////////////////

	@Override
	public void preStart() {
		this.cluster.subscribe(this.self(), MemberUp.class);
	}

	@Override
	public void postStop() {
		this.cluster.unsubscribe(this.self());
	}

	////////////////////
	// Actor Behavior //
	////////////////////

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(CurrentClusterState.class, this::handle)
				.match(MemberUp.class, this::handle)
				.match(PasswordCrackingWorkMessage.class, this::unhash)
				.match(GeneAnalysisWorkMessage.class, this::match)
				.match(LinearCombinationWorkMessage.class, this::solve)
				.match(HashMiningWorkMessage.class, this::encrypt)
				.matchAny(object -> this.log.info("Received unknown message: \"{}\"", object.toString())).build();
	}
	private void match(GeneAnalysisWorkMessage message) {
		int[] partners = new int[message.sequences.size()];
		for (int i = 0; i < message.sequences.size(); i++)
			partners[i] = this.longestOverlapPartner(i, message.sequences);
				
		this.log.info("done finding partners in Range " + message.rangeStart + "," + message.rangeEnd);
		this.sender().tell(new GeneAnalysisCompletionMessage(CompletionMessage.status.EXTENDABLE,partners), this.self());
	}
	private int [] solve(LinearCombinationWorkMessage message) {
		for (long a = message.rangeStart; a < message.rangeEnd; a++)
		{	
		String binary = Long.toBinaryString(a);
		
			int[] prefixes = new int[62];
			for (int i = 0; i < prefixes.length; i++)
				prefixes[i] = 1;
			
			int i = 0;
			for (int j = binary.length()-1; j >= 0; j--) {
				if(binary.charAt(j) == '1')
					prefixes[i] =-1;
				i++;
			}
			if(this.sum(message.numbers, prefixes)==0)
			{
				//sucess
				this.log.info("done found linearcombination in Range " + message.rangeStart + "," + message.rangeEnd);
				this.sender().tell(new LinearCombinationCompletionMessage(CompletionMessage.status.EXTENDABLE,prefixes), this.self());
			}
			else
			//fail
			{
				this.log.error("no linearcombination found!");
				this.sender().tell(new LinearCombinationCompletionMessage(CompletionMessage.status.FAILED,null), this.self());
			}
		}
	}

	private void handle(CurrentClusterState message) {
		message.getMembers().forEach(member -> {
			if (member.status().equals(MemberStatus.up()))
				this.register(member);
		});
	}

	private void handle(MemberUp message) {
		this.register(message.member());
	}

	private void register(Member member) {
		if (member.hasRole(OctopusMaster.MASTER_ROLE))
			this.getContext().actorSelection(member.address() + "/user/" + Profiler.DEFAULT_NAME)
					.tell(new RegistrationMessage(), this.self());
	}

	private void unhash(PasswordCrackingWorkMessage message) {
		HashMap<String, Integer> rainbowTable = new HashMap<>();
		for (int i = message.rangeStart; i < message.rangeEnd; i++) {
			String tmp = this.hash(i);
			rainbowTable.put(tmp, i);
		}
		List<Integer> passwords = message.secrets.stream()
			.filter(x -> rainbowTable.containsKey(x))
			.map(x -> rainbowTable.get(x))
			.collect(Collectors.toList());
		this.log.info("done finding partners in Range " + message.rangeStart + "," + message.rangeEnd);
		this.sender().tell(new PasswordCrackingCompletionMessage(CompletionMessage.status.EXTENDABLE,passwords), this.self());	
	}
	private List<String> encrypt(HashMiningWorkMessage message) {
		List<String> hashes = new ArrayList<>(message.partners.size());
		for (int i = 0; i < message.partners.length; i++) {
			int partner = message.partners[i];
			String prefix = (message.prefixes[i] > 0) ? "1" : "0";
			hashes.add(this.findHash(partner, prefix, message.prefixLength));
		}
		return hashes;
	}
	
	private String findHash(int content, String prefix, int prefixLength) {
		StringBuilder fullPrefixBuilder = new StringBuilder();
		for (int i = 0; i < prefixLength; i++)
			fullPrefixBuilder.append(prefix);
		
		Random rand = new Random(13);
		
		String fullPrefix = fullPrefixBuilder.toString();
		int nonce = 0;
		while (true) {
			nonce = rand.nextInt();
			String hash = this.hash(content + nonce);
			if (hash.startsWith(fullPrefix))
				return hash;
		}
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
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
}