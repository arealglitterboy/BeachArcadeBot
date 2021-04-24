/* BeachArcade bot: Ethan Chan, Blake Whittington, Ben Brown */

import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Stream;

public class BeachArcade implements Bot {
    private BoardAPI board;
	private PlayerAPI player;

    private static GameMap map;

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

    	for (Continent continent : map) {
    		if (turn.canUse(continent)) {
    			return turn.getCommand(continent);
			}
		}
    	return map.stream().filter(turn::canUse).findFirst().map(turn::getCommand).orElse("skip");
	}

	/**
	 * <p><strong>getPlacement</strong> — For placing of territories in <em>initialisation stage</em>.</p>
	 * <p>Chooses a territory from one of the neutrals to place reinforcements on sending it to the board reinforcement (from the neutral player's reserves) on the selected territory.</p>
	 * @param forPlayer The index of the player whose territories we are choosing from.
	 * @return String, the name of the territory
	 */
	public String getPlacement(int forPlayer) {
		map.startTurn(board);
		for(int i = 0; i < 6; i++){
			System.out.println(i + ": " + map.getContinent(i).toString());

		}		//! ratio will be defined as .66 for now
		System.out.println("Running get placement...");
		System.out.println("BotID: " + map.getBotID() + "\tPlayerID: " + forPlayer );
		System.out.println("Continent ID: " + map.getContinent(0).id);
		for(Territory t: map.getContinent(0).getTerritories()){
			System.out.println(t.toString());
		}
		double ratio = .66;
		Continent continent; //Will be initialized as the highest or lowest priority for bot or neutral respectively
		int ratioCase; //Cases: // 1.) Over // 2.) Under // 3.) 100%
		if(forPlayer == map.getBotID()){ //Case for the bot
			continent = map.getContinent(0); //Set it to the highest priority territory
		} else { //Case for the neutral player
			continent = map.getContinent(5); //Set it to the lowest priority territory
		}
		ratioCase = (map.getRatio(continent.id) == 1? 3 : (map.getRatio(continent.id) > ratio? 1 : 2)); //sets the case to over under or 100%
		String b =  findLowest(continent, forPlayer).toString();
		System.out.println("DEBUG 2: " + b);
		return b;
	}
	//* This is supposed to be used to evenly distribute troops among a continent
	/**
	 * Finds the territory on a continent with the lowest number of troops
	 * @param continent: the given continent
	 * @param playerID: the player's id
	 * @return the territory with the lowest amount of troops
	 */
	String findLowest(Continent continent, int playerID){
		Territory terr = null; //will be territory with the lowest troops
		boolean hasTerr = false;
		for(int i = 0; i < continent.totalTerritories; i++){
			if(continent.getTerritory(i).occupierID == playerID){
				terr = map.getTerritory(i);
				hasTerr = true;
			}
		}
		if(!hasTerr){
			System.out.println("Terr is null. This is no good");
		}
		else
			System.out.println("DEBUG: " + terr.toString());
		return terr.getName();
	}
	//? We can probably take this out. But we may be able to use it for strategize
	/**
	 * Method to find the territory best suited for the ratio of the highest priority continent
	 * @param continent: the highest priority continent
	 * @param ratioCase: the number corresponding to the ratio
	 * @param playerID: the player's id
	 * @return the territory ID of the highest rated territory
	 */
	private int rate(Continent continent, int ratioCase, int playerID){
		//Gets an array of all the owned territories to make things faster
		int numOwnedTerritories = 0;
		Territory compareTo;
		Territory[] ownedTerritories = new Territory[continent.getTerritories().length];
		int[] weights = new int[ownedTerritories.length];

		for (int i = 0; i < continent.getTerritories().length; i++) {
			if(continent.getTerritories()[i].belongsTo(playerID)){
				ownedTerritories[numOwnedTerritories++] = continent.getTerritory(i);
			}
		}
		if(ratioCase == 1) {
			for(int i = 0; i < ownedTerritories.length && ownedTerritories[i] != null; i++){
				compareTo = ownedTerritories[i];
				weights[i] = 0;
				//check all adjacent territories
				for(int j = 0; j < GameData.ADJACENT[compareTo.id].length; i++){
					if(compareTo.id == GameData.ADJACENT[compareTo.id][j]) { //if its owned by the player
						weights[i] += 1;
					}
					else{
						weights[i] += 25;
					}
				}
			}
		} else if(ratioCase == 2){

		} else{
			//Maybe try to find entry into other continents??

		}
		return 0;
	}

	/* Utility Methods */
	public static String getRandomName() {
		return GameData.COUNTRY_NAMES[(int) (Math.random() * GameData.NUM_COUNTRIES)].replaceAll("\\s", "");
	}

    /**
     * <p><strong>getReinforcement</strong> — For <em>reinforcing</em> a territory with reserves.</p>
     * <p>Gets the name of a territory (that belongs to the bot) to place a number of reinforcements (that is valid) onto.</p>
     *
     * @return String, command in the form "Territory Name" "Number of Reinforcements".
     */
    public String getReinforcement() {
		//return getCommand(Reinforcement.turn);
		return getPlacement(map.getBotID()) + " 3";
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
		return decisions.poll().command;
    }

	/**
	 * This is the case when the bot controlls a co
	 * @param continent
	 * @return
	 */
	private Territory attackFromOtherContinent(Continent continent) {
		Territory highestOwned = null; //The friendly territory with the most amount of troops on another continent
		ArrayList<Territory> owned = new ArrayList<Territory>();

		for (Territory t : continent.getTerritories()) { //Loop thru all territories in the continent
			for (int id : GameData.ADJACENT[t.id]) { //loop thru all adjacents

				if (map.belongsTo(id, map.getBotID())) { //When the territory is owned by the bot
					owned.add(map.getTerritory(id));

					if (highestOwned == null) {	//the first time the loop finds an owned territory
						highestOwned = owned.get(0);
					} else {
						if (highestOwned.numUnits - map.getTerritory(id).numUnits <	//When the the current territory has a better chance of winning than the other
								t.numUnits - map.getTerritory(id).numUnits) {
							highestOwned = t;
						}
					}
				}
			}
		}
		return highestOwned;
	}

	/**
	 * Used to find new territories on an adjacent continent
	 * when the ratio is 100%
	 * @param continent: Continent with the highest priority
	 * @return the best place to attack from
	 */
	private Continent findNewCont(Continent continent){
		Continent best = null; //Used to find the adjacent continent with the highest priority
		Continent compare; //The continent that will be compared to best
		ArrayList<Territory> criticalPoints= new ArrayList<Territory>(); //Stores the entry points to other continents

		/*Go through all the adjacents of every territory in the continent and find
		  the territories are in other continents (Critical Points) */
		for(Territory t: continent.getTerritories()){ //loop through all territories
			for(int id: GameData.ADJACENT[t.id]){ //Loop through all adjacents
				if(GameData.CONTINENT_IDS[id] != continent.id){ //When the adjacent territory is in another continent
					criticalPoints.add(map.getTerritory(id));
				}
			}
		}
		for(Territory t: criticalPoints){
			compare = map.getContinent(GameData.CONTINENT_IDS[t.id]);
			if(best == null){ //The first time the loop runs
				best =  compare; //Set best to the first continent if its null
			}
			//When the adjacent continent has a higher priority than the current best
			if(best.ratio() < compare.ratio() && compare.ratio() < 1){
				best = compare; //Set best to compare because it has a higher priority
			}
		}
    	return best;
	}
	public Territory findAttackSource(Continent continent, int playerID){
		//! figure out what threshold should be for now its .5
		double THRESHOLD = .5;
		double ratio = continent.ratio();
		Territory source = null;
		//Go through all territories on that continent
		Territory[] territories = continent.getTerritories();
		if(ratio == 1){ //when ratio is 100%
			//look thru adjacents to find one in another continent
			source = findAttackSource(findNewCont(continent), playerID);
		} else if (ratio > THRESHOLD){
			//Find adjacent enemy territory with the smallest number of troops
		} else {
			//Attack only territory with the lowest amount of troops
			//Make sure there is still a decent number left on it to fortify?
		}
		return source;
	}
	//TODO make sure itll take over a continent if its able
	public Territory getAttackTarget(Territory source){
    	Territory target = map.getTerritory(GameData.ADJACENT[source.id][0]);
    	for(int id: GameData.ADJACENT[source.id]){
    		if(source.numUnits - map.getTerritory(id).numUnits
					> source.numUnits - target.numUnits){
    			target = map.getTerritory(id);
			}
		}
    	return target;
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

	/* * Internal Methods * */
	private interface GameMap extends Iterable<Continent> {
		int getBotID();
		Territory getTerritory(int territory);
		Continent getContinent(int continent);
		Territory[] getTerritories(int continent);
		Territory[] getAdjacents(int territory);
		double getRatio(int continent);
		int getAttacking();
		int getDefending();

		boolean belongsTo(int territoryID, int playerID);
		boolean belongsTo(int territoryID);

		void setAttacking(int territory);
		void setDefending(int territory);

		void startTurn(BoardAPI board);
		void updateTerritory(int territory, int numUnits, int occupierID);
		Stream<Continent> stream();
	}

	private static class Map extends ArrayList<Continent> implements GameMap {
		private final int[] indexes; // * Holds the indexes of the continents in the arraylist based on their ID
		public final int botID;
		public int attacking, defending; // * Holds the currently targeted territory

		public Map(int playerID) {
			botID = playerID;
			indexes = new int[GameData.NUM_CONTINENTS];

			for (int index = 0; index < GameData.NUM_CONTINENTS; ++index) {
				add(index, new Continent(index, botID));
				indexes[index] = index;
			}
		}

		@Override
		public int getBotID() {
			return botID;
		}

		@Override
		public Territory getTerritory(int territory) {
			return getContinent(getContinentID(territory)).getTerritory(territory);
		}

		@Override
		public Continent getContinent(int continent) {
			return get(indexes[getContinentID(continent)]);
		}

		@Override
		public Territory[] getTerritories(int continent) {
			return getContinent(continent).getTerritories();
		}

		@Override
		public Territory[] getAdjacents(int territory) {
			int length = GameData.ADJACENT[territory].length;
			Territory[] adjacents = new Territory[GameData.ADJACENT[territory].length];

			for (int i = 0; i < length; ++i) {
				adjacents[i] = getTerritory(GameData.ADJACENT[territory][i]);
			}

			return adjacents;
		}

		@Override
		public double getRatio(int continent) {
			return getContinent(continent).ratio();
		}

		@Override
		public int getAttacking() {
			return attacking;
		}

		@Override
		public int getDefending() {
			return defending;
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
		public void setAttacking(int territory) {
			attacking = territory;
		}

		@Override
		public void setDefending(int territory) {
			defending = territory;
		}

		@Override
		public void startTurn(BoardAPI board) {
			forEach(continent -> continent.update(board)); // * Send the board to each continent to get its changes
			sort(Continent::compareTo); // * Sort the array list based on the priority level

			for (int i = 0; i < GameData.NUM_CONTINENTS; ++i) {
				indexes[get(i).id] = i; // * Update the indexes of the continents in the arraylist
			}
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
		public int getContinentID(int territory) {
			return GameData.CONTINENT_IDS[territory];
		}
	}

	private static class Continent implements Comparable<Continent>, Iterable<Territory> {
		public final int id, botID;
		private final TreeMap<Integer, Territory> continent;
		private final double totalTerritories;
		private int territoriesOwned;
		private int botTroops, opponentTroops, neutralTroops;
		private int timesUsedThisTurn; // * Might be useful to put a cap on the number of turns in a move, but still allow multiple moves on different territories
		// * As in, if we have multiple continents at 100%, we don't just focus on one if we are capable of doing multiple moves.

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

		private void updateTroops(Territory territory, int sign) {
			if (territory.belongsTo(map.getBotID())) {
				botTroops += sign * territory.numUnits;
			} else if (territory.belongsTo((map.getBotID() + 1) % 2)) {
				opponentTroops += sign * territory.numUnits;
			} else {
				neutralTroops += sign * territory.numUnits;
			}
		}

		public void updateTerritory(int territoryID, int numUnits, int occupierID) {
			updateTroops(getTerritory(territoryID), -1); // * Remove the troops associated with this territory

			if (getTerritory(territoryID).occupierID == botID ^ occupierID == botID) { // # If the bot lost/gained this territory in the update
				territoriesOwned += (occupierID == botID) ? +1 : -1;  // * If the new ID is the bot's, add 1, if it's not, remove 1
			}
			getTerritory(territoryID).updateTerritory(numUnits, occupierID);

			updateTroops(getTerritory(territoryID), 1); // * Re add the (possibly altered) troops associated with this territory
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

		@Override
		public Iterator<Territory> iterator() {
			return continent.values().iterator();
		}
		public String toString(){
			return GameData.CONTINENT_NAMES[id];
		}
	}

	private static class Territory {
		public final int id;
		public final String name;
		public int occupierID;
		public int numUnits;


		public Territory(int id) {
			this.id = id;
			name = GameData.COUNTRY_NAMES[id];

			occupierID = -1;
			numUnits = -1;
		}

		public void updateTerritory(int numUnits, int occupierID) {
			this.numUnits = numUnits;
			this.occupierID = occupierID;
		}

		public boolean belongsTo(int playerID) {
			return occupierID == playerID;
		}

		public String getName() {
			return name;
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

	private static class MoveIn extends Turn {
		public static final Turn turn = new MoveIn();

		// ! SEE DISCORD FOR DETAILS BEFORE IMPLEMENTING
		@Override
		public String getCommand(Continent continent) {
			return null;
		}

		@Override
		public boolean canUse(Continent continent) {
			return continent == map.getContinent(map.getAttacking()); // * For example
		}
	}

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
	//! I think this might be useful for strategize
	//! but if we don't need it feel free to get rid of it
	/**
	 * <p> I Dont really know what to do with this so for now I stick it down here :) </p>
	 * <p> //TODO implement this so it works for attack and fortify as well </p>
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

	public static void main(String[] args) {
		Board b = new Board();
		BeachArcade ass = new BeachArcade(b, new Player(0));

		System.out.println("Running in the main " + ass.getPlacement(0));
	}
}
