package com.example.damian.monitorapp;

import android.text.InputType;

import androidx.test.core.app.ActivityScenario;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.example.damian.monitorapp.activities.LoginActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withInputType;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4ClassRunner.class)
public class LoginActivityTest {


    @Test
    public void activityExistTest(){

        ActivityScenario.launch(LoginActivity.class);

        onView(withId(R.id.loginActivity)).check(matches(isDisplayed()));
    }

    @Test
    public void correctLoginTitle(){

        ActivityScenario.launch(LoginActivity.class);
        onView(withId(R.id.loginTitleId)).check(matches(withText(R.string.loginStr)));
    }

    @Test
    public void everyButtonExist(){

        ActivityScenario.launch(LoginActivity.class);
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
        onView(withId(R.id.goToRegistrationButton)).check(matches(isDisplayed()));
        onView(withId(R.id.goToForgotPassword)).check(matches(isDisplayed()));
    }


    @Test
    public void loginInputsExists(){
        ActivityScenario.launch(LoginActivity.class);

        onView(withId(R.id.inputLoginUsername)).check(matches(isDisplayed()));
        onView(withId(R.id.inputLoginPassword)).check(matches(isDisplayed()));
    }

    @Test
    public void loginInputsTypes(){
        ActivityScenario.launch(LoginActivity.class);

        onView(withId(R.id.inputLoginUsername)).check(matches(withInputType(InputType.TYPE_CLASS_TEXT)));
    }

    @Test
    public void goToCreateNewAccountTest(){
        ActivityScenario.launch(LoginActivity.class);

        onView(withId(R.id.goToRegistrationButton)).perform(click());
        onView(withId(R.id.registerActivity)).check(matches(isDisplayed()));
    }
    @Test
    public void canBackToLoginTest(){
        ActivityScenario.launch(LoginActivity.class);

        onView(withId(R.id.goToRegistrationButton)).perform(click());
        onView(withId(R.id.registerActivity)).check(matches(isDisplayed()));
        pressBack();
        onView(withId(R.id.loginActivity)).check(matches(isDisplayed()));
    }

    @Test
    public void wrongCredentialsPopupTest() throws InterruptedException {
        ActivityScenario.launch(LoginActivity.class);
        String mockInput = "Test123";
        onView(withId(R.id.inputLoginUsername)).perform(typeText(mockInput));
        onView(withId(R.id.inputLoginPassword)).perform(typeText(mockInput));
        onView(withId(R.id.loginButton)).perform(click());
        Thread.sleep(500);
        onView(withText(R.string.wrong_credentials)).check(matches(isDisplayed()));
    }
}
