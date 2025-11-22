package com.example.veato.data.remote

import android.net.Uri
import com.example.veato.data.model.*
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileApiDataSourceTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var usersCollection: CollectionReference
    private lateinit var documentRef: DocumentReference
    private lateinit var snapshot: DocumentSnapshot

    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var imageRef: StorageReference
    private lateinit var uploadTask: UploadTask
    private lateinit var uri: Uri

    private lateinit var dataSource: ProfileApiDataSource

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        firestore = mockk(relaxed = true)
        usersCollection = mockk(relaxed = true)
        documentRef = mockk(relaxed = true)
        snapshot = mockk(relaxed = true)

        storage = mockk(relaxed = true)
        storageRef = mockk(relaxed = true)
        imageRef = mockk(relaxed = true)
        uploadTask = mockk(relaxed = true)
        uri = mockk(relaxed = true)

        every { firestore.collection("users") } returns usersCollection
        every { usersCollection.document(any()) } returns documentRef

        every { storage.reference } returns storageRef

        dataSource = ProfileApiDataSource(firestore, storage)
    }

    @After fun teardown() = unmockkAll()


    private fun <T> fakeTask(value: T): Task<T> {
        val task = mockk<Task<T>>(relaxed = true)
        every { task.isComplete } returns true
        every { task.isSuccessful } returns true
        every { task.exception } returns null
        every { task.result } returns value
        return task
    }

    @Test
    fun download_ReturnsUserProfile() = runBlocking {
        val fakeMap = mapOf(
            "fullName" to "John Doe",
            "username" to "johnd",
            "profilePictureUrl" to "http://img.com/pic.jpg",
            "dietaryRestrictions" to listOf("HALAL"),
            "allergies" to listOf("EGG"),
            "avoidIngredients" to listOf("onion"),
            "favoriteCuisines" to listOf("KOREAN"),
            "spiceTolerance" to "MILD",
            "onboardingCompleted" to true
        )

        every { snapshot.exists() } returns true
        every { snapshot.data } returns fakeMap
        every { documentRef.get() } returns fakeTask(snapshot)

        val result = dataSource.download("uid123")

        assertNotNull(result)
        assertEquals("John Doe", result!!.fullName)
        assertEquals("johnd", result.userName)
    }

    @Test
    fun download_ReturnsNullWhenNotExists() = runBlocking {
        every { snapshot.exists() } returns false
        every { documentRef.get() } returns fakeTask(snapshot)

        val result = dataSource.download("uid123")
        assertNull(result)
    }

    @Test
    fun download_HandlesMissingFieldsGracefully() = runBlocking {
        val fakeMap = mapOf(
            "fullName" to "Jane",
            "username" to "jane123"
        )

        every { snapshot.exists() } returns true
        every { snapshot.data } returns fakeMap
        every { documentRef.get() } returns fakeTask(snapshot)

        val result = dataSource.download("uid001")

        assertNotNull(result)
        assertEquals("Jane", result!!.fullName)
        assertEquals("jane123", result.userName)

        // Defaults
        assertTrue(result.hardConstraints.dietaryRestrictions.isEmpty())
        assertTrue(result.hardConstraints.allergies.isEmpty())
        assertTrue(result.hardConstraints.avoidIngredients.isEmpty())
        assertTrue(result.softPreferences.favoriteCuisines.isEmpty())
        assertEquals(SpiceLevel.MEDIUM, result.softPreferences.spiceTolerance)
    }

    @Test
    fun upload_FirestoreThrows_NoCrash() = runBlocking {
        val profile = UserProfile(
            userId = "uid999",
            fullName = "Fail Test",
            userName = "fail",
            profilePictureUrl = "",
            hardConstraints = HardConstraints(),
            softPreferences = SoftPreferences()
        )

        every { documentRef.set(any<Map<String, Any>>(), any()) } throws RuntimeException("Firestore error")

        // Should NOT crash
        dataSource.upload(profile)
    }

    @Test
    fun uploadProfileImage_UsesCorrectStoragePathPrefix() = runBlocking {
        val capturedPath = slot<String>()

        // capture the path
        every { storageRef.child(capture(capturedPath)) } returns imageRef

        // upload behavior
        every { imageRef.putFile(uri) } returns uploadTask
        every { uploadTask.isComplete } returns true
        every { uploadTask.isSuccessful } returns true
        every { uploadTask.exception } returns null

        // mock URL result (no Uri.parse!)
        val mockUri = mockk<Uri>()
        every { mockUri.toString() } returns "http://x"

        every { imageRef.downloadUrl } returns fakeTask(mockUri)

        // execute
        dataSource.uploadProfileImage("abc123", uri)

        assertTrue(capturedPath.captured.startsWith("profile_images/abc123_"))
    }

    @Test
    fun download_IgnoresInvalidEnumValues() = runBlocking {
        val fakeMap = mapOf(
            "fullName" to "Enum Tester",
            "username" to "enumuser",
            "dietaryRestrictions" to listOf("INVALID_ENUM"),
            "allergies" to listOf("NOT_AN_ALLERGEN"),
            "favoriteCuisines" to listOf("NOT_A_CUISINE"),
            "spiceTolerance" to "NOT_A_LEVEL"
        )

        every { snapshot.exists() } returns true
        every { snapshot.data } returns fakeMap
        every { documentRef.get() } returns fakeTask(snapshot)

        val result = dataSource.download("uidEnum")!!

        assertTrue(result.hardConstraints.dietaryRestrictions.isEmpty())
        assertTrue(result.hardConstraints.allergies.isEmpty())
        assertTrue(result.softPreferences.favoriteCuisines.isEmpty())
        assertEquals(SpiceLevel.MEDIUM, result.softPreferences.spiceTolerance)
    }

    @Test
    fun download_ReturnsNullWhenSnapshotDataMissing() = runBlocking {
        every { snapshot.exists() } returns true
        every { snapshot.data } returns null
        every { documentRef.get() } returns fakeTask(snapshot)

        val result = dataSource.download("uidNull")
        assertNull(result)
    }
}
