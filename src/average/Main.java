package average;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.ReceiverBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

//we agree that only one agent of this class with name 'main' should exist
public class Main extends Agent {
    private long startTime;
    private AgentContainer container;
    private ArrayList<AgentController> controllers = new ArrayList<>();  //to start agents

    private static DecimalFormat df5 = new DecimalFormat("#.#####");
    static private Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Override
    protected void setup() {
        startTime = System.currentTimeMillis();
        container = getContainerController();  //to create agents
        //we create binary tree
        //create agents...
        //root - 1st level
        createAgent(21, new ArrayList<AID>(),
                new ArrayList<AID>(Arrays.asList(new AID("agent13", AID.ISLOCALNAME), new AID("agent82", AID.ISLOCALNAME))));
        //2nd level
        createAgent(13, new ArrayList<AID>(Arrays.asList(new AID("agent21", AID.ISLOCALNAME))),
                new ArrayList<>(Arrays.asList(new AID("agent35", AID.ISLOCALNAME), new AID("agent8", AID.ISLOCALNAME))));
        createAgent(82, new ArrayList<AID>(Arrays.asList(new AID("agent21", AID.ISLOCALNAME))),
                new ArrayList<>(Arrays.asList(new AID("agent-2", AID.ISLOCALNAME), new AID("agent17", AID.ISLOCALNAME))));
        //3rd level
        createAgent(35, new ArrayList<AID>(Arrays.asList(new AID("agent13", AID.ISLOCALNAME))), new ArrayList<AID>());
        createAgent(8, new ArrayList<AID>(Arrays.asList(new AID("agent13", AID.ISLOCALNAME))), new ArrayList<AID>());
        createAgent(-2, new ArrayList<AID>(Arrays.asList(new AID("agent82", AID.ISLOCALNAME))), new ArrayList<AID>());
        createAgent(17, new ArrayList<AID>(Arrays.asList(new AID("agent82", AID.ISLOCALNAME))), new ArrayList<AID>());
        //start agents
        try {
            for (AgentController c: controllers)
                c.start();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
        //set behaviour of a main agent (server)
        SequentialBehaviour seq = new SequentialBehaviour(this);
        ReceiverBehaviour receiver = new ReceiverBehaviour(this, -1, null);
        HandlerBehaviour handler = new HandlerBehaviour(this, receiver) {
            @Override
            public void handle(ACLMessage msg) {
                try {
                    Double result = (Double) msg.getContentObject();
                    System.out.println("Result is " + df5.format(result));
                    LOGGER.info("Elapsed time is " + Long.toString(System.currentTimeMillis() - startTime) + "ms");
                    shutDownPlatform();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }
            }
        };
        seq.addSubBehaviour(receiver);
        seq.addSubBehaviour(handler);
        addBehaviour(seq);
    }
    //I know this looks complicated, this part is from Internet)
    private void shutDownPlatform() {
        try {
            Codec codec = new SLCodec();
            Ontology jmo = JADEManagementOntology.getInstance();
            getContentManager().registerLanguage(codec);
            getContentManager().registerOntology(jmo);
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(getAMS());
            msg.setLanguage(codec.getName());
            msg.setOntology(jmo.getName());
            getContentManager().fillContent(msg, new Action(getAID(), new ShutdownPlatform()));
            send(msg);
        }
        catch (Exception e) {}
    }

    public void createAgent(Integer number, ArrayList<AID> send_to, ArrayList<AID> receive_from){
        String agentName = "agent" + number;
        try {
            controllers.add(container.createNewAgent(agentName, "average.MyAgent", new Object[]{number, send_to, receive_from}));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }
}
