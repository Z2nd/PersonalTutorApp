package uk.ac.soton.personal_tutor_app.data.repository

import com.google.firebase.functions.FirebaseFunctions

fun sendTestEmail() {
    val data = hashMapOf(
        "to" to "joezheng555@gmail.com", // 收件人
        "subject" to "Test Email",          // 邮件主题
        "content" to "This is a test email"  // 邮件内容
    )

    FirebaseFunctions.getInstance()
        .getHttpsCallable("sendMail")
        .call(data)
        .addOnSuccessListener { result ->
            println("邮件发送成功: ${result.data}")
        }
        .addOnFailureListener { e ->
            println("邮件发送失败: ${e.message}")
        }
}