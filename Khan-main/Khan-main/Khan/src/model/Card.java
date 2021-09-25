package model;

public class Card {

	private int cardID;
	private char insignia;
	private int playerOwned;
	
	//Constructors 
	public Card(int inputID) {
		this.cardID = inputID;
		int insigniaID = inputID%3;
		switch(insigniaID) {
			case 1 : this.insignia = 'i'; break;
			case 2 : this.insignia = 'c'; break;
			default : this.insignia = 'a'; break;
		}
		this.playerOwned  = -1;
	}
	
	public Card(int inputID, char inputInsignia) {
		this.cardID = inputID;
		this.insignia = inputInsignia;
		this.playerOwned = -1;
	}
	
	 // Getters
	public String getCardCountry() { return Constants.COUNTRY_NAMES[this.cardID]; }
	
	public int getCardID() { return cardID;}
	
	public int getPlayerOwned() { return playerOwned; }
	
	public char getInsignia() { return insignia; }

	// Setters
	public void setPlayerOwned(int input){ this.playerOwned = input; }
	
	@Override
	public String toString() {
		if(insignia == 'w')
			return "Insignia: Wildcard";
		if(insignia == 'i')
			return "Country : " + this.getCardCountry()+". Insignia: Infantry";
		if(insignia == 'c')
			return "Country : " + this.getCardCountry()+". Insignia: Cavalry";
		if(insignia == 'a')
			return "Country : " + this.getCardCountry()+". Insignia: Artillary";
		return "Error, invalid card";
	}

	
}
