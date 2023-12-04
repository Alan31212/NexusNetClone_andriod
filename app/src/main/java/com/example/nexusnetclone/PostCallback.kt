package com.example.nexusnetclone

interface PostCallback {
    fun onDeletePost(postId: Int)
    fun onComment(text: String, postId: Int)
}