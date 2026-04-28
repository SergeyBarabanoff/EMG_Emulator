package com.example.emgphone;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void mainScreenLoads() {
        onView(withText("EMG Phone Controller")).check(matches(isDisplayed()));
        onView(withId(R.id.hostEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.portEditText)).check(matches(isDisplayed()));
        onView(withId(R.id.cursorStepEditText)).check(matches(isDisplayed()));
    }

    @Test
    public void diagnosticsViewsAreVisible() {
        onView(withId(R.id.connectionStatusTextView)).check(matches(isDisplayed()));
        onView(withId(R.id.lastMessageTextView)).check(matches(isDisplayed()));
        onView(withId(R.id.lastCommandTextView)).check(matches(isDisplayed()));
    }

    @Test
    public void testButtonsAreVisible() {
        onView(withId(R.id.startTestSquareButton)).check(matches(isDisplayed()));
        onView(withId(R.id.stopTestSquareButton)).check(matches(isDisplayed()));
        onView(withId(R.id.openKeyboardTestButton)).check(matches(isDisplayed()));
    }
}