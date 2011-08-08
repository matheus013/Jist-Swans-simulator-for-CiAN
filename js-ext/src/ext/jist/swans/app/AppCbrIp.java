/*
 * Ulm University JiST/SWANS Extension Project
 * 
 * Author:		Michael Feiri <michael.feiri@uni-ulm.de>
 * 
 */
package ext.jist.swans.app;

import jist.swans.app.AppInterface;

public class AppCbrIp extends AppCbrBase implements AppInterface {


	public AppCbrIp(int sendRate, int waitTimeBetween, int waitTimeStart,
			int waitTimeEnd, int packetsPerConnection, int nodeId,
			int nodeCount, int duration) {
		
		super(sendRate, waitTimeBetween, waitTimeStart, waitTimeEnd,
				packetsPerConnection, nodeId, nodeCount, duration);
	}
	
}
