
public interface BoardAPI {
	/**
	 * <strong>isAdjacent</strong> — Find if two territories are adjacent to each other.
	 * @param fromCountry Index of the source territory.
	 * @param toCountry Index of the target territory.
	 * @return Boolean stating whether the territories connect.
	 */
	public boolean isAdjacent (int fromCountry, int toCountry);	
	public boolean isConnected (int fromCountry, int toCountry);	
	public boolean isOccupied(int country); 	
	public boolean isInvasionSuccess ();	
	public boolean isEliminated (int playerId); 
	public int getOccupier (int countryId);
	public int getNumUnits (int countryId);

}
