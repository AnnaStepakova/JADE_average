package average;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ReceiverBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.logging.Level;
import java.util.logging.Logger;
//we use this class to receive messages
public abstract class HandlerBehaviour extends OneShotBehaviour {
    ReceiverBehaviour receiver;

    private static Logger LOGGER;

    public HandlerBehaviour(Agent agent, ReceiverBehaviour receiverBehaviour) {
        myAgent = agent;
        this.receiver = receiverBehaviour;
        LOGGER = Logger.getLogger(myAgent.getClass().getName());
    }

    @Override
    public void action() {
        if (receiver.done()) {
            try {
                ACLMessage msg = receiver.getMessage();
                handle(msg);
            } catch (ReceiverBehaviour.TimedOut | ReceiverBehaviour.NotYetReady e) {
                LOGGER.log(Level.WARNING, myAgent.getLocalName() + ", " + e.getMessage(), e);
            }
        }
    }

    public abstract void handle(ACLMessage msg);
}
