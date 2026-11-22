package com.csm.mushafi.quran;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KhatmahPlannerTest {

    @Test
    public void dailyAmountRoundsUp() {
        assertEquals(891, KhatmahPlanner.dailyAyahs(6236, 7));
        assertEquals(208, KhatmahPlanner.dailyAyahs(6236, 30));
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroDaysIsRejected() {
        KhatmahPlanner.dailyAyahs(6236, 0);
    }

    @Test
    public void progressIsClampedAndRounded() {
        assertEquals(0, KhatmahPlanner.progressPercent(-5, 6236));
        assertEquals(50, KhatmahPlanner.progressPercent(3118, 6236));
        assertEquals(100, KhatmahPlanner.progressPercent(9000, 6236));
        assertEquals(0, KhatmahPlanner.progressPercent(10, 0));
    }

    @Test
    public void globalIndexCountsPreviousSurahs() {
        int[] counts = {7, 286, 200};
        assertEquals(1, KhatmahPlanner.globalIndex(counts, 1, 1));
        assertEquals(8, KhatmahPlanner.globalIndex(counts, 2, 1));
        assertEquals(493, KhatmahPlanner.globalIndex(counts, 3, 200));
    }
}
