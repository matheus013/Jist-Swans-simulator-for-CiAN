package jist.swans.field;

import jist.swans.misc.Location;

public interface MovementListenerInterface
{

    public void move(long time, Location loc, int node);
}
