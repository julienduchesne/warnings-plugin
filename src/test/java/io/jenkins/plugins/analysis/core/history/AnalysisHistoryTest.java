package io.jenkins.plugins.analysis.core.history;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.plugins.analysis.core.history.AnalysisHistory.JobResultEvaluationMode;
import static io.jenkins.plugins.analysis.core.history.AnalysisHistory.JobResultEvaluationMode.*;
import io.jenkins.plugins.analysis.core.history.AnalysisHistory.QualityGateEvaluationMode;
import static io.jenkins.plugins.analysis.core.history.AnalysisHistory.QualityGateEvaluationMode.*;
import static io.jenkins.plugins.analysis.core.history.AnalysisHistoryTest.ExpectedResult.*;
import io.jenkins.plugins.analysis.core.model.AnalysisResult;
import io.jenkins.plugins.analysis.core.quality.QualityGateStatus;
import io.jenkins.plugins.analysis.core.views.ResultAction;
import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import hudson.model.Result;
import hudson.model.Run;

/**
 * Tests the class {@link AnalysisHistory}.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings({"ParameterNumber", "PMD.UnusedPrivateMethod", "unchecked"})
@SuppressFBWarnings("UPM")
class AnalysisHistoryTest {
    /**
     * Creates a sequence of three failing builds. Verifies that the history contains all of these builds. 
     *
     * @see <a href="http://issues.jenkins-ci.org/browse/JENKINS-41598">Issue 41598</a>
     */
    @Test
    void issue41598() {
        Run last = createFailingBuild();

        ResultAction lastAction = mock(ResultAction.class);
        AnalysisResult lastResult = mock(AnalysisResult.class);
        when(lastAction.getResult()).thenReturn(lastResult);
        when(lastAction.getOwner()).thenReturn(last);
        
        Run middle = createFailingBuild();
        ResultAction middleAction = mock(ResultAction.class);
        AnalysisResult middleResult = mock(AnalysisResult.class);
        when(middleAction.getResult()).thenReturn(middleResult);
        when(middleAction.getOwner()).thenReturn(middle);

        Run first = createFailingBuild();
        ResultAction firstAction = mock(ResultAction.class);
        AnalysisResult firstResult = mock(AnalysisResult.class);
        when(firstAction.getResult()).thenReturn(firstResult);
        when(firstAction.getOwner()).thenReturn(first);
        
        when(last.getPreviousBuild()).thenReturn(middle);
        when(middle.getPreviousBuild()).thenReturn(first);
        
        ResultSelector resultSelector = mock(ResultSelector.class);
        when(resultSelector.get(last)).thenReturn(Optional.of(lastAction));
        when(resultSelector.get(middle)).thenReturn(Optional.of(middleAction));
        when(resultSelector.get(first)).thenReturn(Optional.of(firstAction));
        
        AnalysisHistory history = new AnalysisHistory(last, resultSelector);
        
        assertThat(history.iterator()).containsExactly(lastResult, middleResult, firstResult);
    }

    private Run createFailingBuild() {
        return createBuildWithResult(Result.FAILURE);
    }

    @Test
    void baselineShouldHaveNoPreviousResult() {
        Run baseline = mock(Run.class);
        ResultSelector resultSelector = mock(ResultSelector.class);
        ResultAction baselineAction = mock(ResultAction.class);
        AnalysisResult baselineResult = mock(AnalysisResult.class);
        when(baselineAction.getResult()).thenReturn(baselineResult);
        when(baselineAction.getOwner()).thenReturn(baseline);
        when(resultSelector.get(baseline)).thenReturn(Optional.of(baselineAction));

        AnalysisHistory history = new AnalysisHistory(baseline, resultSelector);

        assertThat(history.getBaselineResult()).contains(baselineResult);
        assertThat(history.getResult()).contains(baselineResult);
        assertThat(history.getBuild()).contains(baseline);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideSingleResultIfQualityGateAndBuildResultIsIgnored")
    @DisplayName("Ignore Job Result + Quality Gate -> should evaluate history of a sequence with one build")
    void shouldTestFirstIterationOfLoopIgnoreStatusAndResult(final String name,
            final QualityGateEvaluationMode qualityGateEvaluationMode,
            final JobResultEvaluationMode jobResultEvaluationMode,
            final boolean hasResult, final QualityGateStatus qualityGateStatus, final Result jobStatus,
            final ExpectedResult expectedBaseline, final ExpectedResult expectedPrevious) {
        runTest(qualityGateEvaluationMode, jobResultEvaluationMode,
                hasResult, qualityGateStatus, jobStatus,
                expectedBaseline, expectedPrevious);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideSingleResultIfQualityGateIsIgnored")
    @DisplayName("Ignore Quality Gate + Successful Job Result -> should evaluate history of a sequence with one build")
    void shouldTestFirstIterationOfLoopIgnoreStatus(final String name,
            final QualityGateEvaluationMode qualityGateEvaluationMode,
            final JobResultEvaluationMode jobResultEvaluationMode,
            final boolean hasResult, final QualityGateStatus qualityGateStatus, final Result jobStatus,
            final ExpectedResult expectedBaseline, final ExpectedResult expectedPrevious) {
        runTest(qualityGateEvaluationMode, jobResultEvaluationMode,
                hasResult, qualityGateStatus, jobStatus,
                expectedBaseline, expectedPrevious);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideSingleResultIfBuildResultIsIgnored")
    @DisplayName("Ignore Job Result + Successful Quality Gate -> should evaluate history of a sequence with one build")
    void shouldTestFirstIterationOfLoopIgnoreResult(final String name,
            final QualityGateEvaluationMode qualityGateEvaluationMode,
            final JobResultEvaluationMode jobResultEvaluationMode,
            final boolean hasResult, final QualityGateStatus qualityGateStatus, final Result jobStatus,
            final ExpectedResult expectedBaseline, final ExpectedResult expectedPrevious) {
        runTest(qualityGateEvaluationMode, jobResultEvaluationMode,
                hasResult, qualityGateStatus, jobStatus,
                expectedBaseline, expectedPrevious);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideSingleResult")
    @DisplayName("Successful Job Result + Successful Quality Gate -> should evaluate history of a sequence with one build")
    void shouldTestFirstIterationOfLoop(final String name,
            final QualityGateEvaluationMode qualityGateEvaluationMode,
            final JobResultEvaluationMode jobResultEvaluationMode,
            final boolean hasResult, final QualityGateStatus qualityGateStatus, final Result jobStatus,
            final ExpectedResult expectedBaseline, final ExpectedResult expectedPrevious) {
        runTest(qualityGateEvaluationMode, jobResultEvaluationMode,
                hasResult, qualityGateStatus, jobStatus,
                expectedBaseline, expectedPrevious);
    }

    private void runTest(final QualityGateEvaluationMode qualityGateEvaluationMode,
            final JobResultEvaluationMode jobResultEvaluationMode, final boolean hasResult,
            final QualityGateStatus qualityGateStatus, final Result jobStatus, final ExpectedResult expectedBaseline,
            final ExpectedResult expectedPrevious) {
        ResultSelector resultSelector = mock(ResultSelector.class);
        Run baseline = createBuild(hasResult, qualityGateStatus, jobStatus, resultSelector, FIRST);

        AnalysisHistory history = new AnalysisHistory(baseline, resultSelector, qualityGateEvaluationMode,
                jobResultEvaluationMode);

        assertResult(history.getBaselineResult(), expectedBaseline);
        assertResult(history.getResult(), expectedPrevious);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void assertResult(final Optional<AnalysisResult> optionalResult, final ExpectedResult expectedBaseline) {
        if (expectedBaseline == NONE) {
            assertThat(optionalResult).isEmpty();
        }
        else {
            assertThat(optionalResult).hasValueSatisfying(result ->
                    assertThat(result.getId()).isEqualTo(expectedBaseline.name()));
        }
    }

    private Run createBuild(final boolean hasResult, final QualityGateStatus qualityGateStatus, final Result jobStatus,
            final ResultSelector resultSelector, final ExpectedResult resultId) {
        Run baseline = createBuildWithResult(jobStatus);

        if (hasResult) {
            AnalysisResult result = mock(AnalysisResult.class);
            when(result.getId()).thenReturn(resultId.name());
            when(result.getQualityGateStatus()).thenReturn(qualityGateStatus);
            ResultAction resultAction = mock(ResultAction.class);
            when(resultAction.getResult()).thenReturn(result);
            when(resultAction.isSuccessful()).thenReturn(qualityGateStatus.isSuccessful());
            when(resultSelector.get(baseline)).thenReturn(Optional.of(resultAction));

        }
        else {
            when(resultSelector.get(baseline)).thenReturn(Optional.empty());
        }
        return baseline;
    }

    private Run createBuildWithResult(final Result jobStatus) {
        Run baseline = mock(Run.class);
        when(baseline.getResult()).thenReturn(jobStatus);
        return baseline;
    }

    /**
     * Method to provide test element that return an present optional.
     *
     * @return list of test data objects
     */
    private static Iterable<Object> provideSingleResult() {
        return asList(
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResultEvaluationMode(NO_JOB_FAILURE)
                        .setJobResult(Result.SUCCESS)
                        .setQualityGateStatus(QualityGateStatus.INACTIVE)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result (SUCCESS, quality gate is not active)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResultEvaluationMode(NO_JOB_FAILURE)
                        .setJobResult(Result.SUCCESS)
                        .setQualityGateStatus(QualityGateStatus.PASSED)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result (SUCCESS, quality gate has been passed)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResultEvaluationMode(NO_JOB_FAILURE)
                        .setJobResult(Result.SUCCESS)
                        .setQualityGateStatus(QualityGateStatus.FAILED)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no analysis result if quality gate has been missed (SUCCESS)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResultEvaluationMode(NO_JOB_FAILURE)
                        .setJobResult(Result.SUCCESS)
                        .setQualityGateStatus(QualityGateStatus.WARNING)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no analysis result if quality gate has a warning (SUCCESS)")
                        .build(),

                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResultEvaluationMode(NO_JOB_FAILURE)
                        .setJobResult(Result.UNSTABLE)
                        .setQualityGateStatus(QualityGateStatus.INACTIVE)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no analysis result even if quality gate is not active (UNSTABLE)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResultEvaluationMode(NO_JOB_FAILURE)
                        .setJobResult(Result.UNSTABLE)
                        .setQualityGateStatus(QualityGateStatus.PASSED)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no analysis result even if quality gate has been passed (UNSTABLE)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResultEvaluationMode(NO_JOB_FAILURE)
                        .setJobResult(Result.UNSTABLE)
                        .setQualityGateStatus(QualityGateStatus.FAILED)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no analysis result if quality gate has been missed (UNSTABLE)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResultEvaluationMode(NO_JOB_FAILURE)
                        .setJobResult(Result.UNSTABLE)
                        .setQualityGateStatus(QualityGateStatus.WARNING)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no analysis result if quality gate has a warning (UNSTABLE)")
                        .build()

        );
    }
    /**
     * Method to provide test element that return an present optional.
     *
     * @return list of test data objects
     */
    private static Iterable<Object> provideSingleResultIfQualityGateAndBuildResultIsIgnored() {
        return asList(
                new BuildHistoryBuilder().setHasResult(true)
                        .setJobResult(Result.SUCCESS)
                        .setQualityGateStatus(QualityGateStatus.INACTIVE)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setJobResult(Result.SUCCESS)
                        .setQualityGateStatus(QualityGateStatus.FAILED)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result even if quality gate has been missed")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setJobResult(Result.FAILURE)
                        .setQualityGateStatus(QualityGateStatus.FAILED)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result even if job Result is FAILURE (and cause is the analysis)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setJobResult(Result.FAILURE)
                        .setQualityGateStatus(QualityGateStatus.INACTIVE)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result if job Result is FAILURE (and quality gate is inactive)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setJobResult(Result.FAILURE)
                        .setQualityGateStatus(QualityGateStatus.WARNING)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result if job Result is FAILURE (and quality gate is warning)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(false)
                        .setJobResult(Result.SUCCESS)
                        .setQualityGateStatus(QualityGateStatus.INACTIVE)
                        .setExpectedBaselineResult(NONE)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no result")
                        .build()
        );
    }

    /**
     * Method to provide test element that return an present optional.
     *
     * @return list of test data objects
     */
    private static Iterable<Object> provideSingleResultIfBuildResultIsIgnored() {
        return asList(
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResult(Result.SUCCESS)
                        .setQualityGateStatus(QualityGateStatus.INACTIVE)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result if quality gate is not active (Result = SUCCESS)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResult(Result.SUCCESS)
                        .setQualityGateStatus(QualityGateStatus.PASSED)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result if quality gate has been passed (Result = SUCCESS)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResult(Result.SUCCESS)
                        .setQualityGateStatus(QualityGateStatus.FAILED)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no analysis result if quality gate has been failed (Result = SUCCESS)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResult(Result.SUCCESS)
                        .setQualityGateStatus(QualityGateStatus.WARNING)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no analysis result if quality gate has been a warning (Result = SUCCESS)")
                        .build(),

                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResult(Result.UNSTABLE)
                        .setQualityGateStatus(QualityGateStatus.INACTIVE)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result if quality gate is not active (Result = UNSTABLE)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResult(Result.UNSTABLE)
                        .setQualityGateStatus(QualityGateStatus.PASSED)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result if quality gate has been passed (Result = UNSTABLE)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResult(Result.UNSTABLE)
                        .setQualityGateStatus(QualityGateStatus.FAILED)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no analysis result if quality gate has been failed (Result = UNSTABLE)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResult(Result.UNSTABLE)
                        .setQualityGateStatus(QualityGateStatus.WARNING)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no analysis result if quality gate has been a warning (Result = UNSTABLE)")
                        .build(),

                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResult(Result.FAILURE)
                        .setQualityGateStatus(QualityGateStatus.INACTIVE)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result if quality gate is not active (Result = FAILURE)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResult(Result.FAILURE)
                        .setQualityGateStatus(QualityGateStatus.PASSED)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result if quality gate has been passed (Result = FAILURE)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResult(Result.FAILURE)
                        .setQualityGateStatus(QualityGateStatus.FAILED)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no analysis result if quality gate has been failed (Result = FAILURE)")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setQualityGateEvaluationMode(SUCCESSFUL_QUALITY_GATE)
                        .setJobResult(Result.FAILURE)
                        .setQualityGateStatus(QualityGateStatus.WARNING)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no analysis result if quality gate has been a warning (Result = FAILURE)")
                        .build()
        );
    }

    /**
     * Method to provide test element that return an present optional.
     *
     * @return list of test data objects
     */
    private static Iterable<Object> provideSingleResultIfQualityGateIsIgnored() {
        return asList(
                new BuildHistoryBuilder().setHasResult(true)
                        .setJobResultEvaluationMode(NO_JOB_FAILURE)
                        .setJobResult(Result.SUCCESS)
                        .setQualityGateStatus(QualityGateStatus.INACTIVE)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setJobResultEvaluationMode(NO_JOB_FAILURE)
                        .setJobResult(Result.SUCCESS)
                        .setQualityGateStatus(QualityGateStatus.FAILED)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(FIRST)
                        .setTestName("Job has analysis result even if quality gate has been missed")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setJobResultEvaluationMode(NO_JOB_FAILURE)
                        .setJobResult(Result.FAILURE)
                        .setQualityGateStatus(QualityGateStatus.FAILED)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no analysis result since Result is FAILURE")
                        .build(),
                new BuildHistoryBuilder().setHasResult(true)
                        .setJobResultEvaluationMode(NO_JOB_FAILURE)
                        .setJobResult(Result.UNSTABLE)
                        .setQualityGateStatus(QualityGateStatus.INACTIVE)
                        .setExpectedBaselineResult(FIRST)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no analysis result since Result is UNSTABLE")
                        .build(),
                new BuildHistoryBuilder().setHasResult(false)
                        .setJobResultEvaluationMode(NO_JOB_FAILURE)
                        .setJobResult(Result.SUCCESS)
                        .setQualityGateStatus(QualityGateStatus.INACTIVE)
                        .setExpectedBaselineResult(NONE)
                        .setExpectedPreviousResult(NONE)
                        .setTestName("Job has no result")
                        .build()
        );
    }

    /**
     * Determines which of the builds is the expected result.
     */
    enum ExpectedResult {
        NONE,
        FIRST,
        SECOND,
        THIRD
    }

    /**
     * Builds arg for the parameterized test.
     */
    private static class BuildHistoryBuilder {
        private String testName;
        private boolean hasResult;
        private QualityGateStatus qualityGateStatus;
        private Result jobResult;
        private ExpectedResult expectedBaselineResult;
        private ExpectedResult expectedPreviousResult;
        private QualityGateEvaluationMode qualityGateEvaluationMode = IGNORE_QUALITY_GATE;
        private JobResultEvaluationMode jobResultEvaluationMode = IGNORE_JOB_RESULT;

        public BuildHistoryBuilder setQualityGateEvaluationMode(
                final QualityGateEvaluationMode qualityGateEvaluationMode) {
            this.qualityGateEvaluationMode = qualityGateEvaluationMode;
            return this;
        }

        public BuildHistoryBuilder setJobResultEvaluationMode(
                final JobResultEvaluationMode jobResultEvaluationMode) {
            this.jobResultEvaluationMode = jobResultEvaluationMode;
            return this;
        }

        public BuildHistoryBuilder setExpectedBaselineResult(
                final ExpectedResult expectedBaselineResult) {
            this.expectedBaselineResult = expectedBaselineResult;
            return this;
        }

        public BuildHistoryBuilder setExpectedPreviousResult(
                final ExpectedResult expectedPreviousResult) {
            this.expectedPreviousResult = expectedPreviousResult;
            return this;
        }

        public BuildHistoryBuilder setTestName(final String testName) {
            this.testName = testName;
            return this;
        }

        public BuildHistoryBuilder setHasResult(final boolean hasResult) {
            this.hasResult = hasResult;
            return this;
        }

        public BuildHistoryBuilder setQualityGateStatus(final QualityGateStatus qualityGateStatus) {
            this.qualityGateStatus = qualityGateStatus;
            return this;
        }

        public BuildHistoryBuilder setJobResult(final Result jobResult) {
            this.jobResult = jobResult;
            return this;
        }

        /**
         * Build the tests argument.
         *
         * @return test arg
         */
        public Object build() {
            return Arguments.of(
                    testName,
                    qualityGateEvaluationMode,
                    jobResultEvaluationMode,
                    hasResult,
                    qualityGateStatus,
                    jobResult,
                    expectedBaselineResult,
                    expectedPreviousResult);
        }
    }
}