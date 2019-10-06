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

public class MyAgent extends Agent {
    private Pair<Integer, Integer> num;  //pair - number and 1
    private ArrayList<AID> send_to;
    private ArrayList<AID> receive_from;

    private static Logger LOGGER;

    private class SendBehaviour extends OneShotBehaviour {  //to send messages
        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            String log = myAgent.getLocalName() + " is going to send message <" + num.first + ", " + num.second + "> to ";
            for (AID aid : send_to) {
                log = log + aid.getLocalName();
                msg.addReceiver(aid);
            }
            LOGGER.log(Level.INFO, log);
            try {
                msg.setContentObject(num);
            } catch (IOException e) {
                e.printStackTrace();
            }
            send(msg);
        }
    }

    //calculate current result
    public synchronized void combine(Pair<Integer, Integer> other) {
        num.first += other.first;
        num.second += other.second;
    }

    @Override
    protected void setup() {
        Object[] args = getArguments();
        num = new Pair<>((Integer) args[0], 1);
        send_to = (ArrayList<AID>) args[1];
        receive_from = (ArrayList<AID>) args[2];

        LOGGER = Logger.getLogger(this.getClass().getName());
        //check different situations, dependent on level of the tree
        SequentialBehaviour seq = new SequentialBehaviour(this);
        if (!send_to.isEmpty() && receive_from.isEmpty()) {
            seq.addSubBehaviour(new SendBehaviour());
        }
        if (!receive_from.isEmpty()) {
            seq.addSubBehaviour(constructReceiver());
            if (!send_to.isEmpty()) {
                seq.addSubBehaviour(new SendBehaviour());
            }
        }
        if (send_to.isEmpty()){
            seq.addSubBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    String log = myAgent.getLocalName() + " is going to send message <" + num.first + ", " + num.second + "> to main";
                    LOGGER.log(Level.INFO, log);
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    try {
                        msg.setContentObject(1.0 * num.first / num.second);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    msg.addReceiver(new AID("main", AID.ISLOCALNAME));
                    send(msg);
                }
            });
        }
        addBehaviour(seq);
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
                        ((MyAgent) myAgent).combine((Pair<Integer, Integer>) msg.getContentObject());
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
}
