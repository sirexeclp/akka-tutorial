package de.hpi.octopus.actors;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import lombok.NoArgsConstructor;

public class Worker extends AbstractActor {

	////////////////////////
	// Actor Construction //
	////////////////////////

	public static final String DEFAULT_NAME = "worker";

	public static Props props() {
		return Props.create(Worker.class);
	}
	public Worker()
	{

	}

	////////////////////
	// Actor WorkMessages //
	////////////////////
	@Data
	@SuppressWarnings("unused")
	public abstract static class WorkMessage {
		private final int rangeStart;
		private final int rangeEnd;
		public WorkMessage()
		{
		}
		public WorkMessage(int rangeStart, int rangeEnd)
		{
			this.rangeStart = rangeStart;
			this.rangeEnd = rangeEnd;
		}
	}
	@Data
	@SuppressWarnings("unused")
	public static class PasswordCrackingWorkMessage extends WorkMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;
		private final List<String> secrets;
		public PasswordCrackingWorkMessage(List<String> secrets, int rangeStart, int rangeEnd)
		{
			super(rangeStart,rangeEnd);
			this.secrets = secrets;
		}
		public PasswordCrackingWorkMessage()
		{
			super();
		}
	}
	@Data
	@SuppressWarnings("unused")
	public static class GeneAnalysisWorkMessage extends WorkMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;
		private final List<String> sequences;
		public GeneAnalysisWorkMessage(List<String> sequences,int rangeStart,int rangeEnd) {


			super(rangeStart,rangeEnd);
			this.sequences = sequences;
		}
		public GeneAnalysisWorkMessage()
		{
			super();
		}
	}


	@Data
	@SuppressWarnings("unused")
	public static class LinearCombinationWorkMessage extends WorkMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;
		private final List<Integer> numbers;
		public LinearCombinationWorkMessage(List<Integer> numbers,int rangeStart,int rangeEnd)
		{
			super(rangeStart,rangeEnd);
			this.numbers = numbers;
		}
		public LinearCombinationWorkMessage()
		{
			super();
		}
	}
	@Data
	@SuppressWarnings("unused")
	public static class HashMiningWorkMessage extends WorkMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;
		private final List<Integer> partners;
		private final List<Integer> prefixes;
		private final int prefixLength;
		public HashMiningWorkMessage(List<Integer> partners, List<Integer> prefixes, int prefixLength,int rangeStart,int rangeEnd)
		{
			super(rangeStart,rangeEnd);
			this.partners = partners;
			this.prefixes = prefixes;
			this.prefixLength = prefixLength;
		}
		public HashMiningWorkMessage()
		{
			super();
		}
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
	private void unhash(PasswordCrackingWorkMessage message) {
		HashMap<String, Integer> rainbowTable = new HashMap<>();
		for (int i = message.getRangeStart(); i < message.getRangeEnd(); i++) {
			String tmp = this.hash(i);
			rainbowTable.put(tmp, i);
		}
		List<Integer> passwords = message.secrets.stream()
			.filter(x -> rainbowTable.containsKey(x))
			.map(x -> rainbowTable.get(x))
			.collect(Collectors.toList());
		this.log.info("done finding partners in Range " + message.getRangeStart() + "," + message.getRangeEnd());
		this.sender().tell(new Profiler.PasswordCrackingCompletionMessage(CompletionMessage.Status.EXTENDABLE,passwords), this.self());	
	}
	private int longestOverlapPartner(int thisIndex, List<String> sequences) {
		int bestOtherIndex = -1;
		int bestOverlap = 0;
		for (int otherIndex = 0; otherIndex < sequences.size(); otherIndex++) {
			if (otherIndex == thisIndex)
				continue;
			
			int longestOverlap = //LCSubStr(sequences.get(thisIndex), sequences.get(otherIndex));
			 	this.longestOverlap(sequences.get(thisIndex), sequences.get(otherIndex)).length();

			if (bestOverlap < longestOverlap) {
				bestOverlap = longestOverlap;
				bestOtherIndex = otherIndex;
			}
		}
		return bestOtherIndex;
	}
	private String longestOverlap(String str1, String str2) {
        if (str1.isEmpty() || str2.isEmpty()) 
        	return "";
        
        if (str1.length() > str2.length()) {
            String temp = str1;
            str1 = str2;
            str2 = temp;
        }

        int[] currentRow = new int[str1.length()];
        int[] lastRow = str2.length() > 1 ? new int[str1.length()] : null;
        int longestSubstringLength = 0;
        int longestSubstringStart = 0;

        for (int str2Index = 0; str2Index < str2.length(); str2Index++) {
            char str2Char = str2.charAt(str2Index);
            for (int str1Index = 0; str1Index < str1.length(); str1Index++) {
                int newLength;
                if (str1.charAt(str1Index) == str2Char) {
                    newLength = str1Index == 0 || str2Index == 0 ? 1 : lastRow[str1Index - 1] + 1;
                    
                    if (newLength > longestSubstringLength) {
                    	longestSubstringLength = newLength;
                    	longestSubstringStart = str1Index - (newLength - 1);
                    }
                } else {
                    newLength = 0;
                }
                currentRow[str1Index] = newLength;
            }
            int[] temp = currentRow;
            currentRow = lastRow;
            lastRow = temp;
        }
        return str1.substring(longestSubstringStart, longestSubstringStart + longestSubstringLength);
	}
	private void match(GeneAnalysisWorkMessage message) {
		int[] partners = new int[message.sequences.size()];
		for (int i = 0; i < message.sequences.size(); i++)
			partners[i] = this.longestOverlapPartner(i, message.sequences);
				
		this.log.info("done finding partners in Range " + message.getRangeStart() + "," + message.getRangeEnd());
		this.sender().tell(new Profiler.GeneAnalysisCompletionMessage(CompletionMessage.Status.EXTENDABLE
				,Arrays.stream(partners).boxed().collect(Collectors.toList()))
			, this.self());
	}
	private int sum(List<Integer> numbers, int [] prefixes) {
		return IntStream
			.range(0, numbers.size())
			.map(index-> numbers.get(index)*prefixes[index])
			.sum();
	}
	private void solve(LinearCombinationWorkMessage message) {
		for (long a = message.getRangeStart(); a < message.getRangeEnd(); a++)
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
				this.log.info("done found linearcombination in Range " + message.getRangeStart() + "," + message.getRangeEnd());
				this.sender().tell(new Profiler.LinearCombinationCompletionMessage(CompletionMessage.Status.EXTENDABLE,prefixes), this.self());
			}
			else
			//fail
			{
				this.log.error("no linearcombination found!");
				this.sender().tell(new Profiler.LinearCombinationCompletionMessage(CompletionMessage.Status.FAILED,null), this.self());
			}
		}
	}
	private List<String> encrypt(HashMiningWorkMessage message) {
		List<String> hashes = new ArrayList<>(message.getPartners().size());
		for (int i = 0; i < message.getPartners().size(); i++) {
			int partner = message.partners.get(i);
			String prefix = (message.getPrefixes().get(i) > 0) ? "1" : "0";
			hashes.add(this.findHash(partner, prefix, message.prefixLength));
		}
		return hashes;
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