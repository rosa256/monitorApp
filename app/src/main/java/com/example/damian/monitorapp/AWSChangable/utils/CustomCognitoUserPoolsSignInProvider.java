package com.example.damian.monitorapp.AWSChangable.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.amazonaws.mobile.auth.core.internal.util.ViewHelper;
import com.amazonaws.mobile.auth.core.signin.SignInProviderResultHandler;
import com.amazonaws.mobile.auth.userpools.CognitoUserPoolsSignInProvider;
import com.amazonaws.mobile.auth.userpools.ForgotPasswordActivity;
import com.amazonaws.mobile.auth.userpools.MFAActivity;
import com.amazonaws.mobile.auth.userpools.SignUpActivity;
import com.amazonaws.mobile.auth.userpools.SignUpConfirmActivity;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.ForgotPasswordHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.VerificationHandler;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentityprovider.model.InvalidParameterException;
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException;
import com.amazonaws.services.cognitoidentityprovider.model.SignUpResult;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotConfirmedException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException;
import com.example.damian.monitorapp.AWSChangable.ui.UserPoolSignInView;
import com.example.damian.monitorapp.R;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by neha on 20/12/17.
 */

public class CustomCognitoUserPoolsSignInProvider extends CognitoUserPoolsSignInProvider {

    private static final String LOG_TAG = CustomCognitoUserPoolsSignInProvider.class.getSimpleName();
    private static final int REQUEST_CODE_START = 10608;
    private static final int FORGOT_PASSWORD_REQUEST_CODE = 10650;
    private static final int SIGN_UP_REQUEST_CODE = 10651;
    private static final int MFA_REQUEST_CODE = 10652;
    private static final int VERIFICATION_REQUEST_CODE = 10653;
    private static final Set<Integer> REQUEST_CODES = new HashSet() {
        {
            this.add(Integer.valueOf(10650));
            this.add(Integer.valueOf(10651));
            this.add(Integer.valueOf(10652));
            this.add(Integer.valueOf(10653));
        }
    };
    private static final String AWS_CONFIGURATION_FILE = "AWSConfiguration";
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final String USERPOOLS_EXCEPTION_PREFIX = "(Service";
    private SignInProviderResultHandler resultsHandler;
    private ForgotPasswordContinuation forgotPasswordContinuation;
    private MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation;
    private Context context;
    private Activity activity;
    private String username;
    private String password;
    private String verificationCode;
    private String cognitoLoginKey;
    private CognitoUserPool cognitoUserPool;
    private CognitoUserSession cognitoUserSession;
    private AWSConfiguration awsConfiguration;
    private int backgroundColor;
    private ForgotPasswordHandler forgotPasswordHandler = new ForgotPasswordHandler() {
        public void onSuccess() {
            Log.d(CustomCognitoUserPoolsSignInProvider.LOG_TAG, "Password change succeeded.");
            ViewHelper.showDialog(CustomCognitoUserPoolsSignInProvider.this.activity, CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_activity_forgot_password), CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.password_change_success));
        }

        public void getResetCode(ForgotPasswordContinuation continuation) {
            CustomCognitoUserPoolsSignInProvider.this.forgotPasswordContinuation = continuation;
            Intent intent = new Intent(CustomCognitoUserPoolsSignInProvider.this.context, ForgotPasswordActivity.class);
            intent.putExtra("signInBackgroundColor", CustomCognitoUserPoolsSignInProvider.this.backgroundColor);
            CustomCognitoUserPoolsSignInProvider.this.activity.startActivityForResult(intent, 10650);
        }

        public void onFailure(Exception exception) {
            Log.e(CustomCognitoUserPoolsSignInProvider.LOG_TAG, "Password change failed.", exception);
            String message;
            if(exception instanceof InvalidParameterException) {
                message = CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.password_change_no_verification_failed);
            } else {
                message = CustomCognitoUserPoolsSignInProvider.getErrorMessageFromException(exception);
            }

            ViewHelper.showDialog(CustomCognitoUserPoolsSignInProvider.this.activity, CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_activity_forgot_password), CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.password_change_failed) + " " + message);
        }
    };
    private SignUpHandler signUpHandler = new SignUpHandler() {
        public void onSuccess(CognitoUser user, boolean signUpConfirmationState, CognitoUserCodeDeliveryDetails cognitoUserCodeDeliveryDetails) {
            if(signUpConfirmationState) {
                Log.d(CustomCognitoUserPoolsSignInProvider.LOG_TAG, "Signed up. User ID = " + user.getUserId());
                ViewHelper.showDialog(CustomCognitoUserPoolsSignInProvider.this.activity, CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_activity_sign_up), CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.sign_up_success) + " " + user.getUserId());
            } else {
                Log.w(CustomCognitoUserPoolsSignInProvider.LOG_TAG, "Additional confirmation for sign up.");
                CustomCognitoUserPoolsSignInProvider.this.startVerificationActivity();
            }

        }

        @Override
        public void onSuccess(CognitoUser user, SignUpResult signUpResult) {

        }

        public void onFailure(Exception exception) {
            Log.e(CustomCognitoUserPoolsSignInProvider.LOG_TAG, "Sign up failed.", exception);
            ViewHelper.showDialog(CustomCognitoUserPoolsSignInProvider.this.activity, CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_dialog_sign_up_failed), exception.getLocalizedMessage() != null? CustomCognitoUserPoolsSignInProvider.getErrorMessageFromException(exception): CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.sign_up_failed));
        }
    };
    private GenericHandler signUpConfirmationHandler = new GenericHandler() {
        public void onSuccess() {
            Log.i(CustomCognitoUserPoolsSignInProvider.LOG_TAG, "Confirmed.");
            ViewHelper.showDialog(CustomCognitoUserPoolsSignInProvider.this.activity, CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_activity_sign_up_confirm), CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.sign_up_confirm_success));
        }

        public void onFailure(Exception exception) {
            Log.e(CustomCognitoUserPoolsSignInProvider.LOG_TAG, "Failed to confirm user.", exception);
            ViewHelper.showDialog(CustomCognitoUserPoolsSignInProvider.this.activity, CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_activity_sign_up_confirm), CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.sign_up_confirm_failed) + " " + CustomCognitoUserPoolsSignInProvider.getErrorMessageFromException(exception));
        }
    };
    private AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
            Log.i(CustomCognitoUserPoolsSignInProvider.LOG_TAG, "Logged in. " + userSession.getIdToken());
            CustomCognitoUserPoolsSignInProvider.this.cognitoUserSession = userSession;
            if(null != CustomCognitoUserPoolsSignInProvider.this.resultsHandler) {
                CustomCognitoUserPoolsSignInProvider.this.resultsHandler.onSuccess(CustomCognitoUserPoolsSignInProvider.this);
            }

        }

        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
            if(null != CustomCognitoUserPoolsSignInProvider.this.username && null != CustomCognitoUserPoolsSignInProvider.this.password) {
                AuthenticationDetails authenticationDetails = new AuthenticationDetails(CustomCognitoUserPoolsSignInProvider.this.username, CustomCognitoUserPoolsSignInProvider.this.password, (Map)null);
                authenticationContinuation.setAuthenticationDetails(authenticationDetails);
                authenticationContinuation.continueTask();
            }

        }

        public void getMFACode(MultiFactorAuthenticationContinuation continuation) {
            CustomCognitoUserPoolsSignInProvider.this.multiFactorAuthenticationContinuation = continuation;
            Intent intent = new Intent(CustomCognitoUserPoolsSignInProvider.this.context, MFAActivity.class);
            intent.putExtra("signInBackgroundColor", CustomCognitoUserPoolsSignInProvider.this.backgroundColor);
            CustomCognitoUserPoolsSignInProvider.this.activity.startActivityForResult(intent, 10652);
        }

        public void authenticationChallenge(ChallengeContinuation continuation) {
            throw new UnsupportedOperationException("Not supported in this sample.");
        }

        public void onFailure(Exception exception) {
            activity.findViewById(R.id.progressBar).setVisibility(View.GONE);
            Log.e(CustomCognitoUserPoolsSignInProvider.LOG_TAG, "Failed to login.", exception);
            if(exception instanceof UserNotConfirmedException) {
                CustomCognitoUserPoolsSignInProvider.this.resendConfirmationCode();
            } else {
                String message;
                if(exception instanceof UserNotFoundException) {
                    message = CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.user_does_not_exist);
                } else if(exception instanceof NotAuthorizedException) {
                    message = CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.incorrect_username_or_password);
                } else {
                    message = CustomCognitoUserPoolsSignInProvider.getErrorMessageFromException(exception);
                }

                if(null != CustomCognitoUserPoolsSignInProvider.this.resultsHandler) {
                    ViewHelper.showDialog(CustomCognitoUserPoolsSignInProvider.this.activity, CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_activity_sign_in), CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.login_failed) + " " + message);
                    CustomCognitoUserPoolsSignInProvider.this.resultsHandler.onError(CustomCognitoUserPoolsSignInProvider.this, exception);
                }

            }
        }
    };

    public CustomCognitoUserPoolsSignInProvider() {
    }

    private void startVerificationActivity() {
        Intent intent = new Intent(this.context, SignUpConfirmActivity.class);
        intent.putExtra("username", this.username);
        intent.putExtra("signInBackgroundColor", this.backgroundColor);
        this.activity.startActivityForResult(intent, 10653);
    }

    private void resendConfirmationCode() {
        CognitoUser cognitoUser = this.cognitoUserPool.getUser(this.username);
        cognitoUser.resendConfirmationCodeInBackground(new VerificationHandler() {
            public void onSuccess(CognitoUserCodeDeliveryDetails verificationCodeDeliveryMedium) {
                CustomCognitoUserPoolsSignInProvider.this.startVerificationActivity();
            }

            public void onFailure(Exception exception) {
                if(null != CustomCognitoUserPoolsSignInProvider.this.resultsHandler) {
                    ViewHelper.showDialog(CustomCognitoUserPoolsSignInProvider.this.activity, CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_activity_sign_in), CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.login_failed) + "\nUser was not verified and resending confirmation code failed.\n" + CustomCognitoUserPoolsSignInProvider.getErrorMessageFromException(exception));
                    CustomCognitoUserPoolsSignInProvider.this.resultsHandler.onError(CustomCognitoUserPoolsSignInProvider.this, exception);
                }

            }
        });
    }

    public void initialize(Context context, AWSConfiguration awsConfiguration) {
        this.context = context;
        this.awsConfiguration = awsConfiguration;
        Log.d(LOG_TAG, "initalizing Cognito User Pools");
        String regionString = this.getCognitoUserPoolRegion();
        Regions region = Regions.fromName(regionString);
        this.cognitoUserPool = new CognitoUserPool(context, this.getCognitoUserPoolId(), this.getCognitoUserPoolClientId(), this.getCognitoUserPoolClientSecret(), region);
        this.cognitoLoginKey = "cognito-idp." + region.getName() + ".amazonaws.com/" + this.getCognitoUserPoolId();
        Log.d(LOG_TAG, "CognitoLoginKey: " + this.cognitoLoginKey);
    }

    public boolean isRequestCodeOurs(int requestCode) {
        return REQUEST_CODES.contains(Integer.valueOf(requestCode));
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if(-1 == resultCode) {
            switch(requestCode) {
                case 10650:
                    this.password = data.getStringExtra("password");
                    this.verificationCode = data.getStringExtra("verification_code");
                    if(this.password.length() < 6) {
                        ViewHelper.showDialog(this.activity, this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_activity_forgot_password), this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.password_change_failed) + " " + this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.password_length_validation_failed));
                        return;
                    }

                    Log.d(LOG_TAG, "verificationCode = " + this.verificationCode);
                    this.forgotPasswordContinuation.setPassword(this.password);
                    this.forgotPasswordContinuation.setVerificationCode(this.verificationCode);
                    this.forgotPasswordContinuation.continueTask();
                    break;
                case 10651:
                    this.username = data.getStringExtra("username");
                    this.password = data.getStringExtra("password");
                    String givenName = data.getStringExtra("given_name");
                    String email = data.getStringExtra("email");
                    String phone = data.getStringExtra("phone_number");
                    if(this.username.length() < 1) {
                        ViewHelper.showDialog(this.activity, this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_activity_sign_up), this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.sign_up_failed) + " " + this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.sign_up_username_missing));
                        return;
                    }

                    if(this.password.length() < 6) {
                        ViewHelper.showDialog(this.activity, this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_activity_sign_up), this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.sign_up_failed) + " " + this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.password_length_validation_failed));
                        return;
                    }

                    Log.d(LOG_TAG, "username = " + this.username);
                    Log.d(LOG_TAG, "given_name = " + givenName);
                    Log.d(LOG_TAG, "email = " + email);
                    Log.d(LOG_TAG, "phone = " + phone);
                    CognitoUserAttributes userAttributes = new CognitoUserAttributes();
                    userAttributes.addAttribute("given_name", givenName);
                    userAttributes.addAttribute("email", email);
                    if(null != phone && phone.length() > 0) {
                        userAttributes.addAttribute("phone_number", phone);
                    }

                    this.cognitoUserPool.signUpInBackground(this.username, this.password, userAttributes, (Map)null, this.signUpHandler);
                    break;
                case 10652:
                    this.verificationCode = data.getStringExtra("verification_code");
                    if(this.verificationCode.length() < 1) {
                        ViewHelper.showDialog(this.activity, this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_activity_mfa), this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.mfa_failed) + " " + this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.mfa_code_empty));
                        return;
                    }

                    Log.d(LOG_TAG, "verificationCode = " + this.verificationCode);
                    this.multiFactorAuthenticationContinuation.setMfaCode(this.verificationCode);
                    this.multiFactorAuthenticationContinuation.continueTask();
                    break;
                case 10653:
                    this.username = data.getStringExtra("username");
                    this.verificationCode = data.getStringExtra("verification_code");
                    if(this.username.length() < 1) {
                        ViewHelper.showDialog(this.activity, this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_activity_sign_up_confirm), this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.sign_up_confirm_title) + " " + this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.sign_up_username_missing));
                        return;
                    }

                    if(this.verificationCode.length() < 1) {
                        ViewHelper.showDialog(this.activity, this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_activity_sign_up_confirm), this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.sign_up_confirm_title) + " " + this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.sign_up_confirm_code_missing));
                        return;
                    }

                    Log.d(LOG_TAG, "username = " + this.username);
                    Log.d(LOG_TAG, "verificationCode = " + this.verificationCode);
                    CognitoUser cognitoUser = this.cognitoUserPool.getUser(this.username);
                    cognitoUser.confirmSignUpInBackground(this.verificationCode, true, this.signUpConfirmationHandler);
                    break;
                default:
                    Log.e(LOG_TAG, "Unknown Request Code sent.");
            }
        }

    }

    public View.OnClickListener initializeSignInButton(Activity signInActivity, View buttonView, SignInProviderResultHandler providerResultsHandler) {
        this.activity = signInActivity;
        this.resultsHandler = providerResultsHandler;
        final UserPoolSignInView userPoolSignInView = (UserPoolSignInView)this.activity.findViewById(com.amazonaws.mobile.auth.userpools.R.id.user_pool_sign_in_view_id);
        this.backgroundColor = userPoolSignInView.getBackgroundColor();
        userPoolSignInView.getSignUpTextView().setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(CustomCognitoUserPoolsSignInProvider.this.context, SignUpActivity.class);
                intent.putExtra("signInBackgroundColor", CustomCognitoUserPoolsSignInProvider.this.backgroundColor);
                CustomCognitoUserPoolsSignInProvider.this.activity.startActivityForResult(intent, 10651);
            }
        });
        TextView forgotPasswordTextView = userPoolSignInView.getForgotPasswordTextView();
        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                CustomCognitoUserPoolsSignInProvider.this.username = userPoolSignInView.getEnteredUserName();
                if(CustomCognitoUserPoolsSignInProvider.this.username.length() < 1) {
                    Log.w(CustomCognitoUserPoolsSignInProvider.LOG_TAG, "Missing username.");
                    ViewHelper.showDialog(CustomCognitoUserPoolsSignInProvider.this.activity, CustomCognitoUserPoolsSignInProvider.this.activity.getString(com.amazonaws.mobile.auth.userpools.R.string.title_activity_sign_in), "Missing username.");
                } else {
                    CognitoUser cognitoUser = CustomCognitoUserPoolsSignInProvider.this.cognitoUserPool.getUser(CustomCognitoUserPoolsSignInProvider.this.username);
                    cognitoUser.forgotPasswordInBackground(CustomCognitoUserPoolsSignInProvider.this.forgotPasswordHandler);
                }

            }
        });
        View.OnClickListener listener = new View.OnClickListener() {
            public void onClick(View view) {
                activity.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                CustomCognitoUserPoolsSignInProvider.this.username = userPoolSignInView.getEnteredUserName();
                CustomCognitoUserPoolsSignInProvider.this.password = userPoolSignInView.getEnteredPassword();
                CognitoUser cognitoUser = CustomCognitoUserPoolsSignInProvider.this.cognitoUserPool.getUser(CustomCognitoUserPoolsSignInProvider.this.username);
                cognitoUser.getSessionInBackground(CustomCognitoUserPoolsSignInProvider.this.authenticationHandler);
            }
        };
        buttonView.setOnClickListener(listener);
        return listener;
    }

    public String getDisplayName() {
        return "Amazon Cognito Your User Pools";
    }

    public String getCognitoLoginKey() {
        return this.cognitoLoginKey;
    }

    public boolean refreshUserSignInState() {
        if(null != this.cognitoUserSession && this.cognitoUserSession.isValid()) {
            return true;
        } else {
            CustomCognitoUserPoolsSignInProvider.RefreshSessionAuthenticationHandler refreshSessionAuthenticationHandler = new CustomCognitoUserPoolsSignInProvider.RefreshSessionAuthenticationHandler();
            this.cognitoUserPool.getCurrentUser().getSession(refreshSessionAuthenticationHandler);
            if(null != refreshSessionAuthenticationHandler.getUserSession()) {
                this.cognitoUserSession = refreshSessionAuthenticationHandler.getUserSession();
                Log.i(LOG_TAG, "refreshUserSignInState: Signed in with Cognito.");
                return true;
            } else {
                Log.i(LOG_TAG, "refreshUserSignInState: Not signed in with Cognito.");
                this.cognitoUserSession = null;
                return false;
            }
        }
    }

    public String getToken() {
        return null == this.cognitoUserSession?null:this.cognitoUserSession.getIdToken().getJWTToken();
    }

    public String refreshToken() {
        if(this.cognitoUserSession != null && !this.cognitoUserSession.isValid()) {
            CustomCognitoUserPoolsSignInProvider.RefreshSessionAuthenticationHandler refreshSessionAuthenticationHandler = new CustomCognitoUserPoolsSignInProvider.RefreshSessionAuthenticationHandler();
            this.cognitoUserPool.getCurrentUser().getSession(refreshSessionAuthenticationHandler);
            if(null != refreshSessionAuthenticationHandler.getUserSession()) {
                this.cognitoUserSession = refreshSessionAuthenticationHandler.getUserSession();
            } else {
                Log.e(LOG_TAG, "Could not refresh the Cognito User Pool Token.");
            }
        }

        return this.getToken();
    }

    public void signOut() {
        if(null != this.cognitoUserPool && null != this.cognitoUserPool.getCurrentUser()) {
            this.cognitoUserPool.getCurrentUser().signOut();
            this.cognitoUserSession = null;
            this.username = null;
            this.password = null;
        }

    }

    public CognitoUserPool getCognitoUserPool() {
        return this.cognitoUserPool;
    }

    private String getCognitoUserPoolId() throws IllegalArgumentException {
        try {
            return this.awsConfiguration.optJsonObject("CognitoUserPool").getString("PoolId");
        } catch (Exception var2) {
            throw new IllegalArgumentException("Cannot find the PoolId from the AWSConfiguration file.", var2);
        }
    }

    private String getCognitoUserPoolClientId() throws IllegalArgumentException {
        try {
            return this.awsConfiguration.optJsonObject("CognitoUserPool").getString("AppClientId");
        } catch (Exception var2) {
            throw new IllegalArgumentException("Cannot find the CognitoUserPool AppClientId from the AWSConfiguration file.", var2);
        }
    }

    private String getCognitoUserPoolClientSecret() throws IllegalArgumentException {
        try {
            return this.awsConfiguration.optJsonObject("CognitoUserPool").getString("AppClientSecret");
        } catch (Exception var2) {
            throw new IllegalArgumentException("Cannot find the CognitoUserPool AppClientSecret from the AWSConfiguration file.", var2);
        }
    }

    private String getCognitoUserPoolRegion() throws IllegalArgumentException {
        try {
            return this.awsConfiguration.optJsonObject("CognitoUserPool").getString("Region");
        } catch (Exception var2) {
            throw new IllegalArgumentException("Cannot find the CognitoUserPool Region from the AWSConfiguration file.", var2);
        }
    }

    private static String getErrorMessageFromException(Exception exception) {
        String message = exception.getLocalizedMessage();
        if(message == null) {
            return exception.getMessage();
        } else {
            int index = message.indexOf("(Service");
            return index == -1?message:message.substring(0, index);
        }
    }

    private static class RefreshSessionAuthenticationHandler implements AuthenticationHandler {
        private CognitoUserSession userSession;

        private RefreshSessionAuthenticationHandler() {
            this.userSession = null;
        }

        private CognitoUserSession getUserSession() {
            return this.userSession;
        }

        public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
            this.userSession = userSession;
        }

        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String UserId) {
            Log.d(CustomCognitoUserPoolsSignInProvider.LOG_TAG, "Can\'t refresh the session silently, due to authentication details needed.");
        }

        public void getMFACode(MultiFactorAuthenticationContinuation continuation) {
            Log.wtf(CustomCognitoUserPoolsSignInProvider.LOG_TAG, "Refresh flow can not trigger request for MFA code.");
        }

        public void authenticationChallenge(ChallengeContinuation continuation) {
            Log.wtf(CustomCognitoUserPoolsSignInProvider.LOG_TAG, "Refresh flow can not trigger request for authentication challenge.");
        }

        public void onFailure(Exception exception) {
            Log.e(CustomCognitoUserPoolsSignInProvider.LOG_TAG, "Can\'t refresh session.", exception);
        }
    }

    public static final class AttributeKeys {
        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        public static final String VERIFICATION_CODE = "verification_code";
        public static final String GIVEN_NAME = "given_name";
        public static final String EMAIL_ADDRESS = "email";
        public static final String PHONE_NUMBER = "phone_number";
        public static final String BACKGROUND_COLOR = "signInBackgroundColor";

        public AttributeKeys() {
        }
    }
}

