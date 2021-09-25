package model;

import java.util.ArrayList;

public class Player {
	
	//Instance variables
	private String name;
	private final int playerId;
	public ArrayList<Card> cardsOwned;
	
	//Constructor
	public Player(String input, int i) {
		setName(input);
		this.playerId = i;
		cardsOwned = new ArrayList<Card>(); 
	}
	
	//Getters
	public String getName() { return this.name; }
	
	public int getPlayerId() { return this.playerId; }
	
	public ArrayList<Card> getCardsOwned() { return this.cardsOwned; }
	
	//Setters
	public void setName(String input) { this.name = input; }
	
}
