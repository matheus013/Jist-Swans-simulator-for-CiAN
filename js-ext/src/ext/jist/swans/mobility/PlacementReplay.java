package ext.jist.swans.mobility;

import jist.swans.field.Placement;
import jist.swans.misc.Location;

/**
 * PlacementReplay tries to provide a Placement model for replayed mobility.
 * CAUTION: This is not a perfect solution, as the original Placement interface
 * just allows to get a arbitrary position on the field. Nodes do not play a
 * role there, whereas for replayed mobility, every node has exactly one
 * specific, initial position. Thus, we assume here that we can increase the
 * node number with each subsequent call of "getNextLocation". As a consequence,
 * nodes need to be instantiated in the order 0, 1, ..., n and the Placement
 * object MUST not be used for getting "legal" positions on the field anywhere
 * else during the simulation.
 * 
 * @author eschoch
 * 
 */
public class PlacementReplay implements Placement
{

    private MobilityReplay mobility;
    private int            currentNode = 0;

    public PlacementReplay(MobilityReplay mobility) {
        this.mobility = mobility;
    }

    public Location getNextLocation() {
        return mobility.getInitialPosition(currentNode++);
    }

}
