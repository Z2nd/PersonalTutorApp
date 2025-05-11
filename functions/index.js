const functions = require("firebase-functions");
const nodemailer = require("nodemailer");

// Configure Nodemailer with Gmail
const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: process.env.GMAIL_USER || "joezheng5@gmail.com", // Use environment variables
    pass: process.env.GMAIL_PASS || "pbmdeieyiyhhqmzt", // Use environment variables
  },
});

exports.sendMail = functions.https.onCall(async (data, context) => {
  // Validate input
  if (!data.to || !data.subject || !data.content) {
    throw new functions.https.HttpsError("invalid-argument", "Missing required fields");
  }

  const mailOptions = {
    from: process.env.GMAIL_USER || "joezheng5@gmail.com",
    to: data.to,
    subject: data.subject,
    text: data.content,
  };

  try {
    await transporter.sendMail(mailOptions);
    return {success: true, message: "Email sent successfully"};
  } catch (error) {
    console.error("Error sending email:", error);
    throw new functions.https.HttpsError("internal", `Failed to send email: ${error.message}`);
  }
});
