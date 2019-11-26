package average;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
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
import java.util.logging.Logger;

public class Link extends Agent {
    FSMBehaviour fsm;

    public AID from;
    public AID to;

    private Double failProbability;
    private Integer delay; // Пусть пока будет постоянной

    Queue<Optional<Double>> queue = new LinkedList<>();

    private static Logger LOGGER;

    @Override
    protected void setup() {
        LOGGER = Logger.getLogger(this.getClass().getName());
        LOGGER.info("Initializing " + getLocalName());

        Object[] args = getArguments();
        from = (AID) args[0];
        to = (AID) args[1];

        failProbability = (Double) args[2];
        delay = (Integer) args[3];

        for (int i = 0; i < delay; ++i) {
            queue.add(Optional.empty());
        }

        fsm = new FSMBehaviour(this);

        SequentialBehaviour seq = new SequentialBehaviour(this);

        ReceiverBehaviour receiver = new ReceiverBehaviour(this, 5000, MessageTemplate.MatchSender(from));
        HandlerBehaviour handler = new HandlerBehaviour(this, receiver) {
            @Override
            public void handle(ACLMessage msg) {
                ACLMessage forward = new ACLMessage();
                forward.addReceiver(to);

                Double value = null;
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
                        forward.setPerformative(ACLMessage.INFORM);
                        //double noise = ThreadLocalRandom.current().nextDouble(-1, 1);
                        //value += noise;
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

        seq.addSubBehaviour(receiver);
        seq.addSubBehaviour(handler);

        fsm.registerFirstState(seq, "ReceiveAndForward");
        fsm.registerDefaultTransition("ReceiveAndForward", "ReceiveAndForward");

        addBehaviour(fsm);
    }
}
