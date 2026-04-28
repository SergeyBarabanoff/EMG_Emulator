package com.example.emgphone;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class MainToKeyboardNavigationTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void openKeyboardTestButtonNavigatesToKeyboardScreen() {
        onView(withId(R.id.openKeyboardTestButton)).perform(click());
        onView(withText("Keyboard Cursor Test")).check(matches(isDisplayed()));
    }

    @Test
    public void canNavigateToKeyboardScreenAndBack() {
        onView(withId(R.id.openKeyboardTestButton)).perform(click());
        onView(withText("Keyboard Cursor Test")).check(matches(isDisplayed()));
        pressBack();
        onView(withText("EMG Phone Controller")).check(matches(isDisplayed()));
    }
}