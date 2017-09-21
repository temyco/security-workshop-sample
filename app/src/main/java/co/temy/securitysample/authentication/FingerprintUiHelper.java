/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package co.temy.securitysample.authentication;

import android.annotation.TargetApi;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.widget.ImageView;
import android.widget.TextView;

import co.temy.securitysample.R;
import co.temy.securitysample.SystemServices;

/**
 * Small helper class to manage text/icon around fingerprint authentication UI.
 */
@TargetApi(Build.VERSION_CODES.M)
public class FingerprintUiHelper extends FingerprintManager.AuthenticationCallback {

    private static final long ERROR_TIMEOUT_MILLIS = 1600;
    private static final long SUCCESS_DELAY_MILLIS = 1300;

    private final SystemServices systemServices;
    private final ImageView icon;
    private final TextView errorTextView;
    private final Callback callback;
    private CancellationSignal mCancellationSignal;

    private boolean mSelfCancelled;

    /**
     * Constructor for {@link FingerprintUiHelper}.
     */
    FingerprintUiHelper(SystemServices systemServices, ImageView icon, TextView errorTextView, Callback callback) {
        this.systemServices = systemServices;
        this.icon = icon;
        this.errorTextView = errorTextView;
        this.callback = callback;
    }

    public boolean isFingerprintAuthAvailable() {
        return systemServices.isFingerprintHardwareAvailable() && systemServices.hasEnrolledFingerprints();
    }

    public void startListening(FingerprintManager.CryptoObject cryptoObject) {
        if (!isFingerprintAuthAvailable()) {
            return;
        }
        mCancellationSignal = new CancellationSignal();
        mSelfCancelled = false;
        // The line below prevents the false positive inspection from Android Studio
        // noinspection ResourceType
        systemServices.authenticateFingerprint(cryptoObject, mCancellationSignal, 0 /* flags */, this, null);
        icon.setImageResource(R.drawable.ic_fp_40px);
    }

    public void stopListening() {
        if (mCancellationSignal != null) {
            mSelfCancelled = true;
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        if (!mSelfCancelled) {
            showError(errString);
            icon.postDelayed(new Runnable() {
                @Override
                public void run() {
                    callback.onError();
                }
            }, ERROR_TIMEOUT_MILLIS);
        }
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        showError(helpString);
    }

    @Override
    public void onAuthenticationFailed() {
        showError(icon.getResources().getString(
                R.string.authentication_fingerprint_not_recognized));
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        errorTextView.removeCallbacks(mResetErrorTextRunnable);
        icon.setImageResource(R.drawable.ic_fingerprint_success);
        errorTextView.setTextColor(
                errorTextView.getResources().getColor(R.color.success_color, null));
        errorTextView.setText(
                errorTextView.getResources().getString(R.string.authentication_fingerprint_success));
        icon.postDelayed(new Runnable() {
            @Override
            public void run() {
                callback.onAuthenticated();
            }
        }, SUCCESS_DELAY_MILLIS);
    }

    private void showError(CharSequence error) {
        icon.setImageResource(R.drawable.ic_fingerprint_error);
        errorTextView.setText(error);
        errorTextView.setTextColor(
                errorTextView.getResources().getColor(R.color.warning_color, null));
        errorTextView.removeCallbacks(mResetErrorTextRunnable);
        errorTextView.postDelayed(mResetErrorTextRunnable, ERROR_TIMEOUT_MILLIS);
    }

    private Runnable mResetErrorTextRunnable = new Runnable() {
        @Override
        public void run() {
            errorTextView.setTextColor(errorTextView.getResources().getColor(R.color.hint_color, null));
            errorTextView.setText(errorTextView.getResources().getString(R.string.authentication_fingerprint_hint));
            icon.setImageResource(R.drawable.ic_fp_40px);
        }
    };

    public interface Callback {

        void onAuthenticated();

        void onError();
    }
}
