package org.openpnp.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TapeTest {

    // TODO delete this, now covered by parameterized tests in `pickLocationFromStart`.
    @Test
    void pickLocationFromStart0402Tape() {
        final double HOLE_PITCH = 4.0;
        final double HOLE_TO_CAVITY_OFFSET_X = 2.0;
        final double TOP_TO_HOLE_OFFSET_Y = 1.75;
        final double HOLE_TO_CAVITY_OFFSET_Y = 3.5;

        // given
        Tape tape = new Tape(
            8.0,
                HOLE_PITCH,
            2.0,
                TOP_TO_HOLE_OFFSET_Y,
                HOLE_TO_CAVITY_OFFSET_X,
                HOLE_TO_CAVITY_OFFSET_Y
        );

        // and
        Location expectedLocation = new Location(
            LengthUnit.Millimeters,
            80.0 - HOLE_PITCH + HOLE_TO_CAVITY_OFFSET_X,
            40.0 - TOP_TO_HOLE_OFFSET_Y - HOLE_TO_CAVITY_OFFSET_Y,
            0,
            0
        );

        // when
        Location startTopRight = new Location(LengthUnit.Millimeters, 80.0, 40.0, 0, 0);
        Location pickLocation = tape.pickLocationFromStart(startTopRight);

        // then
        assertEquals(pickLocation, expectedLocation);
    }

    @ParameterizedTest
    @ArgumentsSource(PickLocationFromStartParameters.class)
    void pickLocationFromStart(
        final String scenario,
        final double holePitch,
        final double partPitch,
        final double topToHoleOffsetY,
        final double holeToCavityOffsetX,
        final double holeToCavityOffsetY,
        final double expectedOffsetX,
        final double expectedOffsetY
    ) {

        // given
        Tape tape = new Tape(
                8.0,
                holePitch,
                partPitch,
                topToHoleOffsetY,
                holeToCavityOffsetX,
                holeToCavityOffsetY
        );

        // and
        Location expectedOffset = new Location(
                LengthUnit.Millimeters,
                0.0 - expectedOffsetX,
                0.0 - expectedOffsetY,
                0,
                0
        );

        // when
        Location startTopRight = new Location(LengthUnit.Millimeters, 80.0, 40.0, 0, 0);
        Location pickLocation = tape.pickLocationFromStart(startTopRight);
        Location offset = pickLocation.subtract(startTopRight);

        // then
        System.out.println(String.format("%s: pick: %s, expected: %s", scenario, pickLocation, expectedOffset));
        assertEquals(offset, expectedOffset, scenario);
    }

    static class PickLocationFromStartParameters implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                //   SCENARIO, HOLE_PITCH, PART_PITCH, TOP_TO_HOLE_OFFSET_Y, HOLE_TO_CAVITY_OFFSET_X, HOLE_TO_CAVITY_OFFSET_Y, EXPECTED_OFFSET_X, EXPECTED_OFFSET_Y
                Arguments.of("0402", 4.0, 2.0, 1.75, 2.0, 3.5, 4.0 - 2.0, 1.75 + 3.5),
                Arguments.of("0603", 4.0, 4.0, 1.75, 4.0, 3.5, 4.0 - 4.0, 1.75 + 3.5),
                Arguments.of("SDCARD", 4.0, 12.0, 1.75, 6.0, 3.5, 12.0 - 6.0, 1.75 + 3.5)
            );
        }
    }

    @Test
    void holeLocations() throws Exception {
        throw new Exception("TODO");
    }

}