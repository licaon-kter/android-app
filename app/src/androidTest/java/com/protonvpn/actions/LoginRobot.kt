/*
 *  Copyright (c) 2021 Proton AG
 *
 * This file is part of ProtonVPN.
 *
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.protonvpn.actions

import com.protonvpn.base.BaseRobot
import android.widget.EditText
import com.google.android.material.textfield.TextInputEditText
import com.protonvpn.android.R
import com.protonvpn.base.BaseVerify
import com.protonvpn.kotlinActions.HomeRobot
import com.protonvpn.test.shared.TestUser


class LoginRobot : BaseRobot() {
    fun signIn(testUser: TestUser): HomeRobot {
        enterCredentials(testUser)
        clickElementById<HomeRobot>(R.id.signInButton)
        return waitUntilDisplayed(R.id.fabQuickConnect)
    }

    fun signInWithIncorrectCredentials(): LoginRobot {
        enterCredentials(TestUser.badUser)
        return clickElementById(R.id.signInButton)
    }

    fun enterCredentials(testUser: TestUser): LoginRobot {
        replaceText<LoginRobot>(R.id.usernameInput, testUser.email)
        return replaceText(R.id.passwordInput, testUser.password)
    }

    fun viewPassword(): LoginRobot = clickElement(
        view
            .isDescendantOf(view.withId(R.id.passwordInput))
            .withId(R.id.text_input_end_icon)
    )

    fun selectNeedHelp(): LoginRobot = clickElementById(R.id.helpButton)

    class Verify : BaseVerify() {

        fun passwordIsVisible(testUser: TestUser) {
            checkIfElementIsDisplayedByText(testUser.password, TextInputEditText::class.java)
        }

        fun userNameIsVisible(testUser: TestUser) = checkIfElementByIdContainsText(
            R.id.usernameInput, testUser.email
        )

        fun needHelpOptionsAreDisplayed() {
            checkIfElementIsDisplayedById(R.id.helpOptionForgotUsername)
            checkIfElementIsDisplayedById(R.id.helpOptionForgotPassword)
            checkIfElementIsDisplayedById(R.id.helpOptionOtherIssues)
            checkIfElementIsDisplayedById(R.id.helpOptionOtherIssues)
        }

        fun incorrectLoginCredentialsIsShown() {
            checkIfElementByIdContainsText(
                R.id.snackbar_text,
                "Incorrect login credentials. Please try again"
            )
            //view.withId(R.id.snackbar_text).withText("Incorrect login credentials. Please try again").checkDisplayed()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

}