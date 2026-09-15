package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.model.WalletEntity
import com.example.data.model.WalletTransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertNotNull(appName)
    }

    @Test
    fun `wallet entity holds accurate initial balance`() {
        val wallet = WalletEntity(
            userId = "user_123",
            walletId = "wal_user_123",
            balance = 1250.0,
            currency = "PKR",
            userRole = "PASSENGER"
        )
        assertEquals("user_123", wallet.userId)
        assertEquals(1250.0, wallet.balance, 0.001)
        assertEquals("PKR", wallet.currency)
    }

    @Test
    fun `wallet transaction entity correctly classifies Easypaisa top-up`() {
        val txn = WalletTransactionEntity(
            transactionId = "txn_999",
            userId = "user_123",
            walletId = "wal_user_123",
            type = TransactionType.TOP_UP,
            amount = 1000.0,
            balanceBefore = 250.0,
            balanceAfter = 1250.0,
            status = TransactionStatus.SUCCESS,
            paymentMethod = "EASYPAISA",
            referenceId = "EP-TXN-123456",
            notes = "Easypaisa Top-up"
        )
        assertEquals(TransactionType.TOP_UP, txn.type)
        assertEquals(TransactionStatus.SUCCESS, txn.status)
        assertEquals("EASYPAISA", txn.paymentMethod)
        assertEquals(1000.0, txn.amount, 0.001)
    }
}
