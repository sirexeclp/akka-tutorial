package de.hpi.octopus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import de.hpi.octopus.actors.Profiler;
import de.hpi.octopus.actors.Worker;
import de.hpi.octopus.actors.listeners.ClusterListener;

public class OctopusMaster extends OctopusSystem {
	
	public static final String MASTER_ROLE = "master";

	public static void start(String actorSystemName, int workers, String host, int port) {

		final Config config = createConfiguration(actorSystemName, MASTER_ROLE, host, port, host, port);
		
		final ActorSystem system = createSystem(actorSystemName, config);
		
		Cluster.get(system).registerOnMemberUp(new Runnable() {
			@Override
			public void run() {
				system.actorOf(ClusterListener.props(), ClusterListener.DEFAULT_NAME);
			//	system.actorOf(MetricsListener.props(), MetricsListener.DEFAULT_NAME);

				system.actorOf(Profiler.props(), Profiler.DEFAULT_NAME);
				
				for (int i = 0; i < workers; i++)
					system.actorOf(Worker.props(), Worker.DEFAULT_NAME + i);
				
			//	int maxInstancesPerNode = workers; // TODO: Every node gets the same number of workers, so it cannot be a parameter for the slave nodes
			//	Set<String> useRoles = new HashSet<>(Arrays.asList("master", "slave"));
			//	ActorRef router = system.actorOf(
			//		new ClusterRouterPool(
			//			new AdaptiveLoadBalancingPool(SystemLoadAverageMetricsSelector.getInstance(), 0),
			//			new ClusterRouterPoolSettings(10000, workers, true, new HashSet<>(Arrays.asList("master", "slave"))))
			//		.props(Props.create(Worker.class)), "router");
			}
		});
		
		// final Scanner scanner = new Scanner(System.in);
		// String line = scanner.nextLine();
		// scanner.close();
		
		//int attributes = Integer.parseInt(line);
		String[] tmp;
		try {
			tmp = Files.readAllLines(Paths.get("students.csv")).toArray(new String[0]);
			String []  lines = Arrays.copyOfRange(tmp, 1, tmp.length);
			List<String> names = new ArrayList<>(42);
			List<String> secrets = new ArrayList<>(42);
			List<String> sequences = new ArrayList<>(42);
			
			for (String line : lines) {
				String[] lineSplit = line.split(";");
				names.add(lineSplit[1]);
				secrets.add(lineSplit[2]);
				sequences.add(lineSplit[3]);
			}
	
			system.actorSelection("/user/" + Profiler.DEFAULT_NAME).tell(new Profiler.PasswordCrackingTaskMessage(secrets), ActorRef.noSender());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
