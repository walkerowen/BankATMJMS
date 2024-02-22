Description: Used object-oriented programming, multithreading, JMS, and IBM MQ to facilitate communicatioon between Bank and ATM's.
-Dynamically create ATM's and allow the Bank to create a Queue for P2P messaging to accept deposit and withdraw queries.
-Use Bank's CLI to send publish messages to subscribed ATM's. Ex) increase withdraw fees, announce ealy closure, ban certain users
-Use ATM's CLI to send withdraw and deposit queries to the Bank to alter users' balances.

site that helped set up environment and get started:
https://developer.ibm.com/tutorials/mq-develop-mq-jms/

compile file:
javac -cp ./com.ibm.mq.allclient-9.3.0.0.jar:./javax.jms-api-2.0.1.jar:./json-20220320.jar <FILENAME>.java

run file:
java -cp ./com.ibm.mq.allclient-9.3.0.0.jar:./javax.jms-api-2.0.1.jar:./json-20220320.jar:. <FILENAME>

