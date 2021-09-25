package view;

import java.awt.BorderLayout;
import javax.swing.*;

import model.Constants;
import model.Country;
import model.Player;

public class View extends JFrame{
	
	private static final long serialVersionUID = 1L;
	//Instance Variables
	private Map map;
	private InfoPanel infoPanel;
	private CommandPanel commandPanel;
	
	/**
	 * Creates main window & objects
	 */
	public View() {
		//Create instance objects
		infoPanel = new InfoPanel();
		commandPanel = new CommandPanel();
		map = new Map();
		
		//Create Frame
		setSize(Constants.FRAME_WIDTH,Constants.FRAME_HEIGHT+Constants.TEXT_HEIGHT);
		setTitle("Risk");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//Add Objects
		add(map, BorderLayout.NORTH);
		add(infoPanel, BorderLayout.CENTER);
		add(commandPanel, BorderLayout.SOUTH);
		setResizable(false);
		setVisible(true);
	}
	
	/**
	 * Displays desired string to infoPanel
	 * @param input : String to be displayed
	 */
	public void displayString(String input) {
		infoPanel.addText(input);
	}
	
	/**
	 * Gets command that was entered by user
	 * @return Input from user
	 */
	public String getCommand() {
		String input = commandPanel.getCommand();
		displayString("> "+input);
		return input;
	}
	
	
	/**
	 * Creates panel/screen to get user input for names.
	 * Hard-code for now for 2 people
	 * 
	 * @return String Array: Names of users
	 */
	public String[] getPlayerNames() {
		String[] output = new String[Constants.NUM_PLAYERS];
		for(int i=0; i<Constants.NUM_PLAYERS; i++) {
			displayString("Enter the name of the player "+(i+1)+" : ");
			output[i] = getCommand();
		}
		map.displayPlayerNames(output);
		return output;
	}
	
	/**
	 * Refreshes the Map
	 * @param input : contains info on who owns what country and how many armies are present
	 */
	public void refresh(Country[] input) {
		map.refresh(input);
	}
	
	/**
	 * Displays winning screen for selected player
	 * @param winner : Player object for winner of game
	 */
	public void displayWinner(Player winner) {
		map.displayWinner(winner);
	}
	
	/**
	 * Displays info if entered info is not valid
	 * @param countryID : Returned id from a findCountry() call
	 * @param entered : The string inputed to the findCountry() call
	 */
	public void displayValidity(int countryID, String entered) {
		if(countryID==-1) {
			displayString("Cannot find Country corresponding to : "+entered);
		} else if(countryID==-2){
			displayString(entered+" : is not unambigous");
		}
	}
	
	/**
	 * Function prints the current state of a players hand
	 * @param inputPlayer : the player we are printing the hand of 
	 */
	public void displayHand(Player inputPlayer) {
		displayString("Player "+(inputPlayer.getPlayerId()+1)+" ("+inputPlayer.getName()+"): You currently have the cards: ");
		for(int i=0; i<inputPlayer.getCardsOwned().size(); i++) {
			displayString(inputPlayer.getCardsOwned().get(i).toString());
		}
	}

}
