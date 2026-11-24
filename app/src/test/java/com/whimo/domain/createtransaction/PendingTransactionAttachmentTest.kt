package com.whimo.domain.createtransaction

import android.content.Context
import android.net.Uri
import com.whimo.data.base.common.BaseResult
import com.whimo.data.createtransaction.model.entity.PendingTransactionEntity
import com.whimo.data.createtransaction.model.mappers.toEntity
import com.whimo.data.createtransaction.model.mappers.toModel
import com.whimo.data.createtransaction.model.request.CreateDownstreamTransactionRequest
import com.whimo.data.createtransaction.model.request.CreateProducerTransactionRequest
import com.whimo.data.createtransaction.repository.CreateTransactionRepository
import com.whimo.domain.commodity.models.CommodityModel
import com.whimo.domain.createtransaction.models.CreateTransactionModel
import com.whimo.domain.createtransaction.models.MFile
import com.whimo.domain.transactions.models.TransactionModel
import com.whimo.extensions.isNetworkAvailable
import com.whimo.utils.gson
import com.whimo.utils.prepareFilePart
import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import okio.Buffer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class PendingTransactionAttachmentTest {

    @Test
    fun `URI adapter reads current and legacy pending attachments`() {
        val uri = Uri.parse("content://documents.example/document/123%3A456")
        val model = CreateTransactionModel(file = MFile(uri = uri, name = "map.pdf"))
        val restored = model.toEntity().toModel().transactionModel
        assertEquals(uri, restored.file?.uri)

        val legacyString = """{"file":{"uri":{"uriString":"$uri"},"name":"map.pdf"}}"""
        assertEquals(uri, gson.fromJson(legacyString, CreateTransactionModel::class.java).file?.uri)

        val legacyHierarchical = """{"file":{"uri":{"scheme":"content","authority":{"encoded":"documents.example"},"path":{"encoded":"/document/123%3A456"}},"name":"map.pdf"}}"""
        assertEquals(uri, gson.fromJson(legacyHierarchical, CreateTransactionModel::class.java).file?.uri)

        val oldGsonJson = """{"file":${Gson().toJson(model.file)}}"""
        assertEquals(uri, gson.fromJson(oldGsonJson, CreateTransactionModel::class.java).file?.uri)
    }

    @Test
    fun `offline attachment is copied before queue and removed only after successful sync`() = runTest {
        val context = RuntimeEnvironment.getApplication() as Context
        val source = File(context.cacheDir, "pending-attachment-test.pdf")
        source.writeText("offline document")
        val repository = RecordingRepository()
        mockkStatic("com.whimo.extensions.ContextExtKt")
        every { context.isNetworkAvailable() } returns false
        try {
            val interactor = CreateTransactionInteractorImpl(repository, context)
            val result = interactor.createTransaction(
                CreateTransactionModel(
                    isProducerTransaction = true,
                    commodity = CommodityModel(
                        id = "commodity-id", code = "1", name = "Cocoa", unit = "kg",
                        hasRecipe = false, group = null, balance = 10f,
                    ),
                    volume = 1f,
                    file = MFile(Uri.fromFile(source), "map.pdf", source.length(), "application/pdf"),
                )
            )
            assertTrue(result is BaseResult.Success)
            val queuedFile = repository.pending.single().toModel().transactionModel.file!!
            val copy = File(queuedFile.uri!!.path!!)
            assertTrue(copy.exists())
            assertEquals("offline document", copy.readText())
            assertEquals("application/pdf", queuedFile.mimeType)
            val part = prepareFilePart(context, queuedFile)!!
            val body = Buffer()
            part.body.writeTo(body)
            assertEquals("offline document", body.readUtf8())
            assertEquals("application/pdf", part.body.contentType().toString())
            source.delete()

            every { context.isNetworkAvailable() } returns true
            repository.shouldFail = true
            assertFalse(interactor.sendPendingTransactions())
            assertTrue(copy.exists())
            assertEquals(1, repository.pending.size)

            repository.shouldFail = false
            assertTrue(interactor.sendPendingTransactions())
            assertEquals(copy.absolutePath, repository.uploadedFile?.uri?.path)
            assertFalse(copy.exists())
            assertTrue(repository.pending.isEmpty())
        } finally {
            unmockkStatic("com.whimo.extensions.ContextExtKt")
            source.delete()
        }
    }

    private class RecordingRepository : CreateTransactionRepository {
        val pending = mutableListOf<PendingTransactionEntity>()
        var uploadedFile: MFile? = null
        var shouldFail = false

        override suspend fun createProducerTransaction(request: CreateProducerTransactionRequest): BaseResult<TransactionModel> {
            uploadedFile = request.locationFile
            return if (shouldFail) BaseResult.Error(IOException("temporary failure"))
                else BaseResult.Success(null)
        }

        override suspend fun createDownstreamTransaction(request: CreateDownstreamTransactionRequest): BaseResult<TransactionModel> =
            BaseResult.Success(null)

        override suspend fun getPendingTransactions(): List<PendingTransactionEntity> = pending.toList()
        override suspend fun addPendingTransaction(transaction: PendingTransactionEntity) {
            pending += transaction.copy(id = 1)
        }
        override suspend fun removePendingTransaction(transaction: PendingTransactionEntity) {
            pending.removeAll { it.id == transaction.id }
        }
        override suspend fun removePendingTransaction(transactionId: Long) {
            pending.removeAll { it.id == transactionId }
        }
        override suspend fun clearPendingTransactions() {
            pending.clear()
        }
    }
}
