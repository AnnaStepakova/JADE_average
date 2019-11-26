package average;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Node extends Agent {
    private FSMBehaviour fsm;

    static private Double alpha = 0.1;
    private Integer step = 0;
    private Double value;

    private Double acc; // for sum accumulation

    // Following lists contain AIDs of corresponding links
    private ArrayList<AID> send_to;
    private ArrayList<AID> receive_from;

    private static Logger LOGGER;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        value = (Double) args[0];
        send_to = (ArrayList<AID>) args[1];
        receive_from = (ArrayList<AID>) args[2];

        LOGGER = Logger.getLogger(this.getClass().getName());

        fsm =  new FSMBehaviour(this);

        fsm.registerFirstState(new SendBehaviour(), "Send");
        fsm.registerState(constructReceiver(), "Receive");
        fsm.registerState(new UpdateBehaviour(), "Update");

        fsm.registerDefaultTransition("Send", "Receive");
        fsm.registerDefaultTransition("Receive", "Update");
        fsm.registerDefaultTransition("Update", "Send");
    }

    private class SendBehaviour extends OneShotBehaviour {  //to send messages
        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            String log = myAgent.getLocalName() + " is going to send message \'" + value + "\' to ";
            for (AID aid : send_to) {
                log = log + aid.getLocalName();
                msg.addReceiver(aid);
            }
            LOGGER.log(Level.INFO, log);
            try {
                msg.setContentObject(value);
            } catch (IOException e) {
                e.printStackTrace();
            }
            send(msg);
        }
    }

    //receive messages by parallelizing
    private ParallelBehaviour constructReceiver(){
        ParallelBehaviour parallelBehaviour = new ParallelBehaviour(this, ParallelBehaviour.WHEN_ALL);
        for (AID aid: receive_from) {
            ReceiverBehaviour receiverBehaviour = new ReceiverBehaviour(this, 5000, MessageTemplate.MatchSender(aid));
            HandlerBehaviour handlerBehaviour = new HandlerBehaviour(this, receiverBehaviour) {
                @Override
                public void handle(ACLMessage msg) {
                    try {
                        int p = msg.getPerformative();
                        if (p == ACLMessage.INFORM) {
                            Double y = (Double) msg.getContentObject();
                            LOGGER.info("Receive " + y.toString() + " from " + aid.getLocalName());
                            acc += y - value;
                        }
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                }
            };

            SequentialBehaviour seq = new SequentialBehaviour(this);
            seq.addSubBehaviour(receiverBehaviour);
            seq.addSubBehaviour(handlerBehaviour);

            parallelBehaviour.addSubBehaviour(seq);
        }
        return parallelBehaviour;
    }

    private class UpdateBehaviour extends OneShotBehaviour {

        @Override
        public void action() {
            value = alpha * acc;
            acc = 0.0;
            step++;

            LOGGER.info("Agent " + getLocalName() + " value on step #" + step.toString() +
                    " is " + value.toString());
        }
    }
}
