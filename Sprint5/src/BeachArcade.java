// put your code here

import java.util.ArrayList;
import java.util.PriorityQueue;

public class BeachArcade implements Bot {
    // The public API of BeachArcade must not change
    // You cannot change any other classes
    // BeachArcade may not alter the state of the board or the player objects
    // It may only inspect the state of the board and the player objects
    // So you can use player.getNumUnits() but you can't use player.addUnits(10000), for example

    // > It might be helpful to store these instance variables in linked lists so we can see how they've changed, maybe with an int flag for if the change happened last go
    // > This would be easy to implement, since we call update Territory on each entry in the list with each turn, so
    private static class Territory {
        public int occupierID;
        public int numUnits;
        public int id;

        public Territory(int id) {
            occupierID = -1;
            numUnits = -1;
            this.id = id;
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

        public boolean belongsTo(int playerID) {
            return occupierID == playerID;
        }

        @Override
        public String toString() {
            return GameData.COUNTRY_NAMES[id] + " (Occupier: " + occupierID + ", number of troops: " + numUnits + ")";
        }
    }

    private static class Decision implements Comparable<Decision> {
        String command;
        int weight;

        public Decision(int weight, String command) {
            this.command = command;
            this.weight = weight;
        }

        public int getWeight() {
            return weight;
        }

        public String getCommand() {
            return command;
        }

        @Override
        public int compareTo(Decision that) {
            return this.weight - that.weight;
        }
    }

    private BoardAPI board;
    private PlayerAPI player;
    private final int opposition;
    private final Territory[] territories;
    private PriorityQueue<Decision> decisions;

    BeachArcade(BoardAPI inBoard, PlayerAPI inPlayer) {
        board = inBoard;
        player = inPlayer;
        territories = new Territory[GameData.NUM_COUNTRIES];

        // ? Do we know if the other player's ID is always the same?
        opposition = player.getId() + 1 % 2;

        for (int i = 0; i < GameData.NUM_COUNTRIES; ++i) {
            territories[i] = new Territory(i);
        }
        decisions = new PriorityQueue<Decision>();
        prepareTurn();
        // put your code here
        return;
    }

    // ! The idea is that we would call this whenever we need to make decisions that concern the whole map.
    private void prepareTurn() {
        for (int i = 0; i < GameData.NUM_COUNTRIES; ++i) {
            territories[i].updateTerritory(board.getNumUnits(i), board.getOccupier(i));
        }
        decisions.clear();
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

    /**
     * <p><strong>getReinforcement</strong> — For <em>reinforcing</em> a territory with reserves.</p>
     * <p>Gets the name of a territory (that belongs to the bot) to place a number of reinforcements (that is valid) onto.</p>
     *
     * @return String, command in the form "Territory Name" "Number of Reinforcements".
     */
    public String getReinforcement() { // * Strategise phase
        String command = "";
        // put your code here
        command = GameData.COUNTRY_NAMES[(int) (Math.random() * GameData.NUM_COUNTRIES)];
        command = command.replaceAll("\\s", "");
        command += " 1";
        return (command);
    }

    /**
     * <p><strong>getPlacement</strong> — For placing of territories in <em>initialisation stage</em>.</p>
     * <p>Chooses a territory from one of the neutrals to place reinforcements on sending it to the board reinforcement (from the neutral player's reserves) on the selected territory.</p>
     *
     * @param forPlayer The index of the player whose territories we are choosing from.
     * @return String, the name of the territory
     */
    public String getPlacement(int forPlayer) {
        prepareTurn(); // * Get snapshot of board and clear the decisions queue.

        for (Territory territory : territories) { // * Go through each of the territories on the board.
            if (territory.belongsTo(forPlayer)) { // # If the current territory belongs to 'forPlayer'.
                int weight = 0;
                int[] adjacents = GameData.ADJACENT[territory.id];
                for (int adj : adjacents) { // * Calculate a weighting for this potential placement based on the number of your enemies territories in the vicinity.
                    weight += territories[adj].belongsTo(opposition) ? 2 * territories[adj].numUnits : -5; // ? I don't know how to weight this
                }
                System.out.println("Decision: " + territory + ", Weight: " + weight);
                decisions.add(new Decision(weight, GameData.COUNTRY_NAMES[territory.id])); // * Add the calculated decision to the priority queue.
            }
        }

        return ((decisions.isEmpty()) ? (getRandomName()) : (decisions.poll().command)); // * If an error occurred, return a random name, otherwise, return the most highly weighted decision.
    }

    /**
     * <p><strong>getCardExchange</strong> — Gets cards from the bot's hand to exchange for reserves.</p>
     * <p>Returns a valid card exchange command; 3 cards to exchange (just first letter, ie. wia), or "<em>skip</em>" if less than 5 cards.</p>
     *
     * @return String, command using letters to represent the cards
     */
    public String getCardExchange() { //First Iteration Complete
        // Later it might be possible to strategise what hands are optimal, and if you have 2 different ways to turn in cards, find the best one
        int[][] validSets = new int[60][3];
        int[] set = new int[Deck.SET_SIZE];
        int r = 0;

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
                        x.append("Error you messed up");
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
        return ((decisions.isEmpty()) ? (getErrorNullExchange()) : (decisions.poll().command));
    }


    /**
     * <p><strong>getBattle</strong> — For getting an attack command.</p>
     * <p>Used in bot's <em>attack stage</em>. Returns a String containing the country to attack from, the country to attack and the number of units to use, or "skip" to skip attack phase.</p>
     *
     * @return String representing attack command ("from", "to", "units")
     */
    public String getBattle() {
        String command = "";
        // put your code here
        command = "skip";
        return (command);
    }

    /**
     * <p><strong>getDefence</strong> — For getting a defend command.</p>
     * <p>Used when other player is in the <em>attack stage</em>. Returns the number of units to defend a territory with.</p>
     *
     * @param countryId int, ID of territory to defend.
     * @return String, number of units
     */
    public String getDefence(int countryId) {// Always defend with the most units you can to give the best chance of winning
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
        String command = "";
        // put your code here
        command = "0";
        return (command);
    }

    /**
     * <p><strong>getFortify</strong> — For selecting territories and a number of troops to exchange.</p>
     * <p>Return country name to move units from, country name to fortify and number of units to move, or "skip" to skip this stage.</p>
     *
     * @return String representing fortify command ("from", "to", "units")
     */
    public String getFortify() {//make method that would return an array of territories in a continent
//        //iterable (for each) continent
//        //make an int array of int arrays, the first array is europe, the second array is whatever, you can do "for each int array in this thing
//        String command = "";
//        // put code here
//
//        command = "skip";
//        return (command);

    }

    /* Utility Methods */
    public static String getRandomName() {
        return GameData.COUNTRY_NAMES[(int) (Math.random() * GameData.NUM_COUNTRIES)].replaceAll("\\s", "");
    }

    public static String getErrorNullExchange() {
        return "An Error has occurred in exchanging cards";
    }
}
