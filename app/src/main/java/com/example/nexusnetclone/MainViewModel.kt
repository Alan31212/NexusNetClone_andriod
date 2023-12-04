package com.example.nexusnetclone

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nexusnetclone.api.CreateComment
import com.example.nexusnetclone.api.CreatePost
import com.example.nexusnetclone.api.CreatePostResponse
import com.example.nexusnetclone.api.ImageUploadResponse
import com.example.nexusnetclone.api.NexusNetApiService
import com.example.nexusnetclone.api.Post
import com.example.nexusnetclone.api.UserLoginResponse
import com.example.nexusnetclone.api.UserSignupRequest
import com.example.nexusnetclone.api.UserSignupResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.w3c.dom.Text
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainViewModel: ViewModel() {

    val posts = MutableLiveData<List<Post>>()
    val message = MutableLiveData<String>()
    val loggedIn = MutableLiveData<Boolean>()

    private var accessToken: String = ""
    private var currentUsername: String? = null
    private var currentUserId: Int? = null

    init {
        getAllPosts()
    }

    fun getAllPosts() {
        NexusNetApiService.api
            .getAllPosts()
            .enqueue(object : Callback<List<Post>> {
                override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                    if (response.isSuccessful) {
                        val list = response.body()?.sortedByDescending { post ->
                            val split = post.timestamp.split(".")
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                            val date = LocalDateTime.parse(split[0], formatter)
                            date
                        }
                        posts.value = list ?: listOf()
                    } else {
                        message.value = response.message()
                    }
                }

                override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                    handleError(t)
                }

            })
    }

    fun onLogin(username: String, password: String) {
        NexusNetApiService.api
            .login(username, password)
            .enqueue(object : Callback<UserLoginResponse> {
                override fun onResponse(
                    call: Call<UserLoginResponse>,
                    response: Response<UserLoginResponse>
                ) {
                    if (response.isSuccessful){
                        accessToken = "${response.body()?.tokenType} ${response.body()?.accessToken}"
                        currentUsername = response.body()?.username
                        currentUserId = response.body()?.userId
                        loggedIn.value = true
                    } else {
                        message.value = response.message()
                    }
                }

                override fun onFailure(call: Call<UserLoginResponse>, t: Throwable) {
                    handleError(t)
                }

            })
    }
    fun onSignup(username: String, email: String, password: String){
        val signupRequest = UserSignupRequest(username, email, password)
        NexusNetApiService.api
            .signup(signupRequest)
            .enqueue(object : Callback<UserSignupResponse>{
                override fun onResponse(
                    call: Call<UserSignupResponse>,
                    response: Response<UserSignupResponse>
                ) {
                    if (response.isSuccessful){
                        message.value = "User $username created. Logging in..."
                        onLogin(username, password)
                    } else {
                        message.value = response.message()
                    }
                }

                override fun onFailure(call: Call<UserSignupResponse>, t: Throwable) {
                    TODO("Not yet implemented")
                }

            })
    }

    fun onPostUpload(inputStream: InputStream, caption: String){
        val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), inputStream.readBytes())
        val part = MultipartBody.Part.createFormData("image", "name.jpg", requestBody)
        NexusNetApiService.api
            .uploadImage(part, accessToken)
            .enqueue(object : Callback<ImageUploadResponse> {
                override fun onResponse(
                    call: Call<ImageUploadResponse>,
                    response: Response<ImageUploadResponse>
                ) {
                    val imageUrl = response.body()?.filename
                    if (response.isSuccessful && !imageUrl.isNullOrEmpty()) {
                        finishPostUpload(imageUrl, caption)
                    } else {
                        message.value = "Something went wrong"
                    }
                }

                override fun onFailure(call: Call<ImageUploadResponse>, t: Throwable) {
                    handleError(t)
                }
            })
    }

    private fun finishPostUpload(imageUrl: String, caption: String){
        if (currentUserId == null) {
            message.value = "went wrong"
            return
        }
        val createPost = CreatePost(imageUrl, "relative", caption, currentUserId!!)
        NexusNetApiService.api
            .createPost(createPost, accessToken)
            .enqueue(object : Callback<CreatePostResponse> {
                override fun onResponse(
                    call: Call<CreatePostResponse>,
                    response: Response<CreatePostResponse>
                ) {
                    if (response.isSuccessful){
                        message.value = "Post created successfully"
                        getAllPosts()
                    } else {
                        message.value = "Something wrong"
                    }
                }

                override fun onFailure(call: Call<CreatePostResponse>, t: Throwable) {
                    handleError(t)
                }

            })
    }

    fun onDeletePost(postId: Int){
        NexusNetApiService.api
            .deletePost(postId, accessToken)
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        message.value = "Post deleted"
                        getAllPosts()
                    } else {
                        message.value = "Post cannot be deleted"
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    handleError(t)
                }

            })

    }

    fun postComment(text: String, postId: Int){
        if (currentUsername.isNullOrEmpty()){
            message.value = "Something went wrong"
            return
        }
        val createComment = CreateComment(currentUsername!!, text, postId)
        NexusNetApiService.api
            .createComment(createComment, accessToken)
            .enqueue(object : Callback<CreatePostResponse> {
                override fun onResponse(
                    call: Call<CreatePostResponse>,
                    response: Response<CreatePostResponse>
                ) {
                    if (response.isSuccessful){
                        message.value = "Comment created"
                        getAllPosts()
                    } else{
                        message.value = "Cannot create comment"
                    }
                }

                override fun onFailure(call: Call<CreatePostResponse>, t: Throwable) {
                    handleError(t)
                }

            })
    }

    private fun handleError(t: Throwable) {
        message.value = t.localizedMessage
        t.printStackTrace()
    }

    fun onLogout() {
        accessToken = ""
        currentUsername = null
        currentUserId = null
        loggedIn.value = false
    }

}