package org.openpnp.machine.reference.feeder;

import com.google.common.io.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.camera.ImageCamera;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReferencePushPullFeederTest {

    public static final String TEST_FEED_ACTUATOR_NAME = "FEED_ACTUATOR";
    public static final String TEST_ROTATION_ACTUATOR_NAME = "ROTATION_ACTUATOR"; // aka peeler
    public static final Location LOCATON_HOLE_1 = new Location(LengthUnit.Millimeters, 100.0, 10.0, 0.0, 0.0);
    public static final Location LOCATON_HOLE_2_4MM_OFFSET = new Location(LengthUnit.Millimeters, 104.0, 10.0, 0.0, 0.0);
    private ReferencePushPullFeeder feeder;
    private Nozzle mockedNozzle;
    private Machine machine;
    private Actuator mockedFeedActuator;
    private Actuator mockedRotationActuator;
    private ReferenceHead head;

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

    private void buildHead() {
        head = new ReferenceHead();
        head.setName("H1");
    }

    private void buildDownCamera() throws Exception {
        ImageCamera camera = new ImageCamera();
        camera.setName("DOWN");
        camera.setLooking(Camera.Looking.Down);
        head.addCamera(camera);
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
        Exception thrown = assertThrows(Exception.class, () -> feeder.feed(mockedNozzle));
        assertEquals(thrown.getMessage(), "Sprocket hole locations undefined/too close together. feeder: 'ReferencePushPullFeeder', distance: 0.000mm");
    }

    /**
     * * uses default CalibrationTrigger value
     * @throws Exception
     */
    @Test void feed() throws Exception {
        configureHoleLocations();
        feeder.feed(mockedNozzle);
    }

    private void configureHoleLocations() {
        feeder.setHole1Location(LOCATON_HOLE_1);
        feeder.setHole2Location(LOCATON_HOLE_2_4MM_OFFSET);
    }
}