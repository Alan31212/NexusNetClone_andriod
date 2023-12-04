package com.example.nexusnetclone

interface AuthCallback {
    fun onLogin(username: String, password: String)
    fun onSignup(username: String, email: String, password: String)
}