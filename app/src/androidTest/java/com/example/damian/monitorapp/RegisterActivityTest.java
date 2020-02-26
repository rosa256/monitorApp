package com.example.damian.monitorapp;


import android.text.InputType;

import androidx.test.core.app.ActivityScenario;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.example.damian.monitorapp.activities.RegisterActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withInputType;
import static androidx.test.espresso.matcher.ViewMatchers.withText;


@RunWith(AndroidJUnit4ClassRunner.class)
public class RegisterActivityTest {

    @Test
    public void activityExistTest(){

        ActivityScenario registerScenario = ActivityScenario.launch(RegisterActivity.class);

        onView(withId(R.id.registerActivity)).check(matches(isDisplayed()));
    }

    @Test
    public void correctRegisterTitle(){

        ActivityScenario registerScenario = ActivityScenario.launch(RegisterActivity.class);

        onView(withId(R.id.registerTitleId)).check(matches(withText(R.string.registerStr)));
    }

    @Test
    public void registerButtonExist(){

        ActivityScenario registerScenario = ActivityScenario.launch(RegisterActivity.class);
        onView(withId(R.id.registerActivity)).check(matches(isDisplayed()));
    }


    @Test
    public void registerInputsExists(){

        ActivityScenario registerScenario = ActivityScenario.launch(RegisterActivity.class);

        onView(withId(R.id.input_layout_username)).check(matches(isDisplayed()));
        onView(withId(R.id.input_layout_email)).check(matches(isDisplayed()));
        onView(withId(R.id.input_layout_password)).check(matches(isDisplayed()));
        onView(withId(R.id.input_layout_password_confirm)).check(matches(isDisplayed()));
    }

    @Test
    public void registerInputsTypes(){

        ActivityScenario registerScenario = ActivityScenario.launch(RegisterActivity.class);

        onView(withId(R.id.usernameEditText)).check(matches(withInputType(InputType.TYPE_CLASS_TEXT)));
        //onView(withId(R.id.emailEditText)).perform(typeText("textEmailAddress"));
        //onView(withId(R.id.passwordEditText)).perform(typeText("textPassword"));
        //onView(withId(R.id.passwordConfirmEditText)).perform(typeText("textPassword"));
    }
}