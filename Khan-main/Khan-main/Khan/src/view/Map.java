package view;

import java.awt.*;
import javax.swing.*;

import model.Constants;
import model.Country;
import model.Player;

import java.awt.geom.*;
import java.util.*;

public class Map extends JPanel {
	
	private static final long serialVersionUID = 1L;
	//Instance Variables
	private ArrayList<Ellipse2D.Double> countryCircleArray;
	private ArrayList<Ellipse2D.Double> playerCircleArray;
	private Country[] countryArray;
	private String[] playerNames;
	boolean checkOver = false;
	Player playerWinner;

	//Constructor
	public Map(){
		//Create Nodes for display
		countryCircleArray = new ArrayList<Ellipse2D.Double>();
		playerCircleArray = new ArrayList<Ellipse2D.Double>();
		for(int i=0; i<Constants.NUM_COUNTRIES; i++) {
			countryCircleArray.add(new Ellipse2D.Double(Constants.COUNTRY_COORD[i][0]-5, Constants.COUNTRY_COORD[i][1]-25, 30, 30));
			playerCircleArray.add(new Ellipse2D.Double(Constants.COUNTRY_COORD[i][0], Constants.COUNTRY_COORD[i][1]-20, 20, 20));
		}
		//Set Panel size
		setPreferredSize(new Dimension(1000, 600));
		//Create name array
		playerNames = new String[Constants.NUM_PLAYERS];
	}
	
	//Refresh and re-draw map
	public void refresh(Country[] input) {
		countryArray = input;
		revalidate();
		repaint();
		return;
	}
	
	//Sets player names
	public void displayPlayerNames(String[] input) {
		playerNames = input;
	}
	
	//Sets values to display the winner screen
	public void displayWinner(Player winner){
		checkOver=true;
		playerWinner = winner;
		revalidate();
		repaint();
	}
	 
	//Create non player components
	protected void paintComponent(Graphics g) {
		 	
	 	//Used for drawing
        Graphics2D g2 = (Graphics2D) g;
        
        //Fills background color
        g2.setColor(new Color(90, 200, 255));
        g2.fillRect(0, 0, 1000, 600); 
        
        //Displays which player has which color
        if(playerNames[0] != null) {
	        //Create Square to display text in
	        g2.setColor(new Color(200, 200, 200));
	        g2.fillRect(-0, Constants.FRAME_HEIGHT-95, 150, 95);
	        g2.setColor(Color.BLACK);
	        g2.drawRect(0, Constants.FRAME_HEIGHT-95, 150, 95);
	 
	        //Create text
	        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
	        g2.drawString(("Player 1 : "+playerNames[0]),10,Constants.FRAME_HEIGHT-80);
	        g2.drawString("Red",10,Constants.FRAME_HEIGHT-60);
	        g2.drawString(("Player 2 : "+playerNames[1]),10,Constants.FRAME_HEIGHT-30);
	        g2.drawString("Green",10,Constants.FRAME_HEIGHT-10);
        }
        
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        //Draws Lines between every node, Currently Inefficient and redraws lines between countries
        g2.setColor(new Color(180, 180, 180));
        g2.setStroke(new BasicStroke(10));
        for(int i=0; i<Constants.NUM_COUNTRIES; i++) {
        	for(int j : Constants.ADJACENT[i]) {
        		//Ensures that we do not redraw lines on top of each other
        		if(j>i) {
        			//Checks if this is countries that cross across the Pacific 
        			if(i==8&&j==22) continue;
        			g2.drawLine(Constants.COUNTRY_COORD[i][0]+10, Constants.COUNTRY_COORD[i][1]-10, Constants.COUNTRY_COORD[j][0]+10, Constants.COUNTRY_COORD[j][1]-10);
        		}
        	}
        }
        //Draws line left from Alaska, and Right from Kamchatka
        g2.drawLine(Constants.COUNTRY_COORD[8][0]+10, Constants.COUNTRY_COORD[8][1]-10, 0, Constants.COUNTRY_COORD[8][1]-10);
        g2.drawLine(Constants.COUNTRY_COORD[22][0]+10, Constants.COUNTRY_COORD[22][1]-10, Constants.FRAME_WIDTH, Constants.COUNTRY_COORD[22][1]-10);
        
        g2.setStroke(new BasicStroke(3));
        for(int i=0; i<Constants.NUM_COUNTRIES; i++){
        	Color color;  
        	//Draws Outside of circle, indicating continent 
        	//Selects correct color for each continent as depicted in rules
        	switch(Constants.CONTINENT_IDS[i]) {
        	case 0: color = Color.YELLOW; break;
        	case 1: color = new Color(0, 0, 255); break;
        	case 2: color = new Color(115, 255, 131); break;
        	case 3: color = new Color(135, 150, 150); break;
        	case 4: color = new Color(255, 180, 30); break;
        	case 5: color = new Color(130, 105, 0); break;
        	default: color = Color.BLACK;
        	}
        	
        	g2.setColor(color);
        	g2.draw(countryCircleArray.get(i));
        	g2.fill(countryCircleArray.get(i));
        	g2.setColor(Color.black);
        	g2.drawString(Constants.COUNTRY_NAMES[i],Constants.COUNTRY_COORD[i][0]-15, Constants.COUNTRY_COORD[i][1]-30);
        } 
        
        //Draws inner circles representing who owns what country
        g2.setFont(new Font(Font.SERIF, Font.PLAIN, 10));
        for(int i=0; i<Constants.NUM_COUNTRIES; i++){
        	Color color;
        	
        	//Find appropriate color
        	if(countryArray==null) {
        		color = Color.GRAY;
        	} else {
	        	switch(countryArray[i].getCountryOwner()) {
	        	case 0: color = Color.RED; break;
	        	case 1: color = Color.green; break;
	        	default: color = Color.GRAY;
	        	}
        	}
        	
        	//Draw and fill circles
        	g2.draw(playerCircleArray.get(i));
        	g2.setColor(color);
        	g2.fill(playerCircleArray.get(i));

        	color = Color.BLACK;
        	g2.setColor(color);
        	if(countryArray!=null) {
        		g2.drawString(Integer.toString(countryArray[i].getNumberArmies()),Constants.COUNTRY_COORD[i][0]+7, Constants.COUNTRY_COORD[i][1]-5);
        	} else {
        		g2.drawString("0",Constants.COUNTRY_COORD[i][0]+7, Constants.COUNTRY_COORD[i][1]-5);
        	}
        }
        
        if (checkOver==true) {
        	
             g2.setColor(Color.BLACK);
             g2.fillRect(Constants.FRAME_WIDTH/4, Constants.FRAME_HEIGHT/4, Constants.FRAME_WIDTH/2, Constants.FRAME_HEIGHT/2);
             
             
             g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
             g2.setColor(Color.WHITE);
             g2.drawRect(Constants.FRAME_WIDTH/4, Constants.FRAME_HEIGHT/4, Constants.FRAME_WIDTH/2, Constants.FRAME_HEIGHT/2);
             
             g2.drawString(("Game Over!"),Constants.FRAME_WIDTH/2-50,Constants.FRAME_HEIGHT-410);
             
             g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 20f));
             String text = ("Player "+(playerWinner.getPlayerId()+1)+" - "+playerWinner.getName()+" Wins The Game");
             g2.drawString(text,Constants.FRAME_WIDTH/2-(text.length()*4),Constants.FRAME_HEIGHT/2);
             
    	}

    }
	
}
