package com.chromadmx.ui.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class OnboardingStepTest {

    @Test
    fun stepsListContainsSixEntries() {
        assertEquals(6, OnboardingStep.steps.size)
    }

    @Test
    fun stepsAreInCorrectOrder() {
        val steps = OnboardingStep.steps
        assertIs<OnboardingStep.Splash>(steps[0])
        assertIs<OnboardingStep.NetworkDiscovery>(steps[1])
        assertIs<OnboardingStep.FixtureScan>(steps[2])
        assertIs<OnboardingStep.VibeCheck>(steps[3])
        assertIs<OnboardingStep.StagePreview>(steps[4])
        assertIs<OnboardingStep.Complete>(steps[5])
    }

    @Test
    fun splashIsFirstStep() {
        assertEquals(0, OnboardingStep.steps.indexOf(OnboardingStep.Splash))
    }

    @Test
    fun completeIsLastStep() {
        assertEquals(
            OnboardingStep.steps.lastIndex,
            OnboardingStep.steps.indexOf(OnboardingStep.Complete)
        )
    }

    @Test
    fun stagePreviewIsBeforeComplete() {
        val previewIndex = OnboardingStep.steps.indexOf(OnboardingStep.StagePreview)
        val completeIndex = OnboardingStep.steps.indexOf(OnboardingStep.Complete)
        assertEquals(completeIndex - 1, previewIndex)
    }

    @Test
    fun allStepsAreDistinct() {
        val steps = OnboardingStep.steps
        assertEquals(steps.size, steps.distinct().size)
    }

    @Test
    fun dataObjectEqualityWorks() {
        assertEquals(OnboardingStep.Splash, OnboardingStep.Splash)
        assertEquals(OnboardingStep.Complete, OnboardingStep.Complete)
        assertNotEquals(OnboardingStep.Splash as OnboardingStep, OnboardingStep.Complete as OnboardingStep)
    }
}
