package model;
	
public class Country {

	private final int countryId ;
	private final String countryName ;
	private final int[] adjacentArray ;
	private int playerOwned;
	private int numberArmies = 0 ;
	
	public Country (int id) {
		countryId = id;
		countryName = Constants.COUNTRY_NAMES[id];
		adjacentArray = Constants.ADJACENT[id];
		setPlayerOwned(-1);
	}
	//Getters
	public int get_id() { return countryId; }
	
	public String getCountryName() { return countryName; }
	
	public int[] getAdjacentArray() { return adjacentArray; }
	
	public int getCountryOwner() { return playerOwned; }
	
	public int getNumberArmies() { return numberArmies ; }
	
	//Setters
	public void setPlayerOwned(int input){ this.playerOwned = input; }
	
	public void setNumberArmies(int input){ this.numberArmies = input; }
	
	@Override 
	public String toString() {
		String output = getCountryName() + ", ";
		return output;
	}
}
