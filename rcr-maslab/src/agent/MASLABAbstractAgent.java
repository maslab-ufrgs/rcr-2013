package agent;

import Exploration.Exploration;
import agent.interfaces.IAbstractAgent;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.Map;

import model.AbstractMessage;
import model.BurningBuilding;
import model.AbstractMessage;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.Constants;
import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.standard.kernel.comms.StandardCommunicationModel;
import rescuecore2.standard.messages.AKSay;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.standard.messages.AKTell;
import util.Channel;
import util.MASLABBFSearch;
import util.MASLABPreProcessamento;
import util.MASLABRouting;
import util.MASLABSectoring;
import util.MSGType;

/**
 * Abstract base class for sample agents.
 *
 * @param <E> The subclass of StandardEntity this agent wants to control.
 */
public abstract class MASLABAbstractAgent<E extends StandardEntity> extends StandardAgent<E> implements IAbstractAgent {

    /**
     * Variaveis Sample Agent
     */
    private static final int RANDOM_WALK_LENGTH = 50;
    private static final String SAY_COMMUNICATION_MODEL = StandardCommunicationModel.class
            .getName();
    private static final String SPEAK_COMMUNICATION_MODEL = ChannelCommunicationModel.class
            .getName();
    /**
     * The search algorithm.
     */
    protected MASLABBFSearch search;
    /**
     * Whether to use AKSpeak messages or not.
     */
    protected boolean useSpeak;
    /**
     * Cache of building IDs.
     */
    protected List<EntityID> buildingIDs;
    /**
     * Cache of road IDs.
     */
    protected List<EntityID> roadIDs;
    /**
     * Cache of refuge IDs.
     */
    protected List<EntityID> refugeIDs;
    private Map<EntityID, Set<EntityID>> neighbours;
    /**
     *
     * Variaveis definidas por nós
     *
     */
    /**
     * The routing algorithms.
     */
    protected MASLABRouting routing;
    /**
     * Classe de setorização, responsável por pre-processar e carregar os arquivos.
     */
	protected MASLABSectoring sectoring;
	protected Exploration exploration;
	private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
	private static final String MAX_VIEW_KEY = "perception.los.max-distance";
	protected List<BurningBuilding> IncendiosComunicar = new LinkedList<BurningBuilding>(); 

	protected int maxDistance;
	protected int maxView;
    protected int capacidadeCombate = 140;

	
    /**
     * Cache of Hydrant IDs.
     */
    protected List<EntityID> hydrantIDs;
    protected List<EntityID> roadIDsSetor1;
    protected List<EntityID> roadIDsSetor2;
    protected List<EntityID> roadIDsSetor3;
    protected List<EntityID> roadIDsSetor4;
    protected List<EntityID> roadIDsPrincipal;
    protected List<EntityID> Bloqueios;
	protected int PreProcessamento = 0;
	Random rand = new Random();

    /**
     *
     * Métodos Standard Agent
     *
     */
    /**
     * Construct an AbstractSampleAgent.
     */
    protected MASLABAbstractAgent(int pp) {
    	PreProcessamento = pp;
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        buildingIDs = new ArrayList<EntityID>();
        roadIDs = new ArrayList<EntityID>();
        refugeIDs = new ArrayList<EntityID>();
        hydrantIDs = new ArrayList<EntityID>();
        Bloqueios = new ArrayList<EntityID>();
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
        
        // Criação de uma lista com hidrantes e refúgios para os bombeiros
        List<EntityID> waterIDs = new ArrayList<EntityID>();
        waterIDs.addAll(refugeIDs);
        sectoring = new MASLABSectoring(model);
        exploration = new Exploration(model);
		
		maxDistance = config.getIntValue(MAX_DISTANCE_KEY);
	    maxView = config.getIntValue(MAX_VIEW_KEY);

		//Realiza o pre-processamento
		if(PreProcessamento > 0){
			//Esse procedimento está descrito em cada agente pois pode ser iremos realizar diferentes processamentos em cada agente
			
		//Carrega os arquivos já processados
		}else{
			//Cria um objeto da classe de pre-processamento e carrega os arquivos já processados
			MASLABPreProcessamento PreProcess = new MASLABPreProcessamento(model);
			PreProcess.CarregarArquivo();
			
			//Carrega o objeto de setorizacao com as informacoes do arquivo, sem necessadade de setorizar novamente
			sectoring = PreProcess.getMASLABSectoring();

			//Carrega um list dos roads de cada setor para fazer o random walk
			roadIDsSetor1 = new ArrayList<EntityID>(sectoring.getMapSetor(1).keySet()); 
			roadIDsSetor2 = new ArrayList<EntityID>(sectoring.getMapSetor(2).keySet()); 
			roadIDsSetor3 = new ArrayList<EntityID>(sectoring.getMapSetor(3).keySet()); 
			roadIDsSetor4 = new ArrayList<EntityID>(sectoring.getMapSetor(4).keySet()); 
			roadIDsPrincipal = new ArrayList<EntityID>(sectoring.getMapSetor(5).keySet()); 
			
		}
		
		routing = new MASLABRouting(sectoring.getMapSetor(1),
				sectoring.getMapSetor(2),
				sectoring.getMapSetor(3),
				sectoring.getMapSetor(4),
				sectoring.getMapSetor(5),
				roadIDs,
				waterIDs,
				buildingIDs,
				model,
				sectoring.getMapSetorSecundarias(),
				sectoring);
		
        useSpeak = config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(
                SPEAK_COMMUNICATION_MODEL);
        Logger.debug("Communcation model: "
                + config.getValue(Constants.COMMUNICATION_MODEL_KEY));
        Logger.debug(useSpeak ? "Using speak model" : "Using say model");
    }
    /*
     * 
     * Métodos Definidos por nós
     * 
     */
    public void debug(int time, String str) {
        System.out.println(time + " - " + me().getID() + " - " + str);
    }

    public void sendMessage(MSGType type, boolean radio, int time, AbstractMessage mensagens) {
    	sendMessage(type, radio, time, Arrays.asList(mensagens));
    }

    /**
     * Envia uma mensagem
     *
     * @param type - Tipo da mensagem (ver tipos disponíveis)
     * @param radio - indica o meio de comunicação. true = radio e false = voz.
     * @param time - timestep atual.
     * @param params - parametros que compõem a mensagem. Variam de acordo com o
     * type da mensagem.
     */

    public void sendMessage(MSGType type, boolean radio, int time, List<AbstractMessage> mensagens) {

        //inicializa variaveis
        String msg = "";
        Channel channel = null;

        //monta a mensagem em um string
        for (AbstractMessage m: mensagens) {
            msg += m.getMSG();
        }
        //compacta a mensagem IMPLEMENTAR! - huffman ou zip?

        //monta a mensagem de acordo com o tipo e define o canal
        switch (type) {
            //Ex: Informar bloqueio
            case BURNING_BUILDING: {
                channel = Channel.FIRE_BRIGADE;
                break;
            }
            case UNBLOCK_ME: {
                channel = Channel.POLICE_FORCE;
                break;
            }
            case SAVE_ME:
            case BURIED_HUMAN: {
                channel = Channel.AMBULANCE;
                break;
            }
        }
        
        //System.out.println("ID: " + me().getID() + " MSG: " + msg);
        //envia de acordo com o meio (voz, radio)
        if (radio) {
            sendSpeak(time, channel.ordinal(), msg.getBytes());
        } else {
        	sendTell(time, msg.getBytes());
            //sendSay(time, msg.getBytes());
        }
    }

    /**
     * Faz o processamento das mensagens recebidas
     *
     * @param messages - Lista de mensagens recebidas do Kernel
     */
    @Override
    public List<String> heardMessage(Collection<Command> messages) {
        List<String> list = new ArrayList<>();
        for (Command next : messages) {
            if ((next instanceof AKSpeak) && (byteToString(((AKSpeak) next).getContent()).length() > 0)) {
                list.add(byteToString(((AKSpeak) next).getContent()));
                //mensagem de rádio
            } else if ((next instanceof AKSay) && (byteToString(((AKSay) next).getContent()).length() > 0)) {
                list.add(byteToString(((AKSay) next).getContent()));
                //mensagem de voz
            } else if ((next instanceof AKTell) && (byteToString(((AKTell) next).getContent()).length() > 0)) {
                //mensagem de voz também
                list.add(byteToString(((AKTell) next).getContent()));
            }
        }
        return list;
    }

    private String byteToString(byte[] msg) {
        try {
            return new String(msg, "ISO-8859-1");
        } catch (UnsupportedEncodingException ex) {
            return "";
        }
    }
    /*
     * 
     * Métodos Acessores se necessário
     * 
     */
    
    /**
     * @return StandardWorldModel
     */
    public StandardWorldModel getWorldModel(){
    	return model;
    }
    
    /**
     * Returns the object that stores the sectors information
     * @return
     */
    public MASLABSectoring getSectoringInfo(){
    	return sectoring;
    }
    
    public Config getConfig(){
    	return config;
    }
    
    /**
     * Atualiza as informações do ambiente:
     *    - Incêndios;
     *    - Bloqueios;
     *    - Agentes soterrados;
     */
    public void PerceberAmbiente(int time, StandardEntity PosicaoAtual){
    	Collection<StandardEntity> all = model.getObjectsInRange(PosicaoAtual, maxDistance);
    	//Monta a string de problemas
    	String civis, fogo, bloqueios;
    	EntityID local;
    	
    	//Para tudo que estiver no raio de visão, verifica se possui bloqueios ou incêndios, ou civis soterrados
    	for(StandardEntity se: all){
    		civis = "0"; fogo = "0"; bloqueios = "0";
    		local = se.getID();
    		
    		//Se for um building, verifica se está pegando fogo e se tem civis (se conseguir)
    		if(se instanceof Building){
    			if(((Building) se).isOnFire()){
    				fogo = "1";
    			}
    			
    		//Se for uma rua, verifica se existe um bloqueio
    		}else if(se instanceof Road){
    			if(((Road) se).isBlockadesDefined()){
    				if(((Road) se).getBlockades().size() > 0){
    					bloqueios = "1";
    				}
    			}
    		}else if(se instanceof Human){
				Human h = (Human)se;
    			local = h.getPosition();
    			if(h.getID().getValue() != me().getID().getValue()){
    				if (h.isHPDefined() && h.isBuriednessDefined()
    						&& h.isDamageDefined() && h.isPositionDefined()
    						&& h.getHP() > 0
    						&& (h.getBuriedness() > 0 || h.getDamage() > 0)) {
    					civis = "1";
    				}
    			}
    		}
    		
    		//Atualiza a exploração com base no que foi percebido e verifica se precisa enviar uma mensagem
    		if(exploration.InsertNewInformation(time, model.getEntity(local), civis+fogo+bloqueios, 0, 0)){
    			if(civis.equals("1")){
    				
    			}
    			if(fogo.equals("1")){
    				BurningBuilding bb = new BurningBuilding(local.getValue(), time, 0);
    				bb.AtualizarImportancia(CalcularImportancia(bb, PosicaoAtual.getID()));
    			}
    			if(bloqueios.equals("1")){
    				
    			}
    		}
    		
    	}
    }
    
    protected int CalcularImportancia(BurningBuilding bb, EntityID PosicaoAtual){
		int imp = 0;
		int totalAgentes = 0;
		
		for(Integer i : bb.getIDsCorrespondentes()){
			Building b = (Building)model.getEntity(new EntityID(i));
			imp += b.getTotalArea();
		}
		
		Collection<StandardEntity> e = model.getObjectsInRange(PosicaoAtual, maxView);
		for(StandardEntity se : e){
			if(se instanceof FireBrigade){
				totalAgentes += 1;
			}
		}
		
		imp = (imp - (totalAgentes * capacidadeCombate));
		
		if(imp != bb.getImportancia()){
			IncendiosComunicar.add(bb);
		}
		
		return imp;
	}
}
