package average;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ReceiverBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Link extends Agent {
    private FSMBehaviour fsm;

    private AID from;
    private AID to;

    private Double failProbability;
    private Integer delay; // Let it be constant

    private boolean enable_noise = true;

    private boolean alive = true;

    private Queue<Optional<Double>> queue = new LinkedList<>();

    private static Logger LOGGER = Logger.getLogger(Link.class.getName());

    @Override
    protected void setup() {
        LOGGER.setLevel(Level.INFO);
        LOGGER.fine("Initializing " + getLocalName());

        Object[] args = getArguments();
        from = (AID) args[0];
        to = (AID) args[1];

        failProbability = (Double) args[2];
        delay = (Integer) args[3];

        for (int i = 0; i < delay; ++i) {
            queue.add(Optional.empty());
        }

        fsm = new FSMBehaviour(this);

        fsm.registerFirstState(new ReceiveAndForwardBehaviour(this), "ReceiveAndForward");
        fsm.registerLastState(new OneShotBehaviour() {
            @Override
            public void action() { }
        }, "Finish");

        fsm.registerDefaultTransition("ReceiveAndForward", "ReceiveAndForward", new String[] { "ReceiveAndForward" });
        fsm.registerTransition("ReceiveAndForward", "Finish", -1);

        addBehaviour(fsm);
    }

    private class ReceiveAndForwardBehaviour extends SequentialBehaviour {
        private ReceiverBehaviour receiver;
        private HandlerBehaviour handler;

        ReceiveAndForwardBehaviour(Link link) {
            setAgent(link);
        }

        @Override
        public void onStart() {
            constructBehaviours();
            addSubBehaviour(receiver);
            addSubBehaviour(handler);
        }

        @Override
        public int onEnd() {
            removeSubBehaviour(receiver);
            removeSubBehaviour(handler);

            if (alive) {
                return fsm.getLastExitValue();
            } else {
                return -1;
            }
        }

        void constructBehaviours() {
            receiver = new ReceiverBehaviour(myAgent, 5000, MessageTemplate.MatchSender(from));
            handler = new HandlerBehaviour(myAgent, receiver) {
                @Override
                public void handle(ACLMessage msg) {
                    int p = msg.getPerformative();
                    if (p == ACLMessage.FAILURE) {
                        alive = false;
                        return;
                    }

                    ACLMessage forward = new ACLMessage(ACLMessage.INFORM);
                    forward.addReceiver(to);

                    Double value;
                    try {
                        value = (Double) msg.getContentObject();
                        queue.add(Optional.of(value));
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }

                    if (queue.isEmpty() || !queue.peek().isPresent()) {
                        forward.setPerformative(ACLMessage.FAILURE);
                        if (!queue.isEmpty()) {
                            queue.poll();
                        }
                    } else {
                        value = queue.poll().get();
                        double random = ThreadLocalRandom.current().nextDouble(0, 1);
                        if (random < failProbability) {
                            forward.setPerformative(ACLMessage.FAILURE);
                        } else {
                            if (enable_noise) {
                                // Simulate noise with suppression using law of large numbers
                                double acc = 0.0;
                                for (int i = 0; i < 100; ++i) {
                                    double noise = ThreadLocalRandom.current().nextDouble(-1, 1);
                                    acc += value + noise;
                                }
                                value = acc / 100;
                            }
                            try {
                                forward.setContentObject(value);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    send(forward);
                }
            };
        }
    }
}
