package com.hb216.recipewizard;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.pressKey;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.anything;

import android.content.res.Resources;
import android.view.KeyEvent;


@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class JeffreyDevTests {
    private static final int SIMULATED_DELAY_MS = 500;

    @Rule   // needed to launch the activity
    public ActivityScenarioRule<AdminDevActivity> devActivity = new ActivityScenarioRule<>(AdminDevActivity.class);

    @Test
    public void themeTest() {
        String newThemeText = "Jeffrey Test";
        String newBgColor = "#ff0000";
        String newTextColor = "#00ff00";
        String editBadTextColor = "-------";
        String editGoodTextColor = "#0000ff";

        onView(withId(R.id.btnDevCreateTheme)).perform(click());

        // create a theme
        onView(withId(R.id.etCreateThemeName)).perform(replaceText(newThemeText), closeSoftKeyboard());
        onView(withId(R.id.etBackgroundHexcode)).perform(typeText(newBgColor), closeSoftKeyboard());
        onView(withId(R.id.etTextHexcode)).perform(typeText(newTextColor), closeSoftKeyboard());
        onView(withId(R.id.btnEditorCreateApplyTheme)).perform(click());

        volleyDelay();

        // check that the current theme name is the test theme that we created
        onView(withId(R.id.spinThemes)).check(matches(withSpinnerText(newThemeText)));

        onView(withId(R.id.btnDevChangeTheme)).perform(click());

        volleyDelay();

        // try to edit theme with incorrect hexcode
        onView(withId(R.id.etBackgroundHexcode)).perform(replaceText(editBadTextColor), closeSoftKeyboard());
        onView(withId(R.id.btnEditorApplyTheme)).perform(click());

        volleyDelay();

        // check that we did not successfully edit the theme, and are instead still in the theme editor page
        Assert.assertEquals(false, devActivity.getScenario().getState().isAtLeast(Lifecycle.State.RESUMED));

        // edit theme with correct info
        onView(withId(R.id.etBackgroundHexcode)).perform(replaceText(editGoodTextColor), closeSoftKeyboard());
        onView(withId(R.id.btnEditorApplyTheme)).perform(click());

        volleyDelay();

        // check if we exited back to dev page, as intended
        Assert.assertEquals(Lifecycle.State.CREATED, devActivity.getScenario().getState());

        // check that the current theme is the one we just edited
        onView(withId(R.id.spinThemes)).check(matches(withSpinnerText(newThemeText)));

        onView(withId(R.id.btnDevChangeTheme)).perform(click());

        // delete theme
        onView(withId(R.id.btnEditorDeleteTheme)).perform(click());

        volleyDelay();

        // check that we exited the theme editor
        Assert.assertEquals(Lifecycle.State.CREATED, devActivity.getScenario().getState());
    }

    @Test
    public void recipeOfTheWeekTest() {
        onView(withId(R.id.btnChangeROTW)).perform(click());
        volleyDelay();

        onData(anything()).inAdapterView(withId(R.id.lvDevRecipeList)).atPosition(1).perform(click());
        volleyDelay();

        onView(withId(R.id.btnChangeROTW)).perform(click());
        volleyDelay();

        onData(anything()).inAdapterView(withId(R.id.lvDevRecipeList)).atPosition(0).perform(click());
        volleyDelay();
    }

    public void volleyDelay() {
        try {
            Thread.sleep(SIMULATED_DELAY_MS);
        } catch (InterruptedException e) {
        }
    }
}
