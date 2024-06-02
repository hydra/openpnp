package org.openpnp.model;

/**
 * Defines a tape.
 *
 * This definition is concerned with the specification of tapes based on the EIA-481 standard, it doesn't care what is actually IN the tape.
 *
 * Implementation currently assumes top of tape is 0 (edge with round sprocket holes), and offsets are as per EIA-481 diagram reference images and
 * positives value indicates a downwards/right direction, negative is upwards/left.  This different to machine/cartesian coordinate systems where positive values are upwards/right.
 *
 * @see <a href="https://www.vishay.com/docs/20014/smdpack.pdf">smdpack.pdf</a>
 * @see <a href="https://community.nxp.com/pwmxy87654/attachments/pwmxy87654/kinetis/37830/1/EIA-481-2003.pdf">EIA-481-2003.pdf</a>
 */
public class Tape {
    // usually 8mm, 12mm, 16mm, 24mm, etc.
    public double width;

    // usually 4mm
    public double holePitch;

    // imperial 1206/0805/0603's = 4mm, 0402/0201 = 2mm, etc.
    public double partPitch;

    // from the top
    public double holeCenterOffsetY;

    // from the hole center
    public double cavityCenterOffsetX;
    public double cavityCenterOffsetY;

    public Tape(double width, double holePitch, double partPitch, double holeCenterOffsetY, double cavityCenterOffsetX, double cavityCenterOffsetY) {
        this.width = width;
        this.holePitch = holePitch;
        this.partPitch = partPitch;
        this.holeCenterOffsetY = holeCenterOffsetY;
        this.cavityCenterOffsetX = cavityCenterOffsetX;
        this.cavityCenterOffsetY = cavityCenterOffsetY;
    }

    /**
     * EIA-481 doesn't clearly define the start of the tape.  In practice tapes are cut between components and the cut
     * is usually through the center of a sprocket hole.
     *
     * EIA-481 Figure 12 defines the start of a tape, but the example doesn't match real-world tapes the author had to hand.
     *
     * For 0402/0201 with 2mm pitch spacing, the start is defined as a cut though a cavity AND a sprocket hole.
     *
     * Various tapes on-hand were inspected by the author. Including ESP32S3, ICM42688P, SX1280, SD card sockets,
     * USB sockets, JST sockets, memory, diodes, regulators, u.FL connectors + passives.
     */
    public Location pickLocationFromStart(Location startTopRight) {

        double offsetX;
        if (partPitch < holePitch) {
            offsetX = holePitch;
        } else {
            offsetX = partPitch;
        }

        Location pickLocation = new Location(
                startTopRight.getUnits(),
                startTopRight.getX() - offsetX + cavityCenterOffsetX,
                startTopRight.getY() - holeCenterOffsetY - cavityCenterOffsetY,
                0.0,
                0.0
        );

        return pickLocation;
    }
}
