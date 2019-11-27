package average;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

//we agree that only one agent of this class with name 'main' should exist
public class Main extends Agent {
    private AgentContainer container;
    private ArrayList<AgentController> controllers = new ArrayList<>();  //to start agents

    static private Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Override
    protected void setup() {
        container = getContainerController();  //to create agents

//        // Graph for debugging
//        createNode(1, 20.0, new ArrayList<>(Arrays.asList(2)), new ArrayList<>(Arrays.asList(2)));
//        createNode(2, 30.0, new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList(1)));
//        createEdge(1, 2, 0.0, 0);
//        createEdge(2, 1, 0.0, 0);

        createNode(1, 13.0, new ArrayList<>(Arrays.asList(2)), new ArrayList<>(Arrays.asList(5)));
        createNode(2, 21.0, new ArrayList<>(Arrays.asList(3, 4, 5)), new ArrayList<>(Arrays.asList(1)));
        createNode(3, 35.0, new ArrayList<>(Arrays.asList(4)), new ArrayList<>(Arrays.asList(2)));
        createNode(4, -5.0, new ArrayList<>(Arrays.asList(5)), new ArrayList<>(Arrays.asList(2, 3)));
        createNode(5, 82.0, new ArrayList<>(Arrays.asList(1)), new ArrayList<>(Arrays.asList(2, 4)));

//        createEdge(1, 2, 0.0, 0);
//        createEdge(2, 3, 0.0, 0);
//        createEdge(2, 4, 0.0, 0);
//        createEdge(2, 5, 0.0, 0);
//        createEdge(3, 4, 0.0, 0);
//        createEdge(4, 5, 0.0, 0);
//        createEdge(5, 1, 0.0, 0);

        createEdge(1, 2, 0.0, 1);
        createEdge(2, 3, 0.0, 0);
        createEdge(2, 4, 0.25, 0);
        createEdge(2, 5, 0.25, 0);
        createEdge(3, 4, 0.0, 1);
        createEdge(4, 5, 0.0, 0);
        createEdge(5, 1, 0.0, 0);

        //start agents
        try {
            for (AgentController c: controllers)
                c.start();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
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

    private String nodeName(Integer num) {
        return "node" + num.toString();
    }
    private String edgeName(Integer from, Integer to) {
        return "edge" + from.toString() + "-" + to.toString();
    }

    private void createNode(Integer num, Double value, ArrayList<Integer> send_to, ArrayList<Integer> receive_from) {
        String agentName = nodeName(num);

        ArrayList<AID> sendLinks = new ArrayList<>();
        ArrayList<AID> receiveLinks = new ArrayList<>();

        for (Integer j: send_to) {
            sendLinks.add(new AID(edgeName(num, j), AID.ISLOCALNAME));
        }
        for (Integer j: receive_from) {
            receiveLinks.add(new AID(edgeName(j, num), AID.ISLOCALNAME));
        }
        try {
            controllers.add(container.createNewAgent(agentName, "average.Node", new Object[]{value, sendLinks, receiveLinks}));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private void createEdge(Integer from, Integer to, Double failProbability, Integer delay) {
        String agentName = edgeName(from, to);
        try {
            controllers.add(container.createNewAgent(agentName, "average.Link", new Object[]{
                    new AID(nodeName(from), AID.ISLOCALNAME),
                    new AID(nodeName(to), AID.ISLOCALNAME),
                    failProbability, delay
            }));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }
}
