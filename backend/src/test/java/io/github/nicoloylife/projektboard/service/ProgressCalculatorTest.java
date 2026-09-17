package io.github.nicoloylife.projektboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProgressCalculatorTest {

    @Test
    void projectWithoutTasksHasZeroProgress() {
        assertThat(ProgressCalculator.percent(0, 0)).isZero();
    }

    @Test
    void twoOfFiveTasksDoneIsFortyPercent() {
        assertThat(ProgressCalculator.percent(2, 5)).isEqualTo(40);
    }

    @Test
    void oneOfThreeTasksDoneIsRoundedToThirtyThree() {
        assertThat(ProgressCalculator.percent(1, 3)).isEqualTo(33);
    }

    @Test
    void allTasksDoneIsHundredPercent() {
        assertThat(ProgressCalculator.percent(5, 5)).isEqualTo(100);
    }

    @Test
    void countsAreSummedPerProject() {
        TaskCounts counts = new TaskCounts(5, 2, 1, 2);
        assertThat(counts.progressPercent()).isEqualTo(40);
        assertThat(TaskCounts.empty().progressPercent()).isZero();
    }
}
