package com.hb216.recipewizard;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.core.StringEndsWith.endsWith;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;



@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class Maddies_Tests {
    private static final int SIMULATED_DELAY_MS = 500;

    @Rule
    public ActivityScenarioRule<HomeActivity> homeActivityRule = new ActivityScenarioRule<>(HomeActivity.class);

    @Test
    public void goToCorrectViewPageAfterClickingOnRecipe(){
        try {
            Thread.sleep(SIMULATED_DELAY_MS * 2);
        } catch (InterruptedException e) {}
        onView(ViewMatchers.withId(R.id.rsRecyclerView)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        try {
            Thread.sleep(SIMULATED_DELAY_MS * 2);
        } catch (InterruptedException e) {}

        String resultTitle = "Just Flour";
        onView(withId(R.id.title)).check(matches(withText(endsWith(resultTitle))));

        String resultInstructions = "flour";
        onView(withId(R.id.instructionBox)).check(matches(withText(endsWith(resultInstructions))));
    }

    @Test
    public void makeRecipeDoestChangePantry(){
        waiting();
        onView(withId(R.id.rsRecyclerView)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        waiting();

        //in ViewRecipe
        onView(withId(R.id.madeBtn)).perform(click());
        waiting();
        onView(withId(R.id.backBtn)).perform(scrollTo()).perform(click());
        //in Home
        onView(withId(R.id.btnHomeToProfile)).perform(click());
        //In Profile
        onView(withId(R.id.btnPantryPage)).perform(click());
        //In Pantry
        onView(withRecyclerView(R.id.pantryRV).atPosition(0)).check(matches(hasDescendant(withText("salt"))));
    }

    @Test
    public void goingToMakeRecipe(){
        waiting();
        onView(withId(R.id.rsRecyclerView)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        waiting();

        //in ViewRecipe
        onView(withId(R.id.goingToMakeBtn)).perform(click());
        waiting();
        onView(withId(R.id.backBtn)).perform(scrollTo()).perform(click());
        //in Home
        onView(withId(R.id.btnHomeToProfile)).perform(click());
        //In Profile
        onView(withId(R.id.btnPantryPage)).perform(click());
        //In Pantry
        onView(withId(R.id.shoppingBtn)).perform(click());
        //In shoppingList
        onView(withRecyclerView(R.id.shoppingRV).atPosition(0)).check(matches(hasDescendant(withText("flour"))));
    }
    @Test
    public void testAddingRecipe() {
        onView(withId(R.id.addRecipeBtn)).perform(click());

        String title = "blueberry pie";
        String photoURl = "https://www.tasteofhome.com/wp-content/uploads/2018/01/exps145896_TH153342B03_13_5b-3.jpg";
        String instructions = "Make the crust. Make the filling. Fill the crust with the filling and bake at 350 for 30 minutes.";
        onView(withId(R.id.editTitle)).perform(typeText(title), closeSoftKeyboard());
        onView(withId(R.id.photoURL)).perform(typeText(photoURl), closeSoftKeyboard());
        onView(withId(R.id.instructions)).perform(typeText(instructions), closeSoftKeyboard());
        onView(withId(R.id.submitBtn)).perform(click());

        try {
            Thread.sleep(SIMULATED_DELAY_MS);
        } catch (InterruptedException e) {}
        //now in ViewRecipe
        //checking to see if recipe was made and we are now looking at it in ViewRecipe
        onView(withId(R.id.title)).check(matches(withText(endsWith(title))));

    }

    public void waiting(){
        try {
            Thread.sleep(SIMULATED_DELAY_MS);
        } catch (InterruptedException e) {}
    }

    public static RecyclerViewMatcher withRecyclerView(final int recyclerViewId) {
        return new RecyclerViewMatcher(recyclerViewId);
    }


}
