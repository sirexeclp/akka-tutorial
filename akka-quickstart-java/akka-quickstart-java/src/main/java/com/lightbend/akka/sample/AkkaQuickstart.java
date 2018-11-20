package com.lightbend.akka.sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.lightbend.akka.sample.Greeter.Greet;
import com.lightbend.akka.sample.Greeter.WhoToGreet;
import com.lightbend.akka.sample.PasswortCrackerManager.CrackingTaskMessage;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class AkkaQuickstart {
  public static void main(String[] args) throws IOException {
	final ActorSystem system = ActorSystem.create("helloakka");
	String [] tmp = Files.readAllLines(Paths.get("students.csv")).toArray(new String [0]);
	String [] lines = Arrays.copyOfRange(tmp, 1, tmp.length);
	List<String> names = new ArrayList<>(42);
	List<String> secrets = new ArrayList<>(42);
	List<String> sequences = new ArrayList<>(42);
	
	for (String line : lines) {
		String[] lineSplit = line.split(";");
		names.add(lineSplit[1]);
		secrets.add(lineSplit[2]);
		sequences.add(lineSplit[3]);
	}

    try {
      //#create-actors
      final ActorRef passwortCracker = 
        system.actorOf(PasswortCrackerManager.props(), "printerActor");


	  passwortCracker.tell(new CrackingTaskMessage(secrets,0,500_000), ActorRef.noSender());

	  final ActorRef passwortCracker2 = 
	  system.actorOf(PasswortCrackerManager.props(), "printerActor2");


	passwortCracker2.tell(new CrackingTaskMessage(secrets,1,500_000), ActorRef.noSender());
	      //   final ActorRef howdyGreeter = 
    //     system.actorOf(Greeter.props("Howdy", printerActor), "howdyGreeter");
    //   final ActorRef helloGreeter = 
    //     system.actorOf(Greeter.props("Hello", printerActor), "helloGreeter");
    //   final ActorRef goodDayGreeter = 
    //     system.actorOf(Greeter.props("Good day", printerActor), "goodDayGreeter");
      //#create-actors
      //#main-send-messages
    //   howdyGreeter.tell(new WhoToGreet("Akka"), ActorRef.noSender());
    //   howdyGreeter.tell(new Greet(), ActorRef.noSender());

    //   howdyGreeter.tell(new WhoToGreet("Lightbend"), ActorRef.noSender());
    //   howdyGreeter.tell(new Greet(), ActorRef.noSender());

    //   helloGreeter.tell(new WhoToGreet("Java"), ActorRef.noSender());
    //   helloGreeter.tell(new Greet(), ActorRef.noSender());

    //   goodDayGreeter.tell(new WhoToGreet("Play"), ActorRef.noSender());
    //   goodDayGreeter.tell(new Greet(), ActorRef.noSender());
      //#main-send-messages

      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } catch (IOException ioe) {
    } finally {
      system.terminate();
    }
  }
}
