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
public class KeyboardTestActivityTest {

    @Rule
    public ActivityScenarioRule<KeyboardTestActivity> activityRule =
            new ActivityScenarioRule<>(KeyboardTestActivity.class);

    @Test
    public void keyboardTestScreenLoads() {
        onView(withText("Keyboard Cursor Test")).check(matches(isDisplayed()));
        onView(withId(R.id.instructionsTextView)).check(matches(isDisplayed()));
        onView(withId(R.id.statusTextView)).check(matches(isDisplayed()));
    }
}