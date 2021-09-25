package controller;

import model.Model;
import view.View;

public class Launch {
	
	/**
	 * Starts Program executable
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		View theView = new View();
		Model theModel = new Model(theView);
		Controller theController = new Controller(theView, theModel);
	}
}
