package de.hpi.octopus.messages;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
@SuppressWarnings("unused")
public abstract class WorkMessage {
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


	@Data
	@SuppressWarnings("unused")
	public static class PasswordCracking extends WorkMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;
		private final List<String> secrets;
		public PasswordCracking(List<String> secrets, int rangeStart, int rangeEnd)
		{
			super(rangeStart,rangeEnd);
			this.secrets = secrets;
		}
		public PasswordCracking()
		{
			super();
		}
	}
	@Data
	@SuppressWarnings("unused")
	public static class GeneAnalysis extends WorkMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;
		private final List<String> sequences;
		public GeneAnalysis(List<String> sequences,int rangeStart,int rangeEnd) {


			super(rangeStart,rangeEnd);
			this.sequences = sequences;
		}
		public GeneAnalysis()
		{
			super();
		}
	}
	
	@Data
	@SuppressWarnings("unused")
	public static class LinearCombination extends WorkMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;
		private final List<Integer> numbers;
		public LinearCombination(List<Integer> numbers,int rangeStart,int rangeEnd)
		{
			super(rangeStart,rangeEnd);
			this.numbers = numbers;
		}
		public LinearCombination()
		{
			super();
		}
	}
	@Data
	@SuppressWarnings("unused")
	public static class HashMining extends WorkMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;
		private final List<Integer> partners;
		private final List<Integer> prefixes;
		private final int prefixLength;
		public HashMining(List<Integer> partners, List<Integer> prefixes, int prefixLength,int rangeStart,int rangeEnd)
		{
			super(rangeStart,rangeEnd);
			this.partners = partners;
			this.prefixes = prefixes;
			this.prefixLength = prefixLength;
		}
		public HashMining()
		{
			super();
		}
	}
}
	