package com.hb216.recipewizard;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.repeatedlyUntil;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class JeffreyPendingRecipeTest {
    private static final int SIMULATED_DELAY_MS = 500;

    @Rule
    public ActivityScenarioRule<AddRecipeActivity> addRecipeActivity = new ActivityScenarioRule<AddRecipeActivity>(AddRecipeActivity.class);

    @Test
    public void denyRecipeTest() {
        String recipeTitle = "Jeffrey Test";
        String instructions = "Do some stuff";

        // create a new recipe
        onView(withId(R.id.editTitle)).perform(typeText(recipeTitle), closeSoftKeyboard());
        onView(withId(R.id.instructions)).perform(typeText(instructions), closeSoftKeyboard());
        onView(withId(R.id.submitBtn)).perform(click());

        volleyDelay();

        // go to admin moderation page
        onView(withId(R.id.backBtn)).perform(click());
        volleyDelay();
        onView(withId(R.id.btnHomeToProfile)).perform(click());
        volleyDelay();
        onView(withId(R.id.btnProfileToAdmin)).perform(click());
        volleyDelay();
        onView(withId(R.id.rbModMode)).perform(click());
        volleyDelay();
        onView(withId(R.id.btnModeratePage)).perform(click());
        volleyDelay();
        onView(withId(R.id.rbModerateRecipes)).perform(click());
        volleyDelay();

        // deny pending recipes until there are no pending recipes left
        onView(withId(R.id.btnDenyPendingRecipe)).perform(repeatedlyUntil(click(),
                withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE), 100));

        volleyDelay();

        // check that the pending recipe title is correct
        onView(withId(R.id.tvModerateRecipeTitle)).check(matches(withText("No pending recipes")));
    }


    public void volleyDelay() {
        try {
            Thread.sleep(SIMULATED_DELAY_MS);
        } catch (InterruptedException e) {
        }
    }
}
