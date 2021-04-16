// put your code here

public class BeachArcade implements Bot {
	// The public API of BeachArcade must not change
	// You cannot change any other classes
	// BeachArcade may not alter the state of the board or the player objects
	// It may only inspect the state of the board and the player objects
	// So you can use player.getNumUnits() but you can't use player.addUnits(10000), for example

	private static class Country {
		public int occupierID;
		public int numUnits;

		public Country() {
			occupierID = -1;
			numUnits = -1;
		}

		public void setNumUnits(int numUnits) {
			this.numUnits = numUnits;
		}

		public void setOccupierID(int occupierID) {
			this.occupierID = occupierID;
		}

		public void updateTerritory(int numUnits, int occupierID) {
			setNumUnits(numUnits);
			setOccupierID(occupierID);
		}

		// * Goes against my very being, but it's probably
		public int getNumUnits() {
			return numUnits;
		}

		public int getOccupierID() {
			return occupierID;
		}
	}

	private BoardAPI board;
	private PlayerAPI player;
	private final Country[] map;

	BeachArcade(BoardAPI inBoard, PlayerAPI inPlayer) {
		board = inBoard;	
		player = inPlayer;
		map = new Country[GameData.NUM_COUNTRIES];
		updateTerritories();
		// put your code here
		return;
	}

	// ! The idea is that we would call this whenever we need to make decisions that concern the whole map.
	private void updateTerritories() {
		for (int i = 0; i < GameData.NUM_COUNTRIES; ++i) {
			map[i].updateTerritory(board.getNumUnits(i), board.getOccupier(i));
		}
	}

	/**
	 * <strong>getName</strong> — Gets the name of the bot.
	 * @return String, Bot's name
	 */
	public String getName () {
		return("Beach Arcade"); // ? What about throwing debug info in here, so whenever the bot's name is used, we get debug stuff
	}

	/**
	 * <p><strong>getReinforcement</strong> — For <em>reinforcing</em> a territory with reserves.</p>
	 * <p>Gets the name of a territory (that belongs to the bot) to place a number of reinforcements (that is valid) onto.</p>
	 * @return String, command in the form "Country Name" "Number of Reinforcements".
	 */
	public String getReinforcement () {
		String command = "";
		// put your code here
		command = GameData.COUNTRY_NAMES[(int)(Math.random() * GameData.NUM_COUNTRIES)];
		command = command.replaceAll("\\s", "");
		command += " 1";
		return(command);
	}

	/**
	 * <p><strong>getPlacement</strong> — For placing of territories in <em>initialisation stage</em>.</p>
	 * <p>Chooses a territory from one of the neutrals to place reinforcements on sending it to the board reinforcement (from the neutral player's reserves) on the selected territory.</p>
	 * @param forPlayer The index of the player whose territories we are choosing from.
	 * @return String, the name of the territory
	 */
	public String getPlacement (int forPlayer) {
		String command = "";
		// put your code here
		// > For now, maybe just pick them at random?
		command = GameData.COUNTRY_NAMES[(int)(Math.random() * GameData.NUM_COUNTRIES)];
		command = command.replaceAll("\\s", "");
		return(command);
	}

	/**
	 * <p><strong>getCardExchange</strong> — Gets cards from the bot's hand to exchange for reserves.</p>
	 * <p>Returns a valid card exchange command; 3 cards to exchange (just first letter, ie. wia), or "<em>skip</em>" if less than 5 cards.</p>
	 * @return String, command using letters to represent the cards
	 */
	public String getCardExchange () {
		String command = "";
		// put your code here
		command = "skip";
		return(command);
	}

	/**
	 * <p><strong>getBattle</strong> — For getting an attack command.</p>
	 * <p>Used in bot's <em>attack stage</em>. Returns a String containing the country to attack from, the country to attack and the number of units to use, or "skip" to skip attack phase.</p>
	 * @return String representing attack command ("from", "to", "units")
	 */
	public String getBattle () {
		String command = "";
		// put your code here
		command = "skip";
		return(command);
	}

	/**
	 * <p><strong>getDefence</strong> — For getting a defend command.</p>
	 * <p>Used when other player is in the <em>attack stage</em>. Returns the number of units to defend a territory with.</p>
	 * @param countryId int, ID of territory to defend.
	 * @return String, number of units
	 */
	public String getDefence (int countryId) {
		String command = "";
		// put your code here
		command = "1";
		return(command);
	}

	/**
	 * <p><strong>getMoveIn</strong> — For sending troops after winning battle.</p>
	 * <p>Used when you have won a battle and claimed a territory. Sends a number of troops to the newly conquered territory, leaving both territories with at least one troop. </p>
	 * @param attackCountryId int, ID of your attacking territory
	 * @return String, number of units
	 */
	public String getMoveIn (int attackCountryId) {
		String command = "";
		// put your code here
		command = "0";
		return(command);
	}

	/**
	 * <p><strong>getFortify</strong> — For selecting territories and a number of troops to exchange.</p>
	 * <p>Return country name to move units from, country name to fortify and number of units to move, or "skip" to skip this stage.</p>
	 * @return String representing fortify command ("from", "to", "units")
	 */
	public String getFortify () {
		String command = "";
		// put code here
		command = "skip";
		return(command);
	}

}
