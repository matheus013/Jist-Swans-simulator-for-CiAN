package vans.apps;

import jist.swans.misc.Location;
import ext.jist.swans.mobility.MobilityReaderNs2;

public class mobility
{

    public static void main(String a[]) {
        MobilityReaderNs2 read_file = new MobilityReaderNs2();
        try {
            System.out.println("Hello");
            read_file.readFile("mobility/simplest.txt");

        } catch (Exception e) {
            System.err.println("Exception: " + e);
        }

        System.out.println("Hello world");
        Location.Location2D[] corners = read_file.getCorners();
        int nodeNumber = read_file.getNodeNumber();
        System.out.println("corners " + corners[0]);
        System.out.println("node numbers " + nodeNumber);

    }

}
