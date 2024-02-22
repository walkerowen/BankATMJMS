package com.ibm.mq.samples.jms;

import java.io.Console;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

public class ATM {
    // Create variables for the connection to MQ
	static final String HOST = "localhost"; // Host name or IP address
	static final int PORT = 1414; // Listener port for your queue manager
	static final String CHANNEL = "DEV.APP.SVRCONN"; // Channel name
	static final String QMGR = "QM1"; // Queue manager name
	static final String APP_USER = "app"; // User name that application uses to connect to MQ
	static final String APP_PASSWORD = "passw0rd"; // Password that the application uses to connect to MQ
	//static final String QUEUE_NAME = "DEV.QUEUE.1"; // Queue that the application uses to put and get messages to and from

	static JmsFactoryFactory ff = null;
    static JmsConnectionFactory connectionFactory = null;
	static int withdrawFee = 1;
	static long atmID;
	public static void main(String args[]){

		try {
			ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
			connectionFactory = ff.createConnectionFactory();
	   }catch(Exception e){
		   System.out.println(e);
	   }

		try {

			// Set the properties
			// connectionFactory.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST);
			// connectionFactory.setIntProperty(WMQConstants.WMQ_PORT, PORT);
			// connectionFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
			// connectionFactory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
			// connectionFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QMGR);
			// connectionFactory.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "ATM (JMS)");
			// connectionFactory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
			// connectionFactory.setStringProperty(WMQConstants.USERID, APP_USER);
			// connectionFactory.setStringProperty(WMQConstants.PASSWORD, APP_PASSWORD);
			//connectionFactory.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "*TLS12ORHIGHER");

			// Create JMS objects
			//context = connectionFactory.createContext();
			IBM_MQ_Messager queue1 = new IBM_MQ_Messager("DEV.QUEUE.1");
			//Send first message with ATM's ID
			//destination = context.createQueue("queue:///" + QUEUE_NAME);
			//producer = context.createProducer();
			atmID = System.currentTimeMillis() % 1000;
			TextMessage first_message = queue1.context.createTextMessage("ATM#"+atmID);
			queue1.producer.send(queue1.destination, first_message);
			System.out.println("Sent message: " + first_message);
			//Recieve Bank's choice of individual channel
			//consumer = context.createConsumer(destination);
			String receivedMessage = queue1.consumer.receiveBody(String.class, 10000000); // in ms or 15 seconds
			System.out.println("[BANK]: " + receivedMessage);
			queue1.close();

			String newDestination = receivedMessage;
			//Write to the new channel that Bank delgated this ATM

			IBM_MQ_Messager subscriber = new IBM_MQ_Messager("dev/");
			Subscriber sub = new Subscriber(subscriber);
			Thread subscriber_thread = new Thread(sub);
            subscriber_thread.start();


			IBM_MQ_Messager bank_queue = new IBM_MQ_Messager(newDestination);
			//destination = context.createQueue("queue:///" + newDestination);
			//producer = context.createProducer();
			for(;;){
				String input = System.console().readLine(); 
				if(input == "stop" || input == "exit" || input == "quit"){
					System.out.print("Stopping ATM Gracefully");
					bank_queue.context.close();
					System.exit(0);
				}

				//long uniqueNumber = System.currentTimeMillis() % 1000;
				TextMessage message = bank_queue.context.createTextMessage("[ATM#" + ATM.atmID + "] : " + input);
				bank_queue.producer.send(bank_queue.destination, message);
				System.out.println("Sent message:\n" + message);
				if(input.contains("withdraw")){
					System.out.println("(Current withdraw fee: "+ ATM.withdrawFee + ")");
				}
				wait(1000);

				/*consumer = context.createConsumer(destination);
				receivedMessage = consumer.receiveBody(String.class, 15000); // in ms or 15 seconds
				System.out.println(receivedMessage);*/
			}

			//consumer = context.createConsumer(destination); // autoclosable
			//String receivedMessage = consumer.receiveBody(String.class, 15000); // in ms or 15 seconds

			//System.out.println("\nReceived message:\n" + receivedMessage);

			//recordSuccess();
		} catch (Exception e) {
			System.out.println(e);
		}/*finally{
			//queue1.context.close();
		}*/
	}

	public static void wait(int ms){
		try{
        	Thread.sleep(ms);
    	}catch(InterruptedException ex){
        	Thread.currentThread().interrupt();
		}
	}
}

class IBM_MQ_Messager {
	//static final String QUEUE_NAME = "DEV.QUEUE.1"; // Queue that the application uses to put and get messages to and from
	JMSContext context = null;
	Destination destination = null;
	JMSProducer producer = null;
	JMSConsumer consumer = null;
	String queue_name;
	
	public IBM_MQ_Messager(String queue_name){
		// Variables
		this.context = null;
		this.destination = null;
		this.producer = null;
		this.consumer = null;
		this.queue_name = queue_name;
	
		try {
	
			// Set the properties
			ATM.connectionFactory.setStringProperty(WMQConstants.WMQ_HOST_NAME, ATM.HOST);
			ATM.connectionFactory.setIntProperty(WMQConstants.WMQ_PORT, ATM.PORT);
			ATM.connectionFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, ATM.CHANNEL);
			ATM.connectionFactory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
			ATM.connectionFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, ATM.QMGR);
			ATM.connectionFactory.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "ATM#" + ATM.atmID + " (JMS)");
			ATM.connectionFactory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
			ATM.connectionFactory.setStringProperty(WMQConstants.USERID, ATM.APP_USER);
			ATM.connectionFactory.setStringProperty(WMQConstants.PASSWORD, ATM.APP_PASSWORD);
			//ATM.connectionFactory.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "*TLS12ORHIGHER");
	
			// Create JMS objects
			context = ATM.connectionFactory.createContext();
	
			if(queue_name.equals("dev/")){
				destination = context.createTopic("dev/");
			}else{
				destination = context.createQueue("queue:///" + queue_name);
			}
			// long uniqueNumber = System.currentTimeMillis() % 1000;
			// TextMessage message = context.createTextMessage("Your lucky number today is " + uniqueNumber);
			if(!queue_name.equals("dev/")){ //subscriber won't need producer
				producer = context.createProducer();
			}
			// producer.send(destination, message);
			// System.out.println("Sent message:\n" + message);
			
			consumer = context.createConsumer(destination); // autoclosable
			// String receivedMessage = consumer.receiveBody(String.class, 15000); // in ms or 15 seconds
			System.out.println("CONNECTED to " + queue_name + ".");
			// System.out.println("\nReceived message:\n" + receivedMessage);
		} catch(Exception e){
			System.out.print(e);
		}
	}

	public void close(){
		System.out.println("DISCONNECTED from " + queue_name + ".");
		context.close();
	}
}

class Subscriber implements Runnable{
	IBM_MQ_Messager subscriber;

	public Subscriber(IBM_MQ_Messager subscriber){
		this.subscriber = subscriber;
	}

	@Override
	public void run(){
		for(;;){
			String receivedMessage = subscriber.consumer.receiveBody(String.class, 10000000); // in ms or 15 seconds
			System.out.println("[BANK] : " + receivedMessage);

			if(receivedMessage.contains("increase")){
				System.out.println("Withdraw Fee raised to: " + (++ATM.withdrawFee));
			}
		}
	}
}
	


