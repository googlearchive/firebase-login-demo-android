# Firebase Login Demo for Android with custom authentication

## これはなんですか？

これは大元の firebase login sample(https://github.com/firebase/firebase-login-demo-android)に
custom authentication を追加したものです。

## custom authentication はどんなところが便利なの？

custom authentication　は自分たちで運営しているサービスの認証機能を使ってfirebaseを操作することができます。
分かりやすいイメージだと自分たちで運営しているサービスのユーザーIDがそのままfirebaseでも使えると思って下さい。

firebase に標準でついているソーシャルログイン機能は１サービス１アカウントなので、
１つのアカウントですべてのソーシャルログインを使いたい場合に便利です。

## 使い方

https://github.com/firebase/firebase-login-demo-android のREADMEに書いてある手順に設定をした後
[`res/values/keys.xml`](/app/src/main/res/values/keys.xml) にある your-firebase-secret の値を
firebaseのadmin画面->左メニューのsecrets上にある secret key の値にしてください。
