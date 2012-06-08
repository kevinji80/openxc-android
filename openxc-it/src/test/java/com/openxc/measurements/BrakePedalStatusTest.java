package com.openxc.measurements;

import junit.framework.TestCase;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;


public class BrakePedalStatusTest extends TestCase {
    BrakePedalStatus measurement;

    @Override
    public void setUp() {
        measurement = new BrakePedalStatus(new Boolean(false));
    }

    public void testGet() {
        assertThat(measurement.getValue().booleanValue(), equalTo(false));
    }

    public void testHasNoRange() {
        assertFalse(measurement.hasRange());
    }
}
