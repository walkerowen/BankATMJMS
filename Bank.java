package com.ibm.mq.samples.jms;

import java.io.Console;
import java.util.*;

import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;
import javax.swing.text.DefaultStyledDocument.ElementSpec;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

public class Bank {

    static final String HOST = "localhost"; // Host name or IP address
	static final int PORT = 1414; // Listener port for your queue manager
	static final String CHANNEL = "DEV.APP.SVRCONN"; // Channel name
	static final String QMGR = "QM1"; // Queue manager name
	static final String APP_USER = "app"; // User name that application uses to connect to MQ
	static final String APP_PASSWORD = "passw0rd"; // Password that the application uses to connect to MQ
	
    static JmsFactoryFactory ff = null;
    static JmsConnectionFactory connectionFactory = null;

    

    static HashMap<String,String> activeConnections = new HashMap<String, String>(){{
        put("DEV.QUEUE.4", "");
        put("DEV.QUEUE.3", "");
        put("DEV.QUEUE.2", "");
        put("DEV.QUEUE.1", "");
    }};
    static int numConnections = 0;
    static HashMap<String, Integer> balances = new HashMap<String, Integer>(){{
         put("Bob", 10000);
         put("Alice", 500);
         put("John", 30000);
    }};

    public static void main(String args[]){

        try {
             ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
		     connectionFactory = ff.createConnectionFactory();
        }catch(Exception e){
            System.out.println(e);
        }
        

		try {

            System.out.println("Welcome to Walker's Bank. Use the ATM's to interact.");
            
            //Set up Publisher connection
            IBM_MQ_QUEUE publisher = new IBM_MQ_QUEUE("dev/", "Publisher")
            
            //Set up CLI thread to run
            CommandLineInterpreter CLI = new CommandLineInterpreter(publisher);
            Thread CLI_thread = new Thread(CLI);
            CLI_thread.start();

            //Set up queue 1 for accepting new connections
            IBM_MQ_QUEUE queue1 = new IBM_MQ_QUEUE("DEV.QUEUE.1", "NewConnectionsProcesser");

            //Constantly listen on queue1 for incoming ATM connections
            for(;;){
			    String recievedMessage = queue1.consumer.receiveBody(String.class, 100000000);
                if(!recievedMessage.contains("#")){ //Edge case where one of banks messages gets caught in queue. if message is a DEV.QUEUE.X one, skip it
                    System.out.println("Skip this one");
                    continue;
                }
			    System.out.println("[NEW ATM CONNECTION]: " + recievedMessage);
                
                //Tell ATM to meet on new Queue
                String message = "DEV.QUEUE."+(Bank.numConnections+1);
                queue1.producer.send(queue1.destination, message);
                System.out.println("Sent message: " + message);

                //Start new thread for communication over DEV.QUEUE.X with new ATM
                BankToATM newATM = new BankToATM("DEV.QUEUE." + (Bank.numConnections+1), recievedMessage);
                Thread newATM_Thread = new Thread(newATM);
                newATM_Thread.start();
            }

			//recordSuccess();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

}

class IBM_MQ_QUEUE {
    //static final String QUEUE_NAME = "DEV.QUEUE.1"; // Queue that the application uses to put and get messages to and from
    JMSContext context = null;
	Destination destination = null;
	JMSProducer producer = null;
	JMSConsumer consumer = null;
    String queue_name;

    public IBM_MQ_QUEUE(String queue_name, String description){
        // Variables
		this.context = null;
		this.destination = null;
		this.producer = null;
		this.consumer = null;
        this.queue_name = queue_name;
        Bank.activeConnections.put(queue_name, description);
        if(!queue_name.equals("dev/")){
            Bank.numConnections++;
        }

		try {

			// Set the properties
			Bank.connectionFactory.setStringProperty(WMQConstants.WMQ_HOST_NAME, Bank.HOST);
			Bank.connectionFactory.setIntProperty(WMQConstants.WMQ_PORT, Bank.PORT);
			Bank.connectionFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, Bank.CHANNEL);
			Bank.connectionFactory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
			Bank.connectionFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, Bank.QMGR);
			Bank.connectionFactory.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "Bank (JMS)");
			Bank.connectionFactory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
			Bank.connectionFactory.setStringProperty(WMQConstants.USERID, Bank.APP_USER);
			Bank.connectionFactory.setStringProperty(WMQConstants.PASSWORD, Bank.APP_PASSWORD);
			//Bank.connectionFactory.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "*TLS12ORHIGHER");

			// Create JMS objects
			context = Bank.connectionFactory.createContext();

            if(queue_name.equals("dev/")){
                destination = context.createTopic("dev/");
            }else{
			    destination = context.createQueue("queue:///" + queue_name);
            }
            //create producer
			producer = context.createProducer();

            //create consumer (Unless we're making the publisher topic. Don't need consumer in that case.)
            if(!queue_name.equals("dev/")){ 
			    consumer = context.createConsumer(destination); // autoclosable
            }

            System.out.println("CONNECTED to " + queue_name);
        } catch(Exception e){
            System.out.print(e);
        }
    }
    public void close(){
		System.out.println("DISCONNECTED from " + queue_name + ".");
		context.close();
	}
}

class BankToATM implements Runnable{
    IBM_MQ_QUEUE queue;

    public BankToATM(String queue, String description){
        this.queue = new IBM_MQ_QUEUE(queue, description);
    }
    @Override
    public void run(){
        
        //Listen for messages from ATM#XXX (withdraw/deposit queries)
        for(;;){
            String receivedMessage = queue.consumer.receiveBody(String.class, 100000000);
			System.out.println("Received message: " + receivedMessage);
            String[] parts = receivedMessage.split(" ");
            String n = "new ";

            //Interpret messages. Ex) "withdraw Bob 500" or "deposit Alice 20"
            if(parts.length == 5 && parts[2].equals("withdraw")){
                Bank.balances.put(parts[3], (Bank.balances.get(parts[3]) - Integer.parseInt(parts[4])));
                System.out.println("Withdrew $" + parts[4] + " from " + parts[3] +"'s account.");
            }else if(parts.length == 5 && parts[2].equals("deposit")){
                Bank.balances.put(parts[3], (Bank.balances.get(parts[3]) + Integer.parseInt(parts[4])));
                System.out.println("Deposited $" + parts[4] + " into " + parts[3] +"'s account.");
            }else if(parts.length == 4 && parts[2].equals("balance")){
                System.out.println("Balance check for " + parts[3]+ ": $" + Bank.balances.get(parts[3]));
                n = "";
            }else{
                System.out.println("Invalid Bank Query");
            }
            
        }
    }

    //Helper function to sleep for some time
    public static void wait(int ms){
		try{
        	Thread.sleep(ms);
    	}catch(InterruptedException ex){
        	Thread.currentThread().interrupt();
		}
	}
}

class CommandLineInterpreter implements Runnable{
    IBM_MQ_QUEUE publisher;

    public CommandLineInterpreter(IBM_MQ_QUEUE publisher){
        this.publisher = publisher;
    }

    @Override
    public void run(){
        //Read in input from Console and either print status or send a publish message to ATM's
        for(;;){
            String input = System.console().readLine().trim();
            if(input.equals("status")){
                System.out.println("\nConnections: " + Bank.activeConnections.toString());
                System.out.println("Balances: " + Bank.balances + "\n");
            }else{
                publisher.producer.send(publisher.destination, input);
            }
        }
    }
}


