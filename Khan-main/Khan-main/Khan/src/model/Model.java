package model;

import view.View;
import java.util.ArrayList;
import java.util.Collections;

public class Model {
	
	private View view;
	//Instance Variables
	private Player[] playerArray;
	private Country[] countryArray;
	
	public ArrayList<Card> deck = new ArrayList<Card>() ;
	public ArrayList<Card> discardPile = new ArrayList<Card>() ;
	
	public int tradeInCount = Constants.TRADEIN_AMOUNT[0]; 

	
	/**
	 * Empty constructor
	 */
	public Model(View view) {
		this.view = view; //Allows for use of display function
	}
	
	//Getters
	public int getPlayerCount() { return Constants.NUM_PLAYERS; }
	
	public Player[] getPlayerArray() { return this.playerArray; }
	
	public Country[] getCountryArray() { return this.countryArray; }
	
	public int getTradeInCount() { return this.tradeInCount; }
	
	//Setters
	public void setTradeInCount(int input) { this.tradeInCount = input; }
	
	/**
	 * Creates player array at boot of new game
	 * @param input: no. of players in the game
	 */
	public void createPlayers(String[] playerNames) {
		if(playerArray==null) {
			playerArray = new Player[getPlayerCount()];
			
			for(int i=0; i<getPlayerCount(); i++) {
				playerArray[i] = new Player(playerNames[i], i);
			}
		} else {
			System.out.println("Error, Players array already initialised");
		}
	}
	
	/**
	 * Creates countries using info found in Constants.java
	 */
	public void createCountries() {
		
		countryArray = new Country[Constants.NUM_COUNTRIES];
		
		for(int i=0; i<Constants.NUM_COUNTRIES; i++) {
			countryArray[i] = (new Country(i));
		}
	}
	
	
	/**
	 * Randomly allocates playerOrder, does not currently use a Die Roll
	 * @param playerOrder : Stores the order that players will use for turns
	 */
	public void choosePlayerOrder(int[] playerOrder) {
		int player1Roll=0 ;
		int player2Roll=0 ;
		
		while(player1Roll==player2Roll) {
			//Rolls Die For both players
			player1Roll = rollDie() ;
			player2Roll = rollDie() ;
			
			//Tells users what they rolled
			
			view.displayString("\nPlayer 1 rolled a "+ player1Roll);
			view.displayString("Player 2 rolled a "+ player2Roll);
			
			//Checking to see if one of the users rolls is higher
			if(player1Roll>player2Roll) {
				playerOrder[0] = 0;
				playerOrder[1] = 1 ;
				view.displayString("Player 1 will go first");
			}else if(player2Roll>player1Roll) {
				playerOrder[0] = 1;
				playerOrder[1] = 0;
				view.displayString("Player 2 will go first");
			}else {
				view.displayString("Players rolled the same numbers. Roll again");

			}
		}
	}
	
	/**
	 * Rolls a six sided die and returns the result from 1-6
	 * @return Result from die roll
	 */
	public int rollDie() {
		return (int) (Math.random()*6)+1;
	}
	
	
	/**
	 * Takes String input and returns what int id that country corresponds to
	 * @param input : unambiguous string
	 * @return : int corresponding to inputed country
	 * @return -1 : Input does not match any country
	 * @return -2 : Input is not unambiguous 
	 */
	public int findCountry(String input) {
		input = input.toLowerCase();
		int output = -1;
		for(int i=0; i<Constants.NUM_COUNTRIES; i++) {
			if(Constants.COUNTRY_NAMES[i].toLowerCase().startsWith(input)==true || Constants.COUNTRY_NAMES[i].toLowerCase().replaceAll(" ", "").startsWith(input)==true) {
				if(output==-1) {
					output=i;
				}
				else {
					return -2;
				}
			}
		}
		return output;
	}
	

	/**
	 * Draw 9 cards per player and allocate the countries to the players
	 */
	public void allocateCountries() {
		makeDeck() ;
		
		int id =0;
		//Randomly give players 9 country cards, and place 1 army on country
		for(int j=0;j<Constants.NUM_PLAYERS;j++) {
			for (int i=0; i<9;i++) {
				id = deck.get(0).getCardID() ;
				if(deck.get(0).getInsignia()=='w') {
					i--;
				} else {
					setCountryOwnership(j, id, 1);
					
					//Tells user what cards where drawn
					view.displayString("Player "+(j+1)+" Drew Card ' " +deck.get(0).toString()+"'");
				}
				
				discardPile.add(deck.remove(0));
			}
		}
		
		//Set rest of countries to neutrals with 1 army
		for(int j=Constants.NUM_PLAYERS;j<6;j++) {
			for (int i=0; i<6;i++) {
				id = deck.get(0).getCardID() ;
				if(deck.get(0).getInsignia()=='w') {
					i--;
				} else { 
					setCountryOwnership(j, id, 1);
				}
				discardPile.add(deck.remove(0));
			}
		}
		//Adds cards back to deck and shuffles
		discardToDeck() ;		
	}
	
	/**
	 * Sets ownership of a country to another player
	 * @param playerID : Player to give ownership to 
	 * @param countryID : Country to give player
	 * @param numberArmies : Number of Armies this country should have after the transaction
	 */
	public void setCountryOwnership(int playerID, int countryID, int numberArmies) {
		countryArray[countryID].setPlayerOwned(playerID);
		countryArray[countryID].setNumberArmies(numberArmies);
	}
	
	/**
	 * Creates deck initially 
	 */
	public void makeDeck() {
		//Make country cards
		for(int i=0; i < 42; i++) {
			deck.add(new Card(i));
		}
		//Make wildcards
		deck.add(new Card(-1, 'w'));
		deck.add(new Card(-1, 'w'));
		Collections.shuffle(deck);
	}
	
	/**
	 * Combines discard pile into normal deck and shuffles
	 */
	public void discardToDeck() {
		for(int i=discardPile.size(); i>0; i--) {
		        deck.add(discardPile.remove(0));
		}
		Collections.shuffle(deck);
	}
	
	/**
	 * Draws a card from deck and adds to players hand
	 * @param playerID : Player that is drawing a card
	 */
	public void playerDrawCard(int inputPlayerId) {
		if(deck.size()==0) {
			discardToDeck();
		}
		playerArray[inputPlayerId].cardsOwned.add(deck.remove(0)) ;
	}
	
	/**
	 * Checks if a territory is belonging to a player
	 * @param inputPlayerId : The player we are checking
	 * @param territoryId : The territory we are checking ownership of 
	 * @return : If the player owns the territory
	 */
	public boolean isPlayerOwned(int inputPlayerId, int territoryId) {
		return getCountryArray()[territoryId].getCountryOwner()==inputPlayerId;
	}
	
	/**
	 * Calculates the number of reinforcements given to a player based on their currently controlled territories
	 * @param inputPlayerId : Player we are checking
	 * @return : Number of reinforcements the player should be given
	 */
	public int calculateReinforcements(int inputPlayerId) {
		//calculate correct amount of reinforcements based on what territories players control
		int countTerr=0;
		int countContinents=0;
		int[] countConin = new int [6];
		for(int i=0;i<Constants.NUM_COUNTRIES;i++) {
			if(isPlayerOwned(inputPlayerId,i)==true) {
				countTerr++;
				countConin[Constants.CONTINENT_IDS[i]]++;
			}
		}
		
		if(countConin[0]==9) {
			countContinents=countContinents+5;
		}
		if(countConin[1]==7) {
			countContinents=countContinents+5;
		}
		if(countConin[2]==12) {
			countContinents=countContinents+7;
		}
		if(countConin[3]==4) {
			countContinents=countContinents+2;
		}
		if(countConin[4]==4) {
			countContinents=countContinents+2;
		}
		if(countConin[5]==6) {
			countContinents=countContinents+3;
		}
			
		int reinf=countTerr/3;
		if(reinf<3)
			reinf=3;
		return reinf+countContinents;
	}
	
	/**
	 * Increase number of troops on given territory by desired amount
	 * @param territoryId : Territory to modify
	 * @param numberOfArmies : Number of armies to reinforce territory with
	 */
	public void reinforce(int territoryId, int numberOfArmies) {
		getCountryArray()[territoryId].setNumberArmies(getCountryArray()[territoryId].getNumberArmies()+numberOfArmies);
	}
	
	/**
	 * Checks if a player has won the game
	 * @param inputPlayerId : Player we are checking
	 * @return : Weather or not the player has won
	 */
	public boolean isWinner(int inputPlayerID) {
		for(int i=0; i<Constants.NUM_COUNTRIES; i++) {
			if(getCountryArray()[i].getCountryOwner()==(1+inputPlayerID)%2)
				return false;
		}
		return true;
	}
	
	/**
	 * Checks if two territories are adjacent to each other
	 * @param firstID : First territory to check
	 * @param secondID : Second territory to check
	 * @return : Weather or not two territories are directly adjacent
	 */
	public boolean isAdjacent(int firstID, int secondID) {
		for( int i : Constants.ADJACENT[firstID]) {
			if(i == secondID)
				return true;
		}	
		return false;
	}
	
	/**
	 * Checks if two territories are linked through friendly territories
	 * @param firstID : Territory we are starting at
	 * @param secondID : Territory we are trying to reach
	 * @return : Weather or not the two territories are linked through friendly territories
	 */
	public boolean isLinked(int inputPlayerID, int firstID, int secondID) {
		
		//Array representing the countries we have checked already, 1 = not checked, 0 = checked already
		int[] countriesChecked = new int[Constants.NUM_COUNTRIES];
		for(int i=0; i<Constants.NUM_COUNTRIES; i++) { countriesChecked[i]=1; }
		
		countriesChecked[firstID]=0;
		return checkNode(inputPlayerID, firstID, secondID, countriesChecked);
		
	}
	
	/**
	 * Helper function for isLinked, runs recursively
	 * @param inputPlayerID : Player who we are checking for
	 * @param firstID : Territory we are checking connections of
	 * @param secondID : Territory we are trying to find a connection to
	 * @param countriesChecked : To avoid endlessly searching we track what territories we have already searched
	 * @return : Weather or not their is a link between between original firstID and secondID
	 */
	public boolean checkNode(int inputPlayerID, int firstID, int secondID, int[] countriesChecked) {
		for(int i : Constants.ADJACENT[firstID]) {
			if(countriesChecked[i] == 1) {
				if(i == secondID ) //If we have reached desired country then we can end the function
					return true;
				
				//We say that we have checked this territory to avoid repetition
				countriesChecked[i]=0;
				if(getCountryArray()[i].getCountryOwner()==inputPlayerID) //see if player owns this country we plan to check
					if(checkNode(inputPlayerID, i, secondID, countriesChecked)) //run check, if true return true up the chain to isLinked()
						return true;
			}
		}
		return false; //If reached then we have hit a dead end with country searching, if we reach every dead end then we know isLinked is false
	}
	
	/**
	 * Increases tradeInCount by appropriate amount
	 */
	public void incrementTradeInCount() {
		//If we reached the end of trade in values defined in Constants
		if(getTradeInCount()>=Constants.TRADEIN_AMOUNT[Constants.TRADEIN_AMOUNT.length-1]) {
			setTradeInCount(getTradeInCount()+5);
			return;
		}
		
		//Increase trade in amount to next value
		for(int i=0; i<Constants.TRADEIN_AMOUNT.length-1; i++) {
			if(Constants.TRADEIN_AMOUNT[i]==getTradeInCount()) {
				setTradeInCount(Constants.TRADEIN_AMOUNT[i+1]); //should never reach out of bounds thanks to check at start of this function
				return;
			}
		}		
		System.out.println("Error, TradeInCount did not increment");
	}
	
	/**
	 * Checks if player has a possible valid set of cards to trade in
	 * @param inputPlayerID : Player we are checking the hand of 
	 * @return : Weather or not the player has a valid set of cards to trade in
	 */
	public boolean isTradePossible(int inputPlayerID) {
		// If they have more than 4 cards they will have a valid combination to trade in
		if(playerArray[inputPlayerID].getCardsOwned().size()>4) { return true ;}
		if(playerArray[inputPlayerID].getCardsOwned().size()<2) { return false ;}
		
		// Contains the number of each insignia contained by players
		int[] insigniaCount = countInsigniaInHand(inputPlayerID);
		
		// any valid combination returns true 
		if(insigniaCount[3]>1) {return true ;}
		if(insigniaCount[0]>0 && insigniaCount[1]>0 && insigniaCount[2]>0) { return true ;}
		if(insigniaCount[0]>=3 || (insigniaCount[1])>=3 || insigniaCount[2]>=3) {return true ;}
		
		return false;
	}
	
	/**
	 * Counts the number of each insignia a player holds in their hand 
	 * @param inputPlayerID : Player we are checking the hand of 
	 * @return : Returns an int array, position 0 representing infantry, 1 cavalry, 2 artillery and 3 wildcards
	 */
	public int[] countInsigniaInHand(int inputPlayerID){
		int[] count = {0,0,0,0} ;
		// Counts how many of each insignia the player has
		for(int i=0;i<playerArray[inputPlayerID].getCardsOwned().size();i++) {
			switch (playerArray[inputPlayerID].getCardsOwned().get(i).getInsignia()) {
			  case 'i': count[0]++; break;
			  case 'c': count[1]++; break;
			  case 'a': count[2]++; break;
			  case 'w': count[3]++; break;
			}
		}
		return count ;
	}
	
}
