const functions = require("firebase-functions/v1");
const nodemailer = require("nodemailer");
const cors = require("cors")({origin: true});

// 配置Nodemailer的邮件传输
const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: "joezheng5@gmail.com", // 替换为您的Gmail地址
    pass: "pbmdeieyiyhhqmzt", // 替换为您的Gmail应用专用密码
  },
});

// Cloud Function：处理邮件发送请求
exports.sendEmail = functions.https.onRequest((req, res) => {
  cors(req, res, () => {
    if (req.method !== "POST") {
      return res.status(405).send("Method Not Allowed");
    }

    const {to, subject, text} = req.body;

    // 邮件内容
    const mailOptions = {
      from: "joezheng5@gmail.com", // 发件人地址
      to: to, // 收件人地址
      subject: subject, // 邮件主题
      text: text, // 邮件正文
    };

    // 发送邮件
    transporter.sendMail(mailOptions, (error, info) => {
      if (error) {
        console.error("Error sending email:", error);
        return res.status(500).send("Error sending email: " + error.message);
      }
      console.log("Email sent:", info.response);
      res.status(200).send("Email sent successfully");
    });
  });
});
