/* BeachArcade bot: Ethan Chan, Blake Whittington, Ben Brown */

import java.util.*;
import java.util.stream.Stream;

public class BeachArcade implements Bot {
	private interface GameMap {
		Territory getTerritory(int territory);
		Continent getContinent(int continent);
		Territory[] getTerritories(int continent);
		double getRatio(int continent);

		boolean belongsTo(int territoryID, int playerID);
		boolean belongsTo(int territoryID);

		void startTurn(BoardAPI board);
		void updateTerritory(int territory, int numUnits, int occupierID);
		Stream<Continent> stream();
	}

	private static class Map extends ArrayList<Continent> implements GameMap {
		public final int botID;
		public int selectedTerritory; // * Holds the currently targeted territory

		public Map(int playerID) {
			botID = playerID;

			for (int index = 0; index < GameData.NUM_CONTINENTS; ++index) {
				add(index, new Continent(index, botID));
			}
		}

		@Override
		public Territory getTerritory(int territory) {
			return get(getContinentID(territory)).getTerritory(territory);
		}

		@Override
		public Continent getContinent(int continent) {
			return get(continent);
		}

		@Override
		public Territory[] getTerritories(int continent) {
			return get(continent).getTerritories();
		}

		@Override
		public double getRatio(int continent) {
			return get(continent).ratio();
		}

		@Override
		public boolean belongsTo(int territoryID, int playerID) {
			return getTerritory(territoryID).occupierID == playerID;
		}

		@Override
		public boolean belongsTo(int territoryID) {
			return belongsTo(territoryID, botID);
		}

		@Override
		public void startTurn(BoardAPI board) {
			forEach(continent -> continent.update(board)); // * Send the board to each continent to get its changes
			sort(Continent::compareTo); // * Sort the array based on the priority level
		}

		@Override
		public void updateTerritory(int territory, int numUnits, int occupierID) {
			getTerritory(territory).updateTerritory(numUnits, occupierID);
		}

		@Override
		public Stream<Continent> stream() {
			return super.stream();
		}

		/* * Utility Methods * */
		private int getContinentID(int territory) {
			return GameData.CONTINENT_IDS[territory];
		}
	}

	private static class Continent implements Comparable<Continent> {
		public final int id, botID;
		private final TreeMap<Integer, Territory> continent;
		private final double totalTerritories;
		private int territoriesOwned;
		private int timesUsedThisTurn; // * Might be useful to put a cap on the number of turns in a move, but still allow multiple moves on different territories.

		public Continent(int continentID, int botID) {
			id = continentID;
			this.botID = botID;
			continent = new TreeMap<>();

			totalTerritories = GameData.CONTINENT_COUNTRIES[continentID].length;

			for (int curr : GameData.CONTINENT_COUNTRIES[continentID]) {
				continent.put(curr, new Territory(curr));
			}

			System.out.println(GameData.CONTINENT_NAMES[id] + " was created, " + totalTerritories + " total territories");
		}

		/**
		 * <strong>ratio()</strong> - Finds the ratio of territories owned by the bot
		 * @return <em>double</em>, ratio
		 */
		public double ratio() {
			return territoriesOwned/totalTerritories;
		}

		public void updateTerritory(int territoryID, int numUnits, int occupierID) {
			if (getTerritory(territoryID).occupierID == botID ^ occupierID == botID) { // # If the bot lost/gained this territory in the update
				territoriesOwned += (occupierID == botID) ? +1 : -1;  // * If the new ID is the bot's, add 1, if it's not, remove 1
			}
			getTerritory(territoryID).updateTerritory(numUnits, occupierID);
		}

		public void update(BoardAPI board) {
			timesUsedThisTurn = 0;
			for (int territory : GameData.CONTINENT_COUNTRIES[id]) {
				updateTerritory(territory, board.getNumUnits(territory), board.getOccupier(territory));
			}
		}

		public Territory getTerritory(int territoryID) {
			return continent.get(territoryID);
		}

		public Territory[] getTerritories() {
			return continent.values().toArray(new Territory[0]);
		}

		@Override
		public int compareTo(Continent that) {
			return (int) (this.ratio() - that.ratio())*100;
		}
	}

	private static class Territory {
		public int occupierID;
		public int numUnits;
		public int id;

		public Territory(int id) {
			occupierID = -1;
			numUnits = -1;
			this.id = id;
		}

		public void updateTerritory(int numUnits, int occupierID) {
			this.numUnits = numUnits;
			this.occupierID = occupierID;
		}

		public boolean belongsTo(int playerID) {
			return occupierID == playerID;
		}

		@Override
		public String toString() {
			return GameData.COUNTRY_NAMES[id] + " (Occupier: " + occupierID + ", number of troops: " + numUnits + ")" ;
		}
	}

	// ! Not sure about using this one anymore
	private static class Decision implements Comparable<Decision> {
		String command;
		int weight;

		public Decision(int weight, String command) {
			this.command = command;
			this.weight = weight;
		}

		@Override
		public int compareTo(Decision that) {
			return this.weight - that.weight;
		}
	}

    private BoardAPI board;
	private PlayerAPI player;

    private final GameMap map;

    protected static int selectedTerritory; // * Holds the ID of the target of an attack

	// * initialisation: place your troops inside the cluster, place neutrals away from you

    // * 1. Select main territory (or territories)
	// * 2. Place troops 3 at a time
	// * 3. Battle from there, priority on inside continent
	// * 4. Fortify walls

	// ? When you take over the whole continent, build up walls, then try and take over next continent

	// ? What about a constant value ATTACK_LIMIT, would be a value representing a ratio

    BeachArcade(BoardAPI inBoard, PlayerAPI inPlayer) {
        board = inBoard;
        player = inPlayer;

		map = new Map(player.getId());

        map.startTurn(board);
    }

    /**
     * <strong>getName</strong> — Gets the name of the bot.
     *
     * @return String, Bot's name
     */
    public String getName() {
        return ("SPF 75"); // ? What about throwing debug info in here, so whenever the bot's name is used, we get debug stuff
        /*
         *debug will include:
         *	how many countries in total the bot partialSetMatchs/percentage of the board partialSetMatchled
         *	how many connecting countries the bot partialSetMatchs
         *	standard deviation of the troops on each territory
         *	how many cards are in the bot's hand/whether or not the bot can turn in its cards(to see if it is intentionally not turning them in)
         * 	how many whole continents the bot owns
         */
    }

    private String getCommand(Turn turn) {
    	map.startTurn(board);

    	return map.stream().filter(turn::canUse).findFirst().map(turn::getCommand).orElse("skip");
	}

	/**
	 * <p><strong>getPlacement</strong> — For placing of territories in <em>initialisation stage</em>.</p>
	 * <p>Chooses a territory from one of the neutrals to place reinforcements on sending it to the board reinforcement (from the neutral player's reserves) on the selected territory.</p>
	 *
	 * @param forPlayer The index of the player whose territories we are choosing from.
	 * @return String, the name of the territory
	 */
	public String getPlacement(int forPlayer) {
		System.out.println("RE-IMPLEMENT THIS");
		return getRandomName();
	}

    /**
     * <p><strong>getReinforcement</strong> — For <em>reinforcing</em> a territory with reserves.</p>
     * <p>Gets the name of a territory (that belongs to the bot) to place a number of reinforcements (that is valid) onto.</p>
     *
     * @return String, command in the form "Territory Name" "Number of Reinforcements".
     */
    public String getReinforcement() {
		return getCommand(Reinforcement.turn);
    }

    /**
     * <p><strong>getCardExchange</strong> — Gets cards from the bot's hand to exchange for reserves.</p>
     * <p>Returns a valid card exchange command; 3 cards to exchange (just first letter, ie. wia), or "<em>skip</em>" if less than 5 cards.</p>
     *
     * @return String, command using letters to represent the cards
     */
    public String getCardExchange() { // First Iteration Complete
        // Later it might be possible to strategise what hands are optimal, and if you have 2 different ways to turn in cards, find the best one
        int[][] validSets = new int[60][3];
        int[] set = new int[Deck.SET_SIZE];
        int r = 0;
        PriorityQueue<Decision> decisions = new PriorityQueue<>();

        for (int i = 0; (i < Deck.NUM_SETS); i++) {
            System.arraycopy(Deck.SETS[i], 0, set, 0, Deck.SET_SIZE);
            int count = 0;
            ArrayList<Card> copyCards = new ArrayList<Card>(player.getCards()); //Takes the cards in the bot's hand and makes a copy to find possible matches with
            boolean partialSetMatch; // PartialSetMatch is made to cut the loop when a match is found
            for (int j : set) {
                partialSetMatch = false;
                for (int m = 0; (m < copyCards.size()) && !partialSetMatch; m++) {
                    if (j == copyCards.get(m).getInsigniaId()) {
                        copyCards.remove(m);
                        count++;
                        partialSetMatch = true;
                    }
                }
                if (count == Deck.SET_SIZE) {
                    System.arraycopy(set, 0, validSets[r], 0, set.length);
                    r++;
                    count = 0;
                }
            }
        }
        for (int i = 0; i < r; i++) { // Takes the int values of the insignias and turns them into usable answers for the bot
            StringBuilder x = new StringBuilder();
            System.out.println("-----------------------");
            for (int j = 0; j < Deck.SET_SIZE; j++) {
                switch (validSets[i][j]) {
                    case 0:
                        x.append("i");
                        break;
                    case 1:
                        x.append("c");
                        break;
                    case 2:
                        x.append("a");
                        break;
                    case 3:
                        x.append("w");
                        break;
                    default:
                    	System.out.println("Error you messed up");
//                        x.append("Error you messed up");
                }
            }
            int weight = 0;
            for (int j = 0; j < Deck.SET_SIZE; j++) {
                if (validSets[i][j] == 0 || validSets[i][j] == 1 || validSets[i][j] == 2)// Weights a set so that the wild cards are saved for later use  if possible
                    weight += 2;
                else
                    weight += 1;

            }
            String possibleDecision = x.toString();
            decisions.add(new Decision(weight, possibleDecision));
        }
        if (decisions.isEmpty()) {
        	throw new IllegalStateException("No valid set of cards");
		}
//        return ((decisions.isEmpty()) ? (getErrorNullExchange()) : (decisions.poll().command));
		return decisions.poll().command;
    }


    /**
     * <p><strong>getBattle</strong> — For getting an attack command.</p>
     * <p>Used in bot's <em>attack stage</em>. Returns a String containing the country to attack from, the country to attack and the number of units to use, or "skip" to skip attack phase.</p>
     *
     * @return String representing attack command ("from", "to", "units")
     */
    public String getBattle() {
    	return getCommand(Battle.turn);
    }

    /**
     * <p><strong>getDefence</strong> — For getting a defend command.</p>
     * <p>Used when other player is in the <em>attack stage</em>. Returns the number of units to defend a territory with.</p>
     *
     * @param countryId int, ID of territory to defend.
     * @return String, number of units
     */
    public String getDefence(int countryId) { // Always defend with the most units you can to give the best chance of winning
        String command = "";
        if (board.getNumUnits(countryId) >= 2)
            command += 2;
        else
            command += 1;
        return (command);
    }

    /**
     * <p><strong>getMoveIn</strong> — For sending troops after winning battle.</p>
     * <p>Used when you have won a battle and claimed a territory. Sends a number of troops to the newly conquered territory, leaving both territories with at least one troop. </p>
     *
     * @param attackCountryId int, ID of your attacking territory
     * @return String, number of units
     */
    public String getMoveIn(int attackCountryId) {
//        return getCommand(MoveIn.turn);
		// > Logic surrounding the value in `selectedTerritory` and the value in attackCountryId
		Territory attacking = map.getTerritory(attackCountryId);
		return "";
    }

	/**
	 * <p><strong>getFortify</strong> — For selecting territories and a number of troops to exchange.</p>
	 * <p>Return country name to move units from, country name to fortify and number of units to move, or "skip" to skip this stage.</p>
	 * @return String representing fortify command ("from", "to", "units")
	 */
	public String getFortify () {
		return getCommand(Fortify.turn);
	}

	/* Utility Methods */
	public static String getRandomName() {
		return GameData.COUNTRY_NAMES[(int)(Math.random() * GameData.NUM_COUNTRIES)].replaceAll("\\s", "");
	}
	//TODO Change array to whatever decisions are held in
	//TODO Make loop only work for owned territories
	//TODO Make sure command argument is correct
	//TODO Change return statement to reflect the decisions or remove it if it can be added to a field
	//TODO Make the decisions

	/**
	 * Finds all the possible decisions that the player can make
	 * @return the decisions?
	 */
	public int findDecisions(){
		Decision[] decisionsArr = new Decision[42];
		int index = 0;
		//Finds the owned territories and weights them
		for(int i = 0; i < 42; i++){
			if(board.getOccupier(i) == player.getId()){
				decisionsArr[index] = new Decision(findWeight(i), "command");
				index++;
			}
		}
		return 0;
	}

	/** //TODO implement this so it works for attack and fortify as well
	 * Finds the weight of a given territory in terms of if they should place troops or not
	 * If territory is surrounded by friendly territories it will be zero
	 * The weight will be increased by 10 for each adjacent friendly territory
	 * and at least 20 for each opponent territory
	 * @param ID: the territory in question
	 * @return weight: the weight of the decision. Higher weight means higher priority
	 */
	public int findWeight(int ID) {
		int weight = 0;	//The weight of the decision
		boolean isSurrounded = true; //when the territory is surrounded only by territories they own
		//Go thru all the adjacents
		for (int i : GameData.ADJACENT[ID]) {
			//When the adjacent territory is owned by the player
			if (board.getOccupier(i) == player.getId()) {
				weight += 10;
			//when the adjacent territory is not owned by the player
			} else {
				weight += 20;
				//Adds extra for the more troops an adjacent enemy territory has
				weight += board.getNumUnits(i) - board.getNumUnits(ID);
				isSurrounded = false;
			}
		}
		//When the territory is in the middle of other territories that are owned by the player
		if(isSurrounded){
			weight = 0; //There's no strategic value in adding troops to a place like this
		}
		return weight;
	}

	/* * Nested Turn * */
	private static abstract class Turn {
		protected double ratio;

		// * Based on the ratio of the continent, get a command
		public abstract String getCommand(Continent continent);

		public boolean canUse(Continent continent) {
			return true;
		}

		public String cancel() {
			return "skip";
		}
	}
	private static class Reinforcement extends Turn {
		public static final Turn turn = new Reinforcement();

		// ! SEE DISCORD FOR DETAILS BEFORE IMPLEMENTING
		@Override
		public String getCommand(Continent continent) {
			return null;
		}

		@Override
		public boolean canUse(Continent continent) {
			// > Would return whether this continent has reached our "safe" condition
			// ! The "safe" condition should be based on the port of entry territories having a certain amount of troops in proportion to its bordering territory (if an enemy)
			// ! By only doing this if its an enemy (and moving our priority to the bordering friendly territory), we prevent a soft lock state where we've almost won the game, but are fortifying unnecessarily.
			// ! I won't lie, this is a bad explanation, but we can talk about it more later
			return true;
		}

		@Override
		public String cancel() {
			throw new IllegalStateException("You can't skip reinforcement stage, an error has occurred");
		}
	}

	private static class Battle extends Turn {
		public static final Turn turn = new Battle();
		public int attackingTerritory;
		public int defendingTerritory;

		// ! SEE DISCORD FOR DETAILS BEFORE IMPLEMENTING
		@Override
		public String getCommand(Continent continent) {

			return null;
		}

		@Override
		public boolean canUse(Continent continent) {
			// ?
			return false;
		}
	}

//	private static class MoveIn extends Turn {
//		public static final Turn turn = new MoveIn();
//
//		// ! SEE DISCORD FOR DETAILS BEFORE IMPLEMENTING
//		@Override
//		public String getCommand(Continent continent) {
//			return null;
//		}
//
//		@Override
//		public boolean canUse(Continent continent) {
//			return continent.contains();
//		}
//	}

	private static class Fortify extends Turn {
		public static final Turn turn = new Fortify();

		// ! SEE DISCORD FOR DETAILS BEFORE IMPLEMENTING
		@Override
		public String getCommand(Continent continent) {
			return null;
		}

		@Override
		public boolean canUse(Continent continent) {
			return false;
		}
	}
}