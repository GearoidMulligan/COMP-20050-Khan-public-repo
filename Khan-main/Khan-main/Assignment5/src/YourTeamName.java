import java.util.ArrayList;

/*
 * Team Name : Khan
 */
public class YourTeamName implements Bot {
	/*
	 * Instance Variables Used to help aid the bot
	 */
	private BoardAPI board;
	private PlayerAPI player;
	private int homeContinent;
	private int targetContinent = -1;
	private int myID;
	private int enemyID;
	private boolean gotCard;
	private boolean doneStartOfTurnPrep = false;
	private int reinforcementCounter = 0;
	private int[] CONTINENT_PREFERNECE = {0,3,4,5,1,2};
	
	/*
	 * Scaler and Minimum Values - Edited During Training
	 */
	private double SCALER_DELTA_CONSIDERED_THREAT = 0.85;	//Scaler affects what we consider threat, i.e. if we have 7 troops and enemy has 5 when scaller is 0.7 then they are a threat
	private double SCALER_TAKENOT_UNUSED = 1.5;				//Affects when we decide not to take from a territory not adjacent to a enemy when we can get SCALER_TAKENOT_UNUSED more from another border
	private double SCALER_ABANDON_FORTIFY = 1.2;			//Scaler affects when we decide no not reinforce a territory because we can move more to another threatened territory (probably 1.1-1.5)
	private double SCALER_LEAVE_AFTERWIN = 1.2;				//Scaler affects how many troops to leave behind after conquering a territory in comparison to other surrounding enemy units
	private double REINFORCEMENTS_FOR_TARGET = 3;			//Amount of reinforcements that we allocate for expansion over target Continent exclusively. If 3 then every third will be for target
	private double MIN_ATTACK_HOME = 1;						//Constant is the min percent chance needed for beginning an attack in home territory
	private double MIN_ATTACK_TARGET = 1;					//Constant is the min percent chance needed for beginning an attack in a target continent territory
	private double MIN_ATTACK_OTHER = 19; 					//Constant is the min percent chance needed for beginning an attack in non home or target territories
	private double MIN_ATTACK_WEAK = 1;						//Constant is the min chance needed for beginning an attack during battleWeakAdjacent();
	private boolean AGRESSIVE_MODE = false; 				//Will probably become redundant once we have other attack modes coded
	private boolean EFFICIENT_MODE = false;					//Will cut out some of the more resource heavy operations if bot is too slow
	
	
	/*
	 * Constructor used to set up instance variables
	 */
	YourTeamName (BoardAPI inBoard, PlayerAPI inPlayer) {
		board = inBoard;	
		player = inPlayer;
		myID = player.getId();
		enemyID = (myID+1)%2;
	}
	
	/**
	 * Returns a string representation of our team name
	 * @return : String of Team name
	 */
	public String getName () {
		return("Khan");
	}
	
	/**
	 * Returns a string saying what territories we should reinforce
	 * Works in two parts, if do not control our home continent then we will reinforce that only territories inside that continent
	 * @return : command input for reinforce phase
	 */
	public String getReinforcement () {
		//Will be executed at start of turn
		if(!doneStartOfTurnPrep) {
			gotCard = false; 
			homeContinent = findHomeContinent();
			targetContinent = getTargetContinent(targetContinent);
//			System.out.println("Starting Khan Turn : Home-"+GameData.CONTINENT_NAMES[homeContinent]+" target-"+GameData.CONTINENT_NAMES[targetContinent]);
			doneStartOfTurnPrep = true;
		} 
		
		String command = "";
		
		if(controlContinent(homeContinent)) {
			if(targetContinent!=-1 &&reinforcementCounter%REINFORCEMENTS_FOR_TARGET==0)
				command += getTargetReinforcement();
			else 
				command += getForeignReinforcement();
		} else {
			command += getHomeReinforcment();
		} 
		

		if(command.contains("error")) {
			command = getRandomTerritory();
		} 
		
		command = command.replaceAll("\\s", "");
		command += " 1";
		
		reinforcementCounter++;
		return command;
	}
	
	/**
	 * Returns a String saying what territories the neutral players should reinforce
	 * @return : Command input for Placing neutral reinforcements 
	 */
	public String getPlacement (int forPlayer) {
		String command = "";
		command += decidePlacement(forPlayer);
		if(command.contains("error")) {
			command = getRandomTerritory();
		}
		command = command.replaceAll("\\s", "");
		return(command);
	}
	
	/**
	 * Requests the bot if they wish to trade in their cards.
	 * As we get more cards trading in later we wish to hold cards for as long as we can.
	 * @return : Command input for request to exchange cards
	 */
	public String getCardExchange () {
		String command = "";
		if(player.isForcedExchange()) {
			command = getValidCardCombination();
		} else {
			command = "skip";
		}
		return(command);
	}
	
	/**
	 * Method returns our input for the battle phase
	 * We try to take over home continent first
	 * If we do not get a card this turn, attack a weak neighbor to get one
	 * @return : Command input for the battle phase 
	 */
	public String getBattle () {
		String command = "";
		
		if(controlContinent(homeContinent)) {
			command = battleOutsideHomeContinent();
		} else {
			command = battleWithinHomeContinent();
		}
		//If we have not gotten a card yet, attack a weak neighbor
		if(command.contains("skip")) 
			if(!gotCard||AGRESSIVE_MODE) 
				command = battleWeakAdjacent();
		if(board.isInvasionSuccess())
			gotCard = true;
		return(command);
	}
	
	/**
	 * Method returns our input to a request for number of units to defend with
	 * Bot should defend with max number of units available
	 * @return : Command input for request of # of units to defend with
	 */
	public String getDefence (int countryId) {
		String command = "";
		if(board.getNumUnits(countryId)>1)
			command = "2";
		else 
			command = "1";
		return(command);
	}
	
	
	/**
	 * Method returns input for request of how many troops to move after conquering a territory
	 * Amount moved depends on SCALER_LEAVE_AFTERWIN constant
	 * @return : Command input for moving troops
	 */
	public String getMoveIn (int attackCountryId) {
		gotCard = true;
		String command = "";
		int toMove;
		if(GameData.CONTINENT_IDS[attackCountryId]==homeContinent||GameData.CONTINENT_IDS[attackCountryId]==targetContinent)
			toMove = (int) (board.getNumUnits(attackCountryId)-(enemysAdjacent(attackCountryId)*SCALER_LEAVE_AFTERWIN)-1);
		else
			toMove = board.getNumUnits(attackCountryId)-1;
		if(toMove>0)
			command = Integer.toString(toMove);
		else 
			command = "0";
		return(command);
	}
	
	/**
	 * Method returns input for fortify stage
	 * Check if home continent is under risk of invasion, fortify if so
	 * Otherwise reinforce border.
	 * @return : Command input for fortify stage
	 */
	public String getFortify () {
		//Will execute at end of turn only
		doneStartOfTurnPrep = false;
		
		String command = "";

		if(homeContinentUnderThreat()) {
			command = fortifyHomeContinent();
			if(command.contains("skip"))
				command = fortifyBorder();
		} else { 
			command = fortifyBorder();
		}
			
		return(command);
	}
	
	/*
	 * Following private methods are helper functions for API methods
	 */
	
	/**
	 * Randomly returns a territory name, for when all else fails
	 * @return : String of random territory name
	 */
	private String getRandomTerritory() {
		String command = "";
		command = GameData.COUNTRY_NAMES[(int)(Math.random() * GameData.NUM_COUNTRIES)];
		command = command.replaceAll("\\s", "");
		return(command);
	}
	
	/**
	 * Returns String with a current valid input for trading in cards
	 * Goes in preference from :
	 * 3 of the same insignia
	 * 3 different insignia
	 * 1 wildcard + 2 of the same 
	 * 1 wildcard + 2 different
	 * 2 wildcard + 1 other card
	 * @return
	 */
	private String getValidCardCombination() {
		ArrayList<Card> playerCards=player.getCards();
		int[] cards=new int[4];//4 card types
		
		//filling cards array
		for(int i=0;i<playerCards.size();i++) {
			Card card = playerCards.get(i);
			cards[card.getInsigniaId()]++;
		}
		
		//3 of the same insignia
		if(cards[0]>=3) return "iii";
		if(cards[1]>=3) return "ccc";
		if(cards[2]>=3) return "aaa";
		
		//3 different insignia
		if((cards[0]>=1)&&(cards[1]>=1)&&(cards[2]>=1)) return "ica";
		
		//1 wildcard + 2 of the same 
		if((cards[3]>=1)&&cards[0]>=2) return "wii";
		if((cards[3]>=1)&&cards[1]>=2) return "wcc";
		if((cards[3]>=1)&&cards[2]>=2) return "waa";
		
		//1 wildcard + 2 different
		if((cards[3]>=1)&&(cards[0]+cards[1]+cards[2]>=2)){
			if((cards[0]>=1)&&(cards[1]>=1)) return "wic";
			if((cards[0]>=1)&&(cards[2]>=1)) return"wia";
			if((cards[2]>=1)&&(cards[3]>=1)) return"wca";
		}
		
		//2 wildcard + 1 other card
		if((cards[3]>=2)&&(cards[0]+cards[1]+cards[2]>=1)) {
			if(cards[0]>=1) return "wwi";
			if(cards[1]>=1) return "wwc";
			if(cards[2]>=1) return "wwa";
		}
		
		return "skip";
	}
	
	/**
	 * Decides what continent we are going to try to conquer first and expand out from 
	 * @return : Our home continent, the continent that we are most likely to conquer 
	 */
	private int findHomeContinent() {
		double[] percentWeOwn = {0,0,0,0,0,0} ;
		double[] percentOppOwn = {0,0,0,0,0,0} ;
		double[] contCount = {9,7,12,4,4,6} ;
		
			
		for(int i=0;i<42;i++) {
			if(board.getOccupier(i)==myID) {
				percentWeOwn[GameData.CONTINENT_IDS[i]]++ ;
			}
			if(board.getOccupier(i)== enemyID) {
				percentOppOwn[GameData.CONTINENT_IDS[i]]++ ;
			}
		}
		
		//////////////////////////////////deciding how much weight we give to opponent owned territory, ((percentOppOwn[i])/2) for now
		for(int i=0;i<6;i++) {
			percentWeOwn[i]= (percentWeOwn[i]-((percentOppOwn[i])/2))/(contCount[i]);
		//	percentOppOwn[i]= percentOppOwn[i]/contCount[i];
		}
			
		ArrayList<int[]> ownedContinents = new ArrayList<int[]>();
		int maxPercent = 3 ;
		for(int i=0;i<6;i++) {
			if(percentWeOwn[i]==1){
				int[] tmp = new int[2];
				tmp[1] = i ;
				tmp[0]=getContRank(i) ;
				ownedContinents.add(tmp) ;
				continue ;
			}
			if(percentWeOwn[i]>percentWeOwn[maxPercent]) {
				maxPercent = i ;
			}
		}
		
		if(ownedContinents.isEmpty()) {
			return maxPercent ;
		}else {
			sortArrayListIntArray(ownedContinents) ;
			return ownedContinents.get(0)[1] ;
		}

	}
	
	/**
	 * Called to get the preference ranking of a given continent
	 * @param continentID: The id of the continent we are getting the rank of
	 * @return : the rank of the given continent
	 */
	private int getContRank(int continentID) {
		for(int i=0;i<CONTINENT_PREFERNECE.length;i++) 
			if(CONTINENT_PREFERNECE[i]==continentID)
				return i ;
		return -1 ;
	}
	
	/**
	 * Called to see if we fully control a specified continent
	 * @return : If we control the inputed continent
	 */
	private boolean controlContinent(int inputID) {
		for(int i=0;i<GameData.NUM_COUNTRIES;i++) {
			if(GameData.CONTINENT_IDS[i]==inputID) {
				if(board.getOccupier(i)!=myID) {
					return false ;
				}
			}
		}
		//If we get down here it means we own all the countries in our home continent so we return true
		return true ;
	}
	
	/**
	 * Gets the territory to reinforce, within our home continent
	 * @return : String corresponding to territory we want to reinforce
	 * If no valid territory exists returns error (lower case)
	 */
	private String getHomeReinforcment() {
		
		if(controlContinent(homeContinent)) {
			ArrayList<int[]> homeTerr = new ArrayList<int[]>();
			
			for(int terrCount=0; terrCount<GameData.NUM_COUNTRIES; terrCount++) {
				if(GameData.CONTINENT_IDS[terrCount]==homeContinent) {
					int totalArmies = 0;
					for(int border : GameData.ADJACENT[terrCount]) {
						if(board.getOccupier(border)!=myID)
							totalArmies += board.getNumUnits(border);
					}
					totalArmies -= board.getNumUnits(terrCount);
					homeTerr.add(new int[] {totalArmies, terrCount});
				}
			}
			
			sortArrayListIntArray(homeTerr);
			
			return GameData.COUNTRY_NAMES[homeTerr.get(0)[1]].replaceAll("\\s", "");
		}
		
		boolean enemyPresent=false;
		for(int terrCount=0; terrCount<GameData.NUM_COUNTRIES; terrCount++) {
			if(GameData.CONTINENT_IDS[terrCount]==homeContinent && board.getOccupier(terrCount)==enemyID) {
				enemyPresent = true; 
				break;
			}
		}
		
		if(enemyPresent) {
			ArrayList<int[]> terrArray = new ArrayList<int[]>();
			
			for(int terrCount=0; terrCount<GameData.NUM_COUNTRIES; terrCount++) {
				if(GameData.CONTINENT_IDS[terrCount]==homeContinent && board.getOccupier(terrCount)==myID) {
					int[] temp = new int[2];
					temp[1] = terrCount;
					temp[0] = 0;
					for(int terrBorder : GameData.ADJACENT[terrCount]) {
						if(board.getOccupier(terrBorder)!=myID) {
							temp[0] += board.getNumUnits(terrBorder);
						}
					}
					terrArray.add(temp);
				}
			}
	
			
			if(terrArray.isEmpty()) {
				return "error";
			} else {
				int[] max = {0, 0};
				for(int[] curr : terrArray)
					if(max[0]<curr[0])
						max = curr;
				return GameData.COUNTRY_NAMES[max[1]];
			} 
		} else {
			//If no enemy's present then place armies next to neutrals
			ArrayList<int[]> notOwned = new ArrayList<int[]>();
			for(int terrCount=0; terrCount<GameData.NUM_COUNTRIES; terrCount++) {
				if(GameData.CONTINENT_IDS[terrCount]==homeContinent && board.getOccupier(terrCount)!=myID)
					notOwned.add(new int[] {-1, -1, terrCount});
			}
			
			for(int[] terrTarget : notOwned) {
				for( int terrBorder : GameData.ADJACENT[terrTarget[2]]) {
					if(board.getOccupier(terrBorder)==myID) {
						if(terrTarget[1]==-1 || terrTarget[0] < board.getNumUnits(terrBorder)-board.getNumUnits(terrTarget[2])) {
							terrTarget[0] = board.getNumUnits(terrBorder)-board.getNumUnits(terrTarget[2]);
							terrTarget[1] = terrBorder;
						}
					}
				}
			}
			
			sortArrayListIntArray(notOwned);
			
			for(int[] curr : notOwned) {
				if(curr[1]!=-1)
					return GameData.COUNTRY_NAMES[curr[1]].replaceAll("\\s", "");
			}
			return "error";
		}
	}
	
	/**
	 * Gets territory to reinforce inside target continent, 
	 * If homeContinentUnderThreat, call getHomeReinforcement()
	 * @return : command for reinforcement call
	 */
	private String getTargetReinforcement() {
		if(homeContinentUnderThreat())
			return getHomeReinforcment();
		
		ArrayList<int[]> terrArray = new ArrayList<int[]>();
		
		for(int terrCount=0; terrCount<GameData.NUM_COUNTRIES; terrCount++) {
			if(GameData.CONTINENT_IDS[terrCount]==targetContinent && board.getOccupier(terrCount)==myID) {
				int[] temp = new int[2];
				temp[1] = terrCount;
				temp[0] = 0;
				for(int terrBorder : GameData.ADJACENT[terrCount]) {
					if(board.getOccupier(terrBorder)!=myID && GameData.CONTINENT_IDS[terrBorder]==targetContinent) {
						temp[0] += board.getNumUnits(terrBorder);
					}
				}
				terrArray.add(temp);
			}
		}

		
		if(terrArray.isEmpty()) {
			return "error";
		} else {
			int[] max = {0, 0};
			for(int[] curr : terrArray)
				if(max[0]<curr[0])
					max = curr;
			return GameData.COUNTRY_NAMES[max[1]];
		}
	}
	
	/**
	 * Gets the territory to reinforce, outside our home continent
	 * If homeContinentUnderThreat, call getHomeReinforcement()
	 * 
	 * @return : String corresponding to territory we want to reinforce
	 * If no valid territory exists returns error (lower case)
	 */
	private String getForeignReinforcement() {
		if(homeContinentUnderThreat())
			return getHomeReinforcment();

		ArrayList<int[]> terrArray = new ArrayList<int[]>();
		
		for(int terrCount=0; terrCount<GameData.NUM_COUNTRIES; terrCount++) {
			if(board.getOccupier(terrCount)==myID) {
				int[] temp = new int[2];
				temp[1] = terrCount;
				temp[0] = 0;
				for(int terrBorder : GameData.ADJACENT[terrCount]) {
					if(board.getOccupier(terrBorder)==enemyID) {
						temp[0] += board.getNumUnits(terrBorder);
					}
				}
				terrArray.add(temp);
			}
		}
		if(terrArray.isEmpty()) {
			return "error";
		}else {
			int[] max = {0, 0};
			for(int[] curr : terrArray)
				if(max[0]<curr[0])
					max = curr;
			return GameData.COUNTRY_NAMES[max[1]];
		}
	}
	
	/**
	 * Gets territory to reinforce for neutral player
	 * 
	 * Should not choose a territory in our home continent under any circumstances.
	 * Should priorities territories in continents that pose little resistance for enemy expansion
	 * (i.e. if enemy owns nearly all of one continent then we should block it)
	 * Should prefer to reinforce territories that are near enemy
	 * Should prefer to reinforce territories not near/adjacent home territory
	 * 
	 * @param forPlayer : neutral player we are deciding who to place reinforcement for
	 * @return : territory to reinforce
	 */
	private String decidePlacement(int forPlayer) {
		int[] neutralCountries= {-1,-1,-1,-1,-1,-1};
		int j=0;
		
		//Filling array for neutral countries 
		for(int i=0;i<GameData.NUM_COUNTRIES;i++) {
			if((board.getOccupier(i))==forPlayer) {
				neutralCountries[j]=i;
				j++;
			}
		}
		int sizeNeu=j;
		
		int[] enemyCountries=new int[GameData.NUM_COUNTRIES];
		j=0;
		//filling array for enemyCountries
		for(int i=0;i<GameData.NUM_COUNTRIES;i++) {
			if((board.getOccupier(i))==enemyID) {
				enemyCountries[j]=i;
				j++;
			}
			
		}
		
		int sizeEnemy=j+1; 
		j=0;
		int[] ownedCountries=new int[GameData.NUM_COUNTRIES];
		//filling array for owned countries
		for(int i=0;i<GameData.NUM_COUNTRIES;i++) {
			if((board.getOccupier(i))==myID) {
				ownedCountries[j]=i;
				j++;
			}
			
		}
		
		int sizeOwned=j+1;
		
		int[] neutralVal= new int[sizeNeu];
		
		j=0;
		
		//nested for loop here checks to see if a neutral territory in our home continent, if so it breaks the loop and gives this a low value
		for (int i=0;i<sizeNeu;i++) {
			for( j=0;j<sizeEnemy;j++) {
				if(homeContinent==GameData.CONTINENT_IDS[neutralCountries[i]]) {
					neutralVal[i]=-100;
					break;
				}
				//if neutral territory is adjacent to an enemy territory we increase its odds of being picked
				else{
					if(board.isAdjacent(neutralCountries[i], enemyCountries[j])) {
						neutralVal[i]++;
					}
					//if neutral territory is adjacent to an enemy territory and has has a difference of more than 5 we increase its odds of being picked
					if((board.isAdjacent(neutralCountries[i], enemyCountries[j]))&&(board.getNumUnits(enemyCountries[j])-(board.getNumUnits(neutralCountries[i]))>1)) {
						neutralVal[i]+= (board.getNumUnits(enemyCountries[j])-(board.getNumUnits(neutralCountries[i])));
					}
				}
			}
		}
		
		//if the neutrals territory is in the opponents most populated continent we should increase its value
		for (int i=0;i<sizeNeu;i++) {
			if((GameData.CONTINENT_IDS[neutralCountries[i]])==(findOppContinent())) {
				neutralVal[i]++;
			}
		}
		
		//if the neutrals territory is adjacent to our territory then decrease its odds
		for (int i=0;i<sizeNeu;i++) {
			for( j=0;j<sizeOwned;j++) {
					if(board.isAdjacent(neutralCountries[i], ownedCountries[j])) {
						neutralVal[i]-=10;
					}
				}
			}
		
		int max=neutralCountries[0];
		//finds highest odds neutral
		for (int i=1;i<sizeNeu;i++) {
			if(neutralVal[i]>max) {
				max=neutralCountries[i];
			}
		}

		return GameData.COUNTRY_NAMES[max];
	}
	
	private int findOppContinent() {
		double[] percentWeOwn = new double[6] ;
		int[] contCount = {9,7,12,4,4,6} ;
			
		for(int i=0;i<42;i++) {
			if(board.getOccupier(i)==enemyID) {
				percentWeOwn[GameData.CONTINENT_IDS[i]]++ ;
			}
		}
		for(int i=0;i<6;i++) {
			percentWeOwn[i]= percentWeOwn[i]/contCount[i];
		}
			
		int maxPercent = 3 ;
		for(int i=0;i<6;i++) {
			if(percentWeOwn[i]>percentWeOwn[maxPercent]) {
				maxPercent = i ;
			}
		}
			
			
		return maxPercent ;

	}
	
	
	/**
	 * Called during battle phase when we do not have full control over home continent.
	 * Should output the desired attack that conquers the continent
	 * If no valid attacks make sense we should output "skip"
	 * @return : String representing an attack command 
	 */
	private String battleWithinHomeContinent() {
		
		/*
		 * Create List of possible attacks, we need to look for :
		 * Territories in home continent that we do not control
		 * Territories that border these territories that we control 
		 */
		//Create list of territories in home continent not controlled by us
		ArrayList<Integer> notOwned = new ArrayList<Integer>();
		for(int terrCount=0; terrCount<GameData.NUM_COUNTRIES; terrCount++) {
			if(GameData.CONTINENT_IDS[terrCount]==homeContinent && board.getOccupier(terrCount)!=myID) {
				notOwned.add(terrCount);
			}
		}
		
		//Array list of possible attacks { chance of success, attacking terr, defending terr }
		ArrayList<int[]> possibleAttacks = new ArrayList<int[]>();
		for(int terrEnemy : notOwned) {
			for( int terrBordering : GameData.ADJACENT[terrEnemy]) {
				if(board.getOccupier(terrBordering)==myID) {
					possibleAttacks.add(new int[] {(int) (probabilityOfBattle(board.getNumUnits(terrBordering), board.getNumUnits(terrEnemy))*10), terrBordering, terrEnemy});
				}
			}
		}
		sortArrayListIntArray(possibleAttacks);
//		System.out.println("\n");
//		for(int[] curr : possibleAttacks) {
//			System.out.println(Arrays.toString(curr));
//			System.out.println(Integer.toString(curr[0])+" "+GameData.COUNTRY_NAMES[curr[1]].replaceAll("\\s", "")+" "+board.getNumUnits(curr[1]) +" "+ GameData.COUNTRY_NAMES[curr[2]].replaceAll("\\s", "")+" "+board.getNumUnits(curr[2]));
//		}
		for(int[] curr : possibleAttacks) {
			int unitsToMove = board.getNumUnits(curr[1])-1;
			if(unitsToMove>0 && curr[0]>MIN_ATTACK_HOME) {
				if(unitsToMove<3) // If 1 or 2
					return GameData.COUNTRY_NAMES[curr[1]].replaceAll("\\s", "") +" "+ GameData.COUNTRY_NAMES[curr[2]].replaceAll("\\s", "") +" "+ Integer.toString(unitsToMove);
				return GameData.COUNTRY_NAMES[curr[1]].replaceAll("\\s", "") +" "+ GameData.COUNTRY_NAMES[curr[2]].replaceAll("\\s", "") + " 3";
			}
		}
		//if we get here then there is no possible attack in possibleAttacks
		return battleOutsideHomeContinent();
	}
	
	
	/**
	 * Called when we fully control our home continent
	 * 
	 * Could/Should Take several things into consideration:
	 * If we are close to taking over a continent
	 * If we are taking over a territory of a continent owned by an enemy, stopping them gaining additional troops
	 * If the territory is controlled by neutrals or enemy, priorities killing enemy
	 * If we have a high probability of winning the battle
	 * 
	 * Make a list of possible moves and assign each attack a score based on theses factors
	 * 
	 * If no attractive attack commands are available, output "skip"
	 * @return : String representing an attack command
	 */
	private String battleOutsideHomeContinent() {
		//If we control our target continent then we need a new one
		if(controlContinent(targetContinent))
			targetContinent = getTargetContinent(targetContinent);
		
		ArrayList<int[]> targetAttacks = new ArrayList<int[]>(); //List of attacks for target continent
		for(int terrCount=0; terrCount<GameData.NUM_COUNTRIES; terrCount++) {
			if(GameData.CONTINENT_IDS[terrCount]==targetContinent && board.getOccupier(terrCount)!=myID) {
				for(int terrBorder : GameData.ADJACENT[terrCount]) {
					if(board.getOccupier(terrBorder)==myID && board.getNumUnits(terrBorder)>1) {
						targetAttacks.add(new int[] { (int)(probabilityOfBattle(board.getNumUnits(terrBorder),board.getNumUnits(terrCount))*10), terrBorder, terrCount});
					}
				}
			}
		}
		
		if(!targetAttacks.isEmpty()) {
			
			sortArrayListIntArray(targetAttacks);
			
			for(int[] curr : targetAttacks) {
				int unitsToMove = board.getNumUnits(curr[1])-1;
				if(unitsToMove>0 && curr[0]>MIN_ATTACK_TARGET) {
					if(unitsToMove<3) // If 1 or 2
						return GameData.COUNTRY_NAMES[curr[1]].replaceAll("\\s", "") +" "+ GameData.COUNTRY_NAMES[curr[2]].replaceAll("\\s", "") +" "+ Integer.toString(unitsToMove);
					return GameData.COUNTRY_NAMES[curr[1]].replaceAll("\\s", "") +" "+ GameData.COUNTRY_NAMES[curr[2]].replaceAll("\\s", "") + " 3";
				}
			}
		} 
		if(!EFFICIENT_MODE) {
			
			ArrayList<int[]> possibleAttacks = new ArrayList<int[]>();
			
			for(int terrCount=0; terrCount<GameData.NUM_COUNTRIES; terrCount++) {
				if(board.getOccupier(terrCount)==myID) {
					for(int terrBorder : GameData.ADJACENT[terrCount]) {
						if(board.getOccupier(terrBorder)!=myID) {
							possibleAttacks.add(new int[] { (int)(probabilityOfBattle(board.getNumUnits(terrCount), board.getNumUnits(terrBorder))*10),terrCount, terrBorder});
						}
					}
				}
			}
			
			sortArrayListIntArray(possibleAttacks);
			
			for(int[] curr : possibleAttacks) {
				int unitsToMove = board.getNumUnits(curr[1])-1;
				if(unitsToMove>0 && curr[0]>MIN_ATTACK_OTHER) {
					if(unitsToMove<3) // If 1 or 2
						return GameData.COUNTRY_NAMES[curr[1]].replaceAll("\\s", "") +" "+ GameData.COUNTRY_NAMES[curr[2]].replaceAll("\\s", "") +" "+ Integer.toString(unitsToMove);
					return GameData.COUNTRY_NAMES[curr[1]].replaceAll("\\s", "") +" "+ GameData.COUNTRY_NAMES[curr[2]].replaceAll("\\s", "") + " 3";
				}
			}
			
			
		}
		return "skip";
	}
	
	/**
	 * Called when we have not gotten a card this turn, yet the player wants to skip
	 * We should attack a weak neighbor to get a card. 
	 * Should return attack with most likely success
	 * Should return skip if we have a >50% chance of success
	 * @return : Command to output to battle Phase
	 */
	private String battleWeakAdjacent() {
		
		ArrayList<double[]> borderArray = new ArrayList<double[]>();
		
		//Creates an array of double[3] where { probability of taking over country, our territoryID, enemy territoryID }
		for(int terrCount=0; terrCount<GameData.NUM_COUNTRIES; terrCount++) {
			if(board.getOccupier(terrCount)==myID) {
				for(int borderCount : GameData.ADJACENT[terrCount]) {
					if(board.getOccupier(borderCount)!=myID) {
						double[] temp = new double[3];
						temp[1] = terrCount;
						temp[2] = borderCount;
						temp[0] = probabilityOfBattle(board.getNumUnits(terrCount), board.getNumUnits(borderCount));
						borderArray.add(temp);
					}
				}
			}
		}
		
		double max[] = {0.5,0,0};
		
		for(double[] curr : borderArray ) {
			if(curr[0]>max[0]) {
				max=curr ; 
			}
		}
		
		if(max[0]<MIN_ATTACK_WEAK) {
			return "skip" ;
		}else {
			// Getting the number of troops to enter into the command line
			int noTroops=board.getNumUnits((int) max[1]) ;
			if(noTroops==2) {
				noTroops=1;
		    }else if(noTroops==3) {
				noTroops=2;
			}else {
				noTroops = 3 ;
			}
			
			return GameData.COUNTRY_NAMES[(int) max[1]].replaceAll("\\s", "")+" "+GameData.COUNTRY_NAMES[(int) max[2]].replaceAll("\\s", "")+" "+noTroops ;
	
		}
	}
	
	
	/**
	 * Used to calculate the probability that we will be able to take over a country based on the nubmer of 
	 * units in our country and theirs
	 * @param ourTroop: The number of troops we have on the country we are checking
	 * @param enemyTroop: The number of troops on the country we are trying to take over
	 * 
	 * @return : probability of a successful attack
	 */
	private double probabilityOfBattle(double ourTroop, double enemyTroop) {
		return (ourTroop-enemyTroop)/(double)enemyTroop ;
	}
	
	/**
	 * Used to check if home continent has immediate threat of invasion
	 * If controlHomeContinent() is false then return true;
	 * @return : boolean if home continent is in immediate danger of invasion
	 */
	private boolean homeContinentUnderThreat() {
		if(!controlContinent(homeContinent))
			return true;

		ArrayList<int[]> borderArray = new ArrayList<int[]>();
		
		//Creates an array of int[3] where { delta between troops, our territoryID, enemy territoryID }
		for(int terrCount=0; terrCount<GameData.NUM_COUNTRIES; terrCount++) {
			if(GameData.CONTINENT_IDS[terrCount]==homeContinent) {
				for(int borderCount : GameData.ADJACENT[terrCount]) {
					if(board.getOccupier(borderCount)!=myID) {
						int[] temp = new int[3];
						temp[1] = terrCount;
						temp[2] = borderCount;
						temp[0] = board.getNumUnits(terrCount)-board.getNumUnits(borderCount);
						borderArray.add(temp);
					}
				}
			}
		}
		
		//If there exists a territory with a delta of our troops - there troops bigger than 
		for(int[] curr : borderArray ) {
			if(curr[0]<SCALER_DELTA_CONSIDERED_THREAT*board.getNumUnits(curr[1]))
				return true;
		}
		return false;
	}
	
	/**
	 * Will be called if there is a risk of invasion to our home continent or if we don't control it
	 * 
	 * If we controlHomeContinent() is false then move troops near the territories we don't control
	 * If we do control home continent, move troops to our border most under threat
	 * 
	 * Preferably take troops from the result from getMostOverStaffed(),
	 * 	but if there is another territory offering Significantly (1.5x for example) then take from there regardless,
	 * 	as long as this is not also in the home continent
	 * 
	 * @return : String input for fortify phase
	 */
	private String fortifyHomeContinent() {
		ArrayList<int[]> terrArray = new ArrayList<int[]>();
		
		for(int terrCount=0; terrCount<GameData.NUM_COUNTRIES; terrCount++) {
			if(GameData.CONTINENT_IDS[terrCount]==homeContinent) {
				if(board.getOccupier(terrCount)==myID) {
					int[] temp = new int[2];
					temp[1] = terrCount;
					temp[0] = 0;
					for(int terrBorder : GameData.ADJACENT[terrCount]) {
						if(board.getOccupier(terrBorder)!=myID) {
							temp[0]+= board.getNumUnits(terrBorder);
						}
					}
					temp[0] =- board.getNumUnits(terrCount);
					terrArray.add(temp);
				}
			}
		}
		
		if(terrArray.isEmpty())
			return fortifyBorder();
		
		//Find most under threat
		int[] max = terrArray.get(0);
		for(int[] curr : terrArray) {
			if(curr[0]>max[0])
				max = curr;
		}
		int toMove = getMostOverStaffed(max[1]);
		
		
		
		//Optional move
		int possibleMove = largestConnected(max[1]);
		
		if(toMove==-1 || board.getNumUnits(toMove)<2) //If toMove is not valid
			if(toMove==-1 || possibleMove==-1 ) //If possibleMove is not valid
				return fortifyBorder();
		
		if(possibleMove!=-1 && board.getNumUnits(possibleMove)-enemysAdjacent(possibleMove)>board.getNumUnits(toMove)*SCALER_TAKENOT_UNUSED) { //If possible move is significantly more beneficial than toMove
			return GameData.COUNTRY_NAMES[possibleMove].replaceAll("\\s", "")+" "+GameData.COUNTRY_NAMES[max[1]].replaceAll("\\s", "")+" "+Integer.toString(board.getNumUnits(possibleMove)-enemysAdjacent(possibleMove)/2);
		} else {
			if(board.getNumUnits(toMove)-1<1)
				return fortifyBorder();
			return GameData.COUNTRY_NAMES[toMove].replaceAll("\\s", "")+" "+GameData.COUNTRY_NAMES[max[1]].replaceAll("\\s", "")+" "+Integer.toString((board.getNumUnits(toMove)-1));
		}
	}

	/**
	 * Returns the Territory with the largest delta of units to enemy units
	 * @param terrID : territory that it must be connected to
	 * @return : territory id;
	 */
	private int largestConnected(int terrID) {
		int currentLargest = -1;
		for(int i=0; i<GameData.NUM_COUNTRIES; i++) {
			if(board.getOccupier(i)==myID) {
				if(board.isConnected(terrID, i) && terrID!=i) {
					if(currentLargest==-1) {
						currentLargest=i;
					} else if(board.getNumUnits(i)-enemysAdjacent(i)>board.getNumUnits(currentLargest)-enemysAdjacent(currentLargest)){
						currentLargest = i;
					}
				}
			}
		}
		return currentLargest;
	}
	
	/**
	 * Used to count # of enemy troops adjacent to a territory
	 * @param terrID : territory to check adjacent of
	 * @return : # of units next to inputed territory
	 */
	private int enemysAdjacent(int terrID) {
		int output = 0;
		for(int curr : GameData.ADJACENT[terrID]) {
			if(board.getOccupier(curr)==enemyID)
				output+=board.getNumUnits(terrID);
		}
		return output;
	}
	
	/**
	 * Will be called if there is no immediate risk to home continent
	 * Finds our weakest territory (territory troops - adjacent enemy troops)
	 * use getMostOverStaffed() to find what territory to fortify our weak territory from 
	 * 
	 * For extra effectiveness make list of weakest territories and find how many troops we would be transfering to them.
	 * 	e.g. if we can move 10 troops to second weakest vs moving 2 to our most weakest we should move troops to the second weakest.
	 * 
	 * Alternatively we could find our most "overStaffed" territory regardless and move to its weakest connected
	 * 
	 * If no valid fortify moves exist then return "skip"
	 * @return : command input for fortify phase
	 */
	private String fortifyBorder() {
		ArrayList<int[]> terrArray = new ArrayList<int[]>();
		
		//Find territory to reinforce - Most under threat
		for(int terrCount=0; terrCount<GameData.NUM_COUNTRIES; terrCount++) {
			if(board.getOccupier(terrCount)==myID) {
				int enemysAdjacent = enemysAdjacent(terrCount);
				if(enemysAdjacent>0) {
					int[] temp = new int[2];
					temp[1] = terrCount;
					temp[0] = enemysAdjacent-board.getNumUnits(terrCount); //Bigger number means more threat
					terrArray.add(temp); //Even if threat is negative, still add to array, in case all cases are negative
				}	
			}
		}
		//If no territory needs fortified
		if(terrArray.isEmpty()) {
			return "skip";
		}
		//Get the most under threat
		int[] mostUnderThreat = terrArray.get(0);
		for(int[] curr : terrArray)
			if(mostUnderThreat[0]>curr[0])
				mostUnderThreat = curr;
		
		
		int toMove = getMostOverStaffed(mostUnderThreat[1]);
		
		if(!EFFICIENT_MODE) { //Resource heavy so optional
			//Checks if there is signicantly(SCALER_ABANDON_FORTIFY) more troops that can be moved to a less threatened territory
			//By going in order of most under of threat down we can get the most under threat that we can reinforce the most
			sortArrayListIntArray(terrArray);
			for(int[] curr : terrArray) {
				int tempToMove = getMostOverStaffed(curr[1]);
				if(tempToMove!=-1) {
					if(toMove==-1 || board.getNumUnits(tempToMove)+curr[0]>(board.getNumUnits(toMove)+mostUnderThreat[0])*SCALER_ABANDON_FORTIFY) {
						toMove = tempToMove;
						mostUnderThreat = curr;
					}
				}
			}
		}
		
		if(toMove==-1)
			return "skip";
		
		String output = GameData.COUNTRY_NAMES[toMove].replaceAll("\\s", "")+" ";
		
		output += GameData.COUNTRY_NAMES[mostUnderThreat[1]].replaceAll("\\s", "")+" ";
		output += Integer.toString((board.getNumUnits(toMove)-1));
		
		
		if(board.getNumUnits(toMove)<=1 || output.contains("skip"))
			return "skip";
		return output;
	}
	
	/**
	 * Used as a helper function for fortifyBorder(), sorting ArrayList<int[]>
	 * Uses insertion sort
	 */
	private void sortArrayListIntArray(ArrayList<int[]> input) {
		for(int i=0; i<input.size(); i++) {
			int[] key = input.get(i);
			int j = i-1;
			while(j>=0 && input.get(j)[0] < key[0]){
				input.set(j+1, input.get(j));
				j = j-1;
			}
			input.set(j+1, key);
		}
	}
	
	/**
	 * Returns territory to take from for fortifying
	 * Should look for territory with most troops that is not bordering an enemy territory
	 * Must be linked to territoryID which is the territory we are reinforcing too
	 * 
	 * If such territories exist with >1 troops not bordering enemy territories then return "skip"
	 * 
	 * @return : territory id to fortify from
	 */
	private int getMostOverStaffed(int territoryID) {
		
		//ArrayList of int[2], where { #of units, terrID }
		ArrayList<int[]> terrArray = new ArrayList<int[]>();

		for(int currTerr=0; currTerr<GameData.NUM_COUNTRIES; currTerr++) {
			if(board.getOccupier(currTerr)==myID) {
				if(board.isConnected(territoryID, currTerr) && currTerr!=territoryID) {
					boolean safe = true;
					for(int borderTerr : GameData.ADJACENT[currTerr]) {
						if(board.getOccupier(borderTerr)==enemyID) 
							safe = false;
					}
					if(safe) {
						int[] temp = new int[2];
						temp[1] = currTerr;
						temp[0] = board.getNumUnits(currTerr);
						terrArray.add(temp);
					}
				}
			}
		}
		
		if(terrArray.isEmpty()) {
			return -1;
		}
		
		int[] temp;
		temp = terrArray.get(0);
		for(int[] curr : terrArray) {
			if(temp[0]<curr[0])
				temp = curr;
		}
		return temp[1];
	}
	
	/**
	 * Called at the start of every turn. Returns the target continent, that is not home continent thant we plan to take over next
	 * 
	 * Things to take into consideration
	 * % of the continent that we own
	 * No. of units in the continent in non friendly territories
	 * Maybe something like our troops in continent - not our troops in continent
	 * 
	 * Inputed with the previous target we had, in close situations prefer the old target
	 * Needs to be able to handle -1 (no previous target) as input
	 * 
	 * @param lastTarget : The previous continent that we targeted
	 * @return : ID of new target continent
	 */
	private int getTargetContinent(int lastTarget) {
	
		ArrayList<int[]> newTarget = new ArrayList<int[]>();
		
		for(int i=0; i<6; i++) { newTarget.add(new int[] {0,0}); }
		
		for(int terrCount=0; terrCount<GameData.NUM_COUNTRIES; terrCount++) {
			if(board.getOccupier(terrCount)==myID)
				newTarget.get(GameData.CONTINENT_IDS[terrCount])[0] += board.getNumUnits(terrCount);
			else
				newTarget.get(GameData.CONTINENT_IDS[terrCount])[1] += board.getNumUnits(terrCount);
		}
		
		
		for(int i=0; i<6; i++) {
			newTarget.get(i)[0] = (int)((newTarget.get(i)[0]+15/(double)(newTarget.get(i)[0]+newTarget.get(i)[1]))*100);
			newTarget.get(i)[1] = i;
		}
		
		sortArrayListIntArray(newTarget);
		
		for(int i=0; i<6; i++) {
			if(!controlContinent(newTarget.get(i)[1]) && newTarget.get(i)[1]!=homeContinent) {
				return newTarget.get(i)[1];
			}
		}
		
		return lastTarget;
	}
	

}
