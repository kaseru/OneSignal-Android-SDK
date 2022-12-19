package com.onesignal.user.internal.backend

import com.onesignal.common.exceptions.BackendException
import com.onesignal.core.internal.http.HttpResponse
import com.onesignal.core.internal.http.IHttpClient
import com.onesignal.debug.LogLevel
import com.onesignal.debug.internal.logging.Logging
import com.onesignal.extensions.RobolectricTest
import com.onesignal.user.internal.backend.impl.SubscriptionBackendService
import com.onesignal.user.internal.subscriptions.SubscriptionStatus
import io.kotest.assertions.throwables.shouldThrowUnit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.runner.junit4.KotestTestRunner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.runner.RunWith

@RobolectricTest
@RunWith(KotestTestRunner::class)
class SubscriptionBackendServiceTests : FunSpec({
    beforeAny {
        Logging.logLevel = LogLevel.NONE
    }

    test("create subscription") {
        /* Given */
        val aliasLabel = "onesignal_id"
        val aliasValue = "11111111-1111-1111-1111-111111111111"
        val spyHttpClient = mockk<IHttpClient>()
        coEvery { spyHttpClient.post(any(), any()) } returns HttpResponse(202, "{id: \"subscriptionId\"}")
        val subscriptionBackendService = SubscriptionBackendService(spyHttpClient)

        /* When */
        val subscription = SubscriptionObject(
            "no-id",
            SubscriptionObjectType.ANDROID_PUSH,
            "pushToken",
            true,
            SubscriptionStatus.SUBSCRIBED.value
        )

        val response = subscriptionBackendService.createSubscription("appId", aliasLabel, aliasValue, subscription)

        /* Then */
        response shouldBe "subscriptionId"
        coVerify {
            spyHttpClient.post(
                "apps/appId/user/by/$aliasLabel/$aliasValue/subscription",
                withArg {
                    it.has("id") shouldBe false
                    it.getString("type") shouldBe "AndroidPush"
                    it.getString("token") shouldBe "pushToken"
                    it.getBoolean("enabled") shouldBe true
                    it.getInt("notification_types") shouldBe 1
                }
            )
        }
    }

    test("create subscription throws exception when bad response") {
        /* Given */
        val aliasLabel = "onesignal_id"
        val aliasValue = "11111111-1111-1111-1111-111111111111"
        val spyHttpClient = mockk<IHttpClient>()
        coEvery { spyHttpClient.post(any(), any()) } returns HttpResponse(404, "NOT FOUND")
        val subscriptionBackendService = SubscriptionBackendService(spyHttpClient)

        /* When */
        val subscription = SubscriptionObject(
            "no-id",
            SubscriptionObjectType.ANDROID_PUSH,
            "pushToken",
            true,
            SubscriptionStatus.SUBSCRIBED.value
        )
        val exception = shouldThrowUnit<BackendException> {
            subscriptionBackendService.createSubscription(
                "appId",
                aliasLabel,
                aliasValue,
                subscription
            )
        }

        exception.statusCode shouldBe 404
        exception.response shouldBe "NOT FOUND"
        /* Then */
        coVerify {
            spyHttpClient.post(
                "apps/appId/user/by/$aliasLabel/$aliasValue/subscription",
                withArg {
                    it.has("id") shouldBe false
                    it.getString("type") shouldBe "AndroidPush"
                    it.getString("token") shouldBe "pushToken"
                    it.getBoolean("enabled") shouldBe true
                    it.getInt("notification_types") shouldBe 1
                }
            )
        }
    }
})
