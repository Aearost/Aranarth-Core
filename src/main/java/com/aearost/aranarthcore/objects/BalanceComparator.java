package com.aearost.aranarthcore.objects;

import java.util.Comparator;

/**
 * A comparator to determine which of the two AranarthPlayer objects has a
 * larger balance.
 * 
 * @author Aearost
 *
 */
public class BalanceComparator implements Comparator<AranarthPlayer> {

	@Override
	public int compare(AranarthPlayer player1, AranarthPlayer player2) {
        return Double.compare(player2.getBalance(), player1.getBalance());
	}

}