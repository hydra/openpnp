package org.openpnp.machine.reference.feeder;

import com.google.common.io.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.camera.ImageCamera;
import org.openpnp.model.*;
import org.openpnp.spi.*;
import org.openpnp.util.FeederVisionHelper;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    public static final Tape TEST_0402_TAPE = new Tape(8.0, 4.0, 2.0, 1.75, 2.0, 3.5);

    private ReferencePushPullFeeder feeder;
    private Nozzle mockedNozzle;
    private Machine machine;
    private Actuator feedActuator;
    private Actuator rotationActuator;
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
            camera.setDefaultZ(new Length(0.0, LengthUnit.Millimeters));
            head.addCamera(camera);
        }
    }

    private void useCameraImage(CameraImage cameraImage) {
        // FIXME using a 'Location' instance set set Image units per pixel, from an API perspective, makes no sense.  Z and rotation are never used. Suggest using a new type without Z and rotation.
        Location imageUnitsPerPixel = new Location(LengthUnit.Millimeters, cameraImage.mm_per_pixel, cameraImage.mm_per_pixel, 0, 0);
        camera.setImageUnitsPerPixel(imageUnitsPerPixel);

        // FIXME what's the difference between UnitsPerPixel and ImageUnitsPerPixel ?
        camera.setUnitsPerPixel(imageUnitsPerPixel);
        camera.setSourceUri(cameraImage.uri);
    }

    private void buildActuators() throws Exception {

        // initially was using mocked actuators, but it's not possible to save the configuration, you just get a stack overflow or some reflection/opens errors
        // the requirement for being able to save the XML only exists because the vision pipeline wasn't working and need to compare the pipeline XML to find out what's going on
        if (false) {
            feedActuator = mock(Actuator.class);
            when(feedActuator.getName()).thenReturn(TEST_FEED_ACTUATOR_NAME);
            machine.addActuator(feedActuator);

            rotationActuator = mock(Actuator.class);
            when(rotationActuator.getName()).thenReturn(TEST_ROTATION_ACTUATOR_NAME);
            machine.addActuator(rotationActuator);
        } else {
            // so instead, we use a TestActuator instance, which can be saved to XML.
            feedActuator = new TestActuator();
            feedActuator.setName(TEST_FEED_ACTUATOR_NAME);

            rotationActuator = new TestActuator();
            rotationActuator.setName(TEST_ROTATION_ACTUATOR_NAME);
        }

        head.addActuator(feedActuator);
    }

    private void buildFeeder() throws Exception {
        feeder = new ReferencePushPullFeeder();

        feeder.setActuator(feedActuator);
        feeder.setActuator2(rotationActuator);

        List<Part> parts = Configuration.get().getParts();
        feeder.setPart(parts.get(0));

        machine.addFeeder(feeder);
    }

    private void buildNozzle() throws Exception {
        mockedNozzle = mock(Nozzle.class);
        lenient().when(mockedNozzle.getName()).thenReturn("Test Nozzle");
        lenient().when(mockedNozzle.getHead()).thenReturn(head);

        head.addNozzle(mockedNozzle);
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

    private void buildMachine() {
        machine = Configuration.get().getMachine();
    }

    private void prepareMachine() throws Exception {
        machine.setEnabled(true);
        machine.home();
    }

    // FIXME sprocket holes really only required if vision is used/enabled.
    @Test
    public void feedRequiresCalibratedHolesLocations() {
        // when
        Exception thrown = assertThrows(Exception.class, () -> feeder.feed(mockedNozzle));

        // then
        assertEquals(thrown.getMessage(), "Sprocket hole locations undefined/too close together. feeder: 'ReferencePushPullFeeder', distance: 0.000mm");
    }

    /**
     * * uses default CalibrationTrigger value
     * * doesn't actually feed anything, since there's no push-pull-motion defined
     */
    @Test void feed() throws Exception {
        // given
        Location pickLocation = calculatePickLocationFromTape(TEST_0402_TAPE, LOCATON_0402_STRIP_TOP_RIGHT, TEST_PICK_HEAD_OFFSET_Z);

        // and
        feeder.setLocation(pickLocation);
        configureHoleLocations(pickLocation);

        // and
        useCameraImage(CameraImage.BEFORE_DRAG);

        // FIXME why do we need to move the camera here?  Without this, the call to `performVisionOperations` from `obtainCalibratedVisionOffset` will be
        //       using the current head position (0,0) which is obviously wrong for vision calibration.
        machine.getDefaultHead().getDefaultCamera().moveTo(pickLocation);

        // when
        Exception e = feeder.autoSetupPipeline(camera, FeederVisionHelper.PipelineType.CircularSymmetry);

        // then
        assertNull(e);

        // when
        feeder.feed(mockedNozzle);

        // then
        // no exception thrown
    }

    @Test void autoSetup() throws Exception {
        // given
        Location pickLocation = calculatePickLocationFromTape(TEST_0402_TAPE, LOCATON_0402_STRIP_TOP_RIGHT, TEST_PICK_HEAD_OFFSET_Z);
        feeder.setLocation(pickLocation);

        // and
        useCameraImage(CameraImage.BEFORE_DRAG);

        // and
        // Configuration.get().save(); // currently disabled as using a mock for the nozzle causing saving to fail.

        // FIXME why do we need to move the camera here?  Without this, the call to `performVisionOperations` from `obtainCalibratedVisionOffset` will be
        //       using the current head position (0,0) which is obviously wrong for vision calibration.
        machine.getDefaultHead().getDefaultCamera().moveTo(pickLocation);

        // when
        feeder.autoSetup();

        // then
        // no exception thrown
    }

    private Location calculatePickLocationFromTape(Tape tape, Location topRight, double pick_z_offset) {
        Location pickLocation = tape.pickLocationFromStart(topRight);

        // need an offset to the left, equal to one component pitch, because the start tape hole is cut down the middle and cannot be recognised as a hole
        // and because the first component cavity may be cut in two.
        // also with a non-zero negative Z offset.
        pickLocation = pickLocation.derive( pickLocation.getX() - tape.partPitch, null, pick_z_offset, null);
        return pickLocation;
    }

    private void configureHoleLocations(Location pickLocation) {
        // feed/pull direction is to the right
        feeder.setHole1Location(TEST_0402_TAPE.getHole1Location(pickLocation));
        feeder.setHole2Location(TEST_0402_TAPE.getHole2Location(pickLocation));
    }

    public static class TestActuator extends ReferenceActuator {
        String readValue = "0.0";

        public void setReadValue(String readValue) {
            this.readValue = readValue;
        }

        @Override
        public void actuate(boolean on) throws Exception {
        }

        @Override
        public String read() throws Exception {
            return readValue;
        }
    }
}