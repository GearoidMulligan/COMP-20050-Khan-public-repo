package controller;

import model.Constants;
import model.Model;
import view.View;
/**
 * Controller Class contains a View and Model Object 
 * 
 */
public class Controller {
	
	public View theView;
	public Model theModel;
	private int[] playerOrder;
	
	public Controller(View theView, Model theModel) {
		this.theView = theView;
		this.theModel = theModel;
		
		playerOrder = new int[Constants.NUM_PLAYERS];
		
		//We may ask if players are loading a save file here
		createNewGame();
		
	}
	
	/**
	 * Initializes Game Objects
	 */
	public void createNewGame() {
		//Create Player Objects
		String[] playerNames = theView.getPlayerNames();
		theModel.createPlayers(playerNames);
		
		//Create Country objects
		theModel.createCountries();
		theView.refresh(theModel.getCountryArray());
		
		//Allocate 9 countries to each player
		theModel.allocateCountries();
		theView.refresh(theModel.getCountryArray());
		
		//Roll Die to see who goes first
		theModel.choosePlayerOrder(playerOrder);
		
		//Ask Players to place reinforcements
		chooseInitialReinforcements();
		
		//Run Game Loop
		int gameWinnerID = runGameLoop();

		//DisplayWinner
		theView.displayString("\n\n\n\n\nGAME OVER\n============================\nWinner is Player "+(gameWinnerID+1)+" ("+theModel.getPlayerArray()[gameWinnerID].getName()+")");
		theView.displayWinner(theModel.getPlayerArray()[gameWinnerID]);
		
	}
	
	/**
	 * Asks users to choose what to reinforce
	 */
	public void chooseInitialReinforcements() {
		//We ask every player 9 times
		for(int i=0; i<9; i++) {
			//Ask players in order
			for(int j=0; j<Constants.NUM_PLAYERS; j++) {
				String userInput;
				int userInputId = -1;
				//Prompt User for input
				theView.displayString("\nPlayer "+(playerOrder[j]+1)+" chose what country to reinforce :");
				
				//Check that input is valid
				do {
					userInput = theView.getCommand();
					userInputId = theModel.findCountry(userInput);
					if(userInputId<0) {
						theView.displayValidity(userInputId, userInput);
					} else if(theModel.isPlayerOwned(playerOrder[j], userInputId)==false) { //Checking to see if player owns the country
						theView.displayString("Player "+theModel.getPlayerArray()[playerOrder[j]].getName()+" does not own "+theModel.getCountryArray()[userInputId].getCountryName()+". Please enter another Country :");
						userInputId=-1;
					}
				} while(userInputId<0);
				
				//Increase army count on selected country
				theModel.reinforce(userInputId, 3);
				//Refresh game board
				theView.refresh(theModel.getCountryArray());
			}
		}
	}
	
	/**
	 * Game loop that goes through the phases of the game in the player order.
	 * @return Returns the winning players ID
	 */
	public int runGameLoop() {
		int turncount = 0;
		//for now hard stop at 100 turns while testing, set to while(true) for game release
		while(true) {	
			turncount++;
			theView.displayString("Turn "+turncount);
			//Run each turn for each player. i goes from 1-NUM_PLAYERS
			for(int turnID : playerOrder) {
				reinforcePhase(turnID, theModel.calculateReinforcements(turnID));
				attackPhase(turnID);
				//After attackPhase check if player has won
				if(theModel.isWinner(turnID))
					return turnID;
				fortifyPhase(turnID);
			}
		}
	}
	
	/**
	 * Gets the player to place reinforcements
	 * @param inputPlayerId : Player placing reinforcements
	 * @param numberOfReinforcements : Number of armies to place
	 */
	public void reinforcePhase(int inputPlayerID, int numberOfReinforcements) {
		
		int selectedNoReinforcements = 0;
		String selectedCountryName;
		int selectedCountryId = 0;
		String input;
		
		numberOfReinforcements += requestTrade(inputPlayerID);
		
		while(numberOfReinforcements>0) {
			
			do {
				theView.displayString("Player "+(inputPlayerID+1)+" ("+theModel.getPlayerArray()[inputPlayerID].getName()+ "): has "+numberOfReinforcements+" reinforcements to place:\nPlayer "+(inputPlayerID+1)+" ("+theModel.getPlayerArray()[inputPlayerID].getName()+"): REINFORCE: Enter a country to reinforce and the number of units you wish to place on this country" );

				// Getting input from player
				input = theView.getCommand().trim() ;
				
				// Checking if the user has inputed nothing 
				if(input.isEmpty() ) {
					theView.displayString("No parameters entered");
					continue ;
				}
				// Checks if the user has only entered one parameter
				if(input.contains(" ")==false) {
					theView.displayString("Not enough parameters");
					continue ;
				}
			
				// Splitting input into the entered country and number of reinforcements to be placed
				selectedCountryName = input.substring(0, (input.lastIndexOf(' ')) ) ;
				try {
					selectedNoReinforcements = Integer.parseInt(input.substring(input.lastIndexOf(' ')+1));
				 }catch (NumberFormatException e){
					selectedNoReinforcements = -1 ;
				}
				
				selectedCountryId = theModel.findCountry(selectedCountryName);
				
				// Checks if the inputed country is valid and owned by player
				if(selectedCountryId<0) {
					theView.displayValidity(selectedCountryId, selectedCountryName);
				} else if(theModel.isPlayerOwned(inputPlayerID, selectedCountryId)==false) { //Checking to see if player owns the country
					theView.displayString("Player "+(inputPlayerID+1)+" ("+theModel.getPlayerArray()[inputPlayerID].getName()+"): does not own "+theModel.getCountryArray()[selectedCountryId].getCountryName()+". Please enter again:");
					selectedCountryId=-1;
				} else if(selectedNoReinforcements<1 || selectedNoReinforcements>numberOfReinforcements){ // If valid Country is chosen this makes sure a valid number of reinforcements is chosen
					theView.displayString("Invalid number of reinforcements! Please enter a number between 1 and "+numberOfReinforcements);
					selectedCountryId=-1;
					continue;
				}	
			} while(selectedCountryId<0);
			
			//Increase army count on selected country
			theModel.reinforce(selectedCountryId, selectedNoReinforcements);
			//Refresh game board
			theView.refresh(theModel.getCountryArray());
			//Update the number of reinforcements the player has left to place
			numberOfReinforcements -= selectedNoReinforcements ;
		}
	}
	
	/**
	 * Repeat Attack Phase until player decides to continue
	 * @param inputPlayerID : ID of player who is attacking
	 */
	public void attackPhase(int inputPlayerID) {
		String prefixString = "Player "+(inputPlayerID+1)+" ("+theModel.getPlayerArray()[inputPlayerID].getName()+"):";
		String userInput, firstWord, secondWord, thirdWord; //Used to store user input, and again individually
		int index1, index2; //Used to track 
		int firstCountryID, secondCountryID, armiesToUse, defendingArmies=0;
		boolean cardRecieved = false; //Track if the player has recieved a card this turn already
		
		do {
			//Prompt player that attack phase has started
			theView.displayString(prefixString+" ATTACK: Enter country to attack from, country to attack and number of units to use, or enter skip");
			
			userInput = theView.getCommand();
			
			//Check if player is trying to skip their turn
			if(userInput.toLowerCase().trim().equals("skip")) {
				theView.displayString("Skipping attack Phase. . .");
				return;
			}
			
			//Get indexes for each word
			index1 = userInput.indexOf(" ");
			index2 = userInput.indexOf(" ", index1+1);
			
			//Check that correct number of parameters where entered
			if(index1==-1 || index2==-1) {
				theView.displayString("Not enough parameters entered!");
				continue; 	
			} else if(userInput.indexOf(" ", index2+1) != -1){
				theView.displayString("Too many parameters entered! If a territory name contains a space, remove the space, e.g. 'E Africa' becomes 'EAfrica' ");
				continue;
			}
			
			//Separate word 
			firstWord = userInput.substring(0, index1).trim();
			secondWord = userInput.substring(index1+1, index2).trim();
			thirdWord = userInput.substring(index2+1).trim();
			
			//Check that countries are valid
			firstCountryID = theModel.findCountry(firstWord);
			theView.displayValidity(firstCountryID, firstWord);
			if(firstCountryID < 0) { continue; }
			if (theModel.isPlayerOwned(inputPlayerID, firstCountryID)==false) { //Checking to see if player owns the country
				theView.displayString(prefixString+" does not own "+theModel.getCountryArray()[firstCountryID].getCountryName()+" and cannot attack from there!");
				continue;
			}
			secondCountryID = theModel.findCountry(secondWord);
			theView.displayValidity(secondCountryID, secondWord);
			if(secondCountryID < 0) { 
				continue; 
			} else if (theModel.isPlayerOwned(inputPlayerID, secondCountryID)==true) { //Checking to see if player owns the country
				theView.displayString(prefixString+" owns "+theModel.getCountryArray()[firstCountryID].getCountryName()+" and connot attack themselves!");
				continue;
			}
			
			//Check that territories are adjacent
			if(!theModel.isAdjacent(firstCountryID, secondCountryID)) {
				theView.displayString(theModel.getCountryArray()[firstCountryID].getCountryName()+" is not linked to "+theModel.getCountryArray()[secondCountryID].getCountryName());
				continue;
			}
			
			//Try to parse third Word to Integer
			try {
				armiesToUse = Integer.parseInt(thirdWord);
			} catch (NumberFormatException e) {
				theView.displayString(thirdWord+" is not an Integer!");
				continue;
			}
			
			//Check that we can attack with that amount of armies
			if(armiesToUse>3 || armiesToUse<1) {
				theView.displayString("Cannot perform an attack with "+armiesToUse+" Armies");
				continue;
			} else if(theModel.getCountryArray()[firstCountryID].getNumberArmies()-armiesToUse < 1) {
				theView.displayString("Not enough armies on "+theModel.getCountryArray()[firstCountryID].getCountryName()+" to attack with that amount of Armies");
				continue;
			}
			
			//Find how may armies will defend
			int defenderID = theModel.getCountryArray()[secondCountryID].getCountryOwner();
			if(theModel.getCountryArray()[secondCountryID].getNumberArmies() == 1) { //If there is only 1 army then defender has to use 1 army regardless of owner
				defendingArmies = 1; 
			} else if(defenderID<Constants.NUM_PLAYERS) { //If Player owned then ask the player how many armies to defend with
				do {
					theView.displayString("Player "+(defenderID+1)+" ("+theModel.getPlayerArray()[defenderID].getName()+"): Enter how many armies to defend with (1 or 2):");
					try {
						defendingArmies = Integer.parseInt(theView.getCommand());
					} catch (NumberFormatException e) {
						theView.displayString("Invalid Amount, Please enter 1 or 2!");
						continue;
					}
				} while (defendingArmies != 1 && defendingArmies != 2);
			} else { //AI defends with max amount possible
				defendingArmies = 2;
			}
			
			//Roll Dice
			int[] attackerRoll = new int[armiesToUse];
			int[] defenderRoll = new int[defendingArmies];
			for(int i=0; i<armiesToUse; i++) { attackerRoll[i] = theModel.rollDie(); }
			for(int i=0; i<defendingArmies; i++) { defenderRoll[i] = theModel.rollDie(); }
			
			//Display What rolls players got
			String output = "Attacker Roll : [ ";
			for(int i=0; i<armiesToUse; i++) { output += attackerRoll[i]+" "; };
			output += "]. Defender Roll: [ ";
			for(int i=0; i<defendingArmies; i++) { output += defenderRoll[i]+" "; };
			output += "].";
			theView.displayString(output);
			
			//Compare who won, removing armies equal to the least amount of rolls done
			for(int i=0; i<armiesToUse && i<defendingArmies; i++) {
				//Find the largest roll for each player
				int maxAttack = 0, maxDefend = 0;
				for(int j=0; j<armiesToUse; j++) {
					if(attackerRoll[j]>attackerRoll[maxAttack]) 
						maxAttack = j;
				}
				for(int j=0; j<defendingArmies; j++) {
					if(defenderRoll[j]>defenderRoll[maxDefend])
						maxDefend = j;
				}
				
				//Compare the two top results
				if(attackerRoll[maxAttack]>defenderRoll[maxDefend]) {
					theView.displayString("Defending Country lost a unit");
					theModel.reinforce(secondCountryID, -1);
				} else {
					theModel.reinforce(firstCountryID, -1);
					theView.displayString("Attacking Country lost a unit");
				}
				
				//Remove the two die for further comparisons
				attackerRoll[maxAttack] = 0;
				defenderRoll[maxDefend] = 0;
			}
			
			//Check if defending country is out of units
			if(theModel.getCountryArray()[secondCountryID].getNumberArmies()<1) {
				int moveable = theModel.getCountryArray()[firstCountryID].getNumberArmies()-1;
				if(moveable==armiesToUse) {
					theModel.setCountryOwnership(inputPlayerID, secondCountryID, armiesToUse);
					theModel.reinforce(firstCountryID, -armiesToUse);
				} else {
					int toMove = 0;
					do {
						//Ask Attacking player how many armies to move from invading territory to invaded territory
						theView.displayString(prefixString+" Enter the amount of armies to move from "+theModel.getCountryArray()[firstCountryID].getCountryName()+" to "+theModel.getCountryArray()[secondCountryID].getCountryName()+". Between "+armiesToUse+" -"+moveable);
						do {
							try {
								toMove = Integer.parseInt(theView.getCommand());
							} catch (NumberFormatException e) {
								theView.displayString("Please enter a valid Integer!");
								toMove = 0;
							}
						} while (toMove==0);
						
						if(toMove<armiesToUse||toMove>moveable) {
							theView.displayString("Please Enter a number between "+armiesToUse+" and "+moveable);
						}
					} while(toMove<armiesToUse||toMove>moveable);
					theModel.setCountryOwnership(inputPlayerID, secondCountryID, toMove);
					theModel.reinforce(firstCountryID, -toMove);
				}
				//If player has won, end their turn
				if(theModel.isWinner(inputPlayerID)) {
					return;
				}
				//If this is the first time invading a country this turn, draw a card
				if(!cardRecieved) {
					theModel.playerDrawCard(inputPlayerID);
					theView.displayString(prefixString+" Drew the card : "+theModel.getPlayerArray()[inputPlayerID].getCardsOwned().get(theModel.getPlayerArray()[inputPlayerID].getCardsOwned().size()-1).toString());
					cardRecieved = true; 
				}
			}
			
			//Refresh GameBoard
			theView.refresh(theModel.getCountryArray());
		//We repeat until the player chooses to skip
		} while(true);
		
	}
	/**
	 * Player get choice to move armies from one territory to another territory that they controll, if there 
	 * @param inputPlayerID : Player who is performing the reinforcement
	 */
	public void fortifyPhase(int inputPlayerID) {
		boolean finished = false;
		String prefixString = "Player "+(inputPlayerID+1)+" ("+theModel.getPlayerArray()[inputPlayerID].getName()+"):";
		String userInput, firstWord, secondWord, thirdWord; //Used to store user input, and again individually
		int index1, index2; //Used to track 
		int firstCountryID, secondCountryID, armiesToUse;
		
		while(!finished) {
			theView.displayString(prefixString+" FORTIFY: Enter country to move units from, country to fortify and number of units to move, or enter skip");
			
			userInput = theView.getCommand();
			
			//Check if player is trying to skip their turn
			if(userInput.toLowerCase().trim().equals("skip")) {
				theView.displayString("Skipping Fortify Phase. . .");
				return;
			}
			
			//Get indexes for each word
			index1 = userInput.indexOf(" ");
			index2 = userInput.indexOf(" ", index1+1);
			
			//Check that correct number of parameters where entered
			if(index1==-1 || index2==-1) {
				theView.displayString("Not enough parameters entered!");
				continue; 	
			} else if(userInput.indexOf(" ", index2+1) != -1){
				theView.displayString("Too many parameters entered! If a territory name contains a space, remove the space, e.g. 'E Africa' becomes 'EAfrica'");
				continue;
			}
			
			//Separate word 
			firstWord = userInput.substring(0, index1).trim();
			secondWord = userInput.substring(index1+1, index2).trim();
			thirdWord = userInput.substring(index2+1).trim();
			
			//Check that countries are valid
			firstCountryID = theModel.findCountry(firstWord);
			theView.displayValidity(firstCountryID, firstWord);
			if(firstCountryID < 0) { continue; }
			if (theModel.isPlayerOwned(inputPlayerID, firstCountryID)==false) { //Checking to see if player owns the country
				theView.displayString(prefixString+" does not own "+theModel.getCountryArray()[firstCountryID].getCountryName()+" and cannot rinforce from there!");
				continue;
			}
			secondCountryID = theModel.findCountry(secondWord);
			theView.displayValidity(secondCountryID, secondWord);
			if(secondCountryID < 0) { 
				continue; 
			} else if (theModel.isPlayerOwned(inputPlayerID, secondCountryID)==false) { //Checking to see if player owns the country
				theView.displayString(prefixString+" does not own "+theModel.getCountryArray()[secondCountryID].getCountryName()+" an cannot reinforce there!");
				continue;
			}
			
			//Check that territory1 has >1 troops
			if(theModel.getCountryArray()[firstCountryID].getNumberArmies()<2) {
				theView.displayString(theModel.getCountryArray()[firstCountryID].getCountryName()+" does not have enough troops to reinforce another territory!");
				continue;
			}
			
			//Check that territories are linked through friendly territories
			if(!theModel.isLinked(inputPlayerID, firstCountryID, secondCountryID)) {
				theView.displayString(theModel.getCountryArray()[firstCountryID].getCountryName()+" is not linked to "+theModel.getCountryArray()[secondCountryID].getCountryName()+". Reinforcements would need to go through enemy territory");
				continue;
			}
			
			//Try to parse third Word to Integer
			try {
				armiesToUse = Integer.parseInt(thirdWord);
			} catch (NumberFormatException e) {
				theView.displayString(thirdWord+" is not an Integer!");
				continue;
			}
			
			//Check that we can move that many armies 
			if(armiesToUse<1 || armiesToUse>theModel.getCountryArray()[firstCountryID].getNumberArmies()-1) {
				theView.displayString("Cannot move this amount of troops, Please enter a value between 1 and "+(theModel.getCountryArray()[firstCountryID].getNumberArmies()-1)+"!");
				continue;
			}
			
			//Change each territory army count
			theModel.reinforce(firstCountryID, -armiesToUse);
			theModel.reinforce(secondCountryID, armiesToUse);
			theView.refresh(theModel.getCountryArray());
			finished = true;
		}
	}
	
	/**
	 * Function to ask user if they want to trade in cards from their hand, and what cards that is
	 * @param inputPlayerID : Player trading in cards
	 * @return : Number of reinforcements from any trade ins
	 */
	public int requestTrade(int inputPlayerID) {
		boolean first = true; 
		theView.displayHand(theModel.getPlayerArray()[inputPlayerID]);
		int reinforcements = 0; //value to track reinforcements to receive. 
		
		//Continue while 
		while(theModel.isTradePossible(inputPlayerID)) {
			if(first) {
				first = false;
			} else {
				theView.displayHand(theModel.getPlayerArray()[inputPlayerID]);
			}
			//Prompt user and get valid input - allow to skip
			char[] cardsToTrade = new char[3];
			String inputString ;
			boolean found = false;
			while(!found) {
				if(theModel.getPlayerArray()[inputPlayerID].getCardsOwned().size()>4) {
					theView.displayString("Player "+(inputPlayerID+1)+" ("+theModel.getPlayerArray()[inputPlayerID].getName()+"): Enter the Insignia initials to trade in cards");
					inputString = theView.getCommand().trim();
				}else {
					theView.displayString("Player "+(inputPlayerID+1)+" ("+theModel.getPlayerArray()[inputPlayerID].getName()+"): Enter the Insignia initials to trade in cards, or enter skip");
					inputString = theView.getCommand().trim();
					if(inputString.toLowerCase().equals("skip")) {
						theView.displayString("Skipping Card Trade In. . .");
						return reinforcements;
					}
				}
				boolean notEnoughChar = false; //track if not enough characters entered
				for(int i=0; i<3; i++) {
					if(inputString.isBlank()) {
						theView.displayString("Error, not enough characters detected");
						notEnoughChar = true; 
						break;
					}
					cardsToTrade[i] = inputString.charAt(0);
					inputString = inputString.substring(1).trim();
				}
				if(notEnoughChar)
					continue;
				
				//Check to see if there were too many inputs
				if(!inputString.isBlank()) {
					theView.displayString("Error, too many characters detected");
					continue;
				}
				
				//Check to see if inputs are valid insignia char's
				boolean invalidChar = false; //track if a character is invalid
				for(char c : cardsToTrade) {
					if(c != 'i' && c != 'c' && c != 'a' && c != 'w') {
						theView.displayString("Error, invalid character input");
						invalidChar = true;
						break;
					}	
				}
				if(invalidChar)
					continue;
				
				//Check if combination is a valid trade in, only invalid combination is two insignia the same, that is not a wildcard
				boolean validCombination = false ;
				if(cardsToTrade[0]=='w'||cardsToTrade[1]=='w'||cardsToTrade[2]=='w') {validCombination = true ;}
				if(cardsToTrade[0]==cardsToTrade[1] && cardsToTrade[1]==cardsToTrade[2]) {validCombination = true ;}
				if(cardsToTrade[0]!=cardsToTrade[1] && cardsToTrade[0]!=cardsToTrade[2] && cardsToTrade[1]!=cardsToTrade[2] ) {validCombination = true ;}
				if(!validCombination) {
					theView.displayString("Error, not a valid hand trade in, enter three of the same insignia, three different insignia or a wildcard and two other insignia");
					continue;
				}
				
				//Check that the user owns the inputed insignia cards
				boolean owned = true;
				char[] insignia = {'i','c','a','w'};
				int[] insigniaCount = theModel.countInsigniaInHand(inputPlayerID) ;
				
				for(int j=0;j<3;j++) {
					if(!owned)
						break;
					for(int k=0;k<4;k++) {
						if(cardsToTrade[j]==insignia[k]) {
							if(insigniaCount[k]<1) {
								theView.displayString("Error, not a valid hand trade in, Player does not own selected cards");
								owned = false ;
								break;
							}else {
								insigniaCount[k]--;
							}
						}
					}
				}
				if(!owned) {
					continue;
				}
				found =  true ;
			}
			
			//Remove those cards from players hand and place in discard pile & increase reinforcements int 
			for(int i=0;i<3;i++) {
				for(int j=0;j<theModel.getPlayerArray()[inputPlayerID].getCardsOwned().size();j++) {
					if(cardsToTrade[i]==theModel.getPlayerArray()[inputPlayerID].getCardsOwned().get(j).getInsignia() ) {
						theModel.discardPile.add(theModel.getPlayerArray()[inputPlayerID].getCardsOwned().remove(j));
						break;
					}
				}
			}
			reinforcements += theModel.getTradeInCount() ;
			
			//Increase count for next amount of reinforcements
			theModel.incrementTradeInCount();
		}
	
		
		//Return reinforcements to receive
		return reinforcements;
	}
}
