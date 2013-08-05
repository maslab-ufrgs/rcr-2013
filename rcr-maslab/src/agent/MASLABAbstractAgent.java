package agent;

import agent.interfaces.IAbstractAgent;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Map;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.Constants;
import rescuecore2.log.Logger;

import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.standard.kernel.comms.StandardCommunicationModel;
import util.BFSearch;
import util.MASLABRouting;

/**
   Abstract base class for sample agents.
   @param <E> The subclass of StandardEntity this agent wants to control.
 */
public abstract class MASLABAbstractAgent<E extends StandardEntity> extends StandardAgent<E> implements IAbstractAgent{
    
    /**
     *
     * Variaveis Sample Agent
     * 
     */
    private static final int RANDOM_WALK_LENGTH = 50;

    private static final String SAY_COMMUNICATION_MODEL = StandardCommunicationModel.class.getName();
    private static final String SPEAK_COMMUNICATION_MODEL = ChannelCommunicationModel.class.getName();

    /**
       The search algorithm.
    */
    protected BFSearch search;

    /**
       Whether to use AKSpeak messages or not.
    */
    protected boolean useSpeak;

    /**
       Cache of building IDs.
    */
    protected List<EntityID> buildingIDs;

    /**
       Cache of road IDs.
    */
    protected List<EntityID> roadIDs;

    /**
       Cache of refuge IDs.
    */
    protected List<EntityID> refugeIDs;

    private Map<EntityID, Set<EntityID>> neighbours;

    
    /**
     *
     * Variaveis definidas por nós
     * 
     */
    
    /**
       The routing algorithms.
    */
    protected MASLABRouting routing;    
 
    
    /**
	   Cache of Hydrant IDs.
    */
    protected List<EntityID> hydrantIDs;
    
    
    /**
     *
     * Métodos Standard Agent
     * 
     */
    
    /**
       Construct an AbstractSampleAgent.
    */
    protected MASLABAbstractAgent() {
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        buildingIDs = new ArrayList<EntityID>();
        roadIDs = new ArrayList<EntityID>();
        refugeIDs = new ArrayList<EntityID>();
        for (StandardEntity next : model) {
            if (next instanceof Building) {
                buildingIDs.add(next.getID());
            }
            if (next instanceof Road) {
                roadIDs.add(next.getID());
            }
            if (next instanceof Refuge) {
                refugeIDs.add(next.getID());
            }
            if (next instanceof Hydrant) {
            	hydrantIDs.add(next.getID());
            }
        }
        //Criação de uma lista com hidrantes e refúgios para os bombeiros
        List<EntityID> waterIDs = new ArrayList<EntityID>();
        waterIDs.addAll(refugeIDs);
        waterIDs.addAll(hydrantIDs);

        //TODO - Depois de ter os setores carregados, passar para o construtor do objeto routing 
        //routing = new MASLABRouting(s1, s2, s3, s4, p, refugeIDs, waterIDs, buildingIDs);

        
        search = new BFSearch(model);
        neighbours = search.getGraph();
        useSpeak = config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(SPEAK_COMMUNICATION_MODEL);
        Logger.debug("Communcation model: " + config.getValue(Constants.COMMUNICATION_MODEL_KEY));
        Logger.debug(useSpeak ? "Using speak model" : "Using say model");
    }
    
    /**
     *
     * Métodos Sample Agent
     * 
     */

    /**
       Construct a random walk starting from this agent's current location to a random building.
       @return A random walk.
    */
    protected List<EntityID> randomWalk() {
        List<EntityID> result = new ArrayList<EntityID>(RANDOM_WALK_LENGTH);
        Set<EntityID> seen = new HashSet<EntityID>();
        EntityID current = ((Human)me()).getPosition();
        for (int i = 0; i < RANDOM_WALK_LENGTH; ++i) {
            result.add(current);
            seen.add(current);
            List<EntityID> possible = new ArrayList<EntityID>(neighbours.get(current));
            Collections.shuffle(possible, random);
            boolean found = false;
            for (EntityID next : possible) {
                if (seen.contains(next)) {
                    continue;
                }
                current = next;
                found = true;
                break;
            }
            if (!found) {
                // We reached a dead-end.
                break;
            }
        }
        return result;
    }
    
    /*
     *
     * Métodos Definidos por nós
     * 
     */
    /*
     * 
     * Métodos Acessores se necessário
     * 
     * 
     */
}