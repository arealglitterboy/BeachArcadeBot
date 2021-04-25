/* BeachArcade bot: Ethan Chan, Blake Whittington, Ben Brown */

import java.util.*;
import java.util.stream.Stream;

public class BeachArcade implements Bot {
    private BoardAPI board;
    private PlayerAPI player;

    public GameMap map;

    public Placement placement = new Placement();
    public Reinforcement reinforcement = new Reinforcement();
    public Battle battle = new Battle();
    public MoveIn moveIn = new MoveIn();
    public Fortify fortify = new Fortify();

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

	/* Utility Methods */
	public static String getRandomName() {
		return GameData.COUNTRY_NAMES[(int) (Math.random() * GameData.NUM_COUNTRIES)].replaceAll("\\s", "");
	}

    private String getCommand(Turn turn) {
		System.out.println(turn.getClass().getSimpleName() + ":");
		map.startTurn(board); // * Update the map to the changes made on the board

		System.out.println();
    	String command = turn.getCommand(); // * Get the command for this turn.
    	System.out.println("====================================\nCommand: " + command + "\n====================================");
    	return command.toLowerCase();
    }

	/**
	 * <p><strong>getPlacement</strong> — For placing of territories in <em>initialisation stage</em>.</p>
	 * <p>Chooses a territory from one of the neutrals to place reinforcements on sending it to the board reinforcement (from the neutral player's reserves) on the selected territory.</p>
	 * @param forPlayer The index of the player whose territories we are choosing from.
	 * @return String, the name of the territory
	 */
	public String getPlacement(int forPlayer) {
    	map.setForPlayer(forPlayer);
    	return getCommand(placement);
	}

    /**
     * <p><strong>getReinforcement</strong> — For <em>reinforcing</em> a territory with reserves.</p>
     * <p>Gets the name of a territory (that belongs to the bot) to place a number of reinforcements (that is valid) onto.</p>
     *
     * @return String, command in the form "Territory Name" "Number of Reinforcements".
     */
    public String getReinforcement() {
		return getCommand(reinforcement);
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
     * This is the case when the bot controls a co
     *
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

                    if (highestOwned == null) {    //the first time the loop finds an owned territory
                        highestOwned = owned.get(0);
                    } else {
                        if (highestOwned.numUnits - map.getTerritory(id).numUnits <    //When the the current territory has a better chance of winning than the other
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
     *
     * @param continent: Continent with the highest priority
     * @return the best place to attack from
     */
    private Continent findNewCont(Continent continent) {
        Continent best = null; //Used to find the adjacent continent with the highest priority
        Continent compare; //The continent that will be compared to best
        ArrayList<Territory> criticalPoints = new ArrayList<Territory>(); //Stores the entry points to other continents

		/*Go through all the adjacents of every territory in the continent and find
		  the territories are in other continents (Critical Points) */
        for (Territory t : continent.getTerritories()) { //loop through all territories
            for (int id : GameData.ADJACENT[t.id]) { //Loop through all adjacents
                if (GameData.CONTINENT_IDS[id] != continent.id) { //When the adjacent territory is in another continent
                    criticalPoints.add(map.getTerritory(id));
                }
            }
        }
        for (Territory t : criticalPoints) {
            compare = map.getContinent(GameData.CONTINENT_IDS[t.id]);
            if (best == null) { //The first time the loop runs
                best = compare; //Set best to the first continent if its null
            }
            //When the adjacent continent has a higher priority than the current best
            if (best.ratio() < compare.ratio() && compare.ratio() < 1) {
                best = compare; //Set best to compare because it has a higher priority
            }
        }
        return best;
    }

    public Territory findAttackSource(Continent continent, int playerID) {
        //! figure out what threshold should be for now its .5
        double THRESHOLD = .5;
        double ratio = continent.ratio();
        Territory source = null;
        //Go through all territories on that continent
        Territory[] territories = continent.getTerritories();
        if (ratio == 1) { //when ratio is 100%
            //look thru adjacents to find one in another continent
            source = findAttackSource(findNewCont(continent), playerID);
        } else if (ratio > THRESHOLD) {
            //Find adjacent enemy territory with the smallest number of troops
        } else {
            //Attack only territory with the lowest amount of troops
            //Make sure there is still a decent number left on it to fortify?
        }
        return source;
    }

    //TODO make sure itll take over a continent if its able
    public Territory getAttackTarget(Territory source) {
        Territory target = map.getTerritory(GameData.ADJACENT[source.id][0]);
        for (int id : GameData.ADJACENT[source.id]) {
            if (source.numUnits - map.getTerritory(id).numUnits
                    > source.numUnits - target.numUnits) {
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
        return getCommand(battle);
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
        return "1";
    }

    /**
     * <p><strong>getFortify</strong> — For selecting territories and a number of troops to exchange.</p>
     * <p>Return country name to move units from, country name to fortify and number of units to move, or "skip" to skip this stage.</p>
     *
     * @return String representing fortify command ("from", "to", "units")
     */
    public String getFortify() {
        return getCommand(fortify);
    }

	/* * Internal Methods * */
	private interface GameMap extends Iterable<Continent> {
		int getBotID();
		int getForPlayer();
		Territory getTerritory(int territory);
		Continent getContinent(int continent);
		Territory[] getTerritories(int continent);
		Territory[] getAdjacents(int territory);
		double getRatio(int continent);
		int getAttacking();
		int getDefending();

		boolean belongsTo(int territoryID, int playerID);
		boolean belongsTo(int territoryID);

		void setForPlayer(int forPlayer);
		void setAttacking(int territory);
		void setDefending(int territory);

		void startTurn(BoardAPI board);
		void updateTerritory(int territory, int numUnits, int occupierID);
		Stream<Continent> stream();
	}

    private class Map extends ArrayList<Continent> implements GameMap {
        private final int[] indexes; // * Holds the indexes of the continents in the arraylist based on their ID
        public final int botID;
        public int forPlayer;
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
        public int getForPlayer() {
            return forPlayer;
        }

        @Override
        public Territory getTerritory(int territory) {
            return findContinent(territory).getTerritory(territory);
        }

        private Continent findContinent(int territory) {
            return get(indexes[getContinentID(territory)]);
        }

        private Continent getCanonicalContinent(int continent) {
            return get(indexes[continent]);
        }

        @Override
        public Continent getContinent(int continent) {
            return get(continent);
        }

        @Override
        public Territory[] getTerritories(int continent) {
            return getCanonicalContinent(continent).getTerritories();
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
            return getCanonicalContinent(continent).ratio();
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
        public void setForPlayer(int forPlayer) {
            this.forPlayer = forPlayer;
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

//			System.out.println();
			for (int i = 0; i < GameData.NUM_CONTINENTS; ++i) {
//				System.out.println("Updated Order: " + get(i).id + ", " + get(i).name + ", priority = " + get(i).ratio());
				indexes[get(i).id] = i; // * Update the indexes of the continents in the arraylist
			}
//			System.out.println();
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

    public static final int[] portsOfEntry = {3, 4, 5, 1, 2, 3};
    public static final int[] portIDs = {0, 0, 0, 0, 1, 0, 0, 1, 1, // North America
										0, 1, 1, 1, 0, 1, 0, // Europe
										1, 0, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, // Asia
										0, 0, 0, 1, // Australia
										1, 0, 1, 0, // South America
										0, 1, 0, 1, 1, 0 // Africa
								};
//    static {
//    	System.out.println(portIDs.length);
//    	int count = 0;
//    	for (int[] arr : GameData.CONTINENT_COUNTRIES) {
//    		count += arr.length;
//			System.out.println(arr.length);
//		}
//		System.out.println(count);
//	}
//    public static final int[] portIDs = {0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0};

    private class Continent implements Comparable<Continent>, Iterable<Territory> {
        public final int id, botID;
        public final String name;
        private final TreeMap<Integer, Territory> continent;
        private final Territory[] portTerritories;


        private final double totalTerritories;
        private int territoriesOwned;
        private int botTroops, opponentTroops, neutralTroops;
        private int timesUsedThisTurn; // * Might be useful to put a cap on the number of turns in a move, but still allow multiple moves on different territories
        // * As in, if we have multiple continents at 100%, we don't just focus on one if we are capable of doing multiple moves.

        public Continent(int continentID, int botID) {
            id = continentID;
            name = GameData.CONTINENT_NAMES[id];
            this.botID = botID;
            continent = new TreeMap<>();
            portTerritories = new Territory[portsOfEntry[id]];
            botTroops = 0;
            opponentTroops = 0;
            neutralTroops = 0;

            totalTerritories = GameData.CONTINENT_COUNTRIES[continentID].length;

            int i = 0;
            for (int curr : GameData.CONTINENT_COUNTRIES[continentID]) {
                continent.put(curr, new Territory(curr));
                if (portIDs[curr] == 1) {
                    portTerritories[i] = continent.get(curr);
                    i++;
                }
            }

            System.out.println(GameData.CONTINENT_NAMES[id] + "(" + id + ")" + " was created, " + totalTerritories + " total territories");
        }

        /**
         * <strong>ratio()</strong> - Finds the ratio of territories owned by the bot
         *
         * @return <em>double</em>, ratio
         */
        public double ratio() {
            return territoriesOwned / totalTerritories;
//			return ((botTroops)/(0.0 + botTroops + neutralTroops + opponentTroops)) * (territoriesOwned / totalTerritories);
        }

		/**
		 * <strong>proportion()</strong> - Finds the proportion of the bot's troops on the continent in relation to all troops on the continent.
		 * If the bot has 20 of the 50 troops on the continent, will return 0.4
		 * @return double, fraction 0 to 1
		 */
		public double proportion() {
			return (botTroops)/(0.0 + botTroops + neutralTroops + opponentTroops);
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

        public void updateTerritory(Territory territory, int numUnits, int occupierID) {
//            removeTroops(territory.numUnits, territory.occupierID); // * Remove the troops associated with this territory

            if (territory.belongsTo(botID) ^ occupierID == botID) { // # If the bot lost/gained this territory in the update
                territoriesOwned += (occupierID == botID) ? +1 : -1;  // * If the new ID is the bot's, add 1, if it's not, remove 1
            }
            territory.updateTerritory(numUnits, occupierID);

//            addTroops(territory.numUnits, territory.occupierID); // * Re add the (possibly altered) troops associated with this territory
        }

        public void update(BoardAPI board) {
            timesUsedThisTurn = 0;
            opponentTroops = 0;
            botTroops = 0;
            neutralTroops = 0;
            for (Territory territory : continent.values()) {
                int terrID = territory.id;
                if (board.getOccupier(terrID) < 2){
                    if (board.getOccupier(terrID) == botID) {
                        botTroops += board.getNumUnits(terrID);
                    } else {
                        opponentTroops += board.getNumUnits(terrID);
                    }
                } else {
                    neutralTroops += board.getNumUnits(terrID);
                }
                updateTerritory(territory, board.getNumUnits(terrID), board.getOccupier(terrID));
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
            System.out.println(this.name + " compared to " + that.name + " was " + (this.ratio() - that.ratio()) * 100);
            return Double.compare(that.ratio(), this.ratio());
        }

        @Override
        public Iterator<Territory> iterator() {
            return continent.values().iterator();
        }

        public Territory minPort(double safe) {
			List<Territory> portList = new ArrayList<>();
			for (Territory portTerritory : portTerritories) {
			    if (portTerritory.belongsTo(botID)) {
			        portList.add(portTerritory);
                }
            }

            portList.sort(Territory::compareAdjacents);
            portList.removeIf(portTerritory -> portTerritory.numAdjacentUnowned() == 0);

            return (portList.size() > 0 && portList.get(0).compareToAdjacents() > safe) ? portList.get(0) : null;
        }

        public String toString() {
            return name + ", " + id;
        }

        /**
         * <strong>territories()</strong> - Returns a stream of the bots owned territories in this continent
         *
         * @return stream of Territory
         */
        public Stream<Territory> territories() {
            return territories(botID);
        }

        public Stream<Territory> streamUnowned() {
            return continent.values().stream().filter(curr -> !curr.belongsTo(botID));
        }


        /**
         * <strong>territories()</strong> - Returns a stream of the territories owned by a player in this continent
         *
         * @param playerID: id of a player
         * @return stream of Territory
         */
        public Stream<Territory> territories(int playerID) {
            return continent.values().stream().filter(curr -> curr.belongsTo(playerID));
        }
    }

    private class Territory implements Comparable<Territory> {
        public final int id;
        public final String name;
        public int occupierID;
        public int numUnits;

        public Territory(int id) {
            this.id = id;
            name = GameData.COUNTRY_NAMES[id].toLowerCase(Locale.ROOT).replaceAll("//s+", "");

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
            return GameData.COUNTRY_NAMES[id] + " (Occupier: " + occupierID + ", number of troops: " + numUnits + ")";
        }

        public int compareTo(Territory that) {
            return Integer.compare(that.numUnits, this.numUnits);
        }

        public int minCompare(Territory that) {
            return -1 * compareTo(that);
        }

        public int maxCompare(Territory that) {
            return compareTo(that);
        }

        public double compareToAdjacents() {
            double counterTroops = 0;

            for (Territory adjacent : map.getAdjacents(id)) {
                System.out.println("name of adjacent = " + adjacent);
                counterTroops += (!adjacent.belongsTo(occupierID)) ? adjacent.numUnits : 0;
            }

            System.out.println(name + " compare to Adjacents: " + counterTroops / numUnits + ", counter troops = " + counterTroops + " and num units = " + numUnits);
            return counterTroops / numUnits;
        }

        public double compareToOpponent() {
        	int opponent = (map.getBotID() + 1) % 2;
			double counterTroops = 0;

			if (occupierID == map.getBotID()){
				for (Territory adjacent : map.getAdjacents(id)) {
					counterTroops += (adjacent.belongsTo(opponent)) ? adjacent.numUnits : 0;
				}
				System.out.println(name + " compare to Opponent: " + counterTroops / numUnits + ", counter troops = " + counterTroops + " and num units = " + numUnits);
			}

			return counterTroops / numUnits;
		}

        public int compareAdjacents(Territory that) {
            return Integer.compare(this.numAdjacentUnowned(), that.numAdjacentUnowned());
        }

        public int numAdjacentUnowned() {
            int count = 0;
            Territory[] arr = map.getAdjacents(id);
            if (arr.length != 0) {
                for (Territory territory : arr) {
                    if (!territory.belongsTo(occupierID)) {
                        ++count;
                    }
                }
            }
            return count;
        }

        public String territoryName() {
            return GameData.COUNTRY_NAMES[id];
        }
    }


    // ! Not sure about using this one anymore
    private class Decision implements Comparable<Decision> {
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

    public interface Check {
        boolean find(Territory check);
    }

    public interface Find {
        Territory territory(Continent continent);
    }

    private abstract class Turn {
        protected double ratio;

        // * Based on the ratio of the continent, get a command
        public abstract String getCommand();

        public Territory getTerritory(Find find) {
            Territory territory = null;
            for (Continent continent : map) {
//                Continent continent = map.getContinent(i);
                if ((territory = find.territory(continent)) != null) {
                    return territory;
                }
            }
            return null;
        }

        public boolean canUse(Continent continent) {
            return true;
        }

        public String cancel() {
            return "skip";
        }
    }
    private class Placement extends Turn {

        @Override
        public String getCommand() {
            Territory out = null;
            double otherRatio = 0;
            double currRatio;
            int forPlayer = map.getForPlayer();
            for (int i = GameData.NUM_CONTINENTS - 1; out == null && i >= 0; i--) {
                out = map.getContinent(i).territories(forPlayer).min(Territory::minCompare).orElse(null);
            }
            for (int i = GameData.NUM_CONTINENTS - 1; i >= 0; i--) {
                currRatio = otherPlayerRatio(map.getContinent(i));
                if (currRatio > otherRatio) {
                    otherRatio = currRatio;
                    for (Territory territory : map.getContinent(i)) {
                        if (territory.belongsTo(forPlayer) && territory.numUnits < 3) { // * If the neutral has pieces in this territory, find the min troops, otherwise null (continuing the loop)
                            out = territory;
                        }

                    }
                }
            }
            if (out == null) {
                throw new IllegalStateException("No neutral territory was found");
            }
            return out.name.replaceAll("\\s+", "");
        }

        public double otherPlayerRatio(Continent continent) {
            double total = 0;
            for (Territory t : continent) {
                if (t.belongsTo((map.getBotID() + 1) % 2)) {
                    total++;
                }
            }
            return total / continent.getTerritories().length;
        }

        @Override
        public String cancel() {
            throw new IllegalStateException("Can't cancel placement of neutrals");
        }
    }

    private class Reinforcement extends Turn {
        public double ratio = 0.3;
        public double aggressive = 4;
        public double safe = 0.6;

		@Override
		public String getCommand() {
			Territory territory;
			for (int i = 0; i < GameData.NUM_CONTINENTS; ++i) {
				System.out.println(map.getContinent(i));
			}
            // * Could replace with a for loop and a switch statement. Loop terminates when the territory has been selected.

			territory = getTerritory(full); // * Get the port with the lowest proportion of troops to adversaries, or null if safe
			if (territory == null) {
			    territory = getTerritory(partial1);
            }
            if (territory == null) {
                territory = getTerritory(partial2);
            }
            if (territory == null) {
                territory = getTerritory(lastSearch);
            }


            if (territory == null) {
                System.out.println("**************************************************\nReinforcements failed\n**************************************************");
                return getRandomName() + " " + getNumUnits();
            }

            return territory.name.replaceAll("\\s+", "") + " " + getNumUnits();
        }

        private int getNumUnits() {
            return Math.max(1, player.getNumUnits() / 2);
        }

		private final Find full = continent -> (continent.ratio() == 1) ? continent.minPort(safe) : null;
		private final Find partial1 = continent -> (continent.ratio() >= ratio && continent.proportion() < safe) ? partialRatio(continent) : null;
		private final Find partial2 = continent -> findNextToAdjacent(continent, check -> check != null && map.belongsTo(check.id));
		private final Find lastSearch = this::belowRatio;

//        private Territory fullRatio(Continent continent) {
//            return continent.minPort(safe);  // * Get the port with the lowest proportion of troops to adversaries, or null if safe
//        }

        private Territory partialRatio(Continent continent) {
            Territory port = continent.minPort(safe);
            Territory defend = continent.territories().max(Territory::compareAdjacents).filter(curr -> curr.compareToAdjacents() < aggressive).orElse(null);
            return (port != null) ? port : defend;
        }
//
//        private final Check partial = check -> check != null && map.belongsTo(check.id) && check.compareToAdjacents() <= aggressive;
//        private final Check below = check -> check != null && map.belongsTo(check.id);


        private Territory findNextToAdjacent(Continent continent, Check filter) {
            for (Territory curr : continent) {
                if (!curr.belongsTo(continent.botID)) {
                    System.out.println(continent.name + " find next to adjacent " + curr);
                    for (Territory adjacent : map.getAdjacents(curr.id)) {
                        if (filter.find(adjacent)) {
                            System.out.println("\tSelected:" + adjacent);
                            return adjacent;
                        }
                    }
                }
            }
            return null;
        }

        private Territory belowRatio(Continent continent) {
            Territory temp = continent.territories().max(Territory::maxCompare).orElse(null);
            return (temp == null || temp.compareToAdjacents() < ratio) ? null : temp; // * If the territory has enough troops already that it could take on those around it.
        }

        @Override
        public boolean canUse(Continent continent) {
            return true;
        }

        @Override
        public String cancel() {
            throw new IllegalStateException("You can't skip reinforcement stage, an error has occurred");
        }
    }

    private class MoveIn extends Turn {

        @Override
        public String getCommand() {
            return "1";
        }

        @Override
        public boolean canUse(Continent continent) {
            return continent == map.getContinent(map.getAttacking());
        }
    }

    private class Battle extends Turn {
        private Territory attack;
        private Territory defend;
        @Override
        public String getCommand() {
            attack = null;
            defend = null;
            String command;
            int numTroops = 1;
            String attackStr, defendStr;
            for (int i = 0; i < GameData.NUM_CONTINENTS - 1; i++) { //Traverse thru all continents with the highest priority first
                if (map.getContinent(i).ratio() < 1) {
                    System.out.println("Condition 1 is being run ~1000");
                    conquerRemainingTerritories(map.getContinent(i));
                } else if (map.getContinent(i).ratio() == 1) {
                    System.out.println("Condition 2 is being run ~1000");
                    conquerRemainingTerritories(findAdjContinent(map.getContinent(i)));
                }

            }
            if(attack == null || defend == null){
                System.out.println("For some reason you can't attack");
                command = "skip";
            } else {
                attackStr = attack.name.replaceAll("\\s","");
                defendStr = defend.name.replaceAll("\\s","");
                if(attack.numUnits <= 3){
                    numTroops = attack.numUnits - 1;
                } else {
                    numTroops = 3;
                }
                command = attackStr + " " + defendStr + " " + numTroops;
            }
            return command;
        }

        public void conquerRemainingTerritories(Continent continent) { //goes thorugh all territories in the continent, f
            Territory[] allTerritories = continent.getTerritories();
            Territory[] adjacentTerritories;
            Territory[] attackDefend =  new Territory[2];
            String defender;
            for (Territory currentTerritory : allTerritories) {
                if(!currentTerritory.belongsTo(map.getBotID())) {
                    adjacentTerritories = map.getAdjacents(currentTerritory.id);
                    for (Territory adj : adjacentTerritories) { //cycle through adjacents to the current territory
                        if (adj.belongsTo(map.getBotID()) && adj.numUnits >= 2) {
//                        botAttacking = adj;
//                        enemyDefending = currentTerritory;
                        attack = adj;
                        defend = currentTerritory;
                        }
                    }
                }
            }

        }

        /**
         * Finds a continent adjacent to the one that is completely controlled by the bot
         * NOTE: May get locked
         * Ex: Australia is 100% and Asia is 100%
         * Maybe recursively call it with a 100% continent?
         * @param continent
         * @return
         */
        public Continent findAdjContinent(Continent continent) {
            Continent targetContinent = null;
            Continent tempContinent;
            for (Territory t : continent) { // Go through all territories in the continent
                for (int id : GameData.ADJACENT[t.id]) { // Go through all the adjacents
                    //find ones in a different continent
                    tempContinent = map.getContinent(GameData.CONTINENT_IDS[id]);
                    if (tempContinent.id != continent.id && tempContinent.ratio() != 1) { //When the territory is in a different continent and not 100%
                        if (targetContinent == null) {
                            targetContinent = tempContinent; //set target continent to the first adjacent if its null
                        } else if (targetContinent.ratio() > tempContinent.ratio()) {
                            targetContinent = tempContinent;
                        }
                    }
                }
            }
            if (targetContinent == null) {
                throw new IllegalStateException("No Adjacent continent was found");
            }
            return targetContinent;
        }

        @Override
        public boolean canUse(Continent continent) {
            return false;
        }
    }


    private class Fortify extends Turn {

        @Override
        public String getCommand() {
            String command = "";
            ArrayList<Territory> startingTerritories = findStartingTerritories();
            Territory startingTerritory = null;
            Territory targetTerritory = null;

            boolean checkEnd = true;
            for (int i = 0; i < GameData.NUM_CONTINENTS; ++i) {
                System.out.println(map.getContinent(i));
            }
            for (int i = 0; checkEnd && i < GameData.NUM_CONTINENTS; ++i) {
                if (map.getContinent(i).ratio() == 1)
                    targetTerritory = fullRatio(map.getContinent(i));
                else if (map.getContinent(i).ratio() >= ratio) //kinda like spread em out so that you can wall off your continent and attack
                    targetTerritory = partialRatio(map.getContinent(i));
                else
                    targetTerritory = subPartialRatio(map.getContinent(i));

                startingTerritory = connection(startingTerritories, targetTerritory); //returns the best point to start from

                if (startingTerritory != null && targetTerritory != null) {
                    checkEnd = false;
                    System.out.println(startingTerritory + " and " + targetTerritory + " were chosen to be starting and target");
                }

            }

            if (startingTerritory == null || targetTerritory == null)
                return "skip";


            return startingTerritory.territoryName().replaceAll("\\s", "") + " " + targetTerritory.territoryName().replaceAll("\\s", "") + " " + (int)(startingTerritory.numUnits * .6);
        }

        @Override
        public boolean canUse(Continent continent) {// Continent does not have a single territory owned by the bot, or all territories can't be fortified to.

            if (continent.ratio() == 0)
                return false;

//            if(continent.ratio() == 1){
//                //find all critical points, make sure that their adjacents are owned by us, if they aren't make sure that the ammount of troops on that critical point is larger than any amount on an enemy adjcacent
//
//            }

            Territory[] adjacentTerritories;
            Territory[] allTerritories = continent.getTerritories();

            for (Territory currentTerritory : allTerritories) { //loop through every territory in the continent
                if (currentTerritory.belongsTo(continent.botID) && currentTerritory.numUnits > 1) {
                    adjacentTerritories = map.getAdjacents(currentTerritory.id);
                    for (Territory adj : adjacentTerritories) { //cycle through adjacents to the current territory
                        if (adj.belongsTo(map.getBotID())) {
                            return true; // i.e there is something that can be done so there is a possibility we should fortify
                        }
                    }
                }

            }
            return false; // At this point there are no good points to fortify on this continent so we should skip it
        }

    }

    private ArrayList<Territory> findStartingTerritories() {
        ArrayList<Territory> potentialStartingPoints = new ArrayList<>();
        for (Continent c : map) {//find the largest bot-owned territory that is surrounded by friendly adjacents
            Territory[] allTerritories = c.getTerritories();
            Territory[] adjacentTerritories;
            boolean check = true;
            for (Territory currentTerritory : allTerritories) { //loop through every territory in the continent
                adjacentTerritories = map.getAdjacents(currentTerritory.id);
                for (Territory adj : adjacentTerritories) { //cycle through adjacents to the current territory
                    if (!adj.belongsTo(map.getBotID())) {
                        check = false;
                    }
                }
                if (check && currentTerritory.numUnits > 2)
                    potentialStartingPoints.add(currentTerritory);
                check = true;
            }
        }
        //sort them by largest troop count
        //potentialStartingPoints.sort();//sort the starting points by largest territory
        int numTroops = 0;
        return potentialStartingPoints;
    }

    private Territory connection(ArrayList<Territory> startingTerritories, Territory targetTerritory) {
        for (Territory sp : startingTerritories) {//loop through the list and see if you can connect to any of them
            if (board.isConnected(sp.id, targetTerritory.id))
                return sp;
        }
        return null;     // If none of them can connect, return null
    }

    private Continent findContinentFromTerritory(Territory t) {
        return map.getContinent(GameData.CONTINENT_IDS[t.id]);
    }

    private Territory fullRatio(Continent continent){
        double safe = 0.4;
        return continent.minPort(safe);

    }
    private Territory partialRatio(Continent continent){// Focus on placing troops near the country with the most troops that isn't owned by the bot   PURE FRONT LINES
        Territory[] adjacentTerritories;
        Territory[] allTerritories = continent.getTerritories();
        Territory maxThreat = new Territory(0);
        maxThreat.numUnits = 0; // maxThreat is the most threatening place to put troops on the offensive
        for (Territory currentTerritory : allTerritories) {
            if (currentTerritory.occupierID != map.getBotID()) {
                adjacentTerritories = map.getAdjacents(currentTerritory.id);
                for (Territory adj : adjacentTerritories) {
                    if (adj.belongsTo(map.getBotID()) && currentTerritory.numUnits >= maxThreat.numUnits) {
                        maxThreat = adj;
                    }
                }
            }
        }
        return maxThreat;
    }
    //        Territory maxThreat = null;
    //
    //        for (Territory port : continent.portTerritories) {
    //
    //        }
    //        for (Territory currentTerritory : continent.getTerritories()) {
    //            if (currentTerritory.occupierID != map.getBotID()) {
    //                for (Territory adj : map.getAdjacents(currentTerritory.id)) {
    //                    if (maxThreat == null) {
    //                        maxThreat = adj;
    //                    }
    //                    if (adj.belongsTo(map.getBotID()) && currentTerritory.numUnits > maxThreat.numUnits) {
    //                        maxThreat = adj;
    //                    }
    //                }
    //            }
    private Territory subPartialRatio(Continent continent){
        Territory[] adjacentTerritories;
        Territory[] allTerritories = continent.getTerritories();
        Territory mostAdvantageousTerritory = null;
        for (Territory currentTerritory : allTerritories) {// Defensive
            if (currentTerritory.occupierID == map.getBotID()) {
                adjacentTerritories = map.getAdjacents(currentTerritory.id);
                for (Territory adj : adjacentTerritories) {
                    if ((findContinentFromTerritory(adj) != findContinentFromTerritory(currentTerritory)) && currentTerritory.numUnits < 4) {//ie if you are lacking in troops and need to fortify on the defensive, return this
                        return currentTerritory;
                    }
                }
            } else {// Offensive
                adjacentTerritories = map.getAdjacents(currentTerritory.id);
                for (Territory adj : adjacentTerritories) {
                    if ((currentTerritory.id != adj.id) && adj.numUnits < currentTerritory.numUnits) {
                        mostAdvantageousTerritory = adj;
                        break;
                    }
                }
            }
        }
        return mostAdvantageousTerritory;
    }

    public static void main(String[] args) {
        Board b = new Board();
        BeachArcade bot = new BeachArcade(b, new Player(0));
    }
}
