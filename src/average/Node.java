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

    static private Double alpha = 0.5;
    private Integer step = 0;
    private Double value;

    private Double acc = 0.0; // for sum accumulation

    // Following lists contain AIDs of corresponding links
    private ArrayList<AID> send_to;
    private ArrayList<AID> receive_from;

    private static Logger LOGGER;

    @Override
    protected void setup() {
        LOGGER = Logger.getLogger(this.getClass().getName());
        LOGGER.info("Initializing " + getLocalName());

        Object[] args = getArguments();
        value = (Double) args[0];
        send_to = (ArrayList<AID>) args[1];
        receive_from = (ArrayList<AID>) args[2];

        fsm =  new FSMBehaviour(this);

        fsm.registerFirstState(new SendBehaviour(this), "Send");
        fsm.registerState(new ParallelReceiver(this), "Receive");
        fsm.registerState(new UpdateBehaviour(this), "Update");
        fsm.registerLastState(new FinishBehaviour(this), "Finish");

        fsm.registerDefaultTransition("Send", "Receive");
        fsm.registerDefaultTransition("Receive", "Update");
        fsm.registerDefaultTransition("Update", "Send");

        fsm.registerTransition("Update", "Finish", 100);

        addBehaviour(fsm);
    }

    private class SendBehaviour extends OneShotBehaviour {  //to send messages

        public SendBehaviour(Node node) {
            setAgent(node);
        }
        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            String log = myAgent.getLocalName() + " is going to send message \'" + value + "\' to ";
            for (AID aid : send_to) {
                log = log + aid.getLocalName();
                msg.addReceiver(aid);
            }
            LOGGER.info(log);
            try {
                msg.setContentObject(value);
            } catch (IOException e) {
                e.printStackTrace();
            }
            send(msg);
        }
    }

    private class ParallelReceiver extends ParallelBehaviour {
        private ArrayList<SequentialBehaviour> bs = new ArrayList<>();

        ParallelReceiver(Node node) {
            setAgent(node);
        }

        @Override
        public void onStart() {
            constructBehaviours();
            for (SequentialBehaviour b: bs) {
                addSubBehaviour(b);
            }
        }

        @Override
        public int onEnd() {
            for (SequentialBehaviour b: bs) {
                removeSubBehaviour(b);
            }
            bs.clear();
            return fsm.getLastExitValue();
        }

        void constructBehaviours() {
            for (AID aid: receive_from) {
                ReceiverBehaviour receiverBehaviour = new ReceiverBehaviour(myAgent, 5000, MessageTemplate.MatchSender(aid));
                HandlerBehaviour handlerBehaviour = new HandlerBehaviour(myAgent, receiverBehaviour) {
                    @Override
                    public void handle(ACLMessage msg) {
                        try {
                            int p = msg.getPerformative();
                            if (p == ACLMessage.INFORM) {
                                Double y = (Double) msg.getContentObject();
                                LOGGER.info("Receive " + y.toString() + " from " + aid.getLocalName());
                                acc += y - value;
                                LOGGER.info("Acc update on " + getLocalName() + ": " + acc.toString());
                            }
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                    }
                };

                SequentialBehaviour seq = new SequentialBehaviour(myAgent);
                seq.addSubBehaviour(receiverBehaviour);
                seq.addSubBehaviour(handlerBehaviour);
                bs.add(seq);
            }
        }
    }

    private class UpdateBehaviour extends OneShotBehaviour {
        public UpdateBehaviour(Node node) {
            setAgent(node);
        }
        @Override
        public void action() {
            value += alpha * acc;
            acc = 0.0;
            step++;

            LOGGER.info("Agent " + myAgent.getLocalName() + " value on step #" + step.toString() +
                    " is " + value.toString());
        }

        @Override
        public int onEnd() {
            return step;
        }
    }

    private class FinishBehaviour extends OneShotBehaviour {
        public FinishBehaviour(Node node) {
            setAgent(node);
        }
        @Override
        public void action() {
            LOGGER.info("Result for agent " + myAgent.getLocalName() + " is " + value.toString());
        }
    }
}
