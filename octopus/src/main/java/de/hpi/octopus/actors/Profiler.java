package de.hpi.octopus.actors;

import java.util.List;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import de.hpi.octopus.actors.Worker.WorkMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class Profiler extends AbstractActor {

	////////////////////////
	// Actor Construction //
	////////////////////////
	
	public static final String DEFAULT_NAME = "profiler";

	public static Props props() {
		return Props.create(Profiler.class);
	}

	////////////////////
	// Actor Messages //
	////////////////////
	@Data @AllArgsConstructor
	public static class RegistrationMessage implements Serializable {
		private static final long serialVersionUID = 4545299661052078209L;
	}

	@Data @SuppressWarnings("unused") @NoArgsConstructor
	public static abstract class TaskMessage implements Serializable {
		private static final long serialVersionUID = -8330958742629706627L;
	}


	@Data
	@AllArgsConstructor
	@SuppressWarnings("unused")
	@NoArgsConstructor
	public static class PasswordCrackingTaskMessage extends TaskMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;
		private final List<String> secrets;
	}

	@Data
	@AllArgsConstructor
	@SuppressWarnings("unused")
	@NoArgsConstructor
	public static class GeneAnalysisTaskMessage extends TaskMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;
		private final List<String> sequences;
	}

	@Data
	@AllArgsConstructor
	@SuppressWarnings("unused")
	@NoArgsConstructor
	public static class LinearCombinationTaskMessage extends TaskMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;
		private final List<Integer> numbers;
	}
	@Data
	@AllArgsConstructor
	@SuppressWarnings("unused")
	@NoArgsConstructor
	public static class HashMiningTaskMessage extends TaskMessage implements Serializable {
		private static final long serialVersionUID = -7643194361868862395L;
		private final List<Integer> partners, prefixes;
		private final int prefixLength;
	}



	
	@Data @AllArgsConstructor @SuppressWarnings("unused") @NoArgsConstructor
	public static abstract class CompletionMessage implements Serializable {
		private static final long serialVersionUID = -6823011111281387872L;
		public enum Status {MINIMAL, EXTENDABLE, FALSE, FAILED}
		private final Status result;
	}

	@Data  @SuppressWarnings("unused") 
	public static class GeneAnalysisCompletionMessage extends CompletionMessage implements Serializable {
		private static final long serialVersionUID = -6823011111281387872L;
		private final List<Integer> partners;

		public GeneAnalysisCompletionMessage(Status result, List<Integer> partners) {
			super(result);
			this.partners = partners;
		}
		public GeneAnalysisCompletionMessage(){super();}
	}


	@Data  @SuppressWarnings("unused")
	public static class PasswordCrackingCompletionMessage extends CompletionMessage implements Serializable {
		private static final long serialVersionUID = -6823011111281387872L;
		private final List<Integer> passwords;
		public PasswordCrackingCompletionMessage(Status result, List<Integer> passwords) {
			super(result);
			this.passwords = passwords;
		}
		public PasswordCrackingCompletionMessage()
		{
			super();		
		}
	}


	@Data @SuppressWarnings("unused") 
	public static class LinearCombinationCompletionMessage extends CompletionMessage implements Serializable {
		private static final long serialVersionUID = -6823011111281387872L;
		private final int [] prefixes;
		public LinearCombinationCompletionMessage(Status result, int [] prefixes) {
			super(result);
			this.prefixes = prefixes;
		}
		public LinearCombinationCompletionMessage()
		{
			super();
		}
	}



	@Data  @SuppressWarnings("unused") 
	public static class HashMiningCompletionMessage extends CompletionMessage implements Serializable {
		private static final long serialVersionUID = -6823011111281387872L;
		private final List<String> hashes;

		public HashMiningCompletionMessage(Status status, List<String> hashes) {
			super(status);
			this.hashes = hashes;
		}
		public HashMiningCompletionMessage()
		{
			super();
		}
	}
	/////////////////
	// Actor State //
	/////////////////
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final Queue<WorkMessage> unassignedWork = new LinkedList<>();
	private final Queue<ActorRef> idleWorkers = new LinkedList<>();
	private final Map<ActorRef, WorkMessage> busyWorkers = new HashMap<>();

	private TaskMessage task;

	////////////////////
	// Actor Behavior //
	////////////////////
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RegistrationMessage.class, this::handle)
				.match(Terminated.class, this::handle)
				.match(PasswordCrackingTaskMessage.class, this::handle)
				.match(PasswordCrackingCompletionMessage.class, this::handle)
				.match(CompletionMessage.class, this::handle)
				.matchAny(object -> this.log.info("Received unknown message: \"{}\"", object.toString()))
				.build();
	}

	private void handle(RegistrationMessage message) {
		this.context().watch(this.sender());
		
		this.assign(this.sender());
		this.log.info("Registered {}", this.sender());
	}
	
	private void handle(Terminated message) {
		this.context().unwatch(message.getActor());
		
		if (!this.idleWorkers.remove(message.getActor())) {
			WorkMessage work = this.busyWorkers.remove(message.getActor());
			if (work != null) {
				this.assign(work);
			}
		}		
		this.log.info("Unregistered {}", message.getActor());
	}
	
	private void handle(PasswordCrackingTaskMessage message) {
		if (this.task != null)
			this.log.error("The profiler actor can process only one task in its current implementation!");
		
		this.task = message;
		this.assign(new  Worker.PasswordCrackingWorkMessage(message.secrets,0,1_000_000));
	}
	private void handle(PasswordCrackingCompletionMessage message) {
		this.log.info("Completed: [{}]", message.getPasswords());
		this.handle((CompletionMessage) message);
	}
	
	private void handle(CompletionMessage message) {
		ActorRef worker = this.sender();
		WorkMessage work = this.busyWorkers.remove(worker);

		this.log.info("Completed: [{},{}]", work.getRangeStart(), work.getRangeEnd());
		
		switch (message.getResult()) {
			case MINIMAL: 
				this.report(work);
				break;
			case EXTENDABLE:
				this.split(work);
				break;
			case FALSE:
				// Ignore
				break;
			case FAILED:
				this.assign(work);
				break;
		}
		
		this.assign(worker);
	}
	
	private void assign(WorkMessage work) {
		ActorRef worker = this.idleWorkers.poll();
		
		if (worker == null) {
			this.unassignedWork.add(work);
			return;
		}
		
		this.busyWorkers.put(worker, work);
		worker.tell(work, this.self());
	}
	
	private void assign(ActorRef worker) {
		WorkMessage work = this.unassignedWork.poll();
		
		if (work == null) {
			this.idleWorkers.add(worker);
			return;
		}
		
		this.busyWorkers.put(worker, work);
		worker.tell(work, this.self());
	}
	
	private void report(WorkMessage work) {
		//this.log.info("UCC: {}", Arrays.toString(work.getX()));
	}

	private void split(WorkMessage work) {
		// int[] x = work.getX();
		// int[] y = work.getY();
		
		// int next = x.length + y.length;
		
		// if (next < this.task.getAttributes() - 1) {
		// 	int[] xNew = Arrays.copyOf(x, x.length + 1);
		// 	xNew[x.length] = next;
		// 	this.assign(new WorkMessage(xNew, y));
			
		// 	int[] yNew = Arrays.copyOf(y, y.length + 1);
		// 	yNew[y.length] = next;
		// 	this.assign(new WorkMessage(x, yNew));
		// }
	}
}