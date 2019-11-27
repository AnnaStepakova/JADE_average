package average;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Node extends Agent {
    private FSMBehaviour fsm;

    static private Double alpha = 0.1;
    private Integer step = 0;
    private Double value;

    private Double acc = 0.0; // for sum accumulation
    private ArrayList<Double> history = new ArrayList<>();

    // Following lists contain AIDs of corresponding links
    private ArrayList<AID> send_to;
    private ArrayList<AID> receive_from;

    private static Logger LOGGER = Logger.getLogger(Node.class.getName());
    private static DecimalFormat df5 = new DecimalFormat("#.#####");
    private static boolean printHistory = false;

    @Override
    protected void setup() {
        LOGGER.setLevel(Level.INFO);
        LOGGER.info("Initializing " + getLocalName());

        Object[] args = getArguments();
        value = (Double) args[0];
        send_to = (ArrayList<AID>) args[1];
        receive_from = (ArrayList<AID>) args[2];

        fsm =  new FSMBehaviour(this);

        fsm.registerState(new SendBehaviour(this), "Send");
        fsm.registerState(new ParallelReceiver(this), "Receive");
        fsm.registerFirstState(new UpdateBehaviour(this), "Update");
        fsm.registerLastState(new FinishBehaviour(this), "Finish");

        fsm.registerDefaultTransition("Send", "Receive", new String[] { "Receive" });
        fsm.registerDefaultTransition("Receive", "Update", new String[] { "Update" });
        fsm.registerDefaultTransition("Update", "Send", new String[] { "Send" });

        fsm.registerTransition("Update", "Finish", 100);

        addBehaviour(fsm);
    }

    private class SendBehaviour extends OneShotBehaviour {  //to send messages

        SendBehaviour(Node node) {
            setAgent(node);
        }
        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            StringBuilder log = new StringBuilder();
            log.append(myAgent.getLocalName());
            log.append(" is going to send message \'");
            log.append(value);
            log.append("\' to ");
            for (AID aid : send_to) {
                log.append(aid.getLocalName());
                log.append(", ");
                msg.addReceiver(aid);
            }
            LOGGER.fine(log.toString());
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
                this.addSubBehaviour(b);
            }
        }

        @Override
        public int onEnd() {
            for (SequentialBehaviour b: bs) {
                this.removeSubBehaviour(b);
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
                            LOGGER.fine(myAgent.getLocalName() + " received " +
                                    ACLMessage.getPerformative(msg.getPerformative()) +
                                    " message from " + aid.getLocalName());
                            if (p == ACLMessage.INFORM) {
                                Double y = (Double) msg.getContentObject();
                                LOGGER.fine("Receive " + y.toString() + " from " + aid.getLocalName());
                                acc += y - value;
                                LOGGER.fine("Acc update on " + getLocalName() + ": " + acc.toString());
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
        UpdateBehaviour(Node node) {
            setAgent(node);
        }
        @Override
        public void action() {
            value += alpha * acc;
            acc = 0.0;
            step++;

            if (printHistory) {
                history.add(value);
            }

            LOGGER.fine("Agent " + myAgent.getLocalName() + " value on step #" + step.toString() +
                    " is " + value.toString());
        }

        @Override
        public int onEnd() {
            return step;
        }
    }

    private class FinishBehaviour extends OneShotBehaviour {
        FinishBehaviour(Node node) {
            setAgent(node);
        }
        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.FAILURE);
            for (AID aid: send_to) {
                msg.addReceiver(aid);
            }
            send(msg);

            if (printHistory) {
                StringBuilder log = new StringBuilder();
                log.append("History for ");
                log.append(myAgent.getLocalName());
                log.append(" is [");
                for (Double v: history) {
                    log.append(df5.format(v));
                    log.append(",");
                }
                log.append("]");
                LOGGER.info(log.toString());
            } else {
                LOGGER.info("Result for " + myAgent.getLocalName() + " is " + df5.format(value));
            }

        }
    }
}
