package org.openpnp.machine.reference.feeder;

import com.google.common.io.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.reference.camera.ImageCamera;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Tape;
import org.openpnp.spi.*;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReferencePushPullFeederTest {

    public static final double TEST_PICK_HEAD_OFFSET_Z = -5.0;

    private enum CameraImage {
        BEFORE_DRAG("classpath://samples/pushpull-test/drag-before.png", Constants.MM_PER_PIXEL_1),
        AFTER_DRAG_2MM("classpath://samples/pushpull-test/drag-after.png", Constants.MM_PER_PIXEL_1);

        private final String uri;
        private final double mm_per_pixel;

        CameraImage(String uri, double mm_per_pixel) {
            this.uri = uri;
            this.mm_per_pixel = mm_per_pixel;
        }

        private static class Constants {
            public static final double MM_PER_PIXEL_1 = 0.1;
        }
    }
    public static final String TEST_FEED_ACTUATOR_NAME = "FEED_ACTUATOR";
    public static final String TEST_ROTATION_ACTUATOR_NAME = "ROTATION_ACTUATOR"; // aka peeler

    // feed direction is to the right (positive X)
    public static final Location LOCATON_0402_STRIP_TOP_RIGHT = new Location(LengthUnit.Millimeters, 80.0, 30.0, 0.0, 0.0);

    public static final Tape TEST_0402_TAPE = new Tape(8.0, 4.0, 2.0, 1.75, 3.5, 2.0);

    private ReferencePushPullFeeder feeder;
    private Nozzle mockedNozzle;
    private Machine machine;
    private Actuator mockedFeedActuator;
    private Actuator mockedRotationActuator;
    private Head head;
    private ImageCamera camera;

    @BeforeEach
    public void setUp() throws Exception {
        buildDefaultConfiguration();
        buildMachine();
        buildHead();
        buildDownCamera();
        buildNozzle();
        buildActuators();
        buildFeeder();

        prepareMachine();
    }

    private void buildHead() throws Exception {
        head = machine.getDefaultHead();
        head.setName("H1");
    }

    private void buildDownCamera() throws Exception {
        // FIXME removing the existing camera and building out own one doesn't work, the camera doesn't move, for now rely on the default camera being an ImageCamera.
        if (true) {
            camera = (ImageCamera) head.getDefaultCamera();
        } else {
            head.removeCamera(head.getDefaultCamera());

            camera = new ImageCamera();
            camera.setName("DOWN");
            camera.setLooking(Camera.Looking.Down);
            head.addCamera(camera);
        }
    }

    private void useCameraImage(CameraImage cameraImage) {
        // FIXME using a 'Location' instance set set Image units per pixel, from an API perspective, makes no sense.  Z and rotation are never used. Suggest using a new type without Z and rotation.
        Location imageUnitsPerPixel = new Location(LengthUnit.Millimeters, cameraImage.mm_per_pixel, cameraImage.mm_per_pixel, 0, 0);
        camera.setImageUnitsPerPixel(imageUnitsPerPixel);
        camera.setSourceUri(cameraImage.uri);
    }

    private void buildActuators() throws Exception {
        mockedFeedActuator = mock(Actuator.class);
        when(mockedFeedActuator.getName()).thenReturn(TEST_FEED_ACTUATOR_NAME);
        machine.addActuator(mockedFeedActuator);

        mockedRotationActuator = mock(Actuator.class);
        when(mockedRotationActuator.getName()).thenReturn(TEST_ROTATION_ACTUATOR_NAME);
        machine.addActuator(mockedRotationActuator);
    }

    private void buildFeeder() throws Exception {
        feeder = new ReferencePushPullFeeder();

        feeder.setActuator(mockedFeedActuator);
        feeder.setActuator2(mockedRotationActuator);

        machine.addFeeder(feeder);
    }

    private void buildNozzle() {
        mockedNozzle = mock(Nozzle.class);
        when(mockedNozzle.getName()).thenReturn("Test Nozzle");
    }

    private void buildDefaultConfiguration() throws Exception {
        Configurator
                .currentConfig()
                //.level(org.openpnp.vision.pipeline.stages.ImageWriteDebug.class, Level.DEBUG)
                .level(Level.TRACE) // change this for other log levels.
                .activate();

        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);
        Configuration.initialize(workingDirectory);
        Configuration.get().load();
    }

    private void buildMachine() throws Exception {
        machine = Configuration.get().getMachine();
        //machine.setHomed(true);
    }

    private void prepareMachine() throws Exception {
        machine.setEnabled(true);
        machine.home();
    }

    // FIXME sprocket holes really only required if vision is used/enabled.
    @Test
    public void feedRequiresCalibratedHolesLocations() throws Exception {
        // when
        Exception thrown = assertThrows(Exception.class, () -> feeder.feed(mockedNozzle));

        // then
        assertEquals(thrown.getMessage(), "Sprocket hole locations undefined/too close together. feeder: 'ReferencePushPullFeeder', distance: 0.000mm");
    }

    /**
     * * uses default CalibrationTrigger value
     * @throws Exception
     */
    @Test void feed() throws Exception {
        // given
        Location pickLocation = TEST_0402_TAPE.pickLocationFromStart(LOCATON_0402_STRIP_TOP_RIGHT);

        // and an offset to the left, equal to one tape hole, because the start tape hole is cut down the middle and cannot be recognised as a hole
        pickLocation = pickLocation.derive( pickLocation.getX() - TEST_0402_TAPE.holePitch, null, null, null);

        pickLocation = pickLocation.derive(null, null, TEST_PICK_HEAD_OFFSET_Z, null);
        feeder.setLocation(pickLocation);

        configureHoleLocations(pickLocation);

        // and
        useCameraImage(CameraImage.BEFORE_DRAG);

        // FIXME why do we need to move the camera here?  Without this, the call to `performVisionOperations` from `obtainCalibratedVisionOffset` will be
        //       using the current head position (0,0) which is obviously wrong for vision calibration.
        machine.getDefaultHead().getDefaultCamera().moveTo(pickLocation);

        // when
        feeder.feed(mockedNozzle);

        // then
        // TODO
    }

    private void configureHoleLocations(Location pickLocation) {
        // feed/pull direction is to the right
        feeder.setHole1Location(TEST_0402_TAPE.getHole1Location(pickLocation));
        feeder.setHole2Location(TEST_0402_TAPE.getHole2Location(pickLocation));
    }
}